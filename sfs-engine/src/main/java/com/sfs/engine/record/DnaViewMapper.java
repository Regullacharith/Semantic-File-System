package com.sfs.engine.record;

import com.sfs.contracts.semantic.ProtectedReferenceView;
import com.sfs.contracts.semantic.SemanticDnaView;
import com.sfs.core.dna.SemanticDna;

import java.util.Locale;

public final class DnaViewMapper {

    private DnaViewMapper() {
    }

    public static SemanticDnaView toView(SemanticDna dna) {
        return new SemanticDnaView(
                dna.objectId(),
                dna.schemaVersion(),
                dna.dnaVersion(),
                dna.summary(),
                dna.concepts().stream().map(concept -> concept.name()).toList(),
                dna.topics().stream().map(topic -> topic.name()).toList(),
                dna.entities().stream()
                        .map(entity -> new SemanticDnaView.EntityView(
                                entity.name(), entity.type(), entity.mentions()))
                        .toList(),
                dna.facts().stream()
                        .map(fact -> new SemanticDnaView.FactView(
                                fact.statement(), fact.critical(), fact.confidence()))
                        .toList(),
                dna.relationships().stream()
                        .map(relationship -> new SemanticDnaView.RelationshipView(
                                relationship.subject(), relationship.type(),
                                relationship.object()))
                        .toList(),
                dna.structure().stream()
                        .map(node -> new SemanticDnaView.StructureNodeView(
                                node.heading(), node.level(), node.order()))
                        .toList(),
                dna.security().protectedReferences().stream()
                        .map(reference -> new ProtectedReferenceView(
                                reference.referenceId(),
                                sensitiveType(reference.sensitiveType()),
                                reference.semanticRole(),
                                reference.location(),
                                true))
                        .toList(),
                dna.embedding().dimensions(),
                new SemanticDnaView.FidelityProfileView(
                        dna.fidelity().extractionConfidence(),
                        dna.fidelity().structuralCompleteness(),
                        dna.fidelity().analyzerVersion()));
    }

    private static ProtectedReferenceView.SensitiveType sensitiveType(String name) {
        try {
            return ProtectedReferenceView.SensitiveType.valueOf(name);
        } catch (IllegalArgumentException e) {
            return ProtectedReferenceView.SensitiveType.OTHER;
        }
    }
}
