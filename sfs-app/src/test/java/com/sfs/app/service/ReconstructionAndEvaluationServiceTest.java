package com.sfs.app.service;

import com.sfs.app.api.error.ApiErrorCode;
import com.sfs.app.api.request.ReconstructionApiRequest;
import com.sfs.app.api.response.FidelityReportResponse;
import com.sfs.app.api.response.JobStatusResponse;
import com.sfs.contracts.evaluation.EvaluationAvailability;
import com.sfs.contracts.evaluation.EvaluationService;
import com.sfs.contracts.evaluation.FidelityDimension;
import com.sfs.contracts.evaluation.FidelityReportView;
import com.sfs.contracts.reconstruction.ReconstructionArtifact;
import com.sfs.contracts.reconstruction.ReconstructionJobView;
import com.sfs.contracts.reconstruction.ReconstructionService;
import com.sfs.contracts.reconstruction.ReconstructionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Reconstruction and evaluation services")
class ReconstructionAndEvaluationServiceTest {

    private static final String COMPLETED_JOB = "job-0001";
    private static final String REJECTED_JOB = "job-0002";
    private static final String OBJECT_ID = "sfs-obj-0001-a1b2c3d4";

    private ReconstructionApplicationService reconstruction;
    private EvaluationApplicationService evaluation;

    @BeforeEach
    void setUp() {
        StubReconstructionService reconstructionService = new StubReconstructionService();
        reconstruction = new ReconstructionApplicationService(reconstructionService);
        evaluation = new EvaluationApplicationService(new StubEvaluationService());
    }

    @Nested
    @DisplayName("job identifiers")
    class JobIdentifiers {

        @ParameterizedTest
        @ValueSource(strings = {
                "not-a-job",
                "job-1",
                "job-00001",
                "../../etc/passwd",
                "job-0001; DROP TABLE jobs",
                "job-0001\r\nX-Injected: yes"})
        @DisplayName("rejects a malformed job ID with a deterministic code")
        void rejectsMalformedJobId(String jobId) {
            assertThatThrownBy(() -> reconstruction.getJob(jobId))
                    .isInstanceOf(ApplicationException.class)
                    .extracting(e -> ((ApplicationException) e).errorCode())
                    .isEqualTo(ApiErrorCode.JOB_ID_INVALID);
        }

