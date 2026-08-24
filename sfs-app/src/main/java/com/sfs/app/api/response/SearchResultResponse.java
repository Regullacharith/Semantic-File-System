package com.sfs.app.api.response;

import com.sfs.contracts.search.SearchResult;

import java.util.List;
import java.util.Objects;

public record SearchResultResponse(
        String objectId,
        String fileName,
        String status,
        double relevance,
        String summary,
        boolean rawDataRemoved,
        List<EvidenceResponse> evidence) {

    public SearchResultResponse {
        Objects.requireNonNull(objectId, "objectId must not be null");
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }

    public static SearchResultResponse from(SearchResult result) {
        Objects.requireNonNull(result, "result must not be null");

        return new SearchResultResponse(
                result.objectId(),
                result.displayName(),
                result.status().name(),
                result.relevance(),
                result.summary(),
                result.status().isRawDataRemoved(),
                result.evidence().stream().map(EvidenceResponse::from).toList());
    }

    public record EvidenceResponse(String type, String detail) {

        public static EvidenceResponse from(com.sfs.contracts.search.SearchEvidence evidence) {
            return new EvidenceResponse(evidence.type().name(), evidence.detail());
        }
    }
}
