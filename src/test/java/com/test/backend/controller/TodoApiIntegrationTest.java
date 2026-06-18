package com.test.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TodoApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void supportsPublicCrudFlow() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/todos")
                        .contentType("application/json")
                        .content("""
                                {
                                  "title":"통합 테스트",
                                  "dueAt":"2026-06-10T09:00:00+09:00"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isString())
                .andExpect(jsonPath("$.data.completed").value(false))
                .andReturn();

        JsonNode createBody = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String id = createBody.path("data").path("id").asText();

        mockMvc.perform(patch("/todos/{id}", id)
                        .contentType("application/json")
                        .content("""
                                {"completed":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.completed").value(true))
                .andExpect(jsonPath("$.data.completedAt").isString());

        mockMvc.perform(get("/todos").param("status", "completed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(id))
                .andExpect(jsonPath("$.meta.total").value(1));

        mockMvc.perform(delete("/todos/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(patch("/todos/{id}", id)
                        .contentType("application/json")
                        .content("""
                                {"completed":false}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("TODO_NOT_FOUND"));
    }

    @Test
    void searchesTodosByTitleWithCaseInsensitivePartialMatch() throws Exception {
        String matchingId = createTodo("Spring 검색 API");
        String lowerCaseMatchingId = createTodo("spring batch 점검");
        String nonMatchingId = createTodo("장보기");

        MvcResult searchResult = mockMvc.perform(get("/todos")
                        .param("search", "  SPRING  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.total").value(2))
                .andReturn();

        JsonNode data = objectMapper.readTree(searchResult.getResponse().getContentAsString()).path("data");
        assertThat(StreamSupport.stream(data.spliterator(), false)
                .map(node -> node.path("id").asText())
                .toList())
                .contains(matchingId, lowerCaseMatchingId)
                .doesNotContain(nonMatchingId);

        deleteTodo(matchingId);
        deleteTodo(lowerCaseMatchingId);
        deleteTodo(nonMatchingId);
    }

    private String createTodo(String title) throws Exception {
        MvcResult createResult = mockMvc.perform(post("/todos")
                        .contentType("application/json")
                        .content("""
                                {"title":"%s"}
                                """.formatted(title)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asText();
    }

    private void deleteTodo(String id) throws Exception {
        mockMvc.perform(delete("/todos/{id}", id))
                .andExpect(status().isNoContent());
    }
}
