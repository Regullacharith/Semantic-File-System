package com.sfs.ui.view;

import com.sfs.contracts.file.FileStatus;
import com.sfs.contracts.file.FileSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the presentation projection of a file summary.
 */
@DisplayName("FileViewModel")
class FileViewModelTest {

    private static FileSummary summary(FileStatus status, long size, Instant analyzed) {
        return new FileSummary("sfs-obj-0001", "notes.txt", status, size,
                Instant.parse("2026-08-16T10:00:00Z"), analyzed);
    }

    @Test
    @DisplayName("projects identity and status from the contract object")
    void projectsContractFields() {
        FileViewModel model = FileViewModel.from(
                summary(FileStatus.ANALYZED, 2_048, Instant.parse("2026-08-16T11:00:00Z")));

        assertThat(model.objectId()).isEqualTo("sfs-obj-0001");
        assertThat(model.displayName()).isEqualTo("notes.txt");
        assertThat(model.statusLabel()).isEqualTo("Analyzed");
        assertThat(model.statusDescription()).isNotBlank();
    }

    @Test
    @DisplayName("rejects a null summary")
    void rejectsNullSummary() {
        assertThatThrownBy(() -> FileViewModel.from(null))
                .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @CsvSource({
            "512,        512 B",
            "1024,       1.0 KB",
            "15360,      15.0 KB",
            "1048576,    1.0 MB"
    })
    @DisplayName("formats sizes in binary units")
    void formatsSizes(long bytes, String expected) {
        assertThat(FileViewModel.formatSize(bytes)).isEqualTo(expected);
    }

    @Test
    @DisplayName("renders a placeholder when a file has not been analyzed")
    void rendersPlaceholderForMissingTimestamp() {
        FileViewModel model = FileViewModel.from(summary(FileStatus.REGISTERED, 100, null));

        assertThat(model.analyzedAt()).isEqualTo("—");
        assertThat(model.registeredAt()).isNotBlank();
    }

    @Test
    @DisplayName("offers analysis for a registered file but not deletion")
    void gatesActionsForRegisteredFile() {
        FileViewModel model = FileViewModel.from(summary(FileStatus.REGISTERED, 100, null));

        assertThat(model.canAnalyze()).isTrue();
        assertThat(model.canDelete()).isFalse();
        assertThat(model.rawDataRemoved()).isFalse();
    }

    @Test
    @DisplayName("offers deletion only once a file is analyzed")
    void gatesActionsForAnalyzedFile() {
        FileViewModel model = FileViewModel.from(
                summary(FileStatus.ANALYZED, 100, Instant.now()));

        assertThat(model.canDelete()).isTrue();
        assertThat(model.canAnalyze()).isFalse();
    }

    @Test
    @DisplayName("marks a memorized record as having no raw data and no further actions")
    void marksMemorizedRecord() {
        FileViewModel model = FileViewModel.from(
                summary(FileStatus.MEMORIZED, 100, Instant.now()));

        assertThat(model.rawDataRemoved()).isTrue();
        assertThat(model.canAnalyze()).isFalse();
        assertThat(model.canDelete()).isFalse();
    }
}