        @Test
        @DisplayName("reports an unknown but well-formed job as not found")
        void reportsUnknownJob() {
            assertThatThrownBy(() -> reconstruction.getJob("job-9999"))
                    .isInstanceOf(ApplicationException.class)
                    .extracting(e -> ((ApplicationException) e).errorCode())
                    .isEqualTo(ApiErrorCode.JOB_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("reconstruction")
    class Reconstruction {

        @Test
        @DisplayName("starts a job from an explicit request")
        void startsJob() {
            JobStatusResponse response =
                    reconstruction.startReconstruction(new ReconstructionApiRequest(OBJECT_ID));

            assertThat(response.jobId()).isEqualTo(COMPLETED_JOB);
            assertThat(response.provenance().dnaVersion()).isNotBlank();
        }

        @Test
        @DisplayName("exposes provenance on every job")
        void exposesProvenance() {
            JobStatusResponse job = reconstruction.getJob(COMPLETED_JOB);

            assertThat(job.provenance().dnaVersion()).isNotBlank();
            assertThat(job.provenance().rulesVersion()).isNotBlank();
            assertThat(job.provenance().modelVersion()).isNotBlank();
        }

        @Test
        @DisplayName("marks a refusal as refused rather than failed")
        void marksRefusalDistinctly() {
            JobStatusResponse job = reconstruction.getJob(REJECTED_JOB);

            assertThat(job.status()).isEqualTo("REJECTED");
            assertThat(job.refused()).isTrue();
            assertThat(job.hasArtifact()).isFalse();
        }

        @Test
        @DisplayName("returns an artifact only for a completed job")
        void returnsArtifactForCompletedJob() {
            ReconstructionArtifact artifact = reconstruction.getArtifact(COMPLETED_JOB);

            assertThat(artifact.content()).contains("NOT THE ORIGINAL FILE");
        }

        @Test
        @DisplayName("refuses to serve an artifact for a rejected job")
        void refusesArtifactForRejectedJob() {
            assertThatThrownBy(() -> reconstruction.getArtifact(REJECTED_JOB))
                    .isInstanceOf(ApplicationException.class)
                    .extracting(e -> ((ApplicationException) e).errorCode())
                    .isEqualTo(ApiErrorCode.ARTIFACT_NOT_AVAILABLE);
        }
    }

    @Nested
    @DisplayName("evaluation")
    class Evaluation {

        @Test
        @DisplayName("returns a report with every dimension scored")
        void returnsFullReport() {
            FidelityReportResponse report = evaluation.getEvaluation(COMPLETED_JOB);

            assertThat(report.availability()).isEqualTo("AVAILABLE");
            assertThat(report.dimensions()).hasSize(FidelityDimension.values().length);
            assertThat(report.dimensions()).allSatisfy(d -> assertThat(d.score()).isNotNull());
        }

        @Test
        @DisplayName("reports critical facts separately from the factual score")
        void reportsCriticalFactsSeparately() {
            FidelityReportResponse report = evaluation.getEvaluation(COMPLETED_JOB);

            assertThat(report.criticalFactsPreserved()).isEqualTo(2);
            assertThat(report.criticalFactsTotal()).isEqualTo(3);
        }

        @Test
        @DisplayName("gives a reason and no scores when nothing was measured")
        void explainsUnmeasuredEvaluation() {
            FidelityReportResponse report = evaluation.getEvaluation(REJECTED_JOB);

            assertThat(report.availability()).isEqualTo("NO_ARTIFACT");
            assertThat(report.reason()).isNotBlank();
            assertThat(report.dimensions()).isEmpty();
            assertThat(report.criticalFactsPreserved()).isNull();
        }

        @Test
        @DisplayName("reports a never-evaluated job as not found")
        void reportsNotEvaluatedAsNotFound() {
            assertThatThrownBy(() -> evaluation.getEvaluation("job-9999"))
                    .isInstanceOf(ApplicationException.class)
                    .extracting(e -> ((ApplicationException) e).errorCode())
                    .isEqualTo(ApiErrorCode.EVALUATION_NOT_FOUND);
        }
    }

    private static ReconstructionJobView completedJob() {
        return new ReconstructionJobView(
                COMPLETED_JOB, OBJECT_ID, "research-summary.txt", ReconstructionStatus.COMPLETED,
                "sfs-dna/0.1 v1", "sfs-rules/0.1", "deterministic-baseline/0.1",
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:05Z"),
                "research-summary.reconstructed.job-0001.txt", 512L, List.of(), null);
    }

    private static ReconstructionJobView rejectedJob() {
        return new ReconstructionJobView(
                REJECTED_JOB, "sfs-obj-0004-b3c4d5e6", "deployment-config.txt",
                ReconstructionStatus.REJECTED,
                "sfs-dna/0.1 v1", "sfs-rules/0.1", "deterministic-baseline/0.1",
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:01Z"),
                null, 0L,
                List.of(new ReconstructionJobView.ConstraintFinding(
                        ReconstructionJobView.ConstraintFinding.Severity.VIOLATION,
                        "protected-reference",
                        "Reconstruction would expose a protected reference.")),
                "Refused: protected references cannot be resolved without authorization.");
    }

    private static final class StubReconstructionService implements ReconstructionService {

        @Override
        public ReconstructionJobView requestReconstruction(String objectId) {
            return completedJob();
        }

        @Override
        public Optional<ReconstructionJobView> findJob(String jobId) {
            return switch (jobId) {
                case COMPLETED_JOB -> Optional.of(completedJob());
                case REJECTED_JOB -> Optional.of(rejectedJob());
                default -> Optional.empty();
            };
        }

        @Override
        public List<ReconstructionJobView> listJobs() {
            return List.of(completedJob(), rejectedJob());
        }

        @Override
        public Optional<ReconstructionArtifact> findArtifact(String jobId) {
            if (!COMPLETED_JOB.equals(jobId)) {
                return Optional.empty();
            }
            return Optional.of(new ReconstructionArtifact(
                    jobId, "research-summary.reconstructed.job-0001.txt",
                    "NOT THE ORIGINAL FILE\n\nReconstructed content.", "text/plain;charset=UTF-8"));
        }
    }

    private static final class StubEvaluationService implements EvaluationService {

        @Override
        public EvaluationAvailability findEvaluation(String jobId) {
            return switch (jobId) {
                case COMPLETED_JOB -> EvaluationAvailability.available(report());
                case REJECTED_JOB -> EvaluationAvailability.noArtifact();
                default -> EvaluationAvailability.notEvaluated();
            };
        }

        @Override
        public List<EvaluationAvailability> listEvaluations() {
            return List.of(EvaluationAvailability.available(report()));
        }

        private FidelityReportView report() {
            Map<FidelityDimension, Double> scores = new EnumMap<>(FidelityDimension.class);
            scores.put(FidelityDimension.SEMANTIC, 0.91);
            scores.put(FidelityDimension.STRUCTURAL, 0.88);
            scores.put(FidelityDimension.FACTUAL, 0.76);
            scores.put(FidelityDimension.ENTITY, 0.94);
            scores.put(FidelityDimension.RELATIONSHIP, 0.83);
            scores.put(FidelityDimension.COMPLETENESS, 0.87);

            return new FidelityReportView(
                    COMPLETED_JOB, OBJECT_ID, scores, 0.67, 3, 2,
                    List.of(), 15_360L, 2_048L, "mock-evaluator/0.1",
                    Instant.parse("2026-01-01T00:00:10Z"));
        }
    }
}
