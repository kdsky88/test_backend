package com.test.backend.dto.response;

// 장소 검색 결과 1건. description/rating은 detail=true(상위 티어)일 때만 채워짐.
public record PlaceResponse(
        String fsqId,
        String name,
        String address,
        String category,
        double latitude,
        double longitude,
        Integer distance,
        String tel,
        String description,
        Double rating
) {
}
