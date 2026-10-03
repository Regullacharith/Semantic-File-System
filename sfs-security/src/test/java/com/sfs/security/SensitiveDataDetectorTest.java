package com.sfs.security;

import com.sfs.contracts.semantic.ProtectedReferenceView.SensitiveType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Sensitive-data detector (13.1)")
class SensitiveDataDetectorTest {

    private final SensitiveDataDetector detector = new SensitiveDataDetector();

    @Test
    @DisplayName("credential assignments are typed by their key")
    void assignments() {
        List<SensitiveValue> values = detector.detect("""
                password=hunter2
                API_KEY = sk-live-9f8e7d6c5b4a
                access_token: eyJhbGciOiJIUzI1NiJ9
                client_secret = "abcdef123456"
                """);

        assertThat(values).extracting(SensitiveValue::type)
                .containsExactly(SensitiveType.PASSWORD, SensitiveType.API_KEY,
                        SensitiveType.ACCESS_TOKEN, SensitiveType.OTHER);
        assertThat(values).extracting(SensitiveValue::exactValue)
                .containsExactly("hunter2", "sk-live-9f8e7d6c5b4a",
                        "eyJhbGciOiJIUzI1NiJ9", "\"abcdef123456\"");
        assertThat(values).allSatisfy(value -> {
            assertThat(value.credentialAssignment()).isTrue();
            assertThat(value.location()).startsWith("line ");
        });
    }

    @Test
    @DisplayName("emails, key-shaped values and bearer tokens are detected")
    void identifiers() {
        List<SensitiveValue> values = detector.detect("""
                Contact alice@example.com for access.
                The key sk-live-9f8e7d6c5b4a3210 is active.
                Authorization: Bearer abcdef123456ghijkl
                """);

        assertThat(values).extracting(SensitiveValue::type)
                .contains(SensitiveType.EMAIL_ADDRESS, SensitiveType.API_KEY,
                        SensitiveType.ACCESS_TOKEN);
        assertThat(values).extracting(SensitiveValue::exactValue)
                .contains("alice@example.com", "sk-live-9f8e7d6c5b4a3210");
    }

    @Test
    @DisplayName("phone numbers are detected while dates and short numbers are not")
    void phones() {
        List<SensitiveValue> values = detector.detect("""
            Reach the team at +1 415 555 0123.
            The plan is due on 2026-09-15.
            Version 1.2.3 shipped.
            Call 040 123 4567 tomorrow.
            """);

        assertThat(values).extracting(SensitiveValue::type)
                .containsOnly(SensitiveType.PHONE_NUMBER);
        assertThat(values).extracting(SensitiveValue::exactValue)
                .contains("+1 415 555 0123", "040 123 4567");
        assertThat(values).extracting(SensitiveValue::exactValue)
                .doesNotContain("2026-09-15", "1.2.3");
    }

    @Test
    @DisplayName("account identifiers and postal addresses are detected when labeled")
    void accountsAndAddresses() {
        List<SensitiveValue> values = detector.detect("""
                account number: XJ44-90881
                Address: 221B Baker Street, London
                """);

        assertThat(values).extracting(SensitiveValue::type)
                .contains(SensitiveType.ACCOUNT_IDENTIFIER,
                        SensitiveType.POSTAL_ADDRESS);
    }

    @Test
    @DisplayName("ordinary content produces no detections")
    void cleanContent() {
        assertThat(detector.detect("""
                # Summary

                This report reviews the database platform for Q3 2026.
                Query latency decreased by 40 percent after indexing changes.
                The plan is due on 2026-09-15.
                """)).isEmpty();
    }

    @Test
    @DisplayName("line sensitivity mirrors value detection")
    void lineSensitivity() {
        assertThat(detector.isSensitiveLine("password=hunter2")).isTrue();
        assertThat(detector.isSensitiveLine("mail bob@example.com")).isTrue();
        assertThat(detector.isSensitiveLine("The plan is due on 2026-09-15."))
                .isFalse();
        assertThat(detector.isSensitiveLine(null)).isFalse();
        assertThat(detector.isCredentialAssignment("api_key=x")).isTrue();
        assertThat(detector.isCredentialAssignment("see bob@example.com")).isFalse();
    }
}
