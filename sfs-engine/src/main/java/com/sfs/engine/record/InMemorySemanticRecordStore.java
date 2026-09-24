package com.sfs.engine.record;

import com.sfs.contracts.semantic.SemanticDnaView;
import com.sfs.contracts.semantic.SemanticRecordService;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemorySemanticRecordStore implements SemanticRecordService {

    private final ConcurrentMap<String, SemanticDnaView> recordsByObjectId =
            new ConcurrentHashMap<>();

    @Override
    public Optional<SemanticDnaView> findSemanticDna(String objectId) {
        if (objectId == null || objectId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(recordsByObjectId.get(objectId));
    }

    public void save(SemanticDnaView dna) {
        Objects.requireNonNull(dna, "dna must not be null");
        recordsByObjectId.put(dna.objectId(), dna);
    }

    public int nextDnaVersion(String objectId) {
        return findSemanticDna(objectId)
                .map(current -> current.dnaVersion() + 1)
                .orElse(1);
    }

    public boolean remove(String objectId) {
        if (objectId == null) {
            return false;
        }
        return recordsByObjectId.remove(objectId) != null;
    }

    public int size() {
        return recordsByObjectId.size();
    }

    public Map<String, SemanticDnaView> snapshot() {
        return Map.copyOf(recordsByObjectId);
    }
}
