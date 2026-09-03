package com.test.backend.controller;

import com.test.backend.dto.request.ChangePasswordRequest;
import com.test.backend.dto.request.ForgotPasswordRequest;
import com.test.backend.dto.request.LoginRequest;
import com.test.backend.dto.request.RegisterRequest;
import com.test.backend.dto.request.ResetPasswordRequest;
import com.test.backend.dto.response.TokenResponse;
import com.test.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        TokenResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        TokenResponse response = authService.refresh(authorization);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(
            Authentication authentication, @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }

    // 비밀번호 재설정 요청(메일 발송). 존재 여부 노출 방지로 항상 200.
    @PostMapping("/forgot")
    public ResponseEntity<Void> forgot(@RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok().build();
    }

    // 재설정 토큰으로 새 비밀번호 설정.
    @PostMapping("/reset")
    public ResponseEntity<Void> reset(@RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.noContent().build();
    }
}
