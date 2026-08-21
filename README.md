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

## Project Status

**Version:** V1  
**Status: Complete:** User / Interface Layer  
**Implementation language:** Java 21  
**Current file scope:** Text files   

ui layer has been completed and integrated into the `main` branch.
SFS V1 is being developed incrementally.

The project is **not yet V1-complete**.

Release tag:
`v1-m01`

completed work establishes the user-facing and application-facing foundation. Later will implement the actual semantic, memory, search, reconstruction, evaluation, and security subsystems.

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
│   ├── Object ID
│   ├── Name
│   ├── Type
│   └── Version
│
├── Raw Data
│
├── Semantic DNA
│   ├── Topics
│   ├── Concepts
│   ├── Entities
│   ├── Facts
│   ├── Relationships
│   ├── Structure
│   ├── Semantic Representation
│   ├── Embeddings
│   └── Behaviour / Context
│
└── Reconstruction Rules
```

The **Semantic File** is a logical abstraction. It does not mean that SFS V1 replaces the operating system's physical filesystem.

---

# Semantic DNA

**Semantic DNA** is the persistent semantic representation of a file.

It is designed to capture the characteristics needed to understand and later reconstruct the file without requiring the original raw bytes to remain available.

Depending on the supported file type, Semantic DNA can represent:

- identity
- topics
- concepts
- entities
- facts
- relationships
- document structure
- semantic representation
- vector embeddings
- behavioural/contextual information
- reconstruction constraints
- fidelity information
- security references

Semantic DNA is therefore more than a conventional file summary.

It is a structured representation intended to act as the semantic memory of the object.

---

# Object ID

Each Semantic File has a persistent **Object ID**.

The Object ID provides the logical identity of the semantic object independently of:

- filename
- filesystem path
- physical storage location
- current file extension

This becomes important when the original raw file is no longer present.

Conceptually:

```text
Original File
     │
     └── Object ID: SFS-OBJECT-...
                    │
                    ▼
             Semantic Memory
                    │
                    ▼
             Search / Inspect
                    │
                    ▼
               Reconstruct
```

The Object ID is therefore a core connection between the original file and its persistent semantic record.

---

# Memory Database

The **Memory Database** stores the persistent semantic information associated with Semantic Files.

A conceptual record may contain:

```text
Object ID
Metadata
Semantic DNA
Topics
Concepts
Entities
Facts
Relationships
Structure
Embedding references
Reconstruction Rules
Versions
Fidelity information
Security references
Lifecycle information
```

The Memory Database is intended to be the canonical structured memory.

The Vector Index is a retrieval mechanism and should not become the sole source of truth.

---

# Semantic Search

SFS is designed to allow files to be searched according to **meaning**, not only filename or path.

For example, a user may search:

```text
"the document describing how SFS reconstructs deleted text files"
```

The system can use semantic retrieval to identify relevant Object IDs and their corresponding Semantic DNA.

Conceptually:

```text
User Query
    │
    ▼
Query Representation
    │
    ▼
Vector / Semantic Retrieval
    │
    ▼
Structured Filtering / Ranking
    │
    ▼
Matching Object IDs
    │
    ▼
Semantic Records
```

Search and reconstruction are separate operations.

A search result does not automatically reconstruct a file.

The user explicitly chooses when reconstruction should occur.

---

# Semantic Reconstruction

SFS does not define reconstruction as forensic recovery of the original bytes.

Instead, reconstruction means:

> **Creating a new artifact from the surviving semantic representation of an object.**

The reconstruction pipeline is:

```text
Object ID
    │
    ▼
Semantic DNA
    │
    ▼
Reconstruction Rules
    │
    ▼
Reconstruction Plan
    │
    ▼
SFS Reconstruction Model
    │
    ▼
Reconstruction Engine
    │
    ▼
New Artifact
    │
    ▼
