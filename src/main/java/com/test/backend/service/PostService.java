package com.test.backend.service;

import com.test.backend.domain.entity.Post;
import com.test.backend.domain.entity.Post.PostStatus;
import com.test.backend.domain.entity.User;
import com.test.backend.dto.request.CreatePostRequest;
import com.test.backend.dto.request.UpdatePostRequest;
import com.test.backend.dto.request.UpdatePostStatusRequest;
import com.test.backend.dto.response.PostResponse;
import com.test.backend.exception.ApiException;
import com.test.backend.repository.PostRepository;
import com.test.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Transactional
    public PostResponse createPost(String email, CreatePostRequest request) {
        User author = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        Post post = new Post();
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setAuthor(author);
        return new PostResponse(postRepository.save(post));
    }

    @Transactional(readOnly = true)
    public List<PostResponse> getPosts(PostStatus status) {
        List<Post> posts = (status != null)
                ? postRepository.findByStatus(status)
                : postRepository.findAll();
        return posts.stream().map(PostResponse::new).toList();
    }

    @Transactional(readOnly = true)
    public PostResponse getPost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));
        return new PostResponse(post);
    }

    @Transactional
    public PostResponse updatePost(String email, Long id, UpdatePostRequest request) {
        Post post = findPostAndCheckOwner(email, id);
        if (request.getTitle() != null) post.setTitle(request.getTitle());
        if (request.getContent() != null) post.setContent(request.getContent());
        return new PostResponse(post);
    }

    @Transactional
    public PostResponse changeStatus(String email, Long id, UpdatePostStatusRequest request) {
        Post post = findPostAndCheckOwner(email, id);
        post.setStatus(request.getStatus());
        return new PostResponse(post);
    }

    @Transactional
    public void deletePost(String email, Long id) {
        Post post = findPostAndCheckOwner(email, id);
        postRepository.delete(post);
    }

    private Post findPostAndCheckOwner(String email, Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));
        if (!post.getAuthor().getEmail().equals(email)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "본인의 게시글만 수정/삭제할 수 있습니다.");
        }
        return post;
    }
}
