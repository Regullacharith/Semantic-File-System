package com.sfs.evaluation;

import com.sfs.contracts.evaluation.FidelityDimension;

import java.util.Map;

public final class OverallFidelity {

    private static final Map<FidelityDimension, Double> WEIGHTS = Map.of(
            FidelityDimension.SEMANTIC, 0.15,
            FidelityDimension.STRUCTURAL, 0.15,
            FidelityDimension.FACTUAL, 0.25,
            FidelityDimension.ENTITY, 0.15,
            FidelityDimension.RELATIONSHIP, 0.10,
            FidelityDimension.COMPLETENESS, 0.20);

    private OverallFidelity() {
    }

    public static double compute(Map<FidelityDimension, Double> dimensionScores,
                                 CriticalFactScore critical) {
        double weighted = dimensionScores.entrySet().stream()
                .mapToDouble(entry -> WEIGHTS.get(entry.getKey()) * entry.getValue())
                .sum();
        double criticalRate = critical.rate();
        if (critical.total() == 0) {
            return round(weighted);
        }
        return round(Math.min(weighted, criticalRate));
    }

    public static Map<FidelityDimension, Double> weights() {
        return Map.copyOf(WEIGHTS);
    }

    private static double round(double value) {
        return Math.round(value * 10_000.0) / 10_000.0;
    }
}
