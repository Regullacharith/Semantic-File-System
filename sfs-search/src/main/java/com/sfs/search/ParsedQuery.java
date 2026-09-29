package com.sfs.search;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public record ParsedQuery(
        String originalText,
        List<String> terms,
        String exactObjectId) {

    private static final Pattern OBJECT_ID = Pattern.compile("^sfs-obj-[0-9]{4}-[a-zA-Z0-9]+$");

    public ParsedQuery {
        Objects.requireNonNull(originalText, "originalText must not be null");
        Objects.requireNonNull(terms, "terms must not be null");
        terms = List.copyOf(terms);
    }

    public Optional<String> exactLookupId() {
        return exactObjectId == null ? Optional.empty() : Optional.of(exactObjectId);
    }

    public boolean isExactLookup() {
        return exactObjectId != null;
    }

    static boolean looksLikeObjectId(String text) {
        String stripped = text.strip();
        return OBJECT_ID.matcher(stripped).matches();
    }

    static String normalize(String term) {
        return term.toLowerCase(Locale.ROOT);
    }
}
