package com.sfs.search;

public record RankingProfile(
        double vectorSimilarity,
        double conceptMatch,
        double topicMatch,
        double entityMatch,
        double factMatch,
        double summaryMatch,
        double relationshipMatch) {

    public static final RankingProfile DEFAULT = new RankingProfile(
            0.55, 0.15, 0.10, 0.20, 0.25, 0.10, 0.10);

    public RankingProfile {
        double[] weights = {vectorSimilarity, conceptMatch, topicMatch,
                entityMatch, factMatch, summaryMatch, relationshipMatch};
        for (double weight : weights) {
            if (weight < 0.0) {
                throw new IllegalArgumentException("ranking weights must not be negative");
            }
        }
    }
}
