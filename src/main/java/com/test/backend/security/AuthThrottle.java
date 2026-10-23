package com.test.backend.security;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 로그인 실패·재설정 메일 요청을 이메일 단위 고정 창(window)으로 제한.
// ponytail: 인스턴스 메모리(재시작 시 초기화, 단일 인스턴스 전제). 여러 인스턴스로 늘리면 DB/Redis로.
// 이메일 기준이라 여러 계정을 번갈아 찌르는 대입(credential stuffing)은 못 막음 — 필요해지면 IP 키 추가.
@Component
public class AuthThrottle {

    private static final int MAX_KEYS = 10_000;

    private record Window(Instant start, int count) {}

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;

    public AuthThrottle() {
        this(Clock.systemUTC());
    }

    AuthThrottle(Clock clock) {
        this.clock = clock;
    }

    /** key가 현재 창 안에서 이미 max번 이상 기록됐으면 true. */
    public boolean isBlocked(String key, int max, Duration window) {
        Window w = windows.get(normalize(key));
        return w != null && !expired(w, window) && w.count() >= max;
    }

    /** key에 1회 기록. 창이 지났으면 새 창을 연다. */
    public void record(String key, Duration window) {
        if (windows.size() > MAX_KEYS) {
            windows.values().removeIf(w -> expired(w, window));
        }
        windows.compute(normalize(key), (k, w) ->
                w == null || expired(w, window) ? new Window(clock.instant(), 1) : new Window(w.start(), w.count() + 1));
    }

    public void reset(String key) {
        windows.remove(normalize(key));
    }

    private boolean expired(Window w, Duration window) {
        return !clock.instant().isBefore(w.start().plus(window));
    }

    private static String normalize(String key) {
        return key == null ? "" : key.strip().toLowerCase(Locale.ROOT);
    }
}
