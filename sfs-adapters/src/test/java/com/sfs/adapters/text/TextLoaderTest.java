package com.sfs.adapters.text;

import com.sfs.adapters.spi.AdapterRefusedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TextLoader")
class TextLoaderTest {

    private final TextLoader loader = new TextLoader();

    @Test
    @DisplayName("accepts ordinary UTF-8 text")
    void acceptsText() {
        String text = loader.load("First paragraph with words.\n\nSecond paragraph.\n"
                .getBytes(StandardCharsets.UTF_8));
        assertThat(text).startsWith("First paragraph");
    }

    @Test
    @DisplayName("refuses empty content with an explicit reason")
    void refusesEmptyContent() {
        assertThatThrownBy(() -> loader.load(new byte[0]))
                .isInstanceOf(AdapterRefusedException.class)
                .hasMessageContaining("empty");
    }

    @Test
    @DisplayName("refuses binary content containing a null byte")
    void refusesBinaryContent() {
        assertThatThrownBy(() -> loader.load(new byte[]{'a', 0, 'b'}))
                .isInstanceOf(AdapterRefusedException.class)
                .hasMessageContaining("binary");
    }

    @Test
    @DisplayName("refuses invalid UTF-8 sequences")
    void refusesInvalidUtf8() {
        assertThatThrownBy(() -> loader.load(
                new byte[]{(byte) 0xC3, (byte) 0x28, ' ', 't', 'e', 'x', 't'}))
                .isInstanceOf(AdapterRefusedException.class)
                .hasMessageContaining("UTF-8");
    }

    @Test
    @DisplayName("refuses wordless content")
    void refusesWordlessContent() {
        assertThatThrownBy(() -> loader.load("...\n---\n".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(AdapterRefusedException.class)
                .hasMessageContaining("no words");
    }

    @Test
    @DisplayName("refuses mostly unprintable content")
    void refusesUnprintableContent() {
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            content.append('\u0001').append("word ");
        }
        assertThatThrownBy(() -> loader.load(content.toString().getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(AdapterRefusedException.class)
                .hasMessageContaining("printable");
    }

    @Test
    @DisplayName("decodes non-ASCII UTF-8 text correctly")
    void decodesNonAscii() {
        String text = loader.load("héllo wörld with ünïcode\n".getBytes(StandardCharsets.UTF_8));
        assertThat(text).contains("héllo");
    }
}
