package com.test.backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
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
@Table(name = "todos")
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

    @Column(nullable = false)
    private boolean completed;

    private OffsetDateTime dueAt;

    private Instant completedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    public Todo(String title, String description, OffsetDateTime dueAt) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.description = description;
        this.dueAt = dueAt;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void updateDueAt(OffsetDateTime dueAt) {
        this.dueAt = dueAt;
    }

    public void updateCompleted(boolean completed, Instant changedAt) {
        if (this.completed == completed) {
            return;
        }
        this.completed = completed;
        this.completedAt = completed ? changedAt : null;
    }
}
