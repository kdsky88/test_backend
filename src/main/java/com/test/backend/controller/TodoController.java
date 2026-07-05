package com.test.backend.controller;

import com.test.backend.dto.request.AddTagRequest;
import com.test.backend.dto.request.CreateTodoRequest;
import com.test.backend.dto.request.UpdateTodoRequest;
import com.test.backend.dto.response.ApiResponse;
import com.test.backend.dto.response.CalendarResponse;
import com.test.backend.dto.response.TodoListResponse;
import com.test.backend.dto.response.TodoResponse;
import com.test.backend.dto.response.TodoStats;
import com.test.backend.service.TodoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/todos")
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;

    @GetMapping("/calendar")
    public ResponseEntity<CalendarResponse> getCalendar(
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(todoService.getCalendar(year, month));
    }

    @GetMapping("/assignees")
    public ResponseEntity<ApiResponse<List<String>>> getAssignees() {
        return ResponseEntity.ok(todoService.getAssignees());
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<TodoStats>> getStats() {
        return ResponseEntity.ok(todoService.getStats());
    }

    @GetMapping("/completed")
    public ResponseEntity<TodoListResponse> getCompletedHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(todoService.getCompletedHistory(page, limit));
    }

    @GetMapping("/tags")
    public ResponseEntity<ApiResponse<List<String>>> getTags() {
        return ResponseEntity.ok(todoService.getTags());
    }

    @GetMapping
    public ResponseEntity<TodoListResponse> getTodos(
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String assignee,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "priority") String sort,
            @RequestParam(defaultValue = "false") boolean hideCompleted,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(todoService.getTodos(status, page, limit, assignee, tag, sort, hideCompleted, search));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TodoResponse>> createTodo(
            @RequestBody CreateTodoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(todoService.createTodo(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<TodoResponse>> updateTodo(
            @PathVariable String id,
            @RequestBody UpdateTodoRequest request) {
        return ResponseEntity.ok(todoService.updateTodo(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTodo(@PathVariable String id) {
        todoService.deleteTodo(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/tags")
    public ResponseEntity<ApiResponse<TodoResponse>> addTag(
            @PathVariable String id,
            @RequestBody AddTagRequest request) {
        return ResponseEntity.ok(todoService.addTag(id, request.tag()));
    }

    @DeleteMapping("/{id}/tags")
    public ResponseEntity<ApiResponse<TodoResponse>> removeTag(
            @PathVariable String id,
            @RequestParam String tag) {
        return ResponseEntity.ok(todoService.removeTag(id, tag));
    }
}
