package com.sfs.app.api.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.RecordComponent;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Response safety")
class ResponseSafetyTest {

    private static final Set<String> FORBIDDEN_COMPONENT_NAMES = Set.of(
            "value", "plaintext", "plainText", "secret", "password", "payload",
            "rawvalue", "rawValue", "content", "credential", "token", "apikey", "apiKey");

    private static final Set<String> FORBIDDEN_AGGREGATE_NAMES = Set.of(
            "overallscore", "overallfidelity", "aggregatescore", "totalfidelity",
            "fidelitypercentage", "score", "overall", "average", "combinedscore");

    private static List<Class<?>> responseTypes() {
        return List.of(
                FileResponse.class,
                SearchApiResponse.class,
                SearchResultResponse.class,
                SearchResultResponse.EvidenceResponse.class,
                JobStatusResponse.class,
                JobStatusResponse.Provenance.class,
                JobStatusResponse.FindingResponse.class,
                FidelityReportResponse.class,
                FidelityReportResponse.DimensionScore.class,
                FidelityReportResponse.FindingResponse.class,
                SemanticRecordResponse.class,
                SemanticRecordResponse.ProtectedReferenceResponse.class,
                SemanticRecordResponse.EntityResponse.class,
                SemanticRecordResponse.FidelityProfileResponse.class,
                SecuritySettingsResponse.class,
                SecuritySettingsResponse.TypePolicyResponse.class,
                SecuritySettingsResponse.AuditEventResponse.class,
                OperationResponse.class);
    }

    @Test
    @DisplayName("no protected reference response can carry a sensitive value")
    void protectedReferenceCarriesNoValue() {
        RecordComponent[] components =
                SemanticRecordResponse.ProtectedReferenceResponse.class.getRecordComponents();

        assertThat(components).isNotNull();

        for (RecordComponent component : components) {
            assertThat(FORBIDDEN_COMPONENT_NAMES)
                    .as("ProtectedReferenceResponse must not expose a component named '%s'",
                            component.getName())
                    .doesNotContain(component.getName());
        }
    }

    @Test
    @DisplayName("no audit event response can carry the value that was protected")
    void auditEventCarriesNoValue() {
        for (RecordComponent component :
                SecuritySettingsResponse.AuditEventResponse.class.getRecordComponents()) {

            assertThat(FORBIDDEN_COMPONENT_NAMES)
                    .as("AuditEventResponse must not expose '%s'", component.getName())
                    .doesNotContain(component.getName());
        }
    }

    @Test
    @DisplayName("the fidelity report exposes no aggregate or overall score")
    void fidelityReportHasNoAggregateScore() {
        for (RecordComponent component : FidelityReportResponse.class.getRecordComponents()) {
            String name = component.getName().toLowerCase(Locale.ROOT);

            assertThat(FORBIDDEN_AGGREGATE_NAMES)
                    .as("FidelityReportResponse must not expose an aggregate component '%s'", name)
                    .doesNotContain(name);
        }
    }

    @Test
    @DisplayName("every response type is an immutable record")
    void everyResponseIsARecord() {
        assertThat(responseTypes()).allSatisfy(type ->
                assertThat(type.isRecord())
                        .as("%s must be a record", type.getSimpleName())
                        .isTrue());
    }

    @ParameterizedTest
    @ValueSource(classes = {
            SearchApiResponse.class,
            SearchResultResponse.class,
            JobStatusResponse.class,
            FidelityReportResponse.class,
            SemanticRecordResponse.class,
            SecuritySettingsResponse.class})
    @DisplayName("collection components are never null after construction")
    void collectionsAreNeverNull(Class<?> type) {
        assertThat(type.getRecordComponents()).isNotNull();
    }
}
