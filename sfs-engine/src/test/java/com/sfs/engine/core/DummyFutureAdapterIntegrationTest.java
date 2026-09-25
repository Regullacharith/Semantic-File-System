package com.sfs.engine.core;

import com.sfs.adapters.registry.AdapterRegistry;
import com.sfs.adapters.resolve.AdapterResolver;
import com.sfs.adapters.spi.AdapterDescriptor;
import com.sfs.adapters.spi.AdapterRefusedException;
import com.sfs.adapters.spi.AdapterRequest;
import com.sfs.adapters.spi.AdapterResult;
import com.sfs.adapters.spi.FileTypeAdapter;
import com.sfs.adapters.spi.TextMetrics;
import com.sfs.adapters.text.StructuralParser;
import com.sfs.adapters.text.TextLoader;
import com.sfs.engine.cache.AnalysisCache;
import com.sfs.engine.level.AnalysisLevelPolicy;
import com.sfs.engine.record.InMemorySemanticRecordStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("A future adapter integrates without Semantic Engine changes")
class DummyFutureAdapterIntegrationTest {

    private static final String FAKE_TEXT = "The fake format carries plain words for 2026.\n";

    private SemanticEngine engine;
    private InMemorySemanticRecordStore store;
    private Map<String, AnalysisInput> inputs;

    static final class DummyFutureAdapter implements FileTypeAdapter {

        @Override
        public AdapterDescriptor descriptor() {
            return new AdapterDescriptor(
                    "sfs-adapter-dummy-fake", "Dummy future adapter", "dummy-fake/0.1",
                    Set.of("fake"), Set.of("application/x-fake"),
                    Set.of("text-extraction"));
        }

        @Override
        public AdapterResult adapt(AdapterRequest request) {
            String text = new TextLoader().load(request.content());
            return new AdapterResult(descriptor().id(), descriptor().version(), text,
                    new TextMetrics(request.content().length, 1,
                            text.split("\\s+").length, 1, 1.0),
                    new StructuralParser().parse(text));
        }
    }

    @BeforeEach
    void setUp() {
        store = new InMemorySemanticRecordStore();
        inputs = new ConcurrentHashMap<>();
        AdapterRegistry registry = new AdapterRegistry();
        registry.register(new com.sfs.adapters.text.TextAdapter());
        registry.register(new DummyFutureAdapter());
        engine = new SemanticEngine(
                objectId -> Optional.ofNullable(inputs.get(objectId)),
                new AdapterResolver(registry),
                store, new AnalysisCache(), AnalysisLevelPolicy.v1(),
                new AnalysisCompletionListener() { }, Clock.systemUTC());
    }

    @Test
    @DisplayName("the resolver routes each format to its own adapter")
    void resolverRoutesPerFormat() {
        AdapterResolver resolver = new AdapterResolver(registryOf());
        assertThat(resolver.resolve("doc.txt", null).descriptor().id())
                .isEqualTo("sfs-adapter-text");
        assertThat(resolver.resolve("doc.fake", null).descriptor().id())
                .isEqualTo("sfs-adapter-dummy-fake");
        assertThatThrownBy(() -> resolver.resolve("doc.xyz", null))
                .isInstanceOf(com.sfs.adapters.resolve.UnsupportedFileTypeException.class);
    }

    @Test
    @DisplayName("the engine analyzes a fake-format object through the future adapter")
    void engineAnalyzesFakeFormat() {
        inputs.put("sfs-obj-0001-a1b2c3d4", new AnalysisInput(
                "sfs-obj-0001-a1b2c3d4", "document.fake", "application/x-fake",
                FAKE_TEXT.getBytes(StandardCharsets.UTF_8)));

        AnalysisJob job = engine.analyzeNow("sfs-obj-0001-a1b2c3d4");

        assertThat(job.status()).isEqualTo(AnalysisJob.Status.COMPLETED);
        var dna = store.findSemanticDna("sfs-obj-0001-a1b2c3d4").orElseThrow();
        assertThat(dna.summary()).contains("fake format");
        assertThat(dna.structure()).hasSize(1);
    }

    @Test
    @DisplayName("an unsupported format fails explicitly and is never treated as text")
    void unsupportedFailsExplicitly() {
        inputs.put("sfs-obj-0001-a1b2c3d4", new AnalysisInput(
                "sfs-obj-0001-a1b2c3d4", "photo.png", "image/png",
                FAKE_TEXT.getBytes(StandardCharsets.UTF_8)));

        AnalysisJob job = engine.analyzeNow("sfs-obj-0001-a1b2c3d4");

        assertThat(job.status()).isEqualTo(AnalysisJob.Status.FAILED);
        assertThat(job.failureReason()).contains("No registered adapter supports");
        assertThat(store.findSemanticDna("sfs-obj-0001-a1b2c3d4")).isEmpty();
    }

    @Test
    @DisplayName("the dummy adapter refuses non-text payloads explicitly")
    void dummyRefusesNonText() {
        assertThatThrownBy(() -> new DummyFutureAdapter().adapt(new AdapterRequest(
                "sfs-obj-0001-a1b2c3d4", "doc.fake", "application/x-fake",
                new byte[]{0, 1, 2})))
                .isInstanceOf(AdapterRefusedException.class);
    }

    private AdapterRegistry registryOf() {
        AdapterRegistry registry = new AdapterRegistry();
        registry.register(new com.sfs.adapters.text.TextAdapter());
        registry.register(new DummyFutureAdapter());
        return registry;
    }
}
