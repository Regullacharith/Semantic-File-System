package com.sfs.lifecycle.audit;

import com.sfs.lifecycle.model.LifecycleEvent;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public final class LifecycleAuditLog {

    private final List<LifecycleEvent> events = new CopyOnWriteArrayList<>();
    private final ConcurrentMap<String, List<LifecycleEvent>> eventsByObjectId =
            new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public LifecycleEvent append(LifecycleEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        String eventId = "sfs-lfe-%06d".formatted(sequence.incrementAndGet());
        LifecycleEvent identified = new LifecycleEvent(
                eventId, event.objectId(), event.type(), event.from(), event.to(),
                event.principalId(), event.refused(), event.reason(), event.at(),
                event.durationMs());
        events.add(identified);
        eventsByObjectId
                .computeIfAbsent(identified.objectId(), ignored -> new CopyOnWriteArrayList<>())
                .add(identified);
        return identified;
    }

    public String nextEventId() {
        return "sfs-lfe-%06d".formatted(sequence.get() + 1);
    }

    public List<LifecycleEvent> eventsFor(String objectId) {
        if (objectId == null || objectId.isBlank()) {
            return List.of();
        }
        return List.copyOf(eventsByObjectId.getOrDefault(objectId, List.of()));
    }

    public List<LifecycleEvent> all() {
        return events.stream()
                .sorted(Comparator.comparing(LifecycleEvent::at)
                        .thenComparing(LifecycleEvent::eventId))
                .toList();
    }

    public long totalEvents() {
        return events.size();
    }

    public Map<String, Long> countsByType() {
        return events.stream()
                .collect(Collectors.groupingBy(
                        event -> event.type().name(),
                        Collectors.counting()));
    }
}
