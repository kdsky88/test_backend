package com.test.backend.repository;

import com.test.backend.domain.entity.Todo;
import com.test.backend.domain.entity.Todo.TodoStatus;
import com.test.backend.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    @EntityGraph(attributePaths = "owner")
    Page<Todo> findByOwner(User owner, Pageable pageable);

    @EntityGraph(attributePaths = "owner")
    Page<Todo> findByOwnerAndStatus(User owner, TodoStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "owner")
    java.util.Optional<Todo> findById(Long id);
}
