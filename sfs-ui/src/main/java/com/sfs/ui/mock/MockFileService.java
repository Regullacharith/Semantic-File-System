package com.sfs.ui.mock;

import com.sfs.contracts.file.FileImportRequest;
import com.sfs.contracts.file.FileOperationResult;
import com.sfs.contracts.file.FileService;
import com.sfs.contracts.file.FileStatus;
import com.sfs.contracts.file.FileSummary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory stand-in for the File Lifecycle Manager.
 */
@Service
@Profile("mock")
public class MockFileService implements FileService {

    private static final int SEED_COUNT = 4;

    private final Map<String, FileSummary> filesByObjectId = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public MockFileService() {
        seedSampleData();
    }

    @Override
    public List<FileSummary> listFiles() {
        return filesByObjectId.values().stream()
                .sorted(Comparator.comparing(FileSummary::registeredAt).reversed())
                .toList();
    }

    @Override
    public Optional<FileSummary> findByObjectId(String objectId) {
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

        String objectId = nextObjectId();

        FileSummary summary = new FileSummary(
                objectId,
                request.fileName(),
                FileStatus.REGISTERED,
                request.contentLength(),
                Instant.now(),
                null);

        filesByObjectId.put(objectId, summary);

        return FileOperationResult.success(
                objectId,
                "Imported '" + request.fileName() + "' and assigned an Object ID. "
                        + "Semantic analysis has not run yet.");
    }

    @Override
    public FileOperationResult requestAnalysis(String objectId) {
        Optional<FileSummary> found = findByObjectId(objectId);
        if (found.isEmpty()) {
            return FileOperationResult.failure("No file exists with that Object ID.");
        }

        FileSummary file = found.get();
        if (!file.status().allowsAnalysis()) {
            return FileOperationResult.failure(
                    "Analysis is not permitted while the file is " + file.status().getLabel() + ".");
        }

        filesByObjectId.put(objectId, new FileSummary(
                file.objectId(),
                file.displayName(),
                FileStatus.ANALYZED,
                file.sizeBytes(),
                file.registeredAt(),
                Instant.now()));

        return FileOperationResult.success(
                objectId,
                "Analysis completed. Note: this is mock data; no Semantic DNA was produced.");
    }

    @Override
    public FileOperationResult requestSemanticDeletion(String objectId) {
        Optional<FileSummary> found = findByObjectId(objectId);
        if (found.isEmpty()) {
            return FileOperationResult.failure("No file exists with that Object ID.");
        }

        FileSummary file = found.get();

        if (file.status() == FileStatus.MEMORIZED) {
            return FileOperationResult.failure(
                    "The raw bytes for this object have already been removed.");
        }

        if (!file.status().allowsSemanticDeletion()) {
            return FileOperationResult.failure(
                    "Semantic deletion is refused: the file must be analyzed and its Semantic "
                            + "Record durably committed before raw bytes can be removed. "
                            + "Current status: " + file.status().getLabel() + ".");
        }

        filesByObjectId.put(objectId, new FileSummary(
                file.objectId(),
                file.displayName(),
                FileStatus.MEMORIZED,
                file.sizeBytes(),
                file.registeredAt(),
                file.analyzedAt()));

        return FileOperationResult.success(
                objectId,
                "Raw bytes removed. The Semantic Record survives and remains searchable.");
    }

    private String nextObjectId() {
        return "sfs-obj-%04d-%s".formatted(
                sequence.incrementAndGet(),
                UUID.randomUUID().toString().substring(0, 8));
    }

    private void seedSampleData() {
        Instant now = Instant.now();

        put(new FileSummary("sfs-obj-0001-a1b2c3d4", "research-summary.txt",
                FileStatus.ANALYZED, 15_360, now.minusSeconds(3_600), now.minusSeconds(3_400)));

        put(new FileSummary("sfs-obj-0002-e5f6a7b8", "archived-report.txt",
                FileStatus.MEMORIZED, 8_192, now.minusSeconds(86_400), now.minusSeconds(86_000)));

        put(new FileSummary("sfs-obj-0003-c9d0e1f2", "meeting-notes.txt",
                FileStatus.REGISTERED, 2_048, now.minusSeconds(7_200), null));

        put(new FileSummary("sfs-obj-0004-b3c4d5e6", "deployment-config.txt",
                FileStatus.ANALYZED, 4_096, now.minusSeconds(10_800), now.minusSeconds(10_600)));

        sequence.set(SEED_COUNT);

        assert filesByObjectId.size() == SEED_COUNT;
    }

    private void put(FileSummary summary) {
        filesByObjectId.put(summary.objectId(), summary);
    }
}