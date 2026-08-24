package com.sfs.contracts.file;

import java.util.Objects;

public record DeletionConfirmation(String acknowledgedObjectId) {

    public DeletionConfirmation {
        Objects.requireNonNull(acknowledgedObjectId, "acknowledgedObjectId must not be null");

        if (acknowledgedObjectId.isBlank()) {
            throw new IllegalArgumentException("acknowledgedObjectId must not be blank");
        }
    }

    public boolean confirms(String objectId) {
        return acknowledgedObjectId.equals(objectId);
    }
}
