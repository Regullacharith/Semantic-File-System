package com.sfs.lifecycle.model;

import com.sfs.core.identity.ObjectId;
import com.sfs.lifecycle.state.FileState;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record SemanticFile(
        ObjectId objectId,
        FileMetadata metadata,
        FileState state,
        FileState deletedFrom,
        String certifiedDnaVersion,
        List<FileVersion> versions,
        Instant stateChangedAt) {

    public SemanticFile {
        Objects.requireNonNull(objectId, "objectId must not be null");
        Objects.requireNonNull(metadata, "metadata must not be null");
        Objects.requireNonNull(state, "state must not be null");
        if (deletedFrom != null && state != FileState.SOFT_DELETED) {
            throw new IllegalArgumentException(
                    "deletedFrom must be null unless the state is SOFT_DELETED");
        }
        if (deletedFrom == FileState.SOFT_DELETED) {
            throw new IllegalArgumentException("deletedFrom must not be SOFT_DELETED itself");
        }
        if (certifiedDnaVersion != null && certifiedDnaVersion.isBlank()) {
            throw new IllegalArgumentException("certifiedDnaVersion must not be blank when present");
        }
        Objects.requireNonNull(versions, "versions must not be null");
        if (versions.isEmpty()) {
            throw new IllegalArgumentException("a SemanticFile always carries at least version 1");
        }
        for (int i = 0; i < versions.size(); i++) {
            if (versions.get(i).number() != i + 1) {
                throw new IllegalArgumentException("version numbers must be contiguous starting at 1");
            }
        }
        versions = List.copyOf(versions);
        Objects.requireNonNull(stateChangedAt, "stateChangedAt must not be null");
    }

    public static SemanticFile initial(ObjectId objectId, FileMetadata metadata,
                                       FileVersion firstVersion, Instant at) {
        if (firstVersion.number() != 1) {
            throw new IllegalArgumentException("the initial version must be version 1");
        }
        return new SemanticFile(objectId, metadata, FileState.REGISTERED, null,
                null, List.of(firstVersion), at);
    }

    public SemanticFile withState(FileState newState, Instant at) {
        Objects.requireNonNull(newState, "newState must not be null");
        return new SemanticFile(objectId, metadata, newState, deletedFrom,
                certifiedDnaVersion, versions, at);
    }

    public SemanticFile softDeletedFrom(FileState origin, Instant at) {
        Objects.requireNonNull(origin, "origin must not be null");
        if (origin == FileState.SOFT_DELETED) {
            throw new IllegalArgumentException("origin must not be SOFT_DELETED itself");
        }
        return new SemanticFile(objectId, metadata, FileState.SOFT_DELETED, origin,
                certifiedDnaVersion, versions, at);
    }

    public SemanticFile withDeletionCleared(FileState restoredState, Instant at) {
        return new SemanticFile(objectId, metadata, restoredState, null,
                certifiedDnaVersion, versions, at);
    }

    public SemanticFile withCertifiedDna(String dnaVersion, Instant at) {
        Objects.requireNonNull(dnaVersion, "dnaVersion must not be null");
        if (dnaVersion.isBlank()) {
            throw new IllegalArgumentException("dnaVersion must not be blank");
        }
        return new SemanticFile(objectId, metadata, state, deletedFrom,
                dnaVersion, versions, at);
    }

    public SemanticFile withMetadata(FileMetadata newMetadata) {
        Objects.requireNonNull(newMetadata, "newMetadata must not be null");
        return new SemanticFile(objectId, newMetadata, state, deletedFrom,
                certifiedDnaVersion, versions, stateChangedAt);
    }

    public SemanticFile withAdditionalVersion(FileVersion nextVersion) {
        Objects.requireNonNull(nextVersion, "nextVersion must not be null");
        if (nextVersion.number() != versions.size() + 1) {
            throw new IllegalArgumentException(
                    "next version must be " + (versions.size() + 1));
        }
        List<FileVersion> extended = new ArrayList<>(versions);
        extended.add(nextVersion);
        return new SemanticFile(objectId, metadata, state, deletedFrom,
                certifiedDnaVersion, List.copyOf(extended), stateChangedAt);
    }

    public FileVersion currentVersion() {
        return versions.getLast();
    }
}
