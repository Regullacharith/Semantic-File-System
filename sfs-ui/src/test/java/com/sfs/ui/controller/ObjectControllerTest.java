package com.sfs.ui.controller;

import com.sfs.contracts.file.FileService;
import com.sfs.contracts.file.FileStatus;
import com.sfs.contracts.file.FileSummary;
import com.sfs.contracts.semantic.ProtectedReferenceView;
import com.sfs.contracts.semantic.SemanticDnaView;
import com.sfs.contracts.semantic.SemanticRecordService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Verifies the object list and Semantic DNA detail view.
 */
@WebMvcTest(ObjectController.class)
@DisplayName("Object and Semantic DNA view")
class ObjectControllerTest {

    private static final String OBJECT_ID = "sfs-obj-0001-a1b2c3d4";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FileService fileService;

    @MockitoBean
    private SemanticRecordService semanticRecordService;

    private static FileSummary file(FileStatus status) {
        return new FileSummary(OBJECT_ID, "notes.txt", status, 2_048,
                Instant.parse("2026-08-16T10:00:00Z"),
                status == FileStatus.REGISTERED ? null : Instant.parse("2026-08-16T11:00:00Z"));
    }

    private static SemanticDnaView dna(List<ProtectedReferenceView> protectedRefs) {
        return new SemanticDnaView(
                OBJECT_ID, "sfs-dna/0.1", 2,
                "A document summary.",
                List.of("semantic representation"),
                List.of("research"),
                List.of(new SemanticDnaView.EntityView("Semantic DNA", "Concept", 12)),
                List.of(
                        new SemanticDnaView.FactView("A critical claim.", true, 0.94),
                        new SemanticDnaView.FactView("An ordinary claim.", false, 0.80)),
                List.of(new SemanticDnaView.RelationshipView("A", "supersedes", "B")),
                List.of(new SemanticDnaView.StructureNodeView("Introduction", 1, 0)),
                protectedRefs,
                384,
                new SemanticDnaView.FidelityProfileView(0.92, 0.88, "mock-analyzer/0.1"));
    }

    // -------------------------------------------------------------- listing

    @Test
    @DisplayName("lists known objects with their Object IDs")
    void listsObjects() throws Exception {
        given(fileService.listFiles()).willReturn(List.of(file(FileStatus.ANALYZED)));

        mockMvc.perform(get("/objects"))
                .andExpect(status().isOk())
                .andExpect(view().name("objects"))
                .andExpect(content().string(containsString("notes.txt")))
                .andExpect(content().string(containsString(OBJECT_ID)));
    }

