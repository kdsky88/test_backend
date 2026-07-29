package com.test.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.backend.domain.entity.User;
import com.test.backend.repository.TodoRepository;
import com.test.backend.repository.TripRepository;
import com.test.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "owner@example.com")
class TripApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TripRepository tripRepository;
    @Autowired private TodoRepository todoRepository;
    @Autowired private UserRepository userRepository;

    @BeforeEach
    void cleanUp() {
        todoRepository.deleteAll();
        tripRepository.deleteAll();
        userRepository.deleteAll();
        saveUser("owner@example.com", "Owner");
        saveUser("other@example.com", "Other");
    }

    @Test
    void linksTodoToTripAndListsItByTrip() throws Exception {
        String tripId = createTrip("제주 여행", "제주");

        // tripId를 지정해 일정 생성 → 응답에 tripId가 실려 나와야 함(연결 확인).
        mockMvc.perform(post("/todos")
                        .contentType("application/json")
                        .content("{\"title\":\"성산일출봉\",\"tripId\":\"" + tripId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.tripId").value(tripId));

        // 여행별 일정 조회 → 방금 만든 항목이 나와야 함(resolveTrip 쓰기 + findByTripId 읽기 실행).
        mockMvc.perform(get("/trips/{id}/todos", tripId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("성산일출봉"))
                .andExpect(jsonPath("$.data[0].tripId").value(tripId))
                .andExpect(jsonPath("$.data[0].tripTitle").value("제주 여행"));
    }

    @Test
    void rejectsAttachingAnotherOwnersTrip() throws Exception {
        String tripId = createTrip("내 여행", null);

        // 다른 사용자가 남의 여행 id로 일정 연결 시도 → 검증 에러(소유자 스코프).
        mockMvc.perform(post("/todos")
                        .with(org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestPostProcessors.user("other@example.com"))
                        .contentType("application/json")
                        .content("{\"title\":\"침입\",\"tripId\":\"" + tripId + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields.tripId").exists());

        // 남의 여행의 일정 목록도 404.
        mockMvc.perform(get("/trips/{id}/todos", tripId)
                        .with(org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestPostProcessors.user("other@example.com")))
                .andExpect(status().isNotFound());
    }

    private String createTrip(String title, String destination) throws Exception {
        String body = destination == null
                ? "{\"title\":\"" + title + "\"}"
                : "{\"title\":\"" + title + "\",\"destination\":\"" + destination + "\"}";
        var result = mockMvc.perform(post("/trips")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("data").get("id").asText();
    }

    private void saveUser(String email, String name) {
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setPassword("password");
        userRepository.save(user);
    }
}
