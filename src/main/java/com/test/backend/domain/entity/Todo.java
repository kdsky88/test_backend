package com.test.backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.OffsetDateTime;

@Entity
@Table(name = "todos")
@Getter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Todo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private boolean completed;

    private OffsetDateTime dueAt;

    private Instant completedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    public Todo(String title, String description, OffsetDateTime dueAt, User user) {
        this.title = title;
        this.description = description;
        this.dueAt = dueAt;
        this.user = user;
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
