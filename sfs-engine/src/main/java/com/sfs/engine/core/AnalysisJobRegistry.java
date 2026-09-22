package com.sfs.engine.core;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.UnaryOperator;

public final class AnalysisJobRegistry {

    private final ConcurrentMap<String, AnalysisJob> jobsById = new ConcurrentHashMap<>();
    private final List<AnalysisJobListener> listeners = new CopyOnWriteArrayList<>();
    private final AtomicLong sequence;
    private final int firstSequenceValue;

    public AnalysisJobRegistry() {
        this(5001);
    }

    public AnalysisJobRegistry(int firstSequenceValue) {
        if (firstSequenceValue < 1) {
            throw new IllegalArgumentException("firstSequenceValue must be positive");
        }
        this.firstSequenceValue = firstSequenceValue;
        this.sequence = new AtomicLong(firstSequenceValue);
    }

    public void addListener(AnalysisJobListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener must not be null"));
    }

    public AnalysisJob create(String objectId, com.sfs.engine.level.AnalysisLevel level,
                              java.time.Instant at) {
        String jobId = "job-%04d".formatted(sequence.getAndIncrement());
        AnalysisJob job = new AnalysisJob(jobId, objectId, level,
                AnalysisJob.Status.QUEUED, at, null, null, null, false, null);
        jobsById.put(jobId, job);
        notifyListeners(null, job);
        return job;
    }

    public AnalysisJob update(String jobId, UnaryOperator<AnalysisJob> transition) {
        AnalysisJob previous = jobsById.get(jobId);
        if (previous == null) {
            throw new IllegalArgumentException("unknown analysis job " + jobId);
        }
        AnalysisJob updated = transition.apply(previous);
        jobsById.put(jobId, updated);
        notifyListeners(previous, updated);
        return updated;
    }

    public Optional<AnalysisJob> find(String jobId) {
        if (jobId == null || jobId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(jobsById.get(jobId));
    }

    public boolean hasActiveJob(String objectId) {
        return jobsById.values().stream()
                .anyMatch(job -> job.objectId().equals(objectId)
                        && (job.status() == AnalysisJob.Status.QUEUED
                        || job.status() == AnalysisJob.Status.RUNNING));
    }

    public List<AnalysisJob> all() {
        return List.copyOf(jobsById.values());
    }

    public Map<AnalysisJob.Status, Long> countsByStatus() {
        return jobsById.values().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        AnalysisJob::status,
                        java.util.stream.Collectors.counting()));
    }

    private void notifyListeners(AnalysisJob previous, AnalysisJob current) {
        for (AnalysisJobListener listener : listeners) {
            listener.onTransition(previous, current);
        }
    }

    public int firstSequenceValue() {
        return firstSequenceValue;
    }
}
