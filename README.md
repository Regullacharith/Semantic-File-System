# Semantic File System (SFS)

SFS is a Java 21, multi-module Maven project for treating files as semantic objects instead of plain byte containers. It combines lifecycle management, text analysis, semantic memory, search, reconstruction planning, and a web interface into a single platform designed for experimentation and prototype workflows.

The project is intentionally split into clear boundaries: domain logic, contracts, lifecycle behavior, adapters, semantic processing, storage, search, reconstruction, and presentation.

---

## Why this project exists

A file in SFS is modeled as more than a blob of bytes. Each file can carry:

- identity and metadata
- raw content
- lifecycle state
- semantic meaning or “DNA”
- protected or sensitive value handling
- reconstruction and memory semantics

This separation makes it possible to reason about analysis, retention, deletion, memory, and reconstruction without conflating storage with meaning.

---

## Repository layout

```text
Semantic-File-System/
├── pom.xml
├── LICENSE
├── README.md
├── sfs-core/                # domain model and semantic DNA concepts
├── sfs-contracts/           # shared contracts and interfaces
├── sfs-lifecycle/           # lifecycle state machine, versioning, and audit
├── sfs-engine/              # semantic analysis orchestration
├── sfs-adapters/            # adapter SPI, registry, and text ingestion
├── sfs-memory/              # H2 persistence and memory indexing
├── sfs-search/              # semantic query processing and ranking
├── sfs-reconstruction/      # reconstruction rules and planning
├── sfs-reconstruction-engine/ # reconstruction execution engine
├── sfs-app/                 # application services and DTOs
├── sfs-ui/                  # Spring Boot UI and REST API
├── target/                  # generated Maven build output
└── .gitignore
```

---

## Module overview

### sfs-core
Defines identity, semantic DNA, canonical serialization, validation, and reconstruction-rule planning. This module keeps domain logic isolated from UI and persistence concerns.

### sfs-contracts
Provides shared contracts for files, lifecycle records, semantic data, search, reconstruction, evaluation, and security boundaries.

### sfs-lifecycle
Handles registration, lifecycle transitions, version tracking, raw content handling, memory operations, deletion gates, restoration, and audit events.

### sfs-adapters
Contains the adapter SPI, registry, resolver, and the text adapter implementation. It currently accepts UTF-8 plain text and Markdown, normalizes content, and extracts document structure.

### sfs-engine
Coordinates adapter routing and the semantic analysis pipeline, including summaries, topics, concepts, entities, facts, relationships, structure, embeddings, and protected-value detection.

### sfs-memory
Stores lifecycle data, raw content, versions, semantic DNA, and reconstruction records in H2. The retrieval index is in-memory and rebuilt from persisted semantic records at startup.

### sfs-search
Implements query parsing, semantic retrieval, candidate ranking, and evidence-rich results over the search index.

### sfs-reconstruction
Contains reconstruction logic and rule planning used to rebuild or interpret file meaning from semantic and lifecycle context.

### sfs-reconstruction-engine
Provides the execution/runtime layer used to drive reconstruction workflows and related evaluation behavior.

### sfs-app
Contains application services and DTO/request models that coordinate file, search, reconstruction, evaluation, and security workflows through the shared contracts.

### sfs-ui
Hosts the Spring Boot application, Thymeleaf templates, and REST controllers. The UI covers file browsing, search, reconstruction, evaluation, settings, and lifecycle views. The REST API is rooted under `/api/v1`.

---

## Current implementation status

This repository is best described as a working prototype rather than a production-grade filesystem. Core lifecycle behavior, text analysis, semantic representation, persistence, and reconstruction-rule planning are implemented, while several of the operational flows are intentionally mocked for demonstration and local development.

The default profile is `mock`, which means:

- search services are development-oriented or simulated
- reconstruction and evaluation paths are mocked
- security and authentication are not production-grade
- sample files are seeded on first startup when the database is empty

Current emphasis includes:

- ingestion of text and Markdown content (`.txt`, `.text`, `.md`, `.markdown`, and `.log`)
- semantic DNA generation and validation
- file lifecycle, version history, audit events, soft deletion, and gated raw-data purge
- H2-backed persistence for lifecycle, content, and semantic records
- semantic query parsing, vector retrieval, and ranking
- browser UI and REST API flows for file, search, lifecycle, reconstruction, and evaluation actions
- protected value detection and protected references in semantic output

The layered architecture is designed so domain logic, application workflows, persistence, and presentation can evolve independently.

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
- invalid transitions are rejected

---

## Security and sensitive data

The analysis pipeline detects protected values and records protected references rather than treating sensitive values as ordinary semantic facts. This is a prototype boundary, not a production security solution; the default profile uses development identities and mock authorization flows.

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

This repository does not include a Maven wrapper, so use an installed Maven 3.9+ executable.

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

Start the UI from the repository root:

```bash
mvn spring-boot:run -pl sfs-ui
```

This builds the dependent modules and launches the application defined by the UI module. The entry point is `com.sfs.ui.SfsUiApplication`.

By default, the app binds to `127.0.0.1` on port `8080` and starts with the `mock` profile:

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

The REST API is organized under `/api/v1`. Main endpoint groups include `/health`, `/version`, `/files`, `/search`, `/reconstructions`, and `/evaluations`. File import is limited to 5 MB per file and per request by default.

---

## Quick API overview

The UI exposes a REST API under `/api/v1` with grouped endpoints for:

- `/health` — application health and status checks
- `/version` — version metadata
- `/files` — file registration, content import, and lifecycle operations
- `/search` — semantic and metadata search
- `/reconstructions` — reconstruction workflows
- `/evaluations` — evaluation and scoring endpoints

---

## Scope and limitations

This is a prototype rather than a full production-grade filesystem implementation.

Current limitations include:

- only the text adapter is implemented; it handles UTF-8 plain text and Markdown, not binary formats
- the vector index is in memory and is rebuilt from persisted semantic records at startup
- the default profile uses mock search, reconstruction, evaluation, and security services alongside development identities
- no kernel-level or OS-backed filesystem integration
- no production authentication, authorization, or multi-user tenancy model
- reconstruction fidelity is not guaranteed; rendering and evaluation remain mocked in the UI

---

## License

This project is distributed under the terms of the repository license.

See [LICENSE](LICENSE) for full details.

---

## Project tagline

Preserve meaning. Search memory. Reconstruct when needed.

