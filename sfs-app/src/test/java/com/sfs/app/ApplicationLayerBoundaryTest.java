package com.sfs.app;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Application layer boundary")
class ApplicationLayerBoundaryTest {

    private static final Path SOURCE_ROOT = Path.of("src", "main", "java");

    private List<Path> sourceFiles() throws IOException {
        try (Stream<Path> files = Files.walk(SOURCE_ROOT)) {
            return files.filter(p -> p.toString().endsWith(".java")).toList();
        }
    }

    @Test
    @DisplayName("never imports a UI type")
    void neverImportsUiTypes() throws IOException {
        for (Path file : sourceFiles()) {
            String source = Files.readString(file, StandardCharsets.UTF_8);

            assertThat(source)
                    .as("%s must not depend on the UI module", file)
                    .doesNotContain("import com.sfs.ui.");
        }
    }

    @Test
    @DisplayName("never imports a web or servlet type")
    void neverImportsWebTypes() throws IOException {
        for (Path file : sourceFiles()) {
            String source = Files.readString(file, StandardCharsets.UTF_8);

            assertThat(source)
                    .as("%s must stay free of HTTP concerns", file)
                    .doesNotContain("jakarta.servlet")
                    .doesNotContain("org.springframework.web")
                    .doesNotContain("org.springframework.http");
        }
    }

    @Test
    @DisplayName("never imports a Spring stereotype")
    void neverImportsSpringStereotypes() throws IOException {
        for (Path file : sourceFiles()) {
            String source = Files.readString(file, StandardCharsets.UTF_8);

            assertThat(source)
                    .as("%s must remain framework-free so it can be unit tested without a context", file)
                    .doesNotContain("org.springframework.stereotype")
                    .doesNotContain("org.springframework.beans");
        }
    }

    @Test
    @DisplayName("never implements a domain rule that belongs to a later milestone")
    void neverImplementsDeferredDomainLogic() throws IOException {
        for (Path file : sourceFiles()) {
            String source = Files.readString(file, StandardCharsets.UTF_8);

            assertThat(source)
                    .as("%s must not embed retrieval, embedding or model logic", file)
                    .doesNotContain("cosineSimilarity")
                    .doesNotContain("embedding(")
                    .doesNotContain("tokenize(");
        }
    }
}
