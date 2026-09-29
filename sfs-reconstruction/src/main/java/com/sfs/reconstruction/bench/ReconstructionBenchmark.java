package com.sfs.reconstruction.bench;

import com.sfs.core.dna.DnaCanonical;
import com.sfs.reconstruction.ConstraintInterface;
import com.sfs.reconstruction.ModelInput;
import com.sfs.reconstruction.ModelOutput;
import com.sfs.reconstruction.SFSReconstructionModel;
import com.sfs.reconstruction.encode.EncodedEntity;
import com.sfs.reconstruction.encode.EncodedFact;
import com.sfs.reconstruction.encode.EncodedRelationship;
import com.sfs.reconstruction.encode.SemanticDnaEncoder;
import com.sfs.reconstruction.encode.UnifiedRepresentation;
import com.sfs.reconstruction.learn.TrainingHook;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ReconstructionBenchmark {

    private final ConstraintInterface constraints;
    private final SemanticDnaEncoder encoder = new SemanticDnaEncoder();

    public ReconstructionBenchmark(ConstraintInterface constraints) {
        this.constraints = Objects.requireNonNull(
                constraints, "constraints must not be null");
    }

    public List<BenchmarkRun> run(List<ModelInput> cases,
                                  SFSReconstructionModel model,
                                  TrainingHook... hooks) {
        Objects.requireNonNull(cases, "cases must not be null");
        Objects.requireNonNull(model, "model must not be null");
        List<BenchmarkRun> runs = new ArrayList<>();
        for (ModelInput input : cases) {
            runs.add(runCase(input, model, hooks));
        }
        return List.copyOf(runs);
    }

    private BenchmarkRun runCase(ModelInput input, SFSReconstructionModel model,
                                 TrainingHook... hooks) {
        long encodeStart = System.nanoTime();
        UnifiedRepresentation representation = encoder.encode(input);
        long encodeNanos = System.nanoTime() - encodeStart;

        long reconstructStart = System.nanoTime();
        ModelOutput output = model.reconstruct(input);
        long reconstructNanos = System.nanoTime() - reconstructStart + encodeNanos;

        long verifyStart = System.nanoTime();
        ConstraintInterface.Result result =
                constraints.judge(input, output.draftText());
        long verifyNanos = System.nanoTime() - verifyStart;

        for (TrainingHook hook : hooks) {
            hook.onReconstruction(input, output, result);
        }

        String draft = output.draftText();
        int dnaBytes = DnaCanonical.serialize(input.dna())
                .getBytes(StandardCharsets.UTF_8).length;
        int draftBytes = draft.getBytes(StandardCharsets.UTF_8).length;

        return new BenchmarkRun(
                model.modelId(),
                input.objectId(),
                result.satisfied(),
                result.violations().size(),
                result.warnings().size(),
                criticalCoverage(representation, draft),
                entityCoverage(representation, draft),
                relationshipCoverage(representation, draft),
                summaryVerbatim(representation, draft),
                reconstructNanos,
                verifyNanos,
                dnaBytes,
                draftBytes);
    }

    private double criticalCoverage(UnifiedRepresentation representation,
                                    String draft) {
        List<EncodedFact> critical = representation.content().facts().stream()
                .filter(EncodedFact::critical)
                .toList();
        if (critical.isEmpty()) {
            return 1.0;
        }
        long covered = critical.stream()
                .filter(fact -> containsNormalized(draft, fact.statement()))
                .count();
        return (double) covered / critical.size();
    }

    private double entityCoverage(UnifiedRepresentation representation,
                                  String draft) {
        List<EncodedEntity> entities = representation.content().entities();
        if (entities.isEmpty()) {
            return 1.0;
        }
        long covered = entities.stream()
                .filter(entity -> containsNormalized(draft, entity.name()))
                .count();
        return (double) covered / entities.size();
    }

    private double relationshipCoverage(UnifiedRepresentation representation,
                                        String draft) {
        List<EncodedRelationship> relationships = representation.relationships();
        if (relationships.isEmpty()) {
            return 1.0;
        }
        long covered = relationships.stream()
                .filter(relationship -> containsNormalized(draft, relationship.subject())
                        && containsNormalized(draft, relationship.object()))
                .count();
        return (double) covered / relationships.size();
    }

    private boolean summaryVerbatim(UnifiedRepresentation representation,
                                    String draft) {
        return containsNormalized(draft, representation.summary());
    }

    private boolean containsNormalized(String draft, String statement) {
        return com.sfs.core.rules.PlanChecker.normalize(draft)
                .contains(com.sfs.core.rules.PlanChecker.normalize(statement));
    }
}
