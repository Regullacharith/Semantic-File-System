package com.sfs.lifecycle.audit;

import com.sfs.contracts.lifecycle.LifecycleAuditEntry;
import com.sfs.contracts.lifecycle.LifecycleAuditService;
import com.sfs.contracts.lifecycle.LifecycleStatistics;
import com.sfs.lifecycle.model.LifecycleEvent;
import com.sfs.lifecycle.model.LifecycleEventType;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class LifecycleAuditAdapter implements LifecycleAuditService {

    private final LifecycleAuditLog auditLog;

    public LifecycleAuditAdapter(LifecycleAuditLog auditLog) {
        this.auditLog = Objects.requireNonNull(auditLog, "auditLog must not be null");
    }

    @Override
    public List<LifecycleAuditEntry> eventsFor(String objectId) {
        return auditLog.eventsFor(objectId).stream()
                .map(LifecycleAuditAdapter::toEntry)
                .toList();
    }

    @Override
    public LifecycleStatistics statistics() {
        List<LifecycleEvent> events = auditLog.all();
        Map<String, Long> counts = auditLog.countsByType();
        List<Long> commitDurations = events.stream()
                .filter(event -> event.type() == LifecycleEventType.MEMORY_COMMITTED)
                .map(LifecycleEvent::durationMs)
                .filter(Objects::nonNull)
                .toList();
        Double average = commitDurations.isEmpty()
                ? null
                : commitDurations.stream().mapToLong(Long::longValue).average().orElse(0);
        Long max = commitDurations.isEmpty()
                ? null
                : commitDurations.stream().mapToLong(Long::longValue).max().orElse(0);
        List<Long> releaseDurations = events.stream()
                .filter(event -> event.type() == LifecycleEventType.RAW_RELEASED)
                .map(LifecycleEvent::durationMs)
                .filter(Objects::nonNull)
                .toList();
        Double releaseAverage = releaseDurations.isEmpty()
                ? null
                : releaseDurations.stream().mapToLong(Long::longValue).average().orElse(0);
        Long releaseMax = releaseDurations.isEmpty()
                ? null
                : releaseDurations.stream().mapToLong(Long::longValue).max().orElse(0);

        return new LifecycleStatistics(
                auditLog.totalEvents(),
                counts,
                events.stream().filter(LifecycleEvent::refused).count(),
                count(events, LifecycleEventType.MEMORY_COMMITTED),
                count(events, LifecycleEventType.MEMORY_COMMIT_REFUSED),
                count(events, LifecycleEventType.RAW_RELEASED),
                count(events, LifecycleEventType.PURGE_REFUSED),
                average,
                max,
                releaseAverage,
                releaseMax);
    }

    private static long count(List<LifecycleEvent> events, LifecycleEventType type) {
        return events.stream().filter(event -> event.type() == type).count();
    }

    private static LifecycleAuditEntry toEntry(LifecycleEvent event) {
        return new LifecycleAuditEntry(
                event.eventId(),
                event.objectId(),
                event.type().name(),
                event.from() == null ? null : event.from().name(),
                event.to().name(),
                event.principalId(),
                event.refused(),
                event.reason(),
                event.at(),
                event.durationMs());
    }
}
