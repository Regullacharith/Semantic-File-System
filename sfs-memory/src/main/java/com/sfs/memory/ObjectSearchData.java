package com.sfs.memory;

import java.util.List;
import java.util.Objects;

public record ObjectSearchData(
        String objectId,
        String fileName,
        String contentType,
        String state,
        String summary,
        List<String> concepts,
        List<String> topics,
        List<String> entities,
        List<String> facts,
        List<String> relationships,
        int protectedReferenceCount,
        List<Double> embedding) {

    private static final List<String> SEARCHABLE_STATES =
            List.of("ANALYZED", "MEMORY_COMMITTED", "MEMORIZED");

    public ObjectSearchData {
        Objects.requireNonNull(objectId, "objectId must not be null");
        Objects.requireNonNull(fileName, "fileName must not be null");
        Objects.requireNonNull(contentType, "contentType must not be null");
        Objects.requireNonNull(state, "state must not be null");
        summary = summary == null ? "" : summary;
        concepts = concepts == null ? List.of() : List.copyOf(concepts);
        topics = topics == null ? List.of() : List.copyOf(topics);
        entities = entities == null ? List.of() : List.copyOf(entities);
        facts = facts == null ? List.of() : List.copyOf(facts);
        relationships = relationships == null ? List.of() : List.copyOf(relationships);
        embedding = embedding == null ? null : List.copyOf(embedding);
    }

    public boolean searchable() {
        return SEARCHABLE_STATES.contains(state);
    }
}
