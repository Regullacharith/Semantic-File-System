package com.sfs.contracts.file;

import java.util.List;
import java.util.Optional;

/**
 * Application-facing contract for file lifecycle operations.
 */
public interface FileService {

    
    List<FileSummary> listFiles();

  
    Optional<FileSummary> findByObjectId(String objectId);

   
    FileOperationResult importFile(FileImportRequest request);

  
    FileOperationResult requestAnalysis(String objectId);


    FileOperationResult requestSemanticDeletion(String objectId);
}
