package com.sfs.core.dna;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryDnaRepository {

    private final Map<String, List<StoredDna>> versionsByObjectId = new ConcurrentHashMap<>();

    public StoredDna save(SemanticDna dna, Instant at) {
        Objects.requireNonNull(dna, "dna must not be null");
        Objects.requireNonNull(at, "at must not be null");
        List<StoredDna> history = versionsByObjectId.get(dna.objectId());
        if (history != null && !history.isEmpty()
                && history.getLast().dna().dnaVersion() >= dna.dnaVersion()) {
            throw new IllegalArgumentException(
                    "DNA version must increase; the stored version is "
                            + history.getLast().dna().dnaVersion());
        }
        String canonicalHash = DnaCanonical.integrityHash(dna);
        String previousHash = history == null || history.isEmpty()
                ? null
                : history.getLast().canonicalSha256();
        StoredDna stored = new StoredDna(dna, canonicalHash, previousHash, at);
        versionsByObjectId.computeIfAbsent(dna.objectId(), ignored -> new ArrayList<>())
                .add(stored);
        return stored;
    }

    public Optional<StoredDna> find(String objectId) {
        List<StoredDna> history = versionsByObjectId.get(objectId);
        return history == null || history.isEmpty()
                ? Optional.empty()
                : Optional.of(history.getLast());
    }

    public List<StoredDna> history(String objectId) {
        List<StoredDna> history = versionsByObjectId.get(objectId);
        return history == null ? List.of() : List.copyOf(history);
    }

    public int nextDnaVersion(String objectId) {
        List<StoredDna> history = versionsByObjectId.get(objectId);
        return history == null || history.isEmpty() ? 1 : history.getLast().dna().dnaVersion() + 1;
    }

    public boolean remove(String objectId) {
        return objectId != null && versionsByObjectId.remove(objectId) != null;
    }

    public int objectCount() {
        return versionsByObjectId.size();
    }
}
