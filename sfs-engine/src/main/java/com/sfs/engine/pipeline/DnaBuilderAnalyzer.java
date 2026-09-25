package com.sfs.engine.pipeline;

import com.sfs.contracts.semantic.ProtectedReferenceView;
import com.sfs.core.dna.DnaSchemaValidator;
import com.sfs.core.dna.EmbeddingRef;
import com.sfs.core.dna.Fact;
import com.sfs.core.dna.SemanticDna;
import com.sfs.core.dna.SemanticDnaBuilder;
import com.sfs.core.dna.StructureNode;
import com.sfs.engine.core.SemanticContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class DnaBuilderAnalyzer implements Analyzer {

    public static final String SCHEMA_VERSION = DnaSchemaValidator.CURRENT_SCHEMA_VERSION;

    private final DnaSchemaValidator validator = new DnaSchemaValidator();

    @Override
    public String name() {
        return "dna-builder";
    }

    @Override
    public void perform(SemanticContext context, SemanticIntermediateRepresentation ir) {
        double[] vector = ir.embedding();
        EmbeddingRef embedding = vector.length == 0
                ? EmbeddingRef.absent()
                : new EmbeddingRef("feature-hashing/0.1", vector.length,
                        Arrays.stream(vector).boxed().toList());

        List<ProtectedReferenceView> sourceReferences = ir.protectedReferences();
        List<com.sfs.core.dna.ProtectedReference> references = new ArrayList<>();
        for (ProtectedReferenceView source : sourceReferences) {
            references.add(new com.sfs.core.dna.ProtectedReference(
                    source.referenceId(),
                    source.sensitiveType().name(),
                    source.semanticRole(),
                    source.location()));
        }

        int sentenceCount = Math.max(1, ir.sentences().size());
        double averageSentenceWords = (double) ir.tokens().size() / sentenceCount;

        SemanticDna dna = SemanticDnaBuilder
                .forIdentity(new com.sfs.core.dna.DnaIdentity(
                        context.objectId(),
                        SCHEMA_VERSION,
                        context.dnaVersion(),
                        context.engineVersion(),
                        context.submittedAt()))
                .summary(ir.summary())
                .concepts(ir.concepts())
                .topics(ir.topics())
                .entities(ir.entities().stream()
                        .map(entity -> new com.sfs.core.dna.Entity(
                                entity.name(), entity.type(), entity.mentions()))
                        .toList())
                .facts(ir.facts().stream()
                        .map(fact -> new Fact(
                                fact.statement(), fact.critical(), fact.confidence()))
                        .toList())
                .relationships(ir.relationships().stream()
                        .map(relationship -> new com.sfs.core.dna.Relationship(
                                relationship.subject(), relationship.type(),
                                relationship.object()))
                        .toList())
                .structure(ir.structure().stream()
                        .map(node -> new StructureNode(
                                node.heading(), node.level(), node.order()))
                        .toList())
                .embedding(embedding)
                .protectedReferences(references)
                .averageSentenceWords(averageSentenceWords)
                .fidelity(new com.sfs.core.dna.FidelityProfile(
                        extractionConfidence(ir),
                        structuralCompleteness(ir),
                        context.engineVersion()))
                .build();

        List<String> issues = validator.validate(dna);
        if (!issues.isEmpty()) {
            throw new IllegalStateException(
                    "built DNA is not schema-valid: " + String.join("; ", issues));
        }
        ir.setDnaDraft(dna);
    }

    static double extractionConfidence(SemanticIntermediateRepresentation ir) {
        double confidence = 0.55
                + 0.03 * Math.min(ir.facts().size(), 8)
                + 0.02 * Math.min(ir.entities().size(), 8);
        return Math.min(0.95, confidence);
    }

    static double structuralCompleteness(SemanticIntermediateRepresentation ir) {
        double completeness = 0.4 + 0.12 * Math.min(ir.structure().size(), 4);
        return Math.min(0.95, completeness);
    }
}
