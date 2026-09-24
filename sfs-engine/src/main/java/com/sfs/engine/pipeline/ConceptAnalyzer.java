package com.sfs.engine.pipeline;

import com.sfs.engine.core.SemanticContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ConceptAnalyzer implements Analyzer {

    private static final int MAX_CONCEPTS = 4;

    @Override
    public String name() {
        return "concepts";
    }

    @Override
    public void perform(SemanticContext context, SemanticIntermediateRepresentation ir) {
        List<Map.Entry<String, Long>> top = ir.bigramFrequency().entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(MAX_CONCEPTS)
                .toList();

        Set<String> concepts = new LinkedHashSet<>();
        for (Map.Entry<String, Long> entry : top) {
            if (concepts.size() >= MAX_CONCEPTS) {
                break;
            }
            if (entry.getValue() >= 2 || ir.bigramFrequency().size() <= 3) {
                concepts.add(entry.getKey());
            }
        }
        if (concepts.isEmpty() && !ir.tokens().isEmpty()) {
            concepts.addAll(topUnigrams(ir, MAX_CONCEPTS));
        }
        ir.setConcepts(new ArrayList<>(concepts));
    }

    static List<String> topUnigrams(SemanticIntermediateRepresentation ir, int limit) {
        List<String> words = ir.wordFrequency().entrySet().stream()
                .filter(entry -> !StopWords.ALL.contains(entry.getKey()))
                .filter(entry -> entry.getKey().length() >= 3)
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
        return new ArrayList<>(words);
    }

    static List<String> orderedByFrequencyThenName(Map<String, Long> frequency) {
        return frequency.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey)
                .toList();
    }

    static Comparator<Map.Entry<String, Long>> frequencyOrder() {
        return Map.Entry.<String, Long>comparingByValue().reversed()
                .thenComparing(Map.Entry::getKey);
    }
}
