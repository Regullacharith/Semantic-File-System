package com.sfs.core.rules;

public enum RulePriority {

    CRITICAL(0),
    HIGH(1),
    NORMAL(2),
    LOW(3);

    private final int weight;

    RulePriority(int weight) {
        this.weight = weight;
    }

    public int weight() {
        return weight;
    }
}
