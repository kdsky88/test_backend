package com.test.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateTodoCompletedRequest {

    @NotNull(message = "완료 여부는 필수입니다.")
    private Boolean completed;
}
