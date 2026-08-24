package com.sfs.app.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("API contract freeze")
class ApiContractFreezeTest {

    private static final Path SPEC = Path.of("..", "docs", "API_SPECIFICATION.md");

    private static final Pattern OPERATION_ROW = Pattern.compile(
            "^\\|\\s*(\\d+[a-z]?)\\s*\\|\\s*`(GET|POST|DELETE)`\\s*\\|\\s*`(/api/v1[^`]*)`",
            Pattern.MULTILINE);

    private static final Pattern ERROR_CODE_ROW = Pattern.compile(
            "^\\|\\s*`([A-Z_]+)`\\s*\\|\\s*(\\d{3})\\s*\\|", Pattern.MULTILINE);

    private static final List<String> FROZEN_OPERATIONS = List.of(
            "GET /api/v1/files",
            "GET /api/v1/files/{objectId}",
            "POST /api/v1/files",
            "POST /api/v1/files/{objectId}/analyze",
            "DELETE /api/v1/files/{objectId}",
            "POST /api/v1/files/{objectId}/undo-delete",
            "POST /api/v1/files/{objectId}/purge",
            "GET /api/v1/objects/{objectId}/dna",
            "POST /api/v1/search",
            "POST /api/v1/reconstructions",
            "GET /api/v1/reconstructions",
            "GET /api/v1/reconstructions/{jobId}",
            "GET /api/v1/reconstructions/{jobId}/artifact",
            "GET /api/v1/evaluations",
            "GET /api/v1/evaluations/{jobId}",
            "GET /api/v1/security/settings",
            "GET /api/v1/jobs/{jobId}",
            "GET /api/v1/health",
            "GET /api/v1/version",
            "GET /api/v1/search");

    private static final List<String> FROZEN_ERROR_CODES = List.of(
            "VALIDATION_FAILED",
            "OBJECT_ID_INVALID",
            "JOB_ID_INVALID",
            "REQUEST_MALFORMED",
            "FILE_NOT_FOUND",
            "JOB_NOT_FOUND",
            "EVALUATION_NOT_FOUND",
            "ARTIFACT_NOT_AVAILABLE",
            "METHOD_NOT_ALLOWED",
            "INVALID_STATE_TRANSITION",
            "PAYLOAD_TOO_LARGE",
            "UNSUPPORTED_MEDIA_TYPE",
            "RECONSTRUCTION_REFUSED",
            "INTERNAL_ERROR",
            "CONFIRMATION_REQUIRED",
            "CONFIRMATION_MISMATCH",
            "AUTHENTICATION_REQUIRED",
            "NOT_PERMITTED");

    private String spec() throws IOException {
        assertThat(SPEC)
                .as("the frozen API specification must exist at %s", SPEC.toAbsolutePath())
                .exists();
        return Files.readString(SPEC, StandardCharsets.UTF_8);
    }

    private Set<String> declaredOperations() throws IOException {
        Set<String> operations = new LinkedHashSet<>();
        Matcher matcher = OPERATION_ROW.matcher(spec());
        while (matcher.find()) {
            operations.add(matcher.group(2) + " " + matcher.group(3));
        }
        return operations;
    }

    @Nested
    @DisplayName("operation inventory")
    class Operations {

        @Test
        @DisplayName("declares exactly the frozen operation set")
        void declaresFrozenOperations() throws IOException {
            assertThat(declaredOperations())
                    .containsExactlyInAnyOrderElementsOf(FROZEN_OPERATIONS);
        }

        @Test
        @DisplayName("freezes twenty operations")
        void freezesTwentyOperations() throws IOException {
            assertThat(declaredOperations()).hasSize(20);
        }

        @Test
        @DisplayName("versions every path")
        void versionsEveryPath() throws IOException {
            assertThat(declaredOperations())
                    .allSatisfy(operation ->
                            assertThat(operation).contains(" /api/v1"));
        }

        @Test
        @DisplayName("exposes no method other than GET, POST or DELETE")
        void exposesOnlyExpectedMethods() throws IOException {
            assertThat(declaredOperations())
                    .allSatisfy(operation -> assertThat(operation)
                            .matches("^(GET|POST|DELETE) /api/v1.*"));
        }

        @Test
        @DisplayName("declares no single-call endpoint that destroys raw bytes")
        void declaresNoUnguardedDestructiveEndpoint() throws IOException {
            assertThat(declaredOperations())
                    .noneMatch(operation -> operation.endsWith("/delete-raw"));
        }

