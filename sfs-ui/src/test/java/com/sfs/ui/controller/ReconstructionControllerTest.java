package com.sfs.ui.controller;

import com.sfs.contracts.reconstruction.ReconstructionArtifact;
import com.sfs.contracts.reconstruction.ReconstructionJobView;
import com.sfs.contracts.reconstruction.ReconstructionJobView.ConstraintFinding;
import com.sfs.contracts.reconstruction.ReconstructionService;
import com.sfs.contracts.reconstruction.ReconstructionStatus;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Verifies the single-click reconstruction flow.
 */
@WebMvcTest(ReconstructionController.class)
@DisplayName("Reconstruction flow")
class ReconstructionControllerTest {

    private static final String OBJECT_ID = "sfs-obj-0001-a1b2c3d4";
    private static final String JOB_ID = "job-0001";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReconstructionService reconstructionService;

    private static ReconstructionJobView completed() {
        return new ReconstructionJobView(
                JOB_ID, OBJECT_ID, "notes.txt", ReconstructionStatus.COMPLETED,
                "sfs-dna/0.1 v2", "sfs-rules/0.1", "deterministic-baseline/0.1",
                Instant.now(), Instant.now(),
                "notes.reconstructed.job-0001.txt", 512,
                List.of(new ConstraintFinding(ConstraintFinding.Severity.SATISFIED,
                        "Required facts", "2 critical fact(s) carried into the reconstruction.")),
                null);
    }

    private static ReconstructionJobView rejected() {
        return new ReconstructionJobView(
                JOB_ID, OBJECT_ID, "config.txt", ReconstructionStatus.REJECTED,
                "sfs-dna/0.1 v1", "sfs-rules/0.1", "deterministic-baseline/0.1",
                Instant.now(), Instant.now(), null, 0,
                List.of(new ConstraintFinding(ConstraintFinding.Severity.VIOLATION,
                        "Protected values", "Reconstruction would have to invent them.")),
                "Verification rejected the output: a required constraint was violated.");
    }

    // ------------------------------------------------------------ job list

    @Test
    @DisplayName("lists jobs without starting one")
    void listsJobsWithoutStartingOne() throws Exception {
        given(reconstructionService.listJobs()).willReturn(List.of(completed()));

        mockMvc.perform(get("/reconstruction"))
                .andExpect(status().isOk())
                .andExpect(view().name("reconstruction"))
                .andExpect(content().string(containsString(JOB_ID)));

        verify(reconstructionService, never()).requestReconstruction(any());
    }

    @Test
    @DisplayName("shows an empty state when no job has been requested")
    void showsEmptyState() throws Exception {
        given(reconstructionService.listJobs()).willReturn(List.of());

        mockMvc.perform(get("/reconstruction"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No reconstruction jobs")));
    }

    // ------------------------------------------------------- explicit start

    @Test
    @DisplayName("starts reconstruction on an explicit POST and redirects to the job")
    void startsReconstructionOnPost() throws Exception {
        given(reconstructionService.requestReconstruction(OBJECT_ID)).willReturn(completed());

        mockMvc.perform(post("/reconstruction/" + OBJECT_ID))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reconstruction/job/" + JOB_ID))
                .andExpect(flash().attribute("resultSuccess", true));

        verify(reconstructionService).requestReconstruction(OBJECT_ID);
    }

    @Test
    @DisplayName("does not start reconstruction over GET")
    void doesNotStartOverGet() throws Exception {
        mockMvc.perform(get("/reconstruction/" + OBJECT_ID))
                .andExpect(status().is4xxClientError());

        verify(reconstructionService, never()).requestReconstruction(any());
    }

    @Test
    @DisplayName("reports a refused reconstruction as an explicit failure")
    void reportsRefusalExplicitly() throws Exception {
        given(reconstructionService.requestReconstruction(OBJECT_ID)).willReturn(rejected());

        mockMvc.perform(post("/reconstruction/" + OBJECT_ID))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("resultSuccess", false))
                .andExpect(flash().attribute("resultMessage", containsString("rejected")));
    }

    // ------------------------------------------------------------ job view

    @Test
    @DisplayName("shows provenance so a result is auditable")
    void showsProvenance() throws Exception {
        given(reconstructionService.findJob(JOB_ID)).willReturn(Optional.of(completed()));

        mockMvc.perform(get("/reconstruction/job/" + JOB_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("reconstruction-job"))
                .andExpect(content().string(containsString("sfs-dna/0.1 v2")))
                .andExpect(content().string(containsString("sfs-rules/0.1")))
                .andExpect(content().string(containsString("deterministic-baseline/0.1")));
    }

    @Test
    @DisplayName("shows verification findings")
    void showsVerificationFindings() throws Exception {
        given(reconstructionService.findJob(JOB_ID)).willReturn(Optional.of(completed()));

        mockMvc.perform(get("/reconstruction/job/" + JOB_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Required facts")))
                .andExpect(content().string(containsString("2 critical fact(s)")));
    }

    @Test
    @DisplayName("states plainly that the artifact is not the original")
    void statesArtifactIsNotOriginal() throws Exception {
        given(reconstructionService.findJob(JOB_ID)).willReturn(Optional.of(completed()));

        mockMvc.perform(get("/reconstruction/job/" + JOB_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("regenerated from semantic memory")));
    }

    @Test
    @DisplayName("offers no download for a rejected job")
    void offersNoDownloadForRejectedJob() throws Exception {
        given(reconstructionService.findJob(JOB_ID)).willReturn(Optional.of(rejected()));

        mockMvc.perform(get("/reconstruction/job/" + JOB_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No artifact was produced")))
                .andExpect(content().string(containsString("Violation")))
                .andExpect(content().string(not(containsString("/artifact"))));
    }

    @Test
    @DisplayName("returns 404 for an unknown job")
    void unknownJobReturns404() throws Exception {
        given(reconstructionService.findJob(any())).willReturn(Optional.empty());

        mockMvc.perform(get("/reconstruction/job/job-9999"))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------ download

    @Test
    @DisplayName("downloads the artifact as an attachment with a reconstructed name")
    void downloadsArtifact() throws Exception {
        String body = ReconstructionArtifact.provenanceHeader(
                OBJECT_ID, "sfs-dna/0.1 v2", "sfs-rules/0.1", "deterministic-baseline/0.1")
                + "SUMMARY\n\nA summary.\n";

        given(reconstructionService.findArtifact(JOB_ID)).willReturn(Optional.of(
                new ReconstructionArtifact(JOB_ID, "notes.reconstructed.job-0001.txt",
                        body, ReconstructionArtifact.TEXT_PLAIN)));

        mockMvc.perform(get("/reconstruction/job/" + JOB_ID + "/artifact"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        containsString("attachment; filename=\"notes.reconstructed.job-0001.txt\"")))
                .andExpect(content().string(containsString("NOT THE ORIGINAL FILE")))
                .andExpect(content().string(containsString(OBJECT_ID)));
    }

    @Test
    @DisplayName("returns 404 when no artifact exists for a job")
    void missingArtifactReturns404() throws Exception {
        given(reconstructionService.findArtifact(any())).willReturn(Optional.empty());

        mockMvc.perform(get("/reconstruction/job/" + JOB_ID + "/artifact"))
                .andExpect(status().isNotFound());
    }
}
