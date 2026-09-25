package com.sfs.core.rules;

import com.sfs.core.dna.Relationship;
import com.sfs.core.dna.SemanticDna;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ReconstructionPlanner {

    private final RuleRepository repository;
    private final RuleDeriver deriver = new RuleDeriver();
    private final RuleSetValidator validator = new RuleSetValidator();
    private final RuleConflictDetector conflictDetector = new RuleConflictDetector();

    public ReconstructionPlanner(RuleRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    public ReconstructionPlan plan(SemanticDna dna) {
        return plan(dna, Instant.now());
    }

    public ReconstructionPlan plan(SemanticDna dna, Instant at) {
        Objects.requireNonNull(dna, "dna must not be null");
        RuleLookup lookup = repository.findOrDerive(dna, deriver, at);
        RuleSet set = lookup.ruleSet();

        List<String> refusals = new ArrayList<>(validator.validate(set));
        List<String> warnings = new ArrayList<>();
        for (RuleConflict conflict : conflictDetector.detect(set)) {
            if (conflict.severity() == RuleConflict.Severity.ERROR) {
                refusals.add(conflict.description());
            } else {
                warnings.add(conflict.description());
            }
        }
        if (lookup.versionConflict() != null) {
            warnings.add(lookup.versionConflict().description());
        }
        if (!refusals.isEmpty()) {
            throw new RulePlanningException(refusals);
        }
        return assemble(dna, set, warnings);
    }

    private static ReconstructionPlan assemble(SemanticDna dna, RuleSet set,
                                               List<String> warnings) {
        List<String> sectionOrder = List.of();
        ReconstructionPlan.ContentContract content =
                new ReconstructionPlan.ContentContract(true, dna.summary(), 0, 0);
        List<RequiredFact> requiredFacts = new ArrayList<>();
        List<RequiredEntity> requiredEntities = new ArrayList<>();
        List<Relationship> requiredRelationships = new ArrayList<>();
        ReconstructionPlan.ValidationContract validation =
                new ReconstructionPlan.ValidationContract(true, true, 0.7);

        for (Rule rule : set.rules()) {
            for (Constraint constraint : rule.constraints()) {
                if (constraint instanceof ContentConstraint contentConstraint) {
                    content = new ReconstructionPlan.ContentContract(
                            contentConstraint.preserveSummaryVerbatim(),
                            contentConstraint.preserveSummaryVerbatim()
                                    ? dna.summary()
                                    : "",
                            contentConstraint.minConcepts(),
                            contentConstraint.minTopics());
                } else if (constraint instanceof StructureConstraint structure) {
                    sectionOrder = structure.requiredHeadings();
                } else if (constraint instanceof OrderingConstraint ordering) {
                    sectionOrder = ordering.sectionOrder();
                } else if (constraint instanceof RequiredFactConstraint requiredFact) {
                    requiredFacts.add(new RequiredFact(requiredFact.statement()));
                } else if (constraint instanceof RequiredEntityConstraint requiredEntity) {
                    requiredEntities.add(new RequiredEntity(
                            requiredEntity.name(), requiredEntity.minMentions()));
                } else if (constraint instanceof RelationshipConstraint relationship) {
                    requiredRelationships.add(new Relationship(
                            relationship.subject(), relationship.type(),
                            relationship.object()));
                } else if (constraint instanceof ValidationConstraint validationConstraint) {
                    validation = new ReconstructionPlan.ValidationContract(
                            validationConstraint.forbidInventedFacts(),
                            validationConstraint.requireAllCriticalFacts(),
                            validationConstraint.minFactConfidence());
                }
            }
        }
        return new ReconstructionPlan(
                dna.objectId(),
                dna.dnaVersion(),
                set.dnaSha256(),
                set.rulesVersion(),
                sectionOrder,
                requiredFacts,
                requiredEntities,
                requiredRelationships,
                content,
                validation,
                warnings);
    }
}
