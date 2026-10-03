package com.sfs.evaluation;

import com.sfs.core.dna.StructureNode;

import java.util.ArrayList;
import java.util.List;

public final class StructuralEvaluator implements Evaluator<StructuralScore> {

    @Override
    public StructuralScore evaluate(EvaluationInput input) {
        List<StructureNode> nodes = new ArrayList<>(input.dna().structure());
        nodes.sort(java.util.Comparator.comparingInt(StructureNode::order));

        int preserved = 0;
        int cursor = -1;
        boolean orderPreserved = true;
        for (StructureNode node : nodes) {
            int position = positionOf(input.artifactText(), node.heading());
            if (position >= 0) {
                preserved++;
                if (position < cursor) {
                    orderPreserved = false;
                }
                cursor = position;
            }
        }
        boolean summaryVerbatim =
                TextMatching.containsNormalized(input.artifactText(),
                        input.dna().summary());
        return new StructuralScore(nodes.size(), preserved, orderPreserved,
                summaryVerbatim);
    }

    private int positionOf(String artifact, String heading) {
        int index = TextMatching.normalize(artifact)
                .indexOf(TextMatching.normalize(heading));
        return index;
    }
}
