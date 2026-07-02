package com.test.backend.controller;

import com.test.backend.domain.entity.User;
import com.test.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthPasswordIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
        User user = new User();
        user.setEmail("owner@example.com");
        user.setName("Owner");
        user.setPassword(passwordEncoder.encode("password1"));
        userRepository.save(user);
    }

    @Test
    @WithAnonymousUser
    void rejectsUnauthenticatedPasswordChange() throws Exception {
        mockMvc.perform(post("/api/auth/password")
                        .contentType("application/json")
                        .content("{\"currentPassword\":\"password1\",\"newPassword\":\"password2\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "owner@example.com")
    void rejectsWrongCurrentPasswordWith400() throws Exception {
        // 401이 아니라 400이어야 함(프론트 apiClient의 401→refresh churn 방지)
        mockMvc.perform(post("/api/auth/password")
                        .contentType("application/json")
                        .content("{\"currentPassword\":\"wrongpass\",\"newPassword\":\"newpassword2\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "owner@example.com")
    void changesPasswordWhenCurrentIsCorrect() throws Exception {
        mockMvc.perform(post("/api/auth/password")
                        .contentType("application/json")
                        .content("{\"currentPassword\":\"password1\",\"newPassword\":\"newpassword2\"}"))
                .andExpect(status().isNoContent());

        User updated = userRepository.findByEmail("owner@example.com").orElseThrow();
        assertThat(passwordEncoder.matches("newpassword2", updated.getPassword())).isTrue();
    }
}
