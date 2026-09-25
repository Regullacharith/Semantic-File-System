package com.sfs.core.dna;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DnaSchemaValidator")
class DnaSchemaValidatorTest {

    private final DnaSchemaValidator validator = new DnaSchemaValidator();

    @Test
    @DisplayName("a well-formed sample is valid")
    void sampleIsValid() {
        assertThat(validator.validate(DnaFixtures.sample())).isEmpty();
        assertThat(validator.isValid(DnaFixtures.sample())).isTrue();
    }

    @Test
    @DisplayName("a wrong schema version is refused")
    void wrongSchemaVersion() {
        SemanticDna dna = new SemanticDna(
                new DnaIdentity("sfs-obj-0001-a1b2c3d4", "sfs-dna/0.9", 1,
                        "sfs-engine/0.1", DnaFixtures.T0),
                DnaFixtures.sample().summary(), DnaFixtures.sample().concepts(),
                DnaFixtures.sample().topics(), DnaFixtures.sample().entities(),
                DnaFixtures.sample().facts(), DnaFixtures.sample().relationships(),
                DnaFixtures.sample().structure(), DnaFixtures.sample().embedding(),
                DnaFixtures.sample().behaviour(), DnaFixtures.sample().reconstructionRules(),
                DnaFixtures.sample().fidelity(), DnaFixtures.sample().security());

        assertThat(validator.validate(dna)).anyMatch(issue -> issue.contains("schema version"));
    }

    @Test
    @DisplayName("empty structure is a validation issue")
    void emptyStructureRejected() {
        SemanticDna dna = DnaFixtures.builder().structure(List.of()).build();
        assertThat(validator.validate(dna)).anyMatch(issue -> issue.contains("structure"));
    }

    @Test
    @DisplayName("inconsistent protected-reference flags are caught")
    void inconsistentSecurityFlags() {
        SecurityProfile broken = new SecurityProfile("protected-refs-v1", List.of());
        SemanticDna dna = new SemanticDna(
                DnaFixtures.identity(), DnaFixtures.sample().summary(),
                DnaFixtures.sample().concepts(), DnaFixtures.sample().topics(),
                DnaFixtures.sample().entities(), DnaFixtures.sample().facts(),
                DnaFixtures.sample().relationships(), DnaFixtures.sample().structure(),
                DnaFixtures.sample().embedding(), DnaFixtures.sample().behaviour(),
                DnaFixtures.sample().reconstructionRules(), DnaFixtures.sample().fidelity(),
                broken);
        assertThat(validator.validate(dna)).isEmpty();
    }
}
