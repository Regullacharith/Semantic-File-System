package com.sfs.ui.semantic;

import com.sfs.contracts.semantic.ProtectedReferenceView;
import com.sfs.contracts.semantic.SemanticDnaView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies validation and invariants of the semantic contract types.
 */
@DisplayName("Semantic contracts")
class SemanticContractTest {

    private static SemanticDnaView.FidelityProfileView profile() {
        return new SemanticDnaView.FidelityProfileView(0.9, 0.85, "mock-analyzer/0.1");
    }

    private static SemanticDnaView dnaWith(List<ProtectedReferenceView> refs, int dimensions) {
        return new SemanticDnaView("sfs-obj-0001-aaaa", "sfs-dna/0.1", 1, "Summary.",
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                refs, dimensions, profile());
    }

    @Nested
    @DisplayName("ProtectedReferenceView")
    class ProtectedReference {

        @Test
        @DisplayName("has no component capable of carrying a plaintext value")
        void hasNoValueField() {
            List<String> componentNames = new ArrayList<>();
            for (RecordComponent component : ProtectedReferenceView.class.getRecordComponents()) {
                componentNames.add(component.getName().toLowerCase(Locale.ROOT));
            }

            assertThat(componentNames)
                    .as("a presentation contract must not be able to hold a secret")
                    .doesNotContain("value", "plaintext", "secret", "content", "rawvalue");
        }

        @Test
        @DisplayName("rejects a blank reference identifier")
        void rejectsBlankReferenceId() {
            assertThatThrownBy(() -> new ProtectedReferenceView(
                    "  ", ProtectedReferenceView.SensitiveType.API_KEY, "role", "location", true))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("requires a semantic role so a value can be described without exposure")
        void requiresSemanticRole() {
            assertThatThrownBy(() -> new ProtectedReferenceView(
                    "ref-1", ProtectedReferenceView.SensitiveType.API_KEY, "  ", "location", true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("semanticRole");
        }

        @Test
        @DisplayName("treats passwords as non-reversible by default")
        void passwordsAreNonReversible() {
            assertThat(ProtectedReferenceView.SensitiveType.PASSWORD.isReversibleByDefault())
                    .isFalse();
        }

        @Test
        @DisplayName("treats an unclassified sensitive value as non-reversible")
        void unclassifiedValuesAreNonReversible() {
            assertThat(ProtectedReferenceView.SensitiveType.OTHER.isReversibleByDefault())
                    .isFalse();
        }

        @ParameterizedTest
        @EnumSource(ProtectedReferenceView.SensitiveType.class)
        @DisplayName("gives every sensitive type a display label")
        void everyTypeHasLabel(ProtectedReferenceView.SensitiveType type) {
            assertThat(type.getLabel()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("SemanticDnaView")
    class Dna {

        @Test
        @DisplayName("carries no embedding vector, only its dimensionality")
        void carriesNoEmbeddingVector() {
            List<String> componentNames = new ArrayList<>();
            for (RecordComponent component : SemanticDnaView.class.getRecordComponents()) {
                componentNames.add(component.getName());
            }

            assertThat(componentNames).contains("embeddingDimensions");
            assertThat(componentNames)
                    .as("embeddings are retrieval representations, not presentation content")
                    .doesNotContain("embedding", "embeddings", "vector");
        }

        @Test
        @DisplayName("reports whether protected references are present")
        void reportsProtectedReferences() {
            assertThat(dnaWith(List.of(), 384).hasProtectedReferences()).isFalse();

            assertThat(dnaWith(List.of(new ProtectedReferenceView("ref-1",
                    ProtectedReferenceView.SensitiveType.API_KEY, "role", "loc", true)), 384)
                    .hasProtectedReferences()).isTrue();
        }

        @Test
        @DisplayName("reports whether an embedding has been generated")
        void reportsEmbeddingPresence() {
            assertThat(dnaWith(List.of(), 384).hasEmbedding()).isTrue();
            assertThat(dnaWith(List.of(), 0).hasEmbedding()).isFalse();
        }

        @Test
        @DisplayName("rejects a version below 1")
        void rejectsInvalidVersion() {
            assertThatThrownBy(() -> new SemanticDnaView("sfs-obj-0001-aaaa", "sfs-dna/0.1", 0,
                    "Summary.", List.of(), List.of(), List.of(), List.of(), List.of(),
                    List.of(), List.of(), 384, profile()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("dnaVersion");
        }

        @Test
        @DisplayName("defensively copies its collections")
        void copiesCollections() {
            List<String> mutable = new ArrayList<>();
            mutable.add("first");

            SemanticDnaView dna = new SemanticDnaView("sfs-obj-0001-aaaa", "sfs-dna/0.1", 1,
                    "Summary.", mutable, List.of(), List.of(), List.of(), List.of(),
                    List.of(), List.of(), 384, profile());
            mutable.add("second");

            assertThat(dna.concepts()).containsExactly("first");
        }
    }

    @Nested
    @DisplayName("FactView")
    class Fact {

        @Test
        @DisplayName("distinguishes critical facts from ordinary ones")
        void distinguishesCriticalFacts() {
            assertThat(new SemanticDnaView.FactView("A claim.", true, 0.9).critical()).isTrue();
            assertThat(new SemanticDnaView.FactView("A claim.", false, 0.9).critical()).isFalse();
        }

        @Test
        @DisplayName("rejects confidence outside 0.0 to 1.0")
        void rejectsOutOfRangeConfidence() {
            assertThatThrownBy(() -> new SemanticDnaView.FactView("A claim.", true, 1.5))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("converts confidence to a whole percentage")
        void convertsConfidence() {
            assertThat(new SemanticDnaView.FactView("A claim.", true, 0.944).confidencePercent())
                    .isEqualTo(94);
        }
    }

    @Nested
    @DisplayName("RelationshipView")
    class Relationship {

        @Test
        @DisplayName("preserves direction between subject and object")
        void preservesDirection() {
            var forward = new SemanticDnaView.RelationshipView("A", "supersedes", "B");
            var reverse = new SemanticDnaView.RelationshipView("B", "supersedes", "A");

            assertThat(forward).isNotEqualTo(reverse);
        }

        @Test
        @DisplayName("rejects a blank part")
        void rejectsBlankPart() {
            assertThatThrownBy(() -> new SemanticDnaView.RelationshipView("A", "  ", "B"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("StructureNodeView")
    class StructureNode {

        @Test
        @DisplayName("rejects a heading level outside 1 to 6")
        void rejectsInvalidLevel() {
            assertThatThrownBy(() -> new SemanticDnaView.StructureNodeView("Heading", 0, 0))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new SemanticDnaView.StructureNodeView("Heading", 7, 0))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
