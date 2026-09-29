package com.sfs.reconstruction.decode;

import com.sfs.core.dna.SemanticDna;
import com.sfs.core.rules.PlanChecker;
import com.sfs.reconstruction.Fixtures;
import com.sfs.reconstruction.ModelInput;
import com.sfs.reconstruction.encode.SemanticDnaEncoder;
import com.sfs.reconstruction.encode.UnifiedRepresentation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Templated decoder: the default deterministic decoder (10.7)")
class TemplatedDecoderTest {

    private final TemplatedDecoder decoder = new TemplatedDecoder();
    private final SemanticDnaEncoder encoder = new SemanticDnaEncoder();

    private UnifiedRepresentation representation(SemanticDna dna) {
        return encoder.encode(
                ModelInput.of(dna, Fixtures.derivedPlan(dna)));
    }

    @Test
    @DisplayName("the decoder id is a stable versioned string")
    void decoderIdStable() {
        assertThat(decoder.decoderId())
                .isEqualTo("sfs-reconstruction/templated-decoder/0.1");
    }

    @Test
    @DisplayName("the draft carries the summary verbatim and headings in order")
    void summaryAndHeadings() {
        String draft = decoder.decode(representation(Fixtures.handDna()));

        String normalized = PlanChecker.normalize(draft);
        assertThat(normalized).contains(PlanChecker.normalize(
                "The platform migration plan moves the core database to new "
                        + "infrastructure during the second quarter."));
        assertThat(draft).contains("# Overview").contains("# Timeline")
                .contains("## Risks");
        assertThat(draft.indexOf("Overview")).isLessThan(draft.indexOf("Timeline"));
        assertThat(draft.indexOf("Timeline")).isLessThan(draft.indexOf("Risks"));
    }

    @Test
    @DisplayName("critical facts are visibly marked in the draft")
    void criticalFactsMarked() {
        String draft = decoder.decode(representation(Fixtures.handDna()));

        assertThat(draft).contains("[critical] The migration window opens on April 7.");
        assertThat(draft).contains("Rollback takes at most two hours.");
    }

    @Test
    @DisplayName("entity emissions meet the required mention floor without padding")
    void entitiesMeetRequiredMentions() {
        String draft = decoder.decode(representation(Fixtures.handDna()));

        assertThat(draft).contains("Core Database (system)");
        assertThat(draft).contains("Migration Plan (document)");
    }

    @Test
    @DisplayName("relationships, concepts and topics all appear as material lines")
    void relationshipsConceptsTopics() {
        String draft = decoder.decode(representation(Fixtures.handDna()));

        assertThat(draft).contains("Core Database migrates-to New Infrastructure.");
        assertThat(draft).contains("platform migration, database infrastructure");
        assertThat(draft).contains("capacity planning");
    }

    @Test
    @DisplayName("identical representation decodes to an identical draft")
    void deterministic() {
        UnifiedRepresentation representation = representation(Fixtures.handDna());

        assertThat(decoder.decode(representation))
                .isEqualTo(decoder.decode(representation));
    }
}
