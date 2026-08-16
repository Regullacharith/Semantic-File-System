package com.sfs.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Enforces the Milestone 01 architectural boundary as an executable rule.
 *
 * <p>The specification states plainly: <em>"The UI must never directly manipulate the Memory
 * Database"</em> and <em>"No semantic logic is implemented in UI"</em>. Those are review
 * instructions in the PDF; this test turns them into a build failure so a violation cannot
 * survive to the commit stage unnoticed.
 *
 * <p>The check is deliberately simple and dependency-free: it scans the UI module's own
 * source files for imports of backend packages. It is a guard rail, not a formal
 * architecture verification tool. Milestone 14 may replace it with ArchUnit, which would be
 * a justified new test dependency at that point.
 *
 * <p><strong>Why there is a self-test.</strong> A rule that cannot fail is not a rule. The
 * backend packages listed below do not exist yet, so the production scan currently passes
 * trivially. {@link #detectorFlagsAForbiddenImport()} feeds the detector a known-bad source
 * file and asserts that it is flagged, proving the detector actually works before there is
 * any real code for it to guard.
 */
@DisplayName("UI architectural boundary")
class ArchitectureBoundaryTest {

    /**
     * Packages the UI layer is forbidden to import directly.
     * The UI must reach these subsystems only through {@code com.sfs.contracts} interfaces.
     */
    private static final List<String> FORBIDDEN_IMPORTS = List.of(
            "com.sfs.memory",          // Memory Database       - M08
            "com.sfs.engine",          // Semantic Engine       - M04
            "com.sfs.adapters",        // Adapter Framework     - M05
            "com.sfs.rules",           // Reconstruction Rules  - M07
            "com.sfs.search",          // Semantic Search       - M09
            "com.sfs.model",           // Reconstruction Model  - M10
            "com.sfs.reconstruction",  // Reconstruction Engine - M11
            "com.sfs.evaluation",      // Evaluation & Fidelity - M12
            "com.sfs.security",        // Security & Privacy    - M13
            "java.sql",                // no direct database access from the UI
            "javax.sql"
    );

    private static final Path UI_SOURCE_ROOT = Path.of("src", "main", "java");

    @Test
    @DisplayName("production UI sources import no backend subsystem packages")
    void uiDoesNotImportBackendSubsystems() throws IOException {
        assertThat(UI_SOURCE_ROOT)
                .as("UI source root must exist, otherwise this test silently passes on nothing")
                .exists();

        List<String> violations = scanForViolations(UI_SOURCE_ROOT);

        assertThat(violations)
                .as("""
                    The UI layer must not depend directly on backend subsystems.
                    Route the call through a service interface in com.sfs.contracts instead.
                    See Milestone 01: "The UI must never directly manipulate the Memory Database."
                    """)
                .isEmpty();
    }

    @Test
    @DisplayName("detector actually flags a forbidden import (self-test)")
    void detectorFlagsAForbiddenImport(@TempDir Path tempDir) throws IOException {
        Path offending = tempDir.resolve("Offender.java");
        Files.writeString(offending, """
                package com.sfs.ui;

                import com.sfs.memory.SemanticRecordRepository;

                public class Offender { }
                """);

        List<String> violations = scanForViolations(tempDir);

        assertThat(violations)
                .as("detector must catch a direct Memory Database import")
                .hasSize(1)
                .first(org.assertj.core.api.InstanceOfAssertFactories.STRING)
                .contains("com.sfs.memory");
    }

    @Test
    @DisplayName("detector does not flag legitimate imports (no false positives)")
    void detectorAllowsPermittedImports(@TempDir Path tempDir) throws IOException {
        Path compliant = tempDir.resolve("Compliant.java");
        Files.writeString(compliant, """
                package com.sfs.ui;

                import com.sfs.contracts.FileService;
                import org.springframework.stereotype.Controller;

                public class Compliant { }
                """);

        assertThat(scanForViolations(tempDir))
                .as("contracts and Spring imports are permitted in the UI layer")
                .isEmpty();
    }

    // ---------------------------------------------------------------------
    // detection logic
    // ---------------------------------------------------------------------

    private static List<String> scanForViolations(Path sourceRoot) throws IOException {
        try (Stream<Path> javaFiles = Files.walk(sourceRoot)) {
            return javaFiles
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(ArchitectureBoundaryTest::findViolations)
                    .toList();
        }
    }

    private static Stream<String> findViolations(Path javaFile) {
        final List<String> lines;
        try {
            lines = Files.readAllLines(javaFile);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read source file: " + javaFile, e);
        }

        return lines.stream()
                .map(String::strip)
                .filter(line -> line.startsWith("import "))
                .flatMap(line -> FORBIDDEN_IMPORTS.stream()
                        .filter(forbidden -> line.contains(forbidden + "."))
                        .map(forbidden -> javaFile.getFileName()
                                + " imports forbidden package: " + forbidden));
    }
}
