#  Acceptance

** User / Interface Layer:** 

##  Acceptance 

| # | | Status | Evidence |
|---|---|---|---|
| 1 | Application starts cleanly | **Met** | `Startup.servesEveryScreen` — all 7 routes HTTP 200 against a live server. Boot logs show no error on startup. |
| 2 | A TXT can be imported and its Object ID displayed | **Met** | `ImportJourney.importsAndDisplaysObjectId` — real `multipart/form-data` upload, Object ID matching `sfs-obj-[0-9]{4}-[a-z0-9]+` read back from `/files`. |
| 3 | Semantic search results displayed | **Met, with a caveat** | `SearchJourney.displaysResults`. The caveat is decisive: matching is **keyword substring**, not semantic. |
| 4 | A selected result launches one explicit reconstruction action | **Met** | `ReconstructionJourney.reconstructsFromExplicitPost` (POST returns a job) and `neverReconstructsFromGet` (GET → 405). |
| 5 | Fidelity report viewable | **Met** | `EvaluationJourney.showsFidelityReport` — job created, report rendered. Figures are constants;. |
| 6 | No semantic logic in the UI | **Met** | `ArchitectureBoundaryTest` (3 tests). Controllers read from contracts and pass data to templates. |

### Definition of Exit

| # | Requirement | Status | Evidence |
|---|---|---|---|
| 1 | Automated tests pass from a clean build | **Met** | `mvn clean install` → **322 tests, 0 failures, 0 errors**. Verified . |
| 2 | Contracts versioned and documented | **Met** | `docs/CONTRACTS.md` — full reference with rationale. Version 0.1, provenance strings versioned independently. |
| 3 | No deferred V1 feature accidentally introduced | **Met** | . Text-only enforced at import; no adapter, no filesystem, no LLM, no byte-exact claim. |
| 4 | Observability captures failures and performance measurement | **Partially met** | Failures are logged and surfaced; **performance measurement is not implemented**. — this is the one gap. |

---

##  Test results

Executed `mvn -B clean install` on Java 21.0.11 with Maven 3.9.16.

```
Tests run: 322, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

| Layer | Tests | What it covers |
|---|---:|---|
| Contract invariants | 76 | Search, semantic, evaluation and security contract rules |
| Mock services | 55 | Seeded data, state transitions, defaults |
| Controllers | 87 | Routes, status codes, rendering, method guards |
| View models | 57 | Validation, immutability, error message safety |
| Architecture / startup | 6 | Layer boundaries, context loads |
| **Milestone integration (new)** | **15** | **End-to-end journeys over real HTTP** |
| Error handling (new) | 10 | Error page, status codes, no framework leakage |

The 15 integration tests are the milestone's acceptance evidence. Unlike the `MockMvc` tests,
they run against a **real embedded server on a random port** with a cookie-managing HTTP client,
exercising redirects, multipart uploads and the container's error dispatch. That distinction
matters: the Whitelabel error page fixed in this task was **invisible to `MockMvc`**, which does
not perform an ERROR dispatch and returned an empty body where a browser saw a framework page.

---

##  What was verified

 labelling, project rules.

| Claim | Level |
|---|---|
| Compiles from clean | **Verified** — `mvn clean install`|
| Unit tested | **Verified** — 322 automated tests |
| Integration tested | **Verified** — 15 journeys over real HTTP against a live server |
| Manually exercised | **Verified** — `curl` against the packaged jar |
| Experimentally evaluated | **Not done** — nothing is measured; no evaluator exists |
| Accepted | |
| Production ready | Every subsystem is a mock. |

Not performed: load, soak, concurrency, browser-compatibility, accessibility audit, penetration
test, third-party security review.

---

## state of each subsystem

| Subsystem | What it actually does  |
|---|---|---|
| **Search** | Keyword substring match over 4 hardcoded documents. No embeddings, no vector index, no model. `meeting minutes` returns **0 results despite `meeting-notes.txt` existing** — the clearest demonstration that this is not semantic search.|
| **Reconstruction** | Generates no language. Deterministically formats DNA fixtures. Jobs complete synchronously behind an async-shaped status machine. | 
| **Evaluation** | Measures nothing. Scores are fixed constants (SEMANTIC .91, STRUCTURAL .88, FACTUAL .76, ENTITY .94, RELATIONSHIP .83, COMPLETENESS .87; critical facts 2/3).  |
| **Security** | Enforces nothing. No detector, policy engine, encrypted store, key management or audit trail. Reports intended configuration only. | |
| **Storage** | In-memory. **All data is lost on restart.**  |

**No fidelity percentage in this build is a measurement.** The 87–92% figure in project
documentation is an experimental target, not a result.

---

##  V1 scope compliance

| Locked constraint | Status |
|---|---|
| Text files only | **Held** — non-UTF-8 upload rejected at import (`rejectsNonTextFile`) |
| No image / audio / video / executable / source-code adapters | **Held** — none exist |
| No physical semantic filesystem | **Held** — no mount, no driver |
| No mandatory large LLM | **Held** — no model dependency of any kind |
| Byte-exact recovery not claimed | **Held** — artifacts carry a `NOT THE ORIGINAL FILE` banner and a distinct filename |
| Term "Reconstruction Rules" (never "Grammar") | **Held** — verified across source, templates and docs |

---

##  Known gaps and risks

| # | Item | Severity | Note |
|---|---|---|---|
| 1 | **No performance measurement** | **Medium** | Definition of Exit asks observability to capture performance measurements. Failures are logged; timings are not. Recommend a timing/metrics task early in M02, before there is real work worth measuring. |
| 2 | Deprecated starter (**D-009**) | Low | `spring-boot-starter-web` is deprecated in Boot 4 in favour of `spring-boot-starter-webmvc`. Still not actioned. Trivial now; noisier later. |
| 3 | Raw deletion has no confirmation step | **Medium** | "Delete raw data" is irreversible and one click, with no confirmation. Currently harmless because storage is in-memory mock data. **Must not ship to real storage in API as-is.** |
| 4 | Mock-search notice is not prominent | Low | The disclosure exists but is easy to miss during a demo, where keyword matching is most likely to be mistaken for semantic search. |
| 5 | In-memory storage | Expected | Everything is lost on restart. Resolved by API. |
| 6 | No authentication | Expected | Any visitor can import and delete. Acceptable for a local mock; blocking for any shared deployment. |

---
