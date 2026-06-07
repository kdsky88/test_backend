package com.test.backend.dto.response;

import com.test.backend.domain.entity.Todo;

import java.time.Instant;
import java.time.OffsetDateTime;

public record TodoResponse(
        Long id,
        String title,
        String description,
        boolean completed,
        OffsetDateTime dueAt,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public TodoResponse(Todo todo) {
        this(
                todo.getId(),
                todo.getTitle(),
                todo.getDescription(),
                todo.isCompleted(),
                todo.getDueAt(),
                todo.getCompletedAt(),
                todo.getCreatedAt(),
                todo.getUpdatedAt()
        );
    }
}
