package com.sfs.core.observe;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Metrics registry (sfs-metrics/0.1)")
class ObservabilityRegistryTest {

    @Test
    @DisplayName("stage aggregates keep count, total, max and mean")
    void stageAggregation() {
        ObservabilityRegistry registry = new ObservabilityRegistry();

        registry.recordStage("search", 100L);
        registry.recordStage("search", 300L);
        registry.recordStage("reconstruction", 50L);

        Map<String, Object> snapshot = registry.snapshot();
        assertThat(snapshot.get("schema")).isEqualTo("sfs-metrics/0.1");

        @SuppressWarnings("unchecked")
        Map<String, Object> stages = (Map<String, Object>) snapshot.get("stages");
        @SuppressWarnings("unchecked")
        Map<String, Object> search = (Map<String, Object>) stages.get("search");
        assertThat(search.get("count")).isEqualTo(2L);
        assertThat(search.get("totalNanos")).isEqualTo(400L);
        assertThat(search.get("maxNanos")).isEqualTo(300L);
        assertThat(search.get("meanNanos")).isEqualTo(200L);
    }

    @Test
    @DisplayName("counters accumulate and unknown counters read zero")
    void counters() {
        ObservabilityRegistry registry = new ObservabilityRegistry();

        registry.increment("requests.served");
        registry.increment("requests.served");
        registry.increment("requests.refused");

        assertThat(registry.counterValue("requests.served")).isEqualTo(2L);
        assertThat(registry.counterValue("requests.refused")).isEqualTo(1L);
        assertThat(registry.counterValue("never.recorded")).isZero();

        Map<String, Object> snapshot = registry.snapshot();
        @SuppressWarnings("unchecked")
        Map<String, Object> counters = (Map<String, Object>) snapshot.get("counters");
        assertThat(counters).containsEntry("requests.served", 2L);
    }

    @Test
    @DisplayName("the event ring keeps the most recent events and drops the oldest")
    void ringBuffer() {
        ObservabilityRegistry registry = new ObservabilityRegistry();

        for (int i = 0; i < 250; i++) {
            registry.record(StructuredEvent.of("INFO", "tick", "OK",
                    "tick-" + i));
        }

        assertThat(registry.recentEvents()).hasSize(200);
        assertThat(registry.recentEvents().getFirst().detail())
                .isEqualTo("tick-50");
        assertThat(registry.recentEvents().getLast().detail()).isEqualTo("tick-249");
        assertThat(registry.droppedEvents()).isEqualTo(50L);
        assertThat(registry.snapshot().get("recentEvents")).isEqualTo(200);
    }

    @Test
    @DisplayName("invalid stage and counter names are refused")
    void validation() {
        ObservabilityRegistry registry = new ObservabilityRegistry();

        assertThatThrownBy(() -> registry.recordStage(" ", 1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> registry.recordStage("search", -1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> registry.increment(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> registry.record(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("an empty registry snapshots empty structures without errors")
    void emptySnapshot() {
        ObservabilityRegistry registry = new ObservabilityRegistry();

        Map<String, Object> snapshot = registry.snapshot();

        assertThat(snapshot.get("schema")).isEqualTo("sfs-metrics/0.1");
        assertThat((Map<?, ?>) snapshot.get("counters")).isEmpty();
        assertThat((Map<?, ?>) snapshot.get("stages")).isEmpty();
        assertThat(snapshot.get("recentEvents")).isEqualTo(0);
    }
}
