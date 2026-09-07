package com.sfs.contracts.file;

import com.sfs.contracts.security.Principal;

import java.util.List;
import java.util.Optional;

public interface FileService {

    List<FileSummary> listFiles();

    Optional<FileSummary> findByObjectId(String objectId);

    FileOperationResult importFile(FileImportRequest request);

    FileOperationResult requestAnalysis(String objectId);

    FileOperationResult softDelete(String objectId, Principal principal);

    FileOperationResult memorize(String objectId, Principal principal);

    FileOperationResult undoDelete(String objectId, Principal principal);

    FileOperationResult purgeRawData(String objectId, Principal principal);
}
