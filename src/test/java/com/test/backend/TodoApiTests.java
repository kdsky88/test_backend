package com.test.backend;

import com.test.backend.repository.TodoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TodoApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TodoRepository todoRepository;

    @Test
    void createsTodoWithoutAuthenticationAndReturnsContractFields() throws Exception {
        todoRepository.deleteAll();

        mockMvc.perform(post("/api/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "title": "Write tests",
                          "description": "Cover todo API",
                          "created_by": "junior"
                        }
                        """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.title").value("Write tests"))
            .andExpect(jsonPath("$.created_by").value("junior"))
            .andExpect(jsonPath("$.created_at").exists())
            .andExpect(jsonPath("$.updated_at").exists());
    }

    @Test
    void rejectsBlankCreatedByWithBadRequest() throws Exception {
        mockMvc.perform(post("/api/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "title": "Invalid todo",
                          "created_by": ""
                        }
                        """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("created_by는 필수입니다."));
    }

    @Test
    void listsTodosByIdAscending() throws Exception {
        todoRepository.deleteAll();

        mockMvc.perform(post("/api/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title": "First", "created_by": "junior"}
                        """))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title": "Second", "created_by": "junior"}
                        """))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/todos"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].title").value("First"))
            .andExpect(jsonPath("$[1].title").value("Second"));
    }
}
