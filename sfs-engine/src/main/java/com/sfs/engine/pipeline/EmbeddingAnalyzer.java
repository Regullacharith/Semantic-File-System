package com.sfs.engine.pipeline;

import com.sfs.engine.core.SemanticContext;

public final class EmbeddingAnalyzer implements Analyzer {

    public static final int DIMENSIONS = 64;

    @Override
    public String name() {
        return "embeddings";
    }

    @Override
    public void perform(SemanticContext context, SemanticIntermediateRepresentation ir) {
        double[] vector = new double[DIMENSIONS];
        for (String token : ir.tokens()) {
            if (StopWords.ALL.contains(token)) {
                continue;
            }
            int index = Math.floorMod(token.hashCode(), DIMENSIONS);
            vector[index] += 1.0;
        }
        double norm = 0.0;
        for (double value : vector) {
            norm += value * value;
        }
        norm = Math.sqrt(norm);
        if (norm > 0.0) {
            for (int i = 0; i < DIMENSIONS; i++) {
                vector[i] = vector[i] / norm;
            }
        }
        ir.setEmbedding(vector);
    }
}
