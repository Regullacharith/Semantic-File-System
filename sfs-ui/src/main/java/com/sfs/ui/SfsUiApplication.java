package com.sfs.ui;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application entry point for the SFS lightweight user interface.
 *
 * <p><strong>Scope.</strong> This class starts the embedded web server and nothing else.
 * It contains no semantic, persistence, reconstruction, evaluation or security logic, in
 * accordance with the Milestone 01 requirement that the UI is a thin operational surface.
 *
 * <p><strong>Component scanning.</strong> Scanning is rooted at {@code com.sfs.ui} only.
 * It deliberately does not scan {@code com.sfs}, so that future backend modules cannot be
 * auto-wired into the UI by accident. When Milestone 02 introduces real services, they are
 * wired in explicitly rather than discovered implicitly.
 *
 * <p><strong>Status at Task 01.0.</strong> The application starts and serves no routes.
 * Navigation and views arrive in Task 01.1 onwards.
 */
@SpringBootApplication
public class SfsUiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SfsUiApplication.class, args);
    }
}
