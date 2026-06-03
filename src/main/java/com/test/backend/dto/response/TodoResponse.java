package com.test.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.test.backend.domain.entity.Todo;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class TodoResponse {

    private Long id;
    private String title;
    private String description;
    private boolean completed;
    @JsonProperty("due_date")
    private LocalDate dueDate;
    @JsonProperty("created_by")
    private String createdBy;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    public TodoResponse(Todo todo) {
        this.id = todo.getId();
        this.title = todo.getTitle();
        this.description = todo.getDescription();
        this.completed = todo.isCompleted();
        this.dueDate = todo.getDueDate();
        this.createdBy = todo.getCreatedBy();
        this.createdAt = todo.getCreatedAt();
        this.updatedAt = todo.getUpdatedAt();
    }
}
