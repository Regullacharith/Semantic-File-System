package com.sfs.lifecycle.store;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryRawContentStore implements RawContentStore {

    private final Map<String, byte[]> contentByObjectId = new ConcurrentHashMap<>();

    @Override
    public void store(String objectId, byte[] content) {
        if (objectId == null || objectId.isBlank()) {
            throw new IllegalArgumentException("objectId must not be blank");
        }
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
        contentByObjectId.put(objectId, Arrays.copyOf(content, content.length));
    }

    @Override
    public Optional<byte[]> retrieve(String objectId) {
        byte[] stored = contentByObjectId.get(objectId);
        if (stored == null) {
            return Optional.empty();
        }
        return Optional.of(Arrays.copyOf(stored, stored.length));
    }

    @Override
    public boolean release(String objectId) {
        return contentByObjectId.remove(objectId) != null;
    }

    @Override
    public boolean contains(String objectId) {
        return contentByObjectId.containsKey(objectId);
    }
}
