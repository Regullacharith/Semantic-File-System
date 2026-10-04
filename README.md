# Semantic File System (SFS)

> **A semantic-memory and reconstruction system for files.**

SFS — **Semantic File System** — is a research and engineering project that explores a different approach to file preservation.

Traditional file systems primarily preserve the raw bytes of a file. SFS additionally preserves a structured representation of **what the file means**: its identity, concepts, topics, facts, relationships, structure, semantic representation, and information required to support later reconstruction.

The central idea is simple:

```text
                    LIVE FILE
                       │
                       ▼
                File-Type Adapter
                       │
                       ▼
                 Semantic Engine
                       │
                       ▼
                  Semantic DNA
                       │
             ┌─────────┴─────────┐
             ▼                   ▼
      Memory Database        Vector Index
             │                   │
             └─────────┬─────────┘
                       ▼
                 Semantic Search
                       │
                       ▼
                    Object ID
                       │
                       ▼
              Semantic DNA + Rules
                       │
                       ▼
              Reconstruction Engine
                       │
                       ▼
              Reconstructed File
                       │
                       ▼
             Fidelity Evaluation
```

SFS is **not** intended to be a conventional backup system or a byte-for-byte forensic recovery mechanism. The objective is to preserve semantic information intentionally so that, after authorized deletion of raw data, the surviving semantic record can be searched and used to produce a new, semantically faithful representation.

---
# What Is SFS?

A normal file can be viewed primarily as:

```text
File
├── Identity
├── Raw Bytes
└── Metadata
```

SFS introduces a richer logical representation:

```text
Semantic File
├── Identity
│
├── Raw Data
│
├── Semantic DNA
│
└── Reconstruction Rules
```
The **Semantic File** is a logical abstraction.

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

## Module overview

### sfs-core
Defines identity, semantic DNA, canonical serialization, validation, reconstruction-rule planning, and the observability kernel (trace IDs, structured log events, metrics registry). This module keeps domain logic isolated from UI and persistence concerns.

### sfs-contracts
Provides shared contracts for files, lifecycle records, semantic data, search, reconstruction, evaluation, and security boundaries.

### sfs-lifecycle
Handles registration, lifecycle transitions, version tracking, raw content handling, memory operations, deletion gates, restoration, and audit events.

### sfs-security
Implements the Security & Privacy System: sensitive-value detection (credentials, API keys, tokens, emails, phone numbers, account identifiers, addresses), the per-type handling policy engine, protected references, an AES-256-GCM encrypted secure store with a separately held master key, authentication and capability-based authorization for secret resolution, and the security audit log.

### sfs-adapters
Contains the adapter SPI, registry, resolver, and the text adapter implementation. It currently accepts UTF-8 plain text and Markdown, normalizes content, and extracts document structure.

### sfs-engine
Coordinates adapter routing and the semantic analysis pipeline, including summaries, topics, concepts, entities, facts, relationships, structure, embeddings, and protected-value detection.

### sfs-memory
Stores lifecycle data, raw content, versions, semantic DNA, and reconstruction records in H2. The retrieval index is in-memory and rebuilt from persisted semantic records at startup.

### sfs-search
Implements query parsing, semantic retrieval, candidate ranking, and evidence-rich results over the search index.

### sfs-reconstruction
Implements the SFS Reconstruction Model: the swappable model contract, the unified representation and its encoders (structure, facts/entities, relationships), the decoder interface with a deterministic baseline renderer, and the reproducible model benchmark.

### sfs-reconstruction-engine
Provides the Reconstruction Engine runtime: the request manager and job state machine, DNA/rule loading, planning, model execution, constraint verification, post-processing, and TXT artifact generation with recorded provenance.

### sfs-evaluation
Implements the Evaluation & Fidelity System: per-dimension measurement (semantic, structural, factual, entity, relationship, completeness), explicit critical-fact scoring, confidence calibration, the regression benchmark with its committed baseline, and the improvement advisor.

### sfs-app
Contains application services and DTO/request models that coordinate file, search, reconstruction, evaluation, and security workflows through the shared contracts.

### sfs-ui
Hosts the Spring Boot application, Thymeleaf templates, and REST controllers. The UI covers file browsing, search, reconstruction, evaluation, settings, and lifecycle views. The REST API is rooted under `/api/v1`.

---

## Current implementation status

### Current Implementation — V1 Complete

SFS V1 is fully implemented and verified. The interface is backed by real lifecycle, semantic analysis, persistence, search, reconstruction, evaluation, security, and observability subsystems.

Implemented:

- file lifecycle manager with audit trail and gated raw-data deletion
- semantic analysis engine producing Semantic DNA (`sfs-dna/0.2`)
- reconstruction rules (`sfs-rules/0.2`) with planning and conflict detection
- durable Memory Database (H2) with rebuildable vector index
- semantic search engine with evidence-bearing results
- SFS Reconstruction Model with deterministic baseline rendering
- reconstruction engine with observable, auditable jobs
- evaluation and fidelity system with measured dimension scores, confidence calibration, and regression baseline
- security and privacy system with sensitive-value detection, handling policies, protected references, encrypted secure store, and authorization-gated resolution
- observability with trace IDs, structured logs (`sfs-log/0.1`), metrics (`sfs-metrics/0.1`), and externalized configuration (`sfs-config/0.1`)

Verification: **1,024 automated tests** pass from a clean build, covering unit, integration, end-to-end, security, performance, and benchmark suites, plus a live executable-JAR release check.

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

SFS implementation focused on text-based semantic file management, semantic representation, search, reconstruction, evaluation, security, and observability.

### Included

- Java 21 and Text Adapter for UTF-8 text and Markdown
- Semantic File abstraction, Object ID, Semantic DNA, and automatic adapter selection
- Durable Memory Database (H2), vector embeddings, and rebuildable vector index
- Semantic Search with evidence-bearing results
- Reconstruction Rules, SFS Reconstruction Model, and Reconstruction Engine
- semantic reconstruction
- Semantic, structural, and factual/content fidelity measurement and evaluation
- Iterative Semantic DNA improvement
- Encryption and sensitive-data policies
- Storage/fidelity measurements and UI

### Limitations

- Text files only; binary formats are deferred.
- Reconstruction is not guaranteed to be exact; information not captured cannot be reliably reconstructed.
- High-entropy values and passwords cannot generally be inferred from semantic meaning; sensitive values require authorized protected storage.
- Reconstruction quality and storage reduction depend on source data and must be experimentally measured.
- The vector index is in memory and rebuilt from persisted semantic records at startup.
- V1 is loopback-only; TLS, hosted CI, and multi-user tenancy are deferred.
- No kernel-level or OS-backed filesystem integration; SFS does not replace the physical filesystem in V1.
---

## License

This project is distributed under the terms of the repository license.

See [LICENSE] for full details.

---

## Project tagline

Preserve meaning. Search memory. Reconstruct when needed.

