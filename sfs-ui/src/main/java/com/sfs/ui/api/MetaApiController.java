package com.sfs.ui.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class MetaApiController {

    private static final String API_VERSION = "v1";
    private static final String CONTRACTS_VERSION = "0.1";
    private static final String DNA_SCHEMA_VERSION = "sfs-dna/0.1";
    private static final String RULES_VERSION = "sfs-rules/0.1";

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "timestamp", Instant.now().toString());
    }

    @GetMapping("/version")
    public Map<String, Object> version() {
        return Map.of(
                "apiVersion", API_VERSION,
                "contractsVersion", CONTRACTS_VERSION,
                "dnaSchemaVersion", DNA_SCHEMA_VERSION,
                "rulesVersion", RULES_VERSION,
                "milestone", "M02 — Application & API Layer",
                "enforcedSubsystems", java.util.List.of(),
                "note", "All domain subsystems are mocked in V1. "
                        + "Search, reconstruction, evaluation and security are not yet implemented.");
    }
}
