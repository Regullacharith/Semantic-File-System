# Semantic File System (SFS)
The core idea is to make a file more than just a collection of raw bytes. SFS creates a semantic representation of the file—what it contains, what it means, how its information is related, and how its structure can be reconstructed.
For example, suppose you have:

project.txt


"This project develops an AI-powered database..."

Instead of only storing the raw text, SFS creates something conceptually like:

Object ID
   │
   ├── File identity
   ├── Type: text
   ├── Topics
   │     ├── AI
   │     └── Database
   ├── Concepts
   ├── Entities
   ├── Facts
   ├── Relationships
   ├── Document structure
   ├── Semantic representation
   ├── Vector embedding
   └── Reconstruction Rules
             │
             ▼
        Semantic DNA

That Semantic DNA is stored in the Memory Database.

If the original raw file is later intentionally deleted, SFS is not trying to recover the original bytes from the disk. Instead:

Deleted raw file
       ↓
Semantic memory remains
       ↓
Semantic Search
       ↓
Find Object ID
       ↓
Retrieve Semantic DNA
       ↓
Reconstruction Rules
       ↓
Reconstruction Engine
       ↓
New reconstructed file

The reconstructed file does not have to be byte-for-byte identical to the original. Your V1 design focuses on:

Semantic similarity — does it mean the same thing?
Structural similarity — does it preserve the organization?
Factual/content fidelity — did it preserve the important information?
SFS is a backend-oriented system that preserves a structured semantic memory of files so that their meaning, structure, and important information can later be searched and used to reconstruct a new representation even after the original raw data has been deleted.A research and engineering project that stores what a file **means**, not just what a file **contains** — and then reconstructs a semantically faithful artifact from that meaning after the original bytes are gone.

---

## Table of Contents

