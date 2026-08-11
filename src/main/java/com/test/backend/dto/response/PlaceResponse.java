package com.test.backend.dto.response;

// Foursquare 장소 추천 결과 1건 (프론트 카드/저장용 최소 필드).
public record PlaceResponse(
        String fsqId,
        String name,
        String address,
        String category,
        double latitude,
        double longitude,
        Integer distance,
        String tel
) {
}
