package com.test.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

// Resend HTTP API로 메일 발송(Render가 SMTP 포트를 막아 SMTP 대신 HTTPS API 사용).
// 키는 서버 env(RESEND_API_KEY). 미설정(로컬)이면 발송 대신 링크 로그.
@Slf4j
@Component
public class EmailSender {

    // 받는 사람에게 보이는 발신 표시 이름.
    private static final String DISPLAY_NAME = "P의 여행 플래너";

    private final RestClient client;
    private final String fromAddress;
    private final boolean configured;

    public EmailSender(@Value("${resend.api-key:}") String apiKey,
                       @Value("${mail.from:onboarding@resend.dev}") String from) {
        this.fromAddress = from;
        this.configured = apiKey != null && !apiKey.isBlank();
        this.client = RestClient.builder()
                .baseUrl("https://api.resend.com")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    public void send(String to, String subject, String body) {
        if (!configured) {
            log.info("[메일 미설정] to={} subject={}\n{}", to, subject, body);
            return;
        }
        Map<String, Object> payload = Map.of(
                "from", DISPLAY_NAME + " <" + fromAddress + ">",
                "to", List.of(to),
                "subject", subject,
                "text", body
        );
        try {
            client.post()
                    .uri("/emails")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity(); // 200 기대; 4xx/5xx면 예외
        } catch (Exception e) {
            throw new RuntimeException("메일 발송 실패: " + e.getMessage(), e);
        }
    }
}
