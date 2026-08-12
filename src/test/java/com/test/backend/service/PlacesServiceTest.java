package com.test.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.backend.dto.response.PlaceResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlacesServiceTest {

    // 실제 Google Places(New) Text Search 응답 1건 → 필드 매핑 고정.
    @Test
    void toPlace_mapsGoogleFields() throws Exception {
        String json = """
            {
              "id": "ChIJabc123",
              "displayName": {"text": "오호리공원", "languageCode": "ko"},
              "formattedAddress": "일본 후쿠오카현 후쿠오카시",
              "location": {"latitude": 33.5861, "longitude": 130.3799},
              "primaryTypeDisplayName": {"text": "공원"}
            }
            """;
        PlaceResponse p = PlacesService.toPlace(new ObjectMapper().readTree(json));
        assertThat(p.fsqId()).isEqualTo("ChIJabc123");
        assertThat(p.name()).isEqualTo("오호리공원");
        assertThat(p.address()).contains("후쿠오카");
        assertThat(p.category()).isEqualTo("공원");
        assertThat(p.latitude()).isEqualTo(33.5861);
        assertThat(p.longitude()).isEqualTo(130.3799);
        // Pro 필드만 요청 → 거리/전화 없음.
        assertThat(p.distance()).isNull();
        assertThat(p.tel()).isNull();
    }

    // 카테고리/주소 없는 결과도 null 안전.
    @Test
    void toPlace_handlesMissingFields() throws Exception {
        String json = """
            {"displayName": {"text": "이름만"}, "location": {"latitude": 1.0, "longitude": 2.0}}
            """;
        PlaceResponse p = PlacesService.toPlace(new ObjectMapper().readTree(json));
        assertThat(p.name()).isEqualTo("이름만");
        assertThat(p.category()).isNull();
        assertThat(p.address()).isNull();
    }
}
