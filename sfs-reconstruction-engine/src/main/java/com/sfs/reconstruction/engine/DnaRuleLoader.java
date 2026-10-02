package com.sfs.reconstruction.engine;

import com.sfs.contracts.file.FileService;
import com.sfs.core.dna.DnaRepository;

import java.util.Objects;

public final class DnaRuleLoader {

    private final DnaRepository dnaRepository;
    private final FileService fileService;

    public DnaRuleLoader(DnaRepository dnaRepository, FileService fileService) {
        this.dnaRepository = Objects.requireNonNull(dnaRepository,
                "dnaRepository must not be null");
        this.fileService = Objects.requireNonNull(fileService,
                "fileService must not be null");
    }

    public ReconstructedSource load(String objectId) {
        Objects.requireNonNull(objectId, "objectId must not be null");
        return fileService.findByObjectId(objectId)
                .map(file -> dnaRepository.find(objectId)
                        .map(dna -> new ReconstructedSource(file, dna))
                        .orElseThrow(() -> new ReconstructionRefusalException(
                                "This object has no Semantic DNA. Run analysis "
                                        + "before reconstructing.")))
                .orElseThrow(() -> new ReconstructionRefusalException(
                        "No object exists with that Object ID."));
    }
}
