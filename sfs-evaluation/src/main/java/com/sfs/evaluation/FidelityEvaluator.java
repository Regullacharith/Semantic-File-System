package com.sfs.evaluation;

import com.sfs.contracts.evaluation.FidelityDimension;
import com.sfs.contracts.evaluation.FidelityReportView;
import com.sfs.core.dna.DnaCanonical;
import com.sfs.core.dna.SemanticDna;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class FidelityEvaluator {

    public static final String EVALUATOR_VERSION = "sfs-evaluation/0.1";

    private final SemanticEvaluator semanticEvaluator = new SemanticEvaluator();
    private final StructuralEvaluator structuralEvaluator = new StructuralEvaluator();
    private final FactualEvaluator factualEvaluator = new FactualEvaluator();
    private final EntityEvaluator entityEvaluator = new EntityEvaluator();
    private final RelationshipEvaluator relationshipEvaluator =
            new RelationshipEvaluator();
    private final CompletenessEvaluator completenessEvaluator =
            new CompletenessEvaluator();

    public FidelityReport evaluate(String jobId, EvaluationInput input) {
        Objects.requireNonNull(jobId, "jobId must not be null");
        Objects.requireNonNull(input, "input must not be null");

        SemanticScore semantic = semanticEvaluator.evaluate(input);
        StructuralScore structural = structuralEvaluator.evaluate(input);
        FactualScore factual = factualEvaluator.evaluate(input);
        EntityScore entity = entityEvaluator.evaluate(input);
        RelationshipScore relationship = relationshipEvaluator.evaluate(input);
        CompletenessScore completeness = completenessEvaluator.evaluate(input);

        List<ErrorCategory> errors = errorsOf(input.dna(), structural, factual,
                entity, relationship, semantic);
        List<CalibrationRecord> calibration = ConfidenceCalibration.calibrate(input);
        double overall = OverallFidelity.compute(dimensionScores(semantic,
                structural, factual, entity, relationship, completeness),
                factual.critical());

        SemanticDna dna = input.dna();
        return new FidelityReport(jobId, input.objectId(), semantic, structural,
                factual, entity, relationship, completeness, errors, calibration,
                overall, input.hasOriginal()
                        ? input.originalText().getBytes(StandardCharsets.UTF_8).length
                        : 0,
                input.artifactText().getBytes(StandardCharsets.UTF_8).length,
                DnaCanonical.serialize(dna)
                        .getBytes(StandardCharsets.UTF_8).length,
                EVALUATOR_VERSION, Instant.now());
    }

    public List<FidelityReportView.EvaluationFinding> findings(FidelityReport report) {
        List<FidelityReportView.EvaluationFinding> findings = new ArrayList<>();
        findings.add(new FidelityReportView.EvaluationFinding(
                FidelityDimension.SEMANTIC,
                report.semantic().toDimensionScore() >= 0.5,
                "Content-word overlap F1 against the original is "
                        + percent(report.semantic().toDimensionScore())
                        + " (" + report.semantic().matchedTokens() + " of "
                        + report.semantic().referenceTokens()
                        + " reference tokens covered)."));
        findings.add(new FidelityReportView.EvaluationFinding(
                FidelityDimension.STRUCTURAL,
                report.structural().headingsPreserved() == report.structural().headingsTotal()
                        && report.structural().orderPreserved()
                        && report.structural().summaryVerbatim(),
                report.structural().headingsPreserved() + " of "
                        + report.structural().headingsTotal()
                        + " recorded headings present"
                        + (report.structural().orderPreserved()
                                ? " in order" : " out of order")
                        + "; the summary is "
                        + (report.structural().summaryVerbatim()
                                ? "preserved verbatim." : "not preserved verbatim.")));
        findings.add(new FidelityReportView.EvaluationFinding(
                FidelityDimension.FACTUAL,
                report.factual().missingStatements().isEmpty(),
                report.factual().factsPreserved() + " of "
                        + report.factual().factsTotal()
                        + " recorded fact statements preserved exactly."));
        findings.add(new FidelityReportView.EvaluationFinding(
                FidelityDimension.FACTUAL,
                report.factual().critical().missing().isEmpty(),
                report.factual().critical().preserved() + " of "
                        + report.factual().critical().total()
                        + " critical fact(s) preserved."));
        findings.add(new FidelityReportView.EvaluationFinding(
                FidelityDimension.ENTITY,
                report.entity().missing().isEmpty(),
                report.entity().preserved() + " of " + report.entity().total()
                        + " recorded entities present without substitution."));
        findings.add(new FidelityReportView.EvaluationFinding(
                FidelityDimension.RELATIONSHIP,
                report.relationship().missing().isEmpty(),
                report.relationship().withDirection() + " of "
                        + report.relationship().total()
                        + " recorded relationships preserved with direction."));
        findings.add(new FidelityReportView.EvaluationFinding(
                FidelityDimension.COMPLETENESS,
                report.completeness().unitsPresent() == report.completeness().unitsTotal(),
                report.completeness().unitsPresent() + " of "
                        + report.completeness().unitsTotal()
                        + " semantic material units survived."));
        return List.copyOf(findings);
    }

    private List<ErrorCategory> errorsOf(SemanticDna dna, StructuralScore structural,
                                         FactualScore factual, EntityScore entity,
                                         RelationshipScore relationship,
                                         SemanticScore semantic) {
        List<ErrorCategory> errors = new ArrayList<>();
        if (!structural.summaryVerbatim()) {
            errors.add(ErrorCategory.MISSING_SUMMARY);
        }
        for (int i = 0; i < structural.headingsTotal()
                - structural.headingsPreserved(); i++) {
            errors.add(ErrorCategory.MISSING_HEADING);
        }
        if (!structural.orderPreserved()) {
            errors.add(ErrorCategory.SECTION_ORDER_BROKEN);
        }
        for (String statement : factual.missingStatements()) {
            errors.add(factual.critical().missing().contains(statement)
                    ? ErrorCategory.MISSING_CRITICAL_FACT
                    : ErrorCategory.MISSING_FACT);
        }
        for (String name : entity.missing()) {
            errors.add(ErrorCategory.MISSING_ENTITY);
        }
        for (int i = 0; i < relationship.missing().size(); i++) {
            errors.add(ErrorCategory.MISSING_RELATIONSHIP);
        }
        if (semantic.artifactOnlyTokens() > 0 && semantic.toDimensionScore() < 0.5) {
            errors.add(ErrorCategory.UNSUPPORTED_CONTENT);
        }
        if (dna.security().containsProtectedReferences()) {
            errors.add(ErrorCategory.PROTECTED_VALUE_WITHHELD);
        }
        return List.copyOf(errors);
    }

    private Map<FidelityDimension, Double> dimensionScores(SemanticScore semantic,
            StructuralScore structural, FactualScore factual, EntityScore entity,
            RelationshipScore relationship, CompletenessScore completeness) {
        Map<FidelityDimension, Double> scores = new java.util.EnumMap<>(
                FidelityDimension.class);
        scores.put(FidelityDimension.SEMANTIC, semantic.toDimensionScore());
        scores.put(FidelityDimension.STRUCTURAL, structural.toDimensionScore());
        scores.put(FidelityDimension.FACTUAL, factual.toDimensionScore());
        scores.put(FidelityDimension.ENTITY, entity.toDimensionScore());
        scores.put(FidelityDimension.RELATIONSHIP, relationship.toDimensionScore());
        scores.put(FidelityDimension.COMPLETENESS, completeness.toDimensionScore());
        return scores;
    }

    private int percent(double score) {
        return (int) Math.round(score * 100);
    }
}
