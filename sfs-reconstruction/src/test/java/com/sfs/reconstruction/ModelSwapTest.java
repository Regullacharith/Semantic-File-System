package com.sfs.reconstruction;

import com.sfs.core.dna.SemanticDna;
import com.sfs.reconstruction.model.DeterministicBaselineRenderer;
import com.sfs.reconstruction.learn.NoopTrainingHook;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("The model is swappable without Memory or Search involvement")
class ModelSwapTest {

    private final SemanticDna dna = Fixtures.handDna();
    private final ModelInput input = ModelInput.of(dna, Fixtures.derivedPlan(dna));

    @Test
    @DisplayName("the first registered model is the default and stays default")
    void firstRegisteredIsDefault() {
        ModelRegistry registry = new ModelRegistry();
        DeterministicBaselineRenderer baseline = new DeterministicBaselineRenderer();
        NaiveEchoModel echo = new NaiveEchoModel();
        registry.register(baseline).register(echo);

        assertThat(registry.defaultModel()).isSameAs(baseline);
        assertThat(registry.modelIds()).containsExactly(
                NaiveEchoModel.MODEL_ID, DeterministicBaselineRenderer.MODEL_ID);
        assertThat(registry.find(NaiveEchoModel.MODEL_ID)).contains(echo);
    }

    @Test
    @DisplayName("re-registering an id replaces that model for future lookups")
    void reregistrationReplaces() {
        ModelRegistry registry = new ModelRegistry();
        registry.register(new DeterministicBaselineRenderer());
        DeterministicBaselineRenderer replacement = new DeterministicBaselineRenderer();

        registry.register(replacement);

        assertThat(registry.find(DeterministicBaselineRenderer.MODEL_ID))
                .contains(replacement);
    }

    @Test
    @DisplayName("swapping the model changes the draft through the same pipeline")
    void swapChangesDraftThroughSamePipeline() {
        ModelRegistry registry = new ModelRegistry();
        registry.register(new DeterministicBaselineRenderer());
        PlanConstraintInterface constraints = new PlanConstraintInterface();

        ModelOutput baselineDraft = registry.defaultModel().reconstruct(input);
        registry.register(new NaiveEchoModel());
        ModelOutput echoDraft = registry.find(NaiveEchoModel.MODEL_ID)
                .orElseThrow()
                .reconstruct(input);

        assertThat(baselineDraft.draftText()).isNotEqualTo(echoDraft.draftText());
        assertThat(constraints.judge(input, baselineDraft.draftText()).satisfied())
                .isTrue();
        assertThat(constraints.judge(input, echoDraft.draftText()).satisfied())
                .isFalse();
    }

    @Test
    @DisplayName("an empty registry has no default and unknown ids look up empty")
    void emptyRegistryAndUnknownIds() {
        ModelRegistry registry = new ModelRegistry();

        assertThatThrownBy(registry::defaultModel)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no reconstruction model");
        assertThat(registry.find("does-not-exist/9.9")).isEmpty();
        assertThat(registry.find(null)).isEmpty();
        assertThat(registry.find("  ")).isEmpty();
        assertThat(registry.size()).isZero();
    }

    @Test
    @DisplayName("the V0.1 training hook is a documented no-op")
    void noopTrainingHook() {
        NoopTrainingHook hook = new NoopTrainingHook();

        assertThat(hook.hookId())
                .isEqualTo("sfs-reconstruction/noop-training-hook/0.1");
        hook.onReconstruction(input,
                new ModelOutput("m/1", "draft", List.of()),
                new ConstraintInterface.Result(List.of(), List.of()));
    }
}
