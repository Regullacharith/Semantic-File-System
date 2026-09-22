package com.sfs.engine.core;

import com.sfs.engine.level.AnalysisLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AnalysisJobRegistry")
class AnalysisJobRegistryTest {

    private static final Instant T0 = Instant.parse("2026-03-15T10:00:00Z");

    private final AnalysisJobRegistry registry = new AnalysisJobRegistry();

    @Test
    @DisplayName("created jobs are queued with sequential identifiers from the configured start")
    void creation() {
        AnalysisJob first = registry.create("sfs-obj-0001-a1b2c3d4", AnalysisLevel.STANDARD, T0);
        AnalysisJob second = registry.create("sfs-obj-0002-e5f6a7b8", AnalysisLevel.STANDARD, T0);

        assertThat(first.jobId()).isEqualTo("job-5001");
        assertThat(second.jobId()).isEqualTo("job-5002");
        assertThat(first.status()).isEqualTo(AnalysisJob.Status.QUEUED);
        assertThat(first.isTerminal()).isFalse();
    }

    @Test
    @DisplayName("transitions notify registered listeners with previous and current states")
    void transitions() {
        AtomicInteger transitions = new AtomicInteger();
        registry.addListener((previous, current) -> transitions.incrementAndGet());

        AnalysisJob job = registry.create("sfs-obj-0001-a1b2c3d4", AnalysisLevel.STANDARD, T0);
        registry.update(job.jobId(), current -> new AnalysisJob(
                current.jobId(), current.objectId(), current.level(),
                AnalysisJob.Status.RUNNING, current.submittedAt(), T0, null, null, false, null));

        assertThat(transitions.get()).isEqualTo(2);
        assertThat(registry.find(job.jobId()).orElseThrow().status())
                .isEqualTo(AnalysisJob.Status.RUNNING);
    }

    @Test
    @DisplayName("active-job detection covers queued and running jobs only")
    void activeJobDetection() {
        AnalysisJob job = registry.create("sfs-obj-0001-a1b2c3d4", AnalysisLevel.STANDARD, T0);
        assertThat(registry.hasActiveJob("sfs-obj-0001-a1b2c3d4")).isTrue();

        registry.update(job.jobId(), current -> new AnalysisJob(
                current.jobId(), current.objectId(), current.level(),
                AnalysisJob.Status.FAILED, current.submittedAt(), T0, T0,
                "boom", false, null));
        assertThat(registry.hasActiveJob("sfs-obj-0001-a1b2c3d4")).isFalse();
    }

    @Test
    @DisplayName("statistics count jobs by status")
    void statistics() {
        registry.create("sfs-obj-0001-a1b2c3d4", AnalysisLevel.STANDARD, T0);
        registry.create("sfs-obj-0002-e5f6a7b8", AnalysisLevel.STANDARD, T0);

        assertThat(registry.countsByStatus()).containsEntry(AnalysisJob.Status.QUEUED, 2L);
        assertThat(registry.all()).hasSize(2);
    }

    @Test
    @DisplayName("updates of unknown jobs are refused")
    void unknownJob() {
        assertThatThrownBy(() -> registry.update("job-9999", job -> job))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
