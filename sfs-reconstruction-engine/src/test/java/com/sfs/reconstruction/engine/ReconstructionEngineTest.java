package com.sfs.reconstruction.engine;

import com.sfs.contracts.file.FileStatus;
import com.sfs.contracts.reconstruction.ReconstructionJobView;
import com.sfs.contracts.reconstruction.ReconstructionStatus;
import com.sfs.core.dna.DnaCanonical;
import com.sfs.core.dna.DnaRepository;
import com.sfs.core.rules.ReconstructionPlanner;
import com.sfs.core.rules.Rule;
import com.sfs.core.rules.RulePriority;
import com.sfs.core.rules.RuleRepository;
import com.sfs.core.rules.RuleSet;
import com.sfs.core.rules.RuleSetCanonical;
import com.sfs.core.rules.RuleType;
import com.sfs.core.rules.RequiredEntityConstraint;
import com.sfs.reconstruction.SFSReconstructionModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static com.sfs.reconstruction.engine.EngineFixtures.awaitTerminal;
import static com.sfs.reconstruction.engine.EngineFixtures.engine;
import static com.sfs.reconstruction.engine.EngineFixtures.engineWithModel;
import static com.sfs.reconstruction.engine.EngineFixtures.filesWith;
import static com.sfs.reconstruction.engine.EngineFixtures.minimalDna;
import static com.sfs.reconstruction.engine.EngineFixtures.protectedDna;
import static com.sfs.reconstruction.engine.EngineFixtures.repositoryWith;
import static com.sfs.reconstruction.engine.EngineFixtures.structuredDna;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Reconstruction engine: end-to-end coordination (11.1-11.8)")
class ReconstructionEngineTest {

    private static final String OBJECT_ID = "sfs-obj-3001-aaaabbbb";
    private static final String MINIMAL_ID = "sfs-obj-3002-ccccdddd";
    private static final String MEMORIZED_ID = "sfs-obj-3003-eeeeffff";
    private static final String PROTECTED_ID = "sfs-obj-3004-00001111";
    private static final String UNKNOWN_ID = "sfs-obj-9999-ffffffff";

    private ReconstructionEngine engine;

    @AfterEach
    void tearDown() {
        if (engine != null) {
            engine.shutdown();
        }
    }

    private ReconstructionEngine defaultEngine() {
        engine = engine(
                repositoryWith(structuredDna(OBJECT_ID), minimalDna(MINIMAL_ID),
                        structuredDna(MEMORIZED_ID), protectedDna(PROTECTED_ID)),
                filesWithMulti());
        return engine;
    }

    private EngineFixtures.TestFileService filesWithMulti() {
        EngineFixtures.TestFileService files = new EngineFixtures.TestFileService();
        files.register(OBJECT_ID, "plan.txt", FileStatus.ANALYZED);
        files.register(MINIMAL_ID, "note.txt", FileStatus.ANALYZED);
        files.register(MEMORIZED_ID, "memorized.txt", FileStatus.MEMORIZED);
        files.register(PROTECTED_ID, "credentials.txt", FileStatus.ANALYZED);
        return files;
    }

    @Nested
    @DisplayName("request manager")
    class RequestManager {

        @Test
        @DisplayName("an unknown object is refused synchronously with an explicit reason")
        void unknownObjectRefused() {
            ReconstructionEngine target = defaultEngine();

            ReconstructionJobView job = target.requestReconstruction(UNKNOWN_ID);

            assertThat(job.status()).isEqualTo(ReconstructionStatus.FAILED);
            assertThat(job.isTerminal()).isTrue();
            assertThat(job.failureReason()).contains("No object exists");
            assertThat(target.findArtifact(job.jobId())).isEmpty();
        }

        @Test
        @DisplayName("an object without Semantic DNA is refused synchronously")
        void objectWithoutDnaRefused() {
            ReconstructionEngine target = defaultEngine();
            EngineFixtures.TestFileService files = new EngineFixtures.TestFileService();
            files.register("sfs-obj-3005-22223333", "fresh.txt", FileStatus.ANALYZED);
            ReconstructionEngine extra = engine(repositoryWith(), files);
            try {
                ReconstructionJobView job = extra.requestReconstruction(
                        "sfs-obj-3005-22223333");

                assertThat(job.status()).isEqualTo(ReconstructionStatus.FAILED);
                assertThat(job.failureReason()).contains("no Semantic DNA");
            } finally {
                extra.shutdown();
            }
        }

