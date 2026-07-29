package com.test.backend.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class CreateTripRequest {

    private String title;
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
}
