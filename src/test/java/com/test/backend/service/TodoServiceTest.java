package com.test.backend.service;

import com.test.backend.domain.entity.Todo;
import com.test.backend.domain.entity.TodoPriority;
import com.test.backend.dto.request.CreateTodoRequest;
import com.test.backend.dto.request.UpdateTodoRequest;
import com.test.backend.dto.response.ApiResponse;
import com.test.backend.dto.response.CalendarResponse;
import com.test.backend.dto.response.TodoListResponse;
import com.test.backend.dto.response.TodoResponse;
import com.test.backend.exception.TodoApiException;
import com.test.backend.repository.TodoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TodoServiceTest {

    private static final String TODO_ID = "550e8400-e29b-41d4-a716-446655440000";

    @Mock
    private TodoRepository todoRepository;

    private TodoService todoService;

    @BeforeEach
    void setUp() {
        todoService = new TodoService(todoRepository);
    }

    @Test
    void getsActiveTodosWithOneBasedPageAndStableSort() {
        Todo todo = new Todo("할 일", null, null);
        PageRequest requestedPage = PageRequest.of(1, 30);
        given(todoRepository.findByCompletedOrderByPriority(anyBoolean(), anyString(), nullable(String.class), any()))
                .willReturn(new PageImpl<>(List.of(todo), requestedPage, 1));

        TodoListResponse response = todoService.getTodos("active", 2, 30, null, null, "priority", false, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(todoRepository).findByCompletedOrderByPriority(
                eq(false),
                anyString(),
                nullable(String.class),
                pageableCaptor.capture()
        );
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(30);
        assertThat(pageable.getSort().isUnsorted()).isTrue();
        assertThat(response.meta().page()).isEqualTo(2);
        assertThat(response.meta().limit()).isEqualTo(30);
    }

    @Test
    void supportsAllAndCompletedStatusFilters() {
        given(todoRepository.findAllByPriorityOrder(anyString(), nullable(String.class), any(Pageable.class))).willReturn(Page.empty());
        given(todoRepository.findByCompletedOrderByPriority(anyBoolean(), anyString(), nullable(String.class), any())).willReturn(Page.empty());

        todoService.getTodos("all", 1, 20, null, null, "priority", false, null);
        todoService.getTodos("completed", 1, 20, null, null, "priority", false, null);

        verify(todoRepository).findAllByPriorityOrder(anyString(), nullable(String.class), any(Pageable.class));
        verify(todoRepository).findByCompletedOrderByPriority(true, "PRIORITY", null, PageRequest.of(0, 20));
    }

    @Test
    void rejectsInvalidStatusWithInvalidFilter() {
        assertThatThrownBy(() -> todoService.getTodos("unknown", 1, 20, null, null, "priority", false, null))
                .isInstanceOfSatisfying(TodoApiException.class, exception ->
                    assertThat(exception.getCode()).isEqualTo("INVALID_FILTER"));
        verifyNoInteractions(todoRepository);
    }

    @Test
    void rejectsInvalidPageAndLimitWithValidationError() {
        assertThatThrownBy(() -> todoService.getTodos("all", 0, 101, null, null, "priority", false, null))
                .isInstanceOfSatisfying(TodoApiException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("VALIDATION_ERROR");
                    assertThat(exception.getFields()).containsKeys("page", "limit");
                });
        verifyNoInteractions(todoRepository);
    }

    @Test
    void createsTodoWithUuidAndOneHundredCharacterTitleLimit() {
        CreateTodoRequest request = new CreateTodoRequest();
        org.springframework.test.util.ReflectionTestUtils.setField(request, "title", " 할 일 ");
        given(todoRepository.save(any(Todo.class))).willAnswer(invocation -> invocation.getArgument(0));

        ApiResponse<TodoResponse> response = todoService.createTodo(request);

        assertThat(response.data().id()).matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
        );
        assertThat(response.data().title()).isEqualTo("할 일");
        assertThat(response.data().priority()).isEqualTo(TodoPriority.MEDIUM);
        assertThat(response.data().tags()).isEmpty();
    }

    @Test
    void createsTodoWithRequestedPriority() {
        CreateTodoRequest request = new CreateTodoRequest();
        org.springframework.test.util.ReflectionTestUtils.setField(request, "title", "긴급 작업");
        request.setPriority("HIGH");
        given(todoRepository.save(any(Todo.class))).willAnswer(invocation -> invocation.getArgument(0));

        TodoResponse response = todoService.createTodo(request).data();

        assertThat(response.priority()).isEqualTo(TodoPriority.HIGH);
    }

    @Test
    void createsTodoWithOptionalNote() {
        CreateTodoRequest request = new CreateTodoRequest();
        org.springframework.test.util.ReflectionTestUtils.setField(request, "title", "메모 있는 할 일");
        org.springframework.test.util.ReflectionTestUtils.setField(request, "note", "상세 설명입니다.");
        given(todoRepository.save(any(Todo.class))).willAnswer(invocation -> invocation.getArgument(0));

        TodoResponse response = todoService.createTodo(request).data();

        assertThat(response.note()).isEqualTo("상세 설명입니다.");
    }

    @Test
    void rejectsInvalidOrNullPriorityOnCreate() {
        CreateTodoRequest invalid = new CreateTodoRequest();
        org.springframework.test.util.ReflectionTestUtils.setField(invalid, "title", "할 일");
        invalid.setPriority("URGENT");

        assertThatThrownBy(() -> todoService.createTodo(invalid))
                .isInstanceOfSatisfying(TodoApiException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("INVALID_PRIORITY"));

        CreateTodoRequest nullPriority = new CreateTodoRequest();
        org.springframework.test.util.ReflectionTestUtils.setField(nullPriority, "title", "할 일");
        nullPriority.setPriority(null);

        assertThatThrownBy(() -> todoService.createTodo(nullPriority))
                .isInstanceOfSatisfying(TodoApiException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("INVALID_PRIORITY"));
        verifyNoInteractions(todoRepository);
    }

    @Test
    void rejectsTitleLongerThanOneHundredCharacters() {
        CreateTodoRequest request = new CreateTodoRequest();
        org.springframework.test.util.ReflectionTestUtils.setField(request, "title", "a".repeat(101));

        assertThatThrownBy(() -> todoService.createTodo(request))
                .isInstanceOfSatisfying(TodoApiException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("VALIDATION_ERROR");
                    assertThat(exception.getFields()).containsKey("title");
                });

        verifyNoInteractions(todoRepository);
    }

    @Test
    void acceptsTitleOfExactlyOneHundredCharsAfterTrim() {
        String paddedTitle = "  " + "가".repeat(100) + "  ";
        CreateTodoRequest request = new CreateTodoRequest();
        org.springframework.test.util.ReflectionTestUtils.setField(request, "title", paddedTitle);
        given(todoRepository.save(any(Todo.class))).willAnswer(inv -> inv.getArgument(0));

        ApiResponse<TodoResponse> response = todoService.createTodo(request);

        assertThat(response.data().title()).isEqualTo("가".repeat(100));
    }

    @Test
    void rejectsTitleOfOneHundredOneCharsAfterTrim() {
        String paddedTitle = "  " + "가".repeat(101) + "  ";
        CreateTodoRequest request = new CreateTodoRequest();
        org.springframework.test.util.ReflectionTestUtils.setField(request, "title", paddedTitle);

        assertThatThrownBy(() -> todoService.createTodo(request))
                .isInstanceOfSatisfying(TodoApiException.class, ex ->
                        assertThat(ex.getFields()).containsKey("title"));
        verifyNoInteractions(todoRepository);
    }

    @Test
    void rejectsNoteLongerThanOneThousandCharacters() {
        CreateTodoRequest request = new CreateTodoRequest();
        org.springframework.test.util.ReflectionTestUtils.setField(request, "title", "할 일");
        org.springframework.test.util.ReflectionTestUtils.setField(request, "note", "a".repeat(1001));

        assertThatThrownBy(() -> todoService.createTodo(request))
                .isInstanceOfSatisfying(TodoApiException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("VALIDATION_ERROR");
                    assertThat(exception.getFields()).containsKey("note");
                });

        verifyNoInteractions(todoRepository);
    }

    @Test
    void createsTodoWithStartAt() {
        OffsetDateTime startAt = OffsetDateTime.parse("2026-06-10T09:00:00+09:00");
        OffsetDateTime dueAt = OffsetDateTime.parse("2026-06-12T09:00:00+09:00");
        CreateTodoRequest request = new CreateTodoRequest();
        org.springframework.test.util.ReflectionTestUtils.setField(request, "title", "시작일 있는 할 일");
        org.springframework.test.util.ReflectionTestUtils.setField(request, "startAt", startAt);
        org.springframework.test.util.ReflectionTestUtils.setField(request, "dueAt", dueAt);
        given(todoRepository.save(any(Todo.class))).willAnswer(inv -> inv.getArgument(0));

        TodoResponse response = todoService.createTodo(request).data();

        assertThat(response.startAt()).isEqualTo(startAt);
        assertThat(response.dueAt()).isEqualTo(dueAt);
    }

    @Test
    void rejectsStartAtAfterDueAtOnCreate() {
        CreateTodoRequest request = new CreateTodoRequest();
        org.springframework.test.util.ReflectionTestUtils.setField(request, "title", "할 일");
        org.springframework.test.util.ReflectionTestUtils.setField(
                request, "startAt", OffsetDateTime.parse("2026-06-12T09:00:00+09:00"));
        org.springframework.test.util.ReflectionTestUtils.setField(
                request, "dueAt", OffsetDateTime.parse("2026-06-10T09:00:00+09:00"));

        assertThatThrownBy(() -> todoService.createTodo(request))
                .isInstanceOfSatisfying(TodoApiException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("VALIDATION_ERROR");
                    assertThat(exception.getFields()).containsKey("startAt");
                });

        verifyNoInteractions(todoRepository);
    }

    @Test
    void rejectsStartAtAfterExistingDueAtOnUpdate() {
        OffsetDateTime dueAt = OffsetDateTime.parse("2026-06-10T09:00:00+09:00");
        Todo todo = new Todo("기존 제목", null, null, dueAt, TodoPriority.MEDIUM);
        given(todoRepository.findById(TODO_ID)).willReturn(Optional.of(todo));
        UpdateTodoRequest request = new UpdateTodoRequest();
        request.setStartAt(OffsetDateTime.parse("2026-06-12T09:00:00+09:00"));

        assertThatThrownBy(() -> todoService.updateTodo(TODO_ID, request))
                .isInstanceOfSatisfying(TodoApiException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("VALIDATION_ERROR");
                    assertThat(exception.getFields()).containsKey("startAt");
                });

        verify(todoRepository, never()).saveAndFlush(any());
    }

    @Test
    void distinguishesAbsentAndNullFieldsWhenUpdating() {
        OffsetDateTime dueAt = OffsetDateTime.parse("2026-06-10T09:00:00+09:00");
        Todo todo = new Todo("기존 제목", "기존 설명", "기존 메모", dueAt, TodoPriority.MEDIUM);
        given(todoRepository.findById(TODO_ID)).willReturn(Optional.of(todo));
        given(todoRepository.saveAndFlush(any())).willAnswer(inv -> inv.getArgument(0));
        UpdateTodoRequest request = new UpdateTodoRequest();
        request.setDescription(null);
        request.setNote(null);
        request.setDueAt(null);

        TodoResponse response = todoService.updateTodo(TODO_ID, request).data();

        assertThat(response.title()).isEqualTo("기존 제목");
        assertThat(response.description()).isNull();
        assertThat(response.note()).isNull();
        assertThat(response.dueAt()).isNull();
    }

    @Test
    void updatesOnlyNote() {
        Todo todo = new Todo("기존 제목", null, "기존 메모", null, TodoPriority.MEDIUM);
        given(todoRepository.findById(TODO_ID)).willReturn(Optional.of(todo));
        given(todoRepository.saveAndFlush(any())).willAnswer(inv -> inv.getArgument(0));
        UpdateTodoRequest request = new UpdateTodoRequest();
        request.setNote("변경된 메모");

        TodoResponse response = todoService.updateTodo(TODO_ID, request).data();

        assertThat(response.title()).isEqualTo("기존 제목");
        assertThat(response.note()).isEqualTo("변경된 메모");
    }

    @Test
    void setsAndClearsCompletedAtWhenCompletedChanges() {
        Todo todo = new Todo("할 일", null, null);
        given(todoRepository.findById(TODO_ID)).willReturn(Optional.of(todo));
        given(todoRepository.saveAndFlush(any())).willAnswer(inv -> inv.getArgument(0));
        UpdateTodoRequest completeRequest = new UpdateTodoRequest();
        completeRequest.setCompleted(true);

        TodoResponse completed = todoService.updateTodo(TODO_ID, completeRequest).data();

        assertThat(completed.completed()).isTrue();
        assertThat(completed.completedAt()).isNotNull();

        UpdateTodoRequest reopenRequest = new UpdateTodoRequest();
        reopenRequest.setCompleted(false);
        TodoResponse reopened = todoService.updateTodo(TODO_ID, reopenRequest).data();

        assertThat(reopened.completed()).isFalse();
        assertThat(reopened.completedAt()).isNull();
    }

    @Test
    void updatesPriorityAndKeepsItWhenOmitted() {
        Todo todo = new Todo("할 일", null, null, TodoPriority.LOW);
        given(todoRepository.findById(TODO_ID)).willReturn(Optional.of(todo));
        given(todoRepository.saveAndFlush(any())).willAnswer(inv -> inv.getArgument(0));

        UpdateTodoRequest updatePriority = new UpdateTodoRequest();
        updatePriority.setPriority("HIGH");
        assertThat(todoService.updateTodo(TODO_ID, updatePriority).data().priority())
                .isEqualTo(TodoPriority.HIGH);

        UpdateTodoRequest updateTitleOnly = new UpdateTodoRequest();
        updateTitleOnly.setTitle("제목 변경");
        assertThat(todoService.updateTodo(TODO_ID, updateTitleOnly).data().priority())
                .isEqualTo(TodoPriority.HIGH);
    }

    @Test
    void rejectsNullPriorityWithoutChangingTodo() {
        Todo todo = new Todo("할 일", null, null, TodoPriority.LOW);
        UpdateTodoRequest request = new UpdateTodoRequest();
        request.setPriority(null);

        assertThatThrownBy(() -> todoService.updateTodo(TODO_ID, request))
                .isInstanceOfSatisfying(TodoApiException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("INVALID_PRIORITY"));
        assertThat(todo.getPriority()).isEqualTo(TodoPriority.LOW);
        verifyNoInteractions(todoRepository);
    }

    @Test
    void deletesExistingTodo() {
        Todo todo = new Todo("할 일", null, null);
        given(todoRepository.findById(TODO_ID)).willReturn(Optional.of(todo));

        todoService.deleteTodo(TODO_ID);

        verify(todoRepository).delete(todo);
    }

    @Test
    void createsTodoWithAssignee() {
        CreateTodoRequest request = new CreateTodoRequest();
        org.springframework.test.util.ReflectionTestUtils.setField(request, "title", "담당자 있는 할 일");
        org.springframework.test.util.ReflectionTestUtils.setField(request, "assignee", " 철수 ");
        given(todoRepository.save(any(Todo.class))).willAnswer(invocation -> invocation.getArgument(0));

        TodoResponse response = todoService.createTodo(request).data();

        assertThat(response.assignee()).isEqualTo("철수");
    }

    @Test
    void createsTodoWithBlankAssigneeStoresNull() {
        CreateTodoRequest request = new CreateTodoRequest();
        org.springframework.test.util.ReflectionTestUtils.setField(request, "title", "담당자 없는 할 일");
        org.springframework.test.util.ReflectionTestUtils.setField(request, "assignee", "   ");
        given(todoRepository.save(any(Todo.class))).willAnswer(invocation -> invocation.getArgument(0));

        TodoResponse response = todoService.createTodo(request).data();

        assertThat(response.assignee()).isNull();
    }

    @Test
    void rejectsAssigneeLongerThan50Chars() {
        CreateTodoRequest request = new CreateTodoRequest();
        org.springframework.test.util.ReflectionTestUtils.setField(request, "title", "할 일");
        org.springframework.test.util.ReflectionTestUtils.setField(request, "assignee", "a".repeat(51));

        assertThatThrownBy(() -> todoService.createTodo(request))
                .isInstanceOfSatisfying(TodoApiException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("VALIDATION_ERROR");
                    assertThat(exception.getFields()).containsKey("assignee");
                });
        verifyNoInteractions(todoRepository);
    }

    @Test
    void rejectsReservedTokenAsAssigneeName() {
        CreateTodoRequest request = new CreateTodoRequest();
        org.springframework.test.util.ReflectionTestUtils.setField(request, "title", "할 일");
        org.springframework.test.util.ReflectionTestUtils.setField(request, "assignee", "@unassigned");

        assertThatThrownBy(() -> todoService.createTodo(request))
                .isInstanceOfSatisfying(TodoApiException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("VALIDATION_ERROR");
                    assertThat(exception.getFields()).containsKey("assignee");
                });
        verifyNoInteractions(todoRepository);
    }

    @Test
    void filtersByAssignee() {
        Todo todo = new Todo("할 일", null, null, null, TodoPriority.MEDIUM, "철수");
        PageRequest requestedPage = PageRequest.of(0, 20);
        given(todoRepository.findByAssigneeAndCompleted(
                isNull(),
                eq("철수"),
                anyString(),
                nullable(String.class),
                any(Pageable.class)
        )).willReturn(new PageImpl<>(List.of(todo), requestedPage, 1));

        TodoListResponse response = todoService.getTodos("all", 1, 20, "철수", null, "priority", false, null);

        verify(todoRepository).findByAssigneeAndCompleted(
                isNull(),
                eq("철수"),
                anyString(),
                nullable(String.class),
                any(Pageable.class)
        );
        assertThat(response.meta().total()).isEqualTo(1);
    }

    @Test
    void filtersByUnassigned() {
        PageRequest requestedPage = PageRequest.of(0, 20);
        given(todoRepository.findByUnassignedAndCompleted(
                isNull(),
                anyString(),
                nullable(String.class),
                any(Pageable.class)
        )).willReturn(new PageImpl<>(List.of(), requestedPage, 0));

        todoService.getTodos("all", 1, 20, "@unassigned", null, "priority", false, null);

        verify(todoRepository).findByUnassignedAndCompleted(
                isNull(),
                anyString(),
                nullable(String.class),
                any(Pageable.class)
        );
    }

    @Test
    void updatesAssignee() {
        Todo todo = new Todo("할 일", null, null, null, TodoPriority.MEDIUM, "철수");
        given(todoRepository.findById(TODO_ID)).willReturn(Optional.of(todo));
        given(todoRepository.saveAndFlush(any())).willAnswer(inv -> inv.getArgument(0));
        UpdateTodoRequest request = new UpdateTodoRequest();
        request.setAssignee("영희");

        TodoResponse response = todoService.updateTodo(TODO_ID, request).data();

        assertThat(response.assignee()).isEqualTo("영희");
    }

    @Test
    void clearsAssignee() {
        Todo todo = new Todo("할 일", null, null, null, TodoPriority.MEDIUM, "철수");
        given(todoRepository.findById(TODO_ID)).willReturn(Optional.of(todo));
        given(todoRepository.saveAndFlush(any())).willAnswer(inv -> inv.getArgument(0));
        UpdateTodoRequest request = new UpdateTodoRequest();
        request.setAssignee(null);

        TodoResponse response = todoService.updateTodo(TODO_ID, request).data();

        assertThat(response.assignee()).isNull();
    }

    @Test
    void keepsAssigneeWhenNotInUpdateRequest() {
        Todo todo = new Todo("할 일", null, null, null, TodoPriority.MEDIUM, "철수");
        given(todoRepository.findById(TODO_ID)).willReturn(Optional.of(todo));
        given(todoRepository.saveAndFlush(any())).willAnswer(inv -> inv.getArgument(0));
        UpdateTodoRequest request = new UpdateTodoRequest();
        request.setTitle("제목 변경");

        TodoResponse response = todoService.updateTodo(TODO_ID, request).data();

        assertThat(response.assignee()).isEqualTo("철수");
    }

    @Test
    void getsAssignees() {
        given(todoRepository.findDistinctAssignees()).willReturn(List.of("영희", "철수"));

        ApiResponse<List<String>> response = todoService.getAssignees();

        assertThat(response.data()).containsExactly("영희", "철수");
    }

    @Test
    void groupsTodosByKstDate() {
        given(todoRepository.findByDateRangeOverlap(any(), any())).willReturn(List.of(
            new Todo("아침", null, OffsetDateTime.parse("2026-06-15T00:00:00+09:00")),
            new Todo("저녁", null, OffsetDateTime.parse("2026-06-15T21:00:00+09:00"))
        ));

        CalendarResponse res = todoService.getCalendar(2026, 6);

        assertThat(res.data()).containsKey("2026-06-15");
        assertThat(res.data().get("2026-06-15")).hasSize(2);
    }

    @Test
    void rejectsInvalidMonthInCalendar() {
        assertThatThrownBy(() -> todoService.getCalendar(2026, 13))
            .isInstanceOfSatisfying(TodoApiException.class,
                ex -> assertThat(ex.getCode()).isEqualTo("VALIDATION_ERROR"));
        verifyNoInteractions(todoRepository);
    }

    // ── Tag tests ─────────────────────────────────────────────────────────────

    @Test
    void addsTagToTodo() {
        Todo todo = new Todo("할 일", null, null);
        given(todoRepository.findById(TODO_ID)).willReturn(Optional.of(todo));
        given(todoRepository.saveAndFlush(any())).willAnswer(inv -> inv.getArgument(0));

        ApiResponse<TodoResponse> response = todoService.addTag(TODO_ID, "업무");

        assertThat(response.data().tags()).containsExactly("업무");
    }

    @Test
    void addTagIsIdempotentWhenAlreadyPresent() {
        Todo todo = new Todo("할 일", null, null);
        todo.addTag("업무");
        given(todoRepository.findById(TODO_ID)).willReturn(Optional.of(todo));

        ApiResponse<TodoResponse> response = todoService.addTag(TODO_ID, "업무");

        assertThat(response.data().tags()).containsExactly("업무");
        verify(todoRepository, never()).saveAndFlush(any());
    }

    @Test
    void addTagRejectsBlankValue() {
        assertThatThrownBy(() -> todoService.addTag(TODO_ID, "   "))
                .isInstanceOfSatisfying(TodoApiException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo("VALIDATION_ERROR");
                    assertThat(ex.getFields()).containsKey("tag");
                });
        verifyNoInteractions(todoRepository);
    }

    @Test
    void addTagRejectsTooLongValue() {
        assertThatThrownBy(() -> todoService.addTag(TODO_ID, "a".repeat(21)))
                .isInstanceOfSatisfying(TodoApiException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo("VALIDATION_ERROR");
                    assertThat(ex.getFields()).containsKey("tag");
                });
        verifyNoInteractions(todoRepository);
    }

    @Test
    void addTagReturnsNotFoundForMissingTodo() {
        given(todoRepository.findById(TODO_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> todoService.addTag(TODO_ID, "업무"))
                .isInstanceOfSatisfying(TodoApiException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo("TODO_NOT_FOUND"));
    }

    @Test
    void addTagExceedsLimit() {
        Todo todo = new Todo("할 일", null, null);
        for (int i = 1; i <= 10; i++) {
            todo.addTag("태그" + i);
        }
        given(todoRepository.findById(TODO_ID)).willReturn(Optional.of(todo));

        assertThatThrownBy(() -> todoService.addTag(TODO_ID, "초과태그"))
                .isInstanceOfSatisfying(TodoApiException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo("TAG_LIMIT_EXCEEDED"));
    }

    @Test
    void removesTagFromTodo() {
        Todo todo = new Todo("할 일", null, null);
        todo.addTag("업무");
        given(todoRepository.findById(TODO_ID)).willReturn(Optional.of(todo));
        given(todoRepository.saveAndFlush(any())).willAnswer(inv -> inv.getArgument(0));

        ApiResponse<TodoResponse> response = todoService.removeTag(TODO_ID, "업무");

        assertThat(response.data().tags()).isEmpty();
    }

    @Test
    void removeTagIsIdempotentWhenNotPresent() {
        Todo todo = new Todo("할 일", null, null);
        given(todoRepository.findById(TODO_ID)).willReturn(Optional.of(todo));
        given(todoRepository.saveAndFlush(any())).willAnswer(inv -> inv.getArgument(0));

        ApiResponse<TodoResponse> response = todoService.removeTag(TODO_ID, "없는태그");

        assertThat(response.data().tags()).isEmpty();
    }

    @Test
    void removeTagRejectsBlankValue() {
        assertThatThrownBy(() -> todoService.removeTag(TODO_ID, "   "))
                .isInstanceOfSatisfying(TodoApiException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo("VALIDATION_ERROR");
                    assertThat(ex.getFields()).containsKey("tag");
                });
        verifyNoInteractions(todoRepository);
    }

    @Test
    void removeTagReturnsNotFoundForMissingTodo() {
        given(todoRepository.findById(TODO_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> todoService.removeTag(TODO_ID, "업무"))
                .isInstanceOfSatisfying(TodoApiException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo("TODO_NOT_FOUND"));
    }

    @Test
    void getTagsReturnsDistinctSortedList() {
        given(todoRepository.findDistinctTags()).willReturn(List.of("개인", "긴급", "업무"));

        ApiResponse<List<String>> response = todoService.getTags();

        assertThat(response.data()).containsExactly("개인", "긴급", "업무");
    }

    @Test
    void createsTodoWithTags() {
        CreateTodoRequest request = new CreateTodoRequest();
        org.springframework.test.util.ReflectionTestUtils.setField(request, "title", "태그 있는 할 일");
        org.springframework.test.util.ReflectionTestUtils.setField(request, "tags", List.of("업무", "긴급"));
        given(todoRepository.save(any(Todo.class))).willAnswer(inv -> inv.getArgument(0));

        TodoResponse response = todoService.createTodo(request).data();

        assertThat(response.tags()).containsExactlyInAnyOrder("업무", "긴급");
    }

    @Test
    void createsTodoRejectsTooLongTag() {
        CreateTodoRequest request = new CreateTodoRequest();
        org.springframework.test.util.ReflectionTestUtils.setField(request, "title", "할 일");
        org.springframework.test.util.ReflectionTestUtils.setField(request, "tags", List.of("a".repeat(21)));

        assertThatThrownBy(() -> todoService.createTodo(request))
                .isInstanceOfSatisfying(TodoApiException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo("VALIDATION_ERROR");
                    assertThat(ex.getFields()).containsKey("tags");
                });
        verifyNoInteractions(todoRepository);
    }

    @Test
    void filtersTodosByTag() {
        Todo todo = new Todo("할 일", null, null);
        todo.addTag("업무");
        PageRequest requestedPage = PageRequest.of(0, 20);
        given(todoRepository.findByTagAndCompleted(
                eq("업무"),
                isNull(),
                anyString(),
                nullable(String.class),
                any(Pageable.class)
        )).willReturn(new PageImpl<>(List.of(todo), requestedPage, 1));

        TodoListResponse response = todoService.getTodos("all", 1, 20, null, "업무", "priority", false, null);

        verify(todoRepository).findByTagAndCompleted(eq("업무"), isNull(), anyString(), nullable(String.class), any(Pageable.class));
        assertThat(response.meta().total()).isEqualTo(1);
        assertThat(response.data().get(0).tags()).containsExactly("업무");
    }

    @Test
    void filtersTodosByTagAndStatus() {
        PageRequest requestedPage = PageRequest.of(0, 20);
        given(todoRepository.findByTagAndCompleted(
                eq("긴급"),
                eq(false),
                anyString(),
                nullable(String.class),
                any(Pageable.class)
        )).willReturn(new PageImpl<>(List.of(), requestedPage, 0));

        todoService.getTodos("active", 1, 20, null, "긴급", "priority", false, null);

        verify(todoRepository).findByTagAndCompleted(eq("긴급"), eq(false), anyString(), nullable(String.class), any(Pageable.class));
    }
}
