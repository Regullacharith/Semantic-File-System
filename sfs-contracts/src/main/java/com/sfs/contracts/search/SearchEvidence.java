package com.sfs.contracts.search;

import java.util.Objects;

/**
 * A single piece of evidence explaining why an object matched a query.
 */
public record SearchEvidence(EvidenceType type, String detail) {

    /**
     * Canonical constructor.
     */
    public SearchEvidence {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(detail, "detail must not be null");

        if (detail.isBlank()) {
            throw new IllegalArgumentException("detail must not be blank");
        }
    }

    /**
     * The aspect of the Semantic DNA that produced a match.
     */
    public enum EvidenceType {

        /** Matched a concept recorded in the Semantic DNA. */
        CONCEPT("Concept"),

        /** Matched a topic. */
        TOPIC("Topic"),

        /** Matched a named entity. */
        ENTITY("Entity"),

        /** Matched an explicitly recorded fact. */
        FACT("Fact"),

        /** Matched a typed relationship between entities. */
        RELATIONSHIP("Relationship"),

        /** Matched the document summary. */
        SUMMARY("Summary"),

        /**
         * Matched by embedding proximity alone.
         */
        VECTOR_SIMILARITY("Semantic similarity");

        private final String label;

        EvidenceType(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }
}
