package com.sfs.memory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class VectorIndex {

    public record Candidate(String objectId, double similarity) {

        public Candidate {
            Objects.requireNonNull(objectId, "objectId must not be null");
            if (similarity < -1.001 || similarity > 1.001) {
                throw new IllegalArgumentException("similarity out of range");
            }
        }
    }

    private final Map<String, List<Double>> vectorsByObjectId = new ConcurrentHashMap<>();

    public void upsert(String objectId, List<Double> vector) {
        Objects.requireNonNull(objectId, "objectId must not be null");
        Objects.requireNonNull(vector, "vector must not be null");
        if (vector.isEmpty()) {
            vectorsByObjectId.remove(objectId);
            return;
        }
        vectorsByObjectId.put(objectId, List.copyOf(vector));
    }

    public void remove(String objectId) {
        if (objectId != null) {
            vectorsByObjectId.remove(objectId);
        }
    }

    public Optional<List<Double>> vectorOf(String objectId) {
        return Optional.ofNullable(vectorsByObjectId.get(objectId));
    }

    public List<Candidate> searchTopK(List<Double> query, int k) {
        Objects.requireNonNull(query, "query must not be null");
        if (k < 1) {
            throw new IllegalArgumentException("k must be at least 1");
        }
        return vectorsByObjectId.entrySet().stream()
                .map(entry -> new Candidate(
                        entry.getKey(), cosine(query, entry.getValue())))
                .sorted((a, b) -> Double.compare(b.similarity(), a.similarity()))
                .limit(k)
                .toList();
    }

    public void rebuild(Map<String, List<Double>> vectors) {
        Objects.requireNonNull(vectors, "vectors must not be null");
        vectorsByObjectId.clear();
        vectors.forEach(this::upsert);
    }

    public int size() {
        return vectorsByObjectId.size();
    }

    static double cosine(List<Double> a, List<Double> b) {
        if (a.size() != b.size()) {
            throw new IllegalArgumentException(
                    "vector dimensions differ: " + a.size() + " vs " + b.size());
        }
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.size(); i++) {
            dot += a.get(i) * b.get(i);
            normA += a.get(i) * a.get(i);
            normB += b.get(i) * b.get(i);
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
