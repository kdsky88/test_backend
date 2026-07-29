package com.test.backend.dto.response;

import com.test.backend.domain.entity.Trip;

import java.time.Instant;
import java.time.LocalDate;

public record TripResponse(
        String id,
        String title,
        String destination,
        LocalDate startDate,
        LocalDate endDate,
        Instant createdAt,
        Instant updatedAt,
        String ownerEmail
) {
    public TripResponse(Trip trip) {
        this(
                trip.getId(),
                trip.getTitle(),
                trip.getDestination(),
                trip.getStartDate(),
                trip.getEndDate(),
                trip.getCreatedAt(),
                trip.getUpdatedAt(),
                trip.getOwner() == null ? null : trip.getOwner().getEmail()
        );
    }
}
