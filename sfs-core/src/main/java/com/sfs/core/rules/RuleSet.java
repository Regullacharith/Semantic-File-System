package com.sfs.core.rules;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record RuleSet(
        String objectId,
        int dnaVersion,
        String dnaSha256,
        String rulesVersion,
        List<Rule> rules) {

    public RuleSet {
        Objects.requireNonNull(objectId, "objectId must not be null");
        Objects.requireNonNull(dnaSha256, "dnaSha256 must not be null");
        Objects.requireNonNull(rulesVersion, "rulesVersion must not be null");
        Objects.requireNonNull(rules, "rules must not be null");
        if (objectId.isBlank()) {
            throw new IllegalArgumentException("objectId must not be blank");
        }
        if (dnaVersion < 1) {
            throw new IllegalArgumentException("dnaVersion must be at least 1");
        }
        if (dnaSha256.isBlank() || dnaSha256.length() != 64) {
            throw new IllegalArgumentException("dnaSha256 must be a SHA-256 hex digest");
        }
        if (rulesVersion.isBlank()) {
            throw new IllegalArgumentException("rulesVersion must not be blank");
        }
        Set<String> seen = new HashSet<>();
        for (Rule rule : rules) {
            if (!seen.add(rule.ruleId())) {
                throw new IllegalArgumentException(
                        "duplicate rule id in rule set: " + rule.ruleId());
            }
        }
        rules = List.copyOf(rules);
    }

    public String bindingKey() {
        return objectId + "@" + dnaVersion + "-" + dnaSha256.substring(0, 12);
    }
}
