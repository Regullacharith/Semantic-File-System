package com.sfs.ui.delivery;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Obsolete delivered artifacts are absent from the working tree")
class ObsoleteDeliveryArtifactsTest {

    private static final List<Path> MUST_BE_DELETED = List.of(
            Path.of("sfs-ui", "src", "main", "java", "com", "sfs", "ui", "mock",
                    "MockFileService.java"),
            Path.of("sfs-ui", "src", "test", "java", "com", "sfs", "ui", "mock",
                    "MockFileServiceTest.java"),
            Path.of("sfs-ui", "src", "main", "java", "com", "sfs", "ui", "mock",
                    "StubAnalysisEngine.java"),
            Path.of("sfs-ui", "src", "main", "java", "com", "sfs", "ui", "mock",
                    "MockSemanticRecordService.java"),
            Path.of("sfs-ui", "src", "test", "java", "com", "sfs", "ui", "mock",
                    "MockSemanticRecordServiceTest.java"),
            Path.of("sfs-ui", "src", "main", "java", "com", "sfs", "ui", "mock",
                    "DevDataSeeder.java"),
            Path.of("sfs-app", "src", "test", "java", "com", "sfs", "app", "api",
                    "ApiContractFreezeTest.java"),
            Path.of("sfs-engine", "src", "main", "java", "com", "sfs", "engine", "core",
                    "ContentSource.java"),
            Path.of("sfs-engine", "src", "main", "java", "com", "sfs", "engine", "inspect",
                    "FileInspector.java"),
            Path.of("sfs-engine", "src", "main", "java", "com", "sfs", "engine", "inspect",
                    "InspectionReport.java"),
            Path.of("sfs-engine", "src", "test", "java", "com", "sfs", "engine", "inspect",
                    "FileInspectorTest.java"));

    @Test
    @DisplayName("files removed by later milestones are not present; delete the listed files")
    void removedFilesAreAbsent() {
        Path projectRoot = projectRoot();
        List<String> stillPresent = MUST_BE_DELETED.stream()
                .map(projectRoot::resolve)
                .filter(Files::exists)
                .map(path -> projectRoot.relativize(path).toString())
                .toList();

        assertThat(stillPresent)
                .as("zip archives cannot delete files, so these removed artifacts must be "
                        + "deleted manually before the build can run")
                .isEmpty();
    }

    private static Path projectRoot() {
        Path candidate = Path.of("").toAbsolutePath();
        for (int i = 0; i < 4 && candidate != null; i++) {
            if (Files.isDirectory(candidate.resolve("sfs-ui"))
                    && Files.isDirectory(candidate.resolve("sfs-app"))) {
                return candidate;
            }
            candidate = candidate.getParent();
        }
        return Path.of("").toAbsolutePath();
    }
}
