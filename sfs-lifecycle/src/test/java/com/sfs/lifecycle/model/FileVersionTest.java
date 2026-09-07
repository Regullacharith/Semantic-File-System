package com.sfs.lifecycle.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FileVersion")
class FileVersionTest {

    private static final Instant T0 = Instant.parse("2026-03-15T10:00:00Z");
    private static final String SHA = ContentDigest.sha256Hex("content");

    @Test
    @DisplayName("accepts a valid version")
    void acceptsValidVersion() {
        FileVersion version = new FileVersion(1, SHA, 7, T0);
        assertThat(version.number()).isEqualTo(1);
        assertThat(version.sizeBytes()).isEqualTo(7);
    }

    @Test
    @DisplayName("rejects version numbers below one")
    void rejectsNumberBelowOne() {
        assertThatThrownBy(() -> new FileVersion(0, SHA, 7, T0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("rejects malformed digests")
    void rejectsMalformedDigest() {
        assertThatThrownBy(() -> new FileVersion(1, "nothex", 7, T0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("rejects negative sizes and null capture time")
    void rejectsNegativeSizeAndNullTime() {
        assertThatThrownBy(() -> new FileVersion(1, SHA, -1, T0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FileVersion(1, SHA, 7, null))
                .isInstanceOf(NullPointerException.class);
    }
}
