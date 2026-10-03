package com.sfs.evaluation;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class TextMatching {

    private static final Set<String> STOP_WORDS = Set.of(
            "this", "that", "with", "from", "have", "has", "had", "were", "been",
            "their", "them", "they", "after", "before", "about", "into", "onto",
            "over", "under", "then", "than", "when", "while", "where", "which",
            "will", "would", "shall", "should", "could", "must", "such", "very",
            "also", "some", "each", "every", "both", "between", "because", "does",
            "done", "doing", "upon", "per", "via", "aforementioned");

    private static final int MIN_TOKEN_LENGTH = 4;

    private TextMatching() {
    }

    public static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").strip();
    }

    public static boolean containsNormalized(String text, String statement) {
        return normalize(text).contains(normalize(statement));
    }

    public static long countOccurrences(String text, String needle) {
        long count = 0;
        int index = 0;
        String haystack = normalize(text);
        String target = normalize(needle);
        while ((index = haystack.indexOf(target, index)) >= 0) {
            count++;
            index += target.length();
        }
        return count;
    }

    public static Set<String> contentTokens(String text) {
        Set<String> tokens = new HashSet<>();
        if (text == null || text.isBlank()) {
            return tokens;
        }
        for (String token : normalize(text).split("[^a-z0-9]+")) {
            if (token.length() >= MIN_TOKEN_LENGTH && !STOP_WORDS.contains(token)) {
                tokens.add(token);
            }
        }
        return tokens;
    }
}
