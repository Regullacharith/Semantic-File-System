package com.sfs.ui.view;

import com.sfs.contracts.file.FileStatus;
import com.sfs.contracts.file.FileSummary;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Presentation projection of a {@link FileSummary}.
 *
 * @param objectId       stable logical identity
 * @param displayName    file name shown to the user
 * @param status         lifecycle status
 * @param statusLabel    human-readable status
 * @param statusDescription explanatory text for the status
 * @param formattedSize  human-readable original size
 * @param registeredAt   formatted registration timestamp
 * @param analyzedAt     formatted analysis timestamp, or a placeholder when not analyzed
 * @param rawDataRemoved whether the raw bytes are gone while semantic memory persists
 * @param canAnalyze     whether the analyze action should be offered
 * @param canDelete      whether the semantic-deletion action should be offered
 */
public record FileViewModel(
        String objectId,
        String displayName,
        FileStatus status,
        String statusLabel,
        String statusDescription,
        String formattedSize,
        String registeredAt,
        String analyzedAt,
        boolean rawDataRemoved,
        boolean canAnalyze,
        boolean canDelete) {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private static final String NOT_APPLICABLE = "—";

    /**
     * Projects a contract summary into display state.
     */
    public static FileViewModel from(FileSummary summary) {
        Objects.requireNonNull(summary, "summary must not be null");

        return new FileViewModel(
                summary.objectId(),
                summary.displayName(),
                summary.status(),
                summary.status().getLabel(),
                summary.status().getDescription(),
                formatSize(summary.sizeBytes()),
                formatTimestamp(summary.registeredAt()),
                formatTimestamp(summary.analyzedAt()),
                summary.status().isRawDataRemoved(),
                summary.status().allowsAnalysis(),
                summary.status().allowsSemanticDeletion());
    }

    /**
     * Formats a byte count using binary units.
     */
    static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return "%.1f KB".formatted(bytes / 1024.0);
        }
        return "%.1f MB".formatted(bytes / (1024.0 * 1024.0));
    }

    /**
     * Formats a timestamp, or returns a placeholder when absent.
     */
    static String formatTimestamp(Instant instant) {
        return instant == null ? NOT_APPLICABLE : TIMESTAMP_FORMAT.format(instant);
    }
}
