# Observability

LifeOS emits three signals without logging sensitive data: structured logs, metrics and optional distributed traces.

## Request correlation

`RequestContextFilter` (highest precedence) accepts `X-Request-ID` from the caller, sanitizes it (allowlist `[A-Za-z0-9._-]`, max 64 chars) and falls back to a UUID. It puts `requestId` and `traceId` in MDC, echoes the sanitized value back in the `X-Request-ID` response header, and clears MDC afterwards. When Micrometer/OTel has already set `traceId`, that value is preserved.

`GlobalExceptionHandler` reuses the MDC `traceId` in the error envelope, so a log line, the HTTP response and an upstream trace all share the same id.

## Logs

- `local` and `test` profiles: human-readable console pattern.
- All other profiles: one JSON object per line via `logstash-logback-encoder`, including MDC fields and `app` from `spring.application.name`.

Never log secrets, tokens, document contents, raw prompts or personal data. The correlation filter sanitizes caller-supplied ids precisely to prevent log injection.

## Metrics

Actuator exposes `health`, `info`, `metrics` and `prometheus` at `/actuator`. Scrape `/actuator/prometheus`.

Application counters (all low-cardinality):

| Metric | Tags | Meaning |
|---|---|---|
| `lifeos.auth.failures` | `reason` (`unknown_email`, `bad_password`) | Failed login attempts |
| `lifeos.reminder.deliveries` | `outcome` (`delivered`, `deduplicated`, `cancelled`) | Reminder worker results |
| `lifeos.document.uploads` | `outcome` (`stored`, `rejected`, `storage_error`) | Upload results |
| `lifeos.ai.suggestion.reviews` | `action` (`confirmed`, `rejected`) | Human-in-the-loop decisions |

Standard JVM, HTTP, datasource and logback metrics come from Actuator/Micrometer.

## Tracing

`micrometer-tracing-bridge-otel` + `opentelemetry-exporter-otlp` are on the classpath. Tracing is **off by default** (`TRACING_SAMPLING_PROBABILITY=0.0`). To enable:

```bash
export TRACING_SAMPLING_PROBABILITY=1.0
export MANAGEMENT_OTLP_TRACING_ENDPOINT=http://otel-collector:4318/v1/traces
```

## Error reporting

`ErrorReporter` is an integration hook, disabled by default (`ERROR_REPORTER_ENABLED=false`). When enabled it emits one structured ERROR event per 5xx with `traceId`, `requestId`, `code` and `path` — no exception message, payload or personal data. Replace it with a provider-specific implementation if desired.

## Verification

`ObservabilityIntegrationTest` checks `X-Request-ID` propagation/sanitization and that `/actuator/prometheus` is available. Run with:

```bash
cd backend && mvn test
```
