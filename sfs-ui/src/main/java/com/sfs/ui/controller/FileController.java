package com.sfs.ui.controller;

import com.sfs.contracts.file.FileImportRequest;
import com.sfs.contracts.file.FileOperationResult;
import com.sfs.contracts.file.FileService;
import com.sfs.contracts.security.Capability;
import com.sfs.contracts.security.Principal;
import com.sfs.ui.view.FileViewModel;
import com.sfs.ui.view.NavigationItem;
import com.sfs.ui.view.PageViewModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

@Controller
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    private static final String VIEW_FILES = "files";
    private static final String REDIRECT_FILES = "redirect:/files";

    private static final String ATTR_PAGE = "page";
    private static final String ATTR_FILES = "files";
    private static final String ATTR_RESULT_MESSAGE = "resultMessage";
    private static final String ATTR_RESULT_SUCCESS = "resultSuccess";

    private static final long MAX_UPLOAD_BYTES = 5L * 1024 * 1024;

    private static final Principal UI_PRINCIPAL =
            new Principal("ui-dev", "Local development interface",
                    Set.of(Capability.DELETE_RAW, Capability.UNDO_DELETE));

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping("/files")
    public String listFiles(Model model) {
        List<FileViewModel> files = fileService.listFiles().stream()
                .map(FileViewModel::from)
                .toList();

        model.addAttribute(ATTR_PAGE, PageViewModel.of("Files", NavigationItem.FILES));
        model.addAttribute(ATTR_FILES, files);

        return VIEW_FILES;
    }

    @PostMapping("/files/import")
    public String importFile(@RequestParam("file") MultipartFile upload,
                             RedirectAttributes redirectAttributes) {

        if (upload == null || upload.isEmpty()) {
            return redirectWith(redirectAttributes,
                    FileOperationResult.failure("No file was selected."));
        }

        if (upload.getSize() > MAX_UPLOAD_BYTES) {
            return redirectWith(redirectAttributes, FileOperationResult.failure(
                    "File exceeds the %d MB limit for V1 text import."
                            .formatted(MAX_UPLOAD_BYTES / (1024 * 1024))));
        }

        String originalName = upload.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            return redirectWith(redirectAttributes,
                    FileOperationResult.failure("The uploaded file has no name."));
        }

        final String content;
        try {
            content = decodeUtf8(upload.getBytes());
        } catch (MalformedInputException e) {
            return redirectWith(redirectAttributes, FileOperationResult.failure(
                    "The file is not valid UTF-8 text. V1 supports text files only."));
        } catch (IOException e) {
            log.warn("Failed to read uploaded file '{}': {}",
                    sanitizeForLog(originalName), e.getClass().getSimpleName());
            return redirectWith(redirectAttributes,
                    FileOperationResult.failure("The uploaded file could not be read."));
        }

        FileOperationResult result;
        try {
            result = fileService.importFile(
                    new FileImportRequest(originalName, content, upload.getContentType()));
        } catch (IllegalArgumentException e) {
            result = FileOperationResult.failure("Import rejected: " + e.getMessage());
        }

        return redirectWith(redirectAttributes, result);
    }

    @PostMapping("/files/{objectId}/analyze")
    public String analyzeFile(@PathVariable String objectId,
                              RedirectAttributes redirectAttributes) {
        return redirectWith(redirectAttributes, fileService.requestAnalysis(objectId));
    }

    @PostMapping("/files/{objectId}/delete")
    public String softDelete(@PathVariable String objectId,
                             RedirectAttributes redirectAttributes) {
        return redirectWith(redirectAttributes, fileService.softDelete(objectId, UI_PRINCIPAL));
    }

    @PostMapping("/files/{objectId}/memorize")
    public String memorize(@PathVariable String objectId,
                           RedirectAttributes redirectAttributes) {
        return redirectWith(redirectAttributes, fileService.memorize(objectId, UI_PRINCIPAL));
    }

    @PostMapping("/files/{objectId}/undo-delete")
    public String undoDelete(@PathVariable String objectId,
                             RedirectAttributes redirectAttributes) {
        return redirectWith(redirectAttributes, fileService.undoDelete(objectId, UI_PRINCIPAL));
    }

    private String redirectWith(RedirectAttributes redirectAttributes, FileOperationResult result) {
        redirectAttributes.addFlashAttribute(ATTR_RESULT_MESSAGE, result.message());
        redirectAttributes.addFlashAttribute(ATTR_RESULT_SUCCESS, result.successful());
        return REDIRECT_FILES;
    }

    private static String decodeUtf8(byte[] bytes) throws IOException {
        var decoder = StandardCharsets.UTF_8.newDecoder();
        return decoder.decode(java.nio.ByteBuffer.wrap(bytes)).toString();
    }

    private static String sanitizeForLog(String value) {
        return value.replaceAll("[\\p{Cntrl}]", "_");
    }
}
