package com.sfs.contracts.semantic;

import java.util.List;
import java.util.Objects;

/**
 * Read-only projection of a Semantic DNA record for inspection.
 */
public record SemanticDnaView(
        String objectId,
        String schemaVersion,
        int dnaVersion,
        String summary,
        List<String> concepts,
        List<String> topics,
        List<EntityView> entities,
        List<FactView> facts,
        List<RelationshipView> relationships,
        List<StructureNodeView> structure,
        List<ProtectedReferenceView> protectedReferences,
        int embeddingDimensions,
        FidelityProfileView fidelity) {

    /**
     * Canonical constructor.
     */
    public SemanticDnaView {
        Objects.requireNonNull(objectId, "objectId must not be null");
        Objects.requireNonNull(schemaVersion, "schemaVersion must not be null");
        Objects.requireNonNull(summary, "summary must not be null");
        Objects.requireNonNull(fidelity, "fidelity must not be null");

        if (objectId.isBlank()) {
            throw new IllegalArgumentException("objectId must not be blank");
        }
        if (schemaVersion.isBlank()) {
            throw new IllegalArgumentException("schemaVersion must not be blank");
        }
        if (summary.isBlank()) {
            throw new IllegalArgumentException("summary must not be blank");
        }
        if (dnaVersion < 1) {
            throw new IllegalArgumentException("dnaVersion must be at least 1");
        }
        if (embeddingDimensions < 0) {
            throw new IllegalArgumentException("embeddingDimensions must not be negative");
        }

        concepts = List.copyOf(Objects.requireNonNull(concepts, "concepts must not be null"));
        topics = List.copyOf(Objects.requireNonNull(topics, "topics must not be null"));
        entities = List.copyOf(Objects.requireNonNull(entities, "entities must not be null"));
        facts = List.copyOf(Objects.requireNonNull(facts, "facts must not be null"));
        relationships = List.copyOf(
                Objects.requireNonNull(relationships, "relationships must not be null"));
        structure = List.copyOf(Objects.requireNonNull(structure, "structure must not be null"));
        protectedReferences = List.copyOf(Objects.requireNonNull(
                protectedReferences, "protectedReferences must not be null"));
    }

    public boolean hasProtectedReferences() {
        return !protectedReferences.isEmpty();
    }

    public boolean hasEmbedding() {
        return embeddingDimensions > 0;
    }

    public record EntityView(String name, String type, int mentions) {
        public EntityView {
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(type, "type must not be null");
            if (name.isBlank()) {
                throw new IllegalArgumentException("entity name must not be blank");
            }
            if (mentions < 1) {
                throw new IllegalArgumentException("mentions must be at least 1");
            }
        }
    }

    public record FactView(String statement, boolean critical, double confidence) {
        public FactView {
            Objects.requireNonNull(statement, "statement must not be null");
            if (statement.isBlank()) {
                throw new IllegalArgumentException("fact statement must not be blank");
            }
            if (confidence < 0.0 || confidence > 1.0) {
                throw new IllegalArgumentException("confidence must be between 0.0 and 1.0");
            }
        }

        /** @return confidence as a whole percentage, for display */
        public int confidencePercent() {
            return (int) Math.round(confidence * 100);
        }
    }

    public record RelationshipView(String subject, String type, String object) {
        public RelationshipView {
            Objects.requireNonNull(subject, "subject must not be null");
            Objects.requireNonNull(type, "type must not be null");
            Objects.requireNonNull(object, "object must not be null");
            if (subject.isBlank() || type.isBlank() || object.isBlank()) {
                throw new IllegalArgumentException("relationship parts must not be blank");
            }
        }
    }

    public record StructureNodeView(String heading, int level, int order) {
        public StructureNodeView {
            Objects.requireNonNull(heading, "heading must not be null");
            if (heading.isBlank()) {
                throw new IllegalArgumentException("heading must not be blank");
            }
            if (level < 1 || level > 6) {
                throw new IllegalArgumentException("level must be between 1 and 6");
            }
            if (order < 0) {
                throw new IllegalArgumentException("order must not be negative");
            }
        }
    }

    public record FidelityProfileView(
            double extractionConfidence,
            double structuralCompleteness,
            String analyzerVersion) {

        public FidelityProfileView {
            Objects.requireNonNull(analyzerVersion, "analyzerVersion must not be null");
            if (analyzerVersion.isBlank()) {
                throw new IllegalArgumentException("analyzerVersion must not be blank");
            }
            if (extractionConfidence < 0.0 || extractionConfidence > 1.0) {
                throw new IllegalArgumentException(
                        "extractionConfidence must be between 0.0 and 1.0");
            }
            if (structuralCompleteness < 0.0 || structuralCompleteness > 1.0) {
                throw new IllegalArgumentException(
                        "structuralCompleteness must be between 0.0 and 1.0");
            }
        }

        public int extractionConfidencePercent() {
            return (int) Math.round(extractionConfidence * 100);
        }

        public int structuralCompletenessPercent() {
            return (int) Math.round(structuralCompleteness * 100);
        }
    }
}
