package com.sfs.ui.mock;

import com.sfs.contracts.search.SearchEvidence;
import com.sfs.contracts.search.SearchQuery;
import com.sfs.contracts.search.SearchResponse;
import com.sfs.contracts.search.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the mock search service's behavioural contracts.
 */
@DisplayName("Mock search service")
class MockSearchServiceTest {

    private MockSearchService service;

    @BeforeEach
    void setUp() {
        service = new MockSearchService();
    }

    @Nested
    @DisplayName("semantic matching")
    class SemanticMatching {

        @Test
        @DisplayName("finds records by meaning without the file name in the query")
        void findsByMeaningNotFileName() {
            // The  searching without relying on the filename.
            SearchResponse response = service.search(SearchQuery.of("database performance"));

            assertThat(response.results()).isNotEmpty();
            assertThat(response.results())
                    .extracting(SearchResult::displayName)
                    .contains("archived-report.txt");
        }

        @Test
        @DisplayName("reports the semantic retrieval mode")
        void reportsSemanticMode() {
            assertThat(service.search(SearchQuery.of("database")).retrieval())
                    .isEqualTo(SearchResponse.RetrievalMode.SEMANTIC);
        }

        @Test
        @DisplayName("orders results by descending relevance")
        void ordersByRelevance() {
            SearchResponse response = service.search(SearchQuery.of("semantic reconstruction"));

            assertThat(response.results())
                    .isSortedAccordingTo((a, b) -> Double.compare(b.relevance(), a.relevance()));
        }

        @Test
        @DisplayName("returns no results for an unrelated query rather than guessing")
        void returnsEmptyForUnrelatedQuery() {
            SearchResponse response = service.search(SearchQuery.of("zzzqqq nonexistent"));

            assertThat(response.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("ignores stop words so common words do not match everything")
        void ignoresStopWords() {

            assertThat(service.search(SearchQuery.of("the and of it")).isEmpty()).isTrue();
        }

        @Test
        @DisplayName("respects the requested result limit")
        void respectsResultLimit() {
            SearchResponse response = service.search(new SearchQuery("semantic database project", 1));

            assertThat(response.results()).hasSizeLessThanOrEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Object ID lookup")
    class ObjectIdLookup {

        @Test
        @DisplayName("resolves an exact Object ID directly")
        void resolvesExactObjectId() {
            SearchResponse response = service.search(SearchQuery.of("sfs-obj-0002-e5f6a7b8"));

            assertThat(response.results()).hasSize(1);
            assertThat(response.results().getFirst().objectId()).isEqualTo("sfs-obj-0002-e5f6a7b8");
        }

        @Test
        @DisplayName("reports that similarity search was bypassed")
        void reportsLookupMode() {
            SearchResponse response = service.search(SearchQuery.of("sfs-obj-0001-a1b2c3d4"));

            assertThat(response.retrieval())
                    .isEqualTo(SearchResponse.RetrievalMode.OBJECT_ID_LOOKUP);
        }

        @Test
        @DisplayName("returns exactly one result, at full relevance")
        void lookupIsExact() {
            SearchResponse response = service.search(SearchQuery.of("sfs-obj-0001-a1b2c3d4"));

            assertThat(response.results()).hasSize(1);
            assertThat(response.results().getFirst().relevance()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("returns nothing for an unknown but well-formed Object ID")
        void unknownObjectIdReturnsNothing() {
            assertThat(service.search(SearchQuery.of("sfs-obj-9999-ffffffff")).isEmpty()).isTrue();
        }
    }

    @Nested
    @DisplayName("memorized records")
    class MemorizedRecords {

        @Test
        @DisplayName("finds a record whose raw bytes have been deleted")
        void findsMemorizedRecord() {
            // semantic memory outlives the raw file.
            SearchResponse response = service.search(SearchQuery.of("indexing strategy"));

            assertThat(response.results())
                    .anyMatch(SearchResult::isMemorized);
        }
    }

    @Nested
    @DisplayName("evidence")
    class Evidence {

        @Test
        @DisplayName("attaches evidence to every result")
        void everyResultHasEvidence() {
            SearchResponse response = service.search(SearchQuery.of("semantic database project"));

            assertThat(response.results()).isNotEmpty();
            assertThat(response.results()).allSatisfy(result ->
                    assertThat(result.evidence()).isNotEmpty());
        }

        @Test
        @DisplayName("attributes matches to concrete representation elements")
        void evidenceNamesRepresentationElement() {
            SearchResponse response = service.search(SearchQuery.of("indexing"));

            assertThat(response.results().getFirst().evidence())
                    .extracting(SearchEvidence::type)
                    .containsAnyOf(
                            SearchEvidence.EvidenceType.CONCEPT,
                            SearchEvidence.EvidenceType.TOPIC,
                            SearchEvidence.EvidenceType.ENTITY,
                            SearchEvidence.EvidenceType.FACT,
                            SearchEvidence.EvidenceType.SUMMARY);
        }
    }

    @Nested
    @DisplayName("protected values")
    class ProtectedValues {

         /* this shape must never appear in search output. */
        private static final String SECRET_SHAPE = "sk-";

        @Test
        @DisplayName("describes a credential by role and never exposes its value")
        void doesNotLeakProtectedValues() {
            SearchResponse response = service.search(SearchQuery.of("deployment credentials"));

            assertThat(response.results()).isNotEmpty();

            for (SearchResult result : response.results()) {
                assertThat(result.summary()).doesNotContain(SECRET_SHAPE);
                assertThat(result.evidence())
                        .allSatisfy(e -> assertThat(e.detail()).doesNotContain(SECRET_SHAPE));
            }
        }

        @Test
        @DisplayName("still finds the record by the role of its sensitive content")
        void findsRecordBySemanticRole() {
            // Searching for the concept must work even though the value is withheld.
            SearchResponse response = service.search(SearchQuery.of("credentials"));

            assertThat(response.results())
                    .extracting(SearchResult::displayName)
                    .contains("deployment-config.txt");
        }
    }
}
