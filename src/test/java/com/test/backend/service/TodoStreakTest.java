package com.test.backend.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TodoStreakTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 6);

    @Test
    void noCompletions_zero() {
        assertThat(TodoService.computeStreak(Set.of(), TODAY)).isZero();
    }

    @Test
    void onlyToday_one() {
        assertThat(TodoService.computeStreak(Set.of(TODAY), TODAY)).isEqualTo(1);
    }

    @Test
    void threeConsecutiveDaysEndingToday_three() {
        assertThat(TodoService.computeStreak(
                Set.of(TODAY, TODAY.minusDays(1), TODAY.minusDays(2)), TODAY)).isEqualTo(3);
    }

    @Test
    void yesterdayButNotToday_survivesAsOne() {
        // 오늘 아직 완료 안 했지만 어제 했으면 연속은 유지(어제부터 카운트)
        assertThat(TodoService.computeStreak(Set.of(TODAY.minusDays(1)), TODAY)).isEqualTo(1);
    }

    @Test
    void gapAtYesterday_breaksStreak() {
        // 오늘·이틀 전은 있고 어제가 비면 오늘 하나만
        assertThat(TodoService.computeStreak(Set.of(TODAY, TODAY.minusDays(2)), TODAY)).isEqualTo(1);
    }

    @Test
    void neitherTodayNorYesterday_zero() {
        assertThat(TodoService.computeStreak(Set.of(TODAY.minusDays(2)), TODAY)).isZero();
    }
}
