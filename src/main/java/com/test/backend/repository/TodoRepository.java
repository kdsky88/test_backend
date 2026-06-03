package com.test.backend.repository;

import com.test.backend.domain.entity.Todo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    List<Todo> findAllByOrderByIdAsc();

    List<Todo> findByCompletedOrderByIdAsc(boolean completed);
}
