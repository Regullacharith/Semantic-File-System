package com.sfs.reconstruction.engine;

import com.sfs.contracts.file.FileStatus;
import com.sfs.core.dna.DnaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.sfs.reconstruction.engine.EngineFixtures.filesWith;
import static com.sfs.reconstruction.engine.EngineFixtures.repositoryWith;
import static com.sfs.reconstruction.engine.EngineFixtures.structuredDna;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DNA and rule loader (11.2)")
class DnaRuleLoaderTest {

    private static final String OBJECT_ID = "sfs-obj-3001-aaaabbbb";

    @Test
    @DisplayName("refuses an unknown object with an explicit reason")
    void unknownObjectRefused() {
        DnaRuleLoader loader = new DnaRuleLoader(
                repositoryWith(structuredDna(OBJECT_ID)),
                filesWith(OBJECT_ID, "known.txt", FileStatus.ANALYZED));

        assertThatThrownBy(() -> loader.load("sfs-obj-9999-ffffffff"))
                .isInstanceOf(ReconstructionRefusalException.class)
                .hasMessageContaining("No object exists");
    }

    @Test
    @DisplayName("refuses a known object that has no Semantic DNA")
    void objectWithoutDnaRefused() {
        DnaRuleLoader loader = new DnaRuleLoader(
                repositoryWith(),
                filesWith(OBJECT_ID, "notes.txt", FileStatus.ANALYZED));

        assertThatThrownBy(() -> loader.load(OBJECT_ID))
                .isInstanceOf(ReconstructionRefusalException.class)
                .hasMessageContaining("no Semantic DNA");
    }

    @Test
    @DisplayName("loads the file summary and stored DNA for a reconstructable object")
    void loadsSource() {
        DnaRepository repository = repositoryWith(structuredDna(OBJECT_ID));
        DnaRuleLoader loader = new DnaRuleLoader(repository,
                filesWith(OBJECT_ID, "plan.txt", FileStatus.MEMORIZED));

        ReconstructedSource source = loader.load(OBJECT_ID);

        assertThat(source.file().displayName()).isEqualTo("plan.txt");
        assertThat(source.file().status()).isEqualTo(FileStatus.MEMORIZED);
        assertThat(source.dna().dna().objectId()).isEqualTo(OBJECT_ID);
        assertThat(source.dnaVersion()).contains("sfs-dna/0.2");
    }
}
