package com.sfs.core.rules;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Rule schema")
class RuleSchemaTest {

    @Test
    @DisplayName("a rule needs a well-formed id, type, priority and constraints")
    void ruleValidation() {
        assertThatThrownBy(() -> new Rule("Bad Id", RuleType.CONTENT,
                RulePriority.NORMAL, "d", List.of(new ContentConstraint(true, 0, 0))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Rule("ok", RuleType.CONTENT,
                RulePriority.NORMAL, " ", List.of(new ContentConstraint(true, 0, 0))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Rule("ok", RuleType.CONTENT,
                RulePriority.NORMAL, "d", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("a rule set binds to an object, DNA version and DNA hash")
    void ruleSetValidation() {
        Rule rule = new Rule("only", RuleType.CONTENT, RulePriority.NORMAL, "d",
                List.of(new ContentConstraint(true, 0, 0)));
        RuleSet set = new RuleSet("sfs-obj-0001-a1b2c3d4", 3,
                "a".repeat(64), "sfs-rules/0.2", List.of(rule));
        assertThat(set.dnaVersion()).isEqualTo(3);
        assertThat(set.bindingKey()).isEqualTo("sfs-obj-0001-a1b2c3d4@3-aaaaaaaaaaaa");
    }

    @Test
    @DisplayName("duplicate rule ids are refused")
    void duplicateRuleIdsRefused() {
        Rule a = new Rule("same", RuleType.CONTENT, RulePriority.NORMAL, "d",
                List.of(new ContentConstraint(true, 0, 0)));
        Rule b = new Rule("same", RuleType.STRUCTURE, RulePriority.HIGH, "d",
                List.of(new StructureConstraint(1, 1, List.of())));
        assertThatThrownBy(() -> new RuleSet("sfs-obj-0001-a1b2c3d4", 1,
                "a".repeat(64), "sfs-rules/0.2", List.of(a, b)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate rule id");
    }

    @Test
    @DisplayName("constraint records validate their own invariants")
    void constraintValidation() {
        assertThatThrownBy(() -> new RequiredFactConstraint(" ", true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RequiredEntityConstraint("X", 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new StructureConstraint(5, 2, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OrderingConstraint(true, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ValidationConstraint(true, true, 1.5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("priorities carry comparable weights")
    void priorities() {
        assertThat(RulePriority.CRITICAL.weight())
                .isLessThan(RulePriority.HIGH.weight())
                .isLessThan(RulePriority.NORMAL.weight())
                .isLessThan(RulePriority.LOW.weight());
    }
}
