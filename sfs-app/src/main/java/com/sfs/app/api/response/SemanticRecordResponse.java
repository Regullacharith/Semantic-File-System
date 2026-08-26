package com.sfs.app.api.response;

import com.sfs.contracts.semantic.ProtectedReferenceView;
import com.sfs.contracts.semantic.SemanticDnaView;

import java.util.List;
import java.util.Objects;

public record SemanticRecordResponse(
        String objectId,
        boolean present,
        String reason,
        String schemaVersion,
        Integer dnaVersion,
        String summary,
        List<String> concepts,
        List<String> topics,
        List<EntityResponse> entities,
        List<FactResponse> facts,
        List<RelationshipResponse> relationships,
        List<StructureNodeResponse> structure,
        List<ProtectedReferenceResponse> protectedReferences,
        Integer embeddingDimensions,
        FidelityProfileResponse analysisQuality) {

    public SemanticRecordResponse {
        Objects.requireNonNull(objectId, "objectId must not be null");

        concepts = concepts == null ? List.of() : List.copyOf(concepts);
        topics = topics == null ? List.of() : List.copyOf(topics);
        entities = entities == null ? List.of() : List.copyOf(entities);
        facts = facts == null ? List.of() : List.copyOf(facts);
        relationships = relationships == null ? List.of() : List.copyOf(relationships);
        structure = structure == null ? List.of() : List.copyOf(structure);
        protectedReferences =
                protectedReferences == null ? List.of() : List.copyOf(protectedReferences);
    }

    public static SemanticRecordResponse absent(String objectId, String reason) {
        return new SemanticRecordResponse(
                objectId, false, reason, null, null, null,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                null, null);
    }

    public static SemanticRecordResponse from(SemanticDnaView dna) {
        Objects.requireNonNull(dna, "dna must not be null");

        return new SemanticRecordResponse(
                dna.objectId(),
                true,
                null,
                dna.schemaVersion(),
                dna.dnaVersion(),
                dna.summary(),
                dna.concepts(),
                dna.topics(),
                dna.entities().stream().map(EntityResponse::from).toList(),
                dna.facts().stream().map(FactResponse::from).toList(),
                dna.relationships().stream().map(RelationshipResponse::from).toList(),
                dna.structure().stream().map(StructureNodeResponse::from).toList(),
                dna.protectedReferences().stream().map(ProtectedReferenceResponse::from).toList(),
                dna.embeddingDimensions(),
                FidelityProfileResponse.from(dna.fidelity()));
    }

    public record EntityResponse(String name, String type, int mentions) {

        public static EntityResponse from(SemanticDnaView.EntityView entity) {
            return new EntityResponse(entity.name(), entity.type(), entity.mentions());
        }
    }

    public record FactResponse(String statement, boolean critical, double confidence) {

        public static FactResponse from(SemanticDnaView.FactView fact) {
            return new FactResponse(fact.statement(), fact.critical(), fact.confidence());
        }
    }

    public record RelationshipResponse(String subject, String type, String object) {

        public static RelationshipResponse from(SemanticDnaView.RelationshipView relationship) {
            return new RelationshipResponse(
                    relationship.subject(), relationship.type(), relationship.object());
        }
    }

    public record StructureNodeResponse(String heading, int level, int order) {

        public static StructureNodeResponse from(SemanticDnaView.StructureNodeView node) {
            return new StructureNodeResponse(node.heading(), node.level(), node.order());
        }
    }

    public record ProtectedReferenceResponse(
            String referenceId,
            String sensitiveType,
            String sensitiveTypeLabel,
            String semanticRole,
            String location,
            boolean resolvable) {

        public static ProtectedReferenceResponse from(ProtectedReferenceView reference) {
            return new ProtectedReferenceResponse(
                    reference.referenceId(),
                    reference.sensitiveType().name(),
                    reference.sensitiveType().getLabel(),
                    reference.semanticRole(),
                    reference.location(),
                    reference.resolvable());
        }
    }

    public record FidelityProfileResponse(
            double extractionConfidence,
            double structuralCompleteness,
            String analyzerVersion) {

        public static FidelityProfileResponse from(SemanticDnaView.FidelityProfileView profile) {
            return new FidelityProfileResponse(
                    profile.extractionConfidence(),
                    profile.structuralCompleteness(),
                    profile.analyzerVersion());
        }
    }
}
