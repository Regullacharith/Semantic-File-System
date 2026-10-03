package com.sfs.evaluation;

import java.util.ArrayList;
import java.util.List;

public final class EntityEvaluator implements Evaluator<EntityScore> {

    @Override
    public EntityScore evaluate(EvaluationInput input) {
        int total = 0;
        int preserved = 0;
        List<String> missing = new ArrayList<>();
        for (var entity : input.dna().entities()) {
            total++;
            if (TextMatching.countOccurrences(input.artifactText(),
                    entity.name()) >= 1) {
                preserved++;
            } else {
                missing.add(entity.name());
            }
        }
        return new EntityScore(total, preserved, missing);
    }
}