- [Concept](#concept)
- [What SFS Is Not](#what-sfs-is-not)
- [Architecture](#architecture)
- [Core Abstractions](#core-abstractions)
- [Project Status](#project-status)
- [Development Workflow](#development-workflow)
- [Prerequisites](#prerequisites)
- [Build and Run](#build-and-run)
- [Repository Layout](#repository-layout)
- [Milestone Roadmap](#milestone-roadmap)
- [Development Protocol](#development-protocol)
- [Research Metrics](#research-metrics)
- [Known Limitations](#known-limitations)
- [License](#license)  

---

## Concept

An ordinary text file is converted into a structured semantic representation called **Semantic DNA**, which is persisted independently of the raw bytes. Once that semantic memory is durably committed, the original file may be deleted — and the system can still find the record by meaning and regenerate a semantically equivalent artifact from it.

```
ordinary text file
        │
        ▼
   Text Adapter
        │
        ▼
  Semantic Engine
        │
        ▼
   Semantic DNA ──────────────┐
        │                     │
        ▼                     ▼
 Memory Database        Vector Index
        │                     │
        └──── Semantic Search ┘
        │
        ▼
Reconstruction Rules + SFS Reconstruction Model
        │
        ▼
 Reconstruction Engine
        │
        ▼
 reconstructed text file
        │
        ▼
   Fidelity Report
```

The objective is preservation of **semantic identity** — meaning, structure, facts, entities and relationships — measured honestly and separately, rather than byte-level recovery.

---

## What SFS Is Not

Stated explicitly, because these boundaries define the project:

| Not this | Reason |
|---|---|
| A byte-for-byte recovery system | Exact information cannot be recovered from a hash, embedding or semantic representation once discarded. SFS is a *semantic* reconstruction system. |
| A physical filesystem or kernel module | Semantic File is a logical object/API abstraction, not a new on-disk format. |
| A compression tool | Storage is evaluated as **Knowledge Preservation Density** against measured fidelity, not as byte-ratio compression. |
| An LLM wrapper | A deterministic baseline is implemented first. The reconstruction model is replaceable behind an interface and must not fabricate unsupported critical facts. |
| A guaranteed-accuracy system | The 87–92% fidelity figure is an **experimental target to be measured**, never a pre-declared specification. |

---

## Architecture

Fourteen subsystems, each owning a milestone. Dependency direction is strictly enforced by the build.

```
User / Lightweight UI                       [M01]
        │
Application / API Layer                     [M02]
        │
File Lifecycle Manager                      [M03]
        │
Semantic Engine                             [M04]
        │
Adapter Resolver ──── Text Adapter          [M05]
        │
Semantic DNA                                [M06]
        │
        ├── Reconstruction Rules            [M07]
        ├── Memory DB + Vector Index        [M08]
        └── Security & Privacy              [M13]
        │
Semantic Search                             [M09]
        │
Reconstruction Engine                       [M11]
        │  ▲
        │  └── SFS Reconstruction Model      [M10]
        ▼
Evaluation & Fidelity                       [M12]
        │
        └──► improvement loop → DNA / Rules / Model

Infrastructure / Observability / Testing    [M14]  (spans all milestones)
```

### Enforced boundaries

Architectural rules are executable, not merely documented:

- `sfs-core` has **zero production dependencies** — no Spring, no Jackson, no logger. The domain model is depended upon by every subsystem, so a framework here would leak everywhere.
- `sfs-ui` may depend only on `sfs-contracts`. An `ArchitectureBoundaryTest` fails the build if the UI imports any backend subsystem package or `java.sql`.
- Raw data, semantic representation, memory/index information and security-protected information are kept distinctly separate.

---

## Core Abstractions

**Semantic File** — logical object carrying identity, metadata, raw data (while live) and Semantic DNA.

**Object ID** — stable logical identity that survives rename, move and raw-file deletion. Storage addresses are supporting metadata only.

**Semantic DNA** — the structured persistent representation. Not a text summary.

```
SemanticDNA
  schemaVersion · identityRef · summary
  concepts[] · topics[] · entities[] · facts[] · relationships[]
  structure · embeddings[] · behaviour
  reconstructionRules[] · fidelityProfile · securityProfile · version
```

**Semantic Record** — the database record that survives raw-file deletion.

**Reconstruction Rules** — versioned declarative constraints persisted *with* Semantic DNA but executed *by* the reconstruction subsystem. They may mark facts and entities as required, constrain structure and ordering, and forbid deviation.

**Protected Reference** — pointer to a sensitive exact value held outside ordinary Semantic DNA, in an encrypted store, resolvable only under authorization. Passwords use a separate non-reversible policy.

### Semantic deletion workflow

Raw bytes are removed **only** after semantic memory is validated and durably committed:

```
live file → register → background analysis → Semantic DNA + Rules + security refs
   → validate completeness/fidelity/security → commit Semantic Record
   → confirm durable memory → user-authorized deletion → remove raw bytes
   → Semantic Record remains → searchable → reconstructable
```

---

## Project Status

> **Early development.** SFS V1 is being built incrementally. The current work is still inside **Milestone 01 — User / Interface Layer**.

| | |
|---|---|
| **Current milestone** | M01 — User / Interface Layer |
| **Completed task**	 |Semantic Search view |
| **Automated tests** | |

What exists today: a three-module Maven reactor, Java 21 configuration, Spring Boot 4.1.0
application bootstrap, explicit module boundaries enforced by an executable architecture test,
the lightweight UI shell — base layout and primary navigation covering all seven Milestone 01
destinations — plus two working screens: the dashboard and the Files screen. The remaining
five destinations render as visibly disabled items rather than links, so no navigation action
produces a silent 404.

The Files screen imports a UTF-8 text file, assigns an Object ID, requests analysis and
performs semantic deletion. It is backed by MockFileService, an in-memory stand-in — not
the real File Lifecycle Manager. The mock deliberately enforces the lifecycle rules rather
than merely storing data: semantic deletion is refused unless a file has been analyzed,
mirroring the constraint that raw bytes may never be removed before a validated Semantic
Record is durably committed. Its state resets when the application restarts.

What does not exist yet: Text Adapter, Semantic Engine, Semantic DNA, Memory Database,
Vector Index, Semantic Search, Reconstruction Rules, SFS Reconstruction Model, Reconstruction
Engine, Evaluation/Fidelity pipeline, and the V1 security/privacy subsystem. No semantic
analysis is performed and no Semantic DNA is produced — importing a file registers a name
and a size, nothing more. Nothing is persisted. These arrive in their designated milestones.

Verification status is tracked deliberately — implemented, compiled, unit-tested, integration-tested, experimentally evaluated, and accepted are distinct states and are never conflated. No fidelity percentage is treated as a fact unless a reproducible measurement produces it.
---
### M01 sequence

```
M01 — User / Interface Layer
 │
 ├── 01.0  Project skeleton, Maven reactor, module boundaries   ✅ complete
 ├── 01.1  UI shell and navigation                              ✅ complete
 ├── 01.2  File import / analyze / delete controls              ✅ complete
 ├── 01.3  Semantic Search view                                 ✅ complete
 ├── 01.4  Object / Semantic DNA view                           ◀ next
 ├── 01.5  Single-click reconstruction flow
 ├── 01.6  Evaluation / fidelity view
 ├── 01.7  Security / policy settings
 └── 01.8  Milestone integration tests + acceptance report
```
Every M01 view is built against **mock services**. Real backend services arrive from M02
onward, exactly as the milestone specification permits: *"API contracts; can begin with mocks."*

The important distinction is that M01 is the milestone currently being implemented. The
remaining SFS architecture is planned and documented, but intentionally not implemented yet.

### Development workflow

SFS follows this controlled loop:

```
Milestone
   ↓
Phase
|  ↓
Compile
   ↓
Run tests
   ↓
Run application / verification scenario
   ↓
Inspect code and Git diff
   ↓
commits
   ↓
Next phase

```
## Prerequisites

| Tool | Version | Notes |
|---|---|---|
| JDK | **21 LTS** | Eclipse Temurin recommended. |
| Apache Maven | **3.9.16** | |
| Git | 2.4x | |
| Browser | any modern | UI is server-rendered HTML |

Verify your toolchain:

```bash
java -version     # 21.x
mvn -version      # Apache Maven 3.9.16, Java version 21
```
---

## Build and Run

```bash
git clone <repository-url>
cd sfs

mvn clean install
```

Run the UI:

```bash
mvn -pl sfs-ui spring-boot:run
```

Then open <http://localhost:8080>.

"The dashboard, Files and Search screens are implemented. The other four navigation destinations are
deliberately not clickable until their task lands, so no link produces a 404.

The application starts with the mock profile active, which supplies in-memory stand-ins
for backend services that do not exist yet. Override with SFS_PROFILE when real services
arrive."

Run on a different port :

```bash
mvn -pl sfs-ui spring-boot:run -Dspring-boot.run.arguments=--server.port=9090
# or
java -jar sfs-ui/target/sfs-ui-0.1.0-SNAPSHOT.jar --server.port=9090
```

Run the test suite:

```bash
mvn test                      # all modules
mvn -pl sfs-ui test           # UI module only
```
---

## Repository Layout

```
sfs/
├── pom.xml                 # reactor: Java 21, Spring Boot BOM, dependency management
├── .gitignore
├── README.md
│
├── sfs-core/               # domain model — ZERO production dependencies
│   └── src/main/java/com/sfs/core/
│
├── sfs-contracts/          # service interfaces + request/response models
│   └── src/main/java/com/sfs/contracts/
│       ├── file/           # FileService, FileSummary, FileStatus, requests, results
│       └── search/         # SearchService, SearchQuery, SearchResult, evidence
│
└── sfs-ui/                 # server-rendered web UI
    ├── src/main/java/com/sfs/ui/
    │   ├── controller/     # request handling only, no domain logic
    │   ├── mock/           # in-memory stand-ins, active under the "mock" profile
    │   └── view/           # immutable presentation view models
    ├── src/main/resources/
    │   ├── application.properties
    │   ├── templates/      # Thymeleaf views (layout.html + one per screen)
    │   └── static/css/     # local stylesheet, no CDN
    └── src/test/java/com/sfs/ui/
```

Dependency direction is one-way and non-negotiable:

```
sfs-ui  ──►  sfs-contracts  ──►  sfs-core
```
### Technology choices

| Decision | Choice | Rationale |
|---|---|---|
| Language | Java 21 LTS | Records for immutable value objects, sealed types for state machines, virtual threads for background analysis |
| Build | Maven 3.9.16 multi-module | Build-enforced architectural boundaries |
| Web | Spring Boot 4.1.0 + Thymeleaf | Server-rendered; |
| Frontend | Vanilla JS only |  UI framework |
| Testing | JUnit 5 + AssertJ | |
---

## Milestone Roadmap

| # | Milestone | Status |
|---|-----------|--------|
| 01 | User / Interface Layer | 🔨 In progress |
| 02 | Application & API Layer | ⬜ Planned |
| 03 | File Lifecycle Manager | ⬜ Planned |
| 04 | Semantic Engine | ⬜ Planned |
| 05 | File-Type Adapter Framework — Text Adapter | ⬜ Planned |
| 06 | Semantic Representation — Semantic DNA  | ⬜ Planned |
| 07 | Reconstruction Rules System | ⬜ Planned |
| 08 | Memory System — Database + Vector Index | ⬜ Planned |
| 09 | Semantic Search Engine | ⬜ Planned |
| 10 | SFS Reconstruction Model  | ⬜ Planned |
| 11 | Reconstruction Engine  | ⬜ Planned |
| 12 | Evaluation & Fidelity System  | ⬜ Planned |
| 13 | Security & Privacy System  | ⬜ Planned |
| 14 | Infrastructure / Observability / Testing  | ⬜ Planned |

---

## Development Protocol

Development follows a strict, granular loop:

```
Milestone → Phase → implementation + tests → integration
   → compile → test → run → review → commit → next task
```
---

## Research Metrics

Reconstruction quality is decomposed rather than reported as a single score, so a strong semantic score cannot conceal factual loss.

| Metric | Measures |
|---|---|
| Semantic Similarity | How closely reconstructed meaning matches the source |
| Structural Similarity | Hierarchy, section order, paragraph and list organization |
| Factual / Content Fidelity | Preservation of claims, entities, numbers, dates, relationships |
| Critical Fact Score | Explicit survival of facts marked critical in Semantic DNA |
| Completeness | Proportion of required semantic information that survived |
| Confidence Calibration | Whether stated confidence matches observed correctness |
| Semantic Memory Size | Persistent bytes required for the Semantic Record |
| Knowledge Preservation Density | Fidelity retained per unit of persistent semantic storage |
| Search / Reconstruction Latency | Query-to-results and request-to-artifact timing |
| Analysis Cost | CPU / RAM / time to build Semantic DNA |

---

## Known Limitations

**By design, in V1:**

- Text files only. Image, audio, video, code, PDF, spreadsheet and database adapters are deferred — the adapter framework accommodates them without redesign.
- Reconstruction is semantic and approximate. Byte-for-byte recovery is out of scope and impossible by construction.
- Random or high-entropy exact values (API keys, account numbers) cannot be semantically inferred; exact recovery requires the encrypted secure store, under policy.
- Passwords are non-reversible by default and are not treated as reconstructable secrets.
- Single-machine deployment. Distributed operation is not addressed.

**Current:**
Only the dashboard route exists. Six of the seven navigation destinations are inert.
The Files screen is backed by an in-memory mock, not the File Lifecycle Manager. Imported files are not persisted, are not analyzed, and are lost on restart.
Deleting raw data is a single click with no confirmation step. Acceptable while the data is mock; it needs an interstitial before any real implementation.
The file list is not paginated.
Search is keyword overlap against a fixed four-document corpus, not semantic retrieval. Relevance scores are illustrative and carry no measured meaning.
Search results are not paginated and cannot be filtered by status or date.
sfs-core and sfs-contracts contain package only.
Responsive behaviour is minimal — navigation wraps, but there is no mobile-specific layout. Accessibility support is basic (skip link, aria-current, aria-disabled) and has not been screen-reader audited.
spring-boot-starter-web is deprecated in Spring Boot 4 in favour of spring-boot-starter-webmvc; the rename is pending a dedicated task.
The architecture guard rail is a source-level import scan, not bytecode analysis; it may be replaced with a bytecode-level tool such as ArchUnit.
Built and run on Windows 10 with JDK 21.0.12 and Maven 3.9.16. Other platforms are untested.

---

## License

Copyright © 2026 Regullacharith

All rights reserved.

The SFS source code, documentation, architecture, and original
project materials are proprietary to the copyright holder.

No permission is granted to reproduce, modify, distribute,
sublicense, or commercially exploit this project without
express written permission from the copyright holder.

Third-party dependencies and components remain subject to
their respective licenses.

See [`LICENSE`](LICENSE) for the complete and authoritative terms. Where this summary and
the `LICENSE` file differ, the `LICENSE` file governs.
---

