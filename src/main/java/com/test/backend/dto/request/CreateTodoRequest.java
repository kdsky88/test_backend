package com.test.backend.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor
public class CreateTodoRequest {

    private String title;
    private String description;
    private OffsetDateTime dueAt;
}
