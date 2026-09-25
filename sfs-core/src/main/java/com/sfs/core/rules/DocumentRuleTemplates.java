package com.sfs.core.rules;

import com.sfs.core.dna.SemanticDna;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class DocumentRuleTemplates {

    private DocumentRuleTemplates() {
    }

    public static Optional<Rule> contentRule(SemanticDna dna) {
        boolean migrated = "migrated-document".equals(
                dna.behaviour().documentType());
        int minConcepts = migrated ? 0 : Math.min(3, dna.concepts().size());
        int minTopics = migrated ? 0 : Math.min(3, dna.topics().size());
        return Optional.of(new Rule(
                "content-contract",
                RuleType.CONTENT,
                RulePriority.NORMAL,
                "Preserve the summary verbatim and the extracted concept and topic surface.",
                List.of(new ContentConstraint(true, minConcepts, minTopics))));
    }

    public static Optional<Rule> structureRule(SemanticDna dna) {
        int headings = dna.structure().size();
        boolean bodyOnly = headings == 1
                && "Body".equals(dna.structure().getFirst().heading());
        boolean migrated = "migrated-document".equals(dna.behaviour().documentType());
        if (headings == 0 || bodyOnly || migrated) {
            return Optional.empty();
        }
        List<String> requiredHeadings = dna.structure().stream()
                .map(node -> node.heading())
                .filter(heading -> !"Body".equals(heading))
                .toList();
        return Optional.of(new Rule(
                "structure-outline",
                RuleType.STRUCTURE,
                RulePriority.HIGH,
                "Reproduce the document outline with its recorded headings.",
                List.of(new StructureConstraint(
                        Math.max(1, requiredHeadings.size()),
                        Math.max(1, requiredHeadings.size()),
                        requiredHeadings))));
    }

    public static Optional<Rule> orderingRule(SemanticDna dna) {
        if (dna.structure().size() < 2
                || "migrated-document".equals(dna.behaviour().documentType())) {
            return Optional.empty();
        }
        List<String> order = dna.structure().stream()
                .map(node -> node.heading())
                .toList();
        return Optional.of(new Rule(
                "section-order",
                RuleType.ORDERING,
                RulePriority.HIGH,
                "Reconstruct sections in the recorded document order.",
                List.of(new OrderingConstraint(true, order))));
    }

    public static Rule validationRule(SemanticDna dna) {
        double minConfidence = dna.facts().stream()
                .filter(com.sfs.core.dna.Fact::critical)
                .mapToDouble(com.sfs.core.dna.Fact::confidence)
                .min()
                .orElse(0.7);
        return new Rule(
                "reconstruction-validation",
                RuleType.VALIDATION,
                RulePriority.CRITICAL,
                "Never invent critical facts; require every critical fact to survive.",
                List.of(new ValidationConstraint(true, true, minConfidence)));
    }

    static String describe(SemanticDna dna) {
        return dna.behaviour().documentType().toLowerCase(Locale.ROOT);
    }
}
