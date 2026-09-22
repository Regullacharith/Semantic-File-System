package com.sfs.engine.core;

import com.sfs.engine.level.AnalysisLevel;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record AnalysisJob(
        String jobId,
        String objectId,
        AnalysisLevel level,
        Status status,
        Instant submittedAt,
        Instant startedAt,
        Instant completedAt,
        String failureReason,
        boolean reusedPriorWork,
        Map<String, Long> stageDurationsMs) {

    public enum Status {
        QUEUED,
        RUNNING,
        COMPLETED,
        REUSED,
        FAILED,
        REJECTED
    }

    public AnalysisJob {
        Objects.requireNonNull(jobId, "jobId must not be null");
        Objects.requireNonNull(objectId, "objectId must not be null");
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(submittedAt, "submittedAt must not be null");
        if (status == Status.FAILED || status == Status.REJECTED) {
            if (failureReason == null || failureReason.isBlank()) {
                throw new IllegalArgumentException(
                        "a " + status + " job must carry a failure reason");
            }
        }
        stageDurationsMs = stageDurationsMs == null
                ? Map.of()
                : Map.copyOf(stageDurationsMs);
    }

    public boolean isTerminal() {
        return status == Status.COMPLETED || status == Status.REUSED
                || status == Status.FAILED || status == Status.REJECTED;
    }

    public long totalStageMillis() {
        return stageDurationsMs.values().stream().mapToLong(Long::longValue).sum();
    }
}
