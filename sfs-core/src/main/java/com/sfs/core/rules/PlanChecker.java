package com.sfs.core.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class PlanChecker {

    public PlanCompliance check(ReconstructionPlan plan, String candidateText) {
        Objects.requireNonNull(plan, "plan must not be null");
        Objects.requireNonNull(candidateText, "candidateText must not be null");
        String candidate = normalize(candidateText);
        List<String> violations = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (RequiredFact fact : plan.requiredFacts()) {
            if (!candidate.contains(normalize(fact.statement()))) {
                violations.add("required fact missing: " + fact.statement());
            }
        }
        for (RequiredEntity entity : plan.requiredEntities()) {
            long mentions = countOccurrences(candidate, normalize(entity.name()));
            if (mentions < entity.minMentions()) {
                violations.add("required entity '" + entity.name() + "' appears " + mentions
                        + " time(s) but at least " + entity.minMentions()
                        + " are required");
            }
        }
        for (com.sfs.core.dna.Relationship relationship : plan.requiredRelationships()) {
            boolean subjectPresent =
                    candidate.contains(normalize(relationship.subject()));
            boolean objectPresent =
                    candidate.contains(normalize(relationship.object()));
            if (!subjectPresent || !objectPresent) {
                violations.add("required relationship '"
                        + relationship.subject() + " " + relationship.type() + " "
                        + relationship.object() + "' is not expressible in the candidate");
            }
        }
        checkSectionOrder(plan, candidate, violations);
        checkContent(plan, candidate, violations);
        return new PlanCompliance(violations, warnings);
    }

    private void checkSectionOrder(ReconstructionPlan plan, String candidate,
                                   List<String> violations) {
        if (plan.sectionOrder().size() < 2) {
            return;
        }
        int cursor = -1;
        for (String heading : plan.sectionOrder()) {
            int index = candidate.indexOf(normalize(heading));
            if (index < 0) {
                violations.add("required heading missing: " + heading);
                return;
            }
            if (index < cursor) {
                violations.add("section order broken at heading: " + heading);
                return;
            }
            cursor = index;
        }
    }

    private void checkContent(ReconstructionPlan plan, String candidate,
                              List<String> violations) {
        ReconstructionPlan.ContentContract content = plan.content();
        if (content.preserveSummaryVerbatim() && !content.summary().isBlank()) {
            if (!candidate.contains(normalize(content.summary()))) {
                violations.add("the summary is not preserved verbatim");
            }
        }
    }
    
    public static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").strip();
    }

    private static long countOccurrences(String text, String needle) {
        long count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
