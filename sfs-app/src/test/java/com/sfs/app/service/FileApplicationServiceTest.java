package com.sfs.app.service;

import com.sfs.app.api.error.ApiErrorCode;
import com.sfs.app.api.request.FileImportApiRequest;
import com.sfs.app.api.response.SemanticRecordResponse;
import com.sfs.contracts.file.DeletionConfirmation;
import com.sfs.contracts.file.FileImportRequest;
import com.sfs.contracts.file.FileOperationResult;
import com.sfs.contracts.file.FileService;
import com.sfs.contracts.file.FileStatus;
import com.sfs.contracts.file.FileSummary;
import com.sfs.contracts.security.AuthenticationService;
import com.sfs.contracts.security.AuthorizationService;
import com.sfs.contracts.security.Capability;
import com.sfs.contracts.security.Principal;
import com.sfs.contracts.semantic.SemanticDnaView;
import com.sfs.contracts.lifecycle.LifecycleAuditService;
import com.sfs.contracts.semantic.SemanticRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("File application service")
class FileApplicationServiceTest {

    private static final String ANALYZED_ID = "sfs-obj-0001-a1b2c3d4";
    private static final String REGISTERED_ID = "sfs-obj-0003-c9d0e1f2";
    private static final String MEMORIZED_ID = "sfs-obj-0002-e5f6a7b8";
    private static final String DELETED_ID = "sfs-obj-0005-d1e2f3a4";

    private static final String CUSTODIAN = "custodian";
    private static final String OPERATOR = "operator";
    private static final String READER = "reader";

    private StubFileService fileService;
    private FileApplicationService service;

    @BeforeEach
    void setUp() {
        fileService = new StubFileService();
        service = new FileApplicationService(
                fileService,
                new StubSemanticRecordService(),
                new StubAuthenticationService(),
                new StubAuthorizationService(),
                new StubLifecycleAuditService());
    }

    private static DeletionConfirmation confirm(String objectId) {
        return new DeletionConfirmation(objectId);
    }

    private static ApiErrorCode codeOf(Throwable t) {
        return ((ApplicationException) t).errorCode();
    }

