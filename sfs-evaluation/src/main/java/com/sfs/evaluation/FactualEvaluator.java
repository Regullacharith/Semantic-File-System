package com.sfs.evaluation;

import java.util.ArrayList;
import java.util.List;

public final class FactualEvaluator implements Evaluator<FactualScore> {

    @Override
    public FactualScore evaluate(EvaluationInput input) {
        int total = 0;
        int preserved = 0;
        List<String> missing = new ArrayList<>();
        List<String> criticalMissing = new ArrayList<>();
        int criticalTotal = 0;
        int criticalPreserved = 0;

        for (var fact : input.dna().facts()) {
            total++;
            if (TextMatching.containsNormalized(input.artifactText(),
                    fact.statement())) {
                preserved++;
                if (fact.critical()) {
                    criticalPreserved++;
                }
            } else {
                missing.add(fact.statement());
                if (fact.critical()) {
                    criticalMissing.add(fact.statement());
                }
            }
            if (fact.critical()) {
                criticalTotal++;
            }
        }
        return new FactualScore(total, preserved,
                new CriticalFactScore(criticalTotal, criticalPreserved,
                        criticalMissing),
                missing);
    }
}
