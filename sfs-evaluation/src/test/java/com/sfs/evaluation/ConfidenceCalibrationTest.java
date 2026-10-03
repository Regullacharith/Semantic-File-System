package com.sfs.evaluation;

import com.sfs.core.dna.DnaIdentity;
import com.sfs.core.dna.Fact;
import com.sfs.core.dna.FidelityProfile;
import com.sfs.core.dna.SemanticDna;
import com.sfs.core.dna.SemanticDnaBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Confidence calibration (12.8)")
class ConfidenceCalibrationTest {

    @Test
    @DisplayName("facts are binned by stated confidence against observed survival")
    void bins() {
        SemanticDna dna = SemanticDnaBuilder.forIdentity(new DnaIdentity(
                        "sfs-obj-6003-cccccccc", "sfs-dna/0.2", 1,
                        "sfs-engine/0.1", Instant.parse("2026-06-01T08:00:00Z")))
                .summary("Calibration fixture.")
                .facts(List.of(
                        new Fact("alpha survives with high confidence", true, 0.95),
                        new Fact("beta survives with high confidence", true, 0.92),
                        new Fact("gamma is lost with mid confidence", false, 0.7),
                        new Fact("delta is lost with low confidence", false, 0.5)))
                .fidelity(new FidelityProfile(0.9, 0.9, "sfs-engine/0.1"))
                .build();
        String artifact = "alpha survives with high confidence\n"
                + "beta survives with high confidence\nCalibration fixture.\n";
        EvaluationInput input = EvaluationInput.of("sfs-obj-6003-cccccccc",
                null, artifact, dna);

        List<CalibrationRecord> records = ConfidenceCalibration.calibrate(input);

        assertThat(records).hasSize(4);
        CalibrationRecord high = records.get(3);
        assertThat(high.lowerBound()).isEqualTo(0.9);
        assertThat(high.factCount()).isEqualTo(2);
        assertThat(high.meanStatedConfidence()).isEqualTo(0.935);
        assertThat(high.observedSurvivalRate()).isEqualTo(1.0);
        assertThat(high.delta()).isCloseTo(-0.065, org.assertj.core.data.Offset.offset(1e-9));
        assertThat(high.populated()).isTrue();

        CalibrationRecord lowerMid = records.get(1);
        assertThat(lowerMid.lowerBound()).isEqualTo(0.6);
        assertThat(lowerMid.factCount()).isEqualTo(1);
        assertThat(lowerMid.observedSurvivalRate()).isEqualTo(0.0);
        assertThat(lowerMid.delta()).isCloseTo(0.7,
                org.assertj.core.data.Offset.offset(1e-9));

        CalibrationRecord low = records.get(0);
        assertThat(low.factCount()).isEqualTo(1);
        assertThat(low.observedSurvivalRate()).isEqualTo(0.0);

        CalibrationRecord empty = records.get(2);
        assertThat(empty.populated()).isFalse();
    }

    @Test
    @DisplayName("an unpopulated bin reports zero counts without invented rates")
    void emptyBinsAreExplicit() {
        SemanticDna dna = SemanticDnaBuilder.forIdentity(new DnaIdentity(
                        "sfs-obj-6004-dddddddd", "sfs-dna/0.2", 1,
                        "sfs-engine/0.1", Instant.parse("2026-06-01T08:00:00Z")))
                .summary("No facts here.")
                .fidelity(new FidelityProfile(0.9, 0.9, "sfs-engine/0.1"))
                .build();
        EvaluationInput input = EvaluationInput.of("sfs-obj-6004-dddddddd", null,
                "No facts here.", dna);

        List<CalibrationRecord> records = ConfidenceCalibration.calibrate(input);

        assertThat(records).allSatisfy(record -> {
            assertThat(record.populated()).isFalse();
            assertThat(record.meanStatedConfidence()).isEqualTo(0.0);
            assertThat(record.observedSurvivalRate()).isEqualTo(0.0);
        });
    }
}
