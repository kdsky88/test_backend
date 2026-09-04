package com.test.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

// Gmail SMTP(Spring Mail)로 평문 메일 발송. 계정/앱 비밀번호는 서버 env(spring.mail.*)로만.
// 미설정(로컬)이면 실제 발송 대신 링크를 로그로 남겨 테스트 가능하게.
@Slf4j
@Component
public class EmailSender {

    private final JavaMailSender mailSender;
    private final String from;
    private final boolean configured;

    public EmailSender(JavaMailSender mailSender,
                      @Value("${spring.mail.username:}") String username,
                      @Value("${mail.from:}") String from) {
        this.mailSender = mailSender;
        // Gmail SMTP는 from이 인증 계정이어야 함 → 지정 안 하면 계정 주소 사용.
        this.from = (from == null || from.isBlank()) ? username : from;
        this.configured = username != null && !username.isBlank();
    }

    public void send(String to, String subject, String body) {
        if (!configured) {
            log.info("[메일 미설정] to={} subject={}\n{}", to, subject, body);
            return;
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);
        mailSender.send(msg); // 실패 시 예외 → 호출부(AuthService)에서 로깅
    }
}
