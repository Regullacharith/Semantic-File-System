package com.sfs.core.rules;

import java.time.Instant;
import java.util.Objects;

public record StoredRuleSet(
        RuleSet ruleSet,
        String canonicalJson,
        String canonicalSha256,
        Instant boundAt) {

    public StoredRuleSet {
        Objects.requireNonNull(ruleSet, "ruleSet must not be null");
        Objects.requireNonNull(canonicalJson, "canonicalJson must not be null");
        Objects.requireNonNull(canonicalSha256, "canonicalSha256 must not be null");
        Objects.requireNonNull(boundAt, "boundAt must not be null");
    }
}
