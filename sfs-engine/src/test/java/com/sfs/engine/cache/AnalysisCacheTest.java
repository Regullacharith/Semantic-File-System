package com.sfs.engine.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AnalysisCache")
class AnalysisCacheTest {

    private final AnalysisCache cache = new AnalysisCache();

    private AnalysisCache.CacheEntry entry(String hash, String version) {
        return new AnalysisCache.CacheEntry("sfs-obj-0001-a1b2c3d4", hash,
                "sfs-engine/0.1", version);
    }

    @Test
    @DisplayName("an empty cache never matches")
    void emptyCacheNeverMatches() {
        assertThat(cache.matchesUnchangedContent("sfs-obj-0001-a1b2c3d4", "abc", "sfs-engine/0.1"))
                .isFalse();
    }

    @Test
    @DisplayName("a stored entry matches the same content and engine version")
    void matchesSameContentAndVersion() {
        cache.put(entry("sha-1", "sfs-dna/0.1 v1"));
        assertThat(cache.matchesUnchangedContent("sfs-obj-0001-a1b2c3d4", "sha-1", "sfs-engine/0.1"))
                .isTrue();
    }

    @Test
    @DisplayName("changed content or a new engine version misses")
    void missesOnContentOrVersionChange() {
        cache.put(entry("sha-1", "sfs-dna/0.1 v1"));
        assertThat(cache.matchesUnchangedContent("sfs-obj-0001-a1b2c3d4", "sha-2", "sfs-engine/0.1"))
                .isFalse();
        assertThat(cache.matchesUnchangedContent("sfs-obj-0001-a1b2c3d4", "sha-1", "sfs-engine/0.2"))
                .isFalse();
    }

    @Test
    @DisplayName("re-putting an object replaces its entry")
    void replacingAnEntry() {
        cache.put(entry("sha-1", "sfs-dna/0.1 v1"));
        cache.put(entry("sha-2", "sfs-dna/0.1 v2"));
        assertThat(cache.get("sfs-obj-0001-a1b2c3d4").orElseThrow().dnaVersion())
                .isEqualTo("sfs-dna/0.1 v2");
        assertThat(cache.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("eviction removes the entry")
    void eviction() {
        cache.put(entry("sha-1", "sfs-dna/0.1 v1"));
        cache.evict("sfs-obj-0001-a1b2c3d4");
        assertThat(cache.get("sfs-obj-0001-a1b2c3d4")).isEmpty();
    }
}
