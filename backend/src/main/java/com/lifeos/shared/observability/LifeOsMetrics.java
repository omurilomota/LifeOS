package com.lifeos.shared.observability;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/** Application-level counters. Tags are bounded to low-cardinality outcomes. */
@Component
public class LifeOsMetrics {
    private final MeterRegistry registry;

    public LifeOsMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void authFailure(String reason) {
        registry.counter("lifeos.auth.failures", "reason", reason).increment();
    }

    public void reminder(String outcome) {
        registry.counter("lifeos.reminder.deliveries", "outcome", outcome).increment();
    }

    public void documentUpload(String outcome) {
        registry.counter("lifeos.document.uploads", "outcome", outcome).increment();
    }

    public void aiReview(String action) {
        registry.counter("lifeos.ai.suggestion.reviews", "action", action).increment();
    }
}
