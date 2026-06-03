package com.test.backend.controller;

import com.test.backend.dto.request.CreateTodoRequest;
import com.test.backend.dto.request.UpdateTodoCompletedRequest;
import com.test.backend.dto.request.UpdateTodoRequest;
import com.test.backend.dto.response.TodoResponse;
import com.test.backend.service.TodoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;

    @PostMapping
    public ResponseEntity<TodoResponse> createTodo(@Valid @RequestBody CreateTodoRequest request) {
        TodoResponse response = todoService.createTodo(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TodoResponse>> getTodos(@RequestParam(required = false) Boolean completed) {
        return ResponseEntity.ok(todoService.getTodos(completed));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TodoResponse> getTodo(@PathVariable Long id) {
        return ResponseEntity.ok(todoService.getTodo(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TodoResponse> updateTodo(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTodoRequest request) {
        return ResponseEntity.ok(todoService.updateTodo(id, request));
    }

    @PatchMapping("/{id}/completed")
    public ResponseEntity<TodoResponse> changeCompleted(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTodoCompletedRequest request) {
        return ResponseEntity.ok(todoService.changeCompleted(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<TodoResponse> deleteTodo(@PathVariable Long id) {
        return ResponseEntity.ok(todoService.deleteTodo(id));
    }
}
