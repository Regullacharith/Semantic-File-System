package com.sfs.core.observe;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Structured log schema (sfs-log/0.1)")
class StructuredEventTest {

    @Test
    @DisplayName("the schema id is stable")
    void schema() {
        assertThat(StructuredEvent.SCHEMA).isEqualTo("sfs-log/0.1");
    }

    @Test
    @DisplayName("events validate level, event name and trace id")
    void validation() {
        assertThatThrownBy(() -> new StructuredEvent(Instant.now(), "VERBOSE",
                "x", null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("INFO, WARN or ERROR");
        assertThatThrownBy(() -> new StructuredEvent(Instant.now(), "INFO",
                " ", null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new StructuredEvent(Instant.now(), "INFO",
                "x", "bad-trace", null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("trace id");
        assertThatThrownBy(() -> new StructuredEvent(Instant.now(), "INFO",
                "x", null, null, null, -1L, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("the log line carries identifiers and outcomes but never values")
    void logLine() {
        StructuredEvent event = new StructuredEvent(
                Instant.parse("2026-10-03T08:00:00Z"), "WARN",
                "request.refused", "0123456789abcdef", "sfs-obj-0001-a1b2c3d4",
                "DENIED", 1_000L, "POST /api/v1/files");

        String line = event.toLogLine();

        assertThat(line)
                .contains("event=request.refused")
                .contains("level=WARN")
                .contains("traceId=0123456789abcdef")
                .contains("subject=sfs-obj-0001-a1b2c3d4")
                .contains("outcome=DENIED")
                .contains("durationNanos=1000")
                .contains("detail=\"POST /api/v1/files\"");
    }

    @Test
    @DisplayName("optional fields are omitted from the line when absent")
    void minimalLine() {
        String line = StructuredEvent.of("INFO", "boot.completed", "OK", null)
                .toLogLine();

        assertThat(line).isEqualTo("event=boot.completed level=INFO outcome=OK");
    }

    @Test
    @DisplayName("quotes in details are neutralized so lines stay one per event")
    void quotesNeutralized() {
        String line = StructuredEvent.of("INFO", "x", "OK", "say \"hello\"")
                .toLogLine();

        assertThat(line).contains("detail=\"say 'hello'\"");
    }

    @Test
    @DisplayName("withers keep the event immutable and add the intended field")
    void withers() {
        StructuredEvent base = StructuredEvent.of("INFO", "stage.done", "OK", null);

        StructuredEvent enriched = base.withTrace("aaaaaaaaaaaaaaaa")
                .withSubject("sfs-obj-0001-a1b2c3d4").withDuration(42L);

        assertThat(base.traceId()).isNull();
        assertThat(enriched.traceId()).isEqualTo("aaaaaaaaaaaaaaaa");
        assertThat(enriched.subject()).isEqualTo("sfs-obj-0001-a1b2c3d4");
        assertThat(enriched.durationNanos()).isEqualTo(42L);
        assertThat(enriched.event()).isEqualTo("stage.done");
    }
}
