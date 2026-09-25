# Semantic File System (SFS)

SFS is a Java 21, multi-module Maven project that models files as semantic objects, not just raw byte containers. The repository is structured as a layered prototype for semantic file lifecycle, text-adapter processing, and a Spring Boot web application with a server-rendered UI and REST endpoints.

This project is intentionally organized around clear boundaries between domain logic, service contracts, lifecycle behavior, semantic analysis, and presentation.

---

## Why this project exists

The central idea is that a file can be understood as a combination of:

- identity and metadata
- raw content
- lifecycle state
- semantic meaning or "DNA"
- security-sensitive value handling

The design separates the file's physical artifact from its lifecycle and semantic interpretation, so the system can reason about meaning, deletion, and reconstruction without conflating storage and knowledge.

---

## Repository layout

The project is a Maven reactor with the following modules:

```text
Semantic-File-System/
├── pom.xml
├── LICENSE
├── README.md
├── sfs-core/
│   └── Domain model and core identity concepts
├── sfs-contracts/
│   └── Shared interfaces and API contracts
├── sfs-lifecycle/
│   └── File lifecycle, state transitions, raw-content gating, and memory/deletion rules
├── sfs-engine/
│   └── Semantic analysis pipeline and record generation
├── sfs-adapters/
│   └── Adapter SPI, registry, text loading, normalization, and parsing
├── sfs-app/
│   └── Application services and request/response models
├── sfs-ui/
│   └── Spring Boot UI and REST layer
└── target/
    └── Build outputs from Maven
```

---

## Module overview

### sfs-core

Contains the foundational domain model for object identity, semantic concepts, and core file abstractions. This module is intentionally kept small and isolated from persistence and UI concerns.

### sfs-contracts

Defines the shared contracts and interfaces used throughout the system, including lifecycle events, file operations, evaluation, search, reconstruction, and security boundaries.

### sfs-lifecycle

Implements lifecycle handling for registration, analysis, validation, soft delete, undo, memory commit, and raw-data purge. This is where the state machine and deletion gates live.

### sfs-engine

Provides the semantic processing pipeline: adapter resolution, text inspection, structural extraction, semantic analysis, record generation, and caching.

### sfs-adapters

Contains the adapter SPI and the current text-focused implementation. It loads UTF-8 content, normalizes input, recognizes basic structural hints, and prepares data for semantic processing.

### sfs-app

Holds application services and boundary logic. This layer coordinates domain operations, validation, authorization checks, and request handling without exposing implementation details directly to the UI.

### sfs-ui

The runtime application module. It contains the Spring Boot entry point, Thymeleaf templates, MVC controllers, and the web-facing API surface.

---

## Current capabilities

The project currently focuses on a text-first implementation and supports the following flow:

- import text files
- validate and normalize document content
- resolve an adapter based on file characteristics
- inspect and semantically analyze content
- maintain file lifecycle state and audit trail
- support reversible deletion and raw-data purge behavior
- expose a UI and API for file and semantic operations

The design explicitly models sensitive values and prevents them from being exposed casually through semantic output, search results, or debug surfaces.

---

## Lifecycle model

The system uses an auditable state machine to manage how a file evolves over time. The general path is:

```text
REGISTERED
  -> ANALYZING
  -> ANALYZED
  -> MEMORIZABLE
  -> MEMORY_COMMITTED
  -> SOFT_DELETED
  -> MEMORIZED
```

Key lifecycle characteristics:

- raw content and semantic metadata are managed separately
- deletion is intentionally reversible by default
- permanent raw-data removal is a separate gated action
- semantic records can remain usable even after authorized raw-data removal
- invalid transitions are rejected by the lifecycle state machine

---

## API and UI

The application exposes endpoints under:

```text
/api/v1
```

Core API areas include:

- file import and listing
- file analysis and lifecycle events
- semantic record retrieval
- search and reconstruction flows
- security and evaluation views

The UI is built with Spring Boot and Thymeleaf and uses contract-driven application services. The current implementation is intentionally a prototype and uses mock or in-memory backing behavior where deeper infrastructure has not yet been added.

---

## Security and sensitive data

Sensitive information is treated as a first-class concern. The system is designed to avoid leaking secrets through semantic output, search results, logs, or debug responses.

Examples of protected value types include:

- passwords
- authentication tokens
- API keys
- phone numbers
- email addresses
- account identifiers
- addresses

---

## Scope and limitations

This is a prototype rather than a production-grade filesystem implementation.

Current constraints:

- text files only
- UTF-8 input only
- strict upload-size limits
- in-memory stores for lifecycle and semantic data
- no durable persistence layer yet
- no kernel-level or OS-backed filesystem integration

The project is best understood as an architectural and domain-model prototype for semantic file processing rather than a full production filesystem.

---

## Prerequisites

- Java 21
- Maven 3.9+

Check your Java environment:

```bash
java --version
```

---

## Build and test

Run the full test suite from the project root:

```bash
mvn clean test
```

Build the project artifacts:

```bash
mvn clean package
```

---

## Run the application

Start the UI application from the repository root:

```bash
mvn spring-boot:run -pl sfs-ui
```

This builds the dependent modules and launches the Spring Boot app from the UI module. The app serves the web UI and exposes the REST API under the `/api/v1` path.

---

## Technology stack

- Java 21
- Maven 3.9.16
- Spring Boot 4.1.0
- Spring MVC / REST
- Thymeleaf
- JUnit Jupiter
- AssertJ

---

## License

This project is distributed under the terms of the repository license.

See [LICENSE](LICENSE) for full details.

---

## Project tagline

Preserve meaning. Search memory. Reconstruct when needed.

