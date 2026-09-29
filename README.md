# Semantic File System (SFS)

SFS is a Java 21, multi-module Maven project that treats files as semantic objects rather than just raw byte containers. It combines a file lifecycle manager, a text-analysis pipeline, semantic memory and search components, reconstruction rules, and a Spring MVC web UI with a REST API.

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

This separates the physical artifact from its lifecycle and meaning, allowing the system to model analysis, retention, deletion, memory, and reconstruction without conflating storage with knowledge.

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
│   └── H2-backed persistence and vector index
├── sfs-search/
│   └── Query parsing, semantic retrieval, and ranking
├── sfs-app/
│   └── Application services and API models
├── sfs-ui/
│   └── Spring Boot web UI and REST API
```

---

## Module overview

### sfs-core

Contains object identity, the semantic DNA model, canonical serialization, validation, and reconstruction-rule planning. It has no UI or storage infrastructure concerns.

### sfs-contracts

Defines shared contracts and API-facing views for files, lifecycle audit, semantic records, search, reconstruction, evaluation, and security.

### sfs-lifecycle

Implements file registration, lifecycle transitions, version tracking, raw-content handling, memory operations, deletion gates, recovery, and lifecycle audit.

### sfs-adapters

Provides the adapter SPI, registry, and resolver. The implemented text adapter accepts plain text and Markdown formats, normalizes UTF-8 text, and extracts document structure.

### sfs-engine

Coordinates adapter routing and the semantic analysis pipeline, including summaries, topics, concepts, entities, facts, relationships, structure, embeddings, and protected-value detection.

### sfs-memory

Provides H2-backed persistence for lifecycle data, raw content, versions, semantic DNA, and reconstruction rules. The vector index used for retrieval is currently in memory.

### sfs-search

Implements query parsing, semantic retrieval, candidate ranking, and evidence-rich search results over the memory index.

### sfs-app

Contains application services and request/response models that coordinate file, search, reconstruction, evaluation, and security workflows through the shared contracts.

### sfs-ui

Hosts the Spring Boot entry point, Thymeleaf screens, and REST controllers. The UI includes dashboard, file/object browsing, search, reconstruction, evaluation, and settings views; the REST API is rooted at `/api/v1`.

---

## Current implementation status

This repository is a working prototype, not a production filesystem. The core lifecycle, text analysis, semantic representation, H2 persistence, and reconstruction-rule planning components are implemented. The default UI profile is `mock`: search, reconstruction, evaluation, and security-facing services use development/mock implementations, and sample files are seeded on first startup. The search engine and persistence components also exist, but default mock-profile UI workflows should not be mistaken for production integrations.

Current emphasis includes:

- text and Markdown ingestion through the text adapter (`.txt`, `.text`, `.md`, `.markdown`, and `.log`)
- semantic DNA generation and validation
- file lifecycle, version history, audit events, soft deletion, and gated raw-data purge
- H2-backed persistence for lifecycle, content, and semantic records
- semantic query parsing, vector retrieval, and ranking components
- a server-rendered UI and REST API for file, search, lifecycle, reconstruction, and evaluation workflows
- protected-value detection and protected references in semantic output

The modules are layered so domain logic, application workflows, storage, and presentation can evolve independently. The default mock profile is intended for local development and demonstration, not production use.

---

## Lifecycle model

The system models file evolution as an auditable state machine. A typical progression is:

```text
REGISTERED
  -> ANALYZING
  -> ANALYZED
  -> MEMORIZABLE
  -> MEMORY_COMMITTED
```

After memory is committed, a file can be soft-deleted and later restored. Authorized raw-data purge is a separate operation; `MEMORIZED` represents the state after raw content is removed. `FAILED` is also represented for failed processing.

Key characteristics:

- raw content and semantic metadata are stored separately
- soft deletion is reversible
- raw-data purge is a distinct, gated action
- semantic records may remain after authorized raw-data removal
- invalid state transitions are rejected

---

## Security and sensitive data

The analysis pipeline detects protected values and records protected references rather than treating sensitive values as ordinary semantic facts. This is a prototype boundary, not a complete production security solution: the default profile uses development identities and mock authentication/authorization.

Detectors and policy cover values such as:

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

The repository has no Maven wrapper; use an installed Maven 3.9+ executable.

---

## Build and test

Run the full multi-module test suite from the repository root:

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

This builds the dependent modules and launches the application defined by the UI module. The entry point is `com.sfs.ui.SfsUiApplication`.

By default, the UI binds to `127.0.0.1` on port `8080` and starts with the `mock` profile:

```text
http://localhost:8080
```

The `mock` profile seeds sample files on first startup when the database is empty. Configuration can be overridden with these environment variables:

| Variable | Default | Purpose |
| --- | --- | --- |
| `SFS_PROFILE` | `mock` | Spring profile |
| `SFS_UI_PORT` | `8080` | HTTP port |
| `SFS_UI_ADDRESS` | `127.0.0.1` | Bind address |
| `SFS_MEMORY_PATH` | `data/sfs-memory` | H2 file database path |

The REST API is organized under `/api/v1`. Main endpoint groups include `/health`, `/version`, `/files`, `/search`, `/reconstructions`, and `/evaluations`. File import is limited to 5 MB per file and request by default.

---

## Scope and limitations

This is a prototype rather than a full production-grade filesystem implementation.

Current limitations include:

- only the text adapter is implemented; it handles UTF-8 plain text and Markdown, not binary document formats
- the vector index is in memory and is rebuilt from persisted semantic records at startup
- the default profile uses mock search, reconstruction, evaluation, and security services, alongside development identities
- no kernel-level or OS-backed filesystem integration
- no production authentication, authorization, or multi-user tenancy model
- no guarantee of reconstruction fidelity; reconstruction rendering and evaluation are currently mocked in the UI

---

## License

This project is distributed under the terms of the repository license.

See [LICENSE](LICENSE) for full details.

---

## Project tagline

Preserve meaning. Search memory. Reconstruct when needed.

