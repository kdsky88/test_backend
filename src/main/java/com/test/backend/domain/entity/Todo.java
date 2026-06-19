package com.test.backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "todos", indexes = {
    @Index(name = "idx_todos_assignee", columnList = "assignee")
})
@Getter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Todo {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 1000)
    private String note;

    @Column(nullable = false)
    private boolean completed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, columnDefinition = "VARCHAR(10) DEFAULT 'MEDIUM'")
    private TodoPriority priority = TodoPriority.MEDIUM;

    @Column(length = 50)
    private String assignee;

    private OffsetDateTime dueAt;

    private Instant completedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    public Todo(String title, String description, OffsetDateTime dueAt) {
        this(title, description, dueAt, TodoPriority.MEDIUM);
    }

    public Todo(String title, String description, OffsetDateTime dueAt, TodoPriority priority) {
        this(title, description, null, dueAt, priority);
    }

    public Todo(String title, String description, String note, OffsetDateTime dueAt, TodoPriority priority) {
        this(title, description, note, dueAt, priority, null);
    }

    public Todo(String title, String description, String note, OffsetDateTime dueAt, TodoPriority priority, String assignee) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.description = description;
        this.note = note;
        this.dueAt = dueAt;
        this.priority = priority;
        this.assignee = assignee;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void updateNote(String note) {
        this.note = note;
    }

    public void updateDueAt(OffsetDateTime dueAt) {
        this.dueAt = dueAt;
    }

    public void updatePriority(TodoPriority priority) {
        this.priority = priority;
    }

    public void updateAssignee(String assignee) {
        this.assignee = assignee;
    }

    public void updateCompleted(boolean completed, Instant changedAt) {
        if (this.completed == completed) {
            return;
        }
        this.completed = completed;
        this.completedAt = completed ? changedAt : null;
    }
}