        @Test
        @DisplayName("a valid request is accepted and observable until it completes")
        void validRequestObservablyCompletes() {
            ReconstructionEngine target = defaultEngine();

            ReconstructionJobView started = target.requestReconstruction(OBJECT_ID);

            assertThat(started.status()).isIn(ReconstructionStatus.QUEUED,
                    ReconstructionStatus.RUNNING, ReconstructionStatus.COMPLETED);
            awaitTerminal(target, started.jobId());
            ReconstructionJobView finished = target.findJob(started.jobId())
                    .orElseThrow();

            assertThat(finished.status()).isEqualTo(ReconstructionStatus.COMPLETED);
            assertThat(finished.dnaVersion()).contains("sfs-dna/0.2");
            assertThat(finished.modelVersion())
                    .isEqualTo("sfs-reconstruction/deterministic-baseline/0.1");
        }

        @Test
        @DisplayName("an unknown job id looks up empty and blank ids are safe")
        void unknownAndBlankJobIds() {
            ReconstructionEngine target = defaultEngine();

            assertThat(target.findJob("job-9999")).isEmpty();
            assertThat(target.findJob(null)).isEmpty();
            assertThat(target.findJob("  ")).isEmpty();
            assertThat(target.findArtifact(null)).isEmpty();
        }

        @Test
        @DisplayName("jobs list newest first and every request is retained")
        void jobsListedNewestFirst() {
            ReconstructionEngine target = defaultEngine();

            String first = target.requestReconstruction(OBJECT_ID).jobId();
            String second = target.requestReconstruction(MINIMAL_ID).jobId();
            awaitTerminal(target, second);

            List<ReconstructionJobView> jobs = target.listJobs();
            assertThat(jobs).extracting(ReconstructionJobView::jobId)
                    .containsExactly(second, first);
        }
    }

    @Nested
    @DisplayName("end-to-end pipeline")
    class Pipeline {

        @Test
        @DisplayName("a completed artifact is labeled, versioned and downloadable")
        void completedArtifactDownloadable() {
            ReconstructionEngine target = defaultEngine();

            ReconstructionJobView started = target.requestReconstruction(OBJECT_ID);
            awaitTerminal(target, started.jobId());

            var artifact = target.findArtifact(started.jobId()).orElseThrow();
            assertThat(artifact.fileName())
                    .isEqualTo("plan.reconstructed." + started.jobId() + ".txt");
            assertThat(artifact.content()).contains("NOT THE ORIGINAL FILE");
            assertThat(artifact.content()).contains("estimated");
            assertThat(artifact.content()).contains(
                    "The migration window opens on April 7.");
        }

        @Test
        @DisplayName("a memorized object reconstructs without its original bytes")
        void memorizedObjectReconstructs() {
            ReconstructionEngine target = defaultEngine();

            ReconstructionJobView started = target.requestReconstruction(MEMORIZED_ID);
            awaitTerminal(target, started.jobId());

            assertThat(target.findJob(started.jobId()).orElseThrow().status())
                    .isEqualTo(ReconstructionStatus.COMPLETED);
            assertThat(target.findArtifact(started.jobId())).isPresent();
        }

        @Test
        @DisplayName("provenance and measurements are recorded per job")
        void provenanceAndMeasurementsRecorded() {
            ReconstructionEngine target = defaultEngine();

            ReconstructionJobView started = target.requestReconstruction(OBJECT_ID);
            awaitTerminal(target, started.jobId());

            ReconstructionJob record = target.findJobRecord(started.jobId())
                    .orElseThrow();
            ReconstructionMetadata metadata = record.metadata();
            assertThat(metadata).isNotNull();
            assertThat(metadata.objectId()).isEqualTo(OBJECT_ID);
            assertThat(metadata.dnaSha256())
                    .isEqualTo(DnaCanonical.integrityHash(structuredDna(OBJECT_ID)));
            assertThat(metadata.rulesVersion()).isEqualTo("sfs-rules/0.2");
            assertThat(metadata.modelId())
                    .isEqualTo("sfs-reconstruction/deterministic-baseline/0.1");
            assertThat(metadata.planNanos()).isPositive();
            assertThat(metadata.reconstructNanos()).isPositive();
            assertThat(metadata.verifyNanos()).isPositive();
            assertThat(metadata.knowledgePreservationDensity()).isPositive();
            assertThat(record.verification()).isNotNull();
            assertThat(record.verification().satisfied()).isTrue();
        }

