package com.test.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class CreateTodoRequest {

    @NotBlank(message = "할 일 제목은 필수입니다.")
    @Size(max = 200, message = "할 일 제목은 200자를 초과할 수 없습니다.")
    private String title;

    private String description;

    @JsonProperty("due_date")
    private LocalDate dueDate;

    @NotBlank(message = "created_by는 필수입니다.")
    @JsonProperty("created_by")
    private String createdBy;
}
