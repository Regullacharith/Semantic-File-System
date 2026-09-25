package com.sfs.core.rules;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RuleSetValidator")
class RuleSetValidatorTest {

    private final RuleSetValidator validator = new RuleSetValidator();

    @Test
    @DisplayName("a derived set is valid")
    void derivedSetIsValid() {
        assertThat(validator.validate(RuleSetsFixture.derived())).isEmpty();
        assertThat(validator.isValid(RuleSetsFixture.everyKind())).isTrue();
    }

    @Test
    @DisplayName("a constraint of the wrong type for its rule is flagged")
    void wrongTypeConstraint() {
        RuleSet mixed = new RuleSet("sfs-obj-0001-a1b2c3d4", 1, "a".repeat(64),
                RuleSetCanonical.RULES_SCHEMA_VERSION,
                List.of(new Rule("facts", RuleType.FACT, RulePriority.CRITICAL, "d",
                        List.of(new ContentConstraint(true, 0, 0)))));
        assertThat(validator.validate(mixed))
                .anyMatch(issue -> issue.contains("wrong type"));
    }

    @Test
    @DisplayName("more than one ordering rule is flagged")
    void duplicateOrdering() {
        RuleSet doubles = new RuleSet("sfs-obj-0001-a1b2c3d4", 1, "a".repeat(64),
                RuleSetCanonical.RULES_SCHEMA_VERSION,
                List.of(
                        new Rule("ordering-a", RuleType.ORDERING, RulePriority.HIGH, "d",
                                List.of(new OrderingConstraint(true, List.of("A", "B")))),
                        new Rule("ordering-b", RuleType.ORDERING, RulePriority.HIGH, "d",
                                List.of(new OrderingConstraint(true, List.of("A", "B"))))));
        assertThat(validator.validate(doubles))
                .anyMatch(issue -> issue.contains("more than one ordering rule"));
    }
}
