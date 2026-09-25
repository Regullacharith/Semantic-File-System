package com.sfs.core.rules;

import java.util.Objects;
import java.util.Optional;

public record RuleLookup(
        RuleSet ruleSet,
        boolean reused,
        RuleConflict versionConflict) {

    public RuleLookup {
        Objects.requireNonNull(ruleSet, "ruleSet must not be null");
    }

    public static RuleLookup reused(RuleSet ruleSet) {
        return new RuleLookup(ruleSet, true, null);
    }

    public static RuleLookup derived(RuleSet ruleSet, RuleConflict versionConflict) {
        return new RuleLookup(ruleSet, false, versionConflict);
    }
}
