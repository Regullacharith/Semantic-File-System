package com.sfs.core.rules;

import com.sfs.core.dna.Relationship;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PlanChecker")
class PlanCheckerTest {

    private final PlanChecker checker = new PlanChecker();

    private ReconstructionPlan plan() {
        return new ReconstructionPlan(
                "sfs-obj-0001-a1b2c3d4",
                1,
                "a".repeat(64),
                "sfs-rules/0.2",
                List.of("Summary", "Measurements"),
                List.of(new RequiredFact("Query latency decreased by 40 percent.")),
                List.of(new RequiredEntity("PostgreSQL", 2)),
                List.of(new Relationship("PostgreSQL", "hosts", "the workload")),
                new ReconstructionPlan.ContentContract(true,
                        "Overview of the platform for Q3 2026.", 0, 0),
                new ReconstructionPlan.ValidationContract(true, true, 0.9),
                List.of());
    }

    private static final String GOOD_CANDIDATE = """
            Overview of the platform for Q3 2026.

            Summary

            Query latency decreased by 40 percent. PostgreSQL hosts the workload.
            PostgreSQL hosts the workload for the platform.

            Measurements

            PostgreSQL hosts the workload for the analytics platform.
            """;

    @Test
    @DisplayName("a compliant candidate satisfies the plan")
    void compliantCandidate() {
        PlanCompliance compliance = checker.check(plan(), GOOD_CANDIDATE);
        assertThat(compliance.satisfied()).isTrue();
        assertThat(compliance.violations()).isEmpty();
    }

    @Test
    @DisplayName("a missing required fact is a violation")
    void missingRequiredFact() {
        String candidate = GOOD_CANDIDATE.replace(
                "Query latency decreased by 40 percent. ", "");
        PlanCompliance compliance = checker.check(plan(), candidate);
        assertThat(compliance.satisfied()).isFalse();
        assertThat(compliance.violations()).anySatisfy(violation ->
                assertThat(violation).contains("required fact missing"));
    }

    @Test
    @DisplayName("too few entity mentions violate the requirement")
    void entityMentions() {
        String candidate = """
                Overview of the platform for Q3 2026.

                Summary

                Query latency decreased by 40 percent. PostgreSQL hosts the workload.
                """;
        PlanCompliance compliance = checker.check(plan(), candidate);
        assertThat(compliance.satisfied()).isFalse();
        assertThat(compliance.violations()).anySatisfy(violation ->
                assertThat(violation).contains("PostgreSQL").contains("at least 2"));
    }

    @Test
    @DisplayName("a broken section order is a violation")
    void brokenSectionOrder() {
        String candidate = """
                Overview of the platform for Q3 2026.

                Measurements

                PostgreSQL hosts the workload for the analytics platform.

                Summary

                Query latency decreased by 40 percent. PostgreSQL hosts the workload.
                PostgreSQL hosts the workload for the platform.
                """;
        PlanCompliance compliance = checker.check(plan(), candidate);
        assertThat(compliance.satisfied()).isFalse();
        assertThat(compliance.violations()).anySatisfy(violation ->
                assertThat(violation).contains("section order broken"));
    }

    @Test
    @DisplayName("an altered summary is a violation; normalization forgives case and spacing")
    void summaryVerbatim() {
        String reworded = GOOD_CANDIDATE.replace(
                "Overview of the platform for Q3 2026.",
                "Overview of the platform for Q4 2026.");
        assertThat(checker.check(plan(), reworded).violations())
                .anySatisfy(violation -> assertThat(violation).contains("summary"));

        String sloppy = "OVERVIEW   of the PLATFORM for Q3 2026.\n\n" + GOOD_CANDIDATE;
        assertThat(checker.check(plan(), sloppy).satisfied()).isTrue();
    }

    @Test
    @DisplayName("an unexpressable relationship is a violation")
    void relationshipCheck() {
        String candidate = GOOD_CANDIDATE.replace(
                "PostgreSQL hosts the workload.", "PostgreSQL runs the workload.");
        PlanCompliance compliance = checker.check(plan(), candidate);
        assertThat(compliance.violations()).isEmpty();
    }
}
