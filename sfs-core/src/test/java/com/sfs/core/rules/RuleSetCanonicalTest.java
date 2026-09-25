package com.sfs.core.rules;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Rule set canonical serialization")
class RuleSetCanonicalTest {

    @Test
    @DisplayName("serialize then parse is lossless")
    void roundTrip() {
        RuleSet set = RuleSetsFixture.derived();
        RuleSet parsed = RuleSetCanonical.parse(RuleSetCanonical.serialize(set));
        assertThat(parsed).isEqualTo(set);
    }

    @Test
    @DisplayName("serialization is deterministic and integrity-hashed")
    void deterministicAndHashed() {
        RuleSet set = RuleSetsFixture.derived();
        assertThat(RuleSetCanonical.serialize(set))
                .isEqualTo(RuleSetCanonical.serialize(set))
                .doesNotContain(", ");
        assertThat(RuleSetCanonical.integrityHash(set))
                .hasSize(64)
                .isEqualTo(RuleSetCanonical.integrityHash(
                        RuleSetCanonical.parse(RuleSetCanonical.serialize(set))));
    }

    @Test
    @DisplayName("every constraint kind survives the round trip")
    void allConstraintKinds() {
        RuleSet set = RuleSetsFixture.everyKind();
        RuleSet parsed = RuleSetCanonical.parse(RuleSetCanonical.serialize(set));
        assertThat(parsed).isEqualTo(set);
    }

    @Test
    @DisplayName("unknown constraint kinds and schema versions are refused")
    void refusals() {
        assertThatThrownBy(() -> RuleSetCanonical.parse(
                "{\"rulesSchemaVersion\":\"sfs-rules/9.9\",\"objectId\":\"x\","
                        + "\"dnaVersion\":1,\"dnaSha256\":\"" + "a".repeat(64)
                        + "\",\"rulesVersion\":\"sfs-rules/0.2\",\"rules\":[]}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported rules schema version");
        assertThatThrownBy(() -> RuleSetCanonical.parse(
                "{\"rulesSchemaVersion\":\"sfs-rules/0.2\",\"objectId\":\"x\","
                        + "\"dnaVersion\":1,\"dnaSha256\":\"" + "a".repeat(64)
                        + "\",\"rulesVersion\":\"sfs-rules/0.2\",\"rules\":[{"
                        + "\"ruleId\":\"r\",\"type\":\"CONTENT\",\"priority\":\"NORMAL\","
                        + "\"description\":\"d\",\"constraints\":[{\"kind\":\"mystery\"}]}]}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown constraint kind");
    }
}
