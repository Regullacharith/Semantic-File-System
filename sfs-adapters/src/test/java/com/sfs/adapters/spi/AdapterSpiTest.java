package com.sfs.adapters.spi;

import com.sfs.contracts.semantic.SemanticDnaView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Adapter SPI")
class AdapterSpiTest {

    private AdapterDescriptor descriptor() {
        return new AdapterDescriptor(
                "sfs-adapter-test", "Test Adapter", "test/0.1",
                Set.of("TXT", "Md"),
                Set.of("text/plain"),
                Set.of("text-extraction"));
    }

    @Nested
    @DisplayName("descriptor")
    class Descriptor {

        @Test
        @DisplayName("normalizes extensions and content types to lowercase")
        void normalizesClaims() {
            AdapterDescriptor d = descriptor();
            assertThat(d.supportsExtension("txt")).isTrue();
            assertThat(d.supportsExtension("MD")).isTrue();
            assertThat(d.supportsContentType("TEXT/PLAIN")).isTrue();
            assertThat(d.supportsExtension("png")).isFalse();
        }

        @Test
        @DisplayName("an adapter must claim at least one extension or content type")
        void refusesEmptyClaims() {
            assertThatThrownBy(() -> new AdapterDescriptor(
                    "sfs-adapter-empty", "Empty", "v/1",
                    Set.of(), Set.of(), Set.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("identifiers are restricted to lowercase words and dashes")
        void validatesIdentifier() {
            assertThatThrownBy(() -> new AdapterDescriptor(
                    "SFS Adapter", "X", "v/1", Set.of("txt"), Set.of(), Set.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("request")
    class Request {

        @Test
        @DisplayName("extracts a lowercase extension from the file name")
        void extractsExtension() {
            AdapterRequest request = new AdapterRequest(
                    "sfs-obj-0001-a1b2c3d4", "Report.TXT", "text/plain", new byte[]{65});
            assertThat(request.extension()).isEqualTo("txt");
            assertThat(request.extension()).isEqualTo("txt");
        }

        @Test
        @DisplayName("a name without a dot has an empty extension")
        void noExtension() {
            AdapterRequest request = new AdapterRequest(
                    "sfs-obj-0001-a1b2c3d4", "README", "text/plain", new byte[]{65});
            assertThat(request.extension()).isEmpty();
        }

        @Test
        @DisplayName("content is defensively copied in both directions")
        void defensiveCopies() {
            byte[] content = {1, 2, 3};
            AdapterRequest request = new AdapterRequest(
                    "sfs-obj-0001-a1b2c3d4", "a.txt", "text/plain", content);
            content[0] = 9;
            assertThat(request.content()[0]).isEqualTo((byte) 1);
            request.content()[1] = 8;
            assertThat(request.content()[1]).isEqualTo((byte) 2);
        }
    }

    @Nested
    @DisplayName("result")
    class Result {

        @Test
        @DisplayName("a null structure list becomes an empty immutable list")
        void nullStructureBecomesEmpty() {
            AdapterResult result = new AdapterResult(
                    "sfs-adapter-test", "test/0.1", "text",
                    new TextMetrics(4, 1, 1, 1, 1.0), null);
            assertThat(result.structure()).isEmpty();
            assertThatThrownBy(() -> result.structure().add(
                    new SemanticDnaView.StructureNodeView("X", 1, 0)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
