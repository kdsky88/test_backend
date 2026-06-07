package com.test.backend.controller;

import com.test.backend.dto.request.UpdateTodoRequest;
import com.test.backend.dto.response.TodoListResponse;
import com.test.backend.dto.response.TodoResponse;
import com.test.backend.service.TodoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/todos")
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;

    @GetMapping
    public ResponseEntity<TodoListResponse> getTodos(
            Authentication authentication,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        String email = (String) authentication.getPrincipal();
        return ResponseEntity.ok(todoService.getTodos(email, page, limit));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TodoResponse> updateTodo(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody UpdateTodoRequest request) {
        String email = (String) authentication.getPrincipal();
        return ResponseEntity.ok(todoService.updateTodo(email, id, request));
    }
}
