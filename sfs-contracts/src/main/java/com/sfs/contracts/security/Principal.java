package com.sfs.contracts.security;

import java.util.Objects;
import java.util.Set;

public record Principal(String id, String displayName, Set<Capability> capabilities) {

    public Principal {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(displayName, "displayName must not be null");
        Objects.requireNonNull(capabilities, "capabilities must not be null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }

        capabilities = Set.copyOf(capabilities);
    }

    public boolean has(Capability capability) {
        Objects.requireNonNull(capability, "capability must not be null");
        return capabilities.contains(capability);
    }
}
