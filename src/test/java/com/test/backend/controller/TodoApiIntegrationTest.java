package com.test.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.backend.repository.TodoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TodoApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TodoRepository todoRepository;

    @BeforeEach
    void cleanUp() {
        todoRepository.deleteAll();
    }

    @Test
    void supportsPublicCrudFlow() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/todos")
                        .contentType("application/json")
                        .content("""
                                {
                                  "title":"통합 테스트",
                                  "note":"통합 테스트 메모",
                                  "priority":"HIGH",
                                  "dueAt":"2026-06-10T09:00:00+09:00"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isString())
                .andExpect(jsonPath("$.data.note").value("통합 테스트 메모"))
                .andExpect(jsonPath("$.data.completed").value(false))
                .andExpect(jsonPath("$.data.priority").value("HIGH"))
                .andReturn();

        JsonNode createBody = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String id = createBody.path("data").path("id").asText();

        mockMvc.perform(patch("/todos/{id}", id)
                        .contentType("application/json")
                        .content("""
                                {"completed":true,"note":"완료 메모"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.note").value("완료 메모"))
                .andExpect(jsonPath("$.data.completed").value(true))
                .andExpect(jsonPath("$.data.completedAt").isString());

        mockMvc.perform(get("/todos").param("status", "completed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(id))
                .andExpect(jsonPath("$.data[0].note").value("완료 메모"))
                .andExpect(jsonPath("$.meta.total").value(1));

        mockMvc.perform(delete("/todos/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(patch("/todos/{id}", id)
                        .contentType("application/json")
                        .content("""
                                {"completed":false}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("TODO_NOT_FOUND"));
    }

    @Test
    void appliesDefaultPrioritySortAndRejectsInvalidPriority() throws Exception {
        createTodo("낮음", "LOW");
        createTodo("높음", "HIGH");
        mockMvc.perform(post("/todos")
                        .contentType("application/json")
                        .content("""
                                {"title":"보통"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.priority").value("MEDIUM"));

        mockMvc.perform(get("/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].priority").value("HIGH"))
                .andExpect(jsonPath("$.data[1].priority").value("MEDIUM"))
                .andExpect(jsonPath("$.data[2].priority").value("LOW"));

        mockMvc.perform(post("/todos")
                        .contentType("application/json")
                        .content("""
                                {"title":"잘못된 값","priority":null}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_PRIORITY"));
    }

    @Test
    void rejectsNoteLongerThanLimit() throws Exception {
        mockMvc.perform(post("/todos")
                        .contentType("application/json")
                        .content("""
                                {"title":"메모 길이 초과","note":"%s"}
                                """.formatted("a".repeat(1001))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields.note").exists());
    }

    private void createTodo(String title, String priority) throws Exception {
        mockMvc.perform(post("/todos")
                        .contentType("application/json")
                        .content("""
                                {"title":"%s","priority":"%s"}
                                """.formatted(title, priority)))
                .andExpect(status().isCreated());
    }

    private void createTodoWithDue(String title, String priority, String dueAt) throws Exception {
        String body = dueAt == null
                ? "{\"title\":\"" + title + "\",\"priority\":\"" + priority + "\"}"
                : "{\"title\":\"" + title + "\",\"priority\":\"" + priority
                        + "\",\"dueAt\":\"" + dueAt + "\"}";
        mockMvc.perform(post("/todos")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void sortsListByPriorityDueAtAndCreatedAt() throws Exception {
        createTodoWithDue("작업 A", "LOW", "2026-06-01T09:00:00+09:00");
        createTodoWithDue("작업 B", "HIGH", "2026-06-03T09:00:00+09:00");
        createTodoWithDue("작업 C", "MEDIUM", null);

        // 기본(priority): 높음 → 보통 → 낮음
        mockMvc.perform(get("/todos").param("sort", "priority"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("작업 B"))
                .andExpect(jsonPath("$.data[1].title").value("작업 C"))
                .andExpect(jsonPath("$.data[2].title").value("작업 A"));

        // 마감일순: 빠른 날짜 먼저, 마감일 없는 건 마지막
        mockMvc.perform(get("/todos").param("sort", "dueAt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("작업 A"))
                .andExpect(jsonPath("$.data[1].title").value("작업 B"))
                .andExpect(jsonPath("$.data[2].title").value("작업 C"));

        // 등록순(createdAt) 분기도 정상 실행 (동일 시각 tie라 순서는 단정하지 않음)
        mockMvc.perform(get("/todos").param("sort", "createdAt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3));
    }

    @Test
    void calendarSpreadsTodoAcrossStartToDue() throws Exception {
        mockMvc.perform(post("/todos")
                        .contentType("application/json")
                        .content("""
                                {"title":"여행","priority":"MEDIUM",
                                 "startAt":"2026-06-10T09:00:00+09:00",
                                 "dueAt":"2026-06-12T18:00:00+09:00"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/todos/calendar").param("year", "2026").param("month", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data['2026-06-10'][0].title").value("여행"))
                .andExpect(jsonPath("$.data['2026-06-11'][0].title").value("여행"))
                .andExpect(jsonPath("$.data['2026-06-12'][0].title").value("여행"))
                .andExpect(jsonPath("$.data['2026-06-13']").doesNotExist());
    }

    @Test
    void completedSortsToBottomAndHideCompletedExcludesThem() throws Exception {
        createTodo("활성-낮음", "LOW");
        String doneId = createAndReturnId("완료-높음", "HIGH");
        mockMvc.perform(patch("/todos/" + doneId)
                        .contentType("application/json")
                        .content("{\"completed\":true}"))
                .andExpect(status().isOk());
        createTodo("활성-높음", "HIGH");

        // 전체: 완료 항목은 우선순위와 무관하게 맨 아래
        mockMvc.perform(get("/todos").param("status", "all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[2].title").value("완료-높음"))
                .andExpect(jsonPath("$.data[2].completed").value(true));

        // 완료 숨기기: 완료 항목 제외
        mockMvc.perform(get("/todos").param("status", "all").param("hideCompleted", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.total").value(2));
    }

    private String createAndReturnId(String title, String priority) throws Exception {
        MvcResult result = mockMvc.perform(post("/todos")
                        .contentType("application/json")
                        .content("{\"title\":\"" + title + "\",\"priority\":\"" + priority + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asText();
    }

    @Test
    void completingRecurringTodoSpawnsNextOccurrence() throws Exception {
        MvcResult created = mockMvc.perform(post("/todos")
                        .contentType("application/json")
                        .content("""
                                {"title":"운동","priority":"MEDIUM",
                                 "dueAt":"2026-06-10T09:00:00+09:00","recurrence":"DAILY"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String id = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // 완료 처리 → 다음 날짜의 반복 항목 생성
        mockMvc.perform(patch("/todos/" + id)
                        .contentType("application/json")
                        .content("{\"completed\":true}"))
                .andExpect(status().isOk());

        // 미완료(새로 생성된 다음 주기) 1건: 마감일 +1일, recurrence 유지
        mockMvc.perform(get("/todos").param("status", "active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.total").value(1))
                .andExpect(jsonPath("$.data[0].title").value("운동"))
                .andExpect(jsonPath("$.data[0].completed").value(false))
                .andExpect(jsonPath("$.data[0].recurrence").value("DAILY"))
                .andExpect(jsonPath("$.data[0].dueAt", startsWith("2026-06-11")));

        // 완료된 원본 1건
        mockMvc.perform(get("/todos").param("status", "completed"))
                .andExpect(jsonPath("$.meta.total").value(1));
    }

    @Test
    void rejectsRecurrenceWithoutDueAt() throws Exception {
        mockMvc.perform(post("/todos")
                        .contentType("application/json")
                        .content("{\"title\":\"반복\",\"priority\":\"LOW\",\"recurrence\":\"WEEKLY\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields.recurrence").exists());
    }

    @Test
    void searchFiltersByTitleCaseInsensitive() throws Exception {
        createTodo("Grocery shopping", "LOW");
        createTodo("병원 예약", "HIGH");
        createTodo("운동", "MEDIUM");

        mockMvc.perform(get("/todos").param("search", "grocery"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.total").value(1))
                .andExpect(jsonPath("$.data[0].title").value("Grocery shopping"));

        mockMvc.perform(get("/todos").param("search", "병원"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.total").value(1));

        mockMvc.perform(get("/todos").param("search", "zzz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.total").value(0));
    }

    @Test
    void statsReturnsCounts() throws Exception {
        createTodo("활성", "LOW");
        String doneId = createAndReturnId("완료", "HIGH");
        mockMvc.perform(patch("/todos/" + doneId)
                        .contentType("application/json")
                        .content("{\"completed\":true}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/todos")
                        .contentType("application/json")
                        .content("{\"title\":\"지연\",\"priority\":\"MEDIUM\",\"dueAt\":\"2020-01-01T09:00:00+09:00\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/todos/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.completed").value(1))
                .andExpect(jsonPath("$.data.active").value(2))
                .andExpect(jsonPath("$.data.overdue").value(1))
                .andExpect(jsonPath("$.data.dueToday").value(0));
    }
}
