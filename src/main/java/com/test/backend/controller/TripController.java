package com.test.backend.controller;

import com.test.backend.dto.request.CreateTripRequest;
import com.test.backend.dto.request.UpdateTripRequest;
import com.test.backend.dto.response.ApiResponse;
import com.test.backend.dto.response.TodoResponse;
import com.test.backend.dto.response.TripResponse;
import com.test.backend.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TripResponse>>> getTrips() {
        return ResponseEntity.ok(tripService.getTrips());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TripResponse>> getTrip(@PathVariable String id) {
        return ResponseEntity.ok(tripService.getTrip(id));
    }

    @GetMapping("/{id}/todos")
    public ResponseEntity<ApiResponse<List<TodoResponse>>> getTripTodos(@PathVariable String id) {
        return ResponseEntity.ok(tripService.getTripTodos(id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TripResponse>> createTrip(@RequestBody CreateTripRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tripService.createTrip(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<TripResponse>> updateTrip(
            @PathVariable String id,
            @RequestBody UpdateTripRequest request) {
        return ResponseEntity.ok(tripService.updateTrip(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrip(@PathVariable String id) {
        tripService.deleteTrip(id);
        return ResponseEntity.noContent().build();
    }
}
