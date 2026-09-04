package com.test.backend.service;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

// Gmail SMTP(Spring Mail)로 평문 메일 발송. 계정/앱 비밀번호는 서버 env(spring.mail.*)로만.
// 미설정(로컬)이면 실제 발송 대신 링크를 로그로 남겨 테스트 가능하게.
@Slf4j
@Component
public class EmailSender {

    // 받는 사람에게 보이는 발신 표시 이름(주소는 Gmail 계정으로 강제됨).
    private static final String DISPLAY_NAME = "P의 여행 플래너";

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
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            // UTF-8: 한글 제목·본문·표시 이름이 깨지지 않게.
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");
            helper.setFrom(new InternetAddress(from, DISPLAY_NAME, "UTF-8"));
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(mime);
        } catch (Exception e) {
            // 호출부(AuthService)에서 로깅하도록 런타임으로 전달.
            throw new RuntimeException("메일 발송 실패", e);
        }
    }
}
