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

    // 공통 정렬(ORDER BY)·검색(:search 제목 LIKE)을 모든 목록 쿼리에 동일하게 적용.

    @Query(
            value = """
                    SELECT t FROM Todo t
                    WHERE (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')))
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
                    WHERE (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')))
                    """
    )
    Page<Todo> findAllByPriorityOrder(
            @Param("sort") String sort,
            @Param("search") String search,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT t FROM Todo t
                    WHERE t.completed = :completed
                    AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')))
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
                    WHERE t.completed = :completed
                    AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')))
                    """
    )
    Page<Todo> findByCompletedOrderByPriority(
            @Param("completed") boolean completed,
            @Param("sort") String sort,
            @Param("search") String search,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT t FROM Todo t
                    WHERE (:completed IS NULL OR t.completed = :completed)
                    AND t.assignee = :assignee
                    AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')))
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
                    AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')))
                    """
    )
    Page<Todo> findByAssigneeAndCompleted(
            @Param("completed") Boolean completed,
            @Param("assignee") String assignee,
            @Param("sort") String sort,
            @Param("search") String search,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT t FROM Todo t
                    WHERE (:completed IS NULL OR t.completed = :completed)
                    AND (t.assignee IS NULL OR t.assignee = '')
                    AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')))
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
                    AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')))
                    """
    )
    Page<Todo> findByUnassignedAndCompleted(
            @Param("completed") Boolean completed,
            @Param("sort") String sort,
            @Param("search") String search,
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
                    AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')))
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
                    AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')))
                    """
    )
    Page<Todo> findByTagAndCompleted(
            @Param("tag") String tag,
            @Param("completed") Boolean completed,
            @Param("sort") String sort,
            @Param("search") String search,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT t FROM Todo t
                    WHERE :tag MEMBER OF t.tags
                    AND (:completed IS NULL OR t.completed = :completed)
                    AND (t.assignee IS NULL OR t.assignee = '')
                    AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')))
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
                    AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')))
                    """
    )
    Page<Todo> findByTagAndUnassignedAndCompleted(
            @Param("tag") String tag,
            @Param("completed") Boolean completed,
            @Param("sort") String sort,
            @Param("search") String search,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT t FROM Todo t
                    WHERE :tag MEMBER OF t.tags
                    AND (:completed IS NULL OR t.completed = :completed)
                    AND t.assignee = :assignee
                    AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')))
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
                    AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')))
                    """
    )
    Page<Todo> findByTagAndAssigneeAndCompleted(
            @Param("tag") String tag,
            @Param("completed") Boolean completed,
            @Param("assignee") String assignee,
            @Param("sort") String sort,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT DISTINCT tag FROM Todo t JOIN t.tags tag ORDER BY tag")
    List<String> findDistinctTags();

    long countByCompleted(boolean completed);

    @Query("SELECT COUNT(t) FROM Todo t WHERE t.completed = false AND t.dueAt < :now")
    long countOverdue(@Param("now") OffsetDateTime now);

    @Query("SELECT COUNT(t) FROM Todo t WHERE t.completed = false AND t.dueAt >= :start AND t.dueAt < :end")
    long countDueBetween(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);
}
