package com.sfs.ui.mock;

import com.sfs.contracts.semantic.ProtectedReferenceView;
import com.sfs.contracts.semantic.SemanticDnaView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the semantic record fixtures.
 */
@DisplayName("Mock semantic record service")
class MockSemanticRecordServiceTest {

    private static final String MEMORIZED_OBJECT = "sfs-obj-0002-e5f6a7b8";
    private static final String SENSITIVE_OBJECT = "sfs-obj-0004-b3c4d5e6";

    private MockSemanticRecordService service;

    @BeforeEach
    void setUp() {
        service = new MockSemanticRecordService();
    }

    @Test
    @DisplayName("returns Semantic DNA for a known object")
    void returnsDnaForKnownObject() {
        assertThat(service.findSemanticDna("sfs-obj-0001-a1b2c3d4")).isPresent();
    }

    @Test
    @DisplayName("returns empty for an unknown or blank Object ID")
    void returnsEmptyForUnknownObject() {
        assertThat(service.findSemanticDna("sfs-obj-9999-ffffffff")).isEmpty();
        assertThat(service.findSemanticDna("")).isEmpty();
        assertThat(service.findSemanticDna(null)).isEmpty();
    }

    @Test
    @DisplayName("retains full semantic memory for a record whose raw file is gone")
    void memorizedRecordRetainsFullSemanticMemory() {
        SemanticDnaView dna = service.findSemanticDna(MEMORIZED_OBJECT).orElseThrow();

        assertThat(dna.summary()).isNotBlank();
        assertThat(dna.concepts()).isNotEmpty();
        assertThat(dna.facts()).isNotEmpty();
        assertThat(dna.relationships()).isNotEmpty();
        assertThat(dna.structure()).isNotEmpty();
        assertThat(dna.hasEmbedding()).isTrue();
    }

    @Test
    @DisplayName("marks some facts critical so reconstruction can be scored on them")
    void marksCriticalFacts() {
        SemanticDnaView dna = service.findSemanticDna(MEMORIZED_OBJECT).orElseThrow();

        assertThat(dna.facts()).anyMatch(SemanticDnaView.FactView::critical);
        assertThat(dna.facts()).anyMatch(fact -> !fact.critical());
    }

    @Test
    @DisplayName("records relationships with an explicit direction")
    void recordsDirectionalRelationships() {
        SemanticDnaView dna = service.findSemanticDna(MEMORIZED_OBJECT).orElseThrow();

        assertThat(dna.relationships()).allSatisfy(rel -> {
            assertThat(rel.subject()).isNotBlank();
            assertThat(rel.type()).isNotBlank();
            assertThat(rel.object()).isNotBlank();
        });
    }

    @Test
    @DisplayName("exercises both reversible and non-reversible sensitive policies")
    void exercisesBothSensitivePolicies() {
        SemanticDnaView dna = service.findSemanticDna(SENSITIVE_OBJECT).orElseThrow();

        assertThat(dna.hasProtectedReferences()).isTrue();
        assertThat(dna.protectedReferences())
                .extracting(ProtectedReferenceView::sensitiveType)
                .contains(ProtectedReferenceView.SensitiveType.API_KEY,
                        ProtectedReferenceView.SensitiveType.PASSWORD);

        assertThat(dna.protectedReferences())
                .filteredOn(ref -> ref.sensitiveType() == ProtectedReferenceView.SensitiveType.PASSWORD)
                .allSatisfy(ref -> assertThat(ref.resolvable()).isFalse());
    }

    @Test
    @DisplayName("keeps sensitive values out of every fixture field")
    void noFixtureContainsASecretValue() {
        for (String objectId : new String[]{
                "sfs-obj-0001-a1b2c3d4", MEMORIZED_OBJECT,
                "sfs-obj-0003-c9d0e1f2", SENSITIVE_OBJECT}) {

            String rendered = service.findSemanticDna(objectId).orElseThrow().toString();

            assertThat(rendered)
                    .as("no fixture may contain secret-shaped material")
                    .doesNotContain("sk-", "Bearer ", "password=", "apikey=", "AKIA");
        }
    }

    @Test
    @DisplayName("describes the sensitive document by role rather than by value")
    void describesSensitiveContentByRole() {
        SemanticDnaView dna = service.findSemanticDna(SENSITIVE_OBJECT).orElseThrow();

        assertThat(dna.protectedReferences())
                .allSatisfy(ref -> assertThat(ref.semanticRole()).isNotBlank());
        assertThat(dna.summary()).contains("protected references");
    }

    @Test
    @DisplayName("reports a schema version on every record")
    void everyRecordIsVersioned() {
        for (String objectId : new String[]{
                "sfs-obj-0001-a1b2c3d4", MEMORIZED_OBJECT,
                "sfs-obj-0003-c9d0e1f2", SENSITIVE_OBJECT}) {

            SemanticDnaView dna = service.findSemanticDna(objectId).orElseThrow();
            assertThat(dna.schemaVersion()).isNotBlank();
            assertThat(dna.dnaVersion()).isGreaterThanOrEqualTo(1);
        }
    }
}
