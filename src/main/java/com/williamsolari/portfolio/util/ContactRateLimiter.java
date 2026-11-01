package com.williamsolari.portfolio.util;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class ContactRateLimiter {

    private static final int MAX_REQUESTS = 5;
    private static final Duration WINDOW = Duration.ofHours(1);

    private final Map<String, Deque<Instant>> requestsByIp = new ConcurrentHashMap<>();

    public boolean tryAcquire(String ip) {
        Deque<Instant> timestamps = requestsByIp.computeIfAbsent(ip, key -> new ArrayDeque<>());
        Instant now = Instant.now();
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && Duration.between(timestamps.peekFirst(), now).compareTo(WINDOW) > 0) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= MAX_REQUESTS) {
                return false;
            }
            timestamps.addLast(now);
            return true;
        }
    }
}