        @Test
        @DisplayName("fixed versioned inputs reproduce identical artifacts")
        void reproducibleWithinTolerance() {
            ReconstructionEngine target = defaultEngine();

            String first = target.requestReconstruction(OBJECT_ID).jobId();
            String second = target.requestReconstruction(OBJECT_ID).jobId();
            awaitTerminal(target, second);

            String firstContent = target.findArtifact(first).orElseThrow().content();
            String secondContent = target.findArtifact(second).orElseThrow().content();
            assertThat(firstContent).isEqualTo(secondContent);
            assertThat(target.listJobs()).hasSize(2);
        }

        @Test
        @DisplayName("a minimal document without structure reconstructs from its summary rule")
        void minimalDocumentReconstructs() {
            ReconstructionEngine target = defaultEngine();

            ReconstructionJobView started = target.requestReconstruction(MINIMAL_ID);
            awaitTerminal(target, started.jobId());

            ReconstructionJobView finished = target.findJob(started.jobId())
                    .orElseThrow();
            assertThat(finished.status()).isEqualTo(ReconstructionStatus.COMPLETED);
            assertThat(target.findArtifact(started.jobId()).orElseThrow().content())
                    .contains("A short note that records the storage decision "
                            + "for 2026.");
        }
    }

    @Nested
    @DisplayName("refusals and failure paths")
    class Failures {

        @Test
        @DisplayName("a protected object is refused before any reconstruction runs")
        void protectedObjectRefusedAtTheGate() {
            ReconstructionEngine target = defaultEngine();

            ReconstructionJobView job = target.requestReconstruction(PROTECTED_ID);

            assertThat(job.status()).isEqualTo(ReconstructionStatus.REJECTED);
            assertThat(job.isTerminal()).isTrue();
            assertThat(job.constraintFindings()).anySatisfy(finding ->
                    assertThat(finding.constraint()).isEqualTo("Protected values"));
            assertThat(target.findArtifact(job.jobId())).isEmpty();
            assertThat(job.toString()).doesNotContain("hunter2");
        }

        @Test
        @DisplayName("a model that produces nothing is rejected, not silently shipped")
        void emptyDraftRejected() {
            ReconstructionEngine target = engineWithModel(
                    repositoryWith(structuredDna(OBJECT_ID)),
                    filesWith(OBJECT_ID, "plan.txt", FileStatus.ANALYZED),
                    new SFSReconstructionModel() {
                        @Override
                        public String modelId() {
                            return "silent/0.1";
                        }

                        @Override
                        public com.sfs.reconstruction.ModelOutput reconstruct(
                                com.sfs.reconstruction.ModelInput input) {
                            return new com.sfs.reconstruction.ModelOutput(
                                    "silent/0.1", "  ", List.of());
                        }
                    });

            ReconstructionJobView started = target.requestReconstruction(OBJECT_ID);
            awaitTerminal(target, started.jobId());

            ReconstructionJobView finished = target.findJob(started.jobId())
                    .orElseThrow();
            assertThat(finished.status()).isEqualTo(ReconstructionStatus.REJECTED);
            assertThat(finished.constraintFindings()).anySatisfy(finding ->
                    assertThat(finding.detail()).contains("empty"));
            assertThat(target.findArtifact(started.jobId())).isEmpty();
        }

        @Test
        @DisplayName("a throwing model becomes an explicit failed job without value leaks")
        void throwingModelFailsExplicitly() {
            ReconstructionEngine target = engineWithModel(
                    repositoryWith(structuredDna(OBJECT_ID)),
                    filesWith(OBJECT_ID, "plan.txt", FileStatus.ANALYZED),
                    new SFSReconstructionModel() {
                        @Override
                        public String modelId() {
                            return "exploding/0.1";
                        }

                        @Override
                        public com.sfs.reconstruction.ModelOutput reconstruct(
                                com.sfs.reconstruction.ModelInput input) {
                            throw new IllegalStateException(
                                    "secret value password=hunter2 leaked");
                        }
                    });

            ReconstructionJobView started = target.requestReconstruction(OBJECT_ID);
            awaitTerminal(target, started.jobId());

            ReconstructionJobView finished = target.findJob(started.jobId())
                    .orElseThrow();
            assertThat(finished.status()).isEqualTo(ReconstructionStatus.FAILED);
            assertThat(finished.failureReason()).contains("IllegalStateException");
            assertThat(finished.failureReason()).doesNotContain("hunter2");
        }

