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

    @Query(
            value = """
                    SELECT t FROM Todo t
                    ORDER BY CASE t.priority
                        WHEN com.test.backend.domain.entity.TodoPriority.HIGH THEN 1
                        WHEN com.test.backend.domain.entity.TodoPriority.MEDIUM THEN 2
                        WHEN com.test.backend.domain.entity.TodoPriority.LOW THEN 3
                    END ASC, t.createdAt DESC, t.id DESC
                    """,
            countQuery = "SELECT COUNT(t) FROM Todo t"
    )
    Page<Todo> findAllByPriorityOrder(Pageable pageable);

    @Query(
            value = """
                    SELECT t FROM Todo t
                    WHERE t.completed = :completed
                    ORDER BY CASE t.priority
                        WHEN com.test.backend.domain.entity.TodoPriority.HIGH THEN 1
                        WHEN com.test.backend.domain.entity.TodoPriority.MEDIUM THEN 2
                        WHEN com.test.backend.domain.entity.TodoPriority.LOW THEN 3
                    END ASC, t.createdAt DESC, t.id DESC
                    """,
            countQuery = "SELECT COUNT(t) FROM Todo t WHERE t.completed = :completed"
    )
    Page<Todo> findByCompletedOrderByPriority(
            @Param("completed") boolean completed,
            Pageable pageable
    );

    @Query("SELECT t FROM Todo t WHERE t.dueAt >= :start AND t.dueAt < :end ORDER BY t.dueAt ASC, t.id ASC")
    List<Todo> findByDueAtBetween(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);
}
