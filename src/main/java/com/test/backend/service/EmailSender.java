package com.test.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

// SendGrid Web API로 평문 메일 발송. 키는 서버 env(SENDGRID_API_KEY), 발신자는 MAIL_FROM(인증된 단일 발신자).
@Slf4j
@Component
public class MailSender {

    private final RestClient client;
    private final String from;
    private final boolean configured;

    public MailSender(@Value("${sendgrid.api-key:}") String apiKey,
                      @Value("${mail.from:}") String from) {
        this.from = from;
        this.configured = apiKey != null && !apiKey.isBlank() && from != null && !from.isBlank();
        this.client = RestClient.builder()
                .baseUrl("https://api.sendgrid.com")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    public void send(String to, String subject, String body) {
        if (!configured) {
            // 로컬/미설정 환경: 실제 발송 대신 로그로 남겨 테스트 가능하게(프로덕션은 configured=true라 발송).
            log.info("[메일 미설정] to={} subject={}\n{}", to, subject, body);
            return;
        }
        Map<String, Object> payload = Map.of(
                "personalizations", List.of(Map.of("to", List.of(Map.of("email", to)))),
                "from", Map.of("email", from),
                "subject", subject,
                "content", List.of(Map.of("type", "text/plain", "value", body))
        );
        client.post()
                .uri("/v3/mail/send")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity(); // 202 기대; 4xx/5xx면 예외 → 호출부에서 처리
    }
}
