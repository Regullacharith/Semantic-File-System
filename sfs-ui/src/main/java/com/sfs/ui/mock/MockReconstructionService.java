package com.sfs.ui.mock;

import com.sfs.contracts.file.FileService;
import com.sfs.contracts.file.FileSummary;
import com.sfs.contracts.reconstruction.ReconstructionArtifact;
import com.sfs.contracts.reconstruction.ReconstructionJobView;
import com.sfs.contracts.reconstruction.ReconstructionJobView.ConstraintFinding;
import com.sfs.contracts.reconstruction.ReconstructionService;
import com.sfs.contracts.reconstruction.ReconstructionStatus;
import com.sfs.contracts.semantic.SemanticDnaView;
import com.sfs.contracts.semantic.SemanticRecordService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory stand-in for the Reconstruction Engine.
 */
@Service
@Profile("mock")
public class MockReconstructionService implements ReconstructionService {

    private static final String RULES_VERSION = "sfs-rules/0.1";
    private static final String MODEL_VERSION = "deterministic-baseline/0.1";

    private static final String REJECTING_OBJECT_ID = "sfs-obj-0004-b3c4d5e6";

    private final FileService fileService;
    private final SemanticRecordService semanticRecordService;

    private final Map<String, ReconstructionJobView> jobsById = new ConcurrentHashMap<>();
    private final Map<String, ReconstructionArtifact> artifactsByJobId = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public MockReconstructionService(FileService fileService,
                                     SemanticRecordService semanticRecordService) {
        this.fileService = fileService;
        this.semanticRecordService = semanticRecordService;
    }

    @Override
    public ReconstructionJobView requestReconstruction(String objectId) {
        Instant now = Instant.now();
        String jobId = nextJobId();

        Optional<FileSummary> file = fileService.findByObjectId(objectId);
        if (file.isEmpty()) {
            return refuse(jobId, objectId, "unknown", now,
                    "No object exists with that Object ID.");
        }

        Optional<SemanticDnaView> dna = semanticRecordService.findSemanticDna(objectId);
        if (dna.isEmpty()) {
            return refuse(jobId, objectId, file.get().displayName(), now,
                    "This object has no Semantic DNA. Run analysis before reconstructing.");
        }

        return execute(jobId, file.get(), dna.get(), now);
    }

