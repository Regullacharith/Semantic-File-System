package com.sfs.core.rules;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RuleConflictDetector")
class RuleConflictDetectorTest {

    private final RuleConflictDetector detector = new RuleConflictDetector();

    @Test
    @DisplayName("a clean set has no conflicts")
    void clean() {
        assertThat(detector.detect(RuleSetsFixture.derived())).isEmpty();
        assertThat(detector.detect(RuleSetsFixture.everyKind())).isEmpty();
    }

    @Test
    @DisplayName("contradictory required-fact severity is an error")
    void factSeverityConflict() {
        RuleSet conflicting = new RuleSet("sfs-obj-0001-a1b2c3d4", 1, "a".repeat(64),
                RuleSetCanonical.RULES_SCHEMA_VERSION,
                List.of(
                        new Rule("facts-a", RuleType.FACT, RulePriority.CRITICAL, "d",
                                List.of(new RequiredFactConstraint("Same statement.", true))),
                        new Rule("facts-b", RuleType.FACT, RulePriority.CRITICAL, "d",
                                List.of(new RequiredFactConstraint("Same  statement.", false)))));
        List<RuleConflict> conflicts = detector.detect(conflicting);
        assertThat(conflicts).anySatisfy(conflict -> {
            assertThat(conflict.severity()).isEqualTo(RuleConflict.Severity.ERROR);
            assertThat(conflict.description()).contains("contradictory missing-severity");
        });
    }

    @Test
    @DisplayName("contradictory entity minimums are an error")
    void entityMinimumConflict() {
        RuleSet conflicting = new RuleSet("sfs-obj-0001-a1b2c3d4", 1, "a".repeat(64),
                RuleSetCanonical.RULES_SCHEMA_VERSION,
                List.of(
                        new Rule("entities-a", RuleType.ENTITY, RulePriority.HIGH, "d",
                                List.of(new RequiredEntityConstraint("PostgreSQL", 2))),
                        new Rule("entities-b", RuleType.ENTITY, RulePriority.HIGH, "d",
                                List.of(new RequiredEntityConstraint("postgresql", 5)))));
        assertThat(detector.detect(conflicting))
                .anySatisfy(conflict -> {
                    assertThat(conflict.severity()).isEqualTo(RuleConflict.Severity.ERROR);
                    assertThat(conflict.description()).contains("mention minimums");
                });
    }

    @Test
    @DisplayName("ordering rules that disagree are an error; duplicates warn")
    void orderingConflicts() {
        Rule ruleA = new Rule("ordering-a", RuleType.ORDERING, RulePriority.HIGH, "d",
                List.of(new OrderingConstraint(true, List.of("A", "B"))));
        Rule same = new Rule("ordering-b", RuleType.ORDERING, RulePriority.HIGH, "d",
                List.of(new OrderingConstraint(true, List.of("A", "B"))));
        Rule different = new Rule("ordering-c", RuleType.ORDERING, RulePriority.HIGH, "d",
                List.of(new OrderingConstraint(true, List.of("B", "A"))));

        assertThat(detector.detect(set(ruleA, same)))
                .anySatisfy(conflict -> assertThat(conflict.severity())
                        .isEqualTo(RuleConflict.Severity.WARNING));
        assertThat(detector.detect(set(ruleA, different)))
                .anySatisfy(conflict -> {
                    assertThat(conflict.severity()).isEqualTo(RuleConflict.Severity.ERROR);
                    assertThat(conflict.description()).contains("disagree");
                });
    }

    @Test
    @DisplayName("an ordering list smaller than the structure minimum is an error")
    void structureOrderingMismatch() {
        RuleSet conflicting = new RuleSet("sfs-obj-0001-a1b2c3d4", 1, "a".repeat(64),
                RuleSetCanonical.RULES_SCHEMA_VERSION,
                List.of(
                        new Rule("structure", RuleType.STRUCTURE, RulePriority.HIGH, "d",
                                List.of(new StructureConstraint(3, 3, List.of()))),
                        new Rule("ordering", RuleType.ORDERING, RulePriority.HIGH, "d",
                                List.of(new OrderingConstraint(true, List.of("A"))))));
        assertThat(detector.detect(conflicting))
                .anySatisfy(conflict -> {
                    assertThat(conflict.severity()).isEqualTo(RuleConflict.Severity.ERROR);
                    assertThat(conflict.description()).contains("requires at least 3");
                });
    }

    private RuleSet set(Rule... rules) {
        return new RuleSet("sfs-obj-0001-a1b2c3d4", 1, "a".repeat(64),
                RuleSetCanonical.RULES_SCHEMA_VERSION, List.of(rules));
    }
}
