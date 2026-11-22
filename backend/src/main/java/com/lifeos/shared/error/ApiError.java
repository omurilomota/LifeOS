package com.lifeos.shared.error;

import java.time.Instant;
import java.util.List;

public record ApiError(
        String code,
        String message,
        List<ApiErrorDetail> details,
        String traceId,
        Instant timestamp
) {
    public record ApiErrorDetail(String field, String message) {}
}
