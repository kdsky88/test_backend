package com.test.backend.controller;

import com.test.backend.domain.entity.Todo.TodoStatus;
import com.test.backend.dto.request.CreateTodoRequest;
import com.test.backend.dto.request.UpdateTodoRequest;
import com.test.backend.dto.request.UpdateTodoStatusRequest;
import com.test.backend.dto.response.TodoResponse;
import com.test.backend.service.TodoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;

    @PostMapping
    public ResponseEntity<TodoResponse> createTodo(
            Authentication authentication,
            @Valid @RequestBody CreateTodoRequest request) {
        String email = (String) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(todoService.createTodo(email, request));
    }

    @GetMapping
    public ResponseEntity<Page<TodoResponse>> getTodos(
            Authentication authentication,
            @RequestParam(required = false) TodoStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        String email = (String) authentication.getPrincipal();
        return ResponseEntity.ok(todoService.getTodos(email, status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TodoResponse> getTodo(
            Authentication authentication,
            @PathVariable Long id) {
        String email = (String) authentication.getPrincipal();
        return ResponseEntity.ok(todoService.getTodo(email, id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TodoResponse> updateTodo(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody UpdateTodoRequest request) {
        String email = (String) authentication.getPrincipal();
        return ResponseEntity.ok(todoService.updateTodo(email, id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TodoResponse> changeStatus(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody UpdateTodoStatusRequest request) {
        String email = (String) authentication.getPrincipal();
        return ResponseEntity.ok(todoService.changeStatus(email, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTodo(
            Authentication authentication,
            @PathVariable Long id) {
        String email = (String) authentication.getPrincipal();
        todoService.deleteTodo(email, id);
        return ResponseEntity.noContent().build();
    }
}
