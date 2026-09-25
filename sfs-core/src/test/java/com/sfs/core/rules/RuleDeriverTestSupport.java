package com.sfs.core.rules;

import com.sfs.core.dna.DnaFixtures;
import com.sfs.core.dna.SemanticDna;
import com.sfs.core.dna.StructureNode;

import java.util.List;

final class RuleDeriverTestSupport {

    private RuleDeriverTestSupport() {
    }

    static SemanticDna twoHeadingDna() {
        return withStructure(DnaFixtures.sample(), List.of(
                new StructureNode("Summary", 1, 0),
                new StructureNode("Measurements", 1, 1)));
    }

    static SemanticDna withStructure(SemanticDna dna, List<StructureNode> structure) {
        return new SemanticDna(
                dna.identity(), dna.summary(), dna.concepts(), dna.topics(),
                dna.entities(), dna.facts(), dna.relationships(), structure,
                dna.embedding(), dna.behaviour(), dna.reconstructionRules(),
                dna.fidelity(), dna.security());
    }
}
