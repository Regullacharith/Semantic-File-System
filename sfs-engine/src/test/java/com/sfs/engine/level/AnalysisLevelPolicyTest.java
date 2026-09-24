package com.sfs.engine.level;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AnalysisLevelPolicy")
class AnalysisLevelPolicyTest {

    @Test
    @DisplayName("the V1 policy enables only STANDARD and defaults to it")
    void v1Policy() {
        AnalysisLevelPolicy policy = AnalysisLevelPolicy.v1();
        assertThat(policy.isEnabled(AnalysisLevel.STANDARD)).isTrue();
        assertThat(policy.isEnabled(AnalysisLevel.SHALLOW)).isFalse();
        assertThat(policy.isEnabled(AnalysisLevel.DEEP)).isFalse();
        assertThat(policy.defaultLevel()).isEqualTo(AnalysisLevel.STANDARD);
    }

    @Test
    @DisplayName("refusals for disabled levels carry an explicit reason")
    void refusalReason() {
        AnalysisLevelPolicy policy = AnalysisLevelPolicy.v1();
        assertThat(policy.refusalReason(AnalysisLevel.DEEP))
                .contains("DEEP")
                .contains("not enabled");
    }

    @Test
    @DisplayName("a custom strategy can enable several levels")
    void customStrategy() {
        AnalysisLevelPolicy policy = new AnalysisLevelPolicy(
                Set.of(AnalysisLevel.SHALLOW, AnalysisLevel.STANDARD), AnalysisLevel.STANDARD);
        assertThat(policy.isEnabled(AnalysisLevel.SHALLOW)).isTrue();
        assertThat(policy.isEnabled(AnalysisLevel.DEEP)).isFalse();
    }

    @Test
    @DisplayName("policies are validated")
    void validation() {
        assertThatThrownBy(() -> new AnalysisLevelPolicy(Set.of(), AnalysisLevel.STANDARD))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AnalysisLevelPolicy(
                Set.of(AnalysisLevel.SHALLOW), AnalysisLevel.DEEP))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
