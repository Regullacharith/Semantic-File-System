package com.sfs.lifecycle.store;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("InMemoryRawContentStore")
class InMemoryRawContentStoreTest {

    private final InMemoryRawContentStore store = new InMemoryRawContentStore();

    @Test
    @DisplayName("stores and retrieves content by Object ID")
    void storesAndRetrievesContent() {
        byte[] content = "meeting notes".getBytes(StandardCharsets.UTF_8);
        store.store("sfs-obj-0001-a1b2c3d4", content);
        assertThat(store.contains("sfs-obj-0001-a1b2c3d4")).isTrue();
        assertThat(store.retrieve("sfs-obj-0001-a1b2c3d4")).isPresent();
        assertThat(store.retrieve("sfs-obj-0001-a1b2c3d4").orElseThrow()).isEqualTo(content);
    }

    @Test
    @DisplayName("returns a copy so callers cannot mutate stored bytes")
    void returnsDefensiveCopies() {
        byte[] content = "original".getBytes(StandardCharsets.UTF_8);
        store.store("sfs-obj-0001-a1b2c3d4", content);
        content[0] = 'X';
        assertThat(new String(store.retrieve("sfs-obj-0001-a1b2c3d4").orElseThrow(),
                StandardCharsets.UTF_8)).isEqualTo("original");
        byte[] retrieved = store.retrieve("sfs-obj-0001-a1b2c3d4").orElseThrow();
        retrieved[0] = 'Y';
        assertThat(new String(store.retrieve("sfs-obj-0001-a1b2c3d4").orElseThrow(),
                StandardCharsets.UTF_8)).isEqualTo("original");
    }

    @Test
    @DisplayName("release removes the bytes and reports whether they existed")
    void releaseRemovesBytesAndReportsExistence() {
        store.store("sfs-obj-0001-a1b2c3d4", "data".getBytes(StandardCharsets.UTF_8));
        assertThat(store.release("sfs-obj-0001-a1b2c3d4")).isTrue();
        assertThat(store.contains("sfs-obj-0001-a1b2c3d4")).isFalse();
        assertThat(store.retrieve("sfs-obj-0001-a1b2c3d4")).isEqualTo(Optional.empty());
        assertThat(store.release("sfs-obj-0001-a1b2c3d4")).isFalse();
    }

    @Test
    @DisplayName("retrieve is empty for unknown or blank identifiers")
    void retrieveIsEmptyForUnknownIds() {
        assertThat(store.retrieve("sfs-obj-9999-ffffffff")).isEqualTo(Optional.empty());
        assertThat(store.retrieve(" ")).isEqualTo(Optional.empty());
    }

    @Test
    @DisplayName("refuses null content and blank identifiers")
    void refusesNullContentAndBlankIds() {
        assertThatThrownBy(() -> store.store("sfs-obj-0001-a1b2c3d4", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.store(" ", "data".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
