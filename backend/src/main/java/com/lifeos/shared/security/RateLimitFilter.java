package com.lifeos.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private static final int LIMIT = 120;
    private static final long WINDOW_MS = 60_000;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String key = request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
        Window w = windows.computeIfAbsent(key, k -> new Window());
        long now = Instant.now().toEpochMilli();
        synchronized (w) {
            if (now - w.start >= WINDOW_MS) { w.start = now; w.count.set(0); }
            if (w.count.incrementAndGet() > LIMIT) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"code\":\"RATE_LIMITED\",\"message\":\"Too many requests\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private static class Window { long start = Instant.now().toEpochMilli(); AtomicInteger count = new AtomicInteger(0); }
}
