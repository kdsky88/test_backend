package com.test.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.test.backend.dto.response.ApiResponse;
import com.test.backend.dto.response.PlaceResponse;
import com.test.backend.exception.TodoApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;

// 지역명 → (Nominatim 지오코딩) → Foursquare Places(ll+반경) 프록시. 키는 서버 env(FSQ_API_KEY)에만 둠.
// FSQ near는 한국어로 쓴 해외 지명(예: "후쿠오카")을 못 잡아서, 직접 지오코딩해 ll로 검색한다.
// Nominatim은 동명 소지역이 앞설 수 있어 결과 중 importance 최고를 고른다(실측: "후쿠오카"=도야마역 대신 후쿠오카시).
@Service
public class PlacesService {

    // FSQ 최상위 카테고리 ID (실측 확인): 음식점 / 명소(랜드마크+예술·엔터).
    private static final String DINING = "4d4b7105d754a06374d81259";
    private static final String LANDMARKS = "4d4b7105d754a06377d81259";
    private static final String ARTS = "4d4b7104d754a06370d81259";
    private static final int MAX_LIMIT = 50;
    // ponytail: 도시 단위 검색 가정. 좁은 동네(예:"홍대")엔 넓지만 도시 커버가 우선. 필요시 조정.
    private static final int SEARCH_RADIUS_M = 15000;

    private final RestClient fsq;
    private final RestClient nominatim;
    private final boolean configured;

    public PlacesService(
            @Value("${foursquare.api-key:}") String apiKey,
            @Value("${foursquare.api-version:2025-06-17}") String apiVersion) {
        this.configured = apiKey != null && !apiKey.isBlank();
        this.fsq = RestClient.builder()
                .baseUrl("https://places-api.foursquare.com")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("X-Places-Api-Version", apiVersion)
                .defaultHeader("Accept", "application/json")
                .build();
        this.nominatim = RestClient.builder()
                .baseUrl("https://nominatim.openstreetmap.org")
                // Nominatim 정책: User-Agent 필수(없으면 403).
                .defaultHeader("User-Agent", "test-todo-app/1.0 (kdsky88@gmail.com)")
                .defaultHeader("Accept", "application/json")
                .build();
    }

    // ponytail: 캐시 없음. 지역별 반복 호출이 쿼터에 잡히면 @Cacheable(region+type) 한 줄 추가.
    public ApiResponse<List<PlaceResponse>> recommend(String region, String type, int limit) {
        if (!configured) {
            throw new TodoApiException(HttpStatus.SERVICE_UNAVAILABLE, "PLACES_NOT_CONFIGURED",
                    "장소 추천이 설정되지 않았습니다(FSQ_API_KEY).");
        }
        if (region == null || region.isBlank()) {
            throw new TodoApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "지역을 입력해야 합니다.");
        }
        double[] ll = geocode(region.strip());
        String categories = "food".equalsIgnoreCase(type) ? DINING : LANDMARKS + "," + ARTS;
        int cappedLimit = Math.max(1, Math.min(limit, MAX_LIMIT));

        JsonNode body;
        try {
            body = fsq.get()
                    .uri(uri -> uri.path("/places/search")
                            .queryParam("ll", ll[0] + "," + ll[1])
                            .queryParam("radius", SEARCH_RADIUS_M)
                            .queryParam("fsq_category_ids", categories)
                            .queryParam("sort", "POPULARITY")
                            .queryParam("limit", cappedLimit)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            throw new TodoApiException(HttpStatus.BAD_GATEWAY, "PLACES_UPSTREAM_ERROR", "장소 검색에 실패했습니다.");
        }

        List<PlaceResponse> places = new ArrayList<>();
        if (body != null && body.has("results")) {
            for (JsonNode r : body.get("results")) {
                places.add(toPlace(r));
            }
        }
        return new ApiResponse<>(places);
    }

    // 지역명 → 좌표. 못 찾으면 REGION_NOT_FOUND.
    private double[] geocode(String region) {
        JsonNode arr;
        try {
            arr = nominatim.get()
                    .uri(uri -> uri.path("/search")
                            .queryParam("q", region)
                            .queryParam("format", "json")
                            .queryParam("limit", 5)
                            .queryParam("accept-language", "ko")
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            throw new TodoApiException(HttpStatus.BAD_GATEWAY, "GEOCODE_ERROR", "지역 검색에 실패했습니다.");
        }
        double[] ll = pickBestLatLon(arr);
        if (ll == null) {
            throw new TodoApiException(HttpStatus.NOT_FOUND, "REGION_NOT_FOUND",
                    "'" + region + "' 지역을 찾지 못했어요. 다른 이름으로 검색해보세요.");
        }
        return ll;
    }

    // Nominatim 결과 중 importance 최고의 좌표. 빈 결과면 null.
    static double[] pickBestLatLon(JsonNode arr) {
        if (arr == null || !arr.isArray() || arr.isEmpty()) {
            return null;
        }
        JsonNode best = null;
        double bestImp = -1;
        for (JsonNode n : arr) {
            double imp = n.path("importance").asDouble(0);
            if (imp >= bestImp) {
                bestImp = imp;
                best = n;
            }
        }
        try {
            return new double[]{
                    Double.parseDouble(best.path("lat").asText()),
                    Double.parseDouble(best.path("lon").asText())
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static PlaceResponse toPlace(JsonNode r) {
        JsonNode loc = r.path("location");
        JsonNode cats = r.path("categories");
        String category = cats.isArray() && !cats.isEmpty() ? cats.get(0).path("name").asText(null) : null;
        return new PlaceResponse(
                r.path("fsq_place_id").asText(null),
                r.path("name").asText(null),
                loc.path("formatted_address").asText(null),
                category,
                r.path("latitude").asDouble(),
                r.path("longitude").asDouble(),
                r.hasNonNull("distance") ? r.get("distance").asInt() : null,
                r.path("tel").asText(null)
        );
    }
}
