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
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(placesService.recommend(region, type, limit));
    }
}
