# Semantic File System (SFS)

**Semantic File System (SFS)** is a Java-based system for representing files as semantic objects rather than treating them only as byte containers.

The central idea is to preserve the **meaning, structure, identity, facts, entities, and relationships** associated with a file so that its semantic representation can remain useful independently of the original raw data.

The current implementation focuses on a **text-file scope** and provides a lightweight web interface backed by explicit application and API contracts.

---

## What Is SFS?

A conventional file can be viewed primarily as:

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
│   └── Context
│
└── Reconstruction Rules
```

The **Semantic File** is a logical abstraction. Its identity is represented by an Object ID rather than being tied only to a filename or physical storage location.

The long-term purpose of this model is to allow semantic information to remain available after authorized removal of raw data.

---

## What SFS Is Not

| Not this | Reason |
|---|---|
| A byte-for-byte recovery system | Semantic information cannot recreate discarded arbitrary bytes exactly. |
| A physical filesystem or kernel module | SFS currently defines a logical object and application/API model rather than a new physical filesystem. |
| A conventional backup system | The purpose is semantic preservation and reconstruction, not exact byte preservation. |
| A compression tool | The important quantity is preserved knowledge and semantic fidelity, not byte-level compression ratio. |
| An LLM wrapper | Reconstruction and semantic processing are defined behind explicit system boundaries and contracts. |
| A guaranteed-accuracy system | Reconstruction quality must be measured from actual evaluation rather than assumed. |

---

## Architecture

SFS is organized into distinct subsystems with explicit dependency boundaries.

```text
User / Interface Layer
        │
        ▼
Application & API Layer
        │
        ▼
File Lifecycle Manager
        │
        ▼
Semantic Engine
        │
        ▼
File-Type Adapter Framework
        │
        ▼
Semantic Representation System
        │
        ├──────────────► Reconstruction Rules System
        │
        ├──────────────► Memory System
        │                    ├── Memory Database
        │                    └── Vector Index
        │
        └──────────────► Security & Privacy System
                         │
                         ▼
                 Semantic Search Engine
                         │
                         ▼
                 Reconstruction Model
                         │
                         ▼
                 Reconstruction Engine
                         │
                         ▼
                 Evaluation & Fidelity
```

The architecture separates interface concerns, application orchestration, semantic processing, memory, reconstruction, evaluation, and security.

---

## User / Interface Layer

The user interface provides a lightweight web interface for interacting with SFS.

The current UI includes:

- Dashboard
- File management
- File import
- File analysis interaction
- Semantic search interface
- Object inspection
- Semantic DNA inspection
- Reconstruction interface
- Evaluation interface
- Security and privacy information

The UI uses application-facing contracts and deterministic mock services where the corresponding backend implementation is not part of the current implementation.

The interface is intentionally lightweight rather than being a heavy client application.

---

## Application & API Layer

The **Application & API Layer** establishes the application boundary between the user interface and SFS services.

It separates HTTP/API concerns from application operations and defines stable contracts for the rest of the system.

## Responsibilities

- Application services
- REST API layer
- Versioned API contracts
- Request models
- Response models
- DTOs
- Object ID handling
- Request validation
- Error handling
- File operation boundaries
- Analyze operation
- Semantic deletion operation
- Undo deletion operation
- Permanent purge operation
- Semantic DNA inspection boundary
- Semantic search boundary
- Reconstruction boundary
- Evaluation boundary
- Authentication boundary
- Authorization boundary
- Explicit destructive-operation confirmation
- Reversible deletion
- Mock service integrations

## API Version

The API is exposed under:

```text
/api/v1
```

API contracts are defined independently from the implementation of the underlying SFS subsystems.

## Application Flow

```text
HTTP Request
     │
     ▼
Controller
     │
     ▼
Application Service
     │
     ├── Validation
     ├── Authentication
     ├── Authorization
     ├── Confirmation
     └── Lifecycle validation
     │
     ▼
SFS Service Contract
     │
     ▼
Service Implementation
```

Business operations are not placed exclusively inside controllers.

---

## File Lifecycle Boundary

The application/API layer delegates file lifecycle behavior to the `sfs-lifecycle` module. The lifecycle separates analysis, semantic-memory commitment, reversible deletion, and permanent raw-data release:

```text
REGISTERED
    │
    ▼
ANALYZING
    │
        ├──────────────► FAILED
        │                  │
        │                  └──────────────► ANALYZING
        │
        ▼
