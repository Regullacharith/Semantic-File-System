package com.sfs.ui.mock;

import com.sfs.contracts.reconstruction.ReconstructionArtifact;
import com.sfs.contracts.reconstruction.ReconstructionJobView;
import com.sfs.contracts.reconstruction.ReconstructionJobView.ConstraintFinding;
import com.sfs.contracts.reconstruction.ReconstructionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the reconstruction pipeline's behavioural contracts.
 */
@DisplayName("Mock reconstruction service")
class MockReconstructionServiceTest {

    private static final String CLEAN_OBJECT = "sfs-obj-0002-e5f6a7b8";

    private static final String UNANALYZED_OBJECT = "sfs-obj-0003-c9d0e1f2";

    private static final String PROTECTED_OBJECT = "sfs-obj-0004-b3c4d5e6";

    private MockReconstructionService service;
    private MockFileService fileService;

    @BeforeEach
    void setUp() {
        fileService = new MockFileService();
        service = new MockReconstructionService(fileService, new MockSemanticRecordService());
    }

    @Nested
    @DisplayName("preconditions")
    class Preconditions {

        @Test
        @DisplayName("refuses an unknown Object ID")
        void refusesUnknownObject() {
            ReconstructionJobView job = service.requestReconstruction("sfs-obj-9999-ffffffff");

            assertThat(job.status()).isEqualTo(ReconstructionStatus.FAILED);
            assertThat(job.failureReason()).contains("No object exists");
            assertThat(job.hasArtifact()).isFalse();
        }

        @Test
        @DisplayName("refuses an object that has no Semantic DNA")
        void refusesObjectWithoutDna() {
            String imported = fileService.importFile(
                    new com.sfs.contracts.file.FileImportRequest("fresh.txt", "text", null))
                    .objectId();

            ReconstructionJobView job = service.requestReconstruction(imported);

            assertThat(job.status()).isEqualTo(ReconstructionStatus.FAILED);
            assertThat(job.failureReason()).contains("no Semantic DNA");
            assertThat(job.hasArtifact()).isFalse();
        }
    }

    @Nested
    @DisplayName("successful reconstruction")
    class Success {

        @Test
        @DisplayName("produces an artifact for an analyzed object")
        void producesArtifact() {
            ReconstructionJobView job = service.requestReconstruction(CLEAN_OBJECT);

            assertThat(job.status()).isEqualTo(ReconstructionStatus.COMPLETED);
            assertThat(job.hasArtifact()).isTrue();
            assertThat(service.findArtifact(job.jobId())).isPresent();
        }

        @Test
        @DisplayName("reconstructs a memorized object whose raw file is gone")
        void reconstructsMemorizedObject() {
            assertThat(fileService.findByObjectId(CLEAN_OBJECT).orElseThrow().status()
                    .isRawDataRemoved()).isTrue();

            assertThat(service.requestReconstruction(CLEAN_OBJECT).status())
                    .isEqualTo(ReconstructionStatus.COMPLETED);
        }

        @Test
        @DisplayName("records the DNA, rules and model versions used")
        void recordsProvenance() {
            ReconstructionJobView job = service.requestReconstruction(CLEAN_OBJECT);

            assertThat(job.dnaVersion()).isNotBlank().contains("sfs-dna");
            assertThat(job.rulesVersion()).isNotBlank();
            assertThat(job.modelVersion()).isNotBlank();
        }

        @Test
        @DisplayName("names the artifact so it cannot be mistaken for the original")
        void artifactNameMarksItReconstructed() {
            ReconstructionJobView job = service.requestReconstruction(CLEAN_OBJECT);

            assertThat(job.artifactName())
                    .contains("reconstructed")
                    .isNotEqualTo(job.sourceName());
        }

        @Test
        @DisplayName("prepends a header stating the artifact is not the original")
        void artifactCarriesProvenanceHeader() {
            String jobId = service.requestReconstruction(CLEAN_OBJECT).jobId();
            ReconstructionArtifact artifact = service.findArtifact(jobId).orElseThrow();

            assertThat(artifact.content())
                    .contains("NOT THE ORIGINAL FILE")
                    .contains("not a byte-for-byte recovery".toUpperCase().substring(0, 3))
                    .contains(CLEAN_OBJECT);
        }

        @Test
        @DisplayName("carries the recorded facts into the artifact")
        void artifactCarriesFacts() {
            String jobId = service.requestReconstruction(CLEAN_OBJECT).jobId();
            String content = service.findArtifact(jobId).orElseThrow().content();

            assertThat(content)
                    .contains("Query latency decreased by 40 percent")
                    .contains("[critical]");
        }

        @Test
        @DisplayName("preserves document structure and relationships")
        void artifactPreservesStructure() {
            String jobId = service.requestReconstruction(CLEAN_OBJECT).jobId();
            String content = service.findArtifact(jobId).orElseThrow().content();

            assertThat(content)
                    .contains("DOCUMENT STRUCTURE")
                    .contains("Measurements")
                    .contains("RELATIONSHIPS");
        }

        @Test
        @DisplayName("is repeatable for the same versioned input")
        void isRepeatable() {
            String first = service.findArtifact(
                    service.requestReconstruction(CLEAN_OBJECT).jobId()).orElseThrow().content();
            String second = service.findArtifact(
                    service.requestReconstruction(CLEAN_OBJECT).jobId()).orElseThrow().content();

            assertThat(stripHeader(first)).isEqualTo(stripHeader(second));
        }

        private static String stripHeader(String content) {
            int marker = content.indexOf("SUMMARY");
            return marker < 0 ? content : content.substring(marker);
        }
    }

    @Nested
    @DisplayName("verification and rejection")
    class Verification {

        @Test
        @DisplayName("rejects reconstruction of a document holding protected values")
        void rejectsProtectedDocument() {
            ReconstructionJobView job = service.requestReconstruction(PROTECTED_OBJECT);

            assertThat(job.status()).isEqualTo(ReconstructionStatus.REJECTED);
            assertThat(job.hasArtifact()).isFalse();
            assertThat(job.failureReason()).contains("Verification rejected");
        }

        @Test
        @DisplayName("produces no downloadable artifact for a rejected job")
        void rejectedJobHasNoArtifact() {
            String jobId = service.requestReconstruction(PROTECTED_OBJECT).jobId();

            assertThat(service.findArtifact(jobId)).isEmpty();
        }

        @Test
        @DisplayName("explains which constraint was violated")
        void explainsViolation() {
            ReconstructionJobView job = service.requestReconstruction(PROTECTED_OBJECT);

            assertThat(job.constraintFindings())
                    .anySatisfy(finding -> {
                        assertThat(finding.severity())
                                .isEqualTo(ConstraintFinding.Severity.VIOLATION);
                        assertThat(finding.constraint()).isEqualTo("Protected values");
                    });
        }

        @Test
        @DisplayName("records satisfied constraints so the user sees what was checked")
        void recordsSatisfiedConstraints() {
            ReconstructionJobView job = service.requestReconstruction(CLEAN_OBJECT);

            assertThat(job.constraintFindings())
                    .isNotEmpty()
                    .allSatisfy(finding -> assertThat(finding.detail()).isNotBlank());
            assertThat(job.constraintFindings())
                    .extracting(ConstraintFinding::constraint)
                    .contains("Required facts", "Document structure", "Entity consistency");
        }
    }

    @Nested
    @DisplayName("job tracking")
    class JobTracking {

        @Test
        @DisplayName("retrieves a job by identifier")
        void retrievesJob() {
            String jobId = service.requestReconstruction(CLEAN_OBJECT).jobId();

            assertThat(service.findJob(jobId)).isPresent();
        }

        @Test
        @DisplayName("returns empty for an unknown or blank job identifier")
        void handlesUnknownJob() {
            assertThat(service.findJob("job-9999")).isEmpty();
            assertThat(service.findJob("")).isEmpty();
            assertThat(service.findJob(null)).isEmpty();
        }

        @Test
        @DisplayName("lists jobs most recent first")
        void listsJobs() {
            service.requestReconstruction(CLEAN_OBJECT);
            service.requestReconstruction(UNANALYZED_OBJECT);

            assertThat(service.listJobs()).hasSize(2);
        }

        @Test
        @DisplayName("keeps a refused job in the list so failures stay visible")
        void keepsFailedJobs() {
            String jobId = service.requestReconstruction("sfs-obj-9999-ffffffff").jobId();

            assertThat(service.listJobs())
                    .extracting(ReconstructionJobView::jobId)
                    .contains(jobId);
        }
    }

    @Nested
    @DisplayName("security")
    class Security {

        @Test
        @DisplayName("never writes a secret-shaped value into an artifact")
        void artifactContainsNoSecrets() {
            for (String objectId : new String[]{CLEAN_OBJECT, "sfs-obj-0001-a1b2c3d4"}) {
                Optional<ReconstructionArtifact> artifact =
                        service.findArtifact(service.requestReconstruction(objectId).jobId());

                artifact.ifPresent(a -> assertThat(a.content())
                        .doesNotContain("sk-", "Bearer ", "password=", "AKIA"));
            }
        }
    }
}
