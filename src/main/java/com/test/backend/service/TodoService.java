package com.test.backend.service;

import com.test.backend.domain.entity.Todo;
import com.test.backend.domain.entity.User;
import com.test.backend.dto.request.UpdateTodoRequest;
import com.test.backend.dto.response.TodoListResponse;
import com.test.backend.dto.response.TodoResponse;
import com.test.backend.exception.TodoApiException;
import com.test.backend.repository.TodoRepository;
import com.test.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TodoService {

    private static final Sort TODO_SORT = Sort.by(
            Sort.Order.asc("completed"),
            Sort.Order.desc("createdAt"),
            Sort.Order.desc("id")
    );

    private final TodoRepository todoRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public TodoListResponse getTodos(String email, int page, int limit) {
        validatePagination(page, limit);
        User user = findUser(email);
        Page<TodoResponse> todos = todoRepository
                .findByUserId(user.getId(), PageRequest.of(page - 1, limit, TODO_SORT))
                .map(TodoResponse::new);
        return TodoListResponse.from(todos, page);
    }

    @Transactional
    public TodoResponse updateTodo(String email, Long id, UpdateTodoRequest request) {
        validateUpdateRequest(request);
        User user = findUser(email);
        Todo todo = todoRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new TodoApiException(
                        HttpStatus.NOT_FOUND,
                        "TODO_NOT_FOUND",
                        "Todo를 찾을 수 없습니다."
                ));

        if (request.isTitlePresent()) {
            todo.updateTitle(request.getTitle().trim());
        }
        if (request.isDescriptionPresent()) {
            todo.updateDescription(request.getDescription());
        }
        if (request.isDueAtPresent()) {
            todo.updateDueAt(request.getDueAt());
        }
        if (request.isCompletedPresent()) {
            todo.updateCompleted(request.getCompleted(), Instant.now());
        }
        return new TodoResponse(todo);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new TodoApiException(
                        HttpStatus.UNAUTHORIZED,
                        "UNAUTHORIZED",
                        "인증된 사용자를 찾을 수 없습니다."
                ));
    }

    private void validatePagination(int page, int limit) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (page < 1) {
            fields.put("page", "page는 1 이상이어야 합니다.");
        }
        if (limit < 1 || limit > 100) {
            fields.put("limit", "limit는 1 이상 100 이하여야 합니다.");
        }
        if (!fields.isEmpty()) {
            throw invalidRequest(fields);
        }
    }

    private void validateUpdateRequest(UpdateTodoRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (!request.hasAnyField()) {
            fields.put("body", "수정할 필드를 하나 이상 입력해야 합니다.");
        }
        if (request.isTitlePresent()) {
            if (request.getTitle() == null || request.getTitle().isBlank()) {
                fields.put("title", "title은 비어 있을 수 없습니다.");
            } else if (request.getTitle().length() > 200) {
                fields.put("title", "title은 200자를 초과할 수 없습니다.");
            }
        }
        if (request.isCompletedPresent() && request.getCompleted() == null) {
            fields.put("completed", "completed는 null일 수 없습니다.");
        }
        if (!fields.isEmpty()) {
            throw invalidRequest(fields);
        }
    }

    private TodoApiException invalidRequest(Map<String, String> fields) {
        return new TodoApiException(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                "요청 값이 올바르지 않습니다.",
                fields
        );
    }
}
