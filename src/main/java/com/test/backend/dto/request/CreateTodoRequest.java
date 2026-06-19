package com.test.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor
public class CreateTodoRequest {

    private String title;
    private String description;
    private String note;
    private OffsetDateTime dueAt;
    private String assignee;

    private boolean priorityPresent;
    private String priority;

    @JsonSetter("priority")
    public void setPriority(String priority) {
        this.priorityPresent = true;
        this.priority = priority;
    }
}
