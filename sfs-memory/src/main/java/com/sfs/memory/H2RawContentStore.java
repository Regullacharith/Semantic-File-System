package com.sfs.memory;

import com.sfs.lifecycle.store.RawContentStore;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public final class H2RawContentStore implements RawContentStore {

    private final H2MemoryDatabase database;

    public H2RawContentStore(H2MemoryDatabase database) {
        this.database = Objects.requireNonNull(database, "database must not be null");
    }

    @Override
    public void store(String objectId, byte[] content) {
        if (objectId == null || objectId.isBlank()) {
            throw new IllegalArgumentException("objectId must not be blank");
        }
        database.storeRaw(objectId, content);
    }

    @Override
    public Optional<byte[]> retrieve(String objectId) {
        return database.retrieveRaw(objectId).map(bytes -> Arrays.copyOf(bytes, bytes.length));
    }

    @Override
    public boolean release(String objectId) {
        return database.releaseRaw(objectId);
    }

    @Override
    public boolean contains(String objectId) {
        return database.containsRaw(objectId);
    }
}
