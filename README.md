# Semantic File System (SFS)

SFS is a Java 21, multi-module Maven project that models files as semantic objects rather than as raw byte containers alone. The repository currently focuses on text documents, a lifecycle-aware file model, searchable semantic metadata, and a Spring Boot web application that exposes both UI and API endpoints.

The implementation is intentionally structured around explicit boundaries between:

- domain and identity logic
- service contracts
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

Provides the semantic processing pipeline. It inspects text documents, extracts semantic structure and meaning, and produces semantic record data used by the application layer.

### sfs-app

Contains application services and API contracts. This layer validates requests, performs authentication and authorization checks, and coordinates lifecycle and semantic operations without exposing backend implementation details directly to the UI.

### sfs-ui

The runtime application module. It contains the Spring Boot entry point and server-rendered web UI, REST controllers, API handlers, and mock implementations used by the current interface layer.

---

## Lifecycle model

The current implementation follows a lifecycle flow for text files, including analysis, semantic record validation, memoization, deletion, undo, and purge operations.

```text
REGISTERED
  -> ANALYZING
  -> ANALYZED
  -> MEMORIZABLE
  -> MEMORY_COMMITTED
  -> SOFT_DELETED
  -> MEMORIZED
```

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

Current API concerns include:

- file import and listing
- file analysis
- lifecycle event inspection
- soft delete and undo
- memorize and purge flows
- semantic record inspection
- search
- reconstruction
- evaluation
- error handling and validation

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

## Current scope

The codebase currently targets text-based files only. The pipeline and API assume UTF-8 text content and intentionally do not treat browser-provided MIME types as authoritative source-of-truth for file interpretation.

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
mvn spring-boot:run -pl sfs-ui -am
```

This builds the required dependent modules and starts the Spring Boot application from the UI module.

The default web UI is served by the Spring Boot app, and the REST API is available under the `/api/v1` path.

---

## Repository layout

```text
Semantic-File-System/
├── pom.xml
├── LICENSE
├── README.md
├── .gitignore
├── sfs-core/
├── sfs-contracts/
├── sfs-lifecycle/
├── sfs-engine/
├── sfs-app/
├── sfs-ui/
└── target/
```

---

## Technology stack

- Java 21
- Maven 3.9.16
- Spring Boot 4.1.0
- Thymeleaf
- Spring MVC / REST
- JUnit 5
- AssertJ

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

---

## License

Copyright © 2026 Regullacharith

All rights reserved.

See [LICENSE](LICENSE) for the full licensing notice.

---

## Semantic File System

Preserve meaning. Search memory. Reconstruct when needed.

