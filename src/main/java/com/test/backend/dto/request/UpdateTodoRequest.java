package com.test.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class UpdateTodoRequest {

    @Size(max = 200, message = "할 일 제목은 200자를 초과할 수 없습니다.")
    private String title;

    private String description;

    private Boolean completed;

    @JsonProperty("due_date")
    private LocalDate dueDate;
}
