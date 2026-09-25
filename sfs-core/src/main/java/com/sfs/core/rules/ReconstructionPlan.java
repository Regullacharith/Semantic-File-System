package com.sfs.core.rules;

import com.sfs.core.dna.Relationship;

import java.util.List;
import java.util.Objects;

public record ReconstructionPlan(
        String objectId,
        int dnaVersion,
        String dnaSha256,
        String rulesVersion,
        List<String> sectionOrder,
        List<RequiredFact> requiredFacts,
        List<RequiredEntity> requiredEntities,
        List<Relationship> requiredRelationships,
        ContentContract content,
        ValidationContract validation,
        List<String> warnings) {

    public ReconstructionPlan {
        Objects.requireNonNull(objectId, "objectId must not be null");
        Objects.requireNonNull(dnaSha256, "dnaSha256 must not be null");
        Objects.requireNonNull(rulesVersion, "rulesVersion must not be null");
        Objects.requireNonNull(sectionOrder, "sectionOrder must not be null");
        Objects.requireNonNull(requiredFacts, "requiredFacts must not be null");
        Objects.requireNonNull(requiredEntities, "requiredEntities must not be null");
        Objects.requireNonNull(requiredRelationships, "requiredRelationships must not be null");
        Objects.requireNonNull(content, "content must not be null");
        Objects.requireNonNull(validation, "validation must not be null");
        Objects.requireNonNull(warnings, "warnings must not be null");
        if (dnaVersion < 1) {
            throw new IllegalArgumentException("dnaVersion must be at least 1");
        }
        sectionOrder = List.copyOf(sectionOrder);
        requiredFacts = List.copyOf(requiredFacts);
        requiredEntities = List.copyOf(requiredEntities);
        requiredRelationships = List.copyOf(requiredRelationships);
        warnings = List.copyOf(warnings);
    }

    public record ContentContract(
            boolean preserveSummaryVerbatim,
            String summary,
            int minConcepts,
            int minTopics) {

        public ContentContract {
            Objects.requireNonNull(summary, "summary must not be null");
            if (minConcepts < 0 || minTopics < 0) {
                throw new IllegalArgumentException("minimum counts must not be negative");
            }
        }
    }

    public record ValidationContract(
            boolean forbidInventedFacts,
            boolean requireAllCriticalFacts,
            double minFactConfidence) {

        public ValidationContract {
            if (minFactConfidence < 0.0 || minFactConfidence > 1.0) {
                throw new IllegalArgumentException(
                        "minFactConfidence must be between 0.0 and 1.0");
            }
        }
    }
}
