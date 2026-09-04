package com.test.backend.service;

import com.test.backend.domain.entity.User;
import com.test.backend.dto.request.ChangePasswordRequest;
import com.test.backend.dto.request.LoginRequest;
import com.test.backend.dto.request.RegisterRequest;
import com.test.backend.dto.response.TokenResponse;
import com.test.backend.exception.ApiException;
import com.test.backend.repository.UserRepository;
import com.test.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailSender emailSender;

    @Value("${app.web-url:https://test-todo-app-f4c9a.web.app}")
    private String webUrl;

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다.");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());

        String accessToken = jwtTokenProvider.generateAccessToken(request.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(request.getEmail());
        user.setRefreshToken(refreshToken);

        userRepository.save(user);

        return new TokenResponse(accessToken, refreshToken);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());
        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        return new TokenResponse(accessToken, refreshToken);
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다."));

        // 400: 잘못된 현재 비밀번호는 인증 실패(401)가 아니어야 함. 401이면 프론트 apiClient가
        // refresh를 돌려 토큰만 회전시키고 재시도 → 무한 churn. 검증 실패로 취급.
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "현재 비밀번호가 올바르지 않습니다.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    // 비밀번호 재설정 요청: 사용자가 있으면 재설정 링크 메일 발송. 이메일 존재 여부는 노출하지 않음(항상 조용히 반환).
    @Transactional(readOnly = true)
    public void forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = jwtTokenProvider.generateResetToken(user.getEmail());
            String link = webUrl + "/?reset=" + token;
            String body = "안녕하세요, P의 여행 플래너입니다.\n\n"
                    + "아래 링크에서 새 비밀번호를 설정하세요(30분간 유효):\n\n"
                    + link + "\n\n"
                    + "본인이 요청하지 않았다면 이 메일을 무시하세요.\n— P의 여행 플래너";
            try {
                emailSender.send(user.getEmail(), "[P의 여행 플래너] 비밀번호 재설정", body);
            } catch (Exception e) {
                log.error("비밀번호 재설정 메일 발송 실패", e); // 전체 스택(SMTP 원인 포함)
            }
        });
    }

    // 재설정 토큰으로 새 비밀번호 설정. 토큰이 유효/reset 타입이어야 함.
    @Transactional
    public void resetPassword(String token, String newPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "비밀번호는 8자 이상이어야 합니다.");
        }
        if (!jwtTokenProvider.validateToken(token) || !"reset".equals(jwtTokenProvider.getTokenType(token))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "링크가 만료되었거나 유효하지 않습니다.");
        }
        String email = jwtTokenProvider.getEmail(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "사용자를 찾을 수 없습니다."));
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setRefreshToken(null); // 재설정 시 기존 세션 무효화
        userRepository.save(user);
    }

    @Transactional
    public TokenResponse refresh(String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.");
        }

        String refreshToken = bearerToken.substring(7);

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "만료되거나 유효하지 않은 Refresh Token입니다.");
        }

        String tokenType = jwtTokenProvider.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh Token이 아닙니다.");
        }

        String email = jwtTokenProvider.getEmail(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다."));

        if (!refreshToken.equals(user.getRefreshToken())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "저장된 Refresh Token과 일치하지 않습니다.");
        }

        // refresh 토큰을 회전(재발급)하지 않음: 모바일 앱 종료/응답 유실로 기기와 DB의
        // refresh 토큰이 어긋나 로그인이 조기 만료되던 문제를 없앰. 새 access 토큰만 발급하고
        // 기존 refresh 토큰(로그인 시 저장, 90일)을 그대로 유지 → refresh가 멱등해짐.
        String newAccessToken = jwtTokenProvider.generateAccessToken(email);
        return new TokenResponse(newAccessToken, refreshToken);
    }
}
