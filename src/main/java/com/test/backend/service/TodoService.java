package com.test.backend.service;

import com.test.backend.domain.entity.Subtask;
import com.test.backend.domain.entity.Todo;
import com.test.backend.domain.entity.TodoPriority;
import com.test.backend.domain.entity.TodoRecurrence;
import com.test.backend.domain.entity.User;
import com.test.backend.dto.request.CreateTodoRequest;
import com.test.backend.dto.request.SubtaskRequest;
import com.test.backend.dto.request.UpdateTodoRequest;
import com.test.backend.dto.response.ApiResponse;
import com.test.backend.dto.response.TodoListResponse;
import com.test.backend.dto.response.TodoResponse;
import com.test.backend.exception.TodoApiException;
import com.test.backend.repository.TodoRepository;
import com.test.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

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
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TodoService {

    private static final String UNASSIGNED_TOKEN = "@unassigned";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int TAG_MAX_COUNT = 10;
    private static final int TAG_MAX_LENGTH = 20;
    private static final int SUBTASK_MAX_COUNT = 50;
    private static final int SUBTASK_MAX_LENGTH = 100;

    private final TodoRepository todoRepository;
    private final UserRepository userRepository;

    public TodoService(TodoRepository todoRepository) {
        this(todoRepository, null);
    }

    @Autowired
    public TodoService(TodoRepository todoRepository, UserRepository userRepository) {
        this.todoRepository = todoRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public TodoListResponse getTodos(String status, int page, int limit, String assignee, String tag, String sort, boolean hideCompleted, String search) {
        TodoStatus todoStatus = validateListRequest(status, page, limit);
        String normalizedAssignee = normalizeString(assignee);
        String normalizedTag = normalizeString(tag);
        String sortToken = normalizeSort(sort);
        String searchTerm = normalizeString(search);
        PageRequest pageable = PageRequest.of(page - 1, limit);
        Long ownerId = currentOwnerId();

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
                todoPage = ownerId == null
                        ? todoRepository.findByTagAndCompleted(normalizedTag, completed, sortToken, searchTerm, pageable)
                        : todoRepository.findByOwnerIdAndTagAndCompleted(ownerId, normalizedTag, completed, sortToken, searchTerm, pageable);
            } else if (UNASSIGNED_TOKEN.equals(normalizedAssignee)) {
                todoPage = ownerId == null
                        ? todoRepository.findByTagAndUnassignedAndCompleted(normalizedTag, completed, sortToken, searchTerm, pageable)
                        : todoRepository.findByOwnerIdAndTagAndUnassignedAndCompleted(ownerId, normalizedTag, completed, sortToken, searchTerm, pageable);
            } else {
                todoPage = ownerId == null
                        ? todoRepository.findByTagAndAssigneeAndCompleted(normalizedTag, completed, normalizedAssignee, sortToken, searchTerm, pageable)
                        : todoRepository.findByOwnerIdAndTagAndAssigneeAndCompleted(ownerId, normalizedTag, completed, normalizedAssignee, sortToken, searchTerm, pageable);
            }
        } else {
            if (normalizedAssignee == null) {
                todoPage = (completed == null)
                        ? (ownerId == null
                                ? todoRepository.findAllByPriorityOrder(sortToken, searchTerm, pageable)
                                : todoRepository.findAllByOwnerIdOrder(ownerId, sortToken, searchTerm, pageable))
                        : (ownerId == null
                                ? todoRepository.findByCompletedOrderByPriority(completed, sortToken, searchTerm, pageable)
                                : todoRepository.findByOwnerIdAndCompletedOrder(ownerId, completed, sortToken, searchTerm, pageable));
            } else if (UNASSIGNED_TOKEN.equals(normalizedAssignee)) {
                todoPage = ownerId == null
                        ? todoRepository.findByUnassignedAndCompleted(completed, sortToken, searchTerm, pageable)
                        : todoRepository.findByOwnerIdAndUnassignedAndCompleted(ownerId, completed, sortToken, searchTerm, pageable);
            } else {
                todoPage = ownerId == null
                        ? todoRepository.findByAssigneeAndCompleted(completed, normalizedAssignee, sortToken, searchTerm, pageable)
                        : todoRepository.findByOwnerIdAndAssigneeAndCompleted(ownerId, completed, normalizedAssignee, sortToken, searchTerm, pageable);
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

        Long ownerId = currentOwnerId();
        long total = ownerId == null ? todoRepository.count() : todoRepository.countByOwnerId(ownerId);
        long completed = ownerId == null
                ? todoRepository.countByCompleted(true)
                : todoRepository.countByOwnerIdAndCompleted(ownerId, true);
        long overdue = ownerId == null
                ? todoRepository.countOverdue(now)
                : todoRepository.countOverdueByOwnerId(ownerId, now);
        long dueToday = ownerId == null
                ? todoRepository.countDueBetween(startOfToday, endOfToday)
                : todoRepository.countDueBetweenByOwnerId(ownerId, startOfToday, endOfToday);

        // 완료 시각(completedAt, Instant) 기준 오늘/이번 주 완료 수. 주 시작은 달력과 맞춰 일요일.
        java.time.Instant startOfTodayI = today.atStartOfDay(KST).toInstant();
        java.time.Instant endOfTodayI = today.plusDays(1).atStartOfDay(KST).toInstant();
        java.time.Instant startOfWeekI = today
                .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.SUNDAY))
                .atStartOfDay(KST).toInstant();
        long completedToday = ownerId == null
                ? todoRepository.countCompletedBetween(startOfTodayI, endOfTodayI)
                : todoRepository.countCompletedBetweenByOwnerId(ownerId, startOfTodayI, endOfTodayI);
        long completedThisWeek = ownerId == null
                ? todoRepository.countCompletedBetween(startOfWeekI, endOfTodayI)
                : todoRepository.countCompletedBetweenByOwnerId(ownerId, startOfWeekI, endOfTodayI);

        // 최근 400일 완료 시각을 한 번 가져와 streak/최장연속/이번달/주간추이를 모두 계산(추가 쿼리 없음).
        // ponytail: 400일 창이라 그 이상 연속/추이는 잘림 — 개인용엔 충분.
        Instant streakFloor = today.minusDays(400).atStartOfDay(KST).toInstant();
        List<Instant> completions = ownerId == null
                ? todoRepository.findCompletedAtSince(streakFloor)
                : todoRepository.findCompletedAtSinceByOwnerId(ownerId, streakFloor);
        List<LocalDate> completedDates = completions.stream()
                .map(inst -> inst.atZone(KST).toLocalDate())
                .toList();
        Set<LocalDate> completedDays = new java.util.HashSet<>(completedDates);
        long streakDays = computeStreak(completedDays, today);
        long longestStreak = computeLongestStreak(completedDays);

        LocalDate monthStart = today.withDayOfMonth(1);
        long completedThisMonth = completedDates.stream()
                .filter(d -> !d.isBefore(monthStart)).count();

        // 지난 7일 요일별 완료 수(index 6 = 오늘).
        long[] last7 = new long[7];
        for (LocalDate d : completedDates) {
            long ago = java.time.temporal.ChronoUnit.DAYS.between(d, today);
            if (ago >= 0 && ago < 7) last7[(int) (6 - ago)]++;
        }
        List<Long> last7Days = java.util.Arrays.stream(last7).boxed().toList();

        return new ApiResponse<>(new TodoStats(
                total, completed, total - completed, overdue, dueToday,
                completedToday, completedThisWeek, completedThisMonth,
                streakDays, longestStreak, last7Days));
    }

    @Transactional(readOnly = true)
    public TodoListResponse getCompletedHistory(int page, int limit) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (page < 1) fields.put("page", "page는 1 이상이어야 합니다.");
        if (limit < 1 || limit > 100) fields.put("limit", "limit는 1 이상 100 이하여야 합니다.");
        if (!fields.isEmpty()) throw validationError(fields);

        Long ownerId = currentOwnerId();
        PageRequest pageable = PageRequest.of(page - 1, limit);
        Page<Todo> completed = ownerId == null
                ? todoRepository.findCompletedOrderByCompletedAtDesc(pageable)
                : todoRepository.findCompletedByOwnerOrderByCompletedAtDesc(ownerId, pageable);
        return TodoListResponse.from(completed.map(TodoResponse::new), page);
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<String>> getAssignees() {
        Long ownerId = currentOwnerId();
        return new ApiResponse<>(ownerId == null
                ? todoRepository.findDistinctAssignees()
                : todoRepository.findDistinctAssigneesByOwnerId(ownerId));
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<String>> getTags() {
        Long ownerId = currentOwnerId();
        return new ApiResponse<>(ownerId == null
                ? todoRepository.findDistinctTags()
                : todoRepository.findDistinctTagsByOwnerId(ownerId));
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
        validateSubtasks(request.getSubtasks(), fields);
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
        currentUser().ifPresent(todo::assignOwner);
        todo.assignTo(resolveAssignedTo(request.getAssignedToEmail()));
        todo.updateStartAt(request.getStartAt());
        todo.updateRecurrence(parseRecurrence(request.getRecurrence()));
        if (tags != null) {
            tags.forEach(todo::addTag);
        }
        todo.replaceSubtasks(toSubtasks(request.getSubtasks()));
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
        // 소유자 또는 담당자만 접근. 담당자는 '완료'만 변경 가능.
        Long userId = currentUser().map(User::getId).orElse(null);
        Todo todo = (userId == null)
                ? todoRepository.findById(id).orElseThrow(this::todoNotFound)
                : todoRepository.findByIdVisibleTo(id, userId).orElseThrow(this::todoNotFound);
        boolean isOwner = userId == null || todo.getOwner().getId().equals(userId);
        if (!isOwner && request.hasNonCompletedField()) {
            throw new TodoApiException(
                    HttpStatus.FORBIDDEN, "FORBIDDEN", "담당자는 완료 상태만 변경할 수 있습니다.");
        }

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
        if (request.isAssignedToEmailPresent()) {
            // 여기까지 온 건 소유자(위에서 담당자의 비-완료 변경 차단). 담당자 재배정.
            todo.assignTo(resolveAssignedTo(request.getAssignedToEmail()));
        }
        if (request.isSubtasksPresent()) {
            // 하위 항목은 소유자·담당자 모두 편집/체크 가능(위 권한 블록에서 제외됨).
            todo.replaceSubtasks(toSubtasks(request.getSubtasks()));
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
        next.assignOwner(source.getOwner());
        next.assignTo(source.getAssignedTo());
        next.updateStartAt(shiftDate(source.getStartAt(), source.getRecurrence()));
        next.updateRecurrence(source.getRecurrence());
        source.getTags().forEach(next::addTag);
        // 다음 주기 항목은 하위 체크리스트를 모두 미완료 상태로 복제.
        next.replaceSubtasks(source.getSubtasks().stream()
                .map(s -> new Subtask(s.getTitle(), false))
                .toList());
        todoRepository.save(next);
    }

    /** 담당자 이메일 → User. 빈 값이면 null(배정 해제). 없는 이메일이면 검증 에러. */
    private User resolveAssignedTo(String email) {
        if (email == null || email.isBlank()) return null;
        if (userRepository == null) return null; // 유저 저장소 없는 테스트 경로
        return userRepository.findByEmail(email.strip()).orElseThrow(() -> {
            Map<String, String> f = new LinkedHashMap<>();
            f.put("assignedToEmail", "해당 이메일의 사용자가 없습니다.");
            return validationError(f);
        });
    }

    private TodoApiException todoNotFound() {
        return new TodoApiException(
                HttpStatus.NOT_FOUND, "TODO_NOT_FOUND", "Todo를 찾을 수 없습니다.");
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
        Long ownerId = currentOwnerId();
        List<Todo> todos = ownerId == null
                ? todoRepository.findByDateRangeOverlap(start, end)
                : todoRepository.findByOwnerIdAndDateRangeOverlap(ownerId, start, end);
        for (Todo todo : todos) {
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
        Long ownerId = currentOwnerId();
        return (ownerId == null
                ? todoRepository.findById(id)
                : todoRepository.findByIdAndOwnerId(id, ownerId))
                .orElseThrow(() -> new TodoApiException(
                        HttpStatus.NOT_FOUND,
                        "TODO_NOT_FOUND",
                        "Todo를 찾을 수 없습니다."
                ));
    }

    private Long currentOwnerId() {
        return currentUser().map(User::getId).orElse(null);
    }

    private java.util.Optional<User> currentUser() {
        if (userRepository == null) {
            return java.util.Optional.empty();
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw unauthenticated();
        }
        return java.util.Optional.of(userRepository.findByEmail(authentication.getName())
                .orElseThrow(this::unauthenticated));
    }

    private TodoApiException unauthenticated() {
        return new TodoApiException(
                HttpStatus.UNAUTHORIZED,
                "UNAUTHENTICATED",
                "인증이 필요합니다."
        );
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
        if (request.isSubtasksPresent()) {
            validateSubtasks(request.getSubtasks(), fields);
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

    private void validateSubtasks(List<SubtaskRequest> subtasks, Map<String, String> fields) {
        if (subtasks == null) {
            return;
        }
        if (subtasks.size() > SUBTASK_MAX_COUNT) {
            fields.put("subtasks", "하위 항목은 최대 " + SUBTASK_MAX_COUNT + "개까지 지정할 수 있습니다.");
            return;
        }
        for (SubtaskRequest s : subtasks) {
            String title = (s == null || s.getTitle() == null) ? null : s.getTitle().strip();
            if (title == null || title.isBlank()) {
                fields.put("subtasks", "하위 항목 제목은 비어 있을 수 없습니다.");
                return;
            }
            if (title.length() > SUBTASK_MAX_LENGTH) {
                fields.put("subtasks", "하위 항목 제목은 " + SUBTASK_MAX_LENGTH + "자를 초과할 수 없습니다.");
                return;
            }
        }
    }

    /** 요청 하위 항목 → 엔티티 목록. 검증은 validateSubtasks에서 이미 수행(여기선 빈 항목만 방어적으로 제외). */
    private List<Subtask> toSubtasks(List<SubtaskRequest> subtasks) {
        if (subtasks == null) {
            return List.of();
        }
        List<Subtask> result = new ArrayList<>();
        for (SubtaskRequest s : subtasks) {
            if (s == null || s.getTitle() == null || s.getTitle().isBlank()) {
                continue;
            }
            result.add(new Subtask(s.getTitle().strip(), s.isDone()));
        }
        return result;
    }

    private static String normalizeString(String s) {
        if (s == null) return null;
        String trimmed = s.strip();
        return trimmed.isBlank() ? null : trimmed;
    }

    /** 정렬 키를 쿼리에서 쓰는 토큰으로 정규화. 알 수 없는 값은 기본(PRIORITY). */
    // 연속 완료 일수. 오늘 완료가 있으면 오늘부터, 없고 어제 있으면 어제부터(오늘 아직 안 한 것 유예)
    // 하루씩 뒤로 가며 연속으로 완료가 있는 날을 센다. 둘 다 없으면 0. 순수 함수(테스트용).
    static long computeStreak(Set<LocalDate> completedDays, LocalDate today) {
        LocalDate cursor;
        if (completedDays.contains(today)) {
            cursor = today;
        } else if (completedDays.contains(today.minusDays(1))) {
            cursor = today.minusDays(1);
        } else {
            return 0;
        }
        long streak = 0;
        while (completedDays.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    // 기록 전체에서 가장 긴 연속 완료 구간. 순수 함수(테스트용).
    static long computeLongestStreak(Set<LocalDate> completedDays) {
        long best = 0;
        for (LocalDate d : completedDays) {
            // 각 연속 구간의 시작점(전날이 없는 날)에서만 세어 O(n)
            if (completedDays.contains(d.minusDays(1))) continue;
            long len = 0;
            LocalDate cur = d;
            while (completedDays.contains(cur)) {
                len++;
                cur = cur.plusDays(1);
            }
            if (len > best) best = len;
        }
        return best;
    }

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
