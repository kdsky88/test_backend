package com.test.backend.controller;

import com.test.backend.dto.request.UpdateUserRequest;
import com.test.backend.dto.response.UserResponse;
import com.test.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyInfo(Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        UserResponse response = userService.getMyInfo(email);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMyInfo(
            Authentication authentication,
            @Valid @RequestBody UpdateUserRequest request) {
        String email = (String) authentication.getPrincipal();
        UserResponse response = userService.updateMyInfo(email, request);
        return ResponseEntity.ok(response);
    }
}
