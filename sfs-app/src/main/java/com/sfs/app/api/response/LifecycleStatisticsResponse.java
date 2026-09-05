package com.sfs.app.api.response;

import com.sfs.contracts.lifecycle.LifecycleStatistics;

import java.util.Map;
import java.util.Objects;

public record LifecycleStatisticsResponse(
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

    public LifecycleStatisticsResponse {
        Objects.requireNonNull(eventCounts, "eventCounts must not be null");
        eventCounts = Map.copyOf(eventCounts);
    }

    public static LifecycleStatisticsResponse from(LifecycleStatistics statistics) {
        Objects.requireNonNull(statistics, "statistics must not be null");
        return new LifecycleStatisticsResponse(
                statistics.totalEvents(),
                statistics.eventCounts(),
                statistics.refusedEvents(),
                statistics.memoryCommitCount(),
                statistics.memoryCommitRefusedCount(),
                statistics.purgeCount(),
                statistics.purgeRefusedCount(),
                statistics.averageMemoryCommitMillis(),
                statistics.maxMemoryCommitMillis(),
                statistics.averagePurgeReleaseMillis(),
                statistics.maxPurgeReleaseMillis());
    }
}