    @Test
    @DisplayName("shows an empty state when nothing is registered")
    void showsEmptyState() throws Exception {
        given(fileService.listFiles()).willReturn(List.of());

        mockMvc.perform(get("/objects"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No objects are registered yet")));
    }

    // --------------------------------------------------------------- detail

    @Test
    @DisplayName("renders Semantic DNA for an analyzed object")
    void rendersSemanticDna() throws Exception {
        given(fileService.findByObjectId(OBJECT_ID)).willReturn(Optional.of(file(FileStatus.ANALYZED)));
        given(semanticRecordService.findSemanticDna(OBJECT_ID)).willReturn(Optional.of(dna(List.of())));

        mockMvc.perform(get("/objects/" + OBJECT_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("object-detail"))
                .andExpect(content().string(containsString("A document summary")))
                .andExpect(content().string(containsString("semantic representation")))
                .andExpect(content().string(containsString("sfs-dna/0.1")));
    }

    @Test
    @DisplayName("marks critical facts distinctly from ordinary ones")
    void marksCriticalFacts() throws Exception {
        given(fileService.findByObjectId(OBJECT_ID)).willReturn(Optional.of(file(FileStatus.ANALYZED)));
        given(semanticRecordService.findSemanticDna(OBJECT_ID)).willReturn(Optional.of(dna(List.of())));

        mockMvc.perform(get("/objects/" + OBJECT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("fact--critical")))
                .andExpect(content().string(containsString("A critical claim")))
                .andExpect(content().string(containsString("94% confidence")));
    }

    @Test
    @DisplayName("renders relationships with their direction")
    void rendersDirectionalRelationships() throws Exception {
        given(fileService.findByObjectId(OBJECT_ID)).willReturn(Optional.of(file(FileStatus.ANALYZED)));
        given(semanticRecordService.findSemanticDna(OBJECT_ID)).willReturn(Optional.of(dna(List.of())));

        mockMvc.perform(get("/objects/" + OBJECT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("supersedes")))
                .andExpect(content().string(containsString("relationship__subject")));
    }

    @Test
    @DisplayName("treats a not-yet-analyzed object as a normal state, not an error")
    void handlesMissingDna() throws Exception {
        given(fileService.findByObjectId(OBJECT_ID)).willReturn(Optional.of(file(FileStatus.REGISTERED)));
        given(semanticRecordService.findSemanticDna(OBJECT_ID)).willReturn(Optional.empty());

        mockMvc.perform(get("/objects/" + OBJECT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No Semantic DNA yet")));
    }

    @Test
    @DisplayName("states that semantic memory survives raw deletion")
    void explainsMemorizedRecord() throws Exception {
        given(fileService.findByObjectId(OBJECT_ID)).willReturn(Optional.of(file(FileStatus.MEMORIZED)));
        given(semanticRecordService.findSemanticDna(OBJECT_ID)).willReturn(Optional.of(dna(List.of())));

        mockMvc.perform(get("/objects/" + OBJECT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("raw file has been deleted")));
    }

    @Test
    @DisplayName("returns 404 for an unknown Object ID")
    void unknownObjectReturns404() throws Exception {
        given(fileService.findByObjectId(any())).willReturn(Optional.empty());

        mockMvc.perform(get("/objects/sfs-obj-9999-ffffffff"))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------- security

    @Test
    @DisplayName("describes a protected value by role and never shows the value")
    void describesProtectedValueWithoutExposingIt() throws Exception {
        given(fileService.findByObjectId(OBJECT_ID)).willReturn(Optional.of(file(FileStatus.ANALYZED)));
        given(semanticRecordService.findSemanticDna(OBJECT_ID)).willReturn(Optional.of(dna(List.of(
                new ProtectedReferenceView("ref-7f3a9c21",
                        ProtectedReferenceView.SensitiveType.API_KEY,
                        "Authenticates to the payment gateway",
                        "Credentials section, line 12", true)))));

        mockMvc.perform(get("/objects/" + OBJECT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("API key")))
                .andExpect(content().string(containsString("Authenticates to the payment gateway")))
                .andExpect(content().string(containsString("ref-7f3a9c21")))
                .andExpect(content().string(not(containsString("sk-"))));
    }

    @Test
    @DisplayName("states that a password is non-reversible rather than offering to reveal it")
    void marksPasswordNonReversible() throws Exception {
        given(fileService.findByObjectId(OBJECT_ID)).willReturn(Optional.of(file(FileStatus.ANALYZED)));
        given(semanticRecordService.findSemanticDna(OBJECT_ID)).willReturn(Optional.of(dna(List.of(
                new ProtectedReferenceView("ref-2b8e4d05",
                        ProtectedReferenceView.SensitiveType.PASSWORD,
                        "Database account password",
                        "Credentials section, line 18", false)))));

        mockMvc.perform(get("/objects/" + OBJECT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Non-reversible by policy")))
                .andExpect(content().string(containsString("cannot be recovered")));
    }

    @Test
    @DisplayName("reports embedding dimensionality without rendering the vector")
    void doesNotRenderEmbeddingVector() throws Exception {
        given(fileService.findByObjectId(OBJECT_ID)).willReturn(Optional.of(file(FileStatus.ANALYZED)));
        given(semanticRecordService.findSemanticDna(OBJECT_ID)).willReturn(Optional.of(dna(List.of())));

        mockMvc.perform(get("/objects/" + OBJECT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("384 dimensions (not shown)")));
    }

    @Test
    @DisplayName("does not present representation quality as reconstruction fidelity")
    void distinguishesRepresentationQualityFromFidelity() throws Exception {
        given(fileService.findByObjectId(OBJECT_ID)).willReturn(Optional.of(file(FileStatus.ANALYZED)));
        given(semanticRecordService.findSemanticDna(OBJECT_ID)).willReturn(Optional.of(dna(List.of())));

        mockMvc.perform(get("/objects/" + OBJECT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("They are not reconstruction fidelity")));
    }

    // ------------------------------------------------------------ read-only

    @Test
    @DisplayName("exposes no route that mutates semantic memory")
    void exposesNoMutatingRoute() throws Exception {
        given(fileService.findByObjectId(OBJECT_ID)).willReturn(Optional.of(file(FileStatus.ANALYZED)));
        given(semanticRecordService.findSemanticDna(OBJECT_ID)).willReturn(Optional.of(dna(List.of())));

        mockMvc.perform(post("/objects/" + OBJECT_ID))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("offers reconstruction as an explicit POST, never as a link")
    void offersReconstructionOnlyAsExplicitPost() throws Exception {
        given(fileService.findByObjectId(OBJECT_ID)).willReturn(Optional.of(file(FileStatus.ANALYZED)));
        given(semanticRecordService.findSemanticDna(OBJECT_ID)).willReturn(Optional.of(dna(List.of())));

        mockMvc.perform(get("/objects/" + OBJECT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "<form action=\"/reconstruction/" + OBJECT_ID + "\" method=\"post\">")))
                .andExpect(content().string(not(containsString(
                        "href=\"/reconstruction/" + OBJECT_ID + "\""))));
    }

    @Test
    @DisplayName("offers no reconstruction control for an object without Semantic DNA")
    void offersNoReconstructionWithoutDna() throws Exception {
        given(fileService.findByObjectId(OBJECT_ID)).willReturn(Optional.of(file(FileStatus.REGISTERED)));
        given(semanticRecordService.findSemanticDna(OBJECT_ID)).willReturn(Optional.empty());

        mockMvc.perform(get("/objects/" + OBJECT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Reconstruct this object"))));
    }
}
