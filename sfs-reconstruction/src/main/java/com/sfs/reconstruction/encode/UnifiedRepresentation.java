package com.sfs.reconstruction.encode;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public record UnifiedRepresentation(
        String summary,
        List<EncodedSection> sections,
        EncodedContent content,
        List<EncodedRelationship> relationships,
        int protectedReferenceCount) {

    public UnifiedRepresentation {
        if (summary == null || summary.isBlank()) {
            throw new IllegalArgumentException("summary must not be blank");
        }
        sections = sections == null ? List.of() : List.copyOf(sections);
        relationships = relationships == null ? List.of() : List.copyOf(relationships);
        if (protectedReferenceCount < 0) {
            throw new IllegalArgumentException(
                    "protectedReferenceCount must not be negative");
        }
    }

    public Set<String> vocabulary() {
        Set<String> tokens = new HashSet<>();
        addTokens(tokens, summary);
        for (EncodedSection section : sections) {
            addTokens(tokens, section.heading());
        }
        for (EncodedFact fact : content.facts()) {
            addTokens(tokens, fact.statement());
            if (fact.critical()) {
                tokens.add(EncodedFact.CRITICAL_MARKER);
            }
        }
        for (EncodedEntity entity : content.entities()) {
            addTokens(tokens, entity.name());
            addTokens(tokens, entity.type());
        }
        for (String concept : content.concepts()) {
            addTokens(tokens, concept);
        }
        for (String topic : content.topics()) {
            addTokens(tokens, topic);
        }
        for (EncodedRelationship relationship : relationships) {
            addTokens(tokens, relationship.subject());
            addTokens(tokens, relationship.type());
            addTokens(tokens, relationship.object());
        }
        return Set.copyOf(tokens);
    }

    private static void addTokens(Set<String> into, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        for (String token : text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (!token.isBlank()) {
                into.add(token);
            }
        }
    }
}
