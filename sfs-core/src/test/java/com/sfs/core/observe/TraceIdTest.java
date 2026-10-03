package com.sfs.core.observe;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Trace IDs")
class TraceIdTest {

    @Test
    @DisplayName("generated trace ids are 16 lowercase hex characters and unique")
    void generated() {
        TraceId first = TraceId.generate();
        TraceId second = TraceId.generate();

        assertThat(first.value()).matches("[0-9a-f]{16}");
        assertThat(second.value()).matches("[0-9a-f]{16}");
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("an externally supplied id is accepted only in valid form")
    void externalValidation() {
        assertThat(TraceId.isValid("0123456789abcdef")).isTrue();
        assertThat(new TraceId("0123456789abcdef").value()).isEqualTo("0123456789abcdef");

        assertThat(TraceId.isValid(null)).isFalse();
        assertThat(TraceId.isValid("")).isFalse();
        assertThat(TraceId.isValid("0123456789ABCDEF")).isFalse();
        assertThat(TraceId.isValid("0123456789abcdef0")).isFalse();
        assertThat(TraceId.isValid("0123456789abcdez")).isFalse();
        assertThatThrownBy(() -> new TraceId("nope"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("16 lowercase hexadecimal");
    }

    @Test
    @DisplayName("trace ids are value objects")
    void valueSemantics() {
        assertThat(new TraceId("aaaaaaaaaaaaaaaa"))
                .isEqualTo(new TraceId("aaaaaaaaaaaaaaaa"))
                .hasSameHashCodeAs(new TraceId("aaaaaaaaaaaaaaaa"));
    }
}
