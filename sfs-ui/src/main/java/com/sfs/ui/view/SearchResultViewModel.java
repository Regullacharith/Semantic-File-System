package com.sfs.ui.view;

import com.sfs.contracts.search.SearchEvidence;
import com.sfs.contracts.search.SearchResult;

import java.util.List;
import java.util.Objects;

public record SearchResultViewModel(
        String objectId,
        String displayName,
        String statusLabel,
        boolean memorized,
        int relevancePercent,
        String summary,
        List<EvidenceViewModel> evidence) {

    public static SearchResultViewModel from(SearchResult result) {
        Objects.requireNonNull(result, "result must not be null");

        return new SearchResultViewModel(
                result.objectId(),
                result.displayName(),
                result.status().getLabel(),
                result.isMemorized(),
                result.relevancePercent(),
                result.summary(),
                result.evidence().stream().map(EvidenceViewModel::from).toList());
    }

    public record EvidenceViewModel(String typeLabel, String detail) {

        static EvidenceViewModel from(SearchEvidence evidence) {
            return new EvidenceViewModel(evidence.type().getLabel(), evidence.detail());
        }
    }
}
