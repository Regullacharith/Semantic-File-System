package com.sfs.evaluation;

import java.util.Objects;

public record CalibrationRecord(double lowerBound, double upperBound,
                                int factCount, double meanStatedConfidence,
                                double observedSurvivalRate) {

    public CalibrationRecord {
        Objects.requireNonNull(factCount, "factCount must not be null");
        if (lowerBound < 0.0 || upperBound > 1.0001 || lowerBound >= upperBound) {
            throw new IllegalArgumentException(
                    "calibration bounds must be ordered within [0.0, 1.0]");
        }
        if (factCount < 0) {
            throw new IllegalArgumentException("factCount must not be negative");
        }
        if (meanStatedConfidence < 0.0 || meanStatedConfidence > 1.0
                || observedSurvivalRate < 0.0 || observedSurvivalRate > 1.0) {
            throw new IllegalArgumentException(
                    "rates and confidences must be between 0.0 and 1.0");
        }
    }

    public double delta() {
        return meanStatedConfidence - observedSurvivalRate;
    }

    public boolean populated() {
        return factCount > 0;
    }
}
