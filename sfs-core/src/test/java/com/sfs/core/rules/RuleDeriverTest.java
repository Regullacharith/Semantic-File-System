package com.sfs.core.rules;

import com.sfs.core.dna.DnaCanonical;
import com.sfs.core.dna.SemanticDna;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RuleDeriver")
class RuleDeriverTest {

    private final RuleDeriver deriver = new RuleDeriver();

    @Test
    @DisplayName("derives a set bound to the canonical DNA hash")
    void binding() {
        SemanticDna dna = RuleSetsFixture.dna();
        RuleSet set = deriver.derive(dna);

        assertThat(set.objectId()).isEqualTo(dna.objectId());
        assertThat(set.dnaVersion()).isEqualTo(dna.dnaVersion());
        assertThat(set.dnaSha256()).isEqualTo(DnaCanonical.integrityHash(dna));
        assertThat(set.rulesVersion()).isEqualTo("sfs-rules/0.2");
    }

    @Test
    @DisplayName("content rules preserve the summary verbatim with bounded minimums")
    void contentRules() {
        RuleSet set = deriver.derive(RuleSetsFixture.dna());
        Rule content = set.rules().stream()
                .filter(rule -> rule.type() == RuleType.CONTENT)
                .findFirst()
                .orElseThrow();

        ContentConstraint constraint =
                (ContentConstraint) content.constraints().getFirst();
        assertThat(constraint.preserveSummaryVerbatim()).isTrue();
        assertThat(constraint.minConcepts()).isEqualTo(1);
        assertThat(constraint.minTopics()).isEqualTo(1);
    }

    @Test
    @DisplayName("structure rules mirror the outline; ordering appears from two headings")
    void structureAndOrdering() {
        SemanticDna dna = RuleSetsFixture.dna();
        RuleSet single = deriver.derive(dna);
        assertThat(single.rules().stream()
                .anyMatch(rule -> rule.type() == RuleType.STRUCTURE)).isTrue();
        assertThat(single.rules().stream()
                .anyMatch(rule -> rule.type() == RuleType.ORDERING)).isFalse();

        SemanticDna structured = RuleDeriverTestSupport.twoHeadingDna();
        RuleSet withOrder = deriver.derive(structured);
        OrderingConstraint ordering = withOrder.rules().stream()
                .filter(rule -> rule.type() == RuleType.ORDERING)
                .findFirst()
                .orElseThrow()
                .constraints()
                .stream()
                .map(OrderingConstraint.class::cast)
                .findFirst()
                .orElseThrow();
        assertThat(ordering.preserveSectionOrder()).isTrue();
        assertThat(ordering.sectionOrder()).containsExactly("Summary", "Measurements");

        StructureConstraint structure = withOrder.rules().stream()
                .filter(rule -> rule.type() == RuleType.STRUCTURE)
                .findFirst()
                .orElseThrow()
                .constraints()
                .stream()
                .map(StructureConstraint.class::cast)
                .findFirst()
                .orElseThrow();
        assertThat(structure.requiredHeadings()).containsExactly("Summary", "Measurements");
    }

    @Test
    @DisplayName("critical facts become required facts; recurring entities are required")
    void factAndEntityRules() {
        RuleSet set = deriver.derive(RuleSetsFixture.dna());

        RequiredFactConstraint fact = set.rules().stream()
                .filter(rule -> rule.type() == RuleType.FACT)
                .findFirst()
                .orElseThrow()
                .constraints()
                .stream()
                .map(RequiredFactConstraint.class::cast)
                .findFirst()
                .orElseThrow();
        assertThat(fact.statement()).isEqualTo("Query latency decreased by 40 percent.");
        assertThat(fact.failIfMissing()).isTrue();

        RequiredEntityConstraint entity = set.rules().stream()
                .filter(rule -> rule.type() == RuleType.ENTITY)
                .findFirst()
                .orElseThrow()
                .constraints()
                .stream()
                .map(RequiredEntityConstraint.class::cast)
                .findFirst()
                .orElseThrow();
        assertThat(entity.name()).isEqualTo("PostgreSQL");
        assertThat(entity.minMentions()).isEqualTo(2);
    }

    @Test
    @DisplayName("relationships become required constraints; validation forbids invention")
    void relationshipAndValidationRules() {
        RuleSet set = deriver.derive(RuleSetsFixture.dna());

        RelationshipConstraint relationship = set.rules().stream()
                .filter(rule -> rule.type() == RuleType.RELATIONSHIP)
                .findFirst()
                .orElseThrow()
                .constraints()
                .stream()
                .map(RelationshipConstraint.class::cast)
                .findFirst()
                .orElseThrow();
        assertThat(relationship.subject()).isEqualTo("PostgreSQL");
        assertThat(relationship.failIfMissing()).isTrue();

        ValidationConstraint validation = set.rules().stream()
                .filter(rule -> rule.type() == RuleType.VALIDATION)
                .findFirst()
                .orElseThrow()
                .constraints()
                .stream()
                .map(ValidationConstraint.class::cast)
                .findFirst()
                .orElseThrow();
        assertThat(validation.forbidInventedFacts()).isTrue();
        assertThat(validation.requireAllCriticalFacts()).isTrue();
        assertThat(validation.minFactConfidence()).isEqualTo(0.95);
    }

    @Test
    @DisplayName("a derived set serializes, parses and validates cleanly")
    void derivedSetIsCanonicalAndValid() {
        RuleSet set = deriver.derive(RuleSetsFixture.dna());
        assertThat(RuleSetCanonical.parse(RuleSetCanonical.serialize(set))).isEqualTo(set);
        assertThat(new RuleSetValidator().validate(set)).isEmpty();
    }
}
