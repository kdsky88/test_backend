package com.test.backend.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class CreateExpenseRequest {
    private BigDecimal amount;
    private String currency; // 없으면 KRW
    private String category;
    private String memo;
}
