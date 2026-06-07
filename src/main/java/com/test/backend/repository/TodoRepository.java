package com.test.backend.repository;

import com.test.backend.domain.entity.Todo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TodoRepository extends JpaRepository<Todo, String> {

    Page<Todo> findByCompleted(boolean completed, Pageable pageable);
}
