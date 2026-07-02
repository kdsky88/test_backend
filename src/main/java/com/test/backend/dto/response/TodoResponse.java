package com.test.backend.dto.response;

import com.test.backend.domain.entity.Todo;
import com.test.backend.domain.entity.TodoPriority;
import com.test.backend.domain.entity.TodoRecurrence;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record TodoResponse(
        String id,
        String title,
        String description,
        String note,
        boolean completed,
        TodoPriority priority,
        OffsetDateTime startAt,
        OffsetDateTime dueAt,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt,
        String assignee,
        List<String> tags,
        TodoRecurrence recurrence,
        String ownerEmail,
        String assignedToEmail,
        String assignedToName
) {
    // ponytail: owner/assignedTo LAZY 접근으로 목록당 N+1 발생 — 소규모라 허용,
    // 커지면 fetch join으로 최적화.
    public TodoResponse(Todo todo) {
        this(
                todo.getId(),
                todo.getTitle(),
                todo.getDescription(),
                todo.getNote(),
                todo.isCompleted(),
                todo.getPriority(),
                todo.getStartAt(),
                todo.getDueAt(),
                todo.getCompletedAt(),
                todo.getCreatedAt(),
                todo.getUpdatedAt(),
                todo.getAssignee(),
                todo.getTags().stream().sorted().collect(Collectors.toList()),
                todo.getRecurrence(),
                todo.getOwner() == null ? null : todo.getOwner().getEmail(),
                todo.getAssignedTo() == null ? null : todo.getAssignedTo().getEmail(),
                todo.getAssignedTo() == null ? null : todo.getAssignedTo().getName()
        );
    }
}
