package com.sfs.reconstruction.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Post-processing: provenance and labeling (11.6)")
class ReconstructionPostProcessorTest {

    private final ReconstructionPostProcessor processor = new ReconstructionPostProcessor();

    @Test
    @DisplayName("prepends the provenance header with all recorded versions")
    void prependsProvenanceHeader() {
        String labeled = processor.label("sfs-obj-3001-aaaabbbb", "sfs-dna/0.2 v1",
                "sfs-rules/0.2", "sfs-reconstruction/deterministic-baseline/0.1",
                "Draft body.\n");

        assertThat(labeled).startsWith("=====");
        assertThat(labeled).contains("SFS SEMANTIC RECONSTRUCTION - NOT THE ORIGINAL FILE");
        assertThat(labeled).contains("sfs-obj-3001-aaaabbbb");
        assertThat(labeled).contains("sfs-dna/0.2 v1");
        assertThat(labeled).contains("sfs-rules/0.2");
        assertThat(labeled).contains("sfs-reconstruction/deterministic-baseline/0.1");
    }

    @Test
    @DisplayName("keeps the draft intact between header and footer")
    void keepsDraftIntact() {
        String draft = "The migration window opens on April 7.\n# Overview\n";
        String labeled = processor.label("sfs-obj-3001-aaaabbbb", "sfs-dna/0.2 v1",
                "sfs-rules/0.2", "model/0.1", draft);

        assertThat(labeled).contains(draft.strip());
        assertThat(labeled.indexOf("The migration window"))
                .isLessThan(labeled.indexOf("End of reconstruction"));
    }

    @Test
    @DisplayName("labels the output as reconstructed and estimated")
    void labelsReconstructedAndEstimated() {
        String labeled = processor.label("sfs-obj-3001-aaaabbbb", "sfs-dna/0.2 v1",
                "sfs-rules/0.2", "model/0.1", "Draft body.");

        assertThat(labeled).contains("reconstructed");
        assertThat(labeled).contains("estimated");
    }

    @Test
    @DisplayName("identical input produces identical output")
    void deterministic() {
        String first = processor.label("sfs-obj-3001-aaaabbbb", "sfs-dna/0.2 v1",
                "sfs-rules/0.2", "model/0.1", "Draft body.");
        String second = processor.label("sfs-obj-3001-aaaabbbb", "sfs-dna/0.2 v1",
                "sfs-rules/0.2", "model/0.1", "Draft body.");

        assertThat(first).isEqualTo(second);
    }

    @Test
    @DisplayName("refuses to label an empty draft")
    void emptyDraftRefused() {
        assertThatThrownBy(() -> processor.label("sfs-obj-3001-aaaabbbb",
                "sfs-dna/0.2 v1", "sfs-rules/0.2", "model/0.1", "   "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("empty draft");
    }
}
