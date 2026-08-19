package com.test.backend.service;

import com.test.backend.domain.entity.Expense;
import com.test.backend.domain.entity.User;
import com.test.backend.dto.request.CreateExpenseRequest;
import com.test.backend.dto.response.ApiResponse;
import com.test.backend.dto.response.ExpenseResponse;
import com.test.backend.exception.TodoApiException;
import com.test.backend.repository.ExpenseRepository;
import com.test.backend.repository.TripRepository;
import com.test.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExpenseService {

    private static final int MEMO_MAX = 200;
    private static final int CATEGORY_MAX = 20;

    private final ExpenseRepository expenseRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;

    public ExpenseService(ExpenseRepository expenseRepository, TripRepository tripRepository,
                          UserRepository userRepository) {
        this.expenseRepository = expenseRepository;
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<ExpenseResponse>> getExpenses(String tripId) {
        User user = currentUser();
        requireTrip(tripId, user);
        List<ExpenseResponse> items = expenseRepository
                .findByTripIdAndOwnerIdOrderByCreatedAtDesc(tripId, user.getId())
                .stream().map(ExpenseResponse::new).toList();
        return new ApiResponse<>(items);
    }

    @Transactional
    public ApiResponse<ExpenseResponse> create(String tripId, CreateExpenseRequest request) {
        User user = currentUser();
        requireTrip(tripId, user);

        Map<String, String> fields = new LinkedHashMap<>();
        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            fields.put("amount", "금액은 0보다 커야 합니다.");
        }
        String category = request.getCategory() == null ? null : request.getCategory().strip();
        if (category == null || category.isBlank()) {
            fields.put("category", "분류를 입력해야 합니다.");
        } else if (category.length() > CATEGORY_MAX) {
            fields.put("category", "분류는 " + CATEGORY_MAX + "자를 초과할 수 없습니다.");
        }
        String currency = normalizeCurrency(request.getCurrency());
        if (currency.length() != 3) {
            fields.put("currency", "통화 코드가 올바르지 않습니다.");
        }
        if (request.getMemo() != null && request.getMemo().strip().length() > MEMO_MAX) {
            fields.put("memo", "메모는 " + MEMO_MAX + "자를 초과할 수 없습니다.");
        }
        if (!fields.isEmpty()) {
            throw new TodoApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "요청 값이 올바르지 않습니다.", fields);
        }

        String memo = (request.getMemo() == null || request.getMemo().isBlank()) ? null : request.getMemo().strip();
        Expense expense = new Expense(tripId, request.getAmount(), currency, category, memo);
        expense.assignOwner(user);
        return new ApiResponse<>(new ExpenseResponse(expenseRepository.save(expense)));
    }

    @Transactional
    public void delete(String id) {
        Expense expense = expenseRepository.findByIdAndOwnerId(id, currentUser().getId())
                .orElseThrow(() -> new TodoApiException(
                        HttpStatus.NOT_FOUND, "EXPENSE_NOT_FOUND", "경비 항목을 찾을 수 없습니다."));
        expenseRepository.delete(expense);
    }

    private static String normalizeCurrency(String raw) {
        if (raw == null || raw.isBlank()) return "KRW";
        return raw.strip().toUpperCase();
    }

    private void requireTrip(String tripId, User user) {
        tripRepository.findByIdAndOwnerId(tripId, user.getId())
                .orElseThrow(() -> new TodoApiException(
                        HttpStatus.NOT_FOUND, "TRIP_NOT_FOUND", "여행을 찾을 수 없습니다."));
    }

    // ponytail: TripService/TodoService의 인증 헬퍼와 동일 — 소비자가 몇 곳뿐이라 복제가 저렴.
    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw unauthenticated();
        }
        return userRepository.findByEmail(auth.getName()).orElseThrow(this::unauthenticated);
    }

    private TodoApiException unauthenticated() {
        return new TodoApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "인증이 필요합니다.");
    }
}
