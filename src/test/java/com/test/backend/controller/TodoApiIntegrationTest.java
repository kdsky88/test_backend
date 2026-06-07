package com.test.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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
}
