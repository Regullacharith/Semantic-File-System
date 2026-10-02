package com.sfs.reconstruction.engine;

import com.sfs.contracts.reconstruction.ReconstructionJobView;
import com.sfs.contracts.reconstruction.ReconstructionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Reconstruction job state machine (11.1)")
class ReconstructionJobTest {

    private static final Instant T0 = Instant.parse("2026-05-01T08:00:00Z");

    private ReconstructionJob queued() {
        return new ReconstructionJob("job-0001", "sfs-obj-3001-aaaabbbb", "plan.txt",
                ReconstructionStatus.QUEUED, "sfs-dna/0.2 v1", "pending",
                "model/0.1", T0, null, null, 0, List.of(), null, null, null);
    }

    @Test
    @DisplayName("queued jobs observe transitions into running")
    void queuedToRunning() {
        ReconstructionJob running = queued().running(T0.plusSeconds(1));

        assertThat(running.status()).isEqualTo(ReconstructionStatus.RUNNING);
        assertThat(running.terminal()).isFalse();
        assertThat(running.completedAt()).isNotNull();
    }

    @Test
    @DisplayName("a completed job names its artifact and carries verification and metadata")
    void completedShape() {
        VerificationResult verification = new VerificationResult(
                true, List.of(), List.of(), 1, 2, 1);
        ReconstructionMetadata metadata = new ReconstructionMetadata(
                "sfs-obj-3001-aaaabbbb", "sfs-dna/0.2 v1", "sha", "sfs-rules/0.2",
                "model/0.1", 1, 2, 3, 100, 40);
        ReconstructionJob completed = queued().completed(T0.plusSeconds(2),
                "plan.reconstructed.job-0001.txt", 40, List.of(), verification,
                metadata);

        assertThat(completed.status()).isEqualTo(ReconstructionStatus.COMPLETED);
        assertThat(completed.artifactName()).isNotBlank();
        assertThat(completed.verification()).isSameAs(verification);
        assertThat(completed.metadata()).isSameAs(metadata);
        assertThat(completed.toView().hasArtifact()).isTrue();
    }

    @Test
    @DisplayName("rejected and failed jobs never name an artifact")
    void refusalsNeverNameArtifacts() {
        VerificationResult verification = new VerificationResult(
                false, List.of("violation"), List.of(), 1, 0, 0);
        ReconstructionJob rejected = queued().rejected(T0.plusSeconds(1),
                List.of(new ReconstructionJobView.ConstraintFinding(
                        ReconstructionJobView.ConstraintFinding.Severity.VIOLATION,
                        "Constraint check", "violation")), verification);
        ReconstructionJob failed = queued().failed(T0.plusSeconds(1), "boom");

        assertThat(rejected.status()).isEqualTo(ReconstructionStatus.REJECTED);
        assertThat(rejected.toView().hasArtifact()).isFalse();
        assertThat(rejected.status()).isEqualTo(ReconstructionStatus.REJECTED);
        assertThat(failed.status()).isEqualTo(ReconstructionStatus.FAILED);
        assertThat(failed.failureReason()).isEqualTo("boom");
    }

    @Test
    @DisplayName("a refusal is terminal from the moment it is recorded")
    void refusedJobIsTerminal() {
        ReconstructionJob refused = ReconstructionJob.refused("job-0009",
                "sfs-obj-9999-ffffffff", null, T0, "No object exists.");

        assertThat(refused.terminal()).isTrue();
        assertThat(refused.status()).isEqualTo(ReconstructionStatus.FAILED);
        assertThat(refused.sourceName()).isEqualTo("unavailable");
        assertThat(refused.toView().failureReason()).contains("No object exists");
    }

    @Test
    @DisplayName("metadata measures knowledge preservation density")
    void metadataMeasuresDensity() {
        ReconstructionMetadata metadata = new ReconstructionMetadata(
                "sfs-obj-3001-aaaabbbb", "sfs-dna/0.2 v1", "sha", "sfs-rules/0.2",
                "model/0.1", 10, 20, 30, 200, 50);

        assertThat(metadata.knowledgePreservationDensity()).isEqualTo(4.0);
        assertThat(metadata.totalNanos()).isEqualTo(60);
    }

    @Test
    @DisplayName("verification results report satisfaction and counts")
    void verificationReports() {
        VerificationResult satisfied = VerificationResult.from(
                new com.sfs.reconstruction.ConstraintInterface.Result(
                        List.of(), List.of("w")), 2, 3, 1);
        VerificationResult violated = new VerificationResult(
                false, List.of("missing fact"), List.of(), 2, 3, 1);

        assertThat(satisfied.satisfied()).isTrue();
        assertThat(satisfied.checkedCriticalFacts()).isEqualTo(2);
        assertThat(satisfied.checkedEntities()).isEqualTo(3);
        assertThat(violated.satisfied()).isFalse();
        assertThat(violated.violations()).containsExactly("missing fact");
    }
}
