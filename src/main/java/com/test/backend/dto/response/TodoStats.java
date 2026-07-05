package com.test.backend.dto.response;

import java.util.List;

public record TodoStats(
        long total,
        long completed,
        long active,
        long overdue,
        long dueToday,
        long completedToday,
        long completedThisWeek,
        long completedThisMonth,
        long streakDays,
        long longestStreak,
        List<Long> last7Days // index 6 = 오늘, 0 = 6일 전
) {}
