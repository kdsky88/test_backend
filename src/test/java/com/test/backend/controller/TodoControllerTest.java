package com.test.backend.controller;

import com.test.backend.dto.response.ApiResponse;
import com.test.backend.dto.response.TodoListResponse;
import com.test.backend.dto.response.TodoResponse;
import com.test.backend.exception.TodoExceptionHandler;
import com.test.backend.service.TodoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TodoControllerTest {

    @Mock
    private TodoService todoService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TodoController(todoService))
                .setControllerAdvice(new TodoExceptionHandler())
                .build();
    }

    @Test
    void returnsFilteredListWithDataAndMeta() throws Exception {
        given(todoService.getTodos("active", 2, 10))
                .willReturn(new TodoListResponse(
                        List.of(),
                        new TodoListResponse.Meta(2, 10, 0, 0)
                ));

        mockMvc.perform(get("/todos")
                        .param("status", "active")
                        .param("page", "2")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.meta.page").value(2))
                .andExpect(jsonPath("$.meta.limit").value(10));

        verify(todoService).getTodos("active", 2, 10);
    }

    @Test
    void createsTodoWithDataWrapper() throws Exception {
        TodoResponse todo = new TodoResponse(
                "550e8400-e29b-41d4-a716-446655440000",
                "할 일",
                null,
                false,
                null,
                null,
                null,
                null
        );
        given(todoService.createTodo(any())).willReturn(new ApiResponse<>(todo));

        mockMvc.perform(post("/todos")
                        .contentType("application/json")
                        .content("""
                                {"title":"할 일"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(todo.id()))
                .andExpect(jsonPath("$.data.title").value("할 일"));
    }

    @Test
    void deletesTodoWithoutResponseBody() throws Exception {
        String id = "550e8400-e29b-41d4-a716-446655440000";

        mockMvc.perform(delete("/todos/{id}", id))
                .andExpect(status().isNoContent());

        verify(todoService).deleteTodo(id);
    }

    @Test
    void returnsValidationErrorForInvalidNumberParameter() throws Exception {
        mockMvc.perform(get("/todos").param("page", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields.page").exists());
    }

    @Test
    void rejectsDueAtWithoutTimezone() throws Exception {
        mockMvc.perform(patch("/todos/id")
                        .contentType("application/json")
                        .content("""
                                {"dueAt":"2026-06-10T09:00:00"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields.body").exists());
    }
}
