package com.test.backend.repository;

import com.test.backend.domain.entity.Post;
import com.test.backend.domain.entity.Post.PostStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    @EntityGraph(attributePaths = "author")
    List<Post> findAll();

    @EntityGraph(attributePaths = "author")
    List<Post> findByStatus(PostStatus status);

    @EntityGraph(attributePaths = "author")
    Optional<Post> findById(Long id);
}
