package com.sfs.reconstruction;

import java.util.List;
import java.util.Objects;

public record ModelOutput(String modelId, String draftText, List<String> notes) {

    public ModelOutput {
        Objects.requireNonNull(modelId, "modelId must not be null");
        Objects.requireNonNull(draftText, "draftText must not be null");
        Objects.requireNonNull(notes, "notes must not be null");
        if (modelId.isBlank()) {
            throw new IllegalArgumentException("modelId must not be blank");
        }
        notes = List.copyOf(notes);
    }
}