Evaluation
```

The reconstructed result may differ from the original wording or byte sequence while still preserving its intended meaning and structure.

---

# Reconstruction Rules

The project previously referred to this concept as **Reconstruction Grammar**.

The project terminology is now:

**Reconstruction Rules**

Reconstruction Rules define constraints and information that should guide reconstruction.

They can describe:

- required concepts
- important facts
- entities
- relationships
- document structure
- ordering
- consistency requirements
- reconstruction priorities
- confidence requirements
- restrictions against unsupported additions

The rules belong to the semantic representation, while their execution belongs to the reconstruction subsystem.

---

# Reconstruction Model

SFS V1 is intentionally designed so that reconstruction does not depend permanently on a large general-purpose LLM.

The architecture allows a future reconstruction model to be:

- deterministic where possible
- lightweight
- specialized
- replaceable
- optimized specifically for semantic reconstruction

The project will investigate whether a small/custom reconstruction model can provide sufficient fidelity for the target workload.

A model is not considered successful merely because it produces fluent text.

It must preserve the information represented by Semantic DNA and Reconstruction Rules.

---

# Fidelity and Evaluation

SFS reconstruction is evaluated using multiple dimensions.

### Semantic Similarity

Does the reconstructed artifact preserve the meaning of the original?

### Structural Similarity

Does it preserve important organizational characteristics such as:

- sections
- ordering
- relationships
- hierarchy
- document structure

### Factual / Content Fidelity

Does it preserve important facts and content without introducing unsupported information?

Additional measurements can include:

- entity fidelity
- relationship fidelity
- information completeness
- reconstruction confidence
- storage cost
- reconstruction latency

The previously discussed target of approximately **87–92% or higher** is an experimental target, not a guaranteed result. It should only be reported as achieved after reproducible evaluation.

---

# File Lifecycle

The intended lifecycle is:

```text
                 ┌──────────────┐
                 │   Live File  │
                 └──────┬───────┘
                        │
                        ▼
                    Register
                        │
                        ▼
                Semantic Analysis
                        │
                        ▼
                  Semantic DNA
                        │
                        ▼
                   Validation
                        │
                        ▼
              Durable Memory Commit
                        │
                        ▼
              Authorized Deletion
                        │
                        ▼
              Raw Data Removed
                        │
                        ▼
              Semantic Memory Remains
                        │
              ┌─────────┼─────────┐
              ▼         ▼         ▼
            Search   Inspect   Reconstruct
                                  │
                                  ▼
                              Evaluate
```

The semantic memory must be successfully persisted and validated before the raw-data deletion stage is allowed by the configured lifecycle policy.

SFS therefore does not depend on attempting to recover overwritten physical storage.

---

# Security and Sensitive Data

Semantic representations can contain sensitive information.

Examples include:

- passwords
- API keys
- authentication tokens
- phone numbers
- email addresses
- physical addresses
- account identifiers
- private credentials
- other confidential values

SFS should not blindly place such values into:

- ordinary Semantic DNA
- vector embeddings
- application logs
- debugging output
- unrestricted search results

The security architecture therefore uses policies for sensitive information and protected storage/encryption where authorized retention is required.

Passwords are not treated as ordinary reconstructable content.

Security implementation is part of  future  and should not be confused with current state.

---

# V1 Scope

SFS V1 is deliberately restricted to a manageable initial scope.

## Included

- Java 21
- Text files
- Semantic File abstraction
- Object ID
- Semantic DNA
- Text Adapter
- Semantic Engine
- Automatic adapter selection
- Memory Database
- Vector Index
- Semantic Search
- Vector embeddings
- Reconstruction Rules
- SFS Reconstruction Model
- Reconstruction Engine
- Semantic Reconstruction
- Single-click reconstruction
- Semantic similarity
- Structural similarity
- Factual/content fidelity
- Reconstruction evaluation
- Iterative Semantic DNA improvement
- Encryption
- Sensitive-data policies
- Storage/fidelity measurements
- UI

## Current V1 file support

```text
Text
└── Text Adapter
```

The current implementation does **not** attempt to support every file format.

---

# Future File-Type Adapters

The adapter architecture is designed for extension.

Potential future adapters include:

```text
Text
Source Code
Images
Audio
Video
PDF
Spreadsheets
Database Records
Other Structured / Unstructured Formats
```

These are future extensions, not current V1 text implementation.

The intended design is that the **Semantic Engine automatically selects the appropriate adapter** according to the input type.

Conceptually:

```text
Input File
    │
    ▼
File-Type Detection
    │
    ▼
Semantic Engine
    │
    ├── Text Adapter
    ├── Image Adapter       [Future]
    ├── Audio Adapter       [Future]
    ├── Video Adapter       [Future]
    ├── Code Adapter        [Future]
    └── Other Adapters      [Future]