        @Test
        @DisplayName("planning refusals become failed jobs carrying the rule reasons")
        void planningRefusalFailsJob() {
            DnaRepository repository = repositoryWith(structuredDna(OBJECT_ID));
            RuleRepository rules = new RuleRepository();
            rules.save(new RuleSet(OBJECT_ID, 1,
                    DnaCanonical.integrityHash(structuredDna(OBJECT_ID)),
                    RuleSetCanonical.RULES_SCHEMA_VERSION,
                    List.of(new Rule("entities-a", RuleType.ENTITY,
                            RulePriority.HIGH, "minimum two mentions",
                            List.of(new RequiredEntityConstraint("Core Database", 2))),
                            new Rule("entities-b", RuleType.ENTITY,
                                    RulePriority.HIGH, "minimum five mentions",
                                    List.of(new RequiredEntityConstraint(
                                            "Core Database", 5))))), EngineFixtures.T0);
            ReconstructionEngine target = new ReconstructionEngine(
                    new DnaRuleLoader(repository,
                            filesWith(OBJECT_ID, "plan.txt", FileStatus.ANALYZED)),
                    new ReconstructionPlanner(rules),
                    new com.sfs.reconstruction.model.DeterministicBaselineRenderer(),
                    new com.sfs.reconstruction.PlanConstraintInterface(),
                    new ReconstructionPostProcessor(),
                    new ReconstructionArtifactFactory());

            ReconstructionJobView started = target.requestReconstruction(OBJECT_ID);
            awaitTerminal(target, started.jobId());

            ReconstructionJobView finished = target.findJob(started.jobId())
                    .orElseThrow();
            assertThat(finished.status()).isEqualTo(ReconstructionStatus.FAILED);
            assertThat(finished.failureReason()).isNotBlank();
            assertThat(target.findArtifact(started.jobId())).isEmpty();
        }

        @Test
        @DisplayName("shutdown marks unfinished jobs failed and refuses new work explicitly")
        void shutdownMarksJobsFailed() throws Exception {
            CountDownLatch blocked = new CountDownLatch(1);
            ReconstructionEngine target = engineWithModel(
                    repositoryWith(structuredDna(OBJECT_ID)),
                    filesWith(OBJECT_ID, "plan.txt", FileStatus.ANALYZED),
                    new SFSReconstructionModel() {
                        @Override
                        public String modelId() {
                            return "blocking/0.1";
                        }

                        @Override
                        public com.sfs.reconstruction.ModelOutput reconstruct(
                                com.sfs.reconstruction.ModelInput input) {
                            try {
                                blocked.await();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new IllegalStateException("interrupted", e);
                            }
                            return new com.sfs.reconstruction.ModelOutput(
                                    "blocking/0.1", "draft", List.of());
                        }
                    });

            ReconstructionJobView started = target.requestReconstruction(OBJECT_ID);
            target.shutdown();

            ReconstructionJobView finished = target.findJob(started.jobId())
                    .orElseThrow();
            assertThat(finished.isTerminal()).isTrue();
            assertThat(finished.failureReason()).contains("stopped");
            blocked.countDown();
        }

        @Test
        @DisplayName("diagnostics count every outcome explicitly")
        void diagnosticsCountOutcomes() {
            ReconstructionEngine target = defaultEngine();

            target.requestReconstruction(UNKNOWN_ID);
            target.requestReconstruction(PROTECTED_ID);
            String good = target.requestReconstruction(OBJECT_ID).jobId();
            awaitTerminal(target, good);

            var counters = target.diagnostics();
            assertThat(counters.get("engineJobsTotal")).isEqualTo(3);
            assertThat(counters.get("engineJobsCompleted")).isEqualTo(1);
            assertThat(counters.get("engineJobsRejected")).isEqualTo(1);
            assertThat(counters.get("engineJobsFailed")).isEqualTo(1);
            assertThat(counters.get("engineArtifactsAvailable")).isEqualTo(1);
        }
    }
}
