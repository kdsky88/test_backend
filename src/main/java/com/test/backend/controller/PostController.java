package com.test.backend.controller;

import com.test.backend.domain.entity.Post.PostStatus;
import com.test.backend.dto.request.CreatePostRequest;
import com.test.backend.dto.request.UpdatePostRequest;
import com.test.backend.dto.request.UpdatePostStatusRequest;
import com.test.backend.dto.response.PostResponse;
import com.test.backend.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    public ResponseEntity<PostResponse> createPost(
            Authentication authentication,
            @Valid @RequestBody CreatePostRequest request) {
        String email = (String) authentication.getPrincipal();
        PostResponse response = postService.createPost(email, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<PostResponse>> getPosts(
            @RequestParam(required = false) PostStatus status) {
        return ResponseEntity.ok(postService.getPosts(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getPost(@PathVariable Long id) {
        return ResponseEntity.ok(postService.getPost(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PostResponse> updatePost(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody UpdatePostRequest request) {
        String email = (String) authentication.getPrincipal();
        return ResponseEntity.ok(postService.updatePost(email, id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<PostResponse> changeStatus(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody UpdatePostStatusRequest request) {
        String email = (String) authentication.getPrincipal();
        return ResponseEntity.ok(postService.changeStatus(email, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(
            Authentication authentication,
            @PathVariable Long id) {
        String email = (String) authentication.getPrincipal();
        postService.deletePost(email, id);
        return ResponseEntity.noContent().build();
    }
}
