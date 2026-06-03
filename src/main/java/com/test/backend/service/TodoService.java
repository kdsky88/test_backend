package com.test.backend.service;

import com.test.backend.domain.entity.Todo;
import com.test.backend.dto.request.CreateTodoRequest;
import com.test.backend.dto.request.UpdateTodoCompletedRequest;
import com.test.backend.dto.request.UpdateTodoRequest;
import com.test.backend.dto.response.TodoResponse;
import com.test.backend.exception.ApiException;
import com.test.backend.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TodoService {

    private final TodoRepository todoRepository;

    @Transactional
    public TodoResponse createTodo(CreateTodoRequest request) {
        Todo todo = new Todo();
        todo.setTitle(request.getTitle());
        todo.setDescription(request.getDescription());
        todo.setDueDate(request.getDueDate());
        todo.setCreatedBy(request.getCreatedBy());

        return new TodoResponse(todoRepository.save(todo));
    }

    @Transactional(readOnly = true)
    public List<TodoResponse> getTodos(Boolean completed) {
        List<Todo> todos = (completed != null)
                ? todoRepository.findByCompletedOrderByIdAsc(completed)
                : todoRepository.findAllByOrderByIdAsc();
        return todos.stream().map(TodoResponse::new).toList();
    }

    @Transactional(readOnly = true)
    public TodoResponse getTodo(Long id) {
        return new TodoResponse(findTodo(id));
    }

    @Transactional
    public TodoResponse updateTodo(Long id, UpdateTodoRequest request) {
        Todo todo = findTodo(id);
        if (request.getTitle() != null) todo.setTitle(request.getTitle());
        if (request.getDescription() != null) todo.setDescription(request.getDescription());
        if (request.getCompleted() != null) todo.setCompleted(request.getCompleted());
        if (request.getDueDate() != null) todo.setDueDate(request.getDueDate());
        return new TodoResponse(todo);
    }

    @Transactional
    public TodoResponse changeCompleted(Long id, UpdateTodoCompletedRequest request) {
        Todo todo = findTodo(id);
        todo.setCompleted(request.getCompleted());
        return new TodoResponse(todo);
    }

    @Transactional
    public TodoResponse deleteTodo(Long id) {
        Todo todo = findTodo(id);
        TodoResponse response = new TodoResponse(todo);
        todoRepository.delete(todo);
        return response;
    }

    private Todo findTodo(Long id) {
        return todoRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "할 일을 찾을 수 없습니다."));
    }
}
