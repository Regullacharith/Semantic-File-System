package com.sfs.evaluation;

import java.util.ArrayList;
import java.util.List;

public final class CompletenessEvaluator implements Evaluator<CompletenessScore> {

    @Override
    public CompletenessScore evaluate(EvaluationInput input) {
        List<String> units = new ArrayList<>();
        units.add(input.dna().summary());
        input.dna().structure().forEach(node -> units.add(node.heading()));
        input.dna().facts().forEach(fact -> units.add(fact.statement()));
        input.dna().entities().forEach(entity -> units.add(entity.name()));
        input.dna().relationships().forEach(relationship ->
                units.add(relationship.subject()));
        input.dna().concepts().forEach(concept -> units.add(concept.name()));
        input.dna().topics().forEach(topic -> units.add(topic.name()));

        long present = units.stream()
                .filter(unit -> TextMatching.containsNormalized(
                        input.artifactText(), unit))
                .count();
        return new CompletenessScore(units.size(), (int) present);
    }
}
