package com.test.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.backend.domain.entity.Trip;
import com.test.backend.domain.entity.User;
import com.test.backend.dto.request.CreateExpenseRequest;
import com.test.backend.exception.TodoApiException;
import com.test.backend.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class ExpenseValidationTest {
    ExpenseRepository expenses = mock(ExpenseRepository.class);
    TripRepository trips = mock(TripRepository.class);
    UserRepository users = mock(UserRepository.class);
    ExpenseService service = new ExpenseService(expenses, trips, users);
    @BeforeEach void setup() {
        User user = new User(); user.setId(1L);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("owner", null, java.util.List.of()));
        when(users.findByEmail("owner")).thenReturn(Optional.of(user));
        when(trips.findByIdAndOwnerId("trip", 1L)).thenReturn(Optional.of(mock(Trip.class)));
        when(expenses.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    private CreateExpenseRequest request(String amount, String currency) throws Exception {
        return new ObjectMapper().readValue("{\"amount\":" + amount + ",\"currency\":\"" + currency
            + "\",\"category\":\"food\"}", CreateExpenseRequest.class);
    }
    @ParameterizedTest
    @CsvSource({"0,KRW", "-1,USD", "1000000000000,KRW", "1.001,USD", "1,ABC"})
    void rejectsOutOfRangeValues(String amount, String currency) throws Exception {
        var request = request(amount, currency);
        assertThatThrownBy(() -> service.create("trip", request)).isInstanceOf(TodoApiException.class);
        verify(expenses, never()).save(any());
    }
    @ParameterizedTest
    @CsvSource({"0.01,USD", "999999999999.99,KRW", "1.000,usd"})
    void acceptsRepresentableValues(String amount, String currency) throws Exception {
        service.create("trip", request(amount, currency));
        verify(expenses).save(any());
    }
}
