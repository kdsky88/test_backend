package com.test.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
public class UpdateTodoRequest {

    private boolean titlePresent;
    private String title;

    private boolean descriptionPresent;
    private String description;

    private boolean notePresent;
    private String note;

    private boolean startAtPresent;
    private OffsetDateTime startAt;

    private boolean dueAtPresent;
    private OffsetDateTime dueAt;

    private boolean completedPresent;
    private Boolean completed;

    private boolean priorityPresent;
    private String priority;

    private boolean assigneePresent;
    private String assignee;

    private boolean recurrencePresent;
    private String recurrence;

    private boolean assignedToEmailPresent;
    private String assignedToEmail;

    private boolean subtasksPresent;
    private List<SubtaskRequest> subtasks;

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

    @JsonSetter("startAt")
    public void setStartAt(OffsetDateTime startAt) {
        this.startAtPresent = true;
        this.startAt = startAt;
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

    @JsonSetter("recurrence")
    public void setRecurrence(String recurrence) {
        this.recurrencePresent = true;
        this.recurrence = recurrence;
    }

    @JsonSetter("assignedToEmail")
    public void setAssignedToEmail(String assignedToEmail) {
        this.assignedToEmailPresent = true;
        this.assignedToEmail = assignedToEmail;
    }

    @JsonSetter("subtasks")
    public void setSubtasks(List<SubtaskRequest> subtasks) {
        this.subtasksPresent = true;
        this.subtasks = subtasks;
    }

    public boolean hasAnyField() {
        return titlePresent || descriptionPresent || notePresent
                || startAtPresent || dueAtPresent
                || completedPresent || priorityPresent || assigneePresent
                || recurrencePresent || assignedToEmailPresent || subtasksPresent;
    }

    /** completed 외 다른 필드 수정이 있는지(담당자는 완료만 가능하므로 판별용).
     *  하위 항목(subtasks)은 공유 시 담당자도 체크할 수 있어야 하므로 일부러 제외. */
    public boolean hasNonCompletedField() {
        return titlePresent || descriptionPresent || notePresent
                || startAtPresent || dueAtPresent
                || priorityPresent || assigneePresent
                || recurrencePresent || assignedToEmailPresent;
    }
}
