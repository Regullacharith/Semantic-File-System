package com.sfs.security;

import com.sfs.contracts.security.HandlingPolicy;
import com.sfs.contracts.security.SensitiveTypePolicy;
import com.sfs.contracts.semantic.ProtectedReferenceView.SensitiveType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Policy engine (13.2)")
class SecurityPolicyEngineTest {

    private final SecurityPolicyEngine engine = SecurityPolicyEngine.v1();

    @Test
    @DisplayName("the default table covers every sensitive type exactly once")
    void tableComplete() {
        List<SensitiveTypePolicy> policies = engine.policies();

        assertThat(policies).extracting(SensitiveTypePolicy::sensitiveType)
                .containsExactlyInAnyOrder(SensitiveType.values());
    }

    @Test
    @DisplayName("passwords and unclassified data fail closed to locked redaction")
    void failsClosed() {
        PolicyDecision password = engine.decide(new SensitiveValue(
                SensitiveType.PASSWORD, "hunter2", "credential assignment",
                "line 1", true));
        PolicyDecision other = engine.decide(new SensitiveValue(
                SensitiveType.OTHER, "unknown-value", "credential assignment",
                "line 2", true));

        assertThat(password.handling()).isEqualTo(HandlingPolicy.REDACT);
        assertThat(password.resolvable()).isFalse();
        assertThat(engine.policyFor(SensitiveType.PASSWORD).locked()).isTrue();
        assertThat(other.handling()).isEqualTo(HandlingPolicy.REDACT);
        assertThat(engine.policyFor(SensitiveType.OTHER).locked()).isTrue();
    }

    @Test
    @DisplayName("api keys are stored encrypted and stay resolvable under authorization")
    void apiKeysEncrypt() {
        PolicyDecision decision = engine.decide(new SensitiveValue(
                SensitiveType.API_KEY, "sk-live-9f8e7d6c5b4a",
                "credential assignment", "line 1", true));

        assertThat(decision.handling()).isEqualTo(HandlingPolicy.ENCRYPT);
        assertThat(decision.resolvable()).isTrue();
        assertThat(engine.policyFor(SensitiveType.API_KEY).locked()).isFalse();
    }

    @Test
    @DisplayName("reference ids are stable for the same value and differ per value")
    void referenceIds() {
        String first = engine.referenceIdFor("sk-live-9f8e7d6c5b4a");
        String second = engine.referenceIdFor("sk-live-9f8e7d6c5b4a");
        String third = engine.referenceIdFor("different-value");

        assertThat(first).isEqualTo(second).startsWith("sfs-ref-").hasSize(20);
        assertThat(third).isNotEqualTo(first);
    }

    @Test
    @DisplayName("a partial policy table is refused")
    void partialTableRefused() {
        assertThatThrownBy(() -> new SecurityPolicyEngine(List.of(
                engine.policyFor(SensitiveType.PASSWORD))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("every sensitive type");
    }
}
