package com.sfs.engine.pipeline;

import java.util.Set;

final class StopWords {

    private StopWords() {
    }

    static final Set<String> ALL = Set.of(
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
}
