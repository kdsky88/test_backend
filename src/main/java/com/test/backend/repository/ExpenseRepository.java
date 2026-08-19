package com.test.backend.repository;

import com.test.backend.domain.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, String> {

    List<Expense> findByTripIdAndOwnerIdOrderByCreatedAtDesc(String tripId, Long ownerId);

    Optional<Expense> findByIdAndOwnerId(String id, Long ownerId);
}
