package com.test.backend.service;

import com.test.backend.domain.entity.Todo;
import com.test.backend.domain.entity.TodoPriority;
import com.test.backend.domain.entity.TodoRecurrence;
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
import com.test.backend.dto.response.TodoStats;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TodoService {

    private static final String UNASSIGNED_TOKEN = "@unassigned";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int TAG_MAX_COUNT = 10;
    private static final int TAG_MAX_LENGTH = 20;

    private final TodoRepository todoRepository;

    @Transactional(readOnly = true)
    public TodoListResponse getTodos(String status, int page, int limit, String assignee, String tag, String sort, boolean hideCompleted) {
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
        if (hideCompleted && completed == null) {
            completed = false; // '전체' + 완료 숨기기 → 미완료만
        }

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
                todoPage = (completed == null)
                        ? todoRepository.findAllByPriorityOrder(sortToken, pageable)
                        : todoRepository.findByCompletedOrderByPriority(completed, sortToken, pageable);
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
    public ApiResponse<TodoStats> getStats() {
        OffsetDateTime now = OffsetDateTime.now();
        LocalDate today = LocalDate.now(KST);
        OffsetDateTime startOfToday = today.atStartOfDay(KST).toOffsetDateTime();
        OffsetDateTime endOfToday = today.plusDays(1).atStartOfDay(KST).toOffsetDateTime();

        long total = todoRepository.count();
        long completed = todoRepository.countByCompleted(true);
        long overdue = todoRepository.countOverdue(now);
        long dueToday = todoRepository.countDueBetween(startOfToday, endOfToday);
        return new ApiResponse<>(
                new TodoStats(total, completed, total - completed, overdue, dueToday));
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
        validateRecurrenceWithDue(request.getRecurrence(), request.getDueAt(), fields);
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
        todo.updateRecurrence(parseRecurrence(request.getRecurrence()));
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
        TodoRecurrence effectiveRecurrence = request.isRecurrencePresent()
                ? parseRecurrence(request.getRecurrence())
                : todo.getRecurrence();
        Map<String, String> dateFields = new LinkedHashMap<>();
        validateStartNotAfterDue(effectiveStart, effectiveDue, dateFields);
        if (effectiveRecurrence != TodoRecurrence.NONE && effectiveDue == null) {
            dateFields.put("recurrence", "반복 일정은 마감일이 필요합니다.");
        }
        if (!dateFields.isEmpty()) {
            throw validationError(dateFields);
        }

        boolean wasCompleted = todo.isCompleted();

        if (request.isRecurrencePresent()) {
            todo.updateRecurrence(effectiveRecurrence);
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
        Todo saved = todoRepository.saveAndFlush(todo);
        // 반복 항목을 '완료'로 전환하면 다음 주기 항목을 자동 생성
        if (!wasCompleted && saved.isCompleted()
                && saved.getRecurrence() != TodoRecurrence.NONE) {
            spawnNextOccurrence(saved);
        }
        return new ApiResponse<>(new TodoResponse(saved));
    }

    private void spawnNextOccurrence(Todo source) {
        Todo next = new Todo(
                source.getTitle(),
                source.getDescription(),
                source.getNote(),
                shiftDate(source.getDueAt(), source.getRecurrence()),
                source.getPriority(),
                source.getAssignee()
        );
        next.updateStartAt(shiftDate(source.getStartAt(), source.getRecurrence()));
        next.updateRecurrence(source.getRecurrence());
        source.getTags().forEach(next::addTag);
        todoRepository.save(next);
    }

    private OffsetDateTime shiftDate(OffsetDateTime base, TodoRecurrence recurrence) {
        if (base == null) {
            return null;
        }
        return switch (recurrence) {
            case DAILY -> base.plusDays(1);
            case WEEKLY -> base.plusWeeks(1);
            case MONTHLY -> base.plusMonths(1);
            case NONE -> base;
        };
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
        LocalDate monthFirst = ym.atDay(1);
        LocalDate monthLast = ym.atEndOfMonth();

        // 시작일~마감일 사이의 모든 날짜에 배치(월 범위로 클램프). 한쪽만 있으면 그 하루만.
        Map<String, List<TodoResponse>> grouped = new LinkedHashMap<>();
        for (Todo todo : todoRepository.findByDateRangeOverlap(start, end)) {
            OffsetDateTime rangeStart = todo.getStartAt() != null ? todo.getStartAt() : todo.getDueAt();
            OffsetDateTime rangeEnd = todo.getDueAt() != null ? todo.getDueAt() : todo.getStartAt();
            LocalDate from = rangeStart.atZoneSameInstant(KST).toLocalDate();
            LocalDate to = rangeEnd.atZoneSameInstant(KST).toLocalDate();
            if (from.isBefore(monthFirst)) from = monthFirst;
            if (to.isAfter(monthLast)) to = monthLast;
            TodoResponse resp = new TodoResponse(todo);
            for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
                grouped.computeIfAbsent(d.toString(), key -> new ArrayList<>()).add(resp);
            }
        }
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
        if (request.isRecurrencePresent()) {
            validateRecurrence(request.getRecurrence(), fields);
        }
        if (!fields.isEmpty()) {
            if (fields.containsKey("priority")) {
                throw invalidPriority();
            }
            throw validationError(fields);
        }
    }

    private void validateRecurrence(String recurrence, Map<String, String> fields) {
        if (recurrence == null) {
            return;
        }
        try {
            TodoRecurrence.valueOf(recurrence.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            fields.put("recurrence", "recurrence must be one of NONE, DAILY, WEEKLY, MONTHLY");
        }
    }

    private void validateRecurrenceWithDue(String recurrence, OffsetDateTime dueAt, Map<String, String> fields) {
        validateRecurrence(recurrence, fields);
        if (!fields.containsKey("recurrence")
                && parseRecurrence(recurrence) != TodoRecurrence.NONE
                && dueAt == null) {
            fields.put("recurrence", "반복 일정은 마감일이 필요합니다.");
        }
    }

    private TodoRecurrence parseRecurrence(String recurrence) {
        if (recurrence == null || recurrence.isBlank()) {
            return TodoRecurrence.NONE;
        }
        return TodoRecurrence.valueOf(recurrence.trim().toUpperCase());
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
