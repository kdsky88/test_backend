package com.test.backend.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.test.backend.controller.TodoController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

@Slf4j
@RestControllerAdvice(assignableTypes = TodoController.class)
public class TodoExceptionHandler {

    @ExceptionHandler(TodoApiException.class)
    public ResponseEntity<TodoErrorResponse> handleTodoApiException(TodoApiException ex) {
        log.error("TodoApiException: {}", ex.getMessage());
        TodoErrorResponse response = new TodoErrorResponse(
                new TodoError(ex.getCode(), ex.getMessage(), ex.getFields())
        );
        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<TodoErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        TodoErrorResponse response = new TodoErrorResponse(
                new TodoError(
                        "INVALID_REQUEST",
                        "요청 본문의 형식이 올바르지 않습니다.",
                        Map.of("body", "dueAt은 타임존을 포함한 ISO 8601 형식이어야 합니다.")
                )
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<TodoErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        TodoErrorResponse response = new TodoErrorResponse(
                new TodoError(
                        "INVALID_REQUEST",
                        "요청 값이 올바르지 않습니다.",
                        Map.of(ex.getName(), "숫자 형식이어야 합니다.")
                )
        );
        return ResponseEntity.badRequest().body(response);
    }

    public record TodoErrorResponse(TodoError error) {
    }

    public record TodoError(
            String code,
            String message,
            @JsonInclude(JsonInclude.Include.NON_NULL)
            Map<String, String> fields
    ) {
    }
}
