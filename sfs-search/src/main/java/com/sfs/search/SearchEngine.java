package com.sfs.search;

import com.sfs.contracts.search.SearchEvidence;
import com.sfs.contracts.search.SearchQuery;
import com.sfs.contracts.search.SearchResponse;
import com.sfs.contracts.search.SearchResult;
import com.sfs.contracts.search.SearchService;
import com.sfs.contracts.file.FileStatus;
import com.sfs.memory.H2MemoryDatabase;
import com.sfs.memory.ObjectSearchData;
import com.sfs.memory.VectorIndex;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class SearchEngine implements SearchService {

    private final VectorIndex vectorIndex;
    private final H2MemoryDatabase memoryDatabase;
    private final QueryParser queryParser = new QueryParser();
    private final QueryEmbedder queryEmbedder = new QueryEmbedder();
    private final RankingProfile rankingProfile;

    public SearchEngine(VectorIndex vectorIndex, H2MemoryDatabase memoryDatabase) {
        this(vectorIndex, memoryDatabase, RankingProfile.DEFAULT);
    }

    public SearchEngine(VectorIndex vectorIndex, H2MemoryDatabase memoryDatabase,
                        RankingProfile rankingProfile) {
        this.vectorIndex = Objects.requireNonNull(vectorIndex, "vectorIndex must not be null");
        this.memoryDatabase = Objects.requireNonNull(memoryDatabase, "memoryDatabase must not be null");
        this.rankingProfile = Objects.requireNonNull(rankingProfile, "rankingProfile must not be null");
    }

    @Override
    public SearchResponse search(SearchQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        long started = System.nanoTime();
        ParsedQuery parsed = queryParser.parse(query.text());

        List<SearchResult> results = parsed.isExactLookup()
                ? exactLookup(parsed)
                : semanticSearch(parsed, query.maxResults());

        long took = (System.nanoTime() - started) / 1_000_000L;
        return new SearchResponse(
                query.text(),
                results,
                parsed.isExactLookup()
                        ? SearchResponse.RetrievalMode.OBJECT_ID_LOOKUP
                        : SearchResponse.RetrievalMode.SEMANTIC,
                took);
    }

    private List<SearchResult> exactLookup(ParsedQuery parsed) {
        String objectId = parsed.exactLookupId().orElseThrow();
        return memoryDatabase.loadSearchData().stream()
                .filter(row -> row.objectId().equals(objectId) && row.searchable())
                .findFirst()
                .map(row -> List.of(toResult(row, 1.0,
                        List.of(
                                new SearchEvidence(SearchEvidence.EvidenceType.SUMMARY,
                                        "Exact Object ID lookup."),
                                new SearchEvidence(SearchEvidence.EvidenceType.VECTOR_SIMILARITY,
                                        "Resolved directly by identifier, not by similarity.")))))
                .orElse(List.of());
    }

    private List<SearchResult> semanticSearch(ParsedQuery parsed, int maxResults) {
        List<Double> queryVector = queryEmbedder.embed(parsed.terms());
        int poolSize = Math.max(25, maxResults * 3);
        List<ScoredCandidate> candidates = vectorIndex.searchTopK(queryVector, poolSize)
                .stream()
                .map(candidate -> new ScoredCandidate(candidate.objectId(), candidate.similarity()))
                .toList();

        List<ScoredCandidate> embeddable = candidates.stream()
                .filter(candidate -> similarityOf(candidate) > 0.0)
                .toList();
        if (embeddable.isEmpty()) {
            return List.of();
        }

        List<SearchResult> results = new ArrayList<>();
        for (ScoredCandidate candidate : embeddable) {
            ObjectSearchData row = rowFor(candidate.objectId());
            if (row == null || !row.searchable()) {
                continue;
            }
            List<SearchEvidence> evidence = new ArrayList<>();
            double score = rankingProfile.vectorSimilarity() * candidate.similarity();
            evidence.add(new SearchEvidence(SearchEvidence.EvidenceType.VECTOR_SIMILARITY,
                    "Semantic similarity " + Math.round(candidate.similarity() * 100) + "%."));

            score += appendMatches(evidence, SearchEvidence.EvidenceType.CONCEPT,
                    row.concepts(), parsed.terms(), rankingProfile.conceptMatch());
            score += appendMatches(evidence, SearchEvidence.EvidenceType.TOPIC,
                    row.topics(), parsed.terms(), rankingProfile.topicMatch());
            score += appendMatches(evidence, SearchEvidence.EvidenceType.ENTITY,
                    row.entities(), parsed.terms(), rankingProfile.entityMatch());
            score += appendMatches(evidence, SearchEvidence.EvidenceType.FACT,
                    row.facts(), parsed.terms(), rankingProfile.factMatch());

            if (!row.summary().isBlank()
                    && containsAny(normalize(row.summary()), parsed.terms())) {
                evidence.add(new SearchEvidence(SearchEvidence.EvidenceType.SUMMARY,
                        "The summary mentions your terms."));
                score += rankingProfile.summaryMatch();
            }
            if (!row.relationships().isEmpty()
                    && row.relationships().stream().anyMatch(relationship ->
                            containsAny(normalize(relationship), parsed.terms()))) {
                evidence.add(new SearchEvidence(SearchEvidence.EvidenceType.RELATIONSHIP,
                        "A recorded relationship involves your terms."));
                score += rankingProfile.relationshipMatch();
            }

            results.add(toResult(row, Math.min(1.0, score), evidence));
        }
        results.sort((a, b) -> {
            int byRelevance = Double.compare(b.relevance(), a.relevance());
            return byRelevance != 0 ? byRelevance : a.objectId().compareTo(b.objectId());
        });
        return results.size() > maxResults
                ? new ArrayList<>(results.subList(0, maxResults))
                : results;
    }

    private double appendMatches(List<SearchEvidence> evidence, SearchEvidence.EvidenceType type,
                                 List<String> values, List<String> terms, double weight) {
        Set<String> matched = new LinkedHashSet<>();
        for (String value : values) {
            String normalized = normalize(value);
            for (String term : terms) {
                if (normalized.contains(term)) {
                    matched.add(value);
                    break;
                }
            }
        }
        if (matched.isEmpty()) {
            return 0.0;
        }
        evidence.add(new SearchEvidence(type,
                "Matched " + type.getLabel().toLowerCase(Locale.ROOT)
                        + ": " + String.join(", ", matched) + "."));
        return weight;
    }

    private ObjectSearchData rowFor(String objectId) {
        for (ObjectSearchData row : memoryDatabase.loadSearchData()) {
            if (row.objectId().equals(objectId)) {
                return row;
            }
        }
        return null;
    }

    private double similarityOf(ScoredCandidate candidate) {
        return candidate.similarity();
    }

    private SearchResult toResult(ObjectSearchData row, double relevance,
                                  List<SearchEvidence> evidence) {
        return new SearchResult(
                row.objectId(),
                row.fileName(),
                fileStatusOf(row.state()),
                relevance,
                row.summary().isBlank() ? "No summary was captured for this object." : row.summary(),
                evidence);
    }

    private static FileStatus fileStatusOf(String state) {
        return switch (state) {
            case "REGISTERED" -> FileStatus.REGISTERED;
            case "ANALYZING" -> FileStatus.ANALYZING;
            case "ANALYZED", "MEMORIZABLE" -> FileStatus.ANALYZED;
            case "MEMORY_COMMITTED" -> FileStatus.MEMORY_COMMITTED;
            case "SOFT_DELETED" -> FileStatus.SOFT_DELETED;
            case "MEMORIZED" -> FileStatus.MEMORIZED;
            case "FAILED" -> FileStatus.FAILED;
            default -> FileStatus.REGISTERED;
        };
    }

    private static boolean containsAny(String text, List<String> terms) {
        for (String term : terms) {
            if (text.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").strip();
    }
}
