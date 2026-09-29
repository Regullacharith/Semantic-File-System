package com.sfs.reconstruction;

import com.sfs.core.dna.SemanticDna;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Model contract: ModelInput, ModelOutput and constraint results")
class ModelContractTest {

    @Test
    @DisplayName("ModelInput derives its identity from the DNA and plan")
    void inputIdentityFromDnaAndPlan() {
        SemanticDna dna = Fixtures.handDna();
        ModelInput input = ModelInput.of(dna, Fixtures.derivedPlan(dna));

        assertThat(input.objectId()).isEqualTo("sfs-obj-2001-reconmodel");
        assertThat(input.dnaSha256()).isNotBlank();
    }

    @Test
    @DisplayName("ModelInput refuses a plan that belongs to a different object")
    void inputRefusesForeignPlan() {
        SemanticDna dna = Fixtures.handDna();
        SemanticDna other = Fixtures.protectedDna();

        assertThatThrownBy(() -> new ModelInput(dna.objectId(), dna,
                Fixtures.derivedPlan(other)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("identity must agree");
    }

    @Test
    @DisplayName("ModelOutput refuses a blank model id")
    void outputRefusesBlankModelId() {
        assertThatThrownBy(() -> new ModelOutput("  ", "draft", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("modelId");
    }

    @Test
    @DisplayName("ModelOutput notes are defensively copied")
    void outputNotesDefensivelyCopied() {
        List<String> mutable = new java.util.ArrayList<>(List.of("note"));
        ModelOutput output = new ModelOutput("m/1", "draft", mutable);

        mutable.add("intrusion");

        assertThat(output.notes()).containsExactly("note");
    }

    @Test
    @DisplayName("a constraint result with no violations is satisfied")
    void satisfiedWhenNoViolations() {
        var satisfied = new ConstraintInterface.Result(List.of(), List.of("warning"));
        var violated = new ConstraintInterface.Result(List.of("missing"), List.of());

        assertThat(satisfied.satisfied()).isTrue();
        assertThat(satisfied.warnings()).containsExactly("warning");
        assertThat(violated.satisfied()).isFalse();
    }
}
