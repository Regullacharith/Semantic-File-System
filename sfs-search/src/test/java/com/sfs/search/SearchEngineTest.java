package com.sfs.search;

import com.sfs.adapters.registry.AdapterRegistry;
import com.sfs.adapters.resolve.AdapterResolver;
import com.sfs.adapters.text.TextAdapter;
import com.sfs.contracts.search.SearchEvidence;
import com.sfs.contracts.search.SearchQuery;
import com.sfs.contracts.search.SearchResponse;
import com.sfs.contracts.search.SearchResult;
import com.sfs.engine.cache.AnalysisCache;
import com.sfs.engine.core.AnalysisCompletionListener;
import com.sfs.engine.core.AnalysisInput;
import com.sfs.engine.core.AnalysisJob;
import com.sfs.engine.core.SemanticEngine;
import com.sfs.engine.record.InMemorySemanticRecordStore;
import com.sfs.core.identity.Digests;
import com.sfs.core.identity.ObjectId;
import com.sfs.lifecycle.model.FileVersion;
import com.sfs.lifecycle.model.SemanticFile;
import com.sfs.lifecycle.state.FileState;
import com.sfs.memory.H2MemoryDatabase;
import com.sfs.memory.MemoryDnaRepository;
import com.sfs.memory.VectorIndex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Semantic Search Engine")
class SearchEngineTest {

    private static final Instant T0 = Instant.parse("2026-03-15T10:00:00Z");

    @Nested
    @DisplayName("query parser")
    class Parser {

        private final QueryParser parser = new QueryParser();

        @Test
        @DisplayName("extracts lowercase terms and drops stop words")
        void terms() {
            ParsedQuery parsed = parser.parse("Find the database latency reports");
            assertThat(parsed.terms()).containsExactly("database", "latency", "reports");
            assertThat(parsed.isExactLookup()).isFalse();
        }

        @Test
        @DisplayName("an exact Object ID bypasses term extraction")
        void exactId() {
            ParsedQuery parsed = parser.parse("sfs-obj-0002-e5f6a7b8");
            assertThat(parsed.isExactLookup()).isTrue();
            assertThat(parsed.exactLookupId()).contains("sfs-obj-0002-e5f6a7b8");
            assertThat(parsed.terms()).isEmpty();
        }

