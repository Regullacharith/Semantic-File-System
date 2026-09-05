package com.sfs.lifecycle.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FileMetadata")
class FileMetadataTest {

    private static final Instant T0 = Instant.parse("2026-03-15T10:00:00Z");
    private static final String SHA = ContentDigest.sha256Hex("content");
    private static final Instant T1 = Instant.parse("2026-03-15T10:05:00Z");

    private FileMetadata valid() {
        return new FileMetadata("notes.txt", "text/plain", 7, SHA, null, T0, T0);
    }

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        @DisplayName("accepts a valid metadata value")
        void acceptsValidMetadata() {
            FileMetadata metadata = valid();
            assertThat(metadata.fileName()).isEqualTo("notes.txt");
            assertThat(metadata.storageAddress()).isNull();
        }

        @Test
        @DisplayName("accepts a storage address as supporting metadata")
        void acceptsStorageAddress() {
            FileMetadata metadata = new FileMetadata("notes.txt", "text/plain", 7, SHA,
                    "mem://primary/0001", T0, T0);
            assertThat(metadata.storageAddress()).isEqualTo("mem://primary/0001");
        }

        @Test
        @DisplayName("rejects a blank or overlong file name")
        void rejectsBlankOrOverlongFileName() {
            assertThatThrownBy(() -> new FileMetadata(" ", "text/plain", 1, SHA, null, T0, T0))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new FileMetadata("x".repeat(256), "text/plain", 1, SHA, null, T0, T0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects path separators and parent references in the name")
        void rejectsPathLikeNames() {
            assertThatThrownBy(() -> new FileMetadata("a/b.txt", "text/plain", 1, SHA, null, T0, T0))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new FileMetadata("a\\b.txt", "text/plain", 1, SHA, null, T0, T0))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new FileMetadata("..secret.txt", "text/plain", 1, SHA, null, T0, T0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects malformed digests and negative sizes")
        void rejectsMalformedDigestAndNegativeSize() {
            assertThatThrownBy(() -> new FileMetadata("f.txt", "text/plain", 1, "abc123", null, T0, T0))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new FileMetadata("f.txt", "text/plain", 1, SHA.toUpperCase(), null, T0, T0))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new FileMetadata("f.txt", "text/plain", -1, SHA, null, T0, T0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects blank content types and a blank storage address")
        void rejectsBlankContentTypeAndBlankAddress() {
            assertThatThrownBy(() -> new FileMetadata("f.txt", "  ", 1, SHA, null, T0, T0))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new FileMetadata("f.txt", "text/plain", 1, SHA, "  ", T0, T0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects lastModifiedAt before registeredAt")
        void rejectsModifiedBeforeRegistered() {
            assertThatThrownBy(() -> new FileMetadata("f.txt", "text/plain", 1, SHA, null, T1, T0))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("updates")
    class Updates {

        @Test
        @DisplayName("withFileName returns a new metadata value and keeps the digest")
        void withFileNameReturnsNewValue() {
            FileMetadata renamed = valid().withFileName("renamed.txt", T1);
            assertThat(renamed.fileName()).isEqualTo("renamed.txt");
            assertThat(renamed.sha256()).isEqualTo(SHA);
            assertThat(renamed.registeredAt()).isEqualTo(T0);
            assertThat(renamed.lastModifiedAt()).isEqualTo(T1);
        }

        @Test
        @DisplayName("withFileName still refuses path-like names")
        void withFileNameStillRefusesPathLikeNames() {
            assertThatThrownBy(() -> valid().withFileName("../escape.txt", T1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("withoutStorageAddress clears only the address")
        void withoutStorageAddressClearsOnlyAddress() {
            FileMetadata addressed = new FileMetadata("notes.txt", "text/plain", 7, SHA,
                    "mem://primary/0001", T0, T0);
            FileMetadata cleared = addressed.withoutStorageAddress(T1);
            assertThat(cleared.storageAddress()).isNull();
            assertThat(cleared.sha256()).isEqualTo(SHA);
            assertThat(cleared.fileName()).isEqualTo("notes.txt");
        }
    }
}
