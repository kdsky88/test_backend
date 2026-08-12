package com.test.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.test.backend.dto.response.ApiResponse;
import com.test.backend.dto.response.PlaceResponse;
import com.test.backend.exception.TodoApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// 지역/장소 검색을 Google Places API(New) Text Search로 프록시. 키는 서버 env(GOOGLE_PLACES_KEY)에만.
// Text Search가 지역 텍스트를 직접 처리(별도 지오코딩 불필요). type으로 검색어를 살짝 편향.
@Service
public class PlacesService {

    private static final URI SEARCH_URI = URI.create("https://places.googleapis.com/v1/places:searchText");
    // Pro 필드만(별점/전화 제외) → 저렴한 티어(월 5000회 무료).
    private static final String FIELD_MASK =
            "places.id,places.displayName,places.formattedAddress,places.location,places.primaryTypeDisplayName";
    private static final int MAX_LIMIT = 20;

    private final RestClient google;
    private final boolean configured;

    public PlacesService(@Value("${google.places-key:}") String apiKey) {
        this.configured = apiKey != null && !apiKey.isBlank();
        this.google = RestClient.builder()
                .defaultHeader("X-Goog-Api-Key", apiKey)
                .defaultHeader("X-Goog-FieldMask", FIELD_MASK)
                .defaultHeader("Accept", "application/json")
                .build();
    }

    // ponytail: 캐시 없음. 지역별 반복이 쿼터에 잡히면 @Cacheable(region+type) 한 줄 추가.
    public ApiResponse<List<PlaceResponse>> recommend(String region, String type, int limit) {
        if (!configured) {
            throw new TodoApiException(HttpStatus.SERVICE_UNAVAILABLE, "PLACES_NOT_CONFIGURED",
                    "장소 검색이 설정되지 않았습니다(GOOGLE_PLACES_KEY).");
        }
        if (region == null || region.isBlank()) {
            throw new TodoApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "지역을 입력해야 합니다.");
        }
        String base = region.strip();
        String query = switch (type == null ? "" : type.toLowerCase()) {
            case "food" -> base + " 맛집";
            case "attraction" -> base + " 관광명소";
            default -> base; // 'address'/일반: 입력 그대로
        };
        int cappedLimit = Math.max(1, Math.min(limit, MAX_LIMIT));

        JsonNode body;
        try {
            body = google.post()
                    .uri(SEARCH_URI)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "textQuery", query,
                            "languageCode", "ko",
                            "pageSize", cappedLimit
                    ))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            throw new TodoApiException(HttpStatus.BAD_GATEWAY, "PLACES_UPSTREAM_ERROR", "장소 검색에 실패했습니다.");
        }

        List<PlaceResponse> places = new ArrayList<>();
        if (body != null && body.has("places")) {
            for (JsonNode p : body.get("places")) {
                places.add(toPlace(p));
            }
        }
        return new ApiResponse<>(places);
    }

    static PlaceResponse toPlace(JsonNode p) {
        JsonNode loc = p.path("location");
        return new PlaceResponse(
                p.path("id").asText(null),
                p.path("displayName").path("text").asText(null),
                p.path("formattedAddress").asText(null),
                p.path("primaryTypeDisplayName").path("text").asText(null),
                loc.path("latitude").asDouble(),
                loc.path("longitude").asDouble(),
                null, // distance: Text Search는 기준점 없음
                null  // tel: Pro 필드 아님
        );
    }
}