```

This adapter architecture allows SFS to evolve without changing the entire system for every new file type.

---

# V1 Architecture

```text
SFS V1
│
├── 01. User / Interface Layer
│
├── 02. Application & API Layer
│
├── 03. File Lifecycle Manager
│
├── 04. Semantic Engine
│
├── 05. File-Type Adapter Framework
│       └── Text Adapter [V1]
│
├── 06. Semantic Representation System
│       └── Semantic DNA
│
├── 07. Reconstruction Rules System
│
├── 08. Memory System
│       ├── Memory Database
│       └── Vector Index
│
├── 09. Semantic Search Engine
│
├── 10. SFS Reconstruction Model
│
├── 11. Reconstruction Engine
│
├── 12. Evaluation & Fidelity System
│
├── 13. Security & Privacy System
│
└── 14. Infrastructure / Observability / Testing
```

The architecture is developed incrementally.

A component appearing in the architecture does not necessarily mean that its production implementation is already complete.

---

# completed  Implementation — User/Interface Layer

The current repository  focuses on:

## M01 — User / Interface Layer

M01 establishes the  UI and application-facing boundaries required for subsequent .

 includes:

- UI shell
- navigation
- file import interaction
- object inspection views
- Semantic DNA inspection UI
- semantic search UI
- reconstruction UI
- evaluation UI
- security settings UI
- application-facing contracts
- deterministic mock services
- UI/controller tests
- view-model tests
- integration tests
- architecture-boundary tests

The mock services exist to allow the interface to be developed before the real backend components are implemented.

### Important

The following should **not** be interpreted as completed merely because corresponding UI screens or contracts exist:

- real Semantic Engine
- real Semantic DNA generation
- Memory Database
- real Vector Index
- real Semantic Search Engine
- SFS Reconstruction Model
- Reconstruction Engine
- real Evaluation Engine
- production Security/Privacy System

Those belong to later implementation.

---

# Milestone Roadmap

SFS V1 is being developed through the following milestones:

| Milestone | Component | Purpose |
|---|---|---|
| M01 | User / Interface Layer | user-facing foundation |
| M02 | Application & API Layer | Application orchestration and APIs |
| M03 | File Lifecycle Manager | File registration, deletion and lifecycle |
| M04 | Semantic Engine | Semantic processing and adapter routing |
| M05 | File-Type Adapter Framework | Text adapter and future adapter architecture |
| M06 | Semantic Representation System | Semantic DNA |
| M07 | Reconstruction Rules System | Reconstruction constraints and rules |
| M08 | Memory System | Memory Database and Vector Index |
| M09 | Semantic Search Engine | Semantic retrieval and ranking |
| M10 | SFS Reconstruction Model | Specialized reconstruction model |
| M11 | Reconstruction Engine | Reconstruction pipeline |
| M12 | Evaluation & Fidelity System | Fidelity measurement and improvement |
| M13 | Security & Privacy System | Encryption and sensitive-data policies |
| M14 | Infrastructure / Observability / Testing | Reliability, monitoring and final integration |

**V1 is complete only after sfs been implemented and verified.**

---

# Technology

## Primary Language

**Java 21**

## Current Application Stack

- Java 21
- Maven
- Spring Boot
- Thymeleaf
- HTML
- CSS
- JUnit / Spring testing infrastructure

---

# Repository Structure

```text
Semantic-File-System/
│
├── sfs-core/
│   └── Core/domain implementation
│
├── sfs-contracts/
│   └── Shared application contracts
│
├── sfs-ui/
│   ├── Controllers
│   ├── View Models
│   ├── Templates
│   ├── Static CSS
│   └── UI tests
│
├── pom.xml
├── README.md
├── LICENSE
└── .gitignore
```

---

# Build

The project requires **Java 21**.

Verify:

```bash
java --version
```

Then build and test:

```bash
mvn clean test
```

For a complete build:

```bash
mvn clean package
```
---

# Running the Application

From the project root:

```bash
mvn spring-boot:run -pl sfs-ui
```
---

# Testing

SFS uses multiple levels of verification:

```text
Unit Tests
    │
    ▼
Controller Tests
    │
    ▼
Contract Tests
    │
    ▼
Integration Tests
    │
    ▼
Architecture Tests
    │
    ▼
