package com.sfs.lifecycle.model;

import com.sfs.core.identity.ObjectId;
import com.sfs.lifecycle.state.FileState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SemanticFile")
class SemanticFileTest {

    private static final Instant T0 = Instant.parse("2026-03-15T10:00:00Z");
    private static final Instant T1 = Instant.parse("2026-03-15T10:01:00Z");
    private static final String SHA = ContentDigest.sha256Hex("content");

    private SemanticFile initial() {
        return SemanticFile.initial(
                ObjectId.of("sfs-obj-0001-a1b2c3d4"),
                metadata(),
                new FileVersion(1, SHA, 7, T0),
                T0);
    }

    private FileMetadata metadata() {
        return new FileMetadata("notes.txt", "text/plain", 7, SHA, null, T0, T0);
    }

    @Nested
    @DisplayName("creation")
    class Creation {

        @Test
        @DisplayName("starts in REGISTERED with version 1 and no deletion origin")
        void startsRegisteredWithVersionOne() {
            SemanticFile file = initial();
            assertThat(file.state()).isEqualTo(FileState.REGISTERED);
            assertThat(file.deletedFrom()).isNull();
            assertThat(file.versions()).hasSize(1);
            assertThat(file.currentVersion().number()).isEqualTo(1);
            assertThat(file.certifiedDnaVersion()).isNull();
        }

        @Test
        @DisplayName("refuses an initial version other than 1")
        void refusesNonOneInitialVersion() {
            assertThatThrownBy(() -> SemanticFile.initial(
                    ObjectId.of("sfs-obj-0001-a1b2c3d4"), metadata(),
                    new FileVersion(2, SHA, 7, T0), T0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("refuses non-contiguous version lists")
        void refusesNonContiguousVersions() {
            assertThatThrownBy(() -> new SemanticFile(
                    ObjectId.of("sfs-obj-0001-a1b2c3d4"), metadata(), FileState.REGISTERED,
                    null, null, List.of(
                            new FileVersion(1, SHA, 7, T0),
                            new FileVersion(3, SHA, 7, T0)), T0))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("deletion origin")
    class DeletionOrigin {

        @Test
        @DisplayName("carries the origin only while soft-deleted")
        void carriesOriginOnlyWhileSoftDeleted() {
            SemanticFile deleted = initial().softDeletedFrom(FileState.ANALYZED, T1);
            assertThat(deleted.state()).isEqualTo(FileState.SOFT_DELETED);
            assertThat(deleted.deletedFrom()).isEqualTo(FileState.ANALYZED);

            SemanticFile restored = deleted.withDeletionCleared(FileState.ANALYZED, T1);
            assertThat(restored.state()).isEqualTo(FileState.ANALYZED);
            assertThat(restored.deletedFrom()).isNull();
        }

        @Test
        @DisplayName("refuses an inconsistent deletion origin")
        void refusesInconsistentOrigin() {
            assertThatThrownBy(() -> new SemanticFile(
                    ObjectId.of("sfs-obj-0001-a1b2c3d4"), metadata(), FileState.ANALYZED,
                    FileState.ANALYZED, null, List.of(new FileVersion(1, SHA, 7, T0)), T0))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> initial().softDeletedFrom(FileState.SOFT_DELETED, T1))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("immutability")
    class Immutability {

        @Test
        @DisplayName("versions cannot be modified through the aggregate")
        void versionsAreUnmodifiable() {
            SemanticFile file = initial();
            assertThatThrownBy(() -> file.versions().add(
                    new FileVersion(2, SHA, 7, T1)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("withers return new instances and leave the original untouched")
        void withersReturnNewInstances() {
            SemanticFile original = initial();
            SemanticFile moved = original.withState(FileState.ANALYZED, T1);
            assertThat(original.state()).isEqualTo(FileState.REGISTERED);
            assertThat(moved.state()).isEqualTo(FileState.ANALYZED);
            assertThat(moved.objectId()).isEqualTo(original.objectId());
        }
    }
}
