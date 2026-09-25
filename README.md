# Semantic File System (SFS)

SFS is a Java 21, multi-module Maven project that models files as semantic objects rather than as raw byte containers alone. The repository currently focuses on text documents, a lifecycle-aware file model, searchable semantic metadata, and a Spring Boot web application that exposes both UI and API endpoints.

The implementation is intentionally structured around explicit boundaries between:

- domain and identity logic
- service contracts
- file-type adapter resolution and text normalization
- lifecycle management
- semantic analysis and record generation
- application orchestration
- UI and API presentation

---

## Why SFS exists

The project treats a file as more than a collection of bytes. A semantic file combines:

- logical identity
- raw content
- metadata
- lifecycle state
- semantic DNA or derived meaning
- protected/sensitive-value handling

This lets the project track both the file's lifecycle and its meaning independently of the raw storage artifact.

---

## Project structure

This repository is organized as a Maven reactor with the following modules:

```text
Semantic-File-System/
├── pom.xml
├── LICENSE
├── README.md
├── .gitignore
├── sfs-core/
│   └── Domain model and core identity types
├── sfs-contracts/
│   └── Shared service contracts for files, search, reconstruction, security, and evaluation
├── sfs-lifecycle/
│   └── Lifecycle state machine, raw-content store, Object IDs, and deletion gates
├── sfs-engine/
│   └── Semantic analysis pipeline, inspection, cache, and semantic record generation
├── sfs-adapters/
│   └── Adapter SPI, registry, text loading, normalization, and structural parsing
├── sfs-app/
│   └── Application services and API request/response models
├── sfs-ui/
│   └── Spring Boot UI + REST API layer using Thymeleaf and controller-based endpoints
└── target/
    └── Generated build artifacts from local Maven runs
```

---

## Module overview

### sfs-core

Contains the pure domain model for object identity and semantic concepts. This module is kept intentionally small and independent from lifecycle or persistence concerns.

### sfs-contracts

Defines the shared contracts used across the application, including file operations, lifecycle events, search, reconstruction, evaluation, and security boundaries.

### sfs-lifecycle

Implements the file lifecycle model and state transitions for registration, analysis, soft delete, undo, memorize, and raw-data purge. The lifecycle behavior is centered on a state machine and raw-deletion gate.

### sfs-engine

Provides the semantic processing pipeline. It resolves an input adapter, inspects text documents, extracts semantic structure and meaning, caches reusable analysis, and produces semantic record data used by the application layer.

### sfs-adapters

Provides the adapter SPI and V1 text adapter. It selects an adapter from file characteristics, safely loads UTF-8 text, normalizes it, and extracts structural information before semantic analysis.

### sfs-app

Contains application services and API contracts. This layer validates requests, performs authentication and authorization checks, and coordinates lifecycle and semantic operations without exposing backend implementation details directly to the UI.

### sfs-ui

The runtime application module. It contains the Spring Boot entry point and server-rendered web UI, REST controllers, API handlers, and mock implementations used by the current interface layer.

---

## Lifecycle model

The current implementation uses an auditable state machine for registration, analysis, semantic record validation, memorization, deletion, undo, and raw-data purge. The common successful path is:

```text
REGISTERED
  -> ANALYZING
  -> ANALYZED
  -> MEMORIZABLE
  -> MEMORY_COMMITTED
  -> SOFT_DELETED
  -> MEMORIZED
```

Analysis can also fail, be refused, or be requeued after interruption. Soft deletion records the prior live state so it can be restored, while purging releases raw content only after the deletion gate is satisfied. `MEMORIZED` is terminal for the raw-data lifecycle; semantic records and audit information remain available according to their service contracts.

Important characteristics of the design:

- raw content and semantic metadata are treated separately
- deletion is reversible by default
- permanent raw-data removal is controlled as a separate operation
- semantic records remain usable after authorized raw-data removal
- invalid transitions are rejected by the lifecycle state machine

---

## API and UI

The application exposes REST endpoints under:

```text
/api/v1
```

The Spring Boot application provides a server-rendered Thymeleaf UI and REST endpoints including:

- `GET /api/v1/files` and `POST /api/v1/files` for listing and importing text files
- `POST /api/v1/files/{objectId}/analyze` for semantic analysis
- `GET /api/v1/files/{objectId}/events` for lifecycle audit events
- `DELETE /api/v1/files/{objectId}` and `POST /api/v1/files/{objectId}/undo-delete` for reversible deletion
- `POST /api/v1/files/{objectId}/memorize` and `/purge` for memory commit and raw-data release
- `GET /api/v1/objects/{objectId}/dna` for semantic records
- `GET` or `POST /api/v1/search` for semantic search
- `/api/v1/reconstructions` for reconstruction jobs and generated artifacts
- `/api/v1/evaluations` and `/api/v1/security/settings` for evaluation and security views

Destructive operations require the configured `X-SFS-Credential` header and, where applicable, confirmation of the Object ID. API errors use structured validation, authorization, conflict, payload, and job-status responses.

The UI is built with Spring Boot and Thymeleaf, and it uses contract-driven application services plus mock implementations where deeper backend infrastructure is intentionally not yet implemented.

---

## Security and sensitive data

SFS explicitly models sensitive information as a first-class concern. The project is designed to avoid exposing secrets through unrestricted semantic output, logs, search responses, or debug output.

That includes protections for values such as:

- passwords
- authentication tokens
- API keys
- phone numbers
- email addresses
- account identifiers
- physical addresses

The implementation treats protected values differently from ordinary reconstructable content.

---

## Current scope and limits

The V1 codebase targets text-based files only. Imports use UTF-8 text, reject malformed UTF-8, and enforce a 5 MiB limit for multipart uploads. File names are validated as names rather than paths, and browser-provided MIME types are treated as hints instead of authoritative source-of-truth for file interpretation.

The current runtime uses in-memory lifecycle, semantic-record, and job stores. It is a prototype of the domain and application boundaries, not yet a durable production filesystem or persistence layer.

---

## Build and test

The project requires Java 21.

Check the Java version:

```bash
java --version
```

Run the full test suite:

```bash
mvn clean test
```

Create the packaged artifacts:

```bash
mvn clean package
```

---

## Run the application

From the project root, start the UI application with:

```bash
mvn spring-boot:run -pl sfs-ui 
```

This builds the required dependent modules and starts the Spring Boot application from the UI module.

The default web UI is served by the Spring Boot app, and the REST API is available under the `/api/v1` path.

---

---

## Technology stack

- Java 21
- Maven 3.9.16
- Spring Boot 4.1.0
- Thymeleaf
- Spring MVC / REST
- JUnit Jupiter 5.11.4
- AssertJ 3.27.3

---

## Notes

This repository is best understood as a structured semantic file system prototype and application boundary model rather than a kernel-level file system or a full production filesystem implementation.

It is designed to demonstrate:

- semantic identity
- lifecycle management
- contractual boundaries
- explicit destructive-operation rules
- secure handling of sensitive values
- UI/API separation from core logic
- adapter-based text ingestion and deterministic semantic analysis

---

## License

Copyright © 2026 Regullacharith

All rights reserved.

See [LICENSE](LICENSE) for the full licensing notice.

---

## Semantic File System

Preserve meaning. Search memory. Reconstruct when needed.

