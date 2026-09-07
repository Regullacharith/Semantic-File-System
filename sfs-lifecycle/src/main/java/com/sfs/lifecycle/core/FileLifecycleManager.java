package com.sfs.lifecycle.core;

import com.sfs.contracts.file.FileImportRequest;
import com.sfs.contracts.file.FileOperationResult;
import com.sfs.contracts.file.FileService;
import com.sfs.contracts.file.FileStatus;
import com.sfs.contracts.file.FileSummary;
import com.sfs.contracts.security.Principal;
import com.sfs.core.identity.ObjectId;
import com.sfs.lifecycle.audit.LifecycleAuditLog;
import com.sfs.lifecycle.gate.RawDeletionGate;
import com.sfs.lifecycle.identity.ObjectIdService;
import com.sfs.lifecycle.model.ContentDigest;
import com.sfs.lifecycle.model.FileMetadata;
import com.sfs.lifecycle.model.FileVersion;
import com.sfs.lifecycle.model.LifecycleEvent;
import com.sfs.lifecycle.model.LifecycleEventType;
import com.sfs.lifecycle.model.SemanticFile;
import com.sfs.lifecycle.state.FileState;
import com.sfs.lifecycle.state.LifecycleStateMachine;
import com.sfs.lifecycle.store.RawContentStore;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class FileLifecycleManager implements FileService {

    public static final String SYSTEM_PRINCIPAL = "system";

    private final ConcurrentMap<String, SemanticFile> filesByObjectId = new ConcurrentHashMap<>();
    private final RawContentStore rawContentStore;
    private final Clock clock;
    private final ObjectIdService objectIdService;
    private final LifecycleStateMachine stateMachine = new LifecycleStateMachine();
    private final RawDeletionGate deletionGate = new RawDeletionGate();
    private final LifecycleAuditLog auditLog = new LifecycleAuditLog();
    private AnalysisDispatcher analysisDispatcher;

    public FileLifecycleManager(Clock clock, RawContentStore rawContentStore,
                                ObjectIdService objectIdService) {
        this(clock, rawContentStore, objectIdService, null);
    }

    public FileLifecycleManager(Clock clock, RawContentStore rawContentStore,
                                ObjectIdService objectIdService,
                                AnalysisDispatcher analysisDispatcher) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.rawContentStore = Objects.requireNonNull(rawContentStore, "rawContentStore must not be null");
        this.objectIdService = Objects.requireNonNull(objectIdService, "objectIdService must not be null");
        this.analysisDispatcher = analysisDispatcher;
    }

    public void bindAnalysisDispatcher(AnalysisDispatcher dispatcher) {
        this.analysisDispatcher = Objects.requireNonNull(dispatcher, "dispatcher must not be null");
    }

    public LifecycleAuditLog auditLog() {
        return auditLog;
    }

    public int recoverInterruptedMemorizations() {
        int recovered = 0;
        for (Map.Entry<String, SemanticFile> entry : filesByObjectId.entrySet()) {
            if (entry.getValue().state() == FileState.MEMORIZABLE) {
                SemanticFile restored = entry.getValue()
                        .withState(FileState.ANALYZED, clock.instant());
                filesByObjectId.put(entry.getKey(), restored);
                audit(entry.getKey(), LifecycleEventType.MEMORIZE_INTERRUPTED,
                        FileState.MEMORIZABLE, FileState.ANALYZED, SYSTEM_PRINCIPAL,
                        false, "interrupted memorization rolled back at startup", null);
                recovered++;
            }
        }
        return recovered;
    }

    void adopt(SemanticFile file) {
        Objects.requireNonNull(file, "file must not be null");
        filesByObjectId.put(file.objectId().value(), file);
    }

    public Optional<SemanticFile> registeredFile(String objectId) {
        if (objectId == null || objectId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(filesByObjectId.get(objectId));
    }

    @Override
    public FileOperationResult importFile(FileImportRequest request) {
        if (request == null) {
            return FileOperationResult.failure("No import request was supplied.");
        }
        byte[] content = request.content().getBytes(StandardCharsets.UTF_8);
        String sha256 = ContentDigest.sha256Hex(content);
        Instant now = clock.instant();

        String objectId = objectIdService.nextUnique(filesByObjectId::containsKey).value();
        FileMetadata metadata = new FileMetadata(
                request.fileName(),
                normalizeContentType(request.contentType()),
                content.length,
                sha256,
                null,
                now,
                now);
        FileVersion initialVersion = new FileVersion(1, sha256, content.length, now);
        SemanticFile file = SemanticFile.initial(ObjectId.of(objectId), metadata, initialVersion, now);

        rawContentStore.store(objectId, content);
        filesByObjectId.put(objectId, file);
        audit(objectId, LifecycleEventType.REGISTRATION_RECORDED, null,
                file.state(), SYSTEM_PRINCIPAL, false, null, null);

        return FileOperationResult.success(
                objectId,
                "Registered '" + request.fileName() + "' and assigned Object ID " + objectId
                        + ". Semantic analysis has not run yet.");
    }

    @Override
    public FileOperationResult requestAnalysis(String objectId) {
        SemanticFile file = registeredFile(objectId).orElse(null);
        if (file == null) {
            return FileOperationResult.failure("No file exists with that Object ID.");
        }
        if (!stateMachine.isLegal(file.state(), LifecycleEventType.ANALYSIS_STARTED)) {
            return refuse(file, SYSTEM_PRINCIPAL, LifecycleEventType.ANALYSIS_REFUSED,
                    "Analysis is not permitted while the file is "
                            + presentationLabel(file) + ".");
        }
        SemanticFile analyzing = apply(file, LifecycleEventType.ANALYSIS_STARTED, SYSTEM_PRINCIPAL);
        if (analysisDispatcher != null) {
            analysisDispatcher.dispatch(analyzing.objectId().value());
        }
        return FileOperationResult.success(analyzing.objectId().value(),
                "Semantic analysis started for '" + analyzing.metadata().fileName() + "'.");
    }

    public void completeAnalysisSuccess(String objectId, String dnaVersion) {
        Objects.requireNonNull(dnaVersion, "dnaVersion must not be null");
        SemanticFile file = requireExisting(objectId);
        SemanticFile analyzed = apply(file, LifecycleEventType.ANALYSIS_SUCCEEDED, SYSTEM_PRINCIPAL)
                .withCertifiedDna(dnaVersion, clock.instant());
        filesByObjectId.put(objectId, analyzed);
    }

    public void completeAnalysisFailure(String objectId, String reason) {
        Objects.requireNonNull(reason, "reason must not be null");
        SemanticFile file = requireExisting(objectId);
        FileState target = stateMachine.requireTarget(file.state(), LifecycleEventType.ANALYSIS_FAILED);
        SemanticFile failed = file.withState(target, clock.instant());
        filesByObjectId.put(objectId, failed);
        audit(objectId, LifecycleEventType.ANALYSIS_FAILED, file.state(), target,
                SYSTEM_PRINCIPAL, false, reason, null);
    }

    @Override
    public FileOperationResult softDelete(String objectId, Principal principal) {
        SemanticFile file = registeredFile(objectId).orElse(null);
        if (file == null) {
            return FileOperationResult.failure("No file exists with that Object ID.");
        }
        String actor = actorOf(principal);
        if (!stateMachine.isLegal(file.state(), LifecycleEventType.SOFT_DELETED)) {
            return refuse(file, actor, LifecycleEventType.SOFT_DELETE_REFUSED,
                    "Deletion is not permitted while the file is " + presentationLabel(file) + ".");
        }
        apply(file, LifecycleEventType.SOFT_DELETED, actor);
        return FileOperationResult.success(objectId,
                "Object deleted. Raw bytes are retained and the deletion can be undone.");
    }

    @Override
    public FileOperationResult undoDelete(String objectId, Principal principal) {
        SemanticFile file = registeredFile(objectId).orElse(null);
        if (file == null) {
            return FileOperationResult.failure("No file exists with that Object ID.");
        }
        String actor = actorOf(principal);
        if (!stateMachine.isLegal(file.state(), LifecycleEventType.UNDO_DELETED)) {
            return refuse(file, actor, LifecycleEventType.UNDO_DELETE_REFUSED,
                    "Only a deleted object can be restored. The file is "
                            + presentationLabel(file) + ".");
        }
        FileState origin = file.deletedFrom();
        stateMachine.requireUndoTarget(file.state(), origin);
        SemanticFile restored = file.withDeletionCleared(origin, clock.instant());
        filesByObjectId.put(objectId, restored);
        audit(objectId, LifecycleEventType.UNDO_DELETED, FileState.SOFT_DELETED, origin,
                actor, false, null, null);
        return FileOperationResult.success(objectId,
                "Object restored. It is " + presentationLabel(restored) + " again.");
    }

    @Override
    public FileOperationResult purgeRawData(String objectId, Principal principal) {
        SemanticFile file = registeredFile(objectId).orElse(null);
        if (file == null) {
            return FileOperationResult.failure("No file exists with that Object ID.");
        }
        String actor = actorOf(principal);
        if (!stateMachine.isLegal(file.state(), LifecycleEventType.PURGE_REQUESTED)) {
            return refuse(file, actor, LifecycleEventType.PURGE_REFUSED,
                    "Purge requires a deleted object, so raw bytes are never released in a "
                            + "single step. The file is " + presentationLabel(file) + ".");
        }
        RawDeletionGate.DeletionDecision decision =
                deletionGate.evaluate(file.state(), file.deletedFrom());
        if (!decision.allowed()) {
            return refuse(file, actor, LifecycleEventType.PURGE_REFUSED, decision.refusalReason());
        }
        Instant purgeStarted = clock.instant();
        apply(file, LifecycleEventType.PURGE_REQUESTED, actor);
        boolean released = rawContentStore.release(objectId);
        if (!released) {
            return refuse(file, actor, LifecycleEventType.PURGE_REFUSED,
                    "The raw bytes are no longer present; nothing was released.");
        }
        long durationMs = Math.max(0, clock.instant().toEpochMilli() - purgeStarted.toEpochMilli());
        SemanticFile beforeRelease = filesByObjectId.get(objectId);
        SemanticFile afterRelease =
                apply(beforeRelease, LifecycleEventType.RAW_RELEASED, actor, durationMs)
                        .withMetadata(beforeRelease.metadata().withoutStorageAddress(clock.instant()));
        filesByObjectId.put(objectId, afterRelease);
        return FileOperationResult.success(objectId,
                "Raw bytes permanently released. The Semantic Record survives and the object "
                        + "is now memorized.");
    }

    @Override
    public FileOperationResult memorize(String objectId, Principal principal) {
        SemanticFile file = registeredFile(objectId).orElse(null);
        if (file == null) {
            return FileOperationResult.failure("No file exists with that Object ID.");
        }
        String actor = actorOf(principal);
        if (!stateMachine.isLegal(file.state(), LifecycleEventType.MEMORY_COMMIT_REQUESTED)) {
            return refuse(file, actor, LifecycleEventType.MEMORY_COMMIT_REFUSED,
                    "Memorization requires an analyzed object. The file is "
                            + presentationLabel(file) + ".");
        }
        if (file.certifiedDnaVersion() == null) {
            return refuse(file, actor, LifecycleEventType.MEMORY_COMMIT_REFUSED,
                    "Memorization requires a certified Semantic DNA from a successful "
                            + "analysis. A failed analysis can never become a successful "
                            + "memorization.");
        }
        Instant commitStarted = clock.instant();
        apply(file, LifecycleEventType.MEMORY_COMMIT_REQUESTED, actor);
        SemanticFile memorizable =
                apply(filesByObjectId.get(objectId), LifecycleEventType.DNA_VALIDATED, actor);
        long durationMs = Math.max(0, clock.instant().toEpochMilli() - commitStarted.toEpochMilli());
        apply(memorizable, LifecycleEventType.MEMORY_COMMITTED, actor, durationMs);
        return FileOperationResult.success(objectId,
                "Semantic memory committed. Raw bytes remain and can be released only "
                        + "through the deletion protocol.");
    }

    @Override
    public List<FileSummary> listFiles() {
        return filesByObjectId.values().stream()
                .sorted(Comparator.comparing((SemanticFile f) -> f.metadata().registeredAt()).reversed()
                        .thenComparing(f -> f.objectId().value()))
                .map(this::toSummary)
                .toList();
    }

    @Override
    public Optional<FileSummary> findByObjectId(String objectId) {
        return registeredFile(objectId).map(this::toSummary);
    }

    public FileOperationResult renameObject(String objectId, String newFileName, Principal principal) {
        SemanticFile file = registeredFile(objectId).orElse(null);
        if (file == null) {
            return FileOperationResult.failure("No file exists with that Object ID.");
        }
        String actor = actorOf(principal);
        if (!stateMachine.isLegal(file.state(), LifecycleEventType.METADATA_UPDATED)) {
            return refuse(file, actor, LifecycleEventType.METADATA_UPDATE_REFUSED,
                    "The display name cannot change while the file is "
                            + presentationLabel(file) + ".");
        }
        try {
            SemanticFile renamed = file.withMetadata(
                    file.metadata().withFileName(newFileName, clock.instant()));
            filesByObjectId.put(objectId, renamed);
            audit(objectId, LifecycleEventType.METADATA_UPDATED, file.state(), file.state(),
                    actor, false, null, null);
            return FileOperationResult.success(objectId,
                    "Display name updated to '" + newFileName + "'. The Object ID is unchanged.");
        } catch (IllegalArgumentException e) {
            return refuse(file, actor, LifecycleEventType.METADATA_UPDATE_REFUSED,
                    "The display name was rejected: " + e.getMessage());
        }
    }

    public FileOperationResult recordContentVersion(String objectId, String newContent,
                                                    Principal principal) {
        SemanticFile file = registeredFile(objectId).orElse(null);
        if (file == null) {
            return FileOperationResult.failure("No file exists with that Object ID.");
        }
        String actor = actorOf(principal);
        if (!stateMachine.isLegal(file.state(), LifecycleEventType.VERSION_ADDED)) {
            return refuse(file, actor, LifecycleEventType.VERSION_ADD_REFUSED,
                    "Content cannot be replaced while the file is " + presentationLabel(file) + ".");
        }
        byte[] content = newContent.getBytes(StandardCharsets.UTF_8);
        String sha256 = ContentDigest.sha256Hex(content);
        FileVersion current = file.currentVersion();
        if (current.contentSha256().equals(sha256)) {
            return refuse(file, actor, LifecycleEventType.VERSION_ADD_REFUSED,
                    "The supplied content is identical to version " + current.number()
                            + "; no new version was recorded.");
        }
        FileVersion next = new FileVersion(current.number() + 1, sha256,
                content.length, clock.instant());
        SemanticFile updated = file.withAdditionalVersion(next);
        filesByObjectId.put(objectId, updated);
        rawContentStore.store(objectId, content);
        audit(objectId, LifecycleEventType.VERSION_ADDED, file.state(), file.state(),
                actor, false, "version " + next.number() + " captured", null);
        return FileOperationResult.success(objectId,
                "Version " + next.number() + " recorded. The Object ID is unchanged.");
    }

    public List<FileVersion> versionHistory(String objectId) {
        return registeredFile(objectId)
                .map(file -> file.versions())
                .orElse(List.of());
    }

    private SemanticFile apply(SemanticFile before, LifecycleEventType event, String principalId) {
        return apply(before, event, principalId, null);
    }

    private SemanticFile apply(SemanticFile before, LifecycleEventType event,
                               String principalId, Long durationMs) {
        FileState target = stateMachine.requireTarget(before.state(), event);
        SemanticFile after = transitioned(before, target);
        filesByObjectId.put(before.objectId().value(), after);
        audit(before.objectId().value(), event, before.state(), target, principalId,
                false, null, durationMs);
        return after;
    }

    private SemanticFile transitioned(SemanticFile before, FileState target) {
        if (target == before.state()) {
            return before.withState(target, clock.instant());
        }
        if (target == FileState.SOFT_DELETED) {
            return before.softDeletedFrom(before.state(), clock.instant());
        }
        if (target == FileState.MEMORIZED) {
            return before.withDeletionCleared(FileState.MEMORIZED, clock.instant());
        }
        return before.withState(target, clock.instant());
    }

    private FileOperationResult refuse(SemanticFile file, String actor,
                                       LifecycleEventType refusalType, String reason) {
        audit(file.objectId().value(), refusalType, file.state(), file.state(),
                actor, true, reason, null);
        return FileOperationResult.failure(reason);
    }

    private void audit(String objectId, LifecycleEventType type, FileState from,
                       FileState to, String principalId, boolean refused, String reason,
                       Long durationMs) {
        LifecycleEvent event = new LifecycleEvent(
                auditLog.nextEventId(), objectId, type, from, to, principalId,
                refused, reason, clock.instant(), durationMs);
        auditLog.append(event);
    }

    private SemanticFile requireExisting(String objectId) {
        return registeredFile(objectId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No file exists with Object ID " + objectId));
    }

    private static String actorOf(Principal principal) {
        Objects.requireNonNull(principal, "principal must not be null");
        return principal.id();
    }

    private static String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "text/plain";
        }
        return contentType;
    }

    private FileSummary toSummary(SemanticFile file) {
        return new FileSummary(
                file.objectId().value(),
                file.metadata().fileName(),
                toPresentationStatus(file.state()),
                file.metadata().sizeBytes(),
                file.metadata().registeredAt(),
                analyzedAtOf(file));
    }

    private static String presentationLabel(SemanticFile file) {
        return toPresentationStatus(file.state()).getLabel();
    }

    private static Instant analyzedAtOf(SemanticFile file) {
        if (file.state() == FileState.REGISTERED || file.state() == FileState.ANALYZING) {
            return null;
        }
        return file.stateChangedAt();
    }

    private static FileStatus toPresentationStatus(FileState state) {
        return switch (state) {
            case REGISTERED -> FileStatus.REGISTERED;
            case ANALYZING -> FileStatus.ANALYZING;
            case ANALYZED, MEMORIZABLE -> FileStatus.ANALYZED;
            case MEMORY_COMMITTED -> FileStatus.MEMORY_COMMITTED;
            case SOFT_DELETED -> FileStatus.SOFT_DELETED;
            case MEMORIZED -> FileStatus.MEMORIZED;
            case FAILED -> FileStatus.FAILED;
        };
    }
}
