package com.sfs.contracts.file;

import java.util.List;
import java.util.Optional;

public interface FileService {

    Optional<FileSummary> findByObjectId(String objectId);

    FileOperationResult importFile(FileImportRequest request);

    FileOperationResult requestAnalysis(String objectId);

    FileOperationResult softDelete(String objectId);

    FileOperationResult undoDelete(String objectId);

    FileOperationResult purgeRawData(String objectId);
}
