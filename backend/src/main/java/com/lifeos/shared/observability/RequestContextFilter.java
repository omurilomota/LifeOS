package com.lifeos.shared.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Propagates a sanitized correlation id through logs, responses and error envelopes. */
@Component("lifeOsCorrelationFilter")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestContextFilter extends OncePerRequestFilter {
    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final Pattern UNSAFE = Pattern.compile("[^A-Za-z0-9._-]");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = sanitize(request.getHeader(REQUEST_ID_HEADER));
        boolean generatedTraceId = MDC.get("traceId") == null;

        MDC.put("requestId", requestId);
        if (generatedTraceId) MDC.put("traceId", requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove("requestId");
            if (generatedTraceId) MDC.remove("traceId");
        }
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) return UUID.randomUUID().toString();
        String safe = UNSAFE.matcher(value).replaceAll("");
        return safe.isEmpty() || safe.length() > 64 ? UUID.randomUUID().toString() : safe;
    }
}
