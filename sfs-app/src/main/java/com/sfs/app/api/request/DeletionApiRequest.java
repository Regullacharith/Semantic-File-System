package com.sfs.app.api.request;

import com.sfs.contracts.file.DeletionConfirmation;

public record DeletionApiRequest(String confirmObjectId) {

    public DeletionConfirmation toConfirmation() {
        if (confirmObjectId == null || confirmObjectId.isBlank()) {
            return null;
        }
        return new DeletionConfirmation(confirmObjectId.strip());
    }
}
