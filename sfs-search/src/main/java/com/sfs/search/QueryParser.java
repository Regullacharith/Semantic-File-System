package com.sfs.search;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class QueryParser {

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "a", "an", "and", "or", "of", "to", "in", "on", "for", "with", "is",
            "are", "was", "were", "be", "been", "this", "that", "these", "those",
            "it", "its", "as", "at", "by", "from", "has", "have", "had", "will",
            "would", "should", "must", "can", "could", "may", "might", "their",
            "they", "them", "we", "our", "us", "you", "your", "not", "no", "but",
            "if", "then", "than", "so", "such", "which", "who", "what", "when",
            "where", "how", "all", "any", "each", "into", "about", "find", "search",
            "show", "me", "please", "documents", "document", "files", "file");

    public ParsedQuery parse(String text) {
        Objects.requireNonNull(text, "text must not be null");
        String stripped = text.strip();
        if (ParsedQuery.looksLikeObjectId(stripped)) {
            return new ParsedQuery(stripped, List.of(), stripped);
        }
        Set<String> terms = new LinkedHashSet<>();
        for (String raw : stripped.toLowerCase(java.util.Locale.ROOT)
                .split("[^\\p{L}\\p{Nd}'.-]+")) {
            String term = raw.strip();
            if (term.length() >= 2 && !STOP_WORDS.contains(term)) {
                terms.add(term);
            }
        }
        if (terms.isEmpty()) {
            throw new IllegalArgumentException(
                    "the query carries no searchable terms after parsing");
        }
        return new ParsedQuery(stripped, new ArrayList<>(terms), null);
    }
}