ANALYZED
    │
    ├──────────────► MEMORIZABLE ─────► MEMORY_COMMITTED
    │
    └── authenticated, authorized, confirmed ──► SOFT_DELETED
    │
    ├──► UNDO ──► ANALYZED
    │
    └──► AUTHORIZED PURGE
    │
    ▼
 MEMORIZED
```

`MEMORIZED` is terminal: the semantic record remains while raw data is no longer present. Interrupted memorization can be recovered by returning the object from `MEMORIZABLE` to `ANALYZED`.

Normal deletion is reversible. Permanent raw-data removal is a separate purge operation controlled by the raw-deletion gate.

## `sfs-lifecycle` Module

The lifecycle module is the subsystem responsible for the file state machine and lifecycle operations. It implements the shared `FileService` contract and depends on `sfs-core` and `sfs-contracts`.

```text
sfs-lifecycle/src/main/java/com/sfs/lifecycle/
├── core/
│   ├── FileLifecycleManager
│   └── AnalysisDispatcher
├── state/
│   ├── FileState
│   ├── LifecycleStateMachine
│   └── IllegalLifecycleTransitionException
├── model/
│   ├── SemanticFile
│   ├── FileMetadata
│   ├── FileVersion
│   ├── ContentDigest
│   ├── LifecycleEvent
│   ├── LifecycleEventType
│   └── DeletionPolicy
├── identity/
│   └── ObjectIdService
├── gate/
│   └── RawDeletionGate
├── audit/
│   ├── LifecycleAuditLog
│   └── LifecycleAuditAdapter
└── store/
        ├── RawContentStore
        └── InMemoryRawContentStore
```

### Lifecycle Responsibilities

- Register text files and assign stable Object IDs
- Store raw content separately from semantic file metadata
- Track file metadata, content digests, and versions
- Enforce legal lifecycle transitions
- Dispatch and complete semantic analysis
- Record certified semantic DNA versions
- Commit semantic memory through an explicit intermediate state
- Support reversible soft deletion and undo
- Gate permanent raw-data release
- Retain the semantic record after raw-data purge
- Record lifecycle events, refusals, actors, reasons, and operation timing
- Recover interrupted memorization operations

### Lifecycle State Model

The module defines these states:

| State | Meaning |
|---|---|
| `REGISTERED` | The file has an Object ID and retained raw content, but analysis has not started. |
| `ANALYZING` | Semantic analysis is in progress. |
| `ANALYZED` | Analysis completed and the semantic representation is available. |
| `MEMORIZABLE` | The semantic record has passed validation and is ready for memory commitment. |
| `MEMORY_COMMITTED` | Semantic memory has been committed while raw content remains available. |
| `SOFT_DELETED` | The file is logically deleted but can still be restored. |
| `MEMORIZED` | Raw content has been released and the semantic record remains. |
| `FAILED` | Analysis failed and may be retried through the state machine. |

---

## Destructive Operation Security

Destructive operations require:

1. **Authentication**
2. **Authorization**
3. **Explicit confirmation**
4. **Reversible-by-design deletion**

Authentication identifies the principal performing the operation.

Authorization determines whether that principal has the required capability.

Confirmation prevents accidental destructive actions.

Loopback binding can be used as a deployment restriction, but it is **not** a security mechanism.

---

## API Error Model

The API uses a defined error response model so that clients receive structured failures rather than relying on implementation-specific exceptions.

The error contract distinguishes conditions such as:

- Invalid request
- Validation failure
- Authentication failure
- Authorization refusal
- Resource conflict
- Invalid lifecycle state
- Unsupported operation
- Internal application failure

Error responses do not expose sensitive request data or plaintext protected values.

---

## Core Concepts

## Object ID

An **Object ID** is the stable logical identity of a Semantic File.

It is independent of:

- filename
- physical path
- storage address
- rename operations

This allows the logical object to remain identifiable even when its physical representation changes.

---

## Semantic DNA

**Semantic DNA** is the structured semantic representation associated with a Semantic File.

Conceptually:

```text
Semantic DNA
├── Identity
├── Summary
├── Topics
├── Concepts
├── Entities
├── Facts
├── Relationships
├── Structure
├── Semantic Representation
├── Context
└── Reconstruction Rules
```

Semantic DNA is not merely a text summary. It is intended to represent the information required for semantic search, reconstruction, and evaluation.

---

## Semantic Deletion

SFS separates raw data from semantic memory.

The conceptual workflow is:

```text
Live File
    │
    ▼
Semantic Representation
    │
    ▼
Validated Semantic Record
    │
    ▼
Authorized Deletion
    │
    ├──────────────► Raw Data Removed
    │
    ▼
