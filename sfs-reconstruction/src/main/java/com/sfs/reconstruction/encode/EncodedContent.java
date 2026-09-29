package com.sfs.reconstruction.encode;

import java.util.List;

public record EncodedContent(
        List<EncodedFact> facts,
        List<EncodedEntity> entities,
        List<String> concepts,
        List<String> topics) {

    public EncodedContent {
        facts = facts == null ? List.of() : List.copyOf(facts);
        entities = entities == null ? List.of() : List.copyOf(entities);
        concepts = concepts == null ? List.of() : List.copyOf(concepts);
        topics = topics == null ? List.of() : List.copyOf(topics);
    }
}
