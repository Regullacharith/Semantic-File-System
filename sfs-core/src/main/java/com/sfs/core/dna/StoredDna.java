package com.sfs.core.dna;

import java.time.Instant;
import java.util.Objects;

public record StoredDna(
        SemanticDna dna,
        String canonicalSha256,
        String previousSha256,
        Instant storedAt) {

    public StoredDna {
        Objects.requireNonNull(dna, "dna must not be null");
        Objects.requireNonNull(canonicalSha256, "canonicalSha256 must not be null");
        Objects.requireNonNull(storedAt, "storedAt must not be null");
    }

    public boolean chainsTo(StoredDna previous) {
        if (previous == null) {
            return previousSha256() == null;
        }
        return previousSha256() != null
                && previousSha256().equals(previous.canonicalSha256())
                && previous.dna().dnaVersion() == dna.dnaVersion() - 1;
    }
}
