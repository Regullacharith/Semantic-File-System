package com.sfs.engine.cache;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class AnalysisCache {

    public record CacheEntry(
            String objectId,
            String contentSha256,
            String engineVersion,
            String dnaVersion) {

        public CacheEntry {
            Objects.requireNonNull(objectId, "objectId must not be null");
            Objects.requireNonNull(contentSha256, "contentSha256 must not be null");
            Objects.requireNonNull(engineVersion, "engineVersion must not be null");
            Objects.requireNonNull(dnaVersion, "dnaVersion must not be null");
        }
    }

    private final ConcurrentMap<String, CacheEntry> entriesByObjectId = new ConcurrentHashMap<>();

    public void put(CacheEntry entry) {
        entriesByObjectId.put(entry.objectId(), entry);
    }

    public Optional<CacheEntry> get(String objectId) {
        if (objectId == null || objectId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(entriesByObjectId.get(objectId));
    }

    public boolean matchesUnchangedContent(String objectId, String contentSha256,
                                           String engineVersion) {
        return get(objectId)
                .map(entry -> entry.contentSha256().equals(contentSha256)
                        && entry.engineVersion().equals(engineVersion))
                .orElse(false);
    }

    public void evict(String objectId) {
        if (objectId != null) {
            entriesByObjectId.remove(objectId);
        }
    }

    public int size() {
        return entriesByObjectId.size();
    }
}
