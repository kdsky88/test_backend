package com.test.backend.service;

import com.test.backend.domain.entity.Todo;
import com.test.backend.domain.entity.User;
import com.test.backend.dto.request.UpdateTodoRequest;
import com.test.backend.dto.response.TodoListResponse;
import com.test.backend.dto.response.TodoResponse;
import com.test.backend.exception.TodoApiException;
import com.test.backend.repository.TodoRepository;
import com.test.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TodoServiceTest {

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private UserRepository userRepository;

    private TodoService todoService;
    private User user;

    @BeforeEach
    void setUp() {
        todoService = new TodoService(todoRepository, userRepository);
        user = new User();
        user.setEmail("user@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);
        lenient().when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    @Test
    void getsTodosWithOneBasedPageAndStableSort() {
        Todo todo = new Todo("할 일", null, null, user);
        ReflectionTestUtils.setField(todo, "id", 10L);
        given(todoRepository.findByUserId(any(), any()))
                .willReturn(new PageImpl<>(List.of(todo)));

        TodoListResponse response = todoService.getTodos(user.getEmail(), 2, 30);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(todoRepository).findByUserId(org.mockito.ArgumentMatchers.eq(1L), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(30);
        assertThat(pageable.getSort().getOrderFor("completed").getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(pageable.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(pageable.getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(response.meta().page()).isEqualTo(2);
        assertThat(response.data()).extracting(TodoResponse::id).containsExactly(10L);
    }

    @Test
    void rejectsPageAndLimitOutsideAllowedRange() {
        assertThatThrownBy(() -> todoService.getTodos(user.getEmail(), 0, 101))
                .isInstanceOfSatisfying(TodoApiException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("INVALID_REQUEST");
                    assertThat(exception.getFields()).containsKeys("page", "limit");
                });

        verifyNoInteractions(todoRepository);
    }

    @Test
    void distinguishesAbsentAndNullFieldsWhenUpdating() {
        OffsetDateTime dueAt = OffsetDateTime.parse("2026-06-10T09:00:00+09:00");
        Todo todo = new Todo("기존 제목", "기존 설명", dueAt, user);
        given(todoRepository.findByIdAndUserId(5L, 1L)).willReturn(Optional.of(todo));
        UpdateTodoRequest request = new UpdateTodoRequest();
        request.setDescription(null);
        request.setDueAt(null);

        TodoResponse response = todoService.updateTodo(user.getEmail(), 5L, request);

        assertThat(response.title()).isEqualTo("기존 제목");
        assertThat(response.description()).isNull();
        assertThat(response.dueAt()).isNull();
    }

    @Test
    void setsAndClearsCompletedAtOnlyWhenCompletedChanges() {
        Todo todo = new Todo("할 일", null, null, user);
        given(todoRepository.findByIdAndUserId(5L, 1L)).willReturn(Optional.of(todo));
        UpdateTodoRequest completeRequest = new UpdateTodoRequest();
        completeRequest.setCompleted(true);

        TodoResponse completed = todoService.updateTodo(user.getEmail(), 5L, completeRequest);

        assertThat(completed.completed()).isTrue();
        assertThat(completed.completedAt()).isNotNull();

        UpdateTodoRequest reopenRequest = new UpdateTodoRequest();
        reopenRequest.setCompleted(false);
        TodoResponse reopened = todoService.updateTodo(user.getEmail(), 5L, reopenRequest);

        assertThat(reopened.completed()).isFalse();
        assertThat(reopened.completedAt()).isNull();
    }

    @Test
    void rejectsNullForNonNullablePatchFields() {
        UpdateTodoRequest request = new UpdateTodoRequest();
        request.setTitle(null);
        request.setCompleted(null);

        assertThatThrownBy(() -> todoService.updateTodo(user.getEmail(), 5L, request))
                .isInstanceOfSatisfying(TodoApiException.class, exception ->
                        assertThat(exception.getFields()).containsKeys("title", "completed"));

        verifyNoInteractions(todoRepository);
    }
}
