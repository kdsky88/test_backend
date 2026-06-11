package com.test.backend.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record TodoListResponse(
        List<TodoResponse> data,
        Meta meta
) {
    public static TodoListResponse from(Page<TodoResponse> page, int requestedPage) {
        return new TodoListResponse(
                page.getContent(),
                new Meta(requestedPage, page.getSize(), page.getTotalElements(), page.getTotalPages())
        );
    }

    public record Meta(
            int page,
            int limit,
            long total,
            int totalPages
    ) {
    }
}
