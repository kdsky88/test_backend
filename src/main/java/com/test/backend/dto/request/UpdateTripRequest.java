package com.test.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// present 플래그로 '미포함'과 'null로 지우기'를 구분(UpdateTodoRequest와 동일 패턴).
@Getter
@NoArgsConstructor
public class UpdateTripRequest {

    private boolean titlePresent;
    private String title;

    private boolean destinationPresent;
    private String destination;

    private boolean startDatePresent;
    private LocalDate startDate;

    private boolean endDatePresent;
    private LocalDate endDate;

    @JsonSetter("title")
    public void setTitle(String title) {
        this.titlePresent = true;
        this.title = title;
    }

    @JsonSetter("destination")
    public void setDestination(String destination) {
        this.destinationPresent = true;
        this.destination = destination;
    }

    @JsonSetter("startDate")
    public void setStartDate(LocalDate startDate) {
        this.startDatePresent = true;
        this.startDate = startDate;
    }

    @JsonSetter("endDate")
    public void setEndDate(LocalDate endDate) {
        this.endDatePresent = true;
        this.endDate = endDate;
    }

    public boolean hasAnyField() {
        return titlePresent || destinationPresent || startDatePresent || endDatePresent;
    }
}
