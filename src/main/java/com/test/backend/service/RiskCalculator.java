package com.test.backend.service;

import com.test.backend.domain.entity.Todo;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class RiskCalculator {

    public RiskLevel calculate(Todo todo) {
        if (todo.getStatus() == Todo.TodoStatus.DONE) {
            return RiskLevel.NONE;
        }
        int score = score(todo);
        return fromScore(score);
    }

    public int score(Todo todo) {
        if (todo.getStatus() == Todo.TodoStatus.DONE) return 0;
        return priorityScore(todo.getPriority()) + dueDateScore(todo.getDueDate());
    }

    private int priorityScore(Todo.Priority priority) {
        return switch (priority) {
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
        };
    }

    private int dueDateScore(LocalDate dueDate) {
        if (dueDate == null) return 0;
        long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
        if (daysUntil < 0) return 3;
        if (daysUntil <= 1) return 2;
        if (daysUntil <= 7) return 1;
        return 0;
    }

    private RiskLevel fromScore(int score) {
        if (score >= 5) return RiskLevel.CRITICAL;
        if (score >= 4) return RiskLevel.HIGH;
        if (score >= 3) return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }

    public enum RiskLevel {
        NONE, LOW, MEDIUM, HIGH, CRITICAL
    }
}
