package com.sfs.evaluation;

import com.sfs.core.dna.DnaIdentity;
import com.sfs.core.dna.Entity;
import com.sfs.core.dna.Fact;
import com.sfs.core.dna.FidelityProfile;
import com.sfs.core.dna.Relationship;
import com.sfs.core.dna.SemanticDna;
import com.sfs.core.dna.SemanticDnaBuilder;
import com.sfs.core.dna.StructureNode;

import java.time.Instant;
import java.util.List;

final class EngineSupport {

    static final Instant T0 = Instant.parse("2026-06-01T08:00:00Z");

    private EngineSupport() {
    }

    static SemanticDna dna(String objectId) {
        return SemanticDnaBuilder.forIdentity(new DnaIdentity(
                        objectId, "sfs-dna/0.2", 1, "sfs-engine/0.1", T0))
                .summary("The platform migration moves the core database in the "
                        + "second quarter.")
                .concepts(List.of("platform migration", "capacity planning"))
                .entities(List.of(new Entity("Core Database", "system", 2)))
                .facts(List.of(
                        new Fact("The migration window opens on April 7.", true, 0.95),
                        new Fact("Rollback takes at most two hours.", false, 0.8)))
                .relationships(List.of(new Relationship(
                        "Core Database", "migrates-to", "New Infrastructure")))
                .structure(List.of(
                        new StructureNode("Overview", 1, 0),
                        new StructureNode("Timeline", 1, 1)))
                .fidelity(new FidelityProfile(0.9, 0.9, "sfs-engine/0.1"))
                .build();
    }
}
