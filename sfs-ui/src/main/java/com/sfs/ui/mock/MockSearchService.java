package com.sfs.ui.mock;

import com.sfs.contracts.file.FileStatus;
import com.sfs.contracts.search.SearchEvidence;
import com.sfs.contracts.search.SearchQuery;
import com.sfs.contracts.search.SearchResponse;
import com.sfs.contracts.search.SearchResult;
import com.sfs.contracts.search.SearchService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * In-memory stand-in for the Semantic Search Engine.
 */
@Service
@Profile("mock")
public class MockSearchService implements SearchService {

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "by", "for", "from", "how", "in",
            "is", "it", "of", "on", "or", "that", "the", "this", "to", "was", "what",
            "when", "where", "which", "who", "why", "with");

    private final List<CorpusEntry> corpus = buildCorpus();

    @Override
    public SearchResponse search(SearchQuery query) {
        long start = System.nanoTime();

        List<SearchResult> results = query.isObjectIdLookup()
                ? lookupByObjectId(query.text())
                : matchByKeywords(query);

        long tookMillis = Math.max(0, (System.nanoTime() - start) / 1_000_000);

        return new SearchResponse(
                query.text(),
                results,
                query.isObjectIdLookup()
                        ? SearchResponse.RetrievalMode.OBJECT_ID_LOOKUP
                        : SearchResponse.RetrievalMode.SEMANTIC,
                tookMillis);
    }

    
      //Resolves an exact Object ID .
     
    private List<SearchResult> lookupByObjectId(String objectId) {
        return corpus.stream()
                .filter(entry -> entry.objectId.equals(objectId))
                .map(entry -> entry.toResult(1.0, List.of(new SearchEvidence(
                        SearchEvidence.EvidenceType.SUMMARY,
                        "Resolved directly by Object ID; no similarity search was performed."))))
                .toList();
    }

    /**
     * Scores corpus entries by keyword overlap.
     */
    private List<SearchResult> matchByKeywords(SearchQuery query) {
        Set<String> terms = tokenize(query.text());
        if (terms.isEmpty()) {
            return List.of();
        }

        List<SearchResult> matches = new ArrayList<>();

        for (CorpusEntry entry : corpus) {
            List<SearchEvidence> evidence = new ArrayList<>();
            int hits = 0;

            for (String term : terms) {
                hits += entry.collectEvidence(term, evidence);
            }

            if (!evidence.isEmpty()) {
                // Normalised against the term count so relevance stays .
                double relevance = Math.min(1.0, hits / (double) (terms.size() * 2));
                matches.add(entry.toResult(Math.max(0.35, relevance), evidence));
            }
        }

        return matches.stream()
                .sorted(Comparator.comparingDouble(SearchResult::relevance).reversed())
                .limit(query.maxResults())
                .toList();
    }

    private static Set<String> tokenize(String text) {
        return java.util.Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(token -> token.length() > 2)
                .filter(token -> !STOP_WORDS.contains(token))
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * A fixed semantic record used for matching.
     */
    private record CorpusEntry(
            String objectId,
            String displayName,
            FileStatus status,
            String summary,
            List<String> concepts,
            List<String> topics,
            List<String> entities,
            List<String> facts) {

        /**
         * Adds evidence for every element matching the term.
         */
        int collectEvidence(String term, List<SearchEvidence> evidence) {
            int hits = 0;
            hits += add(term, concepts, SearchEvidence.EvidenceType.CONCEPT, "Concept", evidence);
            hits += add(term, topics, SearchEvidence.EvidenceType.TOPIC, "Topic", evidence);
            hits += add(term, entities, SearchEvidence.EvidenceType.ENTITY, "Entity", evidence);
            hits += add(term, facts, SearchEvidence.EvidenceType.FACT, "Fact", evidence);

            if (summary.toLowerCase(Locale.ROOT).contains(term)) {
                evidence.add(new SearchEvidence(
                        SearchEvidence.EvidenceType.SUMMARY,
                        "Summary mentions \"" + term + "\""));
                hits++;
            }
            return hits;
        }

        private static int add(String term, List<String> values,
                               SearchEvidence.EvidenceType type, String prefix,
                               List<SearchEvidence> evidence) {
            int hits = 0;
            for (String value : values) {
                if (value.toLowerCase(Locale.ROOT).contains(term)) {
                    evidence.add(new SearchEvidence(type, prefix + ": " + value));
                    hits++;
                }
            }
            return hits;
        }

        SearchResult toResult(double relevance, List<SearchEvidence> evidence) {
            return new SearchResult(objectId, displayName, status, relevance, summary, evidence);
        }
    }

    /**
     * Builds the fixed corpus.
     */
    private static List<CorpusEntry> buildCorpus() {
        return List.of(
                new CorpusEntry(
                        "sfs-obj-0001-a1b2c3d4",
                        "research-summary.txt",
                        FileStatus.ANALYZED,
                        "Overview of semantic file storage research and reconstruction fidelity targets.",
                        List.of("semantic representation", "knowledge preservation", "reconstruction"),
                        List.of("research", "storage"),
                        List.of("Semantic DNA", "Memory Database"),
                        List.of("Fidelity is measured across semantic, structural and factual dimensions.")),

                new CorpusEntry(
                        "sfs-obj-0002-e5f6a7b8",
                        "archived-report.txt",
                        FileStatus.MEMORIZED,
                        "Quarterly report on database performance and indexing strategy. "
                                + "Raw file deleted; semantic memory retained.",
                        List.of("database performance", "indexing", "query latency"),
                        List.of("database", "performance"),
                        List.of("PostgreSQL", "vector index"),
                        List.of("Query latency decreased by 40 percent after indexing changes.")),

                new CorpusEntry(
                        "sfs-obj-0003-c9d0e1f2",
                        "meeting-notes.txt",
                        FileStatus.ANALYZED,
                        "Project planning notes covering milestones, ownership and delivery sequence.",
                        List.of("project planning", "milestones", "delivery"),
                        List.of("planning", "process"),
                        List.of("Milestone 01", "Milestone 09"),
                        List.of("The user interface layer is delivered before the search engine.")),

                new CorpusEntry(
                        "sfs-obj-0004-b3c4d5e6",
                        "deployment-config.txt",
                        FileStatus.ANALYZED,
                        "Deployment configuration notes. Contains one credential, stored as a "
                                + "protected reference and withheld from search output.",
                        List.of("deployment", "configuration", "credentials"),
                        List.of("operations"),
                        List.of("staging environment"),
                        List.of("An API key is present and is held as a protected reference.")));
    }
}
