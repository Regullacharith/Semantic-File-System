package com.sfs.engine.level;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

public final class AnalysisLevelPolicy {

    private final Set<AnalysisLevel> enabledLevels;
    private final AnalysisLevel defaultLevel;

    public AnalysisLevelPolicy(Collection<AnalysisLevel> enabledLevels, AnalysisLevel defaultLevel) {
        Objects.requireNonNull(enabledLevels, "enabledLevels must not be null");
        if (enabledLevels.isEmpty()) {
            throw new IllegalArgumentException("at least one analysis level must be enabled");
        }
        Objects.requireNonNull(defaultLevel, "defaultLevel must not be null");
        if (!enabledLevels.contains(defaultLevel)) {
            throw new IllegalArgumentException("the default level must be enabled");
        }
        this.enabledLevels = Set.copyOf(enabledLevels);
        this.defaultLevel = defaultLevel;
    }

    public static AnalysisLevelPolicy v1() {
        return new AnalysisLevelPolicy(Set.of(AnalysisLevel.STANDARD), AnalysisLevel.STANDARD);
    }

    public boolean isEnabled(AnalysisLevel level) {
        Objects.requireNonNull(level, "level must not be null");
        return enabledLevels.contains(level);
    }

    public String refusalReason(AnalysisLevel level) {
        return "Analysis level " + level + " exists in the strategy but is not enabled in "
                + "this deployment. Enabled levels: " + enabledLevels + ".";
    }

    public AnalysisLevel defaultLevel() {
        return defaultLevel;
    }

    public Set<AnalysisLevel> enabledLevels() {
        return enabledLevels;
    }
}
