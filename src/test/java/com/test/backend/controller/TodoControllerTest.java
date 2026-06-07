package com.test.backend.controller;

import com.test.backend.dto.response.TodoListResponse;
import com.test.backend.exception.TodoExceptionHandler;
import com.test.backend.service.TodoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
    void returnsDataAndMetaWrapperForList() throws Exception {
        given(todoService.getTodos("user@example.com", 2, 10))
                .willReturn(new TodoListResponse(
                        List.of(),
                        new TodoListResponse.Meta(2, 10, 0, 0)
                ));

        mockMvc.perform(get("/todos")
                        .param("page", "2")
                        .param("limit", "10")
                        .principal(authToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.meta.page").value(2))
                .andExpect(jsonPath("$.meta.limit").value(10))
                .andExpect(jsonPath("$.meta.total").value(0))
                .andExpect(jsonPath("$.meta.totalPages").value(0));

        verify(todoService).getTodos("user@example.com", 2, 10);
    }

    @Test
    void returnsTodoErrorFormatForInvalidNumberParameter() throws Exception {
        mockMvc.perform(get("/todos")
                        .param("page", "not-a-number")
                        .principal(authToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.message").exists())
                .andExpect(jsonPath("$.error.fields.page").exists());
    }

    @Test
    void rejectsDueAtWithoutTimezoneUsingTodoErrorFormat() throws Exception {
        mockMvc.perform(patch("/todos/1")
                        .contentType("application/json")
                        .content("""
                                {"dueAt":"2026-06-10T09:00:00"}
                                """)
                        .principal(authToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.fields.body").exists());
    }

    private UsernamePasswordAuthenticationToken authToken() {
        return new UsernamePasswordAuthenticationToken("user@example.com", null, List.of());
    }
}
