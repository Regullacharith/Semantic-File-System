package com.sfs.core.rules;

import com.sfs.core.dna.SemanticDna;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class RuleRepository {

    private final Map<String, StoredRuleSet> setsByObjectId = new ConcurrentHashMap<>();
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong derivations = new AtomicLong();
    private final AtomicLong versionConflicts = new AtomicLong();

    public RuleLookup findOrDerive(SemanticDna dna, RuleDeriver deriver, Instant at) {
        Objects.requireNonNull(dna, "dna must not be null");
        Objects.requireNonNull(deriver, "deriver must not be null");
        Objects.requireNonNull(at, "at must not be null");

        StoredRuleSet stored = setsByObjectId.get(dna.objectId());
        String expectedHash = com.sfs.core.dna.DnaCanonical.integrityHash(dna);
        if (stored != null
                && stored.ruleSet().dnaVersion() == dna.dnaVersion()
                && stored.ruleSet().dnaSha256().equals(expectedHash)) {
            hits.incrementAndGet();
            return RuleLookup.reused(stored.ruleSet());
        }

        RuleConflict conflict = null;
        if (stored != null && stored.ruleSet().dnaVersion() == dna.dnaVersion()) {
            versionConflicts.incrementAndGet();
            conflict = RuleConflict.warning(
                    "the stored rule set for version " + dna.dnaVersion()
                            + " was derived from different DNA content; re-derived");
        }

        RuleSet derived = deriver.derive(dna);
        derivations.incrementAndGet();
        String canonicalJson = RuleSetCanonical.serialize(derived);
        setsByObjectId.put(dna.objectId(), new StoredRuleSet(
                derived, canonicalJson, RuleSetCanonical.integrityHash(derived), at));
        return RuleLookup.derived(derived, conflict);
    }

    public StoredRuleSet save(RuleSet set, Instant at) {
        Objects.requireNonNull(set, "set must not be null");
        Objects.requireNonNull(at, "at must not be null");
        String canonicalJson = RuleSetCanonical.serialize(set);
        StoredRuleSet stored = new StoredRuleSet(
                set, canonicalJson, RuleSetCanonical.integrityHash(set), at);
        setsByObjectId.put(set.objectId(), stored);
        return stored;
    }

    public Optional<StoredRuleSet> find(String objectId) {
        if (objectId == null || objectId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(setsByObjectId.get(objectId));
    }

    public int size() {
        return setsByObjectId.size();
    }

    public long hits() {
        return hits.get();
    }

    public long derivations() {
        return derivations.get();
    }

    public long versionConflicts() {
        return versionConflicts.get();
    }
}
