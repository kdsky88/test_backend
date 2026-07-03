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
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

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
