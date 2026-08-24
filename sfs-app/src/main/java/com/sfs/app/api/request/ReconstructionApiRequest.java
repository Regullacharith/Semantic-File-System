package com.sfs.app.api.request;

import com.sfs.core.identity.ObjectId;

import java.util.Objects;

public record ReconstructionApiRequest(String objectId) {

    public ReconstructionApiRequest {
        Objects.requireNonNull(objectId, "objectId must not be null");

        ObjectId.of(objectId);
    }

    public ObjectId toObjectId() {
        return ObjectId.of(objectId);
    }
}
