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

@Service
@Profile("mock")
public class MockReconstructionService implements ReconstructionService {

    private static final String LEGACY_RULES_VERSION = "sfs-rules/0.1";
    private static final String MODEL_VERSION = "deterministic-baseline/0.1";

    private static final String REJECTING_OBJECT_ID = "sfs-obj-0004-b3c4d5e6";

    private final FileService fileService;
    private final SemanticRecordService semanticRecordService;
    private final com.sfs.engine.record.InMemorySemanticRecordStore semanticRecordStore;
    private final com.sfs.core.rules.ReconstructionPlanner reconstructionPlanner;
    private final com.sfs.core.rules.PlanChecker planChecker = new com.sfs.core.rules.PlanChecker();

    private final Map<String, ReconstructionJobView> jobsById = new ConcurrentHashMap<>();
    private final Map<String, ReconstructionArtifact> artifactsByJobId = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public MockReconstructionService(FileService fileService,
                                     SemanticRecordService semanticRecordService,
                                     com.sfs.engine.record.InMemorySemanticRecordStore semanticRecordStore,
                                     com.sfs.core.rules.ReconstructionPlanner reconstructionPlanner) {
        this.fileService = fileService;
        this.semanticRecordService = semanticRecordService;
        this.semanticRecordStore = semanticRecordStore;
        this.reconstructionPlanner = reconstructionPlanner;
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
        com.sfs.core.rules.ReconstructionPlan plan = semanticRecordStore
                .findStored(dna.objectId())
                .map(stored -> reconstructionPlanner.plan(stored.dna()))
                .orElse(null);
        String rulesVersion = plan == null ? LEGACY_RULES_VERSION : plan.rulesVersion();
        List<ConstraintFinding> findings = verify(dna, plan);

        boolean violated = findings.stream()
                .anyMatch(f -> f.severity() == ConstraintFinding.Severity.VIOLATION);

        if (violated) {
            ReconstructionJobView job = new ReconstructionJobView(
                    jobId, dna.objectId(), file.displayName(),
                    ReconstructionStatus.REJECTED,
                    dnaVersion, rulesVersion, MODEL_VERSION,
                    now, now, null, 0, findings,
                    "Verification rejected the output: a required constraint was violated. "
                            + "No artifact was produced.");
            jobsById.put(jobId, job);
            return job;
        }

        String content = render(dna, dnaVersion, rulesVersion);
        String artifactName = artifactName(file.displayName(), jobId);

        ReconstructionJobView job = new ReconstructionJobView(
                jobId, dna.objectId(), file.displayName(),
                ReconstructionStatus.COMPLETED,
                dnaVersion, rulesVersion, MODEL_VERSION,
                now, now, artifactName, content.getBytes(java.nio.charset.StandardCharsets.UTF_8).length,
                findings, null);

        jobsById.put(jobId, job);
        artifactsByJobId.put(jobId, new ReconstructionArtifact(
                jobId, artifactName, content, ReconstructionArtifact.TEXT_PLAIN));

        return job;
    }

    private List<ConstraintFinding> verify(SemanticDnaView dna,
                                            com.sfs.core.rules.ReconstructionPlan plan) {
        List<ConstraintFinding> findings = new ArrayList<>();
        if (plan != null) {
            findings.addAll(planFindings(plan, dna));
        }

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

    private List<ConstraintFinding> planFindings(com.sfs.core.rules.ReconstructionPlan plan,
                                                 SemanticDnaView dna) {
        List<ConstraintFinding> findings = new ArrayList<>();
        String content = render(dna, dna.schemaVersion() + " v" + dna.dnaVersion(),
                plan.rulesVersion());
        com.sfs.core.rules.PlanCompliance compliance = planChecker.check(plan, content);

        findings.add(new ConstraintFinding(
                compliance.satisfied()
                        ? ConstraintFinding.Severity.SATISFIED
                        : ConstraintFinding.Severity.VIOLATION,
                "Plan rules (" + plan.rulesVersion() + ")",
                compliance.satisfied()
                        ? "All declarative rule checks passed: "
                                + plan.requiredFacts().size() + " required fact(s), "
                                + plan.requiredEntities().size() + " required entit(ies), "
                                + plan.requiredRelationships().size()
                                + " required relationship(s)"
                                + (plan.sectionOrder().isEmpty()
                                        ? ""
                                        : ", " + plan.sectionOrder().size()
                                                + " section(s) in order") + "."
                        : "Rule violations: " + String.join("; ", compliance.violations())));

        for (String warning : compliance.warnings()) {
            findings.add(new ConstraintFinding(
                    ConstraintFinding.Severity.WARNING, "Plan warning", warning));
        }
        return findings;
    }

    private String render(SemanticDnaView dna, String dnaVersion, String rulesVersion) {
        StringBuilder text = new StringBuilder(ReconstructionArtifact.provenanceHeader(
                dna.objectId(), dnaVersion, rulesVersion, MODEL_VERSION));

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
                "unavailable", LEGACY_RULES_VERSION, MODEL_VERSION,
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
