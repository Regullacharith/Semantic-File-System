package com.sfs.engine.pipeline;

import com.sfs.engine.core.SemanticContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class SemanticPipeline {

    private final List<Analyzer> analyzers;
    private final DnaValidator validator;

    public SemanticPipeline(List<Analyzer> analyzers, DnaValidator validator) {
        Objects.requireNonNull(analyzers, "analyzers must not be null");
        if (analyzers.isEmpty()) {
            throw new IllegalArgumentException("the pipeline needs at least one analyzer");
        }
        this.analyzers = List.copyOf(analyzers);
        this.validator = Objects.requireNonNull(validator, "validator must not be null");
    }

    public static SemanticPipeline v1() {
        return new SemanticPipeline(
                List.of(
                        new TextParsingAnalyzer(),
                        new SummaryAnalyzer(),
                        new StructureAnalyzer(),
                        new ConceptAnalyzer(),
                        new TopicAnalyzer(),
                        new EntityAnalyzer(),
                        new FactAnalyzer(),
                        new RelationshipAnalyzer(),
                        new EmbeddingAnalyzer(),
                        new ProtectedValueAnalyzer(),
                        new DnaBuilderAnalyzer()),
                new DnaValidator());
    }

    public List<String> stageNames() {
        return analyzers.stream().map(Analyzer::name).toList();
    }

    public PipelineResult run(SemanticContext context) {
        return run(context, java.util.List.of());
    }

    public PipelineResult run(SemanticContext context,
                              java.util.List<com.sfs.contracts.semantic.SemanticDnaView.StructureNodeView>
                                      providedStructure) {
        SemanticIntermediateRepresentation ir = new SemanticIntermediateRepresentation(
                context.content());
        if (providedStructure != null && !providedStructure.isEmpty()) {
            ir.setStructure(providedStructure);
        }
        Map<String, Long> durations = new LinkedHashMap<>();
        for (Analyzer analyzer : analyzers) {
            long started = System.nanoTime();
            try {
                analyzer.perform(context, ir);
            } catch (RuntimeException e) {
                long elapsed = (System.nanoTime() - started) / 1_000_000L;
                durations.put(analyzer.name(), elapsed);
                return PipelineResult.failure(analyzer.name(),
                        "Stage '" + analyzer.name() + "' failed: " + safeMessage(e), durations);
            }
            durations.put(analyzer.name(), (System.nanoTime() - started) / 1_000_000L);
        }
        if (ir.dnaDraft() == null) {
            return PipelineResult.failure("dna-builder",
                    "The pipeline produced no Semantic DNA.", durations);
        }
        List<String> issues = validator.validate(context, ir);
        if (!issues.isEmpty()) {
            return PipelineResult.invalid(issues, durations);
        }
        return PipelineResult.success(ir.dnaDraft(), durations);
    }

    private static String safeMessage(RuntimeException e) {
        String message = e.getMessage();
        return message == null ? e.getClass().getSimpleName() : message;
    }
}
