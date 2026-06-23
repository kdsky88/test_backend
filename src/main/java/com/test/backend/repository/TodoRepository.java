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
                    ORDER BY
                        t.completed ASC,
                        CASE WHEN :sort = 'DUE' AND t.dueAt IS NULL THEN 1 ELSE 0 END ASC,
                        CASE WHEN :sort = 'DUE' THEN t.dueAt END ASC,
                        CASE WHEN :sort = 'PRIORITY' THEN
                            CASE t.priority
                                WHEN com.test.backend.domain.entity.TodoPriority.HIGH THEN 1
                                WHEN com.test.backend.domain.entity.TodoPriority.MEDIUM THEN 2
                                WHEN com.test.backend.domain.entity.TodoPriority.LOW THEN 3
                            END
                        END ASC,
                        t.createdAt DESC, t.id DESC
                    """,
            countQuery = "SELECT COUNT(t) FROM Todo t"
    )
    Page<Todo> findAllByPriorityOrder(@Param("sort") String sort, Pageable pageable);

    @Query(
            value = """
                    SELECT t FROM Todo t
                    WHERE t.completed = :completed
                    ORDER BY
                        t.completed ASC,
                        CASE WHEN :sort = 'DUE' AND t.dueAt IS NULL THEN 1 ELSE 0 END ASC,
                        CASE WHEN :sort = 'DUE' THEN t.dueAt END ASC,
                        CASE WHEN :sort = 'PRIORITY' THEN
                            CASE t.priority
                                WHEN com.test.backend.domain.entity.TodoPriority.HIGH THEN 1
                                WHEN com.test.backend.domain.entity.TodoPriority.MEDIUM THEN 2
                                WHEN com.test.backend.domain.entity.TodoPriority.LOW THEN 3
                            END
                        END ASC,
                        t.createdAt DESC, t.id DESC
                    """,
            countQuery = "SELECT COUNT(t) FROM Todo t WHERE t.completed = :completed"
    )
    Page<Todo> findByCompletedOrderByPriority(
            @Param("completed") boolean completed,
            @Param("sort") String sort,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT t FROM Todo t
                    WHERE (:completed IS NULL OR t.completed = :completed)
                    AND t.assignee = :assignee
                    ORDER BY
                        t.completed ASC,
                        CASE WHEN :sort = 'DUE' AND t.dueAt IS NULL THEN 1 ELSE 0 END ASC,
                        CASE WHEN :sort = 'DUE' THEN t.dueAt END ASC,
                        CASE WHEN :sort = 'PRIORITY' THEN
                            CASE t.priority
                                WHEN com.test.backend.domain.entity.TodoPriority.HIGH THEN 1
                                WHEN com.test.backend.domain.entity.TodoPriority.MEDIUM THEN 2
                                WHEN com.test.backend.domain.entity.TodoPriority.LOW THEN 3
                            END
                        END ASC,
                        t.createdAt DESC, t.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(t) FROM Todo t
                    WHERE (:completed IS NULL OR t.completed = :completed)
                    AND t.assignee = :assignee
                    """
    )
    Page<Todo> findByAssigneeAndCompleted(
            @Param("completed") Boolean completed,
            @Param("assignee") String assignee,
            @Param("sort") String sort,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT t FROM Todo t
                    WHERE (:completed IS NULL OR t.completed = :completed)
                    AND (t.assignee IS NULL OR t.assignee = '')
                    ORDER BY
                        t.completed ASC,
                        CASE WHEN :sort = 'DUE' AND t.dueAt IS NULL THEN 1 ELSE 0 END ASC,
                        CASE WHEN :sort = 'DUE' THEN t.dueAt END ASC,
                        CASE WHEN :sort = 'PRIORITY' THEN
                            CASE t.priority
                                WHEN com.test.backend.domain.entity.TodoPriority.HIGH THEN 1
                                WHEN com.test.backend.domain.entity.TodoPriority.MEDIUM THEN 2
                                WHEN com.test.backend.domain.entity.TodoPriority.LOW THEN 3
                            END
                        END ASC,
                        t.createdAt DESC, t.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(t) FROM Todo t
                    WHERE (:completed IS NULL OR t.completed = :completed)
                    AND (t.assignee IS NULL OR t.assignee = '')
                    """
    )
    Page<Todo> findByUnassignedAndCompleted(
            @Param("completed") Boolean completed,
            @Param("sort") String sort,
            Pageable pageable
    );

    @Query("SELECT DISTINCT t.assignee FROM Todo t WHERE t.assignee IS NOT NULL AND t.assignee <> '' ORDER BY t.assignee")
    List<String> findDistinctAssignees();

    @Query("""
            SELECT t FROM Todo t
            WHERE COALESCE(t.startAt, t.dueAt) < :end
            AND COALESCE(t.dueAt, t.startAt) >= :start
            ORDER BY COALESCE(t.dueAt, t.startAt) ASC, t.id ASC
            """)
    List<Todo> findByDateRangeOverlap(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);

    @Query(
            value = """
                    SELECT t FROM Todo t
                    WHERE :tag MEMBER OF t.tags
                    AND (:completed IS NULL OR t.completed = :completed)
                    ORDER BY
                        t.completed ASC,
                        CASE WHEN :sort = 'DUE' AND t.dueAt IS NULL THEN 1 ELSE 0 END ASC,
                        CASE WHEN :sort = 'DUE' THEN t.dueAt END ASC,
                        CASE WHEN :sort = 'PRIORITY' THEN
                            CASE t.priority
                                WHEN com.test.backend.domain.entity.TodoPriority.HIGH THEN 1
                                WHEN com.test.backend.domain.entity.TodoPriority.MEDIUM THEN 2
                                WHEN com.test.backend.domain.entity.TodoPriority.LOW THEN 3
                            END
                        END ASC,
                        t.createdAt DESC, t.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(t) FROM Todo t
                    WHERE :tag MEMBER OF t.tags
                    AND (:completed IS NULL OR t.completed = :completed)
                    """
    )
    Page<Todo> findByTagAndCompleted(
            @Param("tag") String tag,
            @Param("completed") Boolean completed,
            @Param("sort") String sort,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT t FROM Todo t
                    WHERE :tag MEMBER OF t.tags
                    AND (:completed IS NULL OR t.completed = :completed)
                    AND (t.assignee IS NULL OR t.assignee = '')
                    ORDER BY
                        t.completed ASC,
                        CASE WHEN :sort = 'DUE' AND t.dueAt IS NULL THEN 1 ELSE 0 END ASC,
                        CASE WHEN :sort = 'DUE' THEN t.dueAt END ASC,
                        CASE WHEN :sort = 'PRIORITY' THEN
                            CASE t.priority
                                WHEN com.test.backend.domain.entity.TodoPriority.HIGH THEN 1
                                WHEN com.test.backend.domain.entity.TodoPriority.MEDIUM THEN 2
                                WHEN com.test.backend.domain.entity.TodoPriority.LOW THEN 3
                            END
                        END ASC,
                        t.createdAt DESC, t.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(t) FROM Todo t
                    WHERE :tag MEMBER OF t.tags
                    AND (:completed IS NULL OR t.completed = :completed)
                    AND (t.assignee IS NULL OR t.assignee = '')
                    """
    )
    Page<Todo> findByTagAndUnassignedAndCompleted(
            @Param("tag") String tag,
            @Param("completed") Boolean completed,
            @Param("sort") String sort,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT t FROM Todo t
                    WHERE :tag MEMBER OF t.tags
                    AND (:completed IS NULL OR t.completed = :completed)
                    AND t.assignee = :assignee
                    ORDER BY
                        t.completed ASC,
                        CASE WHEN :sort = 'DUE' AND t.dueAt IS NULL THEN 1 ELSE 0 END ASC,
                        CASE WHEN :sort = 'DUE' THEN t.dueAt END ASC,
                        CASE WHEN :sort = 'PRIORITY' THEN
                            CASE t.priority
                                WHEN com.test.backend.domain.entity.TodoPriority.HIGH THEN 1
                                WHEN com.test.backend.domain.entity.TodoPriority.MEDIUM THEN 2
                                WHEN com.test.backend.domain.entity.TodoPriority.LOW THEN 3
                            END
                        END ASC,
                        t.createdAt DESC, t.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(t) FROM Todo t
                    WHERE :tag MEMBER OF t.tags
                    AND (:completed IS NULL OR t.completed = :completed)
                    AND t.assignee = :assignee
                    """
    )
    Page<Todo> findByTagAndAssigneeAndCompleted(
            @Param("tag") String tag,
            @Param("completed") Boolean completed,
            @Param("assignee") String assignee,
            @Param("sort") String sort,
            Pageable pageable
    );

    @Query("SELECT DISTINCT tag FROM Todo t JOIN t.tags tag ORDER BY tag")
    List<String> findDistinctTags();
}
