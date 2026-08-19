package com.test.backend.dto.response;

import com.test.backend.domain.entity.Expense;

import java.math.BigDecimal;
import java.time.Instant;

public record ExpenseResponse(
        String id,
        BigDecimal amount,
        String currency,
        String category,
        String memo,
        Instant createdAt
) {
    public ExpenseResponse(Expense e) {
        this(e.getId(), e.getAmount(), e.getCurrency(), e.getCategory(), e.getMemo(), e.getCreatedAt());
    }
}
