package com.sfs.contracts.lifecycle;

import java.time.Instant;
import java.util.Objects;

public record LifecycleAuditEntry(
        String eventId,
        String objectId,
        String type,
        String fromState,
        String toState,
        String principalId,
        boolean refused,
        String reason,
        Instant at,
        Long durationMs) {

    public LifecycleAuditEntry {
        Objects.requireNonNull(eventId, "eventId must not be null");
        if (eventId.isBlank()) {
            throw new IllegalArgumentException("eventId must not be blank");
        }
        Objects.requireNonNull(objectId, "objectId must not be null");
        if (objectId.isBlank()) {
            throw new IllegalArgumentException("objectId must not be blank");
        }
        Objects.requireNonNull(type, "type must not be null");
        if (type.isBlank()) {
            throw new IllegalArgumentException("type must not be blank");
        }
        Objects.requireNonNull(toState, "toState must not be null");
        Objects.requireNonNull(at, "at must not be null");
        if (refused && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("a refusal must carry a reason");
        }
        if (durationMs != null && durationMs < 0) {
            throw new IllegalArgumentException("durationMs must not be negative");
        }
    }
}
