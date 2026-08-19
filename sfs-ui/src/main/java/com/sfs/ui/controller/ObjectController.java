package com.sfs.ui.controller;

import com.sfs.contracts.file.FileService;
import com.sfs.contracts.file.FileSummary;
import com.sfs.contracts.semantic.SemanticDnaView;
import com.sfs.contracts.semantic.SemanticRecordService;
import com.sfs.ui.view.FileViewModel;
import com.sfs.ui.view.NavigationItem;
import com.sfs.ui.view.PageViewModel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;
import java.util.Optional;

/**
 * Object and Semantic DNA inspection.
 */
@Controller
public class ObjectController {

    private static final String VIEW_OBJECTS = "objects";
    private static final String VIEW_OBJECT_DETAIL = "object-detail";

    private static final String ATTR_PAGE = "page";
    private static final String ATTR_OBJECTS = "objects";
    private static final String ATTR_FILE = "file";
    private static final String ATTR_DNA = "dna";
    private static final String ATTR_HAS_DNA = "hasDna";

    private final FileService fileService;
    private final SemanticRecordService semanticRecordService;

    public ObjectController(FileService fileService,
                            SemanticRecordService semanticRecordService) {
        this.fileService = fileService;
        this.semanticRecordService = semanticRecordService;
    }

    @GetMapping("/objects")
    public String listObjects(Model model) {
        List<FileViewModel> objects = fileService.listFiles().stream()
                .map(FileViewModel::from)
                .toList();

        model.addAttribute(ATTR_PAGE, PageViewModel.of("Objects", NavigationItem.OBJECTS));
        model.addAttribute(ATTR_OBJECTS, objects);

        return VIEW_OBJECTS;
    }

    @GetMapping("/objects/{objectId}")
    public String showObject(@PathVariable String objectId, Model model) {

        FileSummary file = fileService.findByObjectId(objectId)
                .orElseThrow(() -> new ObjectNotFoundException(objectId));

        Optional<SemanticDnaView> dna = semanticRecordService.findSemanticDna(objectId);

        model.addAttribute(ATTR_PAGE,
                PageViewModel.of(file.displayName(), NavigationItem.OBJECTS));
        model.addAttribute(ATTR_FILE, FileViewModel.from(file));
        model.addAttribute(ATTR_HAS_DNA, dna.isPresent());
        dna.ifPresent(view -> model.addAttribute(ATTR_DNA, view));

        return VIEW_OBJECT_DETAIL;
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class ObjectNotFoundException extends RuntimeException {

        public ObjectNotFoundException(String objectId) {
            super("No object exists with the supplied Object ID.");
        }
    }
}
