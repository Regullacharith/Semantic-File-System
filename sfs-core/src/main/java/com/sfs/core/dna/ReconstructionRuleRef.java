package com.sfs.core.dna;

import java.util.Objects;

public record ReconstructionRuleRef(String ruleId, String ruleType, String version) {

    public ReconstructionRuleRef {
        Objects.requireNonNull(ruleId, "ruleId must not be null");
        Objects.requireNonNull(ruleType, "ruleType must not be null");
        Objects.requireNonNull(version, "version must not be null");
        if (ruleId.isBlank() || ruleType.isBlank() || version.isBlank()) {
            throw new IllegalArgumentException("rule reference fields must not be blank");
        }
    }
}
