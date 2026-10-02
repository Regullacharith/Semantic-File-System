package com.sfs.reconstruction.engine;

import com.sfs.contracts.reconstruction.ReconstructionArtifact;

import java.util.Objects;

public final class ReconstructionPostProcessor {

    public String label(String objectId, String dnaVersion, String rulesVersion,
                        String modelId, String draft) {
        Objects.requireNonNull(objectId, "objectId must not be null");
        Objects.requireNonNull(dnaVersion, "dnaVersion must not be null");
        Objects.requireNonNull(rulesVersion, "rulesVersion must not be null");
        Objects.requireNonNull(modelId, "modelId must not be null");
        Objects.requireNonNull(draft, "draft must not be null");

        String body = draft.strip();
        if (body.isEmpty()) {
            throw new IllegalStateException(
                    "the model produced an empty draft; refusing to label it");
        }
        StringBuilder text = new StringBuilder();
        text.append(ReconstructionArtifact.provenanceHeader(
                objectId, dnaVersion, rulesVersion, modelId));
        text.append(body).append("\n\n");
        text.append("""
                =============================================================
                End of reconstruction. The content above is a reconstructed,
                estimated regeneration from semantic memory, not a copy of
                the original document and not a byte-for-byte recovery.
                =============================================================
                """);
        return text.toString();
    }
}
