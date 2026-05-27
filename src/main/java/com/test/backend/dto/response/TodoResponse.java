package com.test.backend.dto.response;

import com.test.backend.domain.entity.Todo;
import com.test.backend.domain.entity.Todo.Priority;
import com.test.backend.domain.entity.Todo.TodoStatus;
import com.test.backend.service.RiskCalculator;
import com.test.backend.service.RiskCalculator.RiskLevel;
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
    private TodoStatus status;
    private Priority priority;
    private LocalDate dueDate;
    private Long ownerId;
    private RiskLevel riskLevel;
    private int riskScore;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public TodoResponse(Todo todo, RiskCalculator calculator) {
        this.id = todo.getId();
        this.title = todo.getTitle();
        this.description = todo.getDescription();
        this.status = todo.getStatus();
        this.priority = todo.getPriority();
        this.dueDate = todo.getDueDate();
        this.ownerId = todo.getOwner().getId();
        this.riskLevel = calculator.calculate(todo);
        this.riskScore = calculator.score(todo);
        this.createdAt = todo.getCreatedAt();
        this.updatedAt = todo.getUpdatedAt();
    }
}
