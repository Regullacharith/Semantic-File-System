package com.sfs.search;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class QueryEmbedder {

    public static final int DIMENSIONS = 64;

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "a", "an", "and", "or", "of", "to", "in", "on", "for", "with", "is",
            "are", "was", "were", "be", "been", "being", "this", "that", "these", "those",
            "it", "its", "as", "at", "by", "from", "has", "have", "had", "will", "would",
            "should", "must", "can", "could", "may", "might", "shall", "their", "they",
            "them", "we", "our", "us", "you", "your", "he", "she", "his", "her", "not",
            "no", "but", "if", "then", "than", "so", "such", "which", "who", "whom",
            "whose", "what", "when", "where", "how", "all", "any", "each", "more",
            "most", "other", "some", "only", "also", "into", "over", "under", "after",
            "before", "between", "during", "through", "up", "down", "out", "off",
            "about", "per", "via", "do", "does", "did", "done", "there", "here");

    public List<Double> embed(List<String> terms) {
        Objects.requireNonNull(terms, "terms must not be null");
        double[] vector = new double[DIMENSIONS];
        int contributing = 0;
        for (String term : terms) {
            String token = term.toLowerCase(Locale.ROOT);
            if (token.isEmpty() || STOP_WORDS.contains(token)) {
                continue;
            }
            contributing++;
            vector[Math.floorMod(token.hashCode(), DIMENSIONS)] += 1.0;
        }
        if (contributing == 0) {
            throw new IllegalArgumentException("the query has no embeddable terms");
        }
        double norm = 0.0;
        for (double value : vector) {
            norm += value * value;
        }
        norm = Math.sqrt(norm);
        Double[] normalized = new Double[DIMENSIONS];
        for (int i = 0; i < DIMENSIONS; i++) {
            normalized[i] = norm == 0.0 ? 0.0 : vector[i] / norm;
        }
        return List.of(normalized);
    }
}