End-to-End Verification
```

Tests validates both:

1. whether functionality works; and
2. whether components remain within their intended architectural boundaries.

---
# Design Principles

SFS follows several core principles.

### 1. Semantic information is first-class

The system should preserve what information means, not merely where bytes are located.

### 2. Raw data and semantic memory are separate

Semantic memory can survive the authorized deletion of raw data.

### 3. Object identity persists independently of physical location

The Object ID represents the logical object.

### 4. Search and reconstruction are separate operations

Finding an object does not automatically reconstruct it.

### 5. Reconstruction is constrained

Semantic DNA and Reconstruction Rules should constrain generated results.

### 6. Fidelity is measurable

Reconstruction quality must be evaluated rather than assumed.

### 7. Sensitive information requires explicit policy

Secrets should not casually enter semantic memory or embeddings.

### 8. Adapters isolate file-type complexity

Each file type can have a specialized adapter.

### 9. Components are replaceable

The architecture  allows implementations such as models, vector stores, and persistence technologies to evolve .

---

# What SFS Is Not

SFS should not be confused with:

- conventional file backup
- disk undelete software
- forensic recovery
- ordinary compression
- a physical replacement for NTFS/ext4/APFS/etc.
- a guaranteed byte-level recovery mechanism
- an LLM-only application

If the original raw bytes have been destroyed, SFS does not claim that those exact bytes can magically be recovered.

Instead, SFS intentionally preserves semantic memory before deletion and uses that memory for later reconstruction.

---

# Research Questions

The project explores questions such as:

### How much information is necessary?

How small can a semantic representation become while still preserving enough information for useful reconstruction?

### How faithful can reconstruction become?

Can a compact semantic representation preserve:

- meaning
- structure
- facts
- relationships
- important entities

with high fidelity?

### Can a specialized small model replace a general LLM?

Can a tiny or custom reconstruction model perform sufficiently well when Semantic DNA and Reconstruction Rules provide strong constraints?

### How should sensitive information be represented?

How can names, addresses, phone numbers, credentials, API keys, and similar values be handled without storing unnecessary collections of sensitive values inside semantic memory or embeddings?

### Can semantic memory scale?

What are the storage and performance characteristics when millions of semantic objects exist?

---

# Limitations

The current project has important limitations.

- V1 initially supports text files only.
- Semantic reconstruction is not guaranteed to be exact.
- Information that was never captured cannot be reconstructed reliably.
- Arbitrary high-entropy values cannot generally be inferred from semantic meaning.
- Exact sensitive values require appropriate protected storage when retention is authorized.
- Passwords are not ordinary reconstructable information.
- Reconstruction quality must be experimentally measured.
- Storage reduction depends strongly on the source data.
- The current UI contains mocks where backend implementations do not yet exist.
- The physical filesystem itself is not being replaced by SFS in V1.

---

# Future Direction

After the text pipeline becomes stable, the adapter framework can expand:

```text
V1
 │
 └── Text
       │
       ▼
Future
 ├── Source Code
 ├── Images
 ├── Audio
 ├── Video
 ├── PDF
 ├── Spreadsheets
 └── Other Data Types
```

For visual and multimedia data, future adapters may use specialized recognition, representation, and reconstruction techniques rather than treating every file as text.

---

# Project Goal

The long-term goal is to explore whether a file system can preserve **semantic identity and memory** independently from the original raw representation.

The intended model is:

```text
Traditional File System

File → Bytes → Storage
```

versus:

```text
SFS

File
 │
 ├── Raw Representation
 │
 └── Semantic Representation
          │
          ▼
      Semantic Memory
          │
          ▼
    Search / Understand
          │
          ▼
      Reconstruct
```

This is the core research direction behind the Semantic File System.

---

# License

Copyright © 2026 Regullacharith

All rights reserved.

The SFS source code, documentation, architecture, original research
materials, and other original project materials are protected by
copyright and are not granted for reproduction, modification,
distribution, sublicensing, or commercial exploitation without
permission from the copyright holder.

Third-party libraries, frameworks, models, datasets, and other external
components remain subject to their respective licenses.

See (LICENSE) for the complete notice.

---

# Project Status

```text
SFS V1
│
├── M01  User / Interface Layer          DEVELOPED
├── M02  Application / API Layer         PLANNED
├── M03  File Lifecycle Manager          PLANNED
├── M04  Semantic Engine                 PLANNED
├── M05  Adapter Framework / Text        PLANNED
├── M06  Semantic DNA                    PLANNED
├── M07  Reconstruction Rules            PLANNED
├── M08  Memory Database / Vector Index  PLANNED
├── M09  Semantic Search                 PLANNED
├── M10  Reconstruction Model            PLANNED
├── M11  Reconstruction Engine           PLANNED
├── M12  Evaluation & Fidelity           PLANNED
├── M13  Security & Privacy              PLANNED
└── M14  Infrastructure / Testing        PLANNED
```

**SFS V1 is incomplete until the planned stages have been completed and verified.**

---

## Semantic File System

**Preserve meaning. Search memory. Reconstruct when needed.**
