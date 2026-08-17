package com.sfs.ui.search;

import com.sfs.contracts.file.FileStatus;
import com.sfs.contracts.search.SearchEvidence;
import com.sfs.contracts.search.SearchQuery;
import com.sfs.contracts.search.SearchResponse;
import com.sfs.contracts.search.SearchResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies validation and invariants of the search contract types.
 */
@DisplayName("Search contracts")
class SearchContractTest {

    private static SearchEvidence anyEvidence() {
        return new SearchEvidence(SearchEvidence.EvidenceType.CONCEPT, "Concept: testing");
    }

    @Nested
    @DisplayName("SearchQuery")
    class Query {

        @Test
        @DisplayName("strips surrounding whitespace")
        void stripsWhitespace() {
            assertThat(SearchQuery.of("  database  ").text()).isEqualTo("database");
        }

        @Test
        @DisplayName("rejects a blank query")
        void rejectsBlank() {
            assertThatThrownBy(() -> SearchQuery.of("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("blank");
        }

        @Test
        @DisplayName("rejects a query beyond the length limit")
        void rejectsOverLongQuery() {
            assertThatThrownBy(() -> SearchQuery.of("x".repeat(SearchQuery.MAX_QUERY_LENGTH + 1)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects an out-of-range result limit")
        void rejectsInvalidResultLimit() {
            assertThatThrownBy(() -> new SearchQuery("database", 0))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new SearchQuery("database", SearchQuery.MAX_ALLOWED_RESULTS + 1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @ValueSource(strings = {"sfs-obj-0001-a1b2c3d4", "sfs-obj-9999-ffffffff"})
        @DisplayName("recognises an exact Object ID")
        void recognisesObjectId(String id) {
            assertThat(SearchQuery.of(id).isObjectIdLookup()).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "database performance",
                "sfs-obj",
                "sfs-obj-0001",
                "notes.txt",
                "what is the project about"
        })
        @DisplayName("treats anything else as a meaning-based query")
        void treatsOtherTextAsSemantic(String text) {
            assertThat(SearchQuery.of(text).isObjectIdLookup()).isFalse();
        }
    }

    @Nested
    @DisplayName("SearchResult")
    class Result {

        @Test
        @DisplayName("requires at least one piece of evidence")
        void requiresEvidence() {
            // A result with no evidence cannot explain why it matched.
            assertThatThrownBy(() -> new SearchResult(
                    "sfs-obj-0001-aaaa", "notes.txt", FileStatus.ANALYZED,
                    0.9, "Summary", List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("evidence");
        }

        @Test
        @DisplayName("rejects a relevance score outside 0.0 to 1.0")
        void rejectsOutOfRangeRelevance() {
            assertThatThrownBy(() -> new SearchResult(
                    "sfs-obj-0001-aaaa", "notes.txt", FileStatus.ANALYZED,
                    1.5, "Summary", List.of(anyEvidence())))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("converts relevance to a whole percentage")
        void convertsRelevanceToPercent() {
            SearchResult result = new SearchResult("sfs-obj-0001-aaaa", "notes.txt",
                    FileStatus.ANALYZED, 0.856, "Summary", List.of(anyEvidence()));

            assertThat(result.relevancePercent()).isEqualTo(86);
        }

        @Test
        @DisplayName("reports a memorized record as having no raw data")
        void reportsMemorizedStatus() {
            SearchResult memorized = new SearchResult("sfs-obj-0002-bbbb", "gone.txt",
                    FileStatus.MEMORIZED, 0.9, "Summary", List.of(anyEvidence()));

            assertThat(memorized.isMemorized()).isTrue();
        }

        @Test
        @DisplayName("defensively copies its evidence list")
        void copiesEvidence() {
            List<SearchEvidence> mutable = new ArrayList<>();
            mutable.add(anyEvidence());

            SearchResult result = new SearchResult("sfs-obj-0001-aaaa", "notes.txt",
                    FileStatus.ANALYZED, 0.9, "Summary", mutable);
            mutable.add(anyEvidence());

            assertThat(result.evidence()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("SearchEvidence")
    class Evidence {

        @Test
        @DisplayName("rejects blank detail")
        void rejectsBlankDetail() {
            assertThatThrownBy(() ->
                    new SearchEvidence(SearchEvidence.EvidenceType.CONCEPT, "  "))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("gives every evidence type a display label")
        void everyTypeHasLabel() {
            for (SearchEvidence.EvidenceType type : SearchEvidence.EvidenceType.values()) {
                assertThat(type.getLabel()).isNotBlank();
            }
        }
    }

    @Nested
    @DisplayName("SearchResponse")
    class Response {

        @Test
        @DisplayName("reports emptiness and result count")
        void reportsCounts() {
            SearchResponse empty = new SearchResponse("q", List.of(),
                    SearchResponse.RetrievalMode.SEMANTIC, 5);

            assertThat(empty.isEmpty()).isTrue();
            assertThat(empty.resultCount()).isZero();
        }

        @Test
        @DisplayName("rejects a negative duration")
        void rejectsNegativeDuration() {
            assertThatThrownBy(() -> new SearchResponse("q", List.of(),
                    SearchResponse.RetrievalMode.SEMANTIC, -1))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
