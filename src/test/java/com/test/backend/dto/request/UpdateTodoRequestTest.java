package com.test.backend.dto.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UpdateTodoRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void distinguishesAbsentNullAndValue() throws Exception {
        UpdateTodoRequest request = objectMapper.readValue(
                """
                {
                  "description": null,
                  "note": "상세 메모",
                  "completed": true,
                  "priority": "HIGH"
                }
                """,
                UpdateTodoRequest.class
        );

        assertThat(request.isTitlePresent()).isFalse();
        assertThat(request.isDescriptionPresent()).isTrue();
        assertThat(request.getDescription()).isNull();
        assertThat(request.isNotePresent()).isTrue();
        assertThat(request.getNote()).isEqualTo("상세 메모");
        assertThat(request.isCompletedPresent()).isTrue();
        assertThat(request.getCompleted()).isTrue();
        assertThat(request.isPriorityPresent()).isTrue();
        assertThat(request.getPriority()).isEqualTo("HIGH");
    }

    @Test
    void acceptsDueAtWithTimezone() throws Exception {
        UpdateTodoRequest request = objectMapper.readValue(
                """
                {"dueAt":"2026-06-10T09:00:00+09:00"}
                """,
                UpdateTodoRequest.class
        );

        assertThat(request.getDueAt().toInstant()).isEqualTo(Instant.parse("2026-06-10T00:00:00Z"));
    }

    @Test
    void subtasksAreEditableByAssignee() throws Exception {
        // 하위 항목만 담긴 수정 요청은 '완료 외 필드'로 취급되면 안 됨(담당자도 체크 가능해야 함).
        UpdateTodoRequest request = objectMapper.readValue(
                """
                {"subtasks":[{"title":"준비물 챙기기","done":true},{"title":"우산","done":false}]}
                """,
                UpdateTodoRequest.class
        );

        assertThat(request.isSubtasksPresent()).isTrue();
        assertThat(request.getSubtasks()).hasSize(2);
        assertThat(request.getSubtasks().get(0).getTitle()).isEqualTo("준비물 챙기기");
        assertThat(request.getSubtasks().get(0).isDone()).isTrue();
        assertThat(request.hasAnyField()).isTrue();
        assertThat(request.hasNonCompletedField()).isFalse();
    }

    @Test
    void rejectsDueAtWithoutTimezone() {
        assertThatThrownBy(() -> objectMapper.readValue(
                """
                {"dueAt":"2026-06-10T09:00:00"}
                """,
                UpdateTodoRequest.class
        )).isInstanceOf(Exception.class);
    }
}
