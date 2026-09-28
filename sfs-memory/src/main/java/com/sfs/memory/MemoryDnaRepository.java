package com.sfs.memory;

import com.sfs.core.dna.DnaRepository;
import com.sfs.core.dna.StoredDna;
import com.sfs.core.dna.SemanticDna;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class MemoryDnaRepository implements DnaRepository {

    private final H2MemoryDatabase database;
    private final VectorIndex vectorIndex;

    public MemoryDnaRepository(H2MemoryDatabase database, VectorIndex vectorIndex) {
        this.database = Objects.requireNonNull(database, "database must not be null");
        this.vectorIndex = Objects.requireNonNull(vectorIndex, "vectorIndex must not be null");
    }

    @Override
    public StoredDna save(SemanticDna dna, Instant at) {
        StoredDna stored = database.saveSemanticDna(dna, at);
        if (dna.embedding().dimensions() > 0) {
            vectorIndex.upsert(dna.objectId(), dna.embedding().vector());
        } else {
            vectorIndex.remove(dna.objectId());
        }
        return stored;
    }

    @Override
    public Optional<StoredDna> find(String objectId) {
        return database.findStored(objectId);
    }

    @Override
    public List<StoredDna> history(String objectId) {
        return database.history(objectId);
    }

    @Override
    public int nextDnaVersion(String objectId) {
        return database.nextDnaVersion(objectId);
    }

    @Override
    public boolean remove(String objectId) {
        vectorIndex.remove(objectId);
        return database.removeDna(objectId);
    }

    @Override
    public int objectCount() {
        return database.dnaObjectCount();
    }

    public int vectorIndexSize() {
        return vectorIndex.size();
    }

    public void rebuildVectorIndex() {
        Map<String, List<Double>> vectors = new java.util.LinkedHashMap<>();
        for (StoredDna stored : database.allCurrentDna()) {
            if (stored.dna().embedding().dimensions() > 0) {
                vectors.put(stored.dna().objectId(), stored.dna().embedding().vector());
            }
        }
        vectorIndex.rebuild(vectors);
    }
}