        @Test
        @DisplayName("a query of only stop words is refused explicitly")
        void stopWordOnlyRefused() {
            assertThatThrownBy(() -> parser.parse("the of and"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no searchable terms");
        }
    }

    @Nested
    @DisplayName("query embedder")
    class Embedder {

        private final QueryEmbedder embedder = new QueryEmbedder();

        @Test
        @DisplayName("produces a deterministic, normalized 64-dimensional vector")
        void deterministicAndNormalized() {
            List<Double> first = embedder.embed(List.of("database", "latency"));
            List<Double> second = embedder.embed(List.of("database", "latency"));
            assertThat(first).isEqualTo(second).hasSize(64);

            double norm = 0.0;
            for (double value : first) {
                norm += value * value;
            }
            assertThat(Math.sqrt(norm)).isCloseTo(1.0,
                    org.assertj.core.data.Offset.offset(1e-9));
        }

        @Test
        @DisplayName("matches the engine's document embedding scheme for the same tokens")
        void consistentWithEngine() {
            String text = "postgresql latency indexing";
            com.sfs.engine.core.SemanticContext context = new com.sfs.engine.core.SemanticContext(
                    "sfs-obj-0001-a1b2c3d4", text,
                    Digests.sha256Hex(text),
                    com.sfs.engine.level.AnalysisLevel.STANDARD, "sfs-engine/0.1", 1, T0);
            com.sfs.engine.pipeline.SemanticIntermediateRepresentation ir =
                    new com.sfs.engine.pipeline.SemanticIntermediateRepresentation(text);
            new com.sfs.engine.pipeline.TextParsingAnalyzer().perform(context, ir);
            new com.sfs.engine.pipeline.EmbeddingAnalyzer().perform(context, ir);

            Double[] engineVector = new Double[ir.embedding().length];
            for (int i = 0; i < engineVector.length; i++) {
                engineVector[i] = ir.embedding()[i];
            }
            assertThat(embedder.embed(List.of("postgresql", "latency", "indexing")))
                    .containsExactly(engineVector);
        }
    }

    @Nested
    @DisplayName("end-to-end search benchmark")
    class Benchmark {

        @TempDir
        Path directory;

        private SearchEngine searchEngine;
        private SemanticEngine analysisEngine;
        private H2MemoryDatabase database;
        private InMemorySemanticRecordStore store;

        private VectorIndex vectorIndex;

        @BeforeEach
        void setUp() throws Exception {
            database = new H2MemoryDatabase("jdbc:h2:file:"
                    + directory.resolve("search").toAbsolutePath() + ";AUTO_SERVER=FALSE");
            database.initialize();
            vectorIndex = new VectorIndex();
            MemoryDnaRepository repository = new MemoryDnaRepository(database, vectorIndex);
            store = new InMemorySemanticRecordStore(repository);
            analysisEngine = new SemanticEngine(
                    id -> Optional.ofNullable(benchmarkInputs().get(id)),
                    resolver(), store, new AnalysisCache(),
                    com.sfs.engine.level.AnalysisLevelPolicy.v1(),
                    new AnalysisCompletionListener() { }, Clock.systemUTC());
            searchEngine = new SearchEngine(vectorIndex, database);
        }

        private Map<String, AnalysisInput> benchmarkInputs() {
            try {
                return loadBenchmarkInputs();
            } catch (Exception e) {
                throw new IllegalStateException("benchmark fixtures are missing", e);
            }
        }

        private static final List<String> BENCHMARK_IDS = List.of(
                "sfs-obj-1001-quarterlyreport",
                "sfs-obj-1002-teammeetingminutes",
                "sfs-obj-1003-researchnotes");

        private Map<String, AnalysisInput> loadBenchmarkInputs() throws Exception {
            Map<String, AnalysisInput> inputs = new ConcurrentHashMap<>();
            inputs.put("sfs-obj-1001-quarterlyreport", new AnalysisInput(
                    "sfs-obj-1001-quarterlyreport", "quarterly-report.txt", "text/plain",
                    Files.readAllBytes(Path.of("..", "sfs-engine", "src", "test",
                            "resources", "benchmarks", "quarterly-report.txt"))));
            inputs.put("sfs-obj-1002-teammeetingminutes", new AnalysisInput(
                    "sfs-obj-1002-teammeetingminutes", "team-meeting-minutes.txt",
                    "text/plain",
                    Files.readAllBytes(Path.of("..", "sfs-engine", "src", "test",
                            "resources", "benchmarks", "team-meeting-minutes.txt"))));
            inputs.put("sfs-obj-1003-researchnotes", new AnalysisInput(
                    "sfs-obj-1003-researchnotes", "research-notes.txt", "text/plain",
                    Files.readAllBytes(Path.of("..", "sfs-engine", "src", "test",
                            "resources", "benchmarks", "research-notes.txt"))));
            return inputs;
        }

        private com.sfs.adapters.resolve.AdapterResolver resolver() {
            AdapterRegistry registry = new AdapterRegistry();
            registry.register(new TextAdapter());
            return new AdapterResolver(registry);
        }

        private void analyzedObject(String objectId, String fileName, String content)
                throws Exception {
            AnalysisInput input = new AnalysisInput(objectId, fileName, "text/plain",
                    content.getBytes(StandardCharsets.UTF_8));
            SemanticEngine dedicated = new SemanticEngine(
                    id -> Optional.of(input), resolver(), store, new AnalysisCache(),
                    com.sfs.engine.level.AnalysisLevelPolicy.v1(),
                    new AnalysisCompletionListener() { }, Clock.systemUTC());
            assertThat(dedicated.analyzeNow(objectId).status())
                    .isEqualTo(AnalysisJob.Status.COMPLETED);
            saveState(objectId, fileName);
        }

        private void saveState(String objectId, String fileName) {
            Instant now = Instant.now();
            String digest = Digests.sha256Hex(fileName);
            database.saveObjectState(new SemanticFile(
                    ObjectId.of(objectId),
                    new com.sfs.lifecycle.model.FileMetadata(fileName, "text/plain",
                            fileName.length(), digest, null, now, now),
                    FileState.ANALYZED,
                    null,
                    "sfs-dna/0.2 v1",
                    List.of(new FileVersion(1, digest, fileName.length(), now)),
                    now));
        }

        private void analyzeAndRegisterBenchmarks() throws Exception {
            for (String objectId : BENCHMARK_IDS) {
                assertThat(analysisEngine.analyzeNow(objectId).status())
                        .isEqualTo(AnalysisJob.Status.COMPLETED);
            }
            saveState("sfs-obj-1001-quarterlyreport", "quarterly-report.txt");
            saveState("sfs-obj-1002-teammeetingminutes", "team-meeting-minutes.txt");
            saveState("sfs-obj-1003-researchnotes", "research-notes.txt");
        }

        @Test
        @DisplayName("benchmark queries find the correct document at rank one")
        void benchmark() throws Exception {
            analyzeAndRegisterBenchmarks();

            assertThat(searchEngine.search(SearchQuery.of(
                    "database query latency indexing performance"))
                    .results().getFirst().objectId()).isEqualTo("sfs-obj-1001-quarterlyreport");
            assertThat(searchEngine.search(SearchQuery.of(
                    "milestone delivery migration plan"))
                    .results().getFirst().objectId()).isEqualTo("sfs-obj-1002-teammeetingminutes");
            assertThat(searchEngine.search(SearchQuery.of(
                    "embeddings retrieval prototype hashing"))
                    .results().getFirst().objectId()).isEqualTo("sfs-obj-1003-researchnotes");
        }

        @Test
        @DisplayName("results carry rich evidence and a bounded, ranked order")
        void richEvidence() throws Exception {
            analyzeAndRegisterBenchmarks();

            SearchResponse response = searchEngine.search(SearchQuery.of(
                    "database query latency indexing performance"));

            assertThat(response.retrieval()).isEqualTo(SearchResponse.RetrievalMode.SEMANTIC);
            assertThat(response.tookMillis()).isGreaterThanOrEqualTo(0);
            assertThat(response.results()).isNotEmpty();
            SearchResult first = response.results().getFirst();
            assertThat(first.relevance()).isBetween(0.0, 1.0);
            assertThat(first.evidence()).isNotEmpty();
            assertThat(first.evidence()).anySatisfy(evidence ->
                    assertThat(evidence.type())
                            .isEqualTo(SearchEvidence.EvidenceType.VECTOR_SIMILARITY));
            for (int i = 1; i < response.results().size(); i++) {
                assertThat(response.results().get(i - 1).relevance())
                        .isGreaterThanOrEqualTo(response.results().get(i).relevance());
            }
        }

        @Test
        @DisplayName("an exact Object ID lookup returns exactly that record")
        void exactLookup() throws Exception {
            saveState("sfs-obj-1003-researchnotes", "research-notes.txt");

            SearchResponse response = searchEngine.search(
                    SearchQuery.of("sfs-obj-1003-researchnotes"));

            assertThat(response.retrieval())
                    .isEqualTo(SearchResponse.RetrievalMode.OBJECT_ID_LOOKUP);
            assertThat(response.results()).hasSize(1);
            assertThat(response.results().getFirst().objectId())
                    .isEqualTo("sfs-obj-1003-researchnotes");
            assertThat(response.results().getFirst().relevance()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("an unknown Object ID yields an empty, explicit result")
        void unknownExactLookup() throws Exception {
            saveState("sfs-obj-1001-quarterlyreport", "quarterly-report.txt");
            SearchResponse response = searchEngine.search(SearchQuery.of("sfs-obj-9999-ffffffff"));
            assertThat(response.results()).isEmpty();
            assertThat(response.retrieval())
                    .isEqualTo(SearchResponse.RetrievalMode.OBJECT_ID_LOOKUP);
        }

        @Test
        @DisplayName("protected objects surface without any secret values in evidence")
        void protectedValuesStayHidden() throws Exception {
            analyzedObject("sfs-obj-1004-credentials", "deployment-config.txt", """
                    # Deployment

                    The deployment report covers the database platform for 2026.

                    # Credentials

                    password=hunter2
                    api_key=sk-live-9f8e7d6c5b4a
                    """);

            SearchResponse response = searchEngine.search(SearchQuery.of(
                    "deployment database report"));

            assertThat(response.results()).isNotEmpty();
            String rendered = response.toString();
            assertThat(rendered).doesNotContain("hunter2");
            assertThat(rendered).doesNotContain("sk-live-9f8e7d6c5b4a");
            assertThat(rendered).doesNotContain("password=");
        }
    }
}