        @Test
        @DisplayName("declares deletion, restore and purge as separate operations")
        void deletionRestoreAndPurgeAreSeparate() throws IOException {
            assertThat(declaredOperations())
                    .contains("DELETE /api/v1/files/{objectId}")
                    .contains("POST /api/v1/files/{objectId}/undo-delete")
                    .contains("POST /api/v1/files/{objectId}/purge");
        }

        @Test
        @DisplayName("offers no endpoint that would edit security policy")
        void offersNoSecurityPolicyWrite() throws IOException {
            assertThat(declaredOperations())
                    .noneMatch(operation -> operation.startsWith("POST /api/v1/security"));
        }

        @Test
        @DisplayName("records accepted risks and deferred work")
        void recordsRisks() throws IOException {
            String spec = spec();

            assertThat(spec).contains("Accepted risks");
            assertThat(spec).contains("Search query text in URLs");
            assertThat(spec).contains("Deliberately deferred");
        }

        @Test
        @DisplayName("keeps every state change on POST")
        void keepsStateChangesOnPost() throws IOException {
            List<String> mutating = List.of("analyze", "purge", "undo-delete", "reconstructions");

            assertThat(declaredOperations())
                    .filteredOn(operation -> operation.startsWith("GET "))
                    .allSatisfy(operation -> {
                        boolean isBareList = operation.equals("GET /api/v1/reconstructions");
                        boolean isSubResource = operation.contains("{jobId}");
                        if (!isBareList && !isSubResource) {
                            assertThat(mutating)
                                    .noneMatch(verb -> operation.endsWith("/" + verb));
                        }
                    });
        }
    }

    @Nested
    @DisplayName("error model")
    class ErrorModel {

        @Test
        @DisplayName("declares exactly the frozen error codes")
        void declaresFrozenErrorCodes() throws IOException {
            Set<String> codes = new LinkedHashSet<>();
            Matcher matcher = ERROR_CODE_ROW.matcher(spec());
            while (matcher.find()) {
                codes.add(matcher.group(1));
            }

            assertThat(codes).containsExactlyInAnyOrderElementsOf(FROZEN_ERROR_CODES);
        }

        @Test
        @DisplayName("defines the mandatory error fields")
        void definesMandatoryErrorFields() throws IOException {
            String spec = spec();

            for (String field : List.of("code", "message", "status", "timestamp", "path", "details")) {
                assertThat(spec)
                        .as("error field %s must be specified", field)
                        .contains("`" + field + "`");
            }
        }

        @Test
        @DisplayName("separates a policy refusal from a lifecycle conflict")
        void separatesRefusalFromConflict() throws IOException {
            String spec = spec();

            assertThat(spec).contains("RECONSTRUCTION_REFUSED");
            assertThat(spec).contains("INVALID_STATE_TRANSITION");
            assertThat(spec).contains("422");
            assertThat(spec).contains("409");
        }
    }

    @Nested
    @DisplayName("safety commitments")
    class Safety {

        @Test
        @DisplayName("commits to exposing no aggregate fidelity score")
        void noAggregateFidelityScore() throws IOException {
            assertThat(spec()).contains("no aggregate or overall score field");
        }

        @Test
        @DisplayName("commits to keeping plaintext values out of responses")
        void noPlaintextInResponses() throws IOException {
            assertThat(spec()).contains("no plaintext value");
        }

        @Test
        @DisplayName("commits to keeping request bodies out of logs")
        void noRequestBodiesInLogs() throws IOException {
            assertThat(spec()).contains("Never** body content");
        }

        @Test
        @DisplayName("states that destructive operations require authentication and authorization")
        void statesSecurityPosture() throws IOException {
            String spec = spec();

            assertThat(spec).contains("127.0.0.1");
            assertThat(spec).contains("M13");
            assertThat(spec).contains("Required for all destructive operations");
        }

        @Test
        @DisplayName("does not claim loopback binding is the security model")
        void doesNotClaimLoopbackIsSecurity() throws IOException {
            String spec = spec();

            assertThat(spec).contains("deployment restriction, not the security");
            assertThat(spec)
                    .as("the superseded justification must not survive anywhere in the spec")
                    .doesNotContain("acceptable *only* because it is bound to loopback");
        }

        @Test
        @DisplayName("prohibits a job vanishing without an explicit status")
        void prohibitsSilentJobLoss() throws IOException {
            assertThat(spec()).contains("Silent disappearance is prohibited");
        }
    }
}
