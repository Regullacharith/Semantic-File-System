package com.sfs.search;

record ScoredCandidate(String objectId, double similarity) {

    ScoredCandidate {
        if (objectId == null || objectId.isBlank()) {
            throw new IllegalArgumentException("objectId must not be blank");
        }
    }
}
