package com.test.backend.controller;

import com.test.backend.dto.response.ApiResponse;
import com.test.backend.dto.response.PlaceResponse;
import com.test.backend.service.PlacesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/places")
@RequiredArgsConstructor
public class PlacesController {

    private final PlacesService placesService;

    // 지역명으로 추천. type=food(맛집)|attraction(관광지, 기본).
    @GetMapping("/recommend")
    public ResponseEntity<ApiResponse<List<PlaceResponse>>> recommend(
            @RequestParam String region,
            @RequestParam(defaultValue = "attraction") String type,
            @RequestParam(defaultValue = "20") int limit,
            // detail=true면 설명·별점 포함(상위 티어). 즉흥 추천 등 저볼륨에서만.
            @RequestParam(defaultValue = "false") boolean detail) {
        return ResponseEntity.ok(placesService.recommend(region, type, limit, detail));
    }

    // 현재 위치 반경 검색. type=food|attraction(기본).
    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<List<PlaceResponse>>> nearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "attraction") String type,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(placesService.nearby(lat, lng, type, limit));
    }
}
