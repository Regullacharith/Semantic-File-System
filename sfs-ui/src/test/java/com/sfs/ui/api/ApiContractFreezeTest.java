package com.sfs.ui.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("API contract freeze")
class ApiContractFreezeTest {

    private static final List<Class<?>> API_CONTROLLERS = List.of(
            FileApiController.class,
            SearchApiController.class,
            ReconstructionApiController.class,
            EvaluationApiController.class,
            MetaApiController.class);

    private static final Set<String> FROZEN_OPERATIONS = Set.of(
            "GET /api/v1/files",
            "GET /api/v1/files/{objectId}",
            "POST /api/v1/files",
            "POST /api/v1/files/{objectId}/analyze",
            "DELETE /api/v1/files/{objectId}",
            "POST /api/v1/files/{objectId}/undo-delete",
            "POST /api/v1/files/{objectId}/memorize",
            "POST /api/v1/files/{objectId}/purge",
            "GET /api/v1/files/{objectId}/events",
            "GET /api/v1/objects/{objectId}/dna",
            "POST /api/v1/search",
            "GET /api/v1/search",
            "POST /api/v1/reconstructions",
            "GET /api/v1/reconstructions",
            "GET /api/v1/reconstructions/{jobId}",
            "GET /api/v1/reconstructions/{jobId}/artifact",
            "GET /api/v1/evaluations",
            "GET /api/v1/evaluations/{jobId}",
            "GET /api/v1/security/settings",
            "GET /api/v1/jobs/{jobId}",
            "GET /api/v1/meta/lifecycle",
            "GET /api/v1/health",
            "GET /api/v1/version");

    private static final Set<String> FROZEN_READ_OPERATIONS = Set.of(
            "GET /api/v1/files",
            "GET /api/v1/files/{objectId}",
            "GET /api/v1/files/{objectId}/events",
            "GET /api/v1/objects/{objectId}/dna",
            "GET /api/v1/search",
            "GET /api/v1/reconstructions",
            "GET /api/v1/reconstructions/{jobId}",
            "GET /api/v1/reconstructions/{jobId}/artifact",
            "GET /api/v1/evaluations",
            "GET /api/v1/evaluations/{jobId}",
            "GET /api/v1/security/settings",
            "GET /api/v1/jobs/{jobId}",
            "GET /api/v1/meta/lifecycle",
            "GET /api/v1/health",
            "GET /api/v1/version");

    private static Set<String> declaredOperations() {
        Set<String> operations = new LinkedHashSet<>();
        for (Class<?> controller : API_CONTROLLERS) {
            String prefix = controller.getAnnotation(
                    org.springframework.web.bind.annotation.RequestMapping.class).value()[0];
            for (Method method : controller.getDeclaredMethods()) {
                operations.addAll(mappingsOf(method, prefix));
            }
        }
        return operations;
    }

    private static List<String> mappingsOf(Method method, String prefix) {
        List<String> operations = new ArrayList<>();
        var get = method.getAnnotation(org.springframework.web.bind.annotation.GetMapping.class);
        var post = method.getAnnotation(org.springframework.web.bind.annotation.PostMapping.class);
        var delete = method.getAnnotation(org.springframework.web.bind.annotation.DeleteMapping.class);
        var put = method.getAnnotation(org.springframework.web.bind.annotation.PutMapping.class);
        var patch = method.getAnnotation(org.springframework.web.bind.annotation.PatchMapping.class);
        if (get != null) {
            addOperations(operations, "GET", prefix, get.value(), get.path());
        }
        if (post != null) {
            addOperations(operations, "POST", prefix, post.value(), post.path());
        }
        if (delete != null) {
            addOperations(operations, "DELETE", prefix, delete.value(), delete.path());
        }
        if (put != null) {
            addOperations(operations, "PUT", prefix, put.value(), put.path());
        }
        if (patch != null) {
            addOperations(operations, "PATCH", prefix, patch.value(), patch.path());
        }
        return operations;
    }

    private static void addOperations(List<String> operations, String verb, String prefix,
                                      String[] value, String[] path) {
        String[] targets = value.length > 0 ? value : path;
        if (targets.length == 0) {
            operations.add(verb + " " + prefix);
            return;
        }
        for (String target : targets) {
            operations.add(verb + " " + prefix + target);
        }
    }

    @Nested
    @DisplayName("operation inventory")
    class Operations {

        @Test
        @DisplayName("declares exactly the frozen operation set")
        void declaresFrozenOperations() {
            assertThat(declaredOperations())
                    .containsExactlyInAnyOrderElementsOf(FROZEN_OPERATIONS);
        }

        @Test
        @DisplayName("freezes twenty-three operations")
        void freezesTwentyThreeOperations() {
            assertThat(declaredOperations()).hasSize(23);
        }

        @Test
        @DisplayName("versions every path under /api/v1")
        void versionsEveryPath() {
            assertThat(declaredOperations()).allSatisfy(operation ->
                    assertThat(operation.split(" ")[1]).startsWith("/api/v1/"));
        }

        @Test
        @DisplayName("exposes deletion, restore, memorize and purge as separate operations")
        void deletionLifecycleIsExplicit() {
            assertThat(declaredOperations())
                    .contains(
                            "DELETE /api/v1/files/{objectId}",
                            "POST /api/v1/files/{objectId}/undo-delete",
                            "POST /api/v1/files/{objectId}/memorize",
                            "POST /api/v1/files/{objectId}/purge");
        }

        @Test
        @DisplayName("never routes the retired unguarded deletion path")
        void retiredDeletionPathStaysNonRoutable() {
            assertThat(declaredOperations()).noneMatch(operation ->
                    operation.contains("delete-raw"));
        }
    }

    @Nested
    @DisplayName("method safety")
    class MethodSafety {

        @Test
        @DisplayName("read operations are exactly the frozen GET set")
        void readsAreFrozenGetSet() {
            Set<String> reads = declaredOperations().stream()
                    .filter(operation -> operation.startsWith("GET "))
                    .collect(Collectors.toSet());
            assertThat(reads).containsExactlyInAnyOrderElementsOf(FROZEN_READ_OPERATIONS);
        }

        @Test
        @DisplayName("every state-changing operation uses a non-GET method")
        void mutationsNeverUseGet() {
            assertThat(declaredOperations())
                    .noneMatch(operation -> operation.startsWith("GET ")
                            && (operation.contains("/analyze")
                            || operation.contains("/undo-delete")
                            || operation.contains("/memorize")
                            || operation.contains("/purge")));
        }
    }
}
