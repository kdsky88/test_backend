package com.test.backend.dto.response;

import com.test.backend.domain.entity.Todo;
import com.test.backend.domain.entity.TodoPriority;

import java.time.Instant;
import java.time.OffsetDateTime;

public record TodoResponse(
        String id,
        String title,
        String description,
        boolean completed,
        TodoPriority priority,
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
                todo.getPriority(),
                todo.getDueAt(),
                todo.getCompletedAt(),
                todo.getCreatedAt(),
                todo.getUpdatedAt()
        );
    }
}