    @Override
    public Optional<ReconstructionJobView> findJob(String jobId) {
        if (jobId == null || jobId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(jobsById.get(jobId));
    }

    @Override
    public List<ReconstructionJobView> listJobs() {
        return jobsById.values().stream()
                .sorted(Comparator.comparing(ReconstructionJobView::requestedAt).reversed())
                .toList();
    }

    @Override
    public Optional<ReconstructionArtifact> findArtifact(String jobId) {
        if (jobId == null || jobId.isBlank()) {
            return Optional.empty();
        }

        return findJob(jobId)
                .filter(ReconstructionJobView::hasArtifact)
                .flatMap(job -> Optional.ofNullable(artifactsByJobId.get(jobId)));
    }

    // -----------------------------------------------------------------------

    private ReconstructionJobView execute(String jobId, FileSummary file,
                                          SemanticDnaView dna, Instant now) {

        String dnaVersion = dna.schemaVersion() + " v" + dna.dnaVersion();
        List<ConstraintFinding> findings = verify(dna);

        boolean violated = findings.stream()
                .anyMatch(f -> f.severity() == ConstraintFinding.Severity.VIOLATION);

        if (violated) {
            ReconstructionJobView job = new ReconstructionJobView(
                    jobId, dna.objectId(), file.displayName(),
                    ReconstructionStatus.REJECTED,
                    dnaVersion, RULES_VERSION, MODEL_VERSION,
                    now, now, null, 0, findings,
                    "Verification rejected the output: a required constraint was violated. "
                            + "No artifact was produced.");
            jobsById.put(jobId, job);
            return job;
        }

        String content = render(dna, dnaVersion);
        String artifactName = artifactName(file.displayName(), jobId);

        ReconstructionJobView job = new ReconstructionJobView(
                jobId, dna.objectId(), file.displayName(),
                ReconstructionStatus.COMPLETED,
                dnaVersion, RULES_VERSION, MODEL_VERSION,
                now, now, artifactName, content.getBytes(java.nio.charset.StandardCharsets.UTF_8).length,
                findings, null);

        jobsById.put(jobId, job);
        artifactsByJobId.put(jobId, new ReconstructionArtifact(
                jobId, artifactName, content, ReconstructionArtifact.TEXT_PLAIN));

        return job;
    }

    private List<ConstraintFinding> verify(SemanticDnaView dna) {
        List<ConstraintFinding> findings = new ArrayList<>();

        long criticalFacts = dna.facts().stream().filter(SemanticDnaView.FactView::critical).count();
        findings.add(new ConstraintFinding(
                criticalFacts > 0 ? ConstraintFinding.Severity.SATISFIED : ConstraintFinding.Severity.WARNING,
                "Required facts",
                criticalFacts > 0
                        ? criticalFacts + " critical fact(s) carried into the reconstruction."
                        : "No critical facts were marked, so none could be guaranteed."));

        findings.add(new ConstraintFinding(
                dna.structure().isEmpty()
                        ? ConstraintFinding.Severity.WARNING : ConstraintFinding.Severity.SATISFIED,
                "Document structure",
                dna.structure().isEmpty()
                        ? "No structure was captured; section order cannot be preserved."
                        : dna.structure().size() + " section(s) preserved in original order."));

        findings.add(new ConstraintFinding(
                dna.entities().isEmpty()
                        ? ConstraintFinding.Severity.WARNING : ConstraintFinding.Severity.SATISFIED,
                "Entity consistency",
                dna.entities().isEmpty()
                        ? "No entities were recorded."
                        : dna.entities().size() + " entity name(s) reproduced without substitution."));

        if (dna.hasProtectedReferences()) {
            findings.add(new ConstraintFinding(
                    ConstraintFinding.Severity.VIOLATION,
                    "Protected values",
                    "This document contains " + dna.protectedReferences().size()
                            + " protected value(s) that cannot be reproduced. Reconstruction "
                            + "would have to invent them, which is not permitted."));
        }

        return findings;
    }

    private String render(SemanticDnaView dna, String dnaVersion) {
        StringBuilder text = new StringBuilder(ReconstructionArtifact.provenanceHeader(
                dna.objectId(), dnaVersion, RULES_VERSION, MODEL_VERSION));

        text.append("SUMMARY\n\n").append(dna.summary()).append("\n\n");

        if (!dna.structure().isEmpty()) {
            text.append("DOCUMENT STRUCTURE\n\n");
            dna.structure().forEach(node ->
                    text.append("  ".repeat(node.level() - 1))
                            .append("- ").append(node.heading()).append('\n'));
            text.append('\n');
        }

        if (!dna.facts().isEmpty()) {
            text.append("RECORDED FACTS\n\n");
            dna.facts().forEach(fact -> text
                    .append(fact.critical() ? "  [critical] " : "  ")
                    .append(fact.statement()).append('\n'));
            text.append('\n');
        }

        if (!dna.relationships().isEmpty()) {
            text.append("RELATIONSHIPS\n\n");
            dna.relationships().forEach(rel -> text
                    .append("  ").append(rel.subject())
                    .append(" -> ").append(rel.type())
                    .append(" -> ").append(rel.object()).append('\n'));
            text.append('\n');
        }

        if (!dna.concepts().isEmpty()) {
            text.append("CONCEPTS\n\n  ")
                    .append(String.join(", ", dna.concepts())).append("\n\n");
        }

        if (!dna.entities().isEmpty()) {
            text.append("ENTITIES\n\n");
            dna.entities().forEach(entity -> text
                    .append("  ").append(entity.name())
                    .append(" (").append(entity.type()).append(")\n"));
            text.append('\n');
        }

        text.append("""
                =============================================================
                End of reconstruction. Content above was regenerated from
                semantic memory and is not a copy of the original document.
                =============================================================
                """);

        return text.toString();
    }

    private ReconstructionJobView refuse(String jobId, String objectId, String sourceName,
                                         Instant now, String reason) {
        ReconstructionJobView job = new ReconstructionJobView(
                jobId, objectId, sourceName,
                ReconstructionStatus.FAILED,
                "unavailable", RULES_VERSION, MODEL_VERSION,
                now, now, null, 0, List.of(), reason);

        jobsById.put(jobId, job);
        return job;
    }

    private static String artifactName(String sourceName, String jobId) {
        String base = sourceName.endsWith(".txt")
                ? sourceName.substring(0, sourceName.length() - 4)
                : sourceName;
        return base + ".reconstructed." + jobId + ".txt";
    }

    private String nextJobId() {
        return "job-%04d".formatted(sequence.incrementAndGet());
    }
}
