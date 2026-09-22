package com.sfs.engine.inspect;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FileInspector")
class FileInspectorTest {

    private final FileInspector inspector = new FileInspector();

    @Test
    @DisplayName("accepts ordinary UTF-8 text and reports statistics")
    void acceptsTextAndReportsStatistics() {
        InspectionReport report = inspector.inspect(
                "# Heading\n\nFirst paragraph with words.\n\nSecond paragraph.\n"
                        .getBytes(StandardCharsets.UTF_8));

        assertThat(report.byteLength()).isPositive();
        assertThat(report.lineCount()).isEqualTo(6);
        assertThat(report.wordCount()).isEqualTo(7);
        assertThat(report.paragraphCount()).isEqualTo(3);
        assertThat(report.printableRatio()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("refuses empty content with an explicit reason")
    void refusesEmptyContent() {
        assertThatThrownBy(() -> inspector.inspect(new byte[0]))
                .isInstanceOf(FileInspector.TextRefusedException.class)
                .hasMessageContaining("empty");
    }

    @Test
    @DisplayName("refuses binary content containing a null byte")
    void refusesBinaryContent() {
        assertThatThrownBy(() -> inspector.inspect(new byte[]{'a', 0, 'b'}))
                .isInstanceOf(FileInspector.TextRefusedException.class)
                .hasMessageContaining("binary");
    }

    @Test
    @DisplayName("refuses invalid UTF-8 sequences")
    void refusesInvalidUtf8() {
        assertThatThrownBy(() -> inspector.inspect(
                new byte[]{(byte) 0xC3, (byte) 0x28, ' ', 't', 'e', 'x', 't'}))
                .isInstanceOf(FileInspector.TextRefusedException.class)
                .hasMessageContaining("UTF-8");
    }

    @Test
    @DisplayName("refuses wordless content")
    void refusesWordlessContent() {
        assertThatThrownBy(() -> inspector.inspect("...\n---\n".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(FileInspector.TextRefusedException.class)
                .hasMessageContaining("no words");
    }
}