    @Nested
    @DisplayName("authentication")
    class Authentication {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "unknown-caller"})
        @DisplayName("rejects deletion without a valid credential")
        void rejectsUnauthenticatedDeletion(String credential) {
            assertThatThrownBy(() ->
                    service.softDelete(ANALYZED_ID, credential, confirm(ANALYZED_ID)))
                    .isInstanceOf(ApplicationException.class)
                    .extracting(FileApplicationServiceTest::codeOf)
                    .isEqualTo(ApiErrorCode.AUTHENTICATION_REQUIRED);
        }

        @Test
        @DisplayName("rejects purge without a valid credential")
        void rejectsUnauthenticatedPurge() {
            assertThatThrownBy(() ->
                    service.purgeRawData(DELETED_ID, null, confirm(DELETED_ID)))
                    .isInstanceOf(ApplicationException.class)
                    .extracting(FileApplicationServiceTest::codeOf)
                    .isEqualTo(ApiErrorCode.AUTHENTICATION_REQUIRED);
        }

        @Test
        @DisplayName("rejects undo without a valid credential")
        void rejectsUnauthenticatedUndo() {
            assertThatThrownBy(() -> service.undoDelete(DELETED_ID, null))
                    .isInstanceOf(ApplicationException.class)
                    .extracting(FileApplicationServiceTest::codeOf)
                    .isEqualTo(ApiErrorCode.AUTHENTICATION_REQUIRED);
        }

        @Test
        @DisplayName("never reaches the domain when authentication fails")
        void unauthenticatedRequestNeverReachesDomain() {
            assertThatThrownBy(() ->
                    service.softDelete(ANALYZED_ID, null, confirm(ANALYZED_ID)))
                    .isInstanceOf(ApplicationException.class);

            assertThat(fileService.softDeletes).isEmpty();
            assertThat(fileService.lookups).isEmpty();
        }

        @Test
        @DisplayName("allows an authenticated and authorized deletion to proceed")
        void allowsAuthenticatedDeletion() {
            assertThat(service.softDelete(ANALYZED_ID, OPERATOR, confirm(ANALYZED_ID))
                    .successful()).isTrue();
        }
    }

    @Nested
    @DisplayName("authorization")
    class Authorization {

        @Test
        @DisplayName("rejects deletion by an authenticated caller without the capability")
        void rejectsUnauthorizedDeletion() {
            assertThatThrownBy(() ->
                    service.softDelete(ANALYZED_ID, READER, confirm(ANALYZED_ID)))
                    .isInstanceOf(ApplicationException.class)
                    .extracting(FileApplicationServiceTest::codeOf)
                    .isEqualTo(ApiErrorCode.NOT_PERMITTED);
        }

        @Test
        @DisplayName("does not treat permission to delete as permission to purge")
        void deleteCapabilityDoesNotImplyPurge() {
            assertThat(service.softDelete(ANALYZED_ID, OPERATOR, confirm(ANALYZED_ID))
                    .successful()).isTrue();

            assertThatThrownBy(() ->
                    service.purgeRawData(ANALYZED_ID, OPERATOR, confirm(ANALYZED_ID)))
                    .isInstanceOf(ApplicationException.class)
                    .extracting(FileApplicationServiceTest::codeOf)
                    .isEqualTo(ApiErrorCode.NOT_PERMITTED);
        }

        @Test
        @DisplayName("permits purge for a caller holding the purge capability")
        void permitsAuthorizedPurge() {
            assertThat(service.purgeRawData(DELETED_ID, CUSTODIAN, confirm(DELETED_ID))
                    .successful()).isTrue();
        }

        @Test
        @DisplayName("never reaches the domain when authorization fails")
        void unauthorizedRequestNeverReachesDomain() {
            assertThatThrownBy(() ->
                    service.purgeRawData(DELETED_ID, OPERATOR, confirm(DELETED_ID)))
                    .isInstanceOf(ApplicationException.class);

            assertThat(fileService.purges).isEmpty();
        }
    }

    @Nested
    @DisplayName("confirmation")
    class Confirmation {

        @Test
        @DisplayName("rejects deletion with no confirmation")
        void rejectsMissingConfirmation() {
            assertThatThrownBy(() -> service.softDelete(ANALYZED_ID, OPERATOR, null))
                    .isInstanceOf(ApplicationException.class)
                    .extracting(FileApplicationServiceTest::codeOf)
                    .isEqualTo(ApiErrorCode.CONFIRMATION_REQUIRED);
        }

        @Test
        @DisplayName("rejects a confirmation naming a different object")
        void rejectsMismatchedConfirmation() {
            assertThatThrownBy(() ->
                    service.softDelete(ANALYZED_ID, OPERATOR, confirm(MEMORIZED_ID)))
                    .isInstanceOf(ApplicationException.class)
                    .extracting(FileApplicationServiceTest::codeOf)
                    .isEqualTo(ApiErrorCode.CONFIRMATION_MISMATCH);
        }

        @Test
        @DisplayName("rejects purge with no confirmation")
        void rejectsPurgeWithoutConfirmation() {
            assertThatThrownBy(() -> service.purgeRawData(DELETED_ID, CUSTODIAN, null))
                    .isInstanceOf(ApplicationException.class)
                    .extracting(FileApplicationServiceTest::codeOf)
                    .isEqualTo(ApiErrorCode.CONFIRMATION_REQUIRED);
        }

        @Test
        @DisplayName("accepts a confirmation naming the correct object")
        void acceptsMatchingConfirmation() {
            assertThat(service.softDelete(ANALYZED_ID, OPERATOR, confirm(ANALYZED_ID))
                    .successful()).isTrue();
        }

        @Test
        @DisplayName("changes nothing when confirmation is missing")
        void missingConfirmationChangesNothing() {
            assertThatThrownBy(() -> service.softDelete(ANALYZED_ID, OPERATOR, null))
                    .isInstanceOf(ApplicationException.class);

            assertThat(fileService.softDeletes).isEmpty();
        }

        @Test
        @DisplayName("does not require confirmation to restore, because restoring is safe")
        void undoNeedsNoConfirmation() {
            assertThat(service.undoDelete(DELETED_ID, OPERATOR).successful()).isTrue();
        }
    }

    @Nested
    @DisplayName("reversible deletion")
    class ReversibleDeletion {

        @Test
        @DisplayName("moves an analyzed object to the soft-deleted state")
        void softDeletesAnalyzedObject() {
            service.softDelete(ANALYZED_ID, OPERATOR, confirm(ANALYZED_ID));

            assertThat(fileService.statusOf(ANALYZED_ID)).isEqualTo(FileStatus.SOFT_DELETED);
        }

        @Test
        @DisplayName("does not permanently remove raw data")
        void doesNotRemoveRawData() {
            service.softDelete(ANALYZED_ID, OPERATOR, confirm(ANALYZED_ID));

            assertThat(fileService.statusOf(ANALYZED_ID).isRawDataRemoved()).isFalse();
            assertThat(fileService.purges).isEmpty();
        }

        @Test
        @DisplayName("restores a soft-deleted object to analyzed")
        void restoresSoftDeletedObject() {
            service.undoDelete(DELETED_ID, OPERATOR);

            assertThat(fileService.statusOf(DELETED_ID)).isEqualTo(FileStatus.ANALYZED);
        }

        @Test
        @DisplayName("survives a delete and restore round trip")
        void roundTripsDeleteAndRestore() {
            service.softDelete(ANALYZED_ID, OPERATOR, confirm(ANALYZED_ID));
            service.undoDelete(ANALYZED_ID, OPERATOR);

            assertThat(fileService.statusOf(ANALYZED_ID)).isEqualTo(FileStatus.ANALYZED);
        }
    }

    @Nested
    @DisplayName("permanent purge")
    class PermanentPurge {

        @Test
        @DisplayName("releases raw bytes and keeps the semantic record")
        void purgeReleasesRawBytes() {
            service.purgeRawData(DELETED_ID, CUSTODIAN, confirm(DELETED_ID));

            assertThat(fileService.statusOf(DELETED_ID)).isEqualTo(FileStatus.MEMORIZED);
            assertThat(fileService.statusOf(DELETED_ID).isRawDataRemoved()).isTrue();
        }

        @Test
        @DisplayName("refuses to purge a live analyzed object in one step")
        void refusesSingleStepPurge() {
            assertThatThrownBy(() ->
                    service.purgeRawData(ANALYZED_ID, CUSTODIAN, confirm(ANALYZED_ID)))
                    .isInstanceOf(ApplicationException.class)
                    .extracting(FileApplicationServiceTest::codeOf)
                    .isEqualTo(ApiErrorCode.INVALID_STATE_TRANSITION);
        }

        @Test
        @DisplayName("refuses to restore an object after purge")
        void refusesUndoAfterPurge() {
            service.purgeRawData(DELETED_ID, CUSTODIAN, confirm(DELETED_ID));

            assertThatThrownBy(() -> service.undoDelete(DELETED_ID, OPERATOR))
                    .isInstanceOf(ApplicationException.class)
                    .extracting(FileApplicationServiceTest::codeOf)
                    .isEqualTo(ApiErrorCode.INVALID_STATE_TRANSITION);
        }

        @Test
        @DisplayName("refuses a repeated purge")
        void refusesRepeatedPurge() {
            service.purgeRawData(DELETED_ID, CUSTODIAN, confirm(DELETED_ID));

            assertThatThrownBy(() ->
                    service.purgeRawData(DELETED_ID, CUSTODIAN, confirm(DELETED_ID)))
                    .isInstanceOf(ApplicationException.class)
                    .extracting(FileApplicationServiceTest::codeOf)
                    .isEqualTo(ApiErrorCode.INVALID_STATE_TRANSITION);
        }
    }

    @Nested
    @DisplayName("lifecycle validation")
    class LifecycleValidation {

        @Test
        @DisplayName("refuses to delete an unanalyzed object")
        void refusesDeletingUnanalyzedObject() {
            assertThatThrownBy(() ->
                    service.softDelete(REGISTERED_ID, OPERATOR, confirm(REGISTERED_ID)))
                    .isInstanceOf(ApplicationException.class)
                    .extracting(FileApplicationServiceTest::codeOf)
                    .isEqualTo(ApiErrorCode.INVALID_STATE_TRANSITION);
        }

        @Test
        @DisplayName("refuses a repeated deletion")
        void refusesRepeatedDeletion() {
            service.softDelete(ANALYZED_ID, OPERATOR, confirm(ANALYZED_ID));

            assertThatThrownBy(() ->
                    service.softDelete(ANALYZED_ID, OPERATOR, confirm(ANALYZED_ID)))
                    .isInstanceOf(ApplicationException.class)
                    .extracting(FileApplicationServiceTest::codeOf)
                    .isEqualTo(ApiErrorCode.INVALID_STATE_TRANSITION);
        }

        @Test
        @DisplayName("refuses to restore an object that was never deleted")
        void refusesRestoringLiveObject() {
            assertThatThrownBy(() -> service.undoDelete(ANALYZED_ID, OPERATOR))
                    .isInstanceOf(ApplicationException.class)
                    .extracting(FileApplicationServiceTest::codeOf)
                    .isEqualTo(ApiErrorCode.INVALID_STATE_TRANSITION);
        }

        @Test
        @DisplayName("reports an unknown object as not found once the caller is permitted")
        void reportsUnknownObject() {
            assertThatThrownBy(() -> service.softDelete(
                    "sfs-obj-9999-nothere", OPERATOR, confirm("sfs-obj-9999-nothere")))
                    .isInstanceOf(ApplicationException.class)
                    .extracting(FileApplicationServiceTest::codeOf)
                    .isEqualTo(ApiErrorCode.FILE_NOT_FOUND);
        }

        @Test
        @DisplayName("rejects a malformed Object ID before touching the domain")
        void rejectsMalformedObjectId() {
            assertThatThrownBy(() -> service.softDelete(
                    "../../etc/passwd", OPERATOR, confirm("../../etc/passwd")))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(fileService.softDeletes).isEmpty();
        }
    }

    @Nested
    @DisplayName("reads and import")
    class ReadsAndImport {

        @Test
        @DisplayName("lists every known file")
        void listsFiles() {
            assertThat(service.listFiles()).hasSize(4);
        }

        @Test
        @DisplayName("returns one file by Object ID")
        void returnsOneFile() {
            assertThat(service.getFile(ANALYZED_ID).objectId()).isEqualTo(ANALYZED_ID);
        }

        @Test
        @DisplayName("imports a text file without triggering analysis")
        void importsWithoutAnalysis() {
            var response = service.importFile(
                    new FileImportApiRequest("notes.txt", "Some content.", "text/plain"));

            assertThat(response.successful()).isTrue();
            assertThat(fileService.analysisRequests).isEmpty();
        }

        @Test
        @DisplayName("reports absent Semantic DNA as a state, not a missing resource")
        void reportsAbsentDnaAsState() {
            SemanticRecordResponse response = service.getSemanticRecord(REGISTERED_ID);

            assertThat(response.present()).isFalse();
            assertThat(response.reason()).contains("analysis");
        }
    }

    @Nested
    @DisplayName("memorization")
    class Memorization {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "unknown-caller"})
        @DisplayName("rejects memorization without a valid credential")
        void rejectsUnauthenticatedMemorization(String credential) {
            assertThatThrownBy(() -> service.memorize(ANALYZED_ID, credential))
                    .isInstanceOf(ApplicationException.class)
                    .extracting(FileApplicationServiceTest::codeOf)
                    .isEqualTo(ApiErrorCode.AUTHENTICATION_REQUIRED);
        }

        @Test
        @DisplayName("rejects memorization for a principal without the capability")
        void rejectsUnauthorizedMemorization() {
            assertThatThrownBy(() -> service.memorize(ANALYZED_ID, READER))
                    .isInstanceOf(ApplicationException.class)
                    .extracting(FileApplicationServiceTest::codeOf)
                    .isEqualTo(ApiErrorCode.NOT_PERMITTED);

            assertThat(fileService.memorizes).isEmpty();
        }

        @Test
        @DisplayName("memorizes an analyzed object for an authorized principal")
        void memorizesAnalyzedObject() {
            var response = service.memorize(ANALYZED_ID, OPERATOR);

            assertThat(response.successful()).isTrue();
            assertThat(fileService.memorizes).containsExactly(ANALYZED_ID);
        }

        @Test
        @DisplayName("refuses memorization for a registered object")
        void refusesMemorizationOfRegisteredObject() {
            assertThatThrownBy(() -> service.memorize(REGISTERED_ID, OPERATOR))
                    .isInstanceOf(ApplicationException.class)
                    .extracting(FileApplicationServiceTest::codeOf)
                    .isEqualTo(ApiErrorCode.INVALID_STATE_TRANSITION);

            assertThat(fileService.memorizes).isEmpty();
        }
    }

    private static final class StubLifecycleAuditService implements LifecycleAuditService {

        @Override
        public List<com.sfs.contracts.lifecycle.LifecycleAuditEntry> eventsFor(String objectId) {
            return List.of();
        }

        @Override
        public com.sfs.contracts.lifecycle.LifecycleStatistics statistics() {
            return new com.sfs.contracts.lifecycle.LifecycleStatistics(
                    0, Map.of(), 0, 0, 0, 0, 0, null, null, null, null);
        }
    }

    private static final class StubFileService implements FileService {

        private final Map<String, FileSummary> files = new LinkedHashMap<>();
        private final List<String> analysisRequests = new ArrayList<>();
        private final List<String> softDeletes = new ArrayList<>();
        private final List<String> purges = new ArrayList<>();
        private final List<String> memorizes = new ArrayList<>();
        private final List<String> lookups = new ArrayList<>();

        private StubFileService() {
            put(ANALYZED_ID, FileStatus.ANALYZED);
            put(REGISTERED_ID, FileStatus.REGISTERED);
            put(MEMORIZED_ID, FileStatus.MEMORIZED);
            put(DELETED_ID, FileStatus.SOFT_DELETED);
        }

        private void put(String objectId, FileStatus status) {
            files.put(objectId, new FileSummary(objectId, objectId + ".txt", status,
                    100L, Instant.parse("2026-01-01T00:00:00Z"),
                    status == FileStatus.REGISTERED ? null : Instant.parse("2026-01-02T00:00:00Z")));
        }

        private FileStatus statusOf(String objectId) {
            return files.get(objectId).status();
        }

        private void transition(String objectId, FileStatus status) {
            put(objectId, status);
        }

        @Override
        public List<FileSummary> listFiles() {
            return List.copyOf(files.values());
        }

        @Override
        public Optional<FileSummary> findByObjectId(String objectId) {
            lookups.add(objectId);
            return Optional.ofNullable(files.get(objectId));
        }

        @Override
        public FileOperationResult importFile(FileImportRequest request) {
            return FileOperationResult.success("sfs-obj-0009-newfile01", "Imported.");
        }

        @Override
        public FileOperationResult requestAnalysis(String objectId) {
            analysisRequests.add(objectId);
            return FileOperationResult.success(objectId, "Analysis requested.");
        }

        @Override
        public FileOperationResult memorize(String objectId, Principal principal) {
            memorizes.add(objectId);
            transition(objectId, FileStatus.MEMORY_COMMITTED);
            return FileOperationResult.success(objectId, "Semantic memory committed.");
        }

        @Override
        public FileOperationResult softDelete(String objectId, Principal principal) {
            softDeletes.add(objectId);
            transition(objectId, FileStatus.SOFT_DELETED);
            return FileOperationResult.success(objectId, "Deleted. Raw bytes retained.");
        }

        @Override
        public FileOperationResult undoDelete(String objectId, Principal principal) {
            transition(objectId, FileStatus.ANALYZED);
            return FileOperationResult.success(objectId, "Restored.");
        }

        @Override
        public FileOperationResult purgeRawData(String objectId, Principal principal) {
            purges.add(objectId);
            transition(objectId, FileStatus.MEMORIZED);
            return FileOperationResult.success(objectId, "Raw bytes released.");
        }
    }

    private static final class StubAuthenticationService implements AuthenticationService {

        @Override
        public Optional<Principal> authenticate(String credential) {
            if (credential == null || credential.isBlank()) {
                return Optional.empty();
            }

            return switch (credential) {
                case READER -> Optional.of(new Principal(
                        "reader", "Reader", Set.of(Capability.READ)));
                case OPERATOR -> Optional.of(new Principal(
                        "operator", "Operator",
                        Set.of(Capability.READ, Capability.WRITE, Capability.MEMORIZE,
                                Capability.DELETE_RAW, Capability.UNDO_DELETE)));
                case CUSTODIAN -> Optional.of(new Principal(
                        "custodian", "Custodian",
                        Set.of(Capability.READ, Capability.WRITE, Capability.MEMORIZE,
                                Capability.DELETE_RAW, Capability.UNDO_DELETE, Capability.PURGE_RAW)));
                default -> Optional.empty();
            };
        }
    }

    private static final class StubAuthorizationService implements AuthorizationService {

        @Override
        public boolean isPermitted(Principal principal, Capability capability) {
            return principal.has(capability);
        }
    }

    private static final class StubSemanticRecordService implements SemanticRecordService {

        @Override
        public Optional<SemanticDnaView> findSemanticDna(String objectId) {
            if (!ANALYZED_ID.equals(objectId)) {
                return Optional.empty();
            }

            return Optional.of(new SemanticDnaView(
                    objectId, "sfs-dna/0.1", 1, "Summary.",
                    List.of("concept"), List.of("topic"),
                    List.of(), List.of(), List.of(), List.of(), List.of(),
                    384,
                    new SemanticDnaView.FidelityProfileView(0.9, 0.9, "analyzer/0.1")));
        }
    }
}
