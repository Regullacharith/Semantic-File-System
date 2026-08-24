package com.sfs.app.service;

import com.sfs.app.api.request.FileImportApiRequest;
import com.sfs.app.api.response.FileResponse;
import com.sfs.app.api.response.OperationResponse;
import com.sfs.app.api.response.SemanticRecordResponse;
import com.sfs.contracts.file.FileImportRequest;
import com.sfs.contracts.file.FileOperationResult;
import com.sfs.contracts.file.FileService;
import com.sfs.contracts.file.FileSummary;
import com.sfs.contracts.file.DeletionConfirmation;
import com.sfs.contracts.security.AuthenticationService;
import com.sfs.contracts.security.AuthorizationService;
import com.sfs.contracts.security.Capability;
import com.sfs.contracts.security.Principal;
import com.sfs.contracts.semantic.SemanticRecordService;
import com.sfs.core.identity.ObjectId;

import java.util.List;
import java.util.Objects;

public class FileApplicationService {

    private final FileService fileService;
    private final SemanticRecordService semanticRecordService;
    private final AuthenticationService authenticationService;
    private final AuthorizationService authorizationService;

    public FileApplicationService(FileService fileService,
                                  SemanticRecordService semanticRecordService,
                                  AuthenticationService authenticationService,
                                  AuthorizationService authorizationService) {
        this.fileService = Objects.requireNonNull(fileService, "fileService must not be null");
        this.semanticRecordService =
                Objects.requireNonNull(semanticRecordService, "semanticRecordService must not be null");
        this.authenticationService = Objects.requireNonNull(
                authenticationService, "authenticationService must not be null");
        this.authorizationService = Objects.requireNonNull(
                authorizationService, "authorizationService must not be null");
    }

    public List<FileResponse> listFiles() {
        return fileService.listFiles().stream().map(FileResponse::from).toList();
    }

    public FileResponse getFile(String objectId) {
        return FileResponse.from(requireFile(objectId));
    }

    public OperationResponse importFile(FileImportApiRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        FileOperationResult result = fileService.importFile(new FileImportRequest(
                request.fileName(), request.content(), request.contentType()));

        if (!result.successful()) {
            throw ApplicationException.validationFailed(result.message());
        }

        return OperationResponse.from(result);
    }

    public OperationResponse analyze(String objectId) {
        FileSummary file = requireFile(objectId);

        if (!file.status().allowsAnalysis()) {
            throw ApplicationException.invalidState(
                    "Analysis is not permitted while the file is " + file.status().getLabel() + ".");
        }

        FileOperationResult result = fileService.requestAnalysis(file.objectId());

        if (!result.successful()) {
            throw ApplicationException.invalidState(result.message());
        }

        return OperationResponse.from(result);
    }

    public OperationResponse softDelete(String objectId,
                                        String credential,
                                        DeletionConfirmation confirmation) {

        Principal principal = requirePrincipal(credential);
        requireCapability(principal, Capability.DELETE_RAW);
        requireConfirmation(confirmation, objectId, "delete");

        FileSummary file = requireFile(objectId);

        if (!file.status().allowsSoftDeletion()) {
            throw ApplicationException.invalidState(
                    "Deletion requires an analyzed object. The object is currently "
                            + file.status().getLabel() + ".");
        }

        return execute(fileService.softDelete(file.objectId()));
    }

    public OperationResponse undoDelete(String objectId, String credential) {
        Principal principal = requirePrincipal(credential);
        requireCapability(principal, Capability.UNDO_DELETE);

        FileSummary file = requireFile(objectId);

        if (!file.status().allowsUndoDelete()) {
            throw ApplicationException.invalidState(
                    "Only a deleted object can be restored. The object is currently "
                            + file.status().getLabel() + ".");
        }

        return execute(fileService.undoDelete(file.objectId()));
    }

    public OperationResponse purgeRawData(String objectId,
                                          String credential,
                                          DeletionConfirmation confirmation) {

        Principal principal = requirePrincipal(credential);
        requireCapability(principal, Capability.PURGE_RAW);
        requireConfirmation(confirmation, objectId, "purge");

        FileSummary file = requireFile(objectId);

        if (!file.status().allowsPurge()) {
            throw ApplicationException.invalidState(
                    "Purge requires a deleted object, so raw bytes are never released in a "
                            + "single step. The object is currently "
                            + file.status().getLabel() + ".");
        }

        return execute(fileService.purgeRawData(file.objectId()));
    }

    private Principal requirePrincipal(String credential) {
        return authenticationService.authenticate(credential)
                .orElseThrow(ApplicationException::authenticationRequired);
    }

    private void requireCapability(Principal principal, Capability capability) {
        if (!authorizationService.isPermitted(principal, capability)) {
            throw ApplicationException.notPermitted(capability.name());
        }
    }

    private void requireConfirmation(DeletionConfirmation confirmation,
                                     String objectId,
                                     String operation) {

        if (confirmation == null) {
            throw ApplicationException.confirmationRequired(operation);
        }
        if (!confirmation.confirms(objectId)) {
            throw ApplicationException.confirmationMismatch();
        }
    }

    private OperationResponse execute(FileOperationResult result) {
        if (!result.successful()) {
            throw ApplicationException.invalidState(result.message());
        }
        return OperationResponse.from(result);
    }

    public SemanticRecordResponse getSemanticRecord(String objectId) {
        FileSummary file = requireFile(objectId);

        return semanticRecordService.findSemanticDna(file.objectId())
                .map(SemanticRecordResponse::from)
                .orElseGet(() -> SemanticRecordResponse.absent(
                        file.objectId(),
                        "No Semantic DNA exists yet. The file is " + file.status().getLabel()
                                + "; analysis produces the semantic record."));
    }

    private FileSummary requireFile(String objectId) {
        String validated = ObjectId.of(objectId).value();

        return fileService.findByObjectId(validated)
                .orElseThrow(() -> ApplicationException.fileNotFound(validated));
    }
}
