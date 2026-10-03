package com.sfs.evaluation;

import com.sfs.core.dna.SemanticDna;

import java.util.HashSet;
import java.util.Set;

public final class SemanticEvaluator implements Evaluator<SemanticScore> {

    @Override
    public SemanticScore evaluate(EvaluationInput input) {
        Set<String> reference = referenceTokens(input);
        Set<String> artifact = TextMatching.contentTokens(input.artifactText());
        Set<String> matched = new HashSet<>(artifact);
        matched.retainAll(reference);
        Set<String> artifactOnly = new HashSet<>(artifact);
        artifactOnly.removeAll(reference);

        double precision = artifact.isEmpty()
                ? 0.0
                : (double) matched.size() / artifact.size();
        double recall = reference.isEmpty()
                ? 0.0
                : (double) matched.size() / reference.size();
        double f1 = (precision + recall) == 0.0
                ? 0.0
                : 2.0 * precision * recall / (precision + recall);
        return new SemanticScore(f1, reference.size(), matched.size(),
                artifactOnly.size());
    }

    private Set<String> referenceTokens(EvaluationInput input) {
        if (input.hasOriginal()) {
            return TextMatching.contentTokens(input.originalText());
        }
        return materialTokens(input.dna());
    }

    static Set<String> materialTokens(SemanticDna dna) {
        Set<String> tokens = new HashSet<>();
        tokens.addAll(TextMatching.contentTokens(dna.summary()));
        dna.facts().forEach(fact ->
                tokens.addAll(TextMatching.contentTokens(fact.statement())));
        dna.entities().forEach(entity ->
                tokens.addAll(TextMatching.contentTokens(entity.name())));
        dna.relationships().forEach(relationship -> {
            tokens.addAll(TextMatching.contentTokens(relationship.subject()));
            tokens.addAll(TextMatching.contentTokens(relationship.object()));
        });
        dna.concepts().forEach(concept ->
                tokens.addAll(TextMatching.contentTokens(concept.name())));
        dna.topics().forEach(topic ->
                tokens.addAll(TextMatching.contentTokens(topic.name())));
        dna.structure().forEach(node ->
                tokens.addAll(TextMatching.contentTokens(node.heading())));
        return tokens;
    }
}
