package com.sfs.ui.api;

import com.sfs.app.api.request.ReconstructionApiRequest;
import com.sfs.app.api.response.JobStatusResponse;
import com.sfs.app.service.ReconstructionApplicationService;
import com.sfs.contracts.reconstruction.ReconstructionArtifact;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ReconstructionApiController {

    private final ReconstructionApplicationService reconstructionApplicationService;

    public ReconstructionApiController(
            ReconstructionApplicationService reconstructionApplicationService) {
        this.reconstructionApplicationService = reconstructionApplicationService;
    }

    @PostMapping(value = "/reconstructions", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JobStatusResponse> start(
            @RequestBody ReconstructionApiRequest request) {

        JobStatusResponse job = reconstructionApplicationService.startReconstruction(request);

        if (job.refused()) {
            return ResponseEntity
                    .unprocessableEntity()
                    .body(job);
        }

        return ResponseEntity
                .created(URI.create("/api/v1/reconstructions/" + job.jobId()))
                .body(job);
    }

    @GetMapping("/reconstructions")
    public List<JobStatusResponse> listJobs() {
        return reconstructionApplicationService.listJobs();
    }

    @GetMapping("/reconstructions/{jobId}")
    public JobStatusResponse getJob(@PathVariable String jobId) {
        return reconstructionApplicationService.getJob(jobId);
    }

    @GetMapping("/jobs/{jobId}")
    public JobStatusResponse getUnifiedJob(@PathVariable String jobId) {
        return reconstructionApplicationService.getJob(jobId);
    }

    @GetMapping("/reconstructions/{jobId}/artifact")
    public ResponseEntity<byte[]> getArtifact(@PathVariable String jobId) {
        ReconstructionArtifact artifact = reconstructionApplicationService.getArtifact(jobId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + artifact.fileName() + "\"")
                .contentType(MediaType.parseMediaType("text/plain;charset=UTF-8"))
                .body(artifact.content().getBytes(StandardCharsets.UTF_8));
    }
}
