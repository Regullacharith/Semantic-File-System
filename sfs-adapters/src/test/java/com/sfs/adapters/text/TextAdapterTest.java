package com.sfs.adapters.text;

import com.sfs.adapters.spi.AdapterDescriptor;
import com.sfs.adapters.spi.AdapterRefusedException;
import com.sfs.adapters.spi.AdapterRequest;
import com.sfs.adapters.spi.AdapterResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TextAdapter")
class TextAdapterTest {

    private final TextAdapter adapter = new TextAdapter();

    private AdapterRequest request(String fileName, String content) {
        return new AdapterRequest("sfs-obj-0001-a1b2c3d4", fileName, "text/plain",
                content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("claims the conservative V1 text set")
    void claimsTextSet() {
        AdapterDescriptor descriptor = adapter.descriptor();
        assertThat(descriptor.id()).isEqualTo("sfs-adapter-text");
        assertThat(descriptor.version()).isEqualTo("sfs-text-adapter/0.1");
        assertThat(descriptor.supportedExtensions())
                .containsExactlyInAnyOrder("txt", "text", "md", "markdown", "log");
        assertThat(descriptor.supportedContentTypes())
                .containsExactlyInAnyOrder("text/plain", "text/markdown");
        assertThat(descriptor.capabilities())
                .containsExactlyInAnyOrder("text-extraction", "normalization", "structure-outline");
    }

    @Test
    @DisplayName("adapts a document into normalized text, metrics and structure")
    void adaptsDocument() {
        AdapterResult result = adapter.adapt(request("notes.txt",
                "# Summary\r\n\r\nThe platform hosts the workload.  \n\n\n\nMore words follow.\n"));

        assertThat(result.adapterId()).isEqualTo("sfs-adapter-text");
        assertThat(result.normalizedText())
                .isEqualTo("# Summary\n\nThe platform hosts the workload.\n\nMore words follow.\n");
        assertThat(result.metrics().lineCount()).isEqualTo(6);
        assertThat(result.metrics().paragraphCount()).isEqualTo(3);
        assertThat(result.metrics().printableRatio()).isEqualTo(1.0);
        assertThat(result.structure()).hasSize(1);
        assertThat(result.structure().getFirst().heading()).isEqualTo("Summary");
    }

    @Test
    @DisplayName("refusals carry explicit reasons and never fabricate text")
    void refusalsAreExplicit() {
        assertThatThrownBy(() -> adapter.adapt(request("notes.txt", "")))
                .isInstanceOf(AdapterRefusedException.class)
                .hasMessageContaining("empty");
        assertThatThrownBy(() -> adapter.adapt(request("notes.txt", "...\n")))
                .isInstanceOf(AdapterRefusedException.class)
                .hasMessageContaining("no words");
        assertThatThrownBy(() -> adapter.adapt(
                new AdapterRequest("sfs-obj-0001-a1b2c3d4", "notes.txt", "text/plain",
                        new byte[]{'a', 0, 'b'})))
                .isInstanceOf(AdapterRefusedException.class)
                .hasMessageContaining("binary");
    }

    @Test
    @DisplayName("a markdown document yields a two-level outline")
    void markdownOutline() {
        AdapterResult result = adapter.adapt(request("readme.md", """
                # Overview

                Words in the overview.

                ## Setup

                Words about setup.
                """));

        assertThat(result.structure()).hasSize(2);
        assertThat(result.structure().get(1).heading()).isEqualTo("Setup");
        assertThat(result.structure().get(1).level()).isEqualTo(2);
    }
}
