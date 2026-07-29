package com.test.backend.service;

import com.test.backend.domain.entity.Trip;
import com.test.backend.domain.entity.User;
import com.test.backend.dto.request.CreateTripRequest;
import com.test.backend.dto.request.UpdateTripRequest;
import com.test.backend.dto.response.ApiResponse;
import com.test.backend.dto.response.TodoResponse;
import com.test.backend.dto.response.TripResponse;
import com.test.backend.exception.TodoApiException;
import com.test.backend.repository.TodoRepository;
import com.test.backend.repository.TripRepository;
import com.test.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TripService {

    private static final int TITLE_MAX = 100;
    private static final int DESTINATION_MAX = 100;

    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final TodoRepository todoRepository;

    public TripService(TripRepository tripRepository, UserRepository userRepository) {
        this(tripRepository, userRepository, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public TripService(TripRepository tripRepository, UserRepository userRepository, TodoRepository todoRepository) {
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
        this.todoRepository = todoRepository;
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<TripResponse>> getTrips() {
        List<TripResponse> trips = tripRepository
                .findByOwnerIdOrderByStartDateAscCreatedAtDesc(currentUser().getId())
                .stream().map(TripResponse::new).toList();
        return new ApiResponse<>(trips);
    }

    @Transactional(readOnly = true)
    public ApiResponse<TripResponse> getTrip(String id) {
        return new ApiResponse<>(new TripResponse(findTrip(id)));
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<TodoResponse>> getTripTodos(String id) {
        Trip trip = findTrip(id); // 소유자 스코프 — 남의 여행이면 404
        List<TodoResponse> todos = todoRepository.findByTripIdOrderBySchedule(trip.getId())
                .stream().map(TodoResponse::new).toList();
        return new ApiResponse<>(todos);
    }

    @Transactional
    public ApiResponse<TripResponse> createTrip(CreateTripRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        String title = validateTitle(request.getTitle(), fields);
        validateDestination(request.getDestination(), fields);
        validateDateOrder(request.getStartDate(), request.getEndDate(), fields);
        if (!fields.isEmpty()) {
            throw validationError(fields);
        }

        Trip trip = new Trip(title, normalize(request.getDestination()),
                request.getStartDate(), request.getEndDate());
        trip.assignOwner(currentUser());
        return new ApiResponse<>(new TripResponse(tripRepository.save(trip)));
    }

    @Transactional
    public ApiResponse<TripResponse> updateTrip(String id, UpdateTripRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (!request.hasAnyField()) {
            fields.put("body", "수정할 필드를 하나 이상 입력해야 합니다.");
        }
        if (request.isTitlePresent()) {
            validateTitle(request.getTitle(), fields);
        }
        if (request.isDestinationPresent()) {
            validateDestination(request.getDestination(), fields);
        }
        if (!fields.isEmpty()) {
            throw validationError(fields);
        }

        Trip trip = findTrip(id);
        LocalDate effectiveStart = request.isStartDatePresent() ? request.getStartDate() : trip.getStartDate();
        LocalDate effectiveEnd = request.isEndDatePresent() ? request.getEndDate() : trip.getEndDate();
        Map<String, String> dateFields = new LinkedHashMap<>();
        validateDateOrder(effectiveStart, effectiveEnd, dateFields);
        if (!dateFields.isEmpty()) {
            throw validationError(dateFields);
        }

        if (request.isTitlePresent()) {
            trip.updateTitle(request.getTitle().strip());
        }
        if (request.isDestinationPresent()) {
            trip.updateDestination(normalize(request.getDestination()));
        }
        if (request.isStartDatePresent()) {
            trip.updateStartDate(request.getStartDate());
        }
        if (request.isEndDatePresent()) {
            trip.updateEndDate(request.getEndDate());
        }
        return new ApiResponse<>(new TripResponse(tripRepository.saveAndFlush(trip)));
    }

    @Transactional
    public void deleteTrip(String id) {
        tripRepository.delete(findTrip(id));
    }

    private Trip findTrip(String id) {
        return tripRepository.findByIdAndOwnerId(id, currentUser().getId())
                .orElseThrow(() -> new TodoApiException(
                        HttpStatus.NOT_FOUND, "TRIP_NOT_FOUND", "여행을 찾을 수 없습니다."));
    }

    // ponytail: TodoService의 인증 헬퍼와 동일 — 소비자 2곳뿐이라 추상화보다 복제가 저렴.
    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw unauthenticated();
        }
        return userRepository.findByEmail(auth.getName()).orElseThrow(this::unauthenticated);
    }

    private String validateTitle(String title, Map<String, String> fields) {
        String trimmed = title == null ? null : title.strip();
        if (trimmed == null || trimmed.isBlank()) {
            fields.put("title", "title은 비어 있을 수 없습니다.");
        } else if (trimmed.length() > TITLE_MAX) {
            fields.put("title", "title은 " + TITLE_MAX + "자를 초과할 수 없습니다.");
        }
        return trimmed;
    }

    private void validateDestination(String destination, Map<String, String> fields) {
        if (destination != null && destination.strip().length() > DESTINATION_MAX) {
            fields.put("destination", "destination은 " + DESTINATION_MAX + "자를 초과할 수 없습니다.");
        }
    }

    private void validateDateOrder(LocalDate start, LocalDate end, Map<String, String> fields) {
        if (start != null && end != null && start.isAfter(end)) {
            fields.put("startDate", "여행 시작일은 종료일보다 늦을 수 없습니다.");
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        return trimmed.isBlank() ? null : trimmed;
    }

    private TodoApiException validationError(Map<String, String> fields) {
        return new TodoApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "요청 값이 올바르지 않습니다.", fields);
    }

    private TodoApiException unauthenticated() {
        return new TodoApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "인증이 필요합니다.");
    }
}
