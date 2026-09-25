package com.sfs.core.rules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RuleSetValidator {

    public List<String> validate(RuleSet set) {
        List<String> issues = new ArrayList<>();
        Map<RuleType, List<Constraint>> constraintsByType = new HashMap<>();
        for (Rule rule : set.rules()) {
            for (Constraint constraint : rule.constraints()) {
                if (!typeMatches(rule.type(), constraint)) {
                    issues.add("rule " + rule.ruleId() + " carries a constraint of the "
                            + "wrong type for " + rule.type());
                }
                constraintsByType.computeIfAbsent(rule.type(), ignored -> new ArrayList<>())
                        .add(constraint);
            }
        }
        for (Rule rule : set.rules()) {
            for (Constraint constraint : rule.constraints()) {
                if (constraint instanceof StructureConstraint structure
                        && structure.minHeadings() > structure.maxHeadings()) {
                    issues.add("rule " + rule.ruleId() + " has inverted heading bounds");
                }
                if (constraint instanceof ValidationConstraint validation
                        && (validation.minFactConfidence() < 0.0
                        || validation.minFactConfidence() > 1.0)) {
                    issues.add("rule " + rule.ruleId() + " has an out-of-range confidence");
                }
            }
        }
        long orderingRules = set.rules().stream()
                .filter(rule -> rule.type() == RuleType.ORDERING)
                .count();
        if (orderingRules > 1) {
            issues.add("more than one ordering rule in the set");
        }
        return issues;
    }

    public boolean isValid(RuleSet set) {
        return validate(set).isEmpty();
    }

    private static boolean typeMatches(RuleType type, Constraint constraint) {
        return switch (type) {
            case CONTENT -> constraint instanceof ContentConstraint;
            case STRUCTURE -> constraint instanceof StructureConstraint;
            case FACT -> constraint instanceof RequiredFactConstraint;
            case ENTITY -> constraint instanceof RequiredEntityConstraint;
            case RELATIONSHIP -> constraint instanceof RelationshipConstraint;
            case ORDERING -> constraint instanceof OrderingConstraint;
            case VALIDATION -> constraint instanceof ValidationConstraint;
        };
    }
}
