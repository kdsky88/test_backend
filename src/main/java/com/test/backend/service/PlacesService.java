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

// 지역명(near)으로 Foursquare Places를 프록시. 키는 서버 env(FSQ_API_KEY)에만 둠.
// near가 지역명을 직접 받으므로 별도 지오코딩 불필요(실측 확인: near=부산/Tokyo 모두 동작).
@Service
public class PlacesService {

    // FSQ 최상위 카테고리 ID (실측 확인): 음식점 / 명소(랜드마크+예술·엔터).
    private static final String DINING = "4d4b7105d754a06374d81259";
    private static final String LANDMARKS = "4d4b7105d754a06377d81259";
    private static final String ARTS = "4d4b7104d754a06370d81259";
    private static final int MAX_LIMIT = 50;

    private final RestClient client;
    private final boolean configured;

    public PlacesService(
            @Value("${foursquare.api-key:}") String apiKey,
            @Value("${foursquare.api-version:2025-06-17}") String apiVersion) {
        this.configured = apiKey != null && !apiKey.isBlank();
        this.client = RestClient.builder()
                .baseUrl("https://places-api.foursquare.com")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("X-Places-Api-Version", apiVersion)
                .defaultHeader("Accept", "application/json")
                .build();
    }

    // ponytail: 캐시 없음. 같은 지역 반복 호출이 쿼터에 잡히면 @Cacheable(region+type) 한 줄 추가.
    public ApiResponse<List<PlaceResponse>> recommend(String region, String type, int limit) {
        if (!configured) {
            throw new TodoApiException(HttpStatus.SERVICE_UNAVAILABLE, "PLACES_NOT_CONFIGURED",
                    "장소 추천이 설정되지 않았습니다(FSQ_API_KEY).");
        }
        if (region == null || region.isBlank()) {
            throw new TodoApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "지역을 입력해야 합니다.");
        }
        String categories = "food".equalsIgnoreCase(type) ? DINING : LANDMARKS + "," + ARTS;
        int cappedLimit = Math.max(1, Math.min(limit, MAX_LIMIT));

        JsonNode body;
        try {
            body = client.get()
                    .uri(uri -> uri.path("/places/search")
                            .queryParam("near", region.strip())
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
