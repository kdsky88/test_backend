package com.test.backend.dto.response;

public record TodoStats(
        long total,
        long completed,
        long active,
        long overdue,
        long dueToday,
        long completedToday,
        long completedThisWeek
) {}
