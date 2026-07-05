package com.test.backend.repository;

import com.test.backend.domain.entity.Todo;
import com.test.backend.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface TodoRepository extends JpaRepository<Todo, String> {

    // 공통 정렬(ORDER BY)·검색(:search 제목 LIKE)을 모든 목록 쿼리에 동일하게 적용.

    @Query(
            value = """
                    SELECT t FROM Todo t
                    WHERE (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
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
                    WHERE (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
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
                    WHERE (t.owner.id = :ownerId OR t.assignedTo.id = :ownerId)
                    AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
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
                    WHERE (t.owner.id = :ownerId OR t.assignedTo.id = :ownerId)
                    AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
                    """
    )
    Page<Todo> findAllByOwnerIdOrder(
            @Param("ownerId") Long ownerId,
            @Param("sort") String sort,
            @Param("search") String search,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT t FROM Todo t
                    WHERE t.completed = :completed
                    AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
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
                    AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
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
                    WHERE (t.owner.id = :ownerId OR t.assignedTo.id = :ownerId)
                    AND t.completed = :completed
                    AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
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
                    WHERE (t.owner.id = :ownerId OR t.assignedTo.id = :ownerId)
                    AND t.completed = :completed
                    AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
                    """
    )
    Page<Todo> findByOwnerIdAndCompletedOrder(
            @Param("ownerId") Long ownerId,
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
                    AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
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
                    AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
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
                    WHERE (t.owner.id = :ownerId OR t.assignedTo.id = :ownerId)
                    AND (:completed IS NULL OR t.completed = :completed)
                    AND t.assignee = :assignee
                    AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
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
                    WHERE (t.owner.id = :ownerId OR t.assignedTo.id = :ownerId)
                    AND (:completed IS NULL OR t.completed = :completed)
                    AND t.assignee = :assignee
                    AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
                    """
    )
    Page<Todo> findByOwnerIdAndAssigneeAndCompleted(
            @Param("ownerId") Long ownerId,
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
                    AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
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
                    AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
                    """
    )
    Page<Todo> findByUnassignedAndCompleted(
            @Param("completed") Boolean completed,
            @Param("sort") String sort,
            @Param("search") String search,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT t FROM Todo t
                    WHERE (t.owner.id = :ownerId OR t.assignedTo.id = :ownerId)
                    AND (:completed IS NULL OR t.completed = :completed)
                    AND (t.assignee IS NULL OR t.assignee = '')
                    AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
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
                    WHERE (t.owner.id = :ownerId OR t.assignedTo.id = :ownerId)
                    AND (:completed IS NULL OR t.completed = :completed)
                    AND (t.assignee IS NULL OR t.assignee = '')
                    AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
                    """
    )
    Page<Todo> findByOwnerIdAndUnassignedAndCompleted(
            @Param("ownerId") Long ownerId,
            @Param("completed") Boolean completed,
            @Param("sort") String sort,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT DISTINCT t.assignee FROM Todo t WHERE t.assignee IS NOT NULL AND t.assignee <> '' ORDER BY t.assignee")
    List<String> findDistinctAssignees();

    @Query("SELECT DISTINCT t.assignee FROM Todo t WHERE (t.owner.id = :ownerId OR t.assignedTo.id = :ownerId) AND t.assignee IS NOT NULL AND t.assignee <> '' ORDER BY t.assignee")
    List<String> findDistinctAssigneesByOwnerId(@Param("ownerId") Long ownerId);

    @Query("""
            SELECT t FROM Todo t
            WHERE COALESCE(t.startAt, t.dueAt) < :end
            AND COALESCE(t.dueAt, t.startAt) >= :start
            ORDER BY COALESCE(t.dueAt, t.startAt) ASC, t.id ASC
            """)
    List<Todo> findByDateRangeOverlap(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);

    @Query("""
            SELECT t FROM Todo t
            WHERE (t.owner.id = :ownerId OR t.assignedTo.id = :ownerId)
            AND COALESCE(t.startAt, t.dueAt) < :end
            AND COALESCE(t.dueAt, t.startAt) >= :start
            ORDER BY COALESCE(t.dueAt, t.startAt) ASC, t.id ASC
            """)
    List<Todo> findByOwnerIdAndDateRangeOverlap(
            @Param("ownerId") Long ownerId,
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end
    );

    @Query(
            value = """
                    SELECT t FROM Todo t
                    WHERE :tag MEMBER OF t.tags
                    AND (:completed IS NULL OR t.completed = :completed)
                    AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
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
                    AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
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
                    WHERE (t.owner.id = :ownerId OR t.assignedTo.id = :ownerId)
                    AND :tag MEMBER OF t.tags
                    AND (:completed IS NULL OR t.completed = :completed)
                    AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
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
                    WHERE (t.owner.id = :ownerId OR t.assignedTo.id = :ownerId)
                    AND :tag MEMBER OF t.tags
                    AND (:completed IS NULL OR t.completed = :completed)
                    AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
                    """
    )
    Page<Todo> findByOwnerIdAndTagAndCompleted(
            @Param("ownerId") Long ownerId,
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
                    AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
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
                    AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
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
                    WHERE (t.owner.id = :ownerId OR t.assignedTo.id = :ownerId)
                    AND :tag MEMBER OF t.tags
                    AND (:completed IS NULL OR t.completed = :completed)
                    AND (t.assignee IS NULL OR t.assignee = '')
                    AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
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
                    WHERE (t.owner.id = :ownerId OR t.assignedTo.id = :ownerId)
                    AND :tag MEMBER OF t.tags
                    AND (:completed IS NULL OR t.completed = :completed)
                    AND (t.assignee IS NULL OR t.assignee = '')
                    AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
                    """
    )
    Page<Todo> findByOwnerIdAndTagAndUnassignedAndCompleted(
            @Param("ownerId") Long ownerId,
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
                    AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
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
                    AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
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

    @Query(
            value = """
                    SELECT t FROM Todo t
                    WHERE (t.owner.id = :ownerId OR t.assignedTo.id = :ownerId)
                    AND :tag MEMBER OF t.tags
                    AND (:completed IS NULL OR t.completed = :completed)
                    AND t.assignee = :assignee
                    AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
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
                    WHERE (t.owner.id = :ownerId OR t.assignedTo.id = :ownerId)
                    AND :tag MEMBER OF t.tags
                    AND (:completed IS NULL OR t.completed = :completed)
                    AND t.assignee = :assignee
                    AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
                    """
    )
    Page<Todo> findByOwnerIdAndTagAndAssigneeAndCompleted(
            @Param("ownerId") Long ownerId,
            @Param("tag") String tag,
            @Param("completed") Boolean completed,
            @Param("assignee") String assignee,
            @Param("sort") String sort,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT DISTINCT tag FROM Todo t JOIN t.tags tag ORDER BY tag")
    List<String> findDistinctTags();

    @Query("SELECT DISTINCT tag FROM Todo t JOIN t.tags tag WHERE (t.owner.id = :ownerId OR t.assignedTo.id = :ownerId) ORDER BY tag")
    List<String> findDistinctTagsByOwnerId(@Param("ownerId") Long ownerId);

    long countByCompleted(boolean completed);

    long countByOwnerId(Long ownerId);

    long countByOwnerIdAndCompleted(Long ownerId, boolean completed);

    @Query("SELECT COUNT(t) FROM Todo t WHERE t.completed = false AND t.dueAt < :now")
    long countOverdue(@Param("now") OffsetDateTime now);

    @Query("SELECT COUNT(t) FROM Todo t WHERE (t.owner.id = :ownerId OR t.assignedTo.id = :ownerId) AND t.completed = false AND t.dueAt < :now")
    long countOverdueByOwnerId(@Param("ownerId") Long ownerId, @Param("now") OffsetDateTime now);

    @Query("SELECT COUNT(t) FROM Todo t WHERE t.completed = false AND t.dueAt >= :start AND t.dueAt < :end")
    long countDueBetween(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);

    @Query("SELECT COUNT(t) FROM Todo t WHERE (t.owner.id = :ownerId OR t.assignedTo.id = :ownerId) AND t.completed = false AND t.dueAt >= :start AND t.dueAt < :end")
    long countDueBetweenByOwnerId(
            @Param("ownerId") Long ownerId,
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end
    );

    @Query("SELECT COUNT(t) FROM Todo t WHERE t.completed = true AND t.completedAt >= :start AND t.completedAt < :end")
    long countCompletedBetween(@Param("start") java.time.Instant start, @Param("end") java.time.Instant end);

    @Query("SELECT COUNT(t) FROM Todo t WHERE (t.owner.id = :ownerId OR t.assignedTo.id = :ownerId) AND t.completed = true AND t.completedAt >= :start AND t.completedAt < :end")
    long countCompletedBetweenByOwnerId(
            @Param("ownerId") Long ownerId,
            @Param("start") java.time.Instant start,
            @Param("end") java.time.Instant end
    );

    // 연속 완료(streak) 계산용: floor 이후의 완료 시각들. Java에서 KST 날짜로 묶어 계산.
    @Query("SELECT t.completedAt FROM Todo t WHERE t.completed = true AND t.completedAt >= :floor")
    java.util.List<java.time.Instant> findCompletedAtSince(@Param("floor") java.time.Instant floor);

    @Query("SELECT t.completedAt FROM Todo t WHERE (t.owner.id = :ownerId OR t.assignedTo.id = :ownerId) AND t.completed = true AND t.completedAt >= :floor")
    java.util.List<java.time.Instant> findCompletedAtSinceByOwnerId(
            @Param("ownerId") Long ownerId,
            @Param("floor") java.time.Instant floor
    );

    // 완료 기록: 완료 시각 내림차순(가장 최근 완료가 위로). 목록 정렬(우선순위 등)과 별개 전용 쿼리.
    @Query("SELECT t FROM Todo t WHERE t.completed = true ORDER BY t.completedAt DESC")
    Page<Todo> findCompletedOrderByCompletedAtDesc(Pageable pageable);

    @Query("SELECT t FROM Todo t WHERE t.completed = true AND (t.owner.id = :ownerId OR t.assignedTo.id = :ownerId) ORDER BY t.completedAt DESC")
    Page<Todo> findCompletedByOwnerOrderByCompletedAtDesc(@Param("ownerId") Long ownerId, Pageable pageable);

    java.util.Optional<Todo> findByIdAndOwnerId(String id, Long ownerId);

    // 소유자 또는 담당자에게 보이는 todo (담당자 완료 허용용)
    @Query("SELECT t FROM Todo t WHERE t.id = :id AND (t.owner.id = :userId OR t.assignedTo.id = :userId)")
    java.util.Optional<Todo> findByIdVisibleTo(@Param("id") String id, @Param("userId") Long userId);

    long countByOwnerIsNull();

    @Modifying
    @Query("UPDATE Todo t SET t.owner = :owner WHERE t.owner IS NULL")
    int assignOwnerToUnowned(@Param("owner") User owner);
}
