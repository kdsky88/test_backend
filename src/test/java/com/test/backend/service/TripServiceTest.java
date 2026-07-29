package com.test.backend.service;

import com.test.backend.domain.entity.User;
import com.test.backend.dto.request.CreateTripRequest;
import com.test.backend.exception.TodoApiException;
import com.test.backend.repository.TripRepository;
import com.test.backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    private static final String TRIP_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String EMAIL = "me@example.com";

    @Mock
    private TripRepository tripRepository;

    @Mock
    private UserRepository userRepository;

    private TripService tripService;

    @BeforeEach
    void setUp() {
        tripService = new TripService(tripRepository, userRepository);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(EMAIL, "n/a", java.util.List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsNotFoundWhenTripBelongsToAnotherOwner() {
        User me = new User();
        me.setId(1L);
        me.setEmail(EMAIL);
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(me));
        // 다른 소유자의 여행 → owner 스코프 쿼리가 비어서 돌아옴.
        given(tripRepository.findByIdAndOwnerId(TRIP_ID, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> tripService.getTrip(TRIP_ID))
                .isInstanceOfSatisfying(TodoApiException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo("TRIP_NOT_FOUND"));
    }

    @Test
    void rejectsStartDateAfterEndDateOnCreate() {
        CreateTripRequest request = new CreateTripRequest();
        ReflectionTestUtils.setField(request, "title", "제주 여행");
        ReflectionTestUtils.setField(request, "startDate", LocalDate.of(2026, 6, 12));
        ReflectionTestUtils.setField(request, "endDate", LocalDate.of(2026, 6, 10));

        assertThatThrownBy(() -> tripService.createTrip(request))
                .isInstanceOfSatisfying(TodoApiException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo("VALIDATION_ERROR");
                    assertThat(ex.getFields()).containsKey("startDate");
                });
    }
}
