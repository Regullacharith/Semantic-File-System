# SFS V1 — Project Records (internal working file)

**This file is NOT part of the GitHub repository.** It lives in `docs/` in the local
workspace only and is excluded from every delivered zip. It is the single place where
decision records, the changelog and verification history are kept so that no program
file ever carries decision metadata. The only repository documentation is `README.md`.

---

## 1. Decision log

Decisions are numbered `D-0xx` in the order they were made. The table below restates
each decision's content. The program files do not reference these numbers.

| # | Milestone | Decision |
|---|---|---|
| D-001 | M01 | Stack: Spring Boot with `spring-boot-starter-webmvc` (embedded server + MVC controllers) and `spring-boot-starter-thymeleaf` (server-rendered HTML). |
| D-002 – D-004 | M01 | Early M01 working decisions (module layout, naming, seed-data approach). Full text was recorded in the M01 working session; not restated here because the program files never depended on their wording. |
| D-005 | M01 | Spring Boot version frozen at 4.1.0 (imported as BOM, not parent POM). |
| D-006 – D-008 | M01–M02 | Early M02 working decisions (DTO/error-model conventions, job registry approach). Full text in the M02 working session. |
| D-009 | M02 | `spring-boot-starter-web` renamed to `spring-boot-starter-webmvc` (deprecated in Spring Boot 4; same transitive set). |
| D-010 | M03 | New Maven module `sfs-lifecycle` for the File Lifecycle Manager (mirrors the M02 `sfs-app` precedent; `sfs-core` keeps `ObjectId` and the future M06 DNA types). |
| D-011 | M03 | `RawContentStore` port with an in-memory implementation; live objects retain content (the deletion gate releases it; M04 analysis reads it). Content is never logged and is released on purge. Durable storage stays M08. Supersedes the M01 mock behavior of discarding content immediately. |
| D-012 | M03 | Authoritative `FileState` lives in `sfs-lifecycle`; `FileStatus` becomes the UI projection and gains `MEMORY_COMMITTED`. `MEMORIZABLE` is an intra-operation state and is not projected. |
| D-013 | M03 | Purge terminal state remains `MEMORIZED` (raw bytes released, Semantic Record retained). `MEMORY_COMMITTED` (memory durable, raw retained) and `MEMORIZED` are distinct states. |
| D-014 | M03 | Capability `MEMORIZE` added; `operator` and `custodian` development identities hold it. |
| D-015 | M03 | API grows 20 → 23 operations: `POST /api/v1/files/{objectId}/memorize`, `GET /api/v1/files/{objectId}/events`, `GET /api/v1/meta/lifecycle`. No new error codes; gate refusals reuse `INVALID_STATE_TRANSITION` (409). |
| D-016 | M03 | `AnalysisDispatcher` / completion port is the M04 seam; `StubAnalysisEngine` in `sfs-ui` keeps development analysis instant until the Semantic Engine arrives. |
| D-017 | M03 | Destructive `FileService` methods carry the authenticated `Principal` for audit attribution; the UI passes an honest `ui-dev` principal. |
| D-018 | M03 | The project carries no `docs/` in the repository; the only repository documentation is `README.md`. This records file (and any future changelog/records) lives in workspace `docs/`, is excluded from all zips and is never uploaded. |
| D-019 | M04 | New Maven module `sfs-engine` for the Semantic Engine. Pure Java, depends only on `sfs-core` + `sfs-contracts`; the engine defines its own ports (`ContentSource`, `AnalysisCompletionListener`) and `sfs-ui` composition wires engine ↔ lifecycle. |
| D-020 | M04 | Real engine-produced semantic records: `InMemorySemanticRecordStore` (in the engine, implementing the existing `SemanticRecordService` contract) replaces the fixture-based `MockSemanticRecordService`; seed records are produced by analyzing the seed files at startup through the real pipeline. A minimal deterministic protected-value detector (pattern-based, line-located, values never copied into views) keeps the protected-document rejection scenario honest; the real detector stays M13. |
| D-021 | M04 | Restart re-queue: objects stranded in `ANALYZING` are re-queued for analysis at startup (`LifecycleRecoveryRunner`), replacing the weaker mark-FAILED option. Complements the M03 `MEMORIZABLE` rollback. |
| D-022 | M04 | The UI architecture boundary test gains a composition exemption: files under `com/sfs/ui/config/` may import subsystem packages for bean wiring only; controllers and services must still go through contracts. The dev seeder moved to `com.sfs.ui.config` accordingly. |
| D-023 | M04 | Analysis observability: `requestAnalysis` transitions to `ANALYZING` before dispatching (eliminates a completion race), the dispatcher returns the job id which is audited on `ANALYSIS_STARTED`, analysis durations are audited on `ANALYSIS_SUCCEEDED`, analysis jobs appear in the unified `GET /api/v1/jobs/{jobId}` (JobRegistry gains a generic register + `TYPE_ANALYSIS`; `JobStatusResponse.fromAnalysisRecord`). Rejected engine submissions land in `FAILED` with an explicit reason and remain retryable. |
| D-024 | M04 | Analysis levels: `SHALLOW`/`STANDARD`/`DEEP` strategy exists; the V1 policy enables only `STANDARD` (also the default). Disabled levels are rejected with an explicit reason at the domain level; no API change. |
| D-025 | M04 | Engine `Analyzer` stages are independently testable and deterministic (no LLM, no new dependencies): text-parsing, summary, structure, concepts, topics, entities, facts, relationships, embeddings (64-dim feature hashing), protected-values, dna-builder; `DnaValidator` enforces completeness (full for ≥40-word documents, lenient below). Engine version string: `sfs-engine/0.1`; DNA schema version: `sfs-dna/0.1`. |
| D-026 | M05 | New Maven module `sfs-adapters` for the File-Type Adapter Framework (SPI, registry, resolver, Text Adapter). Pure Java, depends only on `sfs-core` + `sfs-contracts`; `sfs-engine` depends on `sfs-adapters` (one direction, no cycle). |
| D-027 | M05 | Unsupported types are rejected at IMPORT time (owner decision): the lifecycle gains an `ImportAcceptancePolicy` port (`rejectionReason(fileName, contentType)`); the UI wiring implements it via `AdapterResolver.find`, so registration and rename-to-unsupported are refused with an explicit reason. Import stays storage-neutral otherwise; the engine keeps a defensive resolver rejection (analysis job `FAILED`) as a second line. |
| D-028 | M05 | Adapter diagnostics ride in the `GET /api/v1/version` body: an `adapters` array (id, displayName, version, extensions, contentTypes, capabilities) plus `adapterResolutions`/`adapterRefusals` counters. Additive JSON fields; the frozen 23-operation API surface is unchanged. |
| D-029 | M05 | The V1 Text Adapter claims extensions {txt, text, md, markdown, log} and MIME {text/plain, text/markdown}, capabilities {text-extraction, normalization, structure-outline}, id `sfs-adapter-text`, version `sfs-text-adapter/0.1`. Conservative per the V1 scope lock (code/PDF/spreadsheet adapters stay deferred). MIME match wins even for unknown extensions (a declared text type is authoritative). |
| D-030 | M05 | Engine port change: `ContentSource` is replaced by `AnalysisInputProvider`/`AnalysisInput` (objectId, fileName, contentType, bytes) so routing can see names and types. The engine `inspect` package (FileInspector/InspectionReport) is superseded by the adapter framework's `TextLoader`; structural parsing moved from the engine's StructureAnalyzer into the adapters' `StructuralParser` (engine delegates/falls back; adapter-provided structure seeds the pipeline). |
| D-031 | M05 | Text normalization (deterministic, idempotent): strip BOM, unify CRLF/CR to LF, trim trailing whitespace per line, collapse blank-line runs to one blank line, NFC compose. The analysis cache keeps keying on the RAW-content SHA-256; the DNA validator hashes the NORMALIZED text (both deterministic, no drift). |
| D-032 | M06 | Authoritative Semantic DNA lives in `sfs-core` (`com.sfs.core.dna`), as the original M01 core-pom description anticipated; sfs-core stays dependency-free (hand-written canonical JSON, no Jackson). The contracts `SemanticDnaView` remains the API/UI projection; `DnaViewMapper` (engine) maps authoritative → view. |
| D-033 | M06 | Schema bumped to `sfs-dna/0.2` (identity, summary, concepts, topics, entities, facts, relationships, structure, embedding+ref, behaviour profile, seed reconstruction rules, fidelity, security). `DnaMigrator` upgrades legacy `sfs-dna/0.1` payloads (view-shaped JSON) with deterministic defaults; current-version payloads are refused by the migrator. Version + content changes are detectable via the canonical SHA-256 (`DnaCanonical.integrityHash`); `StoredDna` chains previous hashes per object. |
| D-034 | M06 | Reconstruction rules: deterministic seed refs (preserve-heading-order / retain-critical-facts / retain-explicit-entities / reconstruct-by-outline, version `sfs-rules/0.1`) carried on every built and migrated DNA; M07 replaces them with the formal rule system. BehaviourProfile derived deterministically (document type from outline shape, guidance list); SecurityProfile carries value-free protected references + handling policy. Metadata separation enforced structurally (`MetadataRef` is a record-level pointer; SemanticDna carries no file metadata — reflection-tested). |
| D-035 | M06 | Storage: core `InMemoryDnaRepository` stores authoritative DNA with integrity hashes and version chains; the engine's `InMemorySemanticRecordStore` becomes the adapter over it implementing the existing `SemanticRecordService` (views). Downstream (reconstruction/evaluation mocks, UI, API) unchanged. |
| D-036 | M06 | Product fix found by the M06 acceptance test: protected-value lines (credential assignments, emails) were previously excluded from protected REFERENCES but still fed the summary, topics and embedding (a password and an API-key fragment leaked into canonical DNA). Fix: `ProtectedValueDetector` (engine) — sensitive lines are withheld from semantic extraction (sentences, tokens, frequencies); they are represented only by value-free protected references. |
| D-037 | M07 | The Reconstruction Rules System lives in `sfs-core` (`com.sfs.core.rules`), beside the DNA it constrains (owner decision; the original M01 core-pom description anticipated M06/M07 domain types in core). Pure records + sealed `Constraint` hierarchy (`RequiredFactConstraint`, `RequiredEntityConstraint`, `RelationshipConstraint`, `StructureConstraint`, `ContentConstraint`, `OrderingConstraint`, `ValidationConstraint`), `Rule` (typed, prioritized), `RuleSet` bound to objectId + dnaVersion + canonical DNA hash. |
| D-038 | M07 | Storage = combination (owner delegated the choice): RuleSets are always derivable from DNA alone (DNA is the source of truth), and `RuleRepository` memoizes derived sets bound to dnaVersion + canonical hash. Same version + same hash → reuse; same version + different hash → explicit `RuleConflict` warning + re-derivation. `save()` is the loader path for externally supplied sets. Durable rule storage stays M08. |
| D-039 | M07 | Strict enforcement (owner decision): ERROR-severity conflicts (contradictory required-fact severity, contradictory entity minimums, disagreeing ordering rules, ordering/structure size mismatch) refuse plan generation via `RulePlanningException`; the dev reconstruction checks its own render with `PlanChecker` and a violation means job REJECTED with explicit findings; WARNING conflicts/plan warnings are recorded but non-fatal. |
| D-040 | M07 | `ReconstructionPlanner` turns (DNA | loaded RuleSet) into a `ReconstructionPlan` (sections, required facts/entities/relationships, content + validation contracts, warnings) — the M11 handoff artifact. `RuleDeriver` derives declarative rules deterministically from DNA alone using `DocumentRuleTemplates` per behaviour document type (structured-document / headed-note / narrative / migrated-document). Seed rules in DNA (M06) remain references; full RuleSets live in the repository. |
| D-041 | M07 | The dev reconstruction mock now renders from the real plan (provenance `sfs-rules/0.2`) and verifies its artifact with `PlanChecker`; its findings include a real "Plan rules" check. Plan versioning/derivation counters are exposed in `GET /api/v1/version` (`rules` block). No new API operations; frozen surface unchanged. |
| D-042 | M08 | Relational implementation: H2 database in file mode (owner-approved first third-party dependency, `com.h2database:h2:2.3.232`). Pure Java, no native binaries; real SQL + transactions; DB path `sfs.memory.path` (default `data/sfs-memory`, H2 `./` prefix added for relative paths). |
| D-043 | M08 | Full durability scope (owner decision): the Memory DB persists semantic objects + metadata + file versions + raw content (BLOB, released by the purge gate), DNA records with canonical JSON + hash chains, graph rows (concepts, topics, entities, facts, relationships, structure nodes, reconstruction rules, security references), embeddings, rule sets, lifecycle object states AND lifecycle events. Jobs stay in-memory (M02 orphan semantics). |
| D-044 | M08 | New Maven module `sfs-memory` (the package `com.sfs.memory` the M01 boundary test reserved). Depends on sfs-core, sfs-contracts, sfs-lifecycle, H2. Core gained the `DnaRepository` interface (InMemoryDnaRepository implements it); the engine's record store now accepts any DnaRepository (no-arg ctor keeps the in-memory default for unit tests). |
| D-045 | M08 | Lifecycle write-through: `LifecyclePersistence` port (persistFile/persistEvent/loadFiles/loadEvents); every lifecycle mutation goes through one choke point (`storeFile`) and every audit append is persisted; `restore()` loads state + events (audit sequence continues past restored ids); `objectCount()` gates the dev seeder (seed only when the DB is empty). Startup order: MemoryRestoreRunner (0) → job recovery (1) → lifecycle recovery (2) → seeder (3). |
| D-046 | M08 | Vector index: in-memory `VectorIndex` (cosine similarity, top-k, deterministic) upserted on every DNA save and rebuildable from canonical records via `rebuildVectorIndex()` — the index is a cache of the relational truth, per the spec. Search itself is M09. |
| D-047 | M08 | Transaction boundary: every DNA save is one H2 transaction (DNA row + graph delete/insert + embedding upsert, committed atomically; rollback on failure), so partial writes cannot produce an apparently valid record. Rule-set derivation persistence rides a `RuleRepository` derivation sink; rule sets reload at wiring time. |
| D-048 | M08 | Storage accounting: `storageStats()` (row counts + canonical JSON bytes across the schema tables) plus vector entries, exposed in the `GET /api/v1/version` `storage` block. Measurements only, never targets. |
| D-049 | M09 | New Maven module `sfs-search` (package `com.sfs.search`, owner decision). Depends on sfs-core, sfs-contracts, sfs-memory; sfs-engine is a test-scoped dependency so benchmarks run the real analysis pipeline. Components: `QueryParser` (stop-word reduction, exact `sfs-obj-…` detection via the real `ObjectId` validation, empty query → validation failure), `QueryEmbedder` (deterministic 64-dim L2-normalized embedding), `RankingProfile` (configurable feature weights), `ScoredCandidate`, `SearchEngine` (implements the frozen `SearchService`). |
| D-050 | M09 | One vector space, zero new dependencies: query embedding reuses the exact analyzer chain documents went through (`TextParsingAnalyzer` + `EmbeddingAnalyzer`), so query and document embeddings are directly comparable by cosine similarity; an equivalence test pins query-vs-engine-IR embedding equality. No LLM, text-only, per the V1 scope lock. |
| D-051 | M09 | Search pipeline (never reconstructs by default): exact Object ID lookup bypasses vector search entirely (single exact result, relevance 1.0, `OBJECT_ID_LOOKUP` mode); otherwise parse → embed → cosine top-k over `VectorIndex` → searchable-state filters (status/fileName from the memory DB) → relationship and concept enrichment → `RankingProfile` rerank (vector, concept, entity, fact, relationship, summary features) → results with `SearchEvidence` (VECTOR_SIMILARITY, CONCEPT, ENTITY, FACT, RELATIONSHIP, SUMMARY). Unknown Object IDs return an empty result set, not an error. Protected values never surface in results or evidence (test-enforced). |
| D-052 | M09 | Mock swap with frozen API unchanged (owner decision): `MockSearchService` and its test deleted; `EngineWiringConfiguration` now exposes the real `SearchEngine` as the `SearchService` bean. POST/GET `/api/v1/search` and the UI search page keep their shapes; evidence fields are real. The UI "About these results" copy now describes the engine. `ObsoleteDeliveryArtifactsTest` lists the two deleted mock files for zip-based deliveries. |
| D-053 | M09 | Search benchmark (M09 acceptance): `SearchEngineTest` boots a real `SemanticEngine` over three themed fixture documents in `sfs-engine/src/test/resources/benchmarks/` (quarterly report, meeting minutes, research notes; files loaded from the engine module's test resources), registers them as analyzed objects in a file-mode H2 memory DB, and asserts per-query top-1 wins, exact-ID precision, empty result for unknown IDs, evidence presence, deterministic embeddings and secret non-exposure. |
| D-054 | M09 | Observability: `GET /api/v1/version` gains a `search` block (engine id `sfs-search/0.1`, embedding dimensions, indexed vector count, retrieval modes); milestone string and enforced-subsystems list updated (memory-system, semantic-search now real). Validation failures of empty queries surface as HTTP 400 through the existing `validationFailed` mapping. |
| D-055 | M10 | New Maven module `sfs-reconstruction` (package `com.sfs.reconstruction`, owner decision) for the SFS Reconstruction Model. Depends only on sfs-core (DNA, Rules, Plan, PlanChecker) and sfs-contracts; sfs-engine is test-scoped for the benchmark corpus. No dependency on sfs-memory or sfs-search exists anywhere in the module, so the model can be replaced without touching Memory or Search. The reconstruction FLOW stays on the development stand-in until the reconstruction engine milestone (owner decision; M11 owns the swap and the MockReconstructionService deletion). |
| D-056 | M10 | Frozen model contract: `SFSReconstructionModel` (`modelId()`, `reconstruct(ModelInput)`); `ModelInput` carries the SemanticDna plus the derived ReconstructionPlan (acceptance: the model consumes DNA and Rules); `ModelOutput` (modelId, draftText, notes). `Encoder<I,O>` is the encoding contract; `Decoder` is the swappable tiny-model slot; `ConstraintInterface` (impl `PlanConstraintInterface`) judges every draft: plan compliance via PlanChecker, protected-value refusal, critical-fact coverage, and token-level traceability of the draft against the representation vocabulary — fabricated content is an explicit violation, never a silent pass. `ModelRegistry` holds models; the first registered model is the default. |
| D-057 | M10 | Deterministic baseline first (per the source constraints): `sfs-reconstruction/deterministic-baseline/0.1` renders through `sfs-reconstruction/templated-decoder/0.1` from the encoded material only — summary verbatim, headings in recorded order, facts with visible `[critical]` markers, entities at their required mention floor, relationships as subject-type-object lines, concepts and topics. The draft contains no glue prose, which is what makes token traceability enforceable; provenance header/footer stay a pipeline (engine, M11) concern, not model output. |
| D-058 | M10 | Benchmark (10.8): `ReconstructionBenchmark` over a frozen corpus — three engine-analyzed fixture documents plus one engine-protected document — measures per (model, case): constraint compliance, critical/entity/relationship coverage, summary verbatim, reconstruct/verify latency, canonical DNA bytes vs draft bytes, and Knowledge Preservation Density (dnaBytes/draftBytes). A deliberately weaker `naive-echo/0.1` model (test scope only) proves the harness discriminates quality. Component measurements only; no scalar fidelity percentage is claimed (owner decision). Reproducibility is test-enforced: identical drafts and fidelity numbers across runs. |
| D-059 | M10 | Swappability and the future-learning hook (10.9): `ModelSwapTest` replaces the model through the registry and observes different drafts with the same pipeline, without any Memory/Search involvement. `TrainingHook` is invoked by the benchmark for every reconstruction (collection point for future training data); `NoopTrainingHook` is the V0.1 implementation because training is not assumed. |
| D-060 | M10 | Observability: `GET /api/v1/version` gains a `reconstruction` block (model id, model status, flow status, evaluation status); milestone string becomes M10 and `reconstruction-model` joins the enforced-subsystems list. Benchmark reference numbers (reproducible via `ReconstructionBenchmarkTest`): baseline satisfied on all clean corpus documents with zero violations; KPD 3.33 / 4.10 / 4.20 on the three fixtures; reconstruct ~60-150 microseconds, verify ~180-350 microseconds per document. |
| D-061 | M11 | New Maven module `sfs-reconstruction-engine` (package `com.sfs.reconstruction.engine`, owner decision) for the Reconstruction Engine. Depends on sfs-core, sfs-contracts and sfs-reconstruction only: durable DNA is loaded through the core `DnaRepository` port (the H2-backed bean is injected at wiring time), rules through `RuleRepository`/`ReconstructionPlanner`, generation through the frozen M10 model contract. The engine implements the frozen `ReconstructionService`; the frozen API shapes and the 18 error codes are unchanged. `MockReconstructionService` and its test are deleted (owner decision recorded at M10: M11 owns the swap); `ObsoleteDeliveryArtifactsTest` lists both files. |
| D-062 | M11 | Job state machine (11.1, owner decision: asynchronous): `requestReconstruction` validates preconditions synchronously — unknown object and missing DNA are terminal `FAILED` jobs with explicit reasons, and protected objects are refused at the gate as terminal `REJECTED` before any reconstruction runs (finding "Protected values"), which preserves the frozen 422/`refused` API contract and the M07 "refused before it runs" behaviour. Everything else is a `QUEUED` job; a single daemon-thread executor advances `QUEUED → RUNNING → COMPLETED/REJECTED/FAILED`. Jobs and artifacts are immutable and auditable in memory (owner decision: same durability model as analysis jobs — restarts lose nothing that cannot be deterministically regenerated); `shutdown()` marks unfinished jobs failed with an explicit stop reason. |
| D-063 | M11 | Engine records: `ReconstructedSource` (file summary + stored DNA from the `DnaRuleLoader`), `VerificationResult` (satisfied, violations, warnings, checked counts), `ReconstructionMetadata` (objectId, dnaVersion, dnaSha256, rulesVersion, modelId, plan/reconstruct/verify nanos, dnaBytes, artifactBytes, Knowledge Preservation Density), `ReconstructionJob` (internal superset with `toView()` onto the frozen `ReconstructionJobView`). `ReconstructionPostProcessor` (11.6) prepends the frozen provenance header and appends the reconstructed/estimated footer; `ReconstructionArtifactFactory` (11.7) names artifacts `base.reconstructed.<jobId>.txt` as UTF-8 plain text. Original bytes are never read: memorized objects reconstruct from memory alone. |
| D-064 | M11 | Finding vocabulary continuity: the engine's satisfied/violated finding is named `Plan rules (sfs-rules/0.2)` and enumerates the plan's required facts, entities, relationships and sections exactly as the development stand-in did, so the M07 acceptance assertions and the UI keep their meaning; diagnostics counters (jobs total/completed/rejected/failed/in-flight, artifacts available) join the `/version` `reconstruction` block, the milestone becomes M11 and `reconstruction-engine` joins the enforced subsystems. |
| D-065 | M11 | Acceptance (11.8): `MilestoneElevenAcceptanceTest` drives the engine over HTTP — single-click UI POST to job page to artifact download, provenance-bearing artifact (header, sfs-rules/0.2, model id, estimated labeling), observable terminal status with findings, byte-identical artifacts for fixed versioned inputs, protected refusal with no secret anywhere, unknown object explicit failure, memorized reconstruction, and `/version` engine diagnostics. M01/M02/M07 acceptance journeys gained poll-until-terminal helpers (assertions unchanged) because the engine is now asynchronous. |
| D-066 | M12 | New Maven module `sfs-evaluation` (package `com.sfs.evaluation` — the package the M01 `ArchitectureBoundaryTest` reserved for this milestone, owner decision). Depends on sfs-core, sfs-contracts and sfs-lifecycle (the `RawContentStore` port only); sfs-engine and sfs-reconstruction-engine are test-scoped so the benchmark analyzes the real corpus and reconstructs with the real engine. |
| D-067 | M12 | The PDF's measurement structures are internal to the module; the frozen `FidelityReportView`/`EvaluationAvailability`/`FidelityDimension` contract is unchanged and stays the only external surface (it already demands all six dimension scores, explicit critical-fact counts and storage bytes). Internal: `EvaluationInput` (original text, artifact text, DNA), score records (`SemanticScore`, `StructuralScore`, `FactualScore` + `CriticalFactScore`, `EntityScore`, `RelationshipScore`, `CompletenessScore`), `ErrorCategory` (11 categories with description, advice, correctness flag), `CalibrationRecord`, `FidelityReport` (internal superset; `toView` maps onto the frozen view). |
| D-068 | M12 | The six evaluators (12.1-12.6) measure the ORIGINAL text against the artifact, grounded in DNA material: semantic = deterministic content-word overlap F1 (owner decision; token-level, stop-worded, length >= 4), structural = headings present + order + summary verbatim, factual = statement containment with separate critical-fact score, entity = presence without substitution, relationship = subject+object present with direction counted, completeness = material-unit coverage. Overall fidelity (12.7) is a weighted blend (semantic .15, structural .15, factual .25, entity .15, relationship .10, completeness .20) with a hard critical gate: if any critical fact is lost, overall is capped at the critical survival rate — a high semantic score can never hide a factual failure. Overall lives only inside the module; the frozen view carries no aggregate by contract test. |
| D-069 | M12 | Calibration (12.8): stated fact confidence is binned ([0,0.6), [0.6,0.75), [0.75,0.9), [0.9,1.0]) against observed survival, producing `CalibrationRecord`s (count, mean stated, observed rate, delta) — confidence is never treated as accuracy without this comparison. Regression benchmark (12.9): fixed evaluator version `sfs-evaluation/0.1`, hand-authored gold annotations over the three frozen corpus documents (critical facts, headings, entities), and a committed `baseline.properties` — a drop of any dimension or overall below the committed value fails the build, so fidelity can never silently regress. Improvement loop (12.10): `ImprovementAdvisor` aggregates error categories into prioritized, correctness-first suggestions with concrete advice; V0.1 advises, it never silently rewrites DNA. |
| D-070 | M12 | Mock swap (owner decision): `MockEvaluationService`, `MockEvaluationServiceTest` and the now-unused `EngineTestSupport` deleted (manual-deletion list; `ObsoleteDeliveryArtifactsTest` extended); `FidelityEvaluationService` implements the frozen `EvaluationService` — availability semantics unchanged (no artifact / original unavailable / not evaluated), real measurement otherwise: original bytes from the `RawContentStore`, artifact from the `ReconstructionService`, DNA from the `DnaRepository` port. `/version` gains an `evaluation` block (evaluator id, dimensions, critical checks, calibration), the milestone becomes M12 and `evaluation-fidelity` joins the enforced subsystems; the evaluation report page copy now states the figures are measured, not asserted; the M02 honesty assertion was updated from "mocked" to the real-subsystem disclosure because nothing measured is mocked any more. Benchmark reference numbers (reproducible via `RegressionBenchmarkTest`): all six dimensions 1.0 except semantic (0.63/0.66/0.71 across the corpus — the honest cost of outline-style baseline rendering vs full prose); overall 0.944/0.949/0.956; critical facts preserved on every corpus document. |
| D-071 | M13 | New Maven module `sfs-security` (package `com.sfs.security` — the package the M01 `ArchitectureBoundaryTest` reserved, owner decision). Depends only on sfs-core and sfs-contracts; sfs-engine depends on it so analysis uses the full detector, and the UI wires the real beans. Encrypted storage is file-based (owner decision): ciphertext under `data/sfs-secure/`, the AES-256-GCM master key under `data/sfs-keys/` — key and ciphertext live in different directories and never in the same file (structural key separation), the key auto-generates with owner-only permissions on first use and a missing key fails closed. |
| D-072 | M13 | Contract extensions (all additive): `Capability.RESOLVE_SECRET` (new constant; no existing constant changed), plus `SecretSubmission`, `SecretRecord` (refuses non-reversible types), `EncryptionMetadata` and the `SecretVault` port (submissions in, records out). The PDF's `SensitiveValue`/`PolicyDecision` are internal to sfs-security; the frozen `SensitiveTypePolicy`/`SecuritySettingsView`/`ProtectedReferenceView` contracts are unchanged and remain the external surface. |
| D-073 | M13 | Detection and policy (13.1-13.2): the engine's line-level `ProtectedValueDetector` is replaced by sfs-security's value-level `SensitiveDataDetector` (credential assignments typed by key, key-shaped values, bearer tokens, emails, phone numbers with date/version guards, labeled account identifiers and postal addresses); `TextParsingAnalyzer` still excludes every sensitive line BEFORE tokens, sentences, facts, summary and embedding are built, so plaintext secrets cannot reach the vector index (13.7). `SecurityPolicyEngine` holds the eight-type `SensitiveTypePolicy` table (PASSWORD and OTHER locked to REDACT per the frozen records), decides `PolicyDecision`s and derives stable value-hash reference ids — one token per exact value, consistent across occurrences. |
| D-074 | M13 | Engine integration: `ProtectedValueAnalyzer` is value-level now — one `ProtectedReferenceView` per unique value (`resolvable` only when the policy is reversible) and, for ENCRYPT decisions, a `SecretSubmission` into the optional `SecretVault` that the `SemanticEngine` now accepts (new 8-arg constructor; the 7-arg constructor delegates with no vault and discards reversible values fail-closed). REDACT/TOKENIZE values are never retained in any form. |
| D-075 | M13 | Mock swap (owner decision): `MockAuthenticationService`, `MockAuthorizationService`, `MockSecuritySettingsService` and `MockSecuritySettingsServiceTest` deleted (manual-deletion list enforced). Real beans: `IdentityAuthenticationService` (SHA-256 credential check, constant-time compare, fail-closed, audited; the reader/operator/custodian dev bootstrap identities keep their credentials and capability sets, custodian additionally holds RESOLVE_SECRET), `PolicyAuthorizationService` (capability check, audited secret-resolution denials), `PolicySecuritySettingsService` (live policy table, mandatory protections, real key-storage statement, real audit tail), `FileEncryptedSecureStore` + `FileKeyManager`, and `SecretResolutionService`: resolution requires RESOLVE_SECRET + a stored record + a reversible type, every attempt is audited, and passwords are never stored so they are never resolvable for anyone. |
| D-076 | M13 | Observability: `/version` gains a `security` block (detector id, policy count, stored-secret count, key-storage statement, password-policy statement, audit counter); milestone becomes M13 and `security-privacy` joins the enforced subsystems; the version note now discloses development bootstrap identities with hashed credentials instead of claiming security is mocked. The DNA view's per-reference `resolvable` flag is now policy-derived (the M04-era mapper hardcoded true; PASSWORD and TOKENIZED types correctly report false). Acceptance (`MilestoneThirteenAcceptanceTest`): a synthetic secret document analyzed over HTTP leaves every exact value out of the DNA and search responses; the custodian resolves the API key reference to the exact value over the wired beans while operator and reader are denied; the password reference resolves for no one; settings report live policies and audit events. |
| D-077 | M14 | Observability kernel in sfs-core `com.sfs.core.observe` (owner decision: shared kernel, no new module): `TraceId` (16 lowercase hex, secure-random generation, strict validation), `StructuredEvent` (log schema `sfs-log/0.1`: timestamp, level, event, traceId, subject, outcome, durationNanos, detail — identifiers and outcomes only, never input values; quotes neutralized so lines stay one-per-event), `ObservabilityRegistry` (metrics schema `sfs-metrics/0.1`: 200-event ring with drop accounting, per-stage count/total/max/mean aggregates, named counters). |
| D-078 | M14 | Tracing and structured logging: a `TraceIdFilter` on every request issues or honors `X-SFS-Trace-Id` (response header echo), propagates it into SLF4J MDC so the externalized console pattern prints `traceId=...` on every log line, and records `request.served` events with duration into the registry. Diagnostics carry SHAPES, not values: `redactPath` keeps literal segments (routes, real object/job/reference ids) and replaces everything else with `{id}` — the filter, the error controller and the API exception handler all use it, so a secret embedded in a path reaches no response body, log line or event (probe-tested). Error responses now redact the `path` field the same way. |
| D-079 | M14 | Configuration model (14.2): `EffectiveConfiguration` (`sfs-config/0.1`) externalizes the effective settings (server address/port, memory path, security directories, profile) from `application.properties` (environment-overridable), self-declares its schema version in the payload, and fails fast at startup when binding to a non-loopback address without an explicit override — the standing V1 deployment restriction is now enforced by the configuration itself. Surfaced in `/version` `configuration`. |
| D-080 | M14 | Metrics and storage accounting (14.4): `/version` gains `metrics` (registry snapshot: counters `requests.served`/`requests.refused`, stage aggregate `http.request`, ring accounting), `observability` (log/metrics schema ids, trace scheme, structured-console flag) and `configuration`; the `security` block gains `secureStoreBytes` and `masterKeyBytes` so storage accounting now spans DB, vector and security overhead. Milestone becomes M14; `infrastructure-observability` joins the enforced subsystems. |
| D-081 | M14 | Test pyramid and benchmarks (14.5-14.10): unit tests for every observe component; the E2E journey (`EndToEndJourneyTest`) walks import, analysis, DNA read, search, reconstruction, measured evaluation, memorize, delete, gated purge, memory-only search, memory-only reconstruction and unmeasurable evaluation in one HTTP test; `ObservabilitySecurityTest` probes the leak channels (malformed bodies, secret-bearing paths, event ring, trace propagation); `StagePerformanceBenchmarkTest` measures file I/O, analysis, search, reconstruction and evaluation separately with generous ceilings, reproduces artifacts byte-identically and accounts storage as original vs artifact vs canonical DNA vs vector bytes vs the real H2 database file vs security files. The M12 committed baseline continues to enforce fidelity floors. CI is the `scripts/verify.sh` gate (Java 21 check, clean build with the full suite, release-packaging check); hosted CI is deferred until the repository is hosted — recorded honestly, no fake pipeline. |
| D-082 | M14 | Release (14.11): the Spring Boot repackage goal produces the executable fat jar; the release artifact is `sfs-V1.0-release.zip` (fat jar + START.txt run instructions) verified by running `java -jar` with no development tools and exercising `/version` and reconstruction over HTTP. No correctness depends on manual UI steps: every journey in this record is automated. |

## 2. Changelog

### Milestone 01 — released
UI screens against contracts with mock services; `ObjectId` value object format; Thymeleaf
templates; 322 tests at 01.8. Released by the project owner; `README.md` is the released
version and must be preserved byte-for-byte.

### Milestone 02 — completed (585 tests)
`sfs-app` application layer, REST API under `/api/v1` (20 operations), typed error model,
authentication/authorization/confirmation boundaries for destructive operations, reversible
delete (`SOFT_DELETED`) separate from purge (`MEMORIZED`), job registry with restart
marking, loopback binding as deployment restriction. Mock lifecycle (`MockFileService`)
removed in M03.

### Milestone 03 — File Lifecycle Manager (698 tests)
- **03.0** `sfs-lifecycle` module skeleton.
- **03.1** Registration: `SemanticFile`, `FileMetadata` (SHA-256, size, timestamps,
  optional storage address), `FileVersion`, `ContentDigest`, `RawContentStore` +
  in-memory store; manager registration with metadata capture and version 1.
- **03.2** `ObjectIdService`: unique, path/name-independent identity allocation
  (`sfs-obj-NNNN-8hex`); dev seed suffixes scripted so fixture IDs stay stable.
- **03.3** Authoritative state machine (`FileState`, `LifecycleStateMachine`,
  `LifecycleEvent`, `LifecycleAuditLog`), manager implements `FileService`
  (principal-aware destructive operations), `ANALYSIS_*` events, `StubAnalysisEngine`,
  `DevDataSeeder`, `FileStatus` becomes the projection (gains `MEMORY_COMMITTED`).
- **03.4** Version tracking: rename keeps Object ID + versions; content replacement
  records versions and refreshes the raw store; refusals audited.
- **03.5** Memorize: `ANALYZED → MEMORIZABLE → MEMORY_COMMITTED` transaction with
  certified-DNA precondition; `MEMORIZE` capability; API + UI (files list + object detail).
- **03.6** Semantic deletion protocol: delete/undo remember pre-delete state;
  `LifecycleAuditEntry` / `LifecycleStatistics` / `LifecycleAuditService` contracts;
  `LifecycleAuditAdapter`; audit + statistics API endpoints; address-independence
  (storage address cleared at purge, record remains Object-ID-addressable).
- **03.7** `DeletionPolicy` + `RawDeletionGate` (release only from `SOFT_DELETED` with
  committed memory — tightens M02, purge before memorize now 409), interrupted-memorize
  recovery at startup (`MEMORIZABLE → ANALYZED`, audited), purge durations captured in
  statistics, API contract freeze test rewritten as reflection over controllers
  (23 operations, docs-free), M03 acceptance test suite.
- **Housekeeping (post-03.7):** all decision-record/owner attribution removed from
  program files; project made docs-free (`docs/` not part of the repository;
  `README.md` is the only documentation); `MetaApiController` version facts updated
  to M03 reality.

### Milestone 04 — Semantic Engine (739 tests)
- **04.0** `sfs-engine` module skeleton.
- **04.1** Orchestrator: `SemanticEngine` (sync `analyzeNow` + async `submit` on a single
  worker thread), `SemanticContext`, `AnalysisJob` + `AnalysisJobRegistry` (job ids from
  `job-5001` to avoid reconstruction-id collisions), ports (`ContentSource`,
  `AnalysisCompletionListener`), `EngineWiringConfiguration` composition in the UI,
  lifecycle `AnalysisDispatcher` now returns the job id, duplicate `FileService` bean
  removed (the manager is the `FileService`).
- **04.2** File inspection: `FileInspector`/`InspectionReport` — UTF-8 strict decode,
  binary/null-byte and printability refusal, word/line/paragraph statistics.
- **04.3** Semantic pipeline: `SemanticIntermediateRepresentation`, `Analyzer` SPI,
  deterministic stages (text-parsing, summary, structure with implicit Body fallback,
  concepts, topics, entities with sentence-start heuristics, facts with criticality and
  bounded confidence, relationships, 64-dim feature-hashing embeddings, protected values,
  dna-builder), `PipelineResult`, `DnaValidator`; `InMemorySemanticRecordStore` replaces
  the mock record service.
- **04.4** Change detection: `AnalysisCache` keyed by Object ID with content hash +
  engine version; unchanged content reuses the prior DNA (`REUSED` job, no recomputation);
  changed content bumps the DNA version.
- **04.5** Background processing: single-thread worker queue, analysis jobs in the
  unified jobs API, failures explicit (job `FAILED` + lifecycle `FAILED` + audit),
  startup re-queue of `ANALYZING` objects, completion race eliminated (transition before
  dispatch).
- **04.6** Analysis levels (`STANDARD`-only V1 policy), stage durations captured per job
  and analysis durations in the audit, benchmark TXT fixtures (`sfs-engine` test
  resources) proven to produce complete deterministic DNA, `MilestoneFourAcceptanceTest`
  (async API, job observability, benchmark completeness, measured durations), version
  endpoint updated to M04 facts.
- **Housekeeping:** dev seeder rewritten to drive the real `requestAnalysis` → worker
  path; deleted `StubAnalysisEngine`, `MockSemanticRecordService`,
  `MockSemanticRecordServiceTest`; async-aware polling added to the M02/M03 API tests.

### M04 patch (post-delivery, test-only)
- `BenchmarkDocumentsAcceptanceTest` now includes the job failure reason and stage map in
  its assertion message (the original assertion hid the reason). No production code changed.
- User-side incident diagnosed: fresh extracts of the shipped zips pass all 46 engine tests
  on two JDK 21 builds; the user's failing run showed a `StageAnalyzerTest` DOCUMENT string
  missing "in August" and the final sentence, i.e. the file on disk differed from the zip
  (partial extraction or a stale editor buffer auto-saving over the extracted file).
  Fix: close VS Code, re-extract the zip, `mvn clean install`.

### M04 patch 2 (post-delivery, engine hardening)
- User-side incident root cause confirmed by reproduction: a headings-only text document
  (all content lines are headings) produced zero sentences, so the summary stage emitted a
  blank summary and the DNA builder rejected it with `summary must not be blank`. The
  user's `team-meeting-minutes.txt` contained only the heading lines (truncated file);
  shipped copies are 420 bytes with full sentences.
- Hardening: `SummaryAnalyzer` now falls back to a deterministic heading summary
  (first headings joined with "; ") when a document yields no sentences, so a
  headings-only document completes analysis instead of failing a pipeline stage with a
  constructor exception. Normal documents are unaffected (sentence path unchanged).
- New test: headings-only document completes with summary "Agenda; Decisions; Action Items".

### M04 patch 3 (post-delivery, delivery hygiene)
- User-side incident 3: the obsolete M03 `StubAnalysisEngine.java` still on the user's disk
  failed to compile against the M04 `AnalysisDispatcher` signature (`void` vs `String`).
  Root cause class is unchanged: zip archives cannot delete files.
- Permanent guard: new `ObsoleteDeliveryArtifactsTest` in `sfs-ui` fails with the exact
  paths of any removed-but-still-present artifacts (M03: `MockFileService`,
  `MockFileServiceTest`, doc-parsing `ApiContractFreezeTest`; M04: `StubAnalysisEngine`,
  `MockSemanticRecordService`, `MockSemanticRecordServiceTest`, mock `DevDataSeeder`).
  Self-tested: plants a stale file, fails naming it, passes once deleted.
- `ReconstructionApiController` now builds the 422 response via
  `ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)` instead of the
  `unprocessableEntity()` helper deprecated in Spring Framework 7. Behavior unchanged.
- Remaining known warnings (cosmetic, not addressed): missing `serialVersionUID` on five
  small serializable exception classes.

### Milestone 05 — File-Type Adapter Framework / Text Adapter (791 tests)
- **05.0** `sfs-adapters` module skeleton.
- **05.1** SPI freeze: `FileTypeAdapter`, `AdapterDescriptor`, `AdapterRequest`,
  `AdapterResult`, `AdapterRefusedException`, `TextMetrics` (descriptor claims normalize
  to lowercase; an adapter must claim at least one extension or content type).
- **05.2** `AdapterRegistry`: registration-order preservation, duplicate-id refusal,
  descriptor diagnostics.
- **05.3** `AdapterResolver` + `UnsupportedFileTypeException`: automatic extension/MIME
  matching, explicit rejection message ("not treated as text"), resolution/refusal counters.
- **05.4** `TextAdapter` + `TextLoader`: strict UTF-8 decode, binary/null-byte/empty/
  wordless/printable-ratio refusals (M04 inspection semantics preserved), normalized text +
  `TextMetrics` + structure outline in `AdapterResult`.
- **05.5** `TextNormalizer`: BOM strip, CRLF/CR → LF, trailing-whitespace trim, blank-run
  collapse, NFC; proven idempotent. (Found and fixed a trailing-newline-loss bug from
  `String.lines()` during testing.)
- **05.6** `StructuralParser`: markdown/numbered/all-caps headings with Body fallback
  (moved from the M04 engine analyzer).
- **05.7** Integration: engine routes through `AdapterResolver`; `ContentSource` →
  `AnalysisInputProvider`/`AnalysisInput`; structure seeding overload on `SemanticPipeline`;
  `StructureAnalyzer` delegates; lifecycle `ImportAcceptancePolicy` enforced at import and
  rename; UI wiring (registry/resolver/policy/provider beans); `/version` adapter
  diagnostics; `DummyFutureAdapterIntegrationTest` proves a future adapter plugs in with
  zero engine changes; `MilestoneFiveAcceptanceTest` covers TXT auto-selection, markdown
  routing, unsupported-import refusal, diagnostics and rename refusal;
  `ObsoleteDeliveryArtifactsTest` now also guards the removed M04 engine files.
- Live verification: .md import → analysis → adapter-parsed structure; .png import →
  400 with explicit unsupported message and refusal counter incremented; version endpoint
  lists the Text Adapter descriptor.

### M05 patch (post-delivery, carryover)
- User-side incident: `SemanticEngineTest.headingsOnlyDocumentCompletes` failed on the
  user's machine while passing in the workspace. Root cause: the M04 heading-summary
  fallback (`SummaryAnalyzer`) was never included in a task zip — it shipped only in
  `sfs-m04-summaryfix-CHANGED-ONLY.zip` (apparently not applied), while the M05 05.7 zip
  delivered the regression test that requires it. Fixed by carrying `SummaryAnalyzer.java`
  in `sfs-m05-summaryfix-carryover-CHANGED-ONLY.zip`.
- Same run showed the M04 engine `inspect` files still on the user's disk
  (`FileInspectorTest` executed) — the M05 manual deletions had not been performed; the
  `ObsoleteDeliveryArtifactsTest` guard covers these once the build reaches sfs-ui.
- Rule reinforced for future milestones: when a task zip delivers a test that depends on a
  behavior fix, the same zip must also deliver the production file carrying that fix.

### Milestone 06 — Semantic Representation System / Semantic DNA (828 tests)
- **06.0–06.3** Authoritative model in `com.sfs.core.dna`: `SemanticDna`, `DnaIdentity`,
  `MetadataRef`, `Concept`, `Topic`, `Entity`, `Fact`, `Relationship`, `StructureNode`,
  `EmbeddingRef`, `BehaviourProfile`, `FidelityProfile`, `SecurityProfile`,
  `ProtectedReference`, `ReconstructionRuleRef`; required-field enforcement in canonical
  constructors; metadata separation reflection-tested.
- **06.4** `SemanticDnaBuilder` (deterministic behaviour/security/rules derivation,
  seed rules) + `DnaSchemaValidator` (`sfs-dna/0.2`).
- **06.5** `Json` (hand-written canonical writer/parser, full string escaping) +
  `DnaCanonical` (serialize/deserialize/integrity hash) + `DnaMigrator` (0.1 → 0.2) +
  `StoredDna`/`InMemoryDnaRepository` (hash-chained versions). Lossless round-trip proven
  incl. adversarial strings (quotes, backslashes, control chars, unicode).
- **06.6** Engine integration: `DnaBuilderAnalyzer` builds authoritative DNA
  (schema-validated at build time); store upgraded to repository-backed authoritative
  storage with `DnaViewMapper`; versioning/integrity tests; `SemanticDnaAcceptanceTest`
  covers all six PDF acceptance criteria; the acceptance test caught and drove the fix
  for the protected-value leak into summary/topics/embedding (D-036); version endpoint
  reports M06 + `sfs-dna/0.2` + semantic-representation enforced.
- Live verification: seed DNA served as 0.2; protected object's canonical DNA contains
  zero secret values over HTTP; version endpoint reports M06 facts.

### Milestone 07 — Reconstruction Rules System (869 tests)
- **07.0** core module designated the rules home (pom description).
- **07.1** Rule schema: `RuleType`, `RulePriority`, sealed `Constraint` hierarchy,
  `Rule`, `RuleSet` (duplicate-id refusal, DNA binding), `RuleSetCanonical`
  (canonical JSON with kind-tagged constraints, integrity hash), `RuleSetValidator`.
- **07.2–07.5** `DocumentRuleTemplates` + `RuleDeriver`: content (summary verbatim,
  concept/topic minimums), structure (outline mirror), fact (critical facts required),
  entity (recurring entities with mention minimums), relationship, ordering (section
  order from the outline) and validation (forbid invented facts, require all critical
  facts, confidence floor) rules derived deterministically from DNA alone.
- **07.6** `RuleConflictDetector` (errors vs warnings), `RuleRepository` (version-bound
  memoization + loader `save` + counters), `ReconstructionPlanner` + `ReconstructionPlan`
  + `PlanChecker`/`PlanCompliance`.
- **07.7** Integration: reconstruction mock renders with real plan provenance and checks
  its artifact with `PlanChecker` (strict rejection), `/version` rules diagnostics,
  `MilestoneSevenAcceptanceTest`.
- Acceptance proven: plan from DNA alone (core + live), required facts survive
  (PlanChecker, incl. negative tests), structural constraints enforced, conflicts
  produce explicit errors/warnings.
- Live verification: memorized object reconstructed with `sfs-rules/0.2` provenance and
  a satisfied "Plan rules" finding (4 facts, 3 entities, 2 relationships, 3 sections);
  protected document refused 422 with a REJECTED job; rules counters in /version.

### Milestone 08 — Memory System (877 tests)
- **08.0** `sfs-memory` module + H2 dependency (D-042); core `DnaRepository` interface.
- **08.1–08.2** Relational schema (17 tables incl. schema_version) + `H2MemoryDatabase`
  repositories: DNA records with canonical JSON and hash chains, graph rows, embeddings,
  security references, rule sets, raw-content BLOBs, lifecycle objects/events.
- **08.3** Version history: per-version DNA rows with previous-hash chaining; lifecycle
  events persisted (write-through from the single audit choke point).
- **08.4–08.5** `VectorIndex` (cosine top-k, rebuild) upserted on save and rebuilt from
  canonical records; embedding table as the durable source for the index.
- **08.6** Graph/relationship persistence with per-version delete+insert.
- **08.7** Transaction boundaries: atomic composite commits, rollback on failure;
  restart restore runner (order 0) + seeder guard; analysis requeue/MEMORIZABLE rollback
  unchanged on top of restored state.
- **08.8** Storage accounting in `/version` (`storage` block); milestone reports M08.
- Acceptance proven (file-mode restart test): memorized (deleted) Semantic Record and
  lifecycle state survive a close/reopen; lookup deterministic; index rebuildable;
  IDs consistent; storage measurable. UI tests run against the real file-backed DB.
- Note: the UI suite now shares one `./data/sfs-memory` H2 file; the seeder seeds only
  an empty DB, so repeated runs accumulate state by design (durable system).

### Milestone 09 — Semantic Search Engine (880 tests)

- **09.1** `QueryParser` → `ParsedQuery`: stop-word reduction, exact Object-ID detection
  (real `ObjectId` validation), validation failure on queries with no searchable terms.
- **09.2** `QueryEmbedder`: deterministic 64-dim L2-normalized query embedding through the
  same analyzer chain as documents (equivalence test vs the engine IR pipeline).
- **09.3** Vector retrieval: cosine top-k over the M08 `VectorIndex`; searchable-state
  metadata filters from the memory DB; unknown exact IDs → empty results.
- **09.4–09.5** Relationship/context enrichment: concepts, entities, facts and recorded
  relationships of candidate objects feed both reranking features and evidence entries.
- **09.6** `RankingProfile` reranking: weighted feature blend (vector similarity dominant),
  deterministic, relevance clamped to [0,1], descending order.
- **09.7** `SearchEvidence` per result (vector similarity, matched concept/entity/fact,
  relationship, summary coverage); `SearchEngine.toString` and responses never carry
  protected values.
- **Benchmark** `SearchEngineTest`: three themed real documents analyzed by the real
  engine; per-query top-1 wins; exact-ID lookup exact; unknown ID empty; protected
  secrets absent from responses.
- **Swap** Real `SearchEngine` bean replaces the deleted `MockSearchService`; POST/GET
  `/api/v1/search`, the UI search page and all frozen contracts unchanged; 8-test HTTP
  acceptance suite (`MilestoneNineAcceptanceTest`) proves semantic results, exact lookup,
  empty-result semantics, secret non-exposure, anonymous GET, 400 on empty query, version
  diagnostics and the UI page over real HTTP.
- Acceptance proven: benchmark queries find the correct documents; exact Object ID lookup
  is exact; protected secrets are not exposed; reconstruction starts only by explicit
  action (search returns Object IDs + evidence, never artifacts).

### Milestone 10 — SFS Reconstruction Model (920 tests)

- **Model contract (10.1)** frozen: `SFSReconstructionModel`, `ModelInput` (SemanticDna
  + ReconstructionPlan), `ModelOutput`, `Encoder`, `Decoder`, `ConstraintInterface`,
  `ModelRegistry`; identity must agree across input, DNA and plan.
- **Deterministic baseline (10.2)** `DeterministicBaselineRenderer`
  (`sfs-reconstruction/deterministic-baseline/0.1`) behind `PlanConstraintInterface`:
  every draft is judged against the plan (PlanChecker), protected values (refusal),
  critical facts (coverage) and the representation vocabulary (no fabricated content).
- **Encoders (10.3–10.6)**: `StructureEncoder` (recorded order, rule-required flags),
  `FactEntityEncoder` (criticality, confidence, required facts, required mention floors),
  `RelationshipEncoder` (required flags), composed by `SemanticDnaEncoder` with the
  protected-reference count over the `UnifiedRepresentation`.
- **Tiny decoder interface (10.7)**: the swappable `Decoder` slot with the default
  `TemplatedDecoder` (`sfs-reconstruction/templated-decoder/0.1`); swappable inside the
  model and through the registry.
- **Model evaluation (10.8)**: `ReconstructionBenchmark` measures fidelity components,
  latency and Knowledge Preservation Density over the frozen corpus; the naive echo
  probe measures strictly worse; reproducibility is test-enforced.
- **Training/future-learning hook (10.9)**: `TrainingHook` invoked per reconstruction;
  `NoopTrainingHook` records the V0.1 no-training assumption; `ModelSwapTest` proves
  replacement without Memory/Search changes; `/version` gains the `reconstruction`
  block.
- Acceptance proven: the model consumes DNA and Rules; baseline output passes the
  verifier on every clean corpus document; the benchmark is reproducible (identical
  drafts and measurements across runs); the model is replaceable without Memory/Search
  changes; no unsupported critical fact can pass silently (traceability violations are
  explicit, and the protected document is refused with a protected-values finding while
  no withheld value ever reaches the draft).
- The reconstruction flow keeps running on the development stand-in; the engine
  milestone (M11) swaps the flow onto this model and retires the mock renderer.

### Milestone 11 — Reconstruction Engine (943 tests)

- **Request manager (11.1)**: frozen `ReconstructionService` implemented by
  `ReconstructionEngine`; synchronous gate decisions (unknown object, no DNA, protected
  refusal) and an asynchronous `QUEUED → RUNNING → terminal` state machine on a
  single-thread executor; jobs immutable, audit-ordered, in-memory (restart-safe by
  deterministic regeneration); shutdown marks unfinished jobs failed explicitly.
- **DNA/rule loader (11.2)** + **planner (11.3)**: `DnaRuleLoader` loads file state and
  durable DNA through the core `DnaRepository` port; planning runs `ReconstructionPlanner`
  and records the real `rulesVersion` on the job (planning refusals become explicit
  `FAILED` jobs carrying the rule reasons).
- **Baseline/model execution (11.4)**: the engine generates through the frozen M10 model
  contract (default `deterministic-baseline/0.1` bean); throwing or empty models surface
  as explicit `FAILED`/`REJECTED` jobs without value leaks.
- **Constraint checking (11.5)**: `PlanConstraintInterface` judges every draft;
  findings keep the established `Plan rules (sfs-rules/0.2)` vocabulary with rule counts.
- **Post-processing + artifact (11.6–11.7)**: provenance header + estimated/reconstructed
  labeling + footer; `base.reconstructed.<jobId>.txt` UTF-8 artifact, downloadable only
  for completed jobs.
- **Single-click integration (11.8)**: engine bean wired in `EngineWiringConfiguration`
  (shutdown-aware); M01/M02/M07 journeys poll until terminal; new M11 HTTP acceptance
  suite covers the UI flow, provenance, reproducibility, refusals and diagnostics.
- Acceptance proven: single-click action produces a labeled TXT; job status is
  observable; no unauthorized secret appears; fixed versioned inputs reproduce identical
  artifacts; provenance (DNA/rule/model versions) is recorded; the engine never needs the
  original bytes and never silently invents unsupported critical facts.

### Milestone 12 — Evaluation & Fidelity System (976 tests)

- **Structures (12.1-12.6)**: `EvaluationInput`, the six evaluator score records with
  `CriticalFactScore` explicit, `ErrorCategory` (11 categories with advice), the six
  evaluators (semantic F1 vs the original, structural order, factual containment,
  entity presence, relationship direction, completeness coverage) and `FidelityReport`.
- **Overall fidelity (12.7)**: weighted blend with the critical-fact gate — a lost
  critical fact caps the overall regardless of semantic score.
- **Calibration (12.8)**: confidence bins vs observed survival with explicit deltas.
- **Regression (12.9)**: gold annotations over the frozen corpus + committed baseline
  properties; any dimension or overall drop fails the build.
- **Improvement loop (12.10)**: `ImprovementAdvisor` turns error categories into
  prioritized advice (correctness first); no silent DNA rewriting.
- **Swap**: `FidelityEvaluationService` replaces the deleted evaluation mock behind the
  unchanged frozen contract and `/api/v1/evaluations` shape; the report page states the
  numbers are measured; `/version` reports the evaluator and calibration diagnostics;
  the M12 acceptance suite proves all-metrics, explicit critical facts, reproducibility,
  unmeasurable-deleted-original, measured UI numbers and version reporting over HTTP.
- Acceptance proven: every reconstruction receives all required metrics; critical fact
  loss is visible; results are reproducible; regression cannot silently reduce fidelity;
  the storage/fidelity tradeoff is reported beside every measurement.

### Milestone 13 — Security & Privacy System (999 tests)

- **Detector (13.1)**: value-level `SensitiveDataDetector` in sfs-security replaces the
  engine's line-level detector; credential assignments, key-shaped values, bearer tokens,
  emails, phones (date-guarded), labeled accounts and addresses; sensitive lines are
  excluded before any semantic material is built (13.7 secret-safe embeddings by
  construction).
- **Policy (13.2)**: `SecurityPolicyEngine` with the eight-type table; passwords and
  unclassified data fail closed to locked redaction (13.8 non-reversible by default);
  stable value-hash reference ids (13.3).
- **Encrypted store (13.4) + keys (13.5)**: AES-256-GCM `FileEncryptedSecureStore` under
  `data/sfs-secure/`, master key under `data/sfs-keys/` — different directories, never
  the same file; fail-closed on missing key or wrong key.
- **Authorization (13.6)**: additive `RESOLVE_SECRET` capability, custodian-only;
  `SecretResolutionService` denies without it, denies unknown references, and can never
  resolve passwords; every attempt audited (13.9).
- **Swap**: authentication, authorization and settings mocks deleted; real beans wired;
  the engine optionally receives the vault and stores ENCRYPT-policy values at analysis
  time (7-arg constructor still discards fail-closed).
- Acceptance proven: synthetic secrets absent from ordinary DNA/vector/search; authorized
  access resolves permitted references; unauthorized access denied and audited; password
  policy non-reversible by default; keys not stored with ciphertext.

### Milestone 14 — Infrastructure / Observability / Testing (1024 tests)

- **Build (14.1)**: 13-module Maven build, Java 21, one-command verification via
  `scripts/verify.sh` (Java check, clean build with the full suite, packaging check).
- **Configuration (14.2)**: externalized, environment-overridable, versioned
  (`sfs-config/0.1`), loopback-enforcing, visible in `/version`.
- **Logging (14.3)** + **metrics/tracing (14.4)**: `sfs-log/0.1` structured events,
  `sfs-metrics/0.1` metrics snapshot, 16-hex trace ids on every request/response and in
  every log line via MDC; diagnostics carry shapes, never values.
- **Tests (14.5-14.8)**: 1024 automated tests across 12 modules — unit, per-milestone
  integration, a full-lifecycle E2E journey over HTTP, and security probes covering the
  error/trace/event leak channels.
- **Performance and storage/fidelity benchmarks (14.9-14.10)**: stage-separated
  measurements (file I/O, analysis, search, reconstruction, evaluation), byte-identical
  reproducibility, and storage accounting across original, artifact, DNA, vector,
  database and security files; the M12 committed baseline continues to enforce fidelity.
- **Release (14.11)**: executable fat jar verified with `java -jar` (no development
  tools), shipped as `sfs-V1.0-release.zip` with START.txt.
- Acceptance proven: clean build succeeds; all required tests pass; the end-to-end
  scenario passes; metrics are captured; the benchmark is reproducible; the release
  artifact runs without development tools.

### Deferred (unchanged)
Hosted CI (the repository is not hosted); transport security (TLS) — the V1 deployment
model is loopback-only and the release jar binds 127.0.0.1 by default; production
identity management beyond the V1 bootstrap identities; physical semantic filesystem,
mandatory LLM and byte-exact recovery stay out of V1 scope.

## 3. Verification history

| Milestone | Modules | Tests |
|---|---|---|
| M01 (01.8) | core 37, contracts+ui 285 | 322 |
| M02 final | core 37, app+contracts+ui 548 | 585 |
| M03 final | core 37, lifecycle 116, app 166, ui 379 | 698 |
| M04 final | core 37, lifecycle 116, engine 47, app 166, ui 375 | 741 |
| M05 final | core 37, lifecycle 120, adapters 42, engine 46, app 166, ui 380 | 791 |
| M06 final | core 67, lifecycle 120, adapters 42, engine 53, app 166, ui 380 | 828 |
| M07 final | core 105, lifecycle 120, adapters 42, engine 53, app 166, ui 383 | 869 |
| M08 final | core 105, lifecycle 120, adapters 42, engine 53, memory 8, app 166, ui 383 | 877 |
| M09 final | core 105, lifecycle 120, adapters 42, engine 53, memory 8, search 10, app 166, ui 376 | 880 |
| M10 final | core 105, lifecycle 120, adapters 42, engine 53, memory 8, search 10, reconstruction 40, app 166, ui 376 | 920 |
| M11 final | core 105, lifecycle 120, adapters 42, engine 53, memory 8, search 10, reconstruction 40, reconstruction-engine 34, app 166, ui 365 | 943 |
| M12 final | core 105, lifecycle 120, adapters 42, engine 53, memory 8, search 10, reconstruction 40, reconstruction-engine 34, evaluation 35, app 166, ui 363 | 976 |
| M13 final | core 105, lifecycle 120, adapters 42, engine 53, memory 8, search 10, reconstruction 40, reconstruction-engine 34, evaluation 35, security 26, app 167, ui 359 | 999 |
| M14 final | core 119, lifecycle 120, adapters 42, engine 53, memory 8, search 10, reconstruction 40, reconstruction-engine 34, evaluation 35, security 26, app 167, ui 370 | 1024 |

M04 was verified three ways: workspace `mvn clean install` (739/739), and a live
`mvn spring-boot:run` exercise over HTTP: import → analyze (async) → engine-produced
complete DNA (summary, typed entities, critical facts, relationships, headings,
64-dim embedding, `sfs-engine/0.1`), analysis job `COMPLETED` via the unified jobs API,
seed records engine-produced at startup, protected values detected without leaking
values, protected-document reconstruction `REJECTED`, UI pages serving.

M03 was verified three ways: workspace `mvn clean install` (698/698), a fresh extract of
`sfs-M03-FULL.zip` (698/698), and a live `mvn spring-boot:run` exercise of import →
analyze → delete → gate-refused purge (409) → undo → memorize → delete → purge (200),
with event audit, statistics, post-purge addressability and UI pages all checked over HTTP.

## 4. Delivery inventory (M03)

- `sfs-M03-FULL.zip` — full repository state, **excludes `docs/`**.
- `sfs-task-03.0 … 03.7-CHANGED-ONLY.zip` — per-task final file states, repo-root
  relative paths, applied in order 03.0 → 03.7; contain code/config only.

## 5. Working rules that live here (not in program files)

- No comments in program files; rationale belongs in this file or in test names.
- No decision codes, owner/agent attribution or process metadata in program files.
- `README.md` is the user's released M01 document; only factual-state sections may ever
  change, and only with explicit instruction.
- Records (decisions, changelog, verification) are kept only in this file.
