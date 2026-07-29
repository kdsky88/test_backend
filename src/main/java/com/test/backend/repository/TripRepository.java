package com.test.backend.repository;

import com.test.backend.domain.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TripRepository extends JpaRepository<Trip, String> {

    // 시작일 빠른 순(미정은 뒤), 같으면 최근 생성 순.
    List<Trip> findByOwnerIdOrderByStartDateAscCreatedAtDesc(Long ownerId);

    Optional<Trip> findByIdAndOwnerId(String id, Long ownerId);
}
