package com.sfs.adapters.resolve;

import com.sfs.adapters.registry.AdapterRegistry;
import com.sfs.adapters.spi.AdapterDescriptor;
import com.sfs.adapters.spi.AdapterRequest;
import com.sfs.adapters.spi.AdapterResult;
import com.sfs.adapters.spi.FileTypeAdapter;
import com.sfs.adapters.spi.TextMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AdapterResolver")
class AdapterResolverTest {

    private AdapterRegistry registry;
    private AdapterResolver resolver;

    @BeforeEach
    void setUp() {
        registry = new AdapterRegistry();
        registry.register(claiming("sfs-adapter-text", Set.of("txt", "text", "md"),
                Set.of("text/plain")));
        resolver = new AdapterResolver(registry);
    }

    private FileTypeAdapter claiming(String id, Set<String> extensions, Set<String> types) {
        return new FileTypeAdapter() {

            @Override
            public AdapterDescriptor descriptor() {
                return new AdapterDescriptor(id, id, id + "/0.1",
                        extensions, types, Set.of("text-extraction"));
            }

            @Override
            public AdapterResult adapt(AdapterRequest request) {
                return new AdapterResult(id, id + "/0.1", "text",
                        new TextMetrics(4, 1, 1, 1, 1.0), List.of());
            }
        };
    }

    @Test
    @DisplayName("a supported extension selects the claiming adapter automatically")
    void selectsByExtension() {
        assertThat(resolver.resolve("notes.txt", null).descriptor().id())
                .isEqualTo("sfs-adapter-text");
        assertThat(resolver.resolve("Report.TXT", "application/octet-stream")
                .descriptor().id()).isEqualTo("sfs-adapter-text");
        assertThat(resolver.resolve("readme.md", null).descriptor().id())
                .isEqualTo("sfs-adapter-text");
    }

    @Test
    @DisplayName("a supported content type selects the adapter without an extension")
    void selectsByContentType() {
        assertThat(resolver.resolve("NOTES", "text/plain").descriptor().id())
                .isEqualTo("sfs-adapter-text");
    }

    @Test
    @DisplayName("an unsupported extension and type never selects the text adapter")
    void unsupportedExtensionIsRejected() {
        assertThat(resolver.find("photo.png", "image/png")).isEmpty();
        assertThatThrownBy(() -> resolver.resolve("archive.zip", null))
                .isInstanceOf(UnsupportedFileTypeException.class)
                .hasMessageContaining("archive.zip")
                .hasMessageContaining("not treated as text");
    }

    @Test
    @DisplayName("a declared text content type claims an object even with an unknown extension")
    void declaredTextTypeWinsOverUnknownExtension() {
        assertThat(resolver.find("NOTES", "text/plain")).isPresent();
        assertThat(resolver.find("data.output", "text/plain")).isPresent();
    }

    @Test
    @DisplayName("a blank or missing name is rejected")
    void blankNameRejected() {
        assertThat(resolver.find(" ", "text/plain")).isEmpty();
        assertThat(resolver.find(null, "text/plain")).isEmpty();
    }

    @Test
    @DisplayName("a second registered adapter wins its own extensions")
    void secondAdapterClaimsItsOwnTypes() {
        registry.register(claiming("sfs-adapter-fake", Set.of("fake"),
                Set.of("application/x-fake")));

        assertThat(resolver.resolve("doc.fake", null).descriptor().id())
                .isEqualTo("sfs-adapter-fake");
        assertThat(resolver.resolve("doc.fake", "application/x-fake").descriptor().id())
                .isEqualTo("sfs-adapter-fake");
        assertThat(resolver.resolve("doc.txt", null).descriptor().id())
                .isEqualTo("sfs-adapter-text");
        assertThat(resolver.find("doc.xyz", null)).isEmpty();
    }

    @Test
    @DisplayName("resolution and refusal counters support diagnostics")
    void counters() {
        assertThat(resolver.resolutions()).isZero();
        assertThat(resolver.refusals()).isZero();

        resolver.resolve("a.txt", null);
        resolver.find("b.png", null);
        resolver.find("c.xyz", null);

        assertThat(resolver.resolutions()).isEqualTo(1);
        assertThat(resolver.refusals()).isEqualTo(2);
    }
}
