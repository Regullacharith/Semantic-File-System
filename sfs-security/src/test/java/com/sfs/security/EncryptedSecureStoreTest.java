package com.sfs.security;

import com.sfs.contracts.security.SecretRecord;
import com.sfs.contracts.security.SecretSubmission;
import com.sfs.contracts.semantic.ProtectedReferenceView.SensitiveType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Encrypted secure store and key separation (13.4, 13.5)")
class EncryptedSecureStoreTest {

    @TempDir
    Path root;

    private FileEncryptedSecureStore store(String name) {
        return new FileEncryptedSecureStore(
                root.resolve(name + "-secure"),
                new FileKeyManager(root.resolve(name + "-keys")));
    }

    @Test
    @DisplayName("a submitted secret is encrypted and resolves to the exact value")
    void roundtrip() {
        FileEncryptedSecureStore secureStore = store("roundtrip");
        SecretRecord record = secureStore.encryptAndStore(new SecretSubmission(
                "sfs-ref-aaaaaaaa0001", "sfs-obj-7001-aaaaaaaa",
                SensitiveType.API_KEY, "sk-live-9f8e7d6c5b4a"));

        assertThat(secureStore.find("sfs-ref-aaaaaaaa0001")).isPresent();
        assertThat(secureStore.decrypt(record))
                .isEqualTo("sk-live-9f8e7d6c5b4a");
        assertThat(record.ciphertextBase64())
                .isNotEqualTo("sk-live-9f8e7d6c5b4a");
        assertThat(record.metadata().algorithm()).isEqualTo("AES-256-GCM");
        assertThat(record.reversible()).isEqualTo("authorized-resolution-only");
    }

    @Test
    @DisplayName("keys live outside the secure directory and ciphertext never holds key material")
    void keySeparation() throws Exception {
        FileEncryptedSecureStore secureStore = store("separation");
        FileKeyManager keyManager = new FileKeyManager(root.resolve("separation-keys"));

        secureStore.encryptAndStore(new SecretSubmission(
                "sfs-ref-aaaaaaaa0002", "sfs-obj-7001-aaaaaaaa",
                SensitiveType.API_KEY, "sk-live-9f8e7d6c5b4a"));

        assertThat(keyManager.keyFile())
                .isEqualTo(root.resolve("separation-keys").resolve("master.key"));
        assertThat(keyManager.keyFile().getParent())
                .isNotEqualTo(secureStore.storeFile().getParent());

        String storeContent = Files.readString(secureStore.storeFile());
        assertThat(storeContent).doesNotContain("sk-live-9f8e7d6c5b4a");
        String keyContent = Files.readString(keyManager.keyFile());
        assertThat(keyContent).doesNotContain("AES-256-GCM");
        assertThat(keyContent).doesNotContain("ciphertext");
    }

    @Test
    @DisplayName("state persists across restarts with the same key")
    void persists() {
        FileEncryptedSecureStore first = store("persist");
        first.encryptAndStore(new SecretSubmission(
                "sfs-ref-aaaaaaaa0003", "sfs-obj-7001-aaaaaaaa",
                SensitiveType.API_KEY, "sk-live-9f8e7d6c5b4a"));

        FileEncryptedSecureStore reopened = store("persist");

        assertThat(reopened.contains("sfs-ref-aaaaaaaa0003")).isTrue();
        assertThat(reopened.decrypt(reopened.find("sfs-ref-aaaaaaaa0003")
                .orElseThrow())).isEqualTo("sk-live-9f8e7d6c5b4a");
    }

    @Test
    @DisplayName("a different key cannot decrypt another store's secret")
    void wrongKeyRefused() {
        FileEncryptedSecureStore first = store("wrong-a");
        FileEncryptedSecureStore second = store("wrong-b");
        SecretRecord record = first.encryptAndStore(new SecretSubmission(
                "sfs-ref-aaaaaaaa0004", "sfs-obj-7001-aaaaaaaa",
                SensitiveType.API_KEY, "sk-live-9f8e7d6c5b4a"));

        assertThatThrownBy(() -> second.decrypt(record))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("resolution refused");
    }

    @Test
    @DisplayName("non-reversible types are refused as secret records")
    void passwordsNeverStored() {
        FileEncryptedSecureStore secureStore = store("never");

        assertThatThrownBy(() -> new SecretRecord("sfs-ref-aaaaaaaa0005",
                "sfs-obj-7001-aaaaaaaa", SensitiveType.PASSWORD, "x", "iv",
                null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> secureStore.encryptAndStore(new SecretSubmission(
                "sfs-ref-aaaaaaaa0005", "sfs-obj-7001-aaaaaaaa",
                SensitiveType.PASSWORD, "hunter2")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("never reversibly stored");
        assertThat(secureStore.size()).isZero();
    }

    @Test
    @DisplayName("unknown references look up empty and size tracks stores")
    void lookups() {
        FileEncryptedSecureStore secureStore = store("lookups");

        assertThat(secureStore.find("sfs-ref-aaaaaaaa0006")).isEmpty();
        assertThat(secureStore.contains(null)).isFalse();
        assertThat(secureStore.size()).isZero();
        secureStore.encryptAndStore(new SecretSubmission(
                "sfs-ref-aaaaaaaa0006", "sfs-obj-7001-aaaaaaaa",
                SensitiveType.API_KEY, "sk-live-9f8e7d6c5b4a"));
        assertThat(secureStore.size()).isEqualTo(1);
    }
}
