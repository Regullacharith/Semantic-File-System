package com.sfs.core.dna;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

public final class DnaFixtures {

    public static final Instant T0 = Instant.parse("2026-03-15T10:00:00Z");

    private DnaFixtures() {
    }

    public static DnaIdentity identity() {
        return new DnaIdentity("sfs-obj-0001-a1b2c3d4",
                DnaSchemaValidator.CURRENT_SCHEMA_VERSION, 1,
                "sfs-engine/0.1", T0);
    }

    public static SemanticDnaBuilder builder() {
        return SemanticDnaBuilder.forIdentity(identity())
                .summary("Overview of the platform for Q3 2026.")
                .concepts(List.of("platform review"))
                .topics(List.of("platform"))
                .entities(List.of(new Entity("PostgreSQL", "Named entity", 2)))
                .facts(List.of(new Fact(
                        "Query latency decreased by 40 percent.", true, 0.95)))
                .relationships(List.of(new Relationship(
                        "PostgreSQL", "hosts", "the workload")))
                .structure(List.of(new StructureNode("Summary", 1, 0)))
                .embedding(new EmbeddingRef("feature-hashing/0.1", 4,
                        List.of(0.5, 0.5, 0.0, 0.7)))
                .averageSentenceWords(9.0)
                .fidelity(new FidelityProfile(0.9, 0.8, "sfs-engine/0.1"));
    }

    public static SemanticDna sample() {
        return builder().build();
    }

    public static String legacyJson() {
        return "{\"schemaVersion\":\"sfs-dna/0.1\",\"objectId\":\"sfs-obj-0002-e5f6a7b8\","
                + "\"dnaVersion\":2,\"summary\":\"Quarterly database performance report.\","
                + "\"concepts\":[\"indexing\"],\"topics\":[\"database\"],"
                + "\"entities\":[{\"name\":\"PostgreSQL\",\"type\":\"Named entity\","
                + "\"mentions\":3}],"
                + "\"facts\":[{\"statement\":\"Latency decreased by 40 percent.\","
                + "\"critical\":true,\"confidence\":0.96}],"
                + "\"relationships\":[],"
                + "\"structure\":[{\"heading\":\"Summary\",\"level\":1,\"order\":0}],"
                + "\"embeddingDimensions\":64,"
                + "\"extractionConfidence\":0.89,\"structuralCompleteness\":0.91,"
                + "\"analyzerVersion\":\"sfs-engine/0.1\"}";
    }

    static String escapeProbe() {
        return "quote \" backslash \\ newline \n tab \t unicode é中\u00e9 controls "
                + "\u0001\u0002 end";
    }
}
