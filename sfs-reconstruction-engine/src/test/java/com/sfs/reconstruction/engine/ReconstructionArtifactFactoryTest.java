package com.sfs.reconstruction.engine;

import com.sfs.contracts.reconstruction.ReconstructionArtifact;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TXT artifact generation (11.7)")
class ReconstructionArtifactFactoryTest {

    private final ReconstructionArtifactFactory factory = new ReconstructionArtifactFactory();

    @Test
    @DisplayName("names the artifact so it can never be mistaken for the original")
    void namesArtifact() {
        ReconstructionArtifact artifact = factory.create("job-0001", "plan.txt",
                "labeled content");

        assertThat(artifact.fileName()).isEqualTo("plan.reconstructed.job-0001.txt");
        assertThat(artifact.jobId()).isEqualTo("job-0001");
    }

    @Test
    @DisplayName("keeps the base name for sources without a txt suffix")
    void keepsBaseName() {
        ReconstructionArtifact artifact = factory.create("job-0002", "notes",
                "labeled content");

        assertThat(artifact.fileName()).isEqualTo("notes.reconstructed.job-0002.txt");
    }

    @Test
    @DisplayName("emits UTF-8 plain text with accurate byte counts")
    void emitsPlainText() {
        ReconstructionArtifact artifact = factory.create("job-0003", "plan.txt",
                "labeled content");

        assertThat(artifact.contentType())
                .isEqualTo(ReconstructionArtifact.TEXT_PLAIN);
        assertThat(artifact.content()).isEqualTo("labeled content");
        assertThat(artifact.content().getBytes(StandardCharsets.UTF_8).length)
                .isEqualTo("labeled content".getBytes(StandardCharsets.UTF_8).length);
    }

    @Test
    @DisplayName("refuses an empty labeled document")
    void refusesEmptyContent() {
        assertThatThrownBy(() -> factory.create("job-0004", "plan.txt", "  "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
