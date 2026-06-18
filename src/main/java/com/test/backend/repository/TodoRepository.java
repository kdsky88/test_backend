package com.test.backend.repository;

import com.test.backend.domain.entity.Todo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface TodoRepository extends JpaRepository<Todo, String> {

    Page<Todo> findByCompleted(boolean completed, Pageable pageable);

    Page<Todo> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<Todo> findByCompletedAndTitleContainingIgnoreCase(boolean completed, String title, Pageable pageable);

    @Query("SELECT t FROM Todo t WHERE t.dueAt >= :start AND t.dueAt < :end ORDER BY t.dueAt ASC, t.id ASC")
    List<Todo> findByDueAtBetween(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);
}
