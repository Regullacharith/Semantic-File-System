package com.sfs.contracts.lifecycle;

import java.util.Map;
import java.util.Objects;

public record LifecycleStatistics(
        long totalEvents,
        Map<String, Long> eventCounts,
        long refusedEvents,
        long memoryCommitCount,
        long memoryCommitRefusedCount,
        long purgeCount,
        long purgeRefusedCount,
        Double averageMemoryCommitMillis,
        Long maxMemoryCommitMillis,
        Double averagePurgeReleaseMillis,
        Long maxPurgeReleaseMillis) {

    public LifecycleStatistics {
        Objects.requireNonNull(eventCounts, "eventCounts must not be null");
        eventCounts = Map.copyOf(eventCounts);
        if (totalEvents < 0 || refusedEvents < 0 || memoryCommitCount < 0
                || memoryCommitRefusedCount < 0 || purgeCount < 0 || purgeRefusedCount < 0) {
            throw new IllegalArgumentException("counts must not be negative");
        }
        if (averageMemoryCommitMillis != null && averageMemoryCommitMillis < 0) {
            throw new IllegalArgumentException("averageMemoryCommitMillis must not be negative");
        }
        if (maxMemoryCommitMillis != null && maxMemoryCommitMillis < 0) {
            throw new IllegalArgumentException("maxMemoryCommitMillis must not be negative");
        }
        if (averagePurgeReleaseMillis != null && averagePurgeReleaseMillis < 0) {
            throw new IllegalArgumentException("averagePurgeReleaseMillis must not be negative");
        }
        if (maxPurgeReleaseMillis != null && maxPurgeReleaseMillis < 0) {
            throw new IllegalArgumentException("maxPurgeReleaseMillis must not be negative");
        }
    }
}
