package com.sfs.core.dna;

import java.util.List;
import java.util.Objects;

public record SemanticDna(
        DnaIdentity identity,
        String summary,
        List<Concept> concepts,
        List<Topic> topics,
        List<Entity> entities,
        List<Fact> facts,
        List<Relationship> relationships,
        List<StructureNode> structure,
        EmbeddingRef embedding,
        BehaviourProfile behaviour,
        List<ReconstructionRuleRef> reconstructionRules,
        FidelityProfile fidelity,
        SecurityProfile security) {

    public SemanticDna {
        Objects.requireNonNull(identity, "identity must not be null");
        Objects.requireNonNull(summary, "summary must not be null");
        if (summary.isBlank()) {
            throw new IllegalArgumentException("summary must not be blank");
        }
        Objects.requireNonNull(embedding, "embedding must not be null");
        Objects.requireNonNull(behaviour, "behaviour must not be null");
        Objects.requireNonNull(fidelity, "fidelity must not be null");
        Objects.requireNonNull(security, "security must not be null");
        concepts = concepts == null ? List.of() : List.copyOf(concepts);
        topics = topics == null ? List.of() : List.copyOf(topics);
        entities = entities == null ? List.of() : List.copyOf(entities);
        facts = facts == null ? List.of() : List.copyOf(facts);
        relationships = relationships == null ? List.of() : List.copyOf(relationships);
        structure = structure == null ? List.of() : List.copyOf(structure);
        reconstructionRules = reconstructionRules == null
                ? List.of()
                : List.copyOf(reconstructionRules);
    }

    public String objectId() {
        return identity.objectId();
    }

    public int dnaVersion() {
        return identity.dnaVersion();
    }

    public String schemaVersion() {
        return identity.schemaVersion();
    }
}
