package com.sfs.core.rules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RuleConflictDetector {

    public List<RuleConflict> detect(RuleSet set) {
        List<RuleConflict> conflicts = new ArrayList<>();
        detectFactConflicts(set, conflicts);
        detectEntityConflicts(set, conflicts);
        detectOrderingConflicts(set, conflicts);
        detectStructureOrderingMismatch(set, conflicts);
        return conflicts;
    }

    private void detectFactConflicts(RuleSet set, List<RuleConflict> conflicts) {
        Map<String, Set<Boolean>> failFlagsByStatement = new HashMap<>();
        for (Rule rule : set.rules()) {
            for (Constraint constraint : rule.constraints()) {
                if (constraint instanceof RequiredFactConstraint required) {
                    String key = normalize(required.statement());
                    failFlagsByStatement
                            .computeIfAbsent(key, ignored -> new HashSet<>())
                            .add(required.failIfMissing());
                }
            }
        }
        for (Map.Entry<String, Set<Boolean>> entry : failFlagsByStatement.entrySet()) {
            if (entry.getValue().size() > 1) {
                conflicts.add(RuleConflict.error(
                        "required fact '" + entry.getKey()
                                + "' is declared with contradictory missing-severity flags"));
            }
        }
    }

    private void detectEntityConflicts(RuleSet set, List<RuleConflict> conflicts) {
        Map<String, Set<Integer>> mentionsByName = new HashMap<>();
        for (Rule rule : set.rules()) {
            for (Constraint constraint : rule.constraints()) {
                if (constraint instanceof RequiredEntityConstraint required) {
                    mentionsByName
                            .computeIfAbsent(normalize(required.name()),
                                    ignored -> new HashSet<>())
                            .add(required.minMentions());
                }
            }
        }
        for (Map.Entry<String, Set<Integer>> entry : mentionsByName.entrySet()) {
            if (entry.getValue().size() > 1) {
                conflicts.add(RuleConflict.error(
                        "required entity '" + entry.getKey()
                                + "' is declared with contradictory mention minimums"));
            }
        }
    }

    private void detectOrderingConflicts(RuleSet set, List<RuleConflict> conflicts) {
        List<OrderingConstraint> ordering = new ArrayList<>();
        for (Rule rule : set.rules()) {
            for (Constraint constraint : rule.constraints()) {
                if (constraint instanceof OrderingConstraint orderingConstraint) {
                    ordering.add(orderingConstraint);
                }
            }
        }
        if (ordering.size() > 1) {
            OrderingConstraint first = ordering.getFirst();
            boolean allIdentical = ordering.stream()
                    .allMatch(candidate -> candidate.sectionOrder()
                            .equals(first.sectionOrder()));
            conflicts.add(allIdentical
                    ? RuleConflict.warning(
                    "duplicate ordering rules carry the same section order")
                    : RuleConflict.error(
                    "ordering rules disagree about the section order"));
        }
    }

    private void detectStructureOrderingMismatch(RuleSet set, List<RuleConflict> conflicts) {
        Integer minHeadings = null;
        List<String> sectionOrder = null;
        for (Rule rule : set.rules()) {
            for (Constraint constraint : rule.constraints()) {
                if (constraint instanceof StructureConstraint structure) {
                    minHeadings = structure.minHeadings();
                }
                if (constraint instanceof OrderingConstraint ordering) {
                    sectionOrder = ordering.sectionOrder();
                }
            }
        }
        if (minHeadings != null && sectionOrder != null
                && sectionOrder.size() < minHeadings) {
            conflicts.add(RuleConflict.error(
                    "ordering rule lists " + sectionOrder.size()
                            + " sections but the structure rule requires at least "
                            + minHeadings));
        }
    }

    private static String normalize(String text) {
        return text.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("\\s+", " ")
                .strip();
    }
}
