package com.test.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.backend.dto.response.PlaceResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlacesServiceTest {

    // 실제 FSQ place search 응답 1건(축약) → 필드 매핑이 안 깨지는지 고정.
    @Test
    void toPlace_extractsCoreFields() throws Exception {
        String json = """
            {
              "fsq_place_id": "572c2d36498ec2eb8308ade5",
              "name": "Joo Ok (주옥)",
              "latitude": 37.564705,
              "longitude": 126.977667,
              "distance": 157,
              "tel": "050-71410-9393",
              "categories": [{"name": "Korean Restaurant"}],
              "location": {"formatted_address": "중구 소공로 119, 소공동, 서울특별시, 06064"}
            }
            """;
        PlaceResponse p = PlacesService.toPlace(new ObjectMapper().readTree(json));
        assertThat(p.fsqId()).isEqualTo("572c2d36498ec2eb8308ade5");
        assertThat(p.name()).isEqualTo("Joo Ok (주옥)");
        assertThat(p.category()).isEqualTo("Korean Restaurant");
        assertThat(p.address()).contains("소공로");
        assertThat(p.latitude()).isEqualTo(37.564705);
        assertThat(p.longitude()).isEqualTo(126.977667);
        assertThat(p.distance()).isEqualTo(157);
    }

    // 핵심 버그 픽스: 첫 결과가 아니라 importance 최고를 고른다.
    // (Nominatim "후쿠오카" → 0번=도야마 동명 역, 최고 importance=후쿠오카시)
    @Test
    void pickBestLatLon_choosesHighestImportance_notFirst() throws Exception {
        String json = """
            [
              {"lat":"36.708","lon":"136.931","importance":0.404},
              {"lat":"33.595","lon":"130.362","importance":0.518}
            ]
            """;
        double[] ll = PlacesService.pickBestLatLon(new ObjectMapper().readTree(json));
        assertThat(ll).isNotNull();
        assertThat(ll[0]).isEqualTo(33.595); // 후쿠오카시
        assertThat(ll[1]).isEqualTo(130.362);
    }

    @Test
    void pickBestLatLon_emptyOrNull_returnsNull() throws Exception {
        assertThat(PlacesService.pickBestLatLon(new ObjectMapper().readTree("[]"))).isNull();
        assertThat(PlacesService.pickBestLatLon(null)).isNull();
    }

    // 카테고리/거리 없는 결과도 null 안전.
    @Test
    void toPlace_handlesMissingFields() throws Exception {
        String json = """
            {"name": "이름만", "latitude": 1.0, "longitude": 2.0, "categories": [], "location": {}}
            """;
        PlaceResponse p = PlacesService.toPlace(new ObjectMapper().readTree(json));
        assertThat(p.name()).isEqualTo("이름만");
        assertThat(p.category()).isNull();
        assertThat(p.distance()).isNull();
        assertThat(p.address()).isNull();
    }
}
