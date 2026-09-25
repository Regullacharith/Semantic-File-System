package com.sfs.engine;

import com.sfs.adapters.registry.AdapterRegistry;
import com.sfs.adapters.resolve.AdapterResolver;
import com.sfs.adapters.text.TextAdapter;
import com.sfs.engine.cache.AnalysisCache;
import com.sfs.engine.core.AnalysisCompletionListener;
import com.sfs.engine.core.AnalysisInput;
import com.sfs.engine.core.AnalysisInputProvider;
import com.sfs.engine.core.AnalysisJob;
import com.sfs.engine.core.SemanticEngine;
import com.sfs.engine.level.AnalysisLevelPolicy;
import com.sfs.engine.record.InMemorySemanticRecordStore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Benchmark documents produce complete Semantic DNA")
class BenchmarkDocumentsAcceptanceTest {

    private SemanticEngine engine;
    private InMemorySemanticRecordStore store;

    @BeforeAll
    void setUp() {
        store = new InMemorySemanticRecordStore();
        Map<String, AnalysisInput> inputs = new ConcurrentHashMap<>();
        for (String name : List.of("quarterly-report.txt", "team-meeting-minutes.txt",
                "research-notes.txt")) {
            try {
                String objectId = name.replace(".txt", "").replace("-", "");
                inputs.put(objectId, new AnalysisInput(objectId, name, "text/plain",
                        Files.readAllBytes(Path.of("src", "test", "resources",
                                "benchmarks", name))));
            } catch (Exception e) {
                throw new IllegalStateException("missing benchmark fixture " + name, e);
            }
        }
        AnalysisInputProvider provider = objectId -> Optional.ofNullable(inputs.get(objectId));
        AdapterRegistry registry = new AdapterRegistry();
        registry.register(new TextAdapter());
        engine = new SemanticEngine(provider, new AdapterResolver(registry),
                store, new AnalysisCache(), AnalysisLevelPolicy.v1(),
                new AnalysisCompletionListener() { }, Clock.systemUTC());
    }

    @Test
    @DisplayName("each benchmark document yields complete DNA through every stage")
    void benchmarkDocumentsProduceCompleteDna() {
        for (String objectId : List.of("quarterlyreport", "teammeetingminutes",
                "researchnotes")) {
            AnalysisJob job = engine.analyzeNow(objectId);
            assertThat(job.status())
                    .as("analysis of %s failed. reason: %s. stages: %s",
                        objectId,
                        job.failureReason() == null ? "n/a" : job.failureReason(),
                        job.stageDurationsMs())
                    .isEqualTo(AnalysisJob.Status.COMPLETED);
            assertThat(job.failureReason()).isNull();
            assertThat(job.stageDurationsMs()).isNotEmpty();

            var dna = store.findSemanticDna(objectId).orElseThrow();
            assertThat(dna.schemaVersion()).isEqualTo("sfs-dna/0.2");
            assertThat(dna.summary()).isNotBlank();
            assertThat(dna.concepts()).isNotEmpty();
            assertThat(dna.topics()).isNotEmpty();
            assertThat(dna.entities()).isNotEmpty();
            assertThat(dna.facts()).isNotEmpty();
            assertThat(dna.structure()).isNotEmpty();
            assertThat(dna.embeddingDimensions()).isEqualTo(64);
            assertThat(dna.fidelity().extractionConfidence()).isBetween(0.0, 1.0);
            assertThat(dna.fidelity().structuralCompleteness()).isBetween(0.0, 1.0);
        }
    }

    @Test
    @DisplayName("analysis is deterministic: identical content produces identical DNA")
    void analysisIsDeterministic() throws Exception {
        byte[] content = Files.readAllBytes(Path.of("src", "test", "resources",
                "benchmarks", "quarterly-report.txt"));
        InMemorySemanticRecordStore first = runIsolated(content);
        InMemorySemanticRecordStore second = runIsolated(content);

        assertThat(first.findSemanticDna("bench").orElseThrow())
                .isEqualTo(second.findSemanticDna("bench").orElseThrow());
    }

    private InMemorySemanticRecordStore runIsolated(byte[] content) {
        InMemorySemanticRecordStore isolated = new InMemorySemanticRecordStore();
        SemanticEngine isolatedEngine = new SemanticEngine(
                ignored -> Optional.of(new AnalysisInput("bench", "bench.txt",
                        "text/plain", content)),
                resolver(), isolated, new AnalysisCache(),
                AnalysisLevelPolicy.v1(), new AnalysisCompletionListener() { },
                Clock.systemUTC());
        assertThat(isolatedEngine.analyzeNow("bench").status())
                .isEqualTo(AnalysisJob.Status.COMPLETED);
        return isolated;
    }

    private AdapterResolver resolver() {
        AdapterRegistry registry = new AdapterRegistry();
        registry.register(new TextAdapter());
        return new AdapterResolver(registry);
    }

    private static final class List {
        static java.util.List<String> of(String... items) {
            return java.util.List.of(items);
        }
    }
}
