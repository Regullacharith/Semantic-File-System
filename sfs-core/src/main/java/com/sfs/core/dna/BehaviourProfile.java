package com.sfs.core.dna;

import java.util.List;
import java.util.Objects;

public record BehaviourProfile(
        String documentType,
        boolean structured,
        double averageSentenceWords,
        List<String> guidance) {

    public BehaviourProfile {
        Objects.requireNonNull(documentType, "documentType must not be null");
        Objects.requireNonNull(guidance, "guidance must not be null");
        if (documentType.isBlank()) {
            throw new IllegalArgumentException("documentType must not be blank");
        }
        if (averageSentenceWords < 0) {
            throw new IllegalArgumentException("averageSentenceWords must not be negative");
        }
        guidance = List.copyOf(guidance);
    }
}
