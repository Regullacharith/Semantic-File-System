package com.sfs.core.dna;

import java.util.List;
import java.util.Objects;

public final class SemanticDnaBuilder {

    public static final List<ReconstructionRuleRef> SEED_RULES = List.of(
            new ReconstructionRuleRef("preserve-heading-order", "STRUCTURE_ORDER",
                    "sfs-rules/0.1"),
            new ReconstructionRuleRef("retain-critical-facts", "CRITICAL_FACTS",
                    "sfs-rules/0.1"),
            new ReconstructionRuleRef("retain-explicit-entities", "ENTITIES",
                    "sfs-rules/0.1"),
            new ReconstructionRuleRef("reconstruct-by-outline", "OUTLINE_RECONSTRUCTION",
                    "sfs-rules/0.1"));

    private final DnaIdentity identity;
    private String summary;
    private List<Concept> concepts = List.of();
    private List<Topic> topics = List.of();
    private List<Entity> entities = List.of();
    private List<Fact> facts = List.of();
    private List<Relationship> relationships = List.of();
    private List<StructureNode> structure = List.of();
    private EmbeddingRef embedding = EmbeddingRef.absent();
    private List<ProtectedReference> protectedReferences = List.of();
    private double averageSentenceWords;
    private FidelityProfile fidelity;

    private SemanticDnaBuilder(DnaIdentity identity) {
        this.identity = Objects.requireNonNull(identity, "identity must not be null");
    }

    public static SemanticDnaBuilder forIdentity(DnaIdentity identity) {
        return new SemanticDnaBuilder(identity);
    }

    public SemanticDnaBuilder summary(String summary) {
        this.summary = summary;
        return this;
    }

    public SemanticDnaBuilder concepts(List<String> names) {
        this.concepts = names == null ? List.of()
                : names.stream().map(Concept::new).toList();
        return this;
    }

    public SemanticDnaBuilder topics(List<String> names) {
        this.topics = names == null ? List.of()
                : names.stream().map(Topic::new).toList();
        return this;
    }

    public SemanticDnaBuilder entities(List<Entity> entities) {
        this.entities = entities == null ? List.of() : List.copyOf(entities);
        return this;
    }

    public SemanticDnaBuilder facts(List<Fact> facts) {
        this.facts = facts == null ? List.of() : List.copyOf(facts);
        return this;
    }

    public SemanticDnaBuilder relationships(List<Relationship> relationships) {
        this.relationships = relationships == null ? List.of() : List.copyOf(relationships);
        return this;
    }

    public SemanticDnaBuilder structure(List<StructureNode> structure) {
        this.structure = structure == null ? List.of() : List.copyOf(structure);
        return this;
    }

    public SemanticDnaBuilder embedding(EmbeddingRef embedding) {
        this.embedding = embedding == null ? EmbeddingRef.absent() : embedding;
        return this;
    }

    public SemanticDnaBuilder protectedReferences(List<ProtectedReference> references) {
        this.protectedReferences = references == null
                ? List.of()
                : List.copyOf(references);
        return this;
    }

    public SemanticDnaBuilder averageSentenceWords(double average) {
        this.averageSentenceWords = Math.max(0, average);
        return this;
    }

    public SemanticDnaBuilder fidelity(FidelityProfile fidelity) {
        this.fidelity = fidelity;
        return this;
    }

    public SemanticDna build() {
        Objects.requireNonNull(summary, "summary must be provided");
        Objects.requireNonNull(fidelity, "fidelity must be provided");
        boolean structured = structure.size() >= 2
                || (structure.size() == 1 && !"Body".equals(structure.getFirst().heading()));
        String documentType = structure.size() >= 2
                ? "structured-document"
                : (structured ? "headed-note" : "narrative");
        BehaviourProfile behaviour = new BehaviourProfile(
                documentType,
                structured,
                averageSentenceWords,
                List.of("preserve heading order",
                        "retain explicit facts and entities",
                        "reconstruct sections in document order"));
        SecurityProfile security = protectedReferences.isEmpty()
                ? SecurityProfile.unrestricted()
                : new SecurityProfile("protected-refs-v1", protectedReferences);
        return new SemanticDna(
                identity, summary, concepts, topics, entities, facts, relationships,
                structure, embedding, behaviour, SEED_RULES, fidelity, security);
    }
}
