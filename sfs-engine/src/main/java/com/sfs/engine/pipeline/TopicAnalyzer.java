package com.sfs.engine.pipeline;

import com.sfs.engine.core.SemanticContext;

import java.util.ArrayList;
import java.util.List;

public final class TopicAnalyzer implements Analyzer {

    private static final int MAX_TOPICS = 4;

    @Override
    public String name() {
        return "topics";
    }

    @Override
    public void perform(SemanticContext context, SemanticIntermediateRepresentation ir) {
        List<String> topics = new ArrayList<>();
        for (String candidate : ConceptAnalyzer.topUnigrams(ir, 12)) {
            if (topics.size() >= MAX_TOPICS) {
                break;
            }
            boolean partOfConcept = ir.concepts().stream()
                    .anyMatch(concept -> concept.contains(candidate));
            if (!partOfConcept) {
                topics.add(candidate);
            }
        }
        ir.setTopics(topics);
    }
}
