package com.test.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor
public class UpdateTodoRequest {

    private boolean titlePresent;
    private String title;

    private boolean descriptionPresent;
    private String description;

    private boolean notePresent;
    private String note;

    private boolean dueAtPresent;
    private OffsetDateTime dueAt;

    private boolean completedPresent;
    private Boolean completed;

    private boolean priorityPresent;
    private String priority;

    private boolean assigneePresent;
    private String assignee;

    @JsonSetter("title")
    public void setTitle(String title) {
        this.titlePresent = true;
        this.title = title;
    }

    @JsonSetter("description")
    public void setDescription(String description) {
        this.descriptionPresent = true;
        this.description = description;
    }

    @JsonSetter("note")
    public void setNote(String note) {
        this.notePresent = true;
        this.note = note;
    }

    @JsonSetter("dueAt")
    public void setDueAt(OffsetDateTime dueAt) {
        this.dueAtPresent = true;
        this.dueAt = dueAt;
    }

    @JsonSetter("completed")
    public void setCompleted(Boolean completed) {
        this.completedPresent = true;
        this.completed = completed;
    }

    @JsonSetter("priority")
    public void setPriority(String priority) {
        this.priorityPresent = true;
        this.priority = priority;
    }

    @JsonSetter("assignee")
    public void setAssignee(String assignee) {
        this.assigneePresent = true;
        this.assignee = assignee;
    }

    public boolean hasAnyField() {
        return titlePresent || descriptionPresent || notePresent || dueAtPresent
                || completedPresent || priorityPresent || assigneePresent;
    }
}
