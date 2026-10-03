package com.sfs.evaluation;

import java.util.ArrayList;
import java.util.List;

public final class RelationshipEvaluator implements Evaluator<RelationshipScore> {

    @Override
    public RelationshipScore evaluate(EvaluationInput input) {
        int total = 0;
        int preserved = 0;
        int withDirection = 0;
        List<String> missing = new ArrayList<>();
        for (var relationship : input.dna().relationships()) {
            total++;
            boolean subjectPresent = TextMatching.containsNormalized(
                    input.artifactText(), relationship.subject());
            boolean objectPresent = TextMatching.containsNormalized(
                    input.artifactText(), relationship.object());
            if (subjectPresent && objectPresent) {
                preserved++;
                if (TextMatching.containsNormalized(input.artifactText(),
                        relationship.type())) {
                    withDirection++;
                }
            } else {
                missing.add(relationship.subject() + " " + relationship.type()
                        + " " + relationship.object());
            }
        }
        return new RelationshipScore(total, preserved, withDirection, missing);
    }
}
