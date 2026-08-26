package com.sfs.app.service;

import com.sfs.contracts.reconstruction.ReconstructionJobView;
import com.sfs.contracts.reconstruction.ReconstructionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Job registry")
class JobRegistryTest {

    private JobRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new JobRegistry();
    }

    private ReconstructionJobView job(String jobId, ReconstructionStatus status) {
        return new ReconstructionJobView(
                jobId, "sfs-obj-0001-a1b2c3d4", "research-summary.txt", status,
                "sfs-dna/0.1 v1", "sfs-rules/0.1", "deterministic-baseline/0.1",
                Instant.parse("2026-01-01T00:00:00Z"),
                status.isTerminal() ? Instant.parse("2026-01-01T00:00:05Z") : null,
                status == ReconstructionStatus.COMPLETED ? "artifact.txt" : null,
                status == ReconstructionStatus.COMPLETED ? 512L : 0L,
                List.of(),
                status == ReconstructionStatus.REJECTED ? "Refused on policy grounds." : null);
    }

    @Test
    @DisplayName("records a submitted job so it can be queried")
    void recordsSubmittedJob() {
        registry.record(job("job-0001", ReconstructionStatus.COMPLETED));

        assertThat(registry.find("job-0001")).isPresent();
        assertThat(registry.find("job-0001").orElseThrow().status()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("reports an unknown job as absent")
    void reportsUnknownJobAsAbsent() {
        assertThat(registry.find("job-9999")).isEmpty();
    }

    @Test
    @DisplayName("preserves the refusal reason on a rejected job")
    void preservesRefusalReason() {
        registry.record(job("job-0002", ReconstructionStatus.REJECTED));

        assertThat(registry.find("job-0002").orElseThrow().terminationReason())
                .contains("Refused");
    }

    @Test
    @DisplayName("marks a non-terminal job failed rather than letting it vanish")
    void marksOrphanedJobFailed() {
        registry.record(job("job-0003", ReconstructionStatus.RUNNING));

        int failed = registry.failOrphanedJobs();

        assertThat(failed).isEqualTo(1);

        JobRegistry.JobRecord record = registry.find("job-0003").orElseThrow();
        assertThat(record.status()).isEqualTo("FAILED");
        assertThat(record.isTerminal()).isTrue();
    }

    @Test
    @DisplayName("explains why an orphaned job was failed")
    void explainsOrphanedFailure() {
        registry.record(job("job-0003", ReconstructionStatus.RUNNING));
        registry.failOrphanedJobs();

        assertThat(registry.find("job-0003").orElseThrow().terminationReason())
                .contains("did not reach a terminal state")
                .contains("Milestone 08");
    }

    @Test
    @DisplayName("leaves an already terminal job untouched")
    void leavesTerminalJobUntouched() {
        registry.record(job("job-0001", ReconstructionStatus.COMPLETED));
        registry.record(job("job-0002", ReconstructionStatus.REJECTED));

        assertThat(registry.failOrphanedJobs()).isZero();
        assertThat(registry.find("job-0001").orElseThrow().status()).isEqualTo("COMPLETED");
        assertThat(registry.find("job-0002").orElseThrow().status()).isEqualTo("REJECTED");
    }

    @Test
    @DisplayName("never silently discards a job")
    void neverSilentlyDiscardsAJob() {
        registry.record(job("job-0001", ReconstructionStatus.COMPLETED));
        registry.record(job("job-0003", ReconstructionStatus.RUNNING));

        registry.failOrphanedJobs();

        assertThat(registry.list()).hasSize(2);
        assertThat(registry.list()).allSatisfy(record ->
                assertThat(record.isTerminal()).isTrue());
    }

    @Test
    @DisplayName("lists every recorded job")
    void listsEveryJob() {
        registry.record(job("job-0001", ReconstructionStatus.COMPLETED));
        registry.record(job("job-0002", ReconstructionStatus.REJECTED));

        assertThat(registry.list()).hasSize(2);
    }
}
