# Semantic File System (SFS)

SFS is a Java 21, multi-module Maven project that treats files as semantic objects rather than just raw byte containers. The repository is organized as a layered prototype for lifecycle management, semantic analysis, memory persistence, and a Spring Boot web UI.

The project is intentionally structured around clear boundaries between domain logic, service contracts, lifecycle behavior, adapters, semantic processing, and presentation.

---

## Why this project exists

The core idea is that a file can be understood as a combination of:

- identity and metadata
- raw content
- lifecycle state
- semantic meaning or "DNA"
- sensitive value handling
- reconstruction and memory semantics

This separates the physical artifact from its lifecycle and meaning, allowing the system to reason about analysis, retention, deletion, recovery, and reconstruction without conflating storage with knowledge.

---

## Repository layout

```text
Semantic-File-System/
├── pom.xml
├── LICENSE
├── README.md
├── sfs-core/
│   └── Core domain model and semantic DNA concepts
├── sfs-contracts/
│   └── Shared interfaces and boundary contracts
├── sfs-lifecycle/
│   └── Lifecycle state machine and deletion rules
├── sfs-engine/
│   └── Semantic processing orchestration
├── sfs-adapters/
│   └── Adapter SPI, registry, and text processing
├── sfs-memory/
│   └── H2-backed memory and semantic persistence layer
├── sfs-app/
│   └── Application services and API layer
├── sfs-ui/
│   └── Spring Boot UI and MVC surface
└── target/
    └── Maven build outputs
```

---

## Module overview

### sfs-core

Contains the canonical domain model for object identity, semantic DNA, and reconstruction rules. This module is intentionally focused on modeling and rules rather than infrastructure or UI concerns.

### sfs-contracts

Defines the shared contracts used across the system, including lifecycle, file operations, evaluation, search, reconstruction, and security boundaries.

### sfs-lifecycle

Implements the file lifecycle manager: state transitions, registration, version tracking, memory operations, deletion gates, and audit behavior.

### sfs-adapters

Provides the adapter framework and the current text-first implementation, including loading, normalization, structural parsing, and resolver logic.

### sfs-engine

Coordinates the semantic analysis pipeline, adapter routing, deterministic processing, and semantic record creation.

### sfs-memory

Provides the storage subsystem, including H2-backed relational persistence, memory indexing, version history, and semantic record storage patterns.

### sfs-app

Contains application-level services and API-facing models used to coordinate business logic, validation, and request handling.

### sfs-ui

Hosts the Spring Boot entry point and server-rendered web UI. It includes the MVC surface and templates for human-facing workflow access.

---

## Current implementation status

This repository is best understood as a working prototype for semantic file processing rather than a production filesystem.

Current emphasis includes:

- text-first file analysis
- semantic DNA modeling
- lifecycle state transitions
- adapter resolution and normalization
- memory persistence with H2
- server-rendered UI and API shell
- sensitive-value protections at the model boundary

The current implementation is intentionally layered and modular so the core concepts can evolve independently from the UI and storage details.

---

## Lifecycle model

The system models file evolution as an auditable state machine. A simplified progression is:

```text
REGISTERED
  -> ANALYZING
  -> ANALYZED
  -> MEMORIZABLE
  -> MEMORY_COMMITTED
  -> SOFT_DELETED
  -> MEMORIZED
```

Key characteristics:

- raw content and semantic metadata are treated separately
- deletion is intentionally reversible by default
- raw-data purge is a distinct gated action
- semantic records can survive after authorized raw-data removal
- invalid transitions are rejected by the lifecycle rules

---

## Security and sensitive data

Sensitive values are treated as a first-class concern. The design aims to prevent secrets from flowing into semantic output, search results, or debug surfaces.

Examples include:

- passwords
- authentication tokens
- API keys
- phone numbers
- email addresses
- account identifiers
- addresses

---

## Technology stack

- Java 21
- Maven 3.9+
- Spring Boot 4.1.0
- Spring MVC / REST
- Thymeleaf
- H2 database
- JUnit Jupiter
- AssertJ

---

## Prerequisites

Before running the project, make sure you have:

- Java 21
- Maven 3.9 or newer

Check your environment:

```bash
java --version
mvn --version
```

---

## Build and test

Run the full Maven test suite from the repository root:

```bash
mvn clean test
```

Build all artifacts:

```bash
mvn clean package
```

---

## Run the application

Start the Spring Boot UI from the repository root:

```bash
mvn spring-boot:run -pl sfs-ui
```

This builds the dependent modules and launches the application defined by the UI module. The app entry point is `com.sfs.ui.SfsUiApplication`.

By default, the server runs on the Spring Boot default port:

```text
http://localhost:8080
```

The REST API is organized under the `/api/v1` path.

---

## Scope and limitations

This is a prototype rather than a full production-grade filesystem implementation.

Current constraints include:

- text-first document handling
- UTF-8 input expectations
- in-memory or H2-backed storage patterns rather than full persistence infrastructure
- no kernel-level or OS-backed filesystem integration
- no full production security or multi-user tenancy model yet

---

## License

This project is distributed under the terms of the repository license.

See [LICENSE](LICENSE) for full details.

---

## Project tagline

Preserve meaning. Search memory. Reconstruct when needed.

