package com.test.backend.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdatePostRequest {

    @Size(max = 200, message = "제목은 200자를 초과할 수 없습니다.")
    private String title;

    private String content;
}
