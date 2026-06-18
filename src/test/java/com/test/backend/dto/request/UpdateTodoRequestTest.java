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
    void rejectsDueAtWithoutTimezone() {
        assertThatThrownBy(() -> objectMapper.readValue(
                """
                {"dueAt":"2026-06-10T09:00:00"}
                """,
                UpdateTodoRequest.class
        )).isInstanceOf(Exception.class);
    }
}
