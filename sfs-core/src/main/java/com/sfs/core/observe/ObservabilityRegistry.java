package com.sfs.core.observe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

public final class ObservabilityRegistry {

    public static final String METRICS_SCHEMA = "sfs-metrics/0.1";
    private static final int RING_CAPACITY = 200;

    private final ConcurrentLinkedDeque<StructuredEvent> events =
            new ConcurrentLinkedDeque<>();
    private final AtomicLong droppedEvents = new AtomicLong();
    private final Map<String, StageAggregate> stages = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
    private final AtomicLong counterReads = new AtomicLong();

    public void record(StructuredEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        while (events.size() >= RING_CAPACITY) {
            if (events.pollFirst() == null) {
                break;
            }
            droppedEvents.incrementAndGet();
        }
        events.addLast(event);
    }

    public void recordStage(String stage, long nanos) {
        if (stage == null || stage.isBlank() || nanos < 0) {
            throw new IllegalArgumentException(
                    "stage must be named and nanos must not be negative");
        }
        stages.computeIfAbsent(stage, name -> new StageAggregate())
                .accumulate(nanos);
    }

    public void increment(String counter) {
        if (counter == null || counter.isBlank()) {
            throw new IllegalArgumentException("counter must be named");
        }
        counters.computeIfAbsent(counter, name -> new AtomicLong()).incrementAndGet();
    }

    public long counterValue(String counter) {
        AtomicLong value = counters.get(counter);
        return value == null ? 0L : value.get();
    }

    public List<StructuredEvent> recentEvents() {
        return List.copyOf(events);
    }

    public long droppedEvents() {
        return droppedEvents.get();
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("schema", METRICS_SCHEMA);
        Map<String, Object> countersView = new LinkedHashMap<>();
        counterReads.incrementAndGet();
        for (Map.Entry<String, AtomicLong> entry : counters.entrySet()) {
            countersView.put(entry.getKey(), entry.getValue().get());
        }
        metrics.put("counters", countersView);
        Map<String, Object> stagesView = new LinkedHashMap<>();
        for (Map.Entry<String, StageAggregate> entry : stages.entrySet()) {
            stagesView.put(entry.getKey(), entry.getValue().view());
        }
        metrics.put("stages", stagesView);
        metrics.put("recentEvents", events.size());
        metrics.put("droppedEvents", droppedEvents.get());
        return metrics;
    }

    private static final class StageAggregate {

        private final AtomicLong count = new AtomicLong();
        private final AtomicLong totalNanos = new AtomicLong();
        private final AtomicLong maxNanos = new AtomicLong();

        void accumulate(long nanos) {
            count.incrementAndGet();
            totalNanos.addAndGet(nanos);
            maxNanos.updateAndGet(current -> Math.max(current, nanos));
        }

        Map<String, Object> view() {
            long recorded = count.get();
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("count", recorded);
            view.put("totalNanos", totalNanos.get());
            view.put("maxNanos", maxNanos.get());
            view.put("meanNanos", recorded == 0 ? 0L : totalNanos.get() / recorded);
            return view;
        }
    }
}
