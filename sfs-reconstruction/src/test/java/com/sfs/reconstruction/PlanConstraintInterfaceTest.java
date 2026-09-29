package com.sfs.reconstruction;

import com.sfs.core.dna.SemanticDna;
import com.sfs.core.rules.ReconstructionPlan;
import com.sfs.reconstruction.model.DeterministicBaselineRenderer;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Plan constraint interface: no unsupported critical fact passes silently")
class PlanConstraintInterfaceTest {

    private final PlanConstraintInterface constraints = new PlanConstraintInterface();
    private final DeterministicBaselineRenderer baseline =
            new DeterministicBaselineRenderer();

    @Test
    @DisplayName("a baseline draft of a clean object satisfies every constraint")
    void baselineDraftSatisfies() {
        SemanticDna dna = Fixtures.handDna();
        ModelInput input = ModelInput.of(dna, Fixtures.explicitPlan(dna));
        String draft = baseline.reconstruct(input).draftText();

        ConstraintInterface.Result result = constraints.judge(input, draft);

        assertThat(result.satisfied()).isTrue();
        assertThat(result.violations()).isEmpty();
    }

    @Test
    @DisplayName("removing a required critical fact is a violation, not silence")
    void missingCriticalFactViolates() {
        SemanticDna dna = Fixtures.handDna();
        ModelInput input = ModelInput.of(dna, Fixtures.explicitPlan(dna));
        String draft = baseline.reconstruct(input).draftText()
                .replace("[critical] The migration window opens on April 7.\n", "");

        ConstraintInterface.Result result = constraints.judge(input, draft);

        assertThat(result.satisfied()).isFalse();
        assertThat(result.violations()).anySatisfy(violation ->
                assertThat(violation).contains("required fact missing"));
        assertThat(result.violations()).anySatisfy(violation ->
                assertThat(violation).contains("critical fact missing"));
    }

    @Test
    @DisplayName("fabricated content unsupported by the representation is a violation")
    void fabricatedContentViolates() {
        SemanticDna dna = Fixtures.handDna();
        ModelInput input = ModelInput.of(dna, Fixtures.explicitPlan(dna));
        String draft = baseline.reconstruct(input).draftText()
                + "The budget approved forty million credits for the program.\n";

        ConstraintInterface.Result result = constraints.judge(input, draft);

        assertThat(result.satisfied()).isFalse();
        assertThat(result.violations()).anySatisfy(violation ->
                assertThat(violation).contains("unsupported by the semantic"));
    }

    @Test
    @DisplayName("a protected object cannot yield a complete draft")
    void protectedObjectViolates() {
        SemanticDna dna = Fixtures.protectedDna();
        ModelInput input = ModelInput.of(dna, Fixtures.derivedPlan(dna));
        String draft = baseline.reconstruct(input).draftText();

        ConstraintInterface.Result result = constraints.judge(input, draft);

        assertThat(result.satisfied()).isFalse();
        assertThat(result.violations()).anySatisfy(violation ->
                assertThat(violation).contains("protected value"));
        assertThat(draft).doesNotContain("hunter2");
        assertThat(draft).doesNotContain("sk-live");
    }

    @Test
    @DisplayName("an empty draft is an explicit violation")
    void emptyDraftViolates() {
        SemanticDna dna = Fixtures.handDna();
        ModelInput input = ModelInput.of(dna, Fixtures.derivedPlan(dna));

        ConstraintInterface.Result result = constraints.judge(input, "");

        assertThat(result.satisfied()).isFalse();
        assertThat(result.violations()).anySatisfy(violation ->
                assertThat(violation).contains("empty"));
    }

    @Test
    @DisplayName("planning warnings travel with the result without failing it")
    void warningsTravelWithoutFailing() {
        SemanticDna dna = Fixtures.handDna();
        ReconstructionPlan plan = new ReconstructionPlan(
                dna.objectId(), dna.dnaVersion(), "0f9a2c4b", "sfs-rules/test",
                List.of(), List.of(), List.of(), List.of(),
                new com.sfs.core.rules.ReconstructionPlan.ContentContract(
                        true, dna.summary(), 0, 0),
                new com.sfs.core.rules.ReconstructionPlan.ValidationContract(
                        true, true, 0.7),
                List.of("sample planning warning"));
        ModelInput input = new ModelInput(dna.objectId(), dna, plan);
        String draft = baseline.reconstruct(input).draftText();

        ConstraintInterface.Result result = constraints.judge(input, draft);

        assertThat(result.satisfied()).isTrue();
        assertThat(result.warnings()).containsExactly("sample planning warning");
    }
}
