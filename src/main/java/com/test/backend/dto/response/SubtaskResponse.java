package com.test.backend.dto.response;

import com.test.backend.domain.entity.Subtask;

public record SubtaskResponse(String title, boolean done) {
    public SubtaskResponse(Subtask subtask) {
        this(subtask.getTitle(), subtask.isDone());
    }
}
