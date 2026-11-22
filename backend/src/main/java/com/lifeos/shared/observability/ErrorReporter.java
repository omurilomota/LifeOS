package com.lifeos.shared.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Hook for external error-reporting integrations. Disabled by default; when enabled it emits a
 * structured error event without payloads, tokens or document contents.
 */
@Component
public class ErrorReporter {
    private static final Logger log = LoggerFactory.getLogger(ErrorReporter.class);
    private final boolean enabled;

    public ErrorReporter(@Value("${lifeos.observability.error-reporter.enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    public void report(Throwable error, String code, String path, String traceId, String requestId) {
        if (!enabled) return;
        log.error("error_report traceId={} requestId={} code={} path={}", traceId, requestId, code, path, error);
    }
}
