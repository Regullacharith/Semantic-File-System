package com.sfs.ui.controller;

import com.sfs.contracts.reconstruction.ReconstructionArtifact;
import com.sfs.contracts.reconstruction.ReconstructionJobView;
import com.sfs.contracts.reconstruction.ReconstructionService;
import com.sfs.ui.view.NavigationItem;
import com.sfs.ui.view.PageViewModel;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;

/**
 * get reconstruction flow.
 */
@Controller
public class ReconstructionController {

    private static final String VIEW_RECONSTRUCTION = "reconstruction";
    private static final String VIEW_JOB = "reconstruction-job";
    private static final String REDIRECT_JOB = "redirect:/reconstruction/job/";

    private static final String ATTR_PAGE = "page";
    private static final String ATTR_JOBS = "jobs";
    private static final String ATTR_JOB = "job";
    private static final String ATTR_MESSAGE = "resultMessage";
    private static final String ATTR_SUCCESS = "resultSuccess";

    private final ReconstructionService reconstructionService;

    public ReconstructionController(ReconstructionService reconstructionService) {
        this.reconstructionService = reconstructionService;
    }

    @GetMapping("/reconstruction")
    public String listJobs(Model model) {
        model.addAttribute(ATTR_PAGE,
                PageViewModel.of("Reconstruction", NavigationItem.RECONSTRUCTION));
        model.addAttribute(ATTR_JOBS, reconstructionService.listJobs());

        return VIEW_RECONSTRUCTION;
    }

    @PostMapping("/reconstruction/{objectId}")
    public String reconstruct(@PathVariable String objectId,
                              RedirectAttributes redirectAttributes) {

        ReconstructionJobView job = reconstructionService.requestReconstruction(objectId);

        redirectAttributes.addFlashAttribute(ATTR_SUCCESS,
                job.status().isArtifactAvailable());
        redirectAttributes.addFlashAttribute(ATTR_MESSAGE,
                job.failureReason() != null
                        ? job.failureReason()
                        : "Reconstruction completed. The artifact is a new semantically "
                          + "equivalent document, not the original file.");

        return REDIRECT_JOB + job.jobId();
    }

    @GetMapping("/reconstruction/job/{jobId}")
    public String showJob(@PathVariable String jobId, Model model) {
        ReconstructionJobView job = reconstructionService.findJob(jobId)
                .orElseThrow(JobNotFoundException::new);

        model.addAttribute(ATTR_PAGE,
                PageViewModel.of("Reconstruction job", NavigationItem.RECONSTRUCTION));
        model.addAttribute(ATTR_JOB, job);

        return VIEW_JOB;
    }

    @GetMapping("/reconstruction/job/{jobId}/artifact")
    public ResponseEntity<Resource> downloadArtifact(@PathVariable String jobId) {
        ReconstructionArtifact artifact = reconstructionService.findArtifact(jobId)
                .orElseThrow(JobNotFoundException::new);

        byte[] bytes = artifact.content().getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(artifact.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + artifact.fileName() + "\"")
                .contentLength(bytes.length)
                .body(new ByteArrayResource(bytes));
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class JobNotFoundException extends RuntimeException {

        public JobNotFoundException() {
            super("No reconstruction job exists with that identifier.");
        }
    }
}
