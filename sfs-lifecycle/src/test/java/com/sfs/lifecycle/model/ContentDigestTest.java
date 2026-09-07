package com.sfs.lifecycle.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ContentDigest")
class ContentDigestTest {

    @Test
    @DisplayName("produces the known SHA-256 of a UTF-8 string")
    void producesKnownSha256OfString() {
        assertThat(ContentDigest.sha256Hex("hello"))
                .isEqualTo("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
    }

    @Test
    @DisplayName("produces the known SHA-256 of empty content")
    void producesKnownSha256OfEmptyContent() {
        assertThat(ContentDigest.sha256Hex(""))
                .isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    @Test
    @DisplayName("hashes bytes and the equivalent UTF-8 string identically")
    void hashesBytesAndStringIdentically() {
        byte[] bytes = "grüße aus nizāmābād".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(ContentDigest.sha256Hex(bytes))
                .isEqualTo(ContentDigest.sha256Hex(new String(bytes, java.nio.charset.StandardCharsets.UTF_8)))
                .hasSize(64);
    }

    @Test
    @DisplayName("different content produces different digests")
    void differentContentProducesDifferentDigests() {
        assertThat(ContentDigest.sha256Hex("meeting notes"))
                .isNotEqualTo(ContentDigest.sha256Hex("meeting notes "));
    }
}