Semantic Memory Retained
```

This makes deletion a lifecycle operation rather than simply removing an object from existence.

---

## Security and Sensitive Data

Semantic representations can contain sensitive information.

Examples include:

- Passwords
- API keys
- Authentication tokens
- Phone numbers
- Email addresses
- Physical addresses
- Account identifiers
- Private credentials
- Other confidential values

SFS does not treat all sensitive values as ordinary reconstructable content.

The application boundary enforces the principle that sensitive information must not be unnecessarily exposed through:

- Semantic DNA
- Vector embeddings
- Application logs
- Debugging output
- Unrestricted search responses

Protected references can be used for values that require controlled retention.

Passwords are treated as non-reconstructable values rather than ordinary semantic content.

---

## Current Text Scope

The current file scope is intentionally restricted to text:

```text
Text
└── Text Adapter
```

The text boundary is based on the content being processed as text rather than trusting a browser-supplied content type alone.

---

## Core Design Principles

### 1. Semantic information is first-class

SFS preserves what information means, not merely where bytes are located.

### 2. Raw data and semantic memory are separate

Semantic memory can survive authorized removal of raw data.

### 3. Object identity persists independently of physical location

The Object ID represents the logical object.

### 4. Search and reconstruction are separate operations

Finding an object does not automatically reconstruct it.

### 5. Reconstruction is constrained

Reconstruction uses semantic information and explicit structural constraints rather than treating the task as unrestricted text generation.

### 6. Sensitive information is protected

Secrets and other protected values are not treated as ordinary semantic content.

### 7. Application boundaries are explicit

Controllers, application services, contracts, and backend services have distinct responsibilities.

---

## Repository Structure

```text
Semantic-File-System/
│
├── sfs-core/
│   └── Core/domain implementation
│
├── sfs-contracts/
│   └── Shared service contracts
│
├── sfs-lifecycle/
│   ├── File lifecycle manager
│   ├── Lifecycle state machine
│   ├── Object ID and version tracking
│   ├── Raw-data deletion gate
│   ├── Lifecycle audit log
│   ├── Raw-content store
│   └── Lifecycle tests
│
├── sfs-app/
│   ├── Application services
│   ├── REST/API contracts
│   ├── DTOs
│   ├── Validation
│   ├── Error model
│   ├── Security contracts
│   └── Application tests
│
├── sfs-ui/
│   ├── Controllers
│   ├── View Models
│   ├── Templates
│   ├── Static CSS
│   ├── Mock services
│   └── UI tests
│
├── docs/
│   └── API specifications and project documentation
│
├── pom.xml
├── README.md
├── LICENSE
└── .gitignore
```

---

## Technology

## Primary Language

**Java 21**

## Application Stack

- Java 21
- Maven
- Spring Boot
- Thymeleaf
- HTML
- CSS
- JUnit
- Spring testing infrastructure

---

## Build

The project requires Java 21.

Verify the Java installation:

```bash
java --version
```

Build and run the complete test suite:

```bash
mvn clean test
```

Create the application packages:

```bash
mvn clean package
```

---

## Running the Application

From the project root:

```bash
mvn spring-boot:run -pl sfs-ui
```

The web interface is available through the configured Spring Boot application port.

---

## Testing

The project contains tests for:

- Application services
- API contracts
- Request validation
- Response contracts
- Object ID validation
- Lifecycle rules
- Authentication requirements
- Authorization requirements
- Destructive-operation confirmation
- Reversible deletion
- Undo
- Permanent purge
- Invalid state transitions
- API error responses
- Security contracts
- Controller/application integration
- View models
- UI behavior
- Architecture boundaries

Tests are intended to enforce both functional behavior and architectural boundaries.

---

## Project Structure at Runtime

```text
Browser
   │
   ▼
SFS UI
   │
   ▼
Application & API Layer
   │
   ▼
Contracts
   │
   ├── File Operations
   ├── Semantic Operations
   ├── Search Operations
   ├── Reconstruction Operations
   ├── Evaluation Operations
   └── Security Contracts
```

This structure keeps the UI independent from direct dependencies on backend implementation details.

---

## License

Copyright © 2026 Regullacharith

All rights reserved.

The SFS source code, documentation, architecture, original research materials, and other original project materials are protected by copyright and are not granted for reproduction, modification, distribution, sublicensing, or commercial exploitation without permission from the copyright holder.

Third-party libraries, frameworks, models, datasets, and other external components remain subject to their respective licenses.

See `LICENSE` for the complete notice.

---

## Semantic File System

**Preserve meaning. Search memory. Reconstruct when needed.**

