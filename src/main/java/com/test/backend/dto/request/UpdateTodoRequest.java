package com.test.backend.dto.request;

import com.test.backend.domain.entity.Todo.Priority;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class UpdateTodoRequest {

    @Size(max = 200, message = "제목은 200자를 초과할 수 없습니다.")
    private String title;

    private String description;

    private Priority priority;

    private LocalDate dueDate;
}
