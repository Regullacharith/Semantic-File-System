package com.sfs.adapters.text;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TextNormalizer")
class TextNormalizerTest {

    private final TextNormalizer normalizer = new TextNormalizer();

    @Test
    @DisplayName("unifies Windows and classic Mac line endings to LF")
    void unifiesLineEndings() {
        assertThat(normalizer.normalize("a\r\nb\rc\nd"))
                .isEqualTo("a\nb\nc\nd");
    }

    @Test
    @DisplayName("strips a leading UTF-8 byte-order mark")
    void stripsBom() {
        assertThat(normalizer.normalize("\uFEFFhello world\n"))
                .isEqualTo("hello world\n");
    }

    @Test
    @DisplayName("strips trailing whitespace from every line")
    void stripsTrailingWhitespace() {
        assertThat(normalizer.normalize("one two   \nthree  \n"))
                .isEqualTo("one two\nthree\n");
    }

    @Test
    @DisplayName("collapses runs of blank lines to a single blank line")
    void collapsesBlankRuns() {
        assertThat(normalizer.normalize("para one\n\n\n\n\npara two\n"))
                .isEqualTo("para one\n\npara two\n");
    }

    @Test
    @DisplayName("applies NFC unicode composition")
    void appliesNfc() {
        String decomposed = "e\u0301clair words\n";
        assertThat(normalizer.normalize(decomposed))
                .isEqualTo("éclair words\n");
    }

    @Test
    @DisplayName("normalization is idempotent")
    void idempotent() {
        String messy = "\uFEFFone  \r\ntwo\r\r\n\n\n\nthree   \n";
        String once = normalizer.normalize(messy);
        assertThat(normalizer.normalize(once)).isEqualTo(once);
    }

    @Test
    @DisplayName("an empty document normalizes to an empty document")
    void emptyDocument() {
        assertThat(normalizer.normalize("")).isEmpty();
    }
}
