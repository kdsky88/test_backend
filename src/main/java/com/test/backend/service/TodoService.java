package com.test.backend.service;

import com.test.backend.domain.entity.Todo;
import com.test.backend.domain.entity.TodoPriority;
import com.test.backend.dto.request.CreateTodoRequest;
import com.test.backend.dto.request.UpdateTodoRequest;
import com.test.backend.dto.response.ApiResponse;
import com.test.backend.dto.response.TodoListResponse;
import com.test.backend.dto.response.TodoResponse;
import com.test.backend.exception.TodoApiException;
import com.test.backend.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.test.backend.dto.response.CalendarResponse;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TodoService {

    private static final String UNASSIGNED_TOKEN = "@unassigned";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int TAG_MAX_COUNT = 10;
    private static final int TAG_MAX_LENGTH = 20;

    private final TodoRepository todoRepository;

    @Transactional(readOnly = true)
    public TodoListResponse getTodos(String status, int page, int limit, String assignee, String tag, String sort) {
        TodoStatus todoStatus = validateListRequest(status, page, limit);
        String normalizedAssignee = normalizeString(assignee);
        String normalizedTag = normalizeString(tag);
        String sortToken = normalizeSort(sort);
        PageRequest pageable = PageRequest.of(page - 1, limit);

        Boolean completed = switch (todoStatus) {
            case ALL -> null;
            case ACTIVE -> false;
            case COMPLETED -> true;
        };

        Page<Todo> todoPage;
        if (normalizedTag != null) {
            if (normalizedAssignee == null) {
                todoPage = todoRepository.findByTagAndCompleted(normalizedTag, completed, sortToken, pageable);
            } else if (UNASSIGNED_TOKEN.equals(normalizedAssignee)) {
                todoPage = todoRepository.findByTagAndUnassignedAndCompleted(normalizedTag, completed, sortToken, pageable);
            } else {
                todoPage = todoRepository.findByTagAndAssigneeAndCompleted(normalizedTag, completed, normalizedAssignee, sortToken, pageable);
            }
        } else {
            if (normalizedAssignee == null) {
                todoPage = switch (todoStatus) {
                    case ALL -> todoRepository.findAllByPriorityOrder(sortToken, pageable);
                    case ACTIVE -> todoRepository.findByCompletedOrderByPriority(false, sortToken, pageable);
                    case COMPLETED -> todoRepository.findByCompletedOrderByPriority(true, sortToken, pageable);
                };
            } else if (UNASSIGNED_TOKEN.equals(normalizedAssignee)) {
                todoPage = todoRepository.findByUnassignedAndCompleted(completed, sortToken, pageable);
            } else {
                todoPage = todoRepository.findByAssigneeAndCompleted(completed, normalizedAssignee, sortToken, pageable);
            }
        }

        Page<TodoResponse> todos = todoPage.map(TodoResponse::new);
        return TodoListResponse.from(todos, page);
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<String>> getAssignees() {
        return new ApiResponse<>(todoRepository.findDistinctAssignees());
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<String>> getTags() {
        return new ApiResponse<>(todoRepository.findDistinctTags());
    }

    @Transactional
    public ApiResponse<TodoResponse> createTodo(CreateTodoRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        validateTitle(request.getTitle(), fields);
        validateDescription(request.getDescription(), fields);
        validateNote(request.getNote(), fields);
        validateStartNotAfterDue(request.getStartAt(), request.getDueAt(), fields);
        TodoPriority priority = resolveCreatePriority(request, fields);
        String assignee = resolveAssignee(request.getAssignee(), fields);
        List<String> tags = resolveTags(request.getTags(), fields);
        if (!fields.isEmpty()) {
            if (fields.containsKey("priority")) {
                throw invalidPriority();
            }
            throw validationError(fields);
        }

        Todo todo = new Todo(
                request.getTitle().strip(),
                request.getDescription(),
                request.getNote(),
                request.getDueAt(),
                priority,
                assignee
        );
        todo.updateStartAt(request.getStartAt());
        if (tags != null) {
            tags.forEach(todo::addTag);
        }
        return new ApiResponse<>(new TodoResponse(todoRepository.save(todo)));
    }

    @Transactional
    public ApiResponse<TodoResponse> addTag(String id, String tag) {
        Map<String, String> fields = new LinkedHashMap<>();
        String trimmedTag = tag == null ? null : tag.strip();
        if (trimmedTag == null || trimmedTag.isBlank()) {
            fields.put("tag", "tag는 비어 있을 수 없습니다.");
        } else if (trimmedTag.length() > TAG_MAX_LENGTH) {
            fields.put("tag", "tag는 " + TAG_MAX_LENGTH + "자를 초과할 수 없습니다.");
        }
        if (!fields.isEmpty()) {
            throw validationError(fields);
        }

        Todo todo = findTodo(id);
        if (todo.hasTag(trimmedTag)) {
            return new ApiResponse<>(new TodoResponse(todo));
        }
        if (todo.getTags().size() >= TAG_MAX_COUNT) {
            throw new TodoApiException(HttpStatus.BAD_REQUEST, "TAG_LIMIT_EXCEEDED",
                    "태그는 최대 " + TAG_MAX_COUNT + "개까지 추가할 수 있습니다.");
        }
        todo.addTag(trimmedTag);
        return new ApiResponse<>(new TodoResponse(todoRepository.saveAndFlush(todo)));
    }

    @Transactional
    public ApiResponse<TodoResponse> removeTag(String id, String tag) {
        Map<String, String> fields = new LinkedHashMap<>();
        String trimmedTag = tag == null ? null : tag.strip();
        if (trimmedTag == null || trimmedTag.isBlank()) {
            fields.put("tag", "tag 파라미터가 필요합니다.");
        }
        if (!fields.isEmpty()) {
            throw validationError(fields);
        }

        Todo todo = findTodo(id);
        todo.removeTag(trimmedTag);
        return new ApiResponse<>(new TodoResponse(todoRepository.saveAndFlush(todo)));
    }

    @Transactional
    public ApiResponse<TodoResponse> updateTodo(String id, UpdateTodoRequest request) {
        validateUpdateRequest(request);
        Todo todo = findTodo(id);

        OffsetDateTime effectiveStart = request.isStartAtPresent() ? request.getStartAt() : todo.getStartAt();
        OffsetDateTime effectiveDue = request.isDueAtPresent() ? request.getDueAt() : todo.getDueAt();
        Map<String, String> dateFields = new LinkedHashMap<>();
        validateStartNotAfterDue(effectiveStart, effectiveDue, dateFields);
        if (!dateFields.isEmpty()) {
            throw validationError(dateFields);
        }

        if (request.isTitlePresent()) {
            todo.updateTitle(request.getTitle().strip());
        }
        if (request.isDescriptionPresent()) {
            todo.updateDescription(request.getDescription());
        }
        if (request.isNotePresent()) {
            todo.updateNote(request.getNote());
        }
        if (request.isStartAtPresent()) {
            todo.updateStartAt(request.getStartAt());
        }
        if (request.isDueAtPresent()) {
            todo.updateDueAt(request.getDueAt());
        }
        if (request.isCompletedPresent()) {
            todo.updateCompleted(request.getCompleted(), Instant.now());
        }
        if (request.isPriorityPresent()) {
            todo.updatePriority(parsePriority(request.getPriority()));
        }
        if (request.isAssigneePresent()) {
            todo.updateAssignee(normalizeAssigneeForUpdate(request.getAssignee()));
        }
        // saveAndFlush로 @LastModifiedDate가 DB에 반영된 값을 응답에 포함
        return new ApiResponse<>(new TodoResponse(todoRepository.saveAndFlush(todo)));
    }

    @Transactional
    public void deleteTodo(String id) {
        todoRepository.delete(findTodo(id));
    }

    @Transactional(readOnly = true)
    public CalendarResponse getCalendar(int year, int month) {
        Map<String, String> err = new LinkedHashMap<>();
        if (year < 1) err.put("year", "유효한 연도를 입력해 주세요.");
        if (month < 1 || month > 12) err.put("month", "month는 1~12이어야 합니다.");
        if (!err.isEmpty()) throw validationError(err);

        YearMonth ym = YearMonth.of(year, month);
        var start = ym.atDay(1).atStartOfDay(KST).toOffsetDateTime();
        var end   = ym.plusMonths(1).atDay(1).atStartOfDay(KST).toOffsetDateTime();

        Map<String, List<TodoResponse>> grouped = todoRepository
            .findByDueAtBetween(start, end)
            .stream()
            .collect(Collectors.groupingBy(
                t -> t.getDueAt().atZoneSameInstant(KST).toLocalDate().toString(),
                LinkedHashMap::new,
                Collectors.mapping(TodoResponse::new, Collectors.toList())
            ));
        return new CalendarResponse(grouped);
    }

    private Todo findTodo(String id) {
        return todoRepository.findById(id)
                .orElseThrow(() -> new TodoApiException(
                        HttpStatus.NOT_FOUND,
                        "TODO_NOT_FOUND",
                        "Todo를 찾을 수 없습니다."
                ));
    }

    private TodoStatus validateListRequest(String status, int page, int limit) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (page < 1) {
            fields.put("page", "page는 1 이상이어야 합니다.");
        }
        if (limit < 1 || limit > 100) {
            fields.put("limit", "limit는 1 이상 100 이하여야 합니다.");
        }
        TodoStatus todoStatus = TodoStatus.from(status);
        if (todoStatus == null) {
            throw new TodoApiException(HttpStatus.BAD_REQUEST, "INVALID_FILTER",
                "status는 all, active, completed 중 하나여야 합니다.");
        }
        if (!fields.isEmpty()) {
            throw validationError(fields);
        }
        return todoStatus;
    }

    private void validateUpdateRequest(UpdateTodoRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (!request.hasAnyField()) {
            fields.put("body", "수정할 필드를 하나 이상 입력해야 합니다.");
        }
        if (request.isTitlePresent()) {
            validateTitle(request.getTitle(), fields);
        }
        if (request.isDescriptionPresent()) {
            validateDescription(request.getDescription(), fields);
        }
        if (request.isNotePresent()) {
            validateNote(request.getNote(), fields);
        }
        if (request.isCompletedPresent() && request.getCompleted() == null) {
            fields.put("completed", "completed는 null일 수 없습니다.");
        }
        if (request.isPriorityPresent()) {
            validatePriority(request.getPriority(), fields);
        }
        if (request.isAssigneePresent() && request.getAssignee() != null) {
            validateAssigneeField(request.getAssignee(), fields);
        }
        if (!fields.isEmpty()) {
            if (fields.containsKey("priority")) {
                throw invalidPriority();
            }
            throw validationError(fields);
        }
    }

    private TodoPriority resolveCreatePriority(CreateTodoRequest request, Map<String, String> fields) {
        if (!request.isPriorityPresent()) {
            return TodoPriority.MEDIUM;
        }
        validatePriority(request.getPriority(), fields);
        return fields.containsKey("priority") ? null : parsePriority(request.getPriority());
    }

    private void validatePriority(String priority, Map<String, String> fields) {
        if (priority == null) {
            fields.put("priority", "priority must be one of HIGH, MEDIUM, LOW");
            return;
        }
        try {
            TodoPriority.valueOf(priority);
        } catch (IllegalArgumentException exception) {
            fields.put("priority", "priority must be one of HIGH, MEDIUM, LOW");
        }
    }

    private TodoPriority parsePriority(String priority) {
        return TodoPriority.valueOf(priority);
    }

    private TodoApiException invalidPriority() {
        return new TodoApiException(
                HttpStatus.BAD_REQUEST,
                "INVALID_PRIORITY",
                "priority must be one of HIGH, MEDIUM, LOW"
        );
    }

    private void validateTitle(String title) {
        Map<String, String> fields = new LinkedHashMap<>();
        validateTitle(title, fields);
        if (!fields.isEmpty()) {
            throw validationError(fields);
        }
    }

    private void validateTitle(String title, Map<String, String> fields) {
        String trimmed = title == null ? null : title.strip();
        if (trimmed == null || trimmed.isBlank()) {
            fields.put("title", "title은 비어 있을 수 없습니다.");
        } else if (trimmed.length() > 100) {
            fields.put("title", "title은 100자를 초과할 수 없습니다.");
        }
    }

    private void validateDescription(String description, Map<String, String> fields) {
        if (description != null && description.length() > 1000) {
            fields.put("description", "description은 1000자를 초과할 수 없습니다.");
        }
    }

    private void validateNote(String note, Map<String, String> fields) {
        if (note != null && note.length() > 1000) {
            fields.put("note", "note는 1000자를 초과할 수 없습니다.");
        }
    }

    private void validateStartNotAfterDue(OffsetDateTime startAt, OffsetDateTime dueAt, Map<String, String> fields) {
        if (startAt != null && dueAt != null && startAt.isAfter(dueAt)) {
            fields.put("startAt", "시작일은 마감일보다 늦을 수 없습니다.");
        }
    }

    private String resolveAssignee(String assignee, Map<String, String> fields) {
        if (assignee == null) {
            return null;
        }
        String trimmed = assignee.strip();
        if (trimmed.isBlank()) {
            return null;
        }
        validateAssigneeField(trimmed, fields);
        return fields.containsKey("assignee") ? null : trimmed;
    }

    private void validateAssigneeField(String assignee, Map<String, String> fields) {
        String trimmed = assignee == null ? null : assignee.strip();
        if (trimmed == null || trimmed.isBlank()) {
            return;
        }
        if (UNASSIGNED_TOKEN.equals(trimmed)) {
            fields.put("assignee", "담당자로 예약어 '@unassigned'를 사용할 수 없습니다.");
            return;
        }
        if (trimmed.length() > 50) {
            fields.put("assignee", "담당자는 50자를 초과할 수 없습니다.");
        }
    }

    private String normalizeAssigneeForUpdate(String assignee) {
        if (assignee == null) {
            return null;
        }
        String trimmed = assignee.strip();
        return trimmed.isBlank() ? null : trimmed;
    }

    private List<String> resolveTags(List<String> tags, Map<String, String> fields) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        List<String> normalized = new ArrayList<>();
        for (String tag : tags) {
            if (tag == null) continue;
            String trimmed = tag.strip();
            if (trimmed.isBlank()) continue;
            if (trimmed.length() > TAG_MAX_LENGTH) {
                fields.put("tags", "태그는 " + TAG_MAX_LENGTH + "자를 초과할 수 없습니다.");
                return null;
            }
            if (!normalized.contains(trimmed)) {
                normalized.add(trimmed);
            }
        }
        if (normalized.size() > TAG_MAX_COUNT) {
            fields.put("tags", "태그는 최대 " + TAG_MAX_COUNT + "개까지 지정할 수 있습니다.");
            return null;
        }
        return normalized;
    }

    private static String normalizeString(String s) {
        if (s == null) return null;
        String trimmed = s.strip();
        return trimmed.isBlank() ? null : trimmed;
    }

    /** 정렬 키를 쿼리에서 쓰는 토큰으로 정규화. 알 수 없는 값은 기본(PRIORITY). */
    private static String normalizeSort(String sort) {
        if (sort == null) return "PRIORITY";
        return switch (sort.trim().toLowerCase()) {
            case "dueat", "due" -> "DUE";
            case "createdat", "created", "latest" -> "CREATED";
            default -> "PRIORITY";
        };
    }

    private TodoApiException validationError(Map<String, String> fields) {
        return new TodoApiException(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "요청 값이 올바르지 않습니다.",
                fields
        );
    }

    private enum TodoStatus {
        ALL, ACTIVE, COMPLETED;

        private static TodoStatus from(String value) {
            try {
                return TodoStatus.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException | NullPointerException exception) {
                return null;
            }
        }
    }
}
