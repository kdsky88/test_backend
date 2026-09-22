package com.test.backend.controller;

import com.test.backend.domain.entity.User;
import com.test.backend.repository.UserRepository;
import com.test.backend.security.JwtTokenProvider;
import com.test.backend.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthRevocationIntegrationTest {
    @Autowired UserRepository users;
    @Autowired JwtTokenProvider tokens;
    @Autowired AuthService auth;
    @Autowired PasswordEncoder encoder;
    @Autowired MockMvc mvc;
    @Value("${jwt.secret}") String secret;

    // V9 배포 이전 발급분 = version 클레임이 없는 토큰.
    private String legacyToken(String email, String type) {
        Date now = new Date();
        return Jwts.builder().setSubject(email).claim("type", type)
                .setIssuedAt(now).setExpiration(new Date(now.getTime() + 60_000))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }

    private User user() {
        User u = new User();
        u.setEmail("revoke@example.com");
        u.setName("Revoke");
        u.setPassword(encoder.encode("password1"));
        u.setRefreshToken(tokens.generateRefreshToken(u.getEmail(), 0));
        return users.saveAndFlush(u);
    }

    @Test
    void resetIsSingleUseAndRevokesAccessAndRefresh() throws Exception {
        User u = user();
        String access = tokens.generateAccessToken(u.getEmail(), 0);
        String refresh = u.getRefreshToken();
        String reset = tokens.generateResetToken(u.getEmail(), 0);
        auth.resetPassword(reset, "password2");
        assertThatThrownBy(() -> auth.resetPassword(reset, "password3"))
                .isInstanceOf(com.test.backend.exception.ApiException.class);
        mvc.perform(get("/trips").header("Authorization", "Bearer " + access))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").header("Authorization", "Bearer " + refresh))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void legacyTokensWithoutVersionKeepWorkingUntilPasswordChanges() throws Exception {
        User u = user();
        String legacyAccess = legacyToken(u.getEmail(), "access");
        String legacyRefresh = legacyToken(u.getEmail(), "refresh");
        u.setRefreshToken(legacyRefresh);
        users.saveAndFlush(u);

        // 배포 직후: 기존 세션 그대로 동작(강제 로그아웃 없음)
        mvc.perform(get("/trips").header("Authorization", "Bearer " + legacyAccess))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/refresh").header("Authorization", "Bearer " + legacyRefresh))
                .andExpect(status().isOk());

        // 비밀번호가 바뀌면 그때 폐기
        auth.resetPassword(tokens.generateResetToken(u.getEmail(), 0), "password2");
        mvc.perform(get("/trips").header("Authorization", "Bearer " + legacyAccess))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void passwordChangeRevokesPreviouslyIssuedTokens() throws Exception {
        User u = user();
        String access = tokens.generateAccessToken(u.getEmail(), 0);
        String refresh = u.getRefreshToken();
        mvc.perform(post("/api/auth/password").header("Authorization", "Bearer " + access)
                .contentType("application/json")
                .content("{\"currentPassword\":\"password1\",\"newPassword\":\"password2\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/trips").header("Authorization", "Bearer " + access))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").header("Authorization", "Bearer " + refresh))
                .andExpect(status().isUnauthorized());
    }
}
