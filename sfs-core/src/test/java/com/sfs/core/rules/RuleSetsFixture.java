package com.sfs.core.rules;

import com.sfs.core.dna.DnaFixtures;
import com.sfs.core.dna.SemanticDna;

import java.util.List;

public final class RuleSetsFixture {

    private RuleSetsFixture() {
    }

    public static RuleSet derived() {
        return new RuleDeriver().derive(DnaFixtures.sample());
    }

    public static RuleSet everyKind() {
        return new RuleSet("sfs-obj-0001-a1b2c3d4", 1, "a".repeat(64),
                RuleSetCanonical.RULES_SCHEMA_VERSION,
                List.of(
                        new Rule("content", RuleType.CONTENT, RulePriority.NORMAL, "d",
                                List.of(new ContentConstraint(true, 1, 1))),
                        new Rule("structure", RuleType.STRUCTURE, RulePriority.HIGH, "d",
                                List.of(new StructureConstraint(1, 2, List.of("Summary")))),
                        new Rule("facts", RuleType.FACT, RulePriority.CRITICAL, "d",
                                List.of(new RequiredFactConstraint("A fact.", true))),
                        new Rule("entities", RuleType.ENTITY, RulePriority.HIGH, "d",
                                List.of(new RequiredEntityConstraint("PostgreSQL", 2))),
                        new Rule("relationships", RuleType.RELATIONSHIP,
                                RulePriority.NORMAL, "d",
                                List.of(new RelationshipConstraint("A", "hosts", "B", true))),
                        new Rule("ordering", RuleType.ORDERING, RulePriority.HIGH, "d",
                                List.of(new OrderingConstraint(true, List.of("Summary")))),
                        new Rule("validation", RuleType.VALIDATION,
                                RulePriority.CRITICAL, "d",
                                List.of(new ValidationConstraint(true, true, 0.7)))));
    }

    public static SemanticDna dna() {
        return DnaFixtures.sample();
    }
}
