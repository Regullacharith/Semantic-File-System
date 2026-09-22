package com.sfs.engine.core;

import com.sfs.engine.cache.AnalysisCache;
import com.sfs.engine.level.AnalysisLevel;
import com.sfs.engine.level.AnalysisLevelPolicy;
import com.sfs.engine.record.InMemorySemanticRecordStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SemanticEngine")
class SemanticEngineTest {

    private static final String BENCHMARK = """
            # Summary

            report reviews the database platform . Query latency
            decreased by 40 percent after indexing changes were deployed .

            # Measurements

            PostgreSQL hosts the production workload for the analytics platform.
            """ + "\n";

    @TempDir
    Path tempDir;

    private InMemorySemanticRecordStore store;
    private AnalysisCache cache;
    private RecordingListener listener;
    private SemanticEngine engine;
    private java.util.Map<String, byte[]> contents;

    private static final class RecordingListener implements AnalysisCompletionListener {

        final List<String> successes = new ArrayList<>();
        final List<String> reuses = new ArrayList<>();
        final List<String> failures = new ArrayList<>();
        final List<Long> durations = new ArrayList<>();

        @Override
        public void onAnalysisSuccess(String objectId, String dnaVersion, Long durationMs) {
            successes.add(objectId + "->" + dnaVersion);
            durations.add(durationMs);
        }

        @Override
        public void onAnalysisReused(String objectId, String dnaVersion) {
            reuses.add(objectId + "->" + dnaVersion);
        }

        @Override
        public void onAnalysisFailure(String objectId, String reason) {
            failures.add(objectId + "->" + reason);
        }
    }

