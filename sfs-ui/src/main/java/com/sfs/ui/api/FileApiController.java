package com.sfs.ui.api;

import com.sfs.app.api.request.DeletionApiRequest;
import com.sfs.app.api.request.FileImportApiRequest;
import com.sfs.app.api.response.FileResponse;
import com.sfs.app.api.response.OperationResponse;
import com.sfs.app.api.response.SemanticRecordResponse;
import com.sfs.app.service.FileApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class FileApiController {

    private static final long MAX_UPLOAD_BYTES = 5L * 1024 * 1024;

    static final String AUTH_HEADER = "X-SFS-Credential";

    private final FileApplicationService fileApplicationService;

    public FileApiController(FileApplicationService fileApplicationService) {
        this.fileApplicationService = fileApplicationService;
    }

    @GetMapping("/files")
    public List<FileResponse> listFiles() {
        return fileApplicationService.listFiles();
    }

    @GetMapping("/files/{objectId}")
    public FileResponse getFile(@PathVariable String objectId) {
        return fileApplicationService.getFile(objectId);
    }

    @PostMapping(value = "/files", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OperationResponse> importJson(@RequestBody FileImportApiRequest request) {
        return created(fileApplicationService.importFile(request));
    }

    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<OperationResponse> importMultipart(
            @RequestParam("file") MultipartFile upload) {

        if (upload == null || upload.isEmpty()) {
            throw new ApiExceptions.PayloadRejected("No file was supplied.");
        }
        if (upload.getSize() > MAX_UPLOAD_BYTES) {
            throw new ApiExceptions.PayloadTooLarge(
                    "File exceeds the %d MB limit for V1 text import."
                            .formatted(MAX_UPLOAD_BYTES / (1024 * 1024)));
        }

        String fileName = upload.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            throw new ApiExceptions.PayloadRejected("The uploaded file has no name.");
        }

        final String content;
        try {
            content = decodeUtf8(upload.getBytes());
        } catch (CharacterCodingException e) {
            throw new ApiExceptions.UnsupportedPayload(
                    "The file is not valid UTF-8 text. V1 supports text files only.");
        } catch (IOException e) {
            throw new ApiExceptions.PayloadRejected("The uploaded file could not be read.");
        }

        return created(fileApplicationService.importFile(
                new FileImportApiRequest(fileName, content, upload.getContentType())));
    }

    @PostMapping("/files/{objectId}/analyze")
    public OperationResponse analyze(@PathVariable String objectId) {
        return fileApplicationService.analyze(objectId);
    }

    @DeleteMapping("/files/{objectId}")
    public OperationResponse softDelete(
            @PathVariable String objectId,
            @RequestHeader(name = AUTH_HEADER, required = false) String credential,
            @RequestBody(required = false) DeletionApiRequest request) {

        return fileApplicationService.softDelete(
                objectId,
                credential,
                request == null ? null : request.toConfirmation());
    }

    @PostMapping("/files/{objectId}/undo-delete")
    public OperationResponse undoDelete(
            @PathVariable String objectId,
            @RequestHeader(name = AUTH_HEADER, required = false) String credential) {

        return fileApplicationService.undoDelete(objectId, credential);
    }

    @PostMapping("/files/{objectId}/purge")
    public OperationResponse purgeRawData(
            @PathVariable String objectId,
            @RequestHeader(name = AUTH_HEADER, required = false) String credential,
            @RequestBody(required = false) DeletionApiRequest request) {

        return fileApplicationService.purgeRawData(
                objectId,
                credential,
                request == null ? null : request.toConfirmation());
    }

    @GetMapping("/objects/{objectId}/dna")
    public SemanticRecordResponse getSemanticRecord(@PathVariable String objectId) {
        return fileApplicationService.getSemanticRecord(objectId);
    }

    private ResponseEntity<OperationResponse> created(OperationResponse response) {
        return ResponseEntity
                .created(URI.create("/api/v1/files/" + response.objectId()))
                .body(response);
    }

    private static String decodeUtf8(byte[] bytes) throws CharacterCodingException {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);

        return decoder.decode(ByteBuffer.wrap(bytes)).toString();
    }
}
