package com.sfs.app.api.request;

import com.sfs.core.identity.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("API request validation")
class RequestValidationTest {

    @Nested
    @DisplayName("file import")
    class FileImport {

        @Test
        @DisplayName("accepts a plain text file")
        void acceptsPlainTextFile() {
            FileImportApiRequest request =
                    new FileImportApiRequest("notes.txt", "Some content.", "text/plain");

            assertThat(request.fileName()).isEqualTo("notes.txt");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "../etc/passwd",
                "../../secret.txt",
                "dir/notes.txt",
                "dir\\notes.txt",
                "..",
                "notes/../../../etc/shadow"})
        @DisplayName("rejects a path in the file name")
        void rejectsPathInFileName(String fileName) {
            assertThatThrownBy(() -> new FileImportApiRequest(fileName, "content", "text/plain"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects blank content")
        void rejectsBlankContent() {
            assertThatThrownBy(() -> new FileImportApiRequest("notes.txt", "   ", "text/plain"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("content");
        }

        @Test
        @DisplayName("rejects a blank file name")
        void rejectsBlankFileName() {
            assertThatThrownBy(() -> new FileImportApiRequest("  ", "content", "text/plain"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("fileName");
        }

        @Test
        @DisplayName("rejects an absurdly long file name")
        void rejectsLongFileName() {
            assertThatThrownBy(() ->
                    new FileImportApiRequest("a".repeat(300) + ".txt", "content", "text/plain"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("search")
    class Search {

        @Test
        @DisplayName("defaults the result count when unspecified")
        void defaultsResultCount() {
            assertThat(new SearchApiRequest("quarterly budget", null).effectiveMaxResults())
                    .isEqualTo(SearchApiRequest.DEFAULT_RESULTS);
        }

        @Test
        @DisplayName("honours an explicit result count")
        void honoursExplicitResultCount() {
            assertThat(new SearchApiRequest("budget", 5).effectiveMaxResults()).isEqualTo(5);
        }

        @Test
        @DisplayName("rejects blank query text")
        void rejectsBlankText() {
            assertThatThrownBy(() -> new SearchApiRequest("   ", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("text");
        }

        @Test
        @DisplayName("rejects query text beyond the limit")
        void rejectsOverLongText() {
            assertThatThrownBy(() -> new SearchApiRequest("a".repeat(501), null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("500");
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1, 101, 1000})
        @DisplayName("rejects a result count outside the permitted range")
        void rejectsOutOfRangeResultCount(int maxResults) {
            assertThatThrownBy(() -> new SearchApiRequest("budget", maxResults))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 20, 100})
        @DisplayName("accepts the boundary result counts")
        void acceptsBoundaryResultCounts(int maxResults) {
            assertThatCode(() -> new SearchApiRequest("budget", maxResults))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("reconstruction")
    class Reconstruction {

        @Test
        @DisplayName("accepts a valid Object ID")
        void acceptsValidObjectId() {
            ReconstructionApiRequest request =
                    new ReconstructionApiRequest("sfs-obj-0001-a1b2c3d4");

            assertThat(request.toObjectId()).isEqualTo(ObjectId.of("sfs-obj-0001-a1b2c3d4"));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "not-an-id",
                "sfs-obj-1-abc",
                "../../etc/passwd",
                "sfs-obj-0001-abc; DROP TABLE files"})
        @DisplayName("rejects an invalid Object ID at construction")
        void rejectsInvalidObjectId(String objectId) {
            assertThatThrownBy(() -> new ReconstructionApiRequest(objectId))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
