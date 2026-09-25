package com.sfs.core.dna;

import java.util.List;
import java.util.Objects;

public record SecurityProfile(
        String handlingPolicy,
        List<ProtectedReference> protectedReferences) {

    public SecurityProfile {
        Objects.requireNonNull(handlingPolicy, "handlingPolicy must not be null");
        if (handlingPolicy.isBlank()) {
            throw new IllegalArgumentException("handlingPolicy must not be blank");
        }
        protectedReferences = protectedReferences == null
                ? List.of()
                : List.copyOf(protectedReferences);
    }

    public static SecurityProfile unrestricted() {
        return new SecurityProfile("unrestricted-v1", List.of());
    }

    public boolean containsProtectedReferences() {
        return !protectedReferences.isEmpty();
    }
}
