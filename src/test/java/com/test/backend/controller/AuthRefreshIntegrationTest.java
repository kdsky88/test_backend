package com.test.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithAnonymousUser
class AuthRefreshIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    private String register() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("{\"name\":\"토큰유지\",\"email\":\"keep@example.com\",\"password\":\"password1\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("refreshToken").asText();
    }

    private JsonNode refresh(String refreshToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/refresh")
                        .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    @Test
    void refreshDoesNotRotateRefreshToken() throws Exception {
        String original = register();

        JsonNode refreshed = refresh(original);

        // 새 access는 발급되지만 refresh 토큰은 그대로(회전 안 함)
        assertThat(refreshed.get("accessToken").asText()).isNotBlank();
        assertThat(refreshed.get("refreshToken").asText()).isEqualTo(original);
    }

    @Test
    void sameRefreshTokenWorksRepeatedly() throws Exception {
        String original = register();

        // 같은 refresh 토큰으로 여러 번 refresh 해도 계속 유효(멱등) — 응답 유실/재시도에 안전
        refresh(original);
        JsonNode second = refresh(original);
        assertThat(second.get("refreshToken").asText()).isEqualTo(original);
    }
}
