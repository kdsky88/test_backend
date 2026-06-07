package com.test.backend.service;

import com.test.backend.domain.entity.Todo;
import com.test.backend.dto.request.CreateTodoRequest;
import com.test.backend.dto.request.UpdateTodoRequest;
import com.test.backend.dto.response.ApiResponse;
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
import org.springframework.data.domain.Sort;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
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
        given(todoRepository.findByCompleted(anyBoolean(), any()))
                .willReturn(new PageImpl<>(List.of(todo), requestedPage, 1));

        TodoListResponse response = todoService.getTodos("active", 2, 30);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(todoRepository).findByCompleted(org.mockito.ArgumentMatchers.eq(false), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(30);
        assertThat(pageable.getSort().getOrderFor("completed").getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(pageable.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(pageable.getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(response.meta().page()).isEqualTo(2);
        assertThat(response.meta().limit()).isEqualTo(30);
    }

    @Test
    void supportsAllAndCompletedStatusFilters() {
        given(todoRepository.findAll(any(Pageable.class))).willReturn(Page.empty());
        given(todoRepository.findByCompleted(anyBoolean(), any())).willReturn(Page.empty());

        todoService.getTodos("all", 1, 20);
        todoService.getTodos("completed", 1, 20);

        verify(todoRepository).findAll(any(Pageable.class));
        verify(todoRepository).findByCompleted(true, PageRequest.of(
                0,
                20,
                Sort.by(
                        Sort.Order.asc("completed"),
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        ));
    }

    @Test
    void rejectsInvalidStatusWithInvalidFilter() {
        assertThatThrownBy(() -> todoService.getTodos("unknown", 1, 20))
                .isInstanceOfSatisfying(TodoApiException.class, exception ->
                    assertThat(exception.getCode()).isEqualTo("INVALID_FILTER"));
        verifyNoInteractions(todoRepository);
    }

    @Test
    void rejectsInvalidPageAndLimitWithValidationError() {
        assertThatThrownBy(() -> todoService.getTodos("all", 0, 101))
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
    void distinguishesAbsentAndNullFieldsWhenUpdating() {
        OffsetDateTime dueAt = OffsetDateTime.parse("2026-06-10T09:00:00+09:00");
        Todo todo = new Todo("기존 제목", "기존 설명", dueAt);
        given(todoRepository.findById(TODO_ID)).willReturn(Optional.of(todo));
        given(todoRepository.saveAndFlush(any())).willAnswer(inv -> inv.getArgument(0));
        UpdateTodoRequest request = new UpdateTodoRequest();
        request.setDescription(null);
        request.setDueAt(null);

        TodoResponse response = todoService.updateTodo(TODO_ID, request).data();

        assertThat(response.title()).isEqualTo("기존 제목");
        assertThat(response.description()).isNull();
        assertThat(response.dueAt()).isNull();
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
    void deletesExistingTodo() {
        Todo todo = new Todo("할 일", null, null);
        given(todoRepository.findById(TODO_ID)).willReturn(Optional.of(todo));

        todoService.deleteTodo(TODO_ID);

        verify(todoRepository).delete(todo);
    }
}
