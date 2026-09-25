package com.sfs.engine.core;

import com.sfs.adapters.registry.AdapterRegistry;
import com.sfs.adapters.resolve.AdapterResolver;
import com.sfs.adapters.text.TextAdapter;
import com.sfs.core.dna.DnaCanonical;
import com.sfs.core.dna.DnaSchemaValidator;
import com.sfs.core.dna.SemanticDna;
import com.sfs.core.dna.StoredDna;
import com.sfs.engine.cache.AnalysisCache;
import com.sfs.engine.level.AnalysisLevelPolicy;
import com.sfs.engine.record.InMemorySemanticRecordStore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Milestone 06 acceptance: Semantic DNA representation")
class SemanticDnaAcceptanceTest {

    private static final String SECRET = "sk-live-9f8e7d6c5b4a";

    private static final String CREDENTIALS_DOCUMENT = """
            # Deployment Configuration

            The deployment configuration documents the reporting service.

            # Credentials

            password=password123
            api_key=sk-live-9f8e7d6c5b4a
            ops.contact=charithkumar369@gmail.com

            # Notes

            The platform hosts the reporting workload for 2026.
            """;

    private InMemorySemanticRecordStore store;
    private SemanticEngine engine;

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
        inputs.put("credentials", new AnalysisInput("credentials",
                "deployment-config.txt", "text/plain",
                CREDENTIALS_DOCUMENT.getBytes(StandardCharsets.UTF_8)));
        AdapterRegistry registry = new AdapterRegistry();
        registry.register(new TextAdapter());
        engine = new SemanticEngine(
                objectId -> Optional.ofNullable(inputs.get(objectId)),
                new AdapterResolver(registry),
                store, new AnalysisCache(), AnalysisLevelPolicy.v1(),
                new AnalysisCompletionListener() { }, Clock.systemUTC());
    }

    @Test
    @DisplayName("every benchmark TXT produces schema-valid authoritative DNA")
    void benchmarksProduceValidDna() {
        DnaSchemaValidator validator = new DnaSchemaValidator();
        for (String objectId : List.of("quarterlyreport", "teammeetingminutes",
                "researchnotes")) {
            assertThat(engine.analyzeNow(objectId).status())
                    .as("analysis of %s", objectId)
                    .isIn(AnalysisJob.Status.COMPLETED, AnalysisJob.Status.REUSED);

            StoredDna stored = store.findStored(objectId).orElseThrow();
            assertThat(validator.validate(stored.dna())).isEmpty();
            assertThat(stored.dna().schemaVersion()).isEqualTo("sfs-dna/0.2");
            assertThat(stored.canonicalSha256())
                    .isEqualTo(DnaCanonical.integrityHash(stored.dna()));
        }
    }

    private void ensureAnalyzed(String objectId) {
        AnalysisJob.Status status = engine.analyzeNow(objectId).status();
        assertThat(status).isIn(AnalysisJob.Status.COMPLETED, AnalysisJob.Status.REUSED);
    }

    @Test
    @DisplayName("engine-produced DNA serializes and deserializes losslessly")
    void engineDnaRoundTrips() {
        for (String objectId : List.of("quarterlyreport", "credentials")) {
            ensureAnalyzed(objectId);
            StoredDna stored = store.findStored(objectId).orElseThrow();
            String canonical = DnaCanonical.serialize(stored.dna());
            assertThat(DnaCanonical.deserialize(canonical)).isEqualTo(stored.dna());
        }
    }

    @Test
    @DisplayName("metadata is separated from the semantic model")
    void metadataIsSeparated() {
        ensureAnalyzed("quarterlyreport");
        Set<String> fieldNames = new java.util.HashSet<>();
        for (RecordComponent component : com.sfs.core.dna.SemanticDna.class
                .getRecordComponents()) {
            fieldNames.add(component.getName());
        }
        assertThat(fieldNames).doesNotContain("fileName", "displayName", "contentType",
                "sizeBytes", "registeredAt", "metadata", "storageAddress", "path");

        String canonical = DnaCanonical.serialize(
                store.findStored("quarterlyreport").orElseThrow().dna());
        assertThat(canonical).doesNotContain("quarterly-report.txt");
        assertThat(canonical).doesNotContain("registeredAt");
    }

    @Test
    @DisplayName("version changes are detectable through the canonical hash chain")
    void versionChangesDetectable() {
        ensureAnalyzed("quarterlyreport");

        StoredDna stored = store.findStored("quarterlyreport").orElseThrow();
        assertThat(stored.dna().dnaVersion()).isEqualTo(1);
        assertThat(DnaCanonical.integrityHash(stored.dna()))
                .isEqualTo(stored.canonicalSha256());

        SemanticDna changed = new SemanticDna(
                new com.sfs.core.dna.DnaIdentity(stored.dna().objectId(),
                        stored.dna().schemaVersion(), 2,
                        stored.dna().identity().engineVersion(),
                        stored.dna().identity().generatedAt()),
                stored.dna().summary(), stored.dna().concepts(), stored.dna().topics(),
                stored.dna().entities(), stored.dna().facts(),
                stored.dna().relationships(), stored.dna().structure(),
                stored.dna().embedding(), stored.dna().behaviour(),
                stored.dna().reconstructionRules(), stored.dna().fidelity(),
                stored.dna().security());
        assertThat(DnaCanonical.integrityHash(changed))
                .isNotEqualTo(stored.canonicalSha256());
    }

    @Test
    @DisplayName("sensitive references carry no plaintext values")
    void sensitiveReferencesStayValueFree() {
        ensureAnalyzed("credentials");

        StoredDna stored = store.findStored("credentials").orElseThrow();
        assertThat(stored.dna().security().containsProtectedReferences()).isTrue();
        assertThat(stored.dna().security().protectedReferences())
                .anySatisfy(reference ->
                        assertThat(reference.sensitiveType()).isEqualTo("PASSWORD"));

        String canonical = DnaCanonical.serialize(stored.dna());
        assertThat(canonical).doesNotContain("password123");
        assertThat(canonical).doesNotContain(SECRET);
        assertThat(canonical).doesNotContain("charithkumar369@gmail.com");
    }
}
