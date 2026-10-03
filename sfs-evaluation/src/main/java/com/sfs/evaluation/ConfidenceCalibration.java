package com.sfs.evaluation;

import java.util.ArrayList;
import java.util.List;

public final class ConfidenceCalibration {

    private static final double[][] BINS = {
            {0.0, 0.6}, {0.6, 0.75}, {0.75, 0.9}, {0.9, 1.0001}};

    private ConfidenceCalibration() {
    }

    public static List<CalibrationRecord> calibrate(EvaluationInput input) {
        List<CalibrationRecord> records = new ArrayList<>();
        for (double[] bin : BINS) {
            records.add(bin(input, bin[0], bin[1]));
        }
        return List.copyOf(records);
    }

    private static CalibrationRecord bin(EvaluationInput input, double lower,
                                         double upper) {
        int count = 0;
        double statedSum = 0.0;
        int survived = 0;
        for (var fact : input.dna().facts()) {
            if (fact.confidence() >= lower && fact.confidence() < upper) {
                count++;
                statedSum += fact.confidence();
                if (TextMatching.containsNormalized(input.artifactText(),
                        fact.statement())) {
                    survived++;
                }
            }
        }
        double meanStated = count == 0 ? 0.0 : statedSum / count;
        double observed = count == 0 ? 0.0 : (double) survived / count;
        return new CalibrationRecord(lower, Math.min(upper, 1.0), count,
                meanStated, observed);
    }
}
