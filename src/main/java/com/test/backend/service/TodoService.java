package com.test.backend.service;

import com.test.backend.domain.entity.Todo;
import com.test.backend.domain.entity.Todo.TodoStatus;
import com.test.backend.domain.entity.User;
import com.test.backend.dto.request.CreateTodoRequest;
import com.test.backend.dto.request.UpdateTodoRequest;
import com.test.backend.dto.request.UpdateTodoStatusRequest;
import com.test.backend.dto.response.TodoResponse;
import com.test.backend.exception.ApiException;
import com.test.backend.repository.TodoRepository;
import com.test.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TodoService {

    private final TodoRepository todoRepository;
    private final UserRepository userRepository;
    private final RiskCalculator riskCalculator;

    @Transactional
    public TodoResponse createTodo(String email, CreateTodoRequest request) {
        User owner = findUser(email);
        Todo todo = new Todo();
        todo.setTitle(request.getTitle());
        todo.setDescription(request.getDescription());
        if (request.getPriority() != null) todo.setPriority(request.getPriority());
        todo.setDueDate(request.getDueDate());
        todo.setOwner(owner);
        return new TodoResponse(todoRepository.save(todo), riskCalculator);
    }

    @Transactional(readOnly = true)
    public Page<TodoResponse> getTodos(String email, TodoStatus status, Pageable pageable) {
        User owner = findUser(email);
        Page<Todo> todos = (status != null)
                ? todoRepository.findByOwnerAndStatus(owner, status, pageable)
                : todoRepository.findByOwner(owner, pageable);
        return todos.map(todo -> new TodoResponse(todo, riskCalculator));
    }

    @Transactional(readOnly = true)
    public TodoResponse getTodo(String email, Long id) {
        Todo todo = findTodoAndCheckOwner(email, id);
        return new TodoResponse(todo, riskCalculator);
    }

    @Transactional
    public TodoResponse updateTodo(String email, Long id, UpdateTodoRequest request) {
        Todo todo = findTodoAndCheckOwner(email, id);
        if (request.getTitle() != null) todo.setTitle(request.getTitle());
        if (request.getDescription() != null) todo.setDescription(request.getDescription());
        if (request.getPriority() != null) todo.setPriority(request.getPriority());
        if (request.getDueDate() != null) todo.setDueDate(request.getDueDate());
        return new TodoResponse(todo, riskCalculator);
    }

    @Transactional
    public TodoResponse changeStatus(String email, Long id, UpdateTodoStatusRequest request) {
        Todo todo = findTodoAndCheckOwner(email, id);
        todo.setStatus(request.getStatus());
        return new TodoResponse(todo, riskCalculator);
    }

    @Transactional
    public void deleteTodo(String email, Long id) {
        Todo todo = findTodoAndCheckOwner(email, id);
        todoRepository.delete(todo);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    private Todo findTodoAndCheckOwner(String email, Long id) {
        Todo todo = todoRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "할일을 찾을 수 없습니다."));
        if (!todo.getOwner().getEmail().equals(email)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "본인의 할일만 접근할 수 있습니다.");
        }
        return todo;
    }
}
