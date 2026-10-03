package com.sfs.evaluation;

import com.sfs.core.dna.DnaIdentity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Evaluation model contracts")
class EvaluationModelsTest {

    private static final com.sfs.core.dna.SemanticDna DNA =
            EngineSupport.dna("sfs-obj-6001-aaaaaaaa");

    @Test
    @DisplayName("evaluation input requires an object id, an artifact and DNA")
    void inputValidation() {
        assertThatThrownBy(() -> EvaluationInput.of("  ", "original", "artifact", DNA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EvaluationInput.of("sfs-obj-6001-aaaaaaaa", "o", " ", DNA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EvaluationInput.of("sfs-obj-6001-aaaaaaaa", "o",
                "artifact", null))
                .isInstanceOf(NullPointerException.class);
        assertThat(EvaluationInput.of("sfs-obj-6001-aaaaaaaa", null, "artifact", DNA)
                .hasOriginal()).isFalse();
        assertThat(EvaluationInput.of("sfs-obj-6001-aaaaaaaa", "original text",
                "artifact", DNA).hasOriginal()).isTrue();
    }

    @Test
    @DisplayName("dimension scores reject impossible bounds and counts")
    void scoreValidation() {
        assertThatThrownBy(() -> new SemanticScore(1.5, 10, 5, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new StructuralScore(3, 4, true, true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FactualScore(2, 3,
                new CriticalFactScore(1, 1, List.of()), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CriticalFactScore(1, 2, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RelationshipScore(2, 1, 2, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CompletenessScore(4, 5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("empty material scores as fully preserved rather than zero")
    void emptyMaterialScoresFullyPreserved() {
        assertThat(new FactualScore(0, 0,
                new CriticalFactScore(0, 0, List.of()), List.of()).toDimensionScore())
                .isEqualTo(1.0);
        assertThat(new EntityScore(0, 0, List.of()).toDimensionScore()).isEqualTo(1.0);
        assertThat(new RelationshipScore(0, 0, 0, List.of()).toDimensionScore())
                .isEqualTo(1.0);
        assertThat(new CompletenessScore(0, 0).toDimensionScore()).isEqualTo(1.0);
        assertThat(new StructuralScore(0, 0, true, true).toDimensionScore())
                .isEqualTo(1.0);
    }

    @Test
    @DisplayName("critical fact score exposes its survival rate and missing statements")
    void criticalScoreRate() {
        CriticalFactScore score = new CriticalFactScore(4, 3,
                List.of("the lost fact"));

        assertThat(score.rate()).isEqualTo(0.75);
        assertThat(score.missing()).containsExactly("the lost fact");
        assertThat(new CriticalFactScore(0, 0, List.of()).rate()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("error categories carry advice and mark correctness failures")
    void errorCategories() {
        assertThat(ErrorCategory.MISSING_CRITICAL_FACT.correctness()).isTrue();
        assertThat(ErrorCategory.UNSUPPORTED_CONTENT.correctness()).isTrue();
        assertThat(ErrorCategory.MISSING_HEADING.correctness()).isFalse();
        assertThat(ErrorCategory.MISSING_CRITICAL_FACT.advice()).isNotBlank();
        for (ErrorCategory category : ErrorCategory.values()) {
            assertThat(category.description()).isNotBlank();
            assertThat(category.advice()).isNotBlank();
        }
    }

    @Test
    @DisplayName("calibration records validate bounds, counts and rates")
    void calibrationValidation() {
        assertThatThrownBy(() -> new CalibrationRecord(0.9, 0.6, 1, 0.8, 0.8))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CalibrationRecord(0.6, 0.75, -1, 0.7, 0.7))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CalibrationRecord(0.6, 0.75, 2, 1.5, 0.7))
                .isInstanceOf(IllegalArgumentException.class);

        CalibrationRecord record =
                new CalibrationRecord(0.9, 1.0, 3, 0.95, 0.67);
        assertThat(record.delta()).isCloseTo(0.28, org.assertj.core.data.Offset.offset(1e-9));
        assertThat(record.populated()).isTrue();
        assertThat(new CalibrationRecord(0.0, 0.6, 0, 0.0, 0.0).populated())
                .isFalse();
    }

    @Test
    @DisplayName("semantic scores track matched and unsupported token counts")
    void semanticScoreFields() {
        SemanticScore score = new SemanticScore(0.5, 20, 10, 3);

        assertThat(score.toDimensionScore()).isEqualTo(0.5);
        assertThat(score.matchedTokens()).isEqualTo(10);
        assertThat(score.artifactOnlyTokens()).isEqualTo(3);
    }

}
