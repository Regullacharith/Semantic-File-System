package com.sfs.evaluation;

import com.sfs.contracts.evaluation.FidelityDimension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("Overall fidelity calculation with its critical gate (12.7)")
class OverallFidelityTest {

    @Test
    @DisplayName("weights cover every dimension exactly once and sum to one")
    void weights() {
        Map<FidelityDimension, Double> weights = OverallFidelity.weights();

        assertThat(weights).containsKeys(FidelityDimension.values());
        assertThat(weights.values().stream().mapToDouble(Double::doubleValue).sum())
                .isCloseTo(1.0, within(1e-9));
    }

    @Test
    @DisplayName("overall is the weighted blend when every critical fact survived")
    void weightedBlend() {
        Map<FidelityDimension, Double> scores = new EnumMap<>(FidelityDimension.class);
        scores.put(FidelityDimension.SEMANTIC, 1.0);
        scores.put(FidelityDimension.STRUCTURAL, 1.0);
        scores.put(FidelityDimension.FACTUAL, 1.0);
        scores.put(FidelityDimension.ENTITY, 1.0);
        scores.put(FidelityDimension.RELATIONSHIP, 1.0);
        scores.put(FidelityDimension.COMPLETENESS, 1.0);

        assertThat(OverallFidelity.compute(scores,
                new CriticalFactScore(2, 2, List.of()))).isEqualTo(1.0);
    }

    @Test
    @DisplayName("a high semantic score cannot hide a lost critical fact")
    void criticalGate() {
        Map<FidelityDimension, Double> scores = new EnumMap<>(FidelityDimension.class);
        scores.put(FidelityDimension.SEMANTIC, 0.95);
        scores.put(FidelityDimension.STRUCTURAL, 0.95);
        scores.put(FidelityDimension.FACTUAL, 0.5);
        scores.put(FidelityDimension.ENTITY, 0.9);
        scores.put(FidelityDimension.RELATIONSHIP, 0.9);
        scores.put(FidelityDimension.COMPLETENESS, 0.9);

        double overall = OverallFidelity.compute(scores,
                new CriticalFactScore(2, 1, List.of("lost")));

        assertThat(overall).isLessThanOrEqualTo(0.5);
    }

    @Test
    @DisplayName("documents without critical facts are judged by the blend alone")
    void noCriticalsNoGate() {
        Map<FidelityDimension, Double> scores = new EnumMap<>(FidelityDimension.class);
        scores.put(FidelityDimension.SEMANTIC, 0.8);
        scores.put(FidelityDimension.STRUCTURAL, 0.8);
        scores.put(FidelityDimension.FACTUAL, 0.8);
        scores.put(FidelityDimension.ENTITY, 0.8);
        scores.put(FidelityDimension.RELATIONSHIP, 0.8);
        scores.put(FidelityDimension.COMPLETENESS, 0.8);

        assertThat(OverallFidelity.compute(scores,
                new CriticalFactScore(0, 0, List.of()))).isEqualTo(0.8);
    }
}
