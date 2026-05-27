package com.test.backend.dto.request;

import com.test.backend.domain.entity.Todo.TodoStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateTodoStatusRequest {

    @NotNull(message = "상태 값은 필수입니다.")
    private TodoStatus status;
}
