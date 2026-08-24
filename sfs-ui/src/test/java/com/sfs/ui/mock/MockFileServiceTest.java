package com.sfs.ui.mock;

import com.sfs.contracts.file.FileImportRequest;
import com.sfs.contracts.file.FileOperationResult;
import com.sfs.contracts.file.FileStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Mock file service")
class MockFileServiceTest {

    private MockFileService service;

    @BeforeEach
    void setUp() {
        service = new MockFileService();
    }

    @Nested
    @DisplayName("import")
    class Import {

        @Test
        @DisplayName("registers a file and assigns an Object ID")
        void registersFile() {
            FileOperationResult result = service.importFile(
                    new FileImportRequest("notes.txt", "Some content.", "text/plain"));

            assertThat(result.successful()).isTrue();
            assertThat(result.objectId()).isNotBlank();
            assertThat(service.findByObjectId(result.objectId())).isPresent();
        }

        @Test
        @DisplayName("registers without analyzing")
        void registersWithoutAnalyzing() {
            FileOperationResult result = service.importFile(
                    new FileImportRequest("notes.txt", "Some content.", "text/plain"));

            assertThat(service.findByObjectId(result.objectId()).orElseThrow().status())
                    .isEqualTo(FileStatus.REGISTERED);
        }

        @Test
        @DisplayName("assigns a distinct Object ID per import")
        void assignsDistinctIds() {
            String first = service.importFile(
                    new FileImportRequest("a.txt", "x", null)).objectId();
            String second = service.importFile(
                    new FileImportRequest("b.txt", "y", null)).objectId();

            assertThat(first).isNotEqualTo(second);
        }

        @Test
        @DisplayName("reports failure rather than throwing on a null request")
        void handlesNullRequest() {
            assertThat(service.importFile(null).successful()).isFalse();
        }
    }

    @Nested
    @DisplayName("analysis")
    class Analysis {

        @Test
        @DisplayName("moves a registered file to analyzed")
        void analyzesRegisteredFile() {
            String id = service.importFile(
                    new FileImportRequest("notes.txt", "content", null)).objectId();

            assertThat(service.requestAnalysis(id).successful()).isTrue();
            assertThat(service.findByObjectId(id).orElseThrow().status())
                    .isEqualTo(FileStatus.ANALYZED);
        }

        @Test
        @DisplayName("refuses to re-analyze an already analyzed file")
        void refusesRepeatAnalysis() {
            String id = service.importFile(
                    new FileImportRequest("notes.txt", "content", null)).objectId();
            service.requestAnalysis(id);

            assertThat(service.requestAnalysis(id).successful()).isFalse();
        }

        @Test
        @DisplayName("refuses an unknown Object ID")
        void refusesUnknownId() {
            FileOperationResult result = service.requestAnalysis("does-not-exist");

            assertThat(result.successful()).isFalse();
            assertThat(result.message()).contains("No file exists");
        }
    }

    @Nested
    @DisplayName("reversible deletion and purge")
    class DeletionAndPurge {

        private String analyzedFile() {
            String id = service.importFile(
                    new FileImportRequest("notes.txt", "content", null)).objectId();
            service.requestAnalysis(id);
            return id;
        }

        @Test
        @DisplayName("refuses deletion before the file has been analyzed")
        void refusesDeletionBeforeAnalysis() {
            String id = service.importFile(
                    new FileImportRequest("notes.txt", "content", null)).objectId();

            FileOperationResult result = service.softDelete(id);

            assertThat(result.successful()).isFalse();
            assertThat(result.message()).contains("refused");
            assertThat(service.findByObjectId(id).orElseThrow().status())
                    .isEqualTo(FileStatus.REGISTERED);
        }

        @Test
        @DisplayName("moves an analyzed file to the recoverable deleted state")
        void softDeletesAnalyzedFile() {
            String id = analyzedFile();

            assertThat(service.softDelete(id).successful()).isTrue();
            assertThat(service.findByObjectId(id).orElseThrow().status())
                    .isEqualTo(FileStatus.SOFT_DELETED);
        }

        @Test
        @DisplayName("keeps raw bytes when a file is deleted")
        void deletionKeepsRawBytes() {
            String id = analyzedFile();
            service.softDelete(id);

            assertThat(service.findByObjectId(id).orElseThrow().status().isRawDataRemoved())
                    .isFalse();
        }

        @Test
        @DisplayName("restores a deleted file")
        void restoresDeletedFile() {
            String id = analyzedFile();
            service.softDelete(id);

            assertThat(service.undoDelete(id).successful()).isTrue();
            assertThat(service.findByObjectId(id).orElseThrow().status())
                    .isEqualTo(FileStatus.ANALYZED);
        }

        @Test
        @DisplayName("refuses to delete the same file twice")
        void refusesDoubleDeletion() {
            String id = analyzedFile();
            service.softDelete(id);

            assertThat(service.softDelete(id).successful()).isFalse();
        }

        @Test
        @DisplayName("refuses to purge a file that was never deleted")
        void refusesSingleStepPurge() {
            String id = analyzedFile();

            FileOperationResult result = service.purgeRawData(id);

            assertThat(result.successful()).isFalse();
            assertThat(service.findByObjectId(id).orElseThrow().status())
                    .isEqualTo(FileStatus.ANALYZED);
        }

        @Test
        @DisplayName("releases raw bytes when a deleted file is purged")
        void purgeReleasesRawBytes() {
            String id = analyzedFile();
            service.softDelete(id);

            assertThat(service.purgeRawData(id).successful()).isTrue();
            assertThat(service.findByObjectId(id).orElseThrow().status())
                    .isEqualTo(FileStatus.MEMORIZED);
        }

        @Test
        @DisplayName("retains the Semantic Record after a purge")
        void retainsRecordAfterPurge() {
            String id = analyzedFile();
            service.softDelete(id);
            service.purgeRawData(id);

            assertThat(service.findByObjectId(id)).isPresent();
            assertThat(service.listFiles())
                    .extracting(f -> f.objectId())
                    .contains(id);
        }

        @Test
        @DisplayName("refuses to restore a purged file")
        void refusesUndoAfterPurge() {
            String id = analyzedFile();
            service.softDelete(id);
            service.purgeRawData(id);

            assertThat(service.undoDelete(id).successful()).isFalse();
        }

        @Test
        @DisplayName("refuses to purge the same file twice")
        void refusesDoublePurge() {
            String id = analyzedFile();
            service.softDelete(id);
            service.purgeRawData(id);

            assertThat(service.purgeRawData(id).successful()).isFalse();
        }
    }

    @Nested
    @DisplayName("listing")
    class Listing {

        @Test
        @DisplayName("seeds sample data covering multiple lifecycle states")
        void seedsSampleData() {
            assertThat(service.listFiles())
                    .extracting(f -> f.status())
                    .contains(FileStatus.REGISTERED, FileStatus.ANALYZED, FileStatus.MEMORIZED);
        }

        @Test
        @DisplayName("returns an empty result for a blank Object ID")
        void handlesBlankId() {
            assertThat(service.findByObjectId("  ")).isEmpty();
            assertThat(service.findByObjectId(null)).isEmpty();
        }
    }
}
