package com.sfs.core.rules;

import com.sfs.core.dna.DnaCanonical;
import com.sfs.core.dna.SemanticDna;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class RuleDeriver {

    private static final int MAX_REQUIRED_ENTITIES = 8;
    private static final int MAX_REQUIRED_RELATIONSHIPS = 6;

    public RuleSet derive(SemanticDna dna) {
        Objects.requireNonNull(dna, "dna must not be null");
        List<Rule> rules = new ArrayList<>();
        DocumentRuleTemplates.contentRule(dna).ifPresent(rules::add);
        DocumentRuleTemplates.structureRule(dna).ifPresent(rules::add);
        rules.add(factRule(dna));
        entityRule(dna).ifPresent(rules::add);
        relationshipRule(dna).ifPresent(rules::add);
        DocumentRuleTemplates.orderingRule(dna).ifPresent(rules::add);
        rules.add(DocumentRuleTemplates.validationRule(dna));
        return new RuleSet(
                dna.objectId(),
                dna.dnaVersion(),
                DnaCanonical.integrityHash(dna),
                RuleSetCanonical.RULES_SCHEMA_VERSION,
                rules);
    }

    private static Rule factRule(SemanticDna dna) {
        List<Constraint> constraints = dna.facts().stream()
                .filter(com.sfs.core.dna.Fact::critical)
                .map(fact -> (Constraint) new RequiredFactConstraint(
                        fact.statement(), true))
                .toList();
        if (constraints.isEmpty()) {
            constraints = List.of((Constraint) new RequiredFactConstraint(
                    dna.facts().isEmpty()
                            ? dna.summary()
                            : dna.facts().getFirst().statement(),
                    true));
        }
        return new Rule(
                "required-facts",
                RuleType.FACT,
                RulePriority.CRITICAL,
                "Critical facts must survive reconstruction.",
                constraints);
    }

    private static Optional<Rule> entityRule(SemanticDna dna) {
        List<Constraint> constraints = dna.entities().stream()
                .filter(entity -> entity.mentions() >= 2)
                .limit(MAX_REQUIRED_ENTITIES)
                .map(entity -> (Constraint) new RequiredEntityConstraint(
                        entity.name(), entity.mentions()))
                .toList();
        if (constraints.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Rule(
                "required-entities",
                RuleType.ENTITY,
                RulePriority.HIGH,
                "Recurring entities must appear the recorded number of times.",
                constraints));
    }

    private static Optional<Rule> relationshipRule(SemanticDna dna) {
        List<Constraint> constraints = dna.relationships().stream()
                .limit(MAX_REQUIRED_RELATIONSHIPS)
                .map(relationship -> (Constraint) new RelationshipConstraint(
                        relationship.subject(), relationship.type(),
                        relationship.object(), true))
                .toList();
        if (constraints.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Rule(
                "required-relationships",
                RuleType.RELATIONSHIP,
                RulePriority.NORMAL,
                "Recorded relationships must remain expressible in the reconstruction.",
                constraints));
    }
}
