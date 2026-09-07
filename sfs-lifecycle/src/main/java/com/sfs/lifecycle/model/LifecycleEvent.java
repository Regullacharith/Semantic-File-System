package com.sfs.lifecycle.model;

import com.sfs.lifecycle.state.FileState;

import java.time.Instant;
import java.util.Objects;

public record LifecycleEvent(
        String eventId,
        String objectId,
        LifecycleEventType type,
        FileState from,
        FileState to,
        String principalId,
        boolean refused,
        String reason,
        Instant at,
        Long durationMs) {

    public LifecycleEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        if (eventId.isBlank()) {
            throw new IllegalArgumentException("eventId must not be blank");
        }
        Objects.requireNonNull(objectId, "objectId must not be null");
        if (objectId.isBlank()) {
            throw new IllegalArgumentException("objectId must not be blank");
        }
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(to, "to must not be null");
        if (from == null && type != LifecycleEventType.REGISTRATION_RECORDED) {
            throw new IllegalArgumentException(
                    "from may be null only for " + LifecycleEventType.REGISTRATION_RECORDED);
        }
        if (principalId != null && principalId.isBlank()) {
            throw new IllegalArgumentException("principalId must not be blank when present");
        }
        if (refused && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("a refusal must carry a reason");
        }
        if (!refused && reason != null && reason.isBlank()) {
            throw new IllegalArgumentException("reason must not be blank when present");
        }
        Objects.requireNonNull(at, "at must not be null");
        if (durationMs != null && durationMs < 0) {
            throw new IllegalArgumentException("durationMs must not be negative");
        }
    }

    public static LifecycleEvent transition(String eventId, String objectId,
                                            LifecycleEventType type, FileState from,
                                            FileState to, String principalId, Instant at) {
        return new LifecycleEvent(eventId, objectId, type, from, to, principalId,
                false, null, at, null);
    }

    public static LifecycleEvent refusal(String eventId, String objectId,
                                         LifecycleEventType type, FileState current,
                                         String principalId, String reason, Instant at) {
        return new LifecycleEvent(eventId, objectId, type, current, current, principalId,
                true, reason, at, null);
    }
}
