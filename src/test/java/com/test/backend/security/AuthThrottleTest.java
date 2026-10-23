package com.test.backend.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class AuthThrottleTest {

    private static final Duration WINDOW = Duration.ofMinutes(15);

    // 테스트에서 시간을 앞으로 돌릴 수 있는 시계.
    private static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    @Test
    void blocksAfterMaxWithinWindowAndReopensAfterIt() {
        MutableClock clock = new MutableClock();
        AuthThrottle throttle = new AuthThrottle(clock);

        for (int i = 0; i < 5; i++) {
            assertThat(throttle.isBlocked("login:A@x.com", 5, WINDOW)).isFalse();
            throttle.record("login:a@x.com ", WINDOW); // 대소문자·공백 무시
        }
        assertThat(throttle.isBlocked("login:a@x.com", 5, WINDOW)).isTrue();
        assertThat(throttle.isBlocked("login:b@x.com", 5, WINDOW)).isFalse();

        clock.now = clock.now.plus(WINDOW);
        assertThat(throttle.isBlocked("login:a@x.com", 5, WINDOW)).isFalse();
    }

    @Test
    void resetClearsFailures() {
        AuthThrottle throttle = new AuthThrottle(new MutableClock());
        throttle.record("k", WINDOW);
        assertThat(throttle.isBlocked("k", 1, WINDOW)).isTrue();
        throttle.reset("k");
        assertThat(throttle.isBlocked("k", 1, WINDOW)).isFalse();
    }
}
