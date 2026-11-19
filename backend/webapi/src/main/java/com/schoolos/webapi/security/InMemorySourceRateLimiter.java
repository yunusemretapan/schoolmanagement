package com.schoolos.webapi.security;

import com.schoolos.application.features.identity.ports.SourceRateLimiter;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding one-minute window per source, in memory (plan 0003, R4): correct for one server instance; with several,
 * each counts its own share (backlog). Sources idle for a full window are forgotten so memory stays bounded.
 */
public final class InMemorySourceRateLimiter implements SourceRateLimiter {

    static final Duration WINDOW = Duration.ofMinutes(1);

    private final Clock clock;
    private final int limit;
    private final Map<String, Deque<Instant>> attempts = new ConcurrentHashMap<>();

    public InMemorySourceRateLimiter(Clock clock) {
        this(clock, MAX_ATTEMPTS_PER_MINUTE);
    }

    InMemorySourceRateLimiter(Clock clock, int limit) {
        this.clock = clock;
        this.limit = limit;
    }

    @Override
    public boolean tryAcquire(String source) {
        Instant now = clock.instant();
        Instant windowStart = now.minus(WINDOW);
        attempts.values().removeIf(times -> {
            synchronized (times) {
                return times.isEmpty() || !times.peekLast().isAfter(windowStart);
            }
        });
        Deque<Instant> times = attempts.computeIfAbsent(source, key -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && !times.peekFirst().isAfter(windowStart)) {
                times.pollFirst();
            }
            if (times.size() >= limit) {
                return false;
            }
            times.addLast(now);
            return true;
        }
    }
}
