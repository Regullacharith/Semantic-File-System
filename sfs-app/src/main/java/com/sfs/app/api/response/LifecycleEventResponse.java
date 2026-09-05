package com.sfs.app.api.response;

import com.sfs.contracts.lifecycle.LifecycleAuditEntry;

import java.time.Instant;
import java.util.Objects;

public record LifecycleEventResponse(
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

    public LifecycleEventResponse {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(objectId, "objectId must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(at, "at must not be null");
    }

    public static LifecycleEventResponse from(LifecycleAuditEntry entry) {
        Objects.requireNonNull(entry, "entry must not be null");
        return new LifecycleEventResponse(
                entry.eventId(),
                entry.objectId(),
                entry.type(),
                entry.fromState(),
                entry.toState(),
                entry.principalId(),
                entry.refused(),
                entry.reason(),
                entry.at(),
                entry.durationMs());
    }
}