    @BeforeEach
    void setUp() {
        contents = new java.util.concurrent.ConcurrentHashMap<>();
        store = new InMemorySemanticRecordStore();
        cache = new AnalysisCache();
        listener = new RecordingListener();
        engine = new SemanticEngine(
                objectId -> Optional.ofNullable(contents.get(objectId)),
                store, cache, AnalysisLevelPolicy.v1(), listener,
                Clock.fixed(Instant.parse("2026-03-15T10:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    @DisplayName("a synchronous analysis produces complete, persisted, certified DNA")
    void synchronousAnalysis() {
        contents.put("sfs-obj-0001-a1b2c3d4", BENCHMARK.getBytes(StandardCharsets.UTF_8));

        AnalysisJob job = engine.analyzeNow("sfs-obj-0001-a1b2c3d4");

        assertThat(job.status()).isEqualTo(AnalysisJob.Status.COMPLETED);
        assertThat(store.findSemanticDna("sfs-obj-0001-a1b2c3d4")).isPresent();
        assertThat(listener.successes).hasSize(1);
        assertThat(listener.successes.getFirst()).startsWith("sfs-obj-0001-a1b2c3d4->sfs-dna/0.1 v");
        var dna = store.findSemanticDna("sfs-obj-0001-a1b2c3d4").orElseThrow();
        assertThat(dna.summary()).isNotBlank();
        assertThat(dna.concepts()).isNotEmpty();
        assertThat(dna.entities()).isNotEmpty();
        assertThat(dna.facts()).isNotEmpty();
        assertThat(dna.structure()).isNotEmpty();
        assertThat(dna.embeddingDimensions()).isEqualTo(64);
        assertThat(listener.durations.getFirst()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("unchanged content reuses the prior analysis without recomputation")
    void reuseOfUnchangedContent() {
        contents.put("sfs-obj-0001-a1b2c3d4", BENCHMARK.getBytes(StandardCharsets.UTF_8));
        engine.analyzeNow("sfs-obj-0001-a1b2c3d4");
        String versionBefore = store.findSemanticDna("sfs-obj-0001-a1b2c3d4").orElseThrow()
                .schemaVersion() + " v" + store.findSemanticDna("sfs-obj-0001-a1b2c3d4").orElseThrow().dnaVersion();

        AnalysisJob second = engine.analyzeNow("sfs-obj-0001-a1b2c3d4");

        assertThat(second.status()).isEqualTo(AnalysisJob.Status.REUSED);
        assertThat(second.reusedPriorWork()).isTrue();
        assertThat(listener.reuses).hasSize(1);
        assertThat(store.findSemanticDna("sfs-obj-0001-a1b2c3d4").orElseThrow().dnaVersion())
                .isEqualTo(1);
        assertThat(listener.successes).hasSize(1);
        assertThat(versionBefore).endsWith("v1");
    }

    @Test
    @DisplayName("changed content triggers a fresh analysis with a bumped DNA version")
    void changedContentReanalyzes() {
        contents.put("sfs-obj-0001-a1b2c3d4", BENCHMARK.getBytes(StandardCharsets.UTF_8));
        engine.analyzeNow("sfs-obj-0001-a1b2c3d4");
        contents.put("sfs-obj-0001-a1b2c3d4",
                (BENCHMARK + "Follow-up paragraph with new facts for 2027.\n")
                        .getBytes(StandardCharsets.UTF_8));

        AnalysisJob second = engine.analyzeNow("sfs-obj-0001-a1b2c3d4");

        assertThat(second.status()).isEqualTo(AnalysisJob.Status.COMPLETED);
        assertThat(store.findSemanticDna("sfs-obj-0001-a1b2c3d4").orElseThrow().dnaVersion())
                .isEqualTo(2);
        assertThat(listener.reuses).isEmpty();
    }

    @Test
    @DisplayName("missing content fails explicitly and is reported to the listener")
    void missingContentFails() {
        AnalysisJob job = engine.analyzeNow("sfs-obj-9999-00000000");

        assertThat(job.status()).isEqualTo(AnalysisJob.Status.FAILED);
        assertThat(job.failureReason()).contains("No raw content");
        assertThat(listener.failures).hasSize(1);
        assertThat(listener.failures.getFirst()).contains("No raw content");
    }

    @Test
    @DisplayName("non-text content fails inspection with an explicit reason")
    void binaryContentFailsInspection() {
        contents.put("sfs-obj-0001-a1b2c3d4", new byte[]{'a', 0, 'b'});

        AnalysisJob job = engine.analyzeNow("sfs-obj-0001-a1b2c3d4");

        assertThat(job.status()).isEqualTo(AnalysisJob.Status.FAILED);
        assertThat(job.failureReason()).contains("binary");
        assertThat(store.findSemanticDna("sfs-obj-0001-a1b2c3d4")).isEmpty();
    }

    @Test
    @DisplayName("wordless content fails validation instead of producing empty DNA")
    void wordlessContentFails() {
        contents.put("sfs-obj-0001-a1b2c3d4", "...\n...\n".getBytes(StandardCharsets.UTF_8));

        AnalysisJob job = engine.analyzeNow("sfs-obj-0001-a1b2c3d4");

        assertThat(job.status()).isEqualTo(AnalysisJob.Status.FAILED);
        assertThat(job.failureReason()).contains("no words");
    }

    @Test
    @DisplayName("a disabled analysis level is rejected with the policy reason")
    void disabledLevelRejected() {
        contents.put("sfs-obj-0001-a1b2c3d4", BENCHMARK.getBytes(StandardCharsets.UTF_8));

        AnalysisJob job = engine.analyzeNow("sfs-obj-0001-a1b2c3d4", AnalysisLevel.DEEP);

        assertThat(job.status()).isEqualTo(AnalysisJob.Status.REJECTED);
        assertThat(job.failureReason()).contains("not enabled");
        assertThat(listener.failures).isEmpty();
        assertThat(store.findSemanticDna("sfs-obj-0001-a1b2c3d4")).isEmpty();
    }

    @Test
    @DisplayName("asynchronous submission completes on the worker and records stage durations")
    void asynchronousSubmission() throws Exception {
        contents.put("sfs-obj-0001-a1b2c3d4", BENCHMARK.getBytes(StandardCharsets.UTF_8));

        AnalysisJob queued = engine.submit("sfs-obj-0001-a1b2c3d4");
        assertThat(queued.status()).isEqualTo(AnalysisJob.Status.QUEUED);

        AnalysisJob done = awaitTerminal(queued.jobId());
        assertThat(done.status()).isEqualTo(AnalysisJob.Status.COMPLETED);
        assertThat(done.stageDurationsMs()).isNotEmpty();
        assertThat(done.totalStageMillis()).isGreaterThanOrEqualTo(0);
        assertThat(store.findSemanticDna("sfs-obj-0001-a1b2c3d4")).isPresent();
    }

    @Test
    @DisplayName("a second job for the same object while one is active is rejected")
    void duplicateActiveJobRejected() throws Exception {
        contents.put("sfs-obj-0001-a1b2c3d4", BENCHMARK.getBytes(StandardCharsets.UTF_8));
        AnalysisJob first = engine.submit("sfs-obj-0001-a1b2c3d4");
        AnalysisJob second = engine.submit("sfs-obj-0001-a1b2c3d4");

        AnalysisJob.Status secondStatus = second.status();
        awaitTerminal(first.jobId());
        assertThat(secondStatus).isEqualTo(AnalysisJob.Status.REJECTED);
    }

    @Test
    @DisplayName("the pipeline exposes its ordered stage names for diagnostics")
    void stageNamesAreExposed() {
        assertThat(engine.jobRegistry()).isNotNull();
        List<String> names = java.util.List.of("text-parsing", "summary", "structure",
                "concepts", "topics", "entities", "facts", "relationships", "embeddings",
                "protected-values", "dna-builder");
        assertThat(com.sfs.engine.pipeline.SemanticPipeline.v1().stageNames())
                .containsExactlyElementsOf(names);
    }

    private AnalysisJob awaitTerminal(String jobId) throws Exception {
        for (int i = 0; i < 200; i++) {
            AnalysisJob job = engine.jobRegistry().find(jobId).orElseThrow();
            if (job.isTerminal()) {
                return job;
            }
            Thread.sleep(10);
        }
        throw new AssertionError("analysis job did not reach a terminal state in time");
    }
}
