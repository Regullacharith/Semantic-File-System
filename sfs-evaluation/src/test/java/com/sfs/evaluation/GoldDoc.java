package com.sfs.evaluation;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public record GoldDoc(String objectId, String fileName,
                      List<String> criticalFacts, List<String> headings,
                      List<String> entities) {

    public static final List<GoldDoc> GOLD = List.of(
            new GoldDoc("sfs-obj-5001-quarterly", "quarterly-report.txt",
                    List.of(
                            "Query latency decreased by 40 percent after indexing "
                                    + "changes were deployed in August.",
                            "The platform team must verify capacity before the "
                                    + "Q4 2026 migration."),
                    List.of("Summary", "Measurements", "Recommendations"),
                    List.of("PostgreSQL", "Q3 2026")),
            new GoldDoc("sfs-obj-5002-meeting", "team-meeting-minutes.txt",
                    List.of(
                            "The plan is due on 2026-09-15.",
                            "Alice will draft the migration plan for the storage "
                                    + "platform."),
                    List.of("Agenda", "Decisions", "Action Items"),
                    List.of("Alice", "Bob")),
            new GoldDoc("sfs-obj-5003-research", "research-notes.txt",
                    List.of(
                            "Recall improved by 12 percent after the embedding "
                                    + "change.",
                            "Embeddings support the retrieval quality targets for "
                                    + "2026."),
                    List.of("Overview", "Method", "Findings"),
                    List.of("Embeddings")));

    public byte[] originalBytes() {
        try {
            return Files.readAllBytes(Path.of("..", "sfs-engine", "src", "test",
                    "resources", "benchmarks", fileName));
        } catch (Exception e) {
            throw new IllegalStateException("benchmark fixture missing: "
                    + fileName, e);
        }
    }

    public String originalText() {
        return new String(originalBytes(), StandardCharsets.UTF_8);
    }
}
