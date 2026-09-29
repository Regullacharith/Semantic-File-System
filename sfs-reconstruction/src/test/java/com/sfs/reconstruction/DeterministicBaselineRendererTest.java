package com.sfs.reconstruction;

import com.sfs.core.dna.SemanticDna;
import com.sfs.reconstruction.decode.TemplatedDecoder;
import com.sfs.reconstruction.model.DeterministicBaselineRenderer;
import com.sfs.reconstruction.encode.UnifiedRepresentation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Deterministic baseline renderer (10.2)")
class DeterministicBaselineRendererTest {

    private final DeterministicBaselineRenderer baseline =
            new DeterministicBaselineRenderer();

    @Test
    @DisplayName("the baseline draft passes the constraint interface under derived rules")
    void baselineSatisfiesDerivedRules() {
        SemanticDna dna = Fixtures.handDna();
        ModelInput input = ModelInput.of(dna, Fixtures.derivedPlan(dna));

        ModelOutput output = baseline.reconstruct(input);
        ConstraintInterface.Result result =
                new PlanConstraintInterface().judge(input, output.draftText());

        assertThat(output.modelId())
                .isEqualTo(DeterministicBaselineRenderer.MODEL_ID);
        assertThat(output.notes())
                .containsExactly(TemplatedDecoder.DECODER_ID);
        assertThat(result.satisfied()).isTrue();
    }

    @Test
    @DisplayName("the baseline draft passes an explicit rule set with required elements")
    void baselineSatisfiesExplicitRules() {
        SemanticDna dna = Fixtures.handDna();
        ModelInput input = ModelInput.of(dna, Fixtures.explicitPlan(dna));

        ModelOutput output = baseline.reconstruct(input);
        ConstraintInterface.Result result =
                new PlanConstraintInterface().judge(input, output.draftText());

        assertThat(result.satisfied()).isTrue();
    }

    @Test
    @DisplayName("identical input produces an identical draft")
    void deterministic() {
        SemanticDna dna = Fixtures.handDna();
        ModelInput input = ModelInput.of(dna, Fixtures.derivedPlan(dna));

        assertThat(baseline.reconstruct(input).draftText())
                .isEqualTo(baseline.reconstruct(input).draftText());
    }

    @Test
    @DisplayName("the decoder slot inside the model is swappable")
    void decoderSlotSwappable() {
        DeterministicBaselineRenderer custom = new DeterministicBaselineRenderer(
                new com.sfs.reconstruction.Decoder() {
                    @Override
                    public String decoderId() {
                        return "test/custom-decoder/0.1";
                    }

                    @Override
                    public String decode(UnifiedRepresentation representation) {
                        return "CUSTOM DRAFT";
                    }
                });
        SemanticDna dna = Fixtures.handDna();
        ModelInput input = ModelInput.of(dna, Fixtures.derivedPlan(dna));

        ModelOutput output = custom.reconstruct(input);

        assertThat(output.draftText()).isEqualTo("CUSTOM DRAFT");
        assertThat(output.notes()).containsExactly("test/custom-decoder/0.1");
    }

    @Test
    @DisplayName("a null input is refused")
    void nullInputRefused() {
        assertThatThrownBy(() -> baseline.reconstruct(null))
                .isInstanceOf(NullPointerException.class);
    }
}
