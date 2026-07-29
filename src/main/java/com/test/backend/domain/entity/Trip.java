package com.test.backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "trips", indexes = {
    @Index(name = "idx_trips_owner", columnList = "owner_id")
})
@Getter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Trip {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 100)
    private String destination;

    private LocalDate startDate;

    private LocalDate endDate;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    public Trip(String title, String destination, LocalDate startDate, LocalDate endDate) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.destination = destination;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void assignOwner(User owner) {
        this.owner = owner;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateDestination(String destination) {
        this.destination = destination;
    }

    public void updateStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public void updateEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
