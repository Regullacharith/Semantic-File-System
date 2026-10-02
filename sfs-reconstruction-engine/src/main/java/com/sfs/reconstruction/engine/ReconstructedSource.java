package com.sfs.reconstruction.engine;

import com.sfs.contracts.file.FileSummary;
import com.sfs.core.dna.StoredDna;

import java.util.Objects;

public record ReconstructedSource(FileSummary file, StoredDna dna) {

    public ReconstructedSource {
        Objects.requireNonNull(file, "file must not be null");
        Objects.requireNonNull(dna, "dna must not be null");
    }

    public String dnaVersion() {
        return dna.dna().schemaVersion() + " v" + dna.dna().dnaVersion();
    }
}
