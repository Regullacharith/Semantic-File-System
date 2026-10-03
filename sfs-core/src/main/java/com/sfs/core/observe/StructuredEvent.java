package com.sfs.core.observe;

import java.time.Instant;
import java.util.Objects;

public record StructuredEvent(
        Instant timestamp,
        String level,
        String event,
        String traceId,
        String subject,
        String outcome,
        Long durationNanos,
        String detail) {

    public static final String SCHEMA = "sfs-log/0.1";

    public StructuredEvent {
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(event, "event must not be null");
        if (!level.matches("INFO|WARN|ERROR")) {
            throw new IllegalArgumentException("level must be INFO, WARN or ERROR");
        }
        if (event.isBlank()) {
            throw new IllegalArgumentException("event must not be blank");
        }
        if (traceId != null && !TraceId.isValid(traceId)) {
            throw new IllegalArgumentException("traceId must be a valid trace id");
        }
        if (durationNanos != null && durationNanos < 0) {
            throw new IllegalArgumentException("durationNanos must not be negative");
        }
    }

    public static StructuredEvent of(String level, String event, String outcome,
                                     String detail) {
        return new StructuredEvent(Instant.now(), level, event, null, null,
                outcome, null, detail);
    }

    public StructuredEvent withTrace(String traceId) {
        return new StructuredEvent(timestamp, level, event, traceId, subject,
                outcome, durationNanos, detail);
    }

    public StructuredEvent withSubject(String subject) {
        return new StructuredEvent(timestamp, level, event, traceId, subject,
                outcome, durationNanos, detail);
    }

    public StructuredEvent withDuration(long nanos) {
        return new StructuredEvent(timestamp, level, event, traceId, subject,
                outcome, nanos, detail);
    }

    public String toLogLine() {
        StringBuilder line = new StringBuilder();
        line.append("event=").append(event);
        line.append(" level=").append(level);
        if (traceId != null) {
            line.append(" traceId=").append(traceId);
        }
        if (subject != null) {
            line.append(" subject=").append(subject);
        }
        if (outcome != null) {
            line.append(" outcome=").append(outcome);
        }
        if (durationNanos != null) {
            line.append(" durationNanos=").append(durationNanos);
        }
        if (detail != null && !detail.isBlank()) {
            line.append(" detail=\"").append(detail.replace("\"", "'")).append('"');
        }
        return line.toString();
    }
}
