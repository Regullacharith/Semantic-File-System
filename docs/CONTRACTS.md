# SFS Contracts

**Version:** (UI)
**Module:** `sfs-contracts`
**Status:** Stable for UI.


The contracts module has one dependency: `sfs-core`. It contains no framework annotation, no
persistence concern and no semantic logic. It is the boundary that lets the UI be built and
tested before any engine exists, and lets the engine be replaced without touching the UI.
---

## Conventions

Rules that hold across every contract in the module.

| Rule | Reason |
|---|---|
| View models are immutable records | A view model handed to a template must not change while rendering. |
| Invariants are enforced in the canonical constructor | An object that cannot exist in an invalid state cannot be rendered in one. A check in a controller or template can be bypassed by the next caller. |
| Services return result types, not exceptions, for expected outcomes | A rejected import is an outcome, not a bug. Exceptions are reserved for genuine faults. |
| Collections are defensively copied and returned unmodifiable | Callers cannot mutate a record's contents after construction. |
| No contract carries a plaintext sensitive value | Enforced by reflection tests, not convention. See [Security contracts](#security-contracts). |
| Read-only unless a milestone requires otherwise | A method that cannot change state cannot change it wrongly. |

---

## File contracts

Package `com.sfs.contracts.file`.

### `FileStatus`

The lifecycle of a file known to SFS.

| Status | Meaning |
|---|---|
| `REGISTERED` | Imported. Raw bytes present. Not yet analyzed, so no Semantic DNA. |
| `ANALYZED` | Semantic DNA exists. Raw bytes still present. |
| `SOFT_DELETED` | **Reversibly deleted.** Withdrawn from normal use, but raw bytes are retained and the object can be restored. This is what ordinary deletion produces. |
| `MEMORIZED` | **Raw bytes permanently released.** The semantic record remains, searchable and reconstructable. This is what *purge* produces, and it is irreversible. |
| `FAILED` | Analysis failed. Re-analysis is permitted. |

Two rules carry the design:

- `allowsAnalysis()` is true only from `REGISTERED` or `FAILED`. Re-analyzing an
  already-analyzed file is not a supported V1 operation.
- `allowsSoftDeletion()` is true only from `ANALYZED`. Raw bytes may only start down the path
  toward release once a semantic record exists to replace them. Deleting from `REGISTERED` would
  destroy the file with nothing retained — data loss disguised as a feature.
- `allowsUndoDelete()` and `allowsPurge()` are true only from `SOFT_DELETED`. Because purge is
  reachable only from the deleted state, **no single call can take a live file to permanent
  release**: destruction always takes two deliberate, separately authorized steps.

`MEMORIZED` is the concept most easily misread. It does not mean "deleted". It means the
original bytes are gone and the meaning was kept. The UI must never present it as deletion.

### `DeletionConfirmation`

Object-bound confirmation for destructive operations. Carries the Object ID the caller believes
they are affecting, and the application layer refuses the operation unless it names the object in
the path.

A bare `confirm: true` flag was rejected: it is equally true for every object, so a request body
copied from one call and replayed against a different Object ID would still satisfy it.
Confirmation is validated at the application boundary, never in the UI — a browser dialog is
absent entirely when the API is called directly.

### `FileImportRequest`

Rejects a blank file name, blank content, and any name containing a path separator or a
parent-directory reference. A file name is a name, not a path; accepting a path would invite
traversal once a real storage backend exist. Validating at import — the
system's edge — means no later component has to re-validate.

### `FileOperationResult`

Success or failure with a human-readable message. Import, analyze, delete, restore and purge all
return this rather than throwing, because each has expected failure modes that are not faults.

### `FileService`

`importFile`, `requestAnalysis`, `softDelete`, `undoDelete`, `purgeRawData`, `listFiles`,
`findByObjectId`.

**Import does not trigger analysis.** They are separate operations because analysis is
expensive and, may need scheduling, cancellation and a progress report.
Coupling them here would force that decision now.

---

## Search contracts

Package `com.sfs.contracts.search`.

### `SearchQuery`

Text of 1–500 characters; result count 1–100, defaulting to 20. `isObjectIdLookup()` matches
`^sfs-obj-[0-9]{4}-[a-zA-Z0-9]+$`, letting the engine treat an exact ID as a lookup instead of
a similarity search.

### `SearchEvidence`

Why a result matched. **Evidence may never carry a protected value.** Search explains itself
by quoting the document, and quoting a detected secret would leak through the explanation the
rest of the system is careful to protect.

### `SearchResult` / `SearchResponse`

A result without evidence is rejected at construction: search must always be able to explain
why something matched. `SearchResponse` reports whether a search actually ran, so an empty
result set is never confused with a search that was never performed.

### `SearchService`

Read-only. **Search never reconstructs.** Reconstruction is expensive, produces a new
artifact and must be explicitly requested; a search that reconstructed as a side effect would
make an ordinary query an expensive state change.

---

## Semantic contracts

Package `com.sfs.contracts.semantic`.

### `ProtectedReferenceView`

A detected sensitive value, represented **by reference only**. The nested `SensitiveType` enum
covers `PASSWORD`, `API_KEY`, `ACCESS_TOKEN`, `EMAIL_ADDRESS`, `PHONE_NUMBER`,
`POSTAL_ADDRESS`, `ACCOUNT_IDENTIFIER` and `OTHER`.

**This record has no field capable of holding a plaintext value, and a reflection test
enforces that.** The reasoning: every other protection — redaction in logs, exclusion from
embeddings, authorization on resolution — is a runtime behaviour that a future change could
bypass. Having no field to put a secret in is a structural guarantee that no amount of
downstream carelessness can undo.

### `SemanticDnaView`

The semantic record of an analyzed file: concepts, entities, relationships, structure and a
`FidelityProfileView`.

**`FidelityProfileView` is analysis-time quality — how well the file was understood. It is
not reconstruction fidelity.** Conflating the two would let an unmeasured reconstruction
inherit a plausible-looking score from analysis.

### `SemanticRecordService`

Read-only. A missing DNA record is a **normal condition, not an error**: a `REGISTERED` file
legitimately has none. The service returns an empty optional and the UI explains the state,
rather than raising a 404 for a file that exists.

---

## Reconstruction contracts

Package `com.sfs.contracts.reconstruction`.

### `ReconstructionStatus`

`PENDING`, `RUNNING`, `COMPLETED`, `REJECTED`, `FAILED`.

**`REJECTED` is not `FAILED`.** Rejected means the system refused on policy grounds — for
example, the reconstruction would expose protected references. Failed means it tried and
broke. Merging them would hide a security refusal inside a generic error and invite a retry.

### `ReconstructionJobView`

Reconstruction returns a **job**, not a file. Even though the V1 mock completes synchronously,
the contract is async-shaped because real reconstruction will not be instant, and retrofitting
a job model later would change every caller.

Enforced invariants:

- **Provenance is mandatory** — DNA version, rules version and generator version
  (`sfs-dna/ v1`, `sfs-rules/0.1`, `deterministic-baseline`). A reconstruction whose
  origin cannot be identified cannot be audited or reproduced.
- A `COMPLETED` job must have an artifact; any other status must not. A rejected or failed
  job exposing a downloadable artifact would be the worst possible outcome of a refusal.

### `ReconstructionArtifact`

**Never byte-identical to the original, and never named as though it were.** The file is named
`<base>.reconstructed.<jobId>.txt`, carries a `NOT THE ORIGINAL FILE` banner, and is served as
`attachment` with `text/plain;charset=UTF-8`. Byte-exact recovery is explicitly not a V1
objective, and an artifact that looked like the original would misrepresent the whole system.

### `ReconstructionService`

Reconstruction starts **only** from an explicit POST; GET returns 405. It changes state and
consumes resources, so it must never be triggered by a link, a crawler or a prefetch.

---

## Evaluation contracts

Package `com.sfs.contracts.evaluation`.

### `FidelityDimension`

`SEMANTIC`, `STRUCTURAL`, `FACTUAL`, `ENTITY`, `RELATIONSHIP`, `COMPLETENESS`.

`FACTUAL`, `ENTITY` and `RELATIONSHIP` are the correctness-critical dimensions. A display-only
concern threshold of 0.80 highlights them. It is a display cue, **not** an acceptance
criterion and not a target.

### `FidelityReportView`

- **Every dimension must be scored**, each 0.0–1.0. A partial report would let an unmeasured
  dimension read as an absent problem.
- **No aggregate or overall fidelity figure exists**, and reflection tests forbid component
  names that would introduce one. A single blended number invites exactly the unsupported
  "N% fidelity" claim the project prohibits.
- `criticalFactsPreserved` may not exceed `criticalFactsTotal`, and critical facts are
  reported separately (`2 of 3`) rather than folded into the `FACTUAL` score, where a small
  number of critical errors would be diluted by bulk correctness.
- Storage is reported as **knowledge preservation density**, never as a compression saving.

### `EvaluationAvailability`

`AVAILABLE` requires a report. Every other status requires a **reason** and must carry no
report. `ORIGINAL_UNAVAILABLE` (the `MEMORIZED` case) shows no percentage, no bar and no
placeholder: with the original gone there is nothing to compare against, and a zero or a
greyed-out bar would both imply a measurement that was never made.

---

## Security contracts

Package `com.sfs.contracts.security`. **Reported, not enforced, in V1** — the enforcing
subsystem .

### `HandlingPolicy`

| Policy | Reversible | Meaning |
|---|---|---|
| `REDACT` | No | The value is discarded. |
| `TOKENIZE` | No | Replaced by a stable non-reversible token. |
| `ENCRYPT` | Yes | Recoverable under authorization. |

There is deliberately **no plaintext-retention constant** — the enum offers no way to express
"keep it as-is". `REDACT` is declared first so that any future fallback-to-first-constant
fails closed.

### `SensitiveTypePolicy`

Binds a `SensitiveType` to a `HandlingPolicy` with a rationale and a `locked` flag.

**The canonical constructor throws if a password is paired with reversible handling.**
`PASSWORD` is in a `NEVER_REVERSIBLE` set, and `permittedOptions()` omits `ENCRYPT` for
passwords so an invalid option is never offered in the first place. Placing this in the
constructor rather than the UI means it holds regardless of template, form or crafted request.

### `SecuritySettingsView`

**Rejects construction** if any of `embeddingsExcludeSecrets`, `logsExcludeSecrets`,
`dnaExcludeSecrets` or `authorizationRequired` is false. These are invariants reported as
facts, not switches to be toggled. A regression that disabled one would fail at startup rather
than silently render "disabled" on a settings page nobody re-reads.

Also rejects an empty policy set (an unhandled type would fall through the policy engine) and
a blank key-storage description.

`AuditEventView` records the action, the reference id and whether it was permitted. A
reflection test forbids components named `value`, `plaintext`, `secret` or `payload`: an audit
trail that recorded the secret would become the largest plaintext store in the system.

### `AuthenticationService`, `AuthorizationService`, `Principal`, `Capability`

The security boundary for destructive operations.

`AuthenticationService` answers "who is this?" and returns a `Principal`, or empty. It takes an
opaque credential string so a later implementation can accept a bearer token, session identifier
or signed assertion without changing any caller. `AuthorizationService` answers the separate
question "may they do this?" — collapsing the two, and treating any authenticated caller as
permitted, is the mistake the split exists to prevent.

`Principal` carries **no credential**: no password, token, secret or key. It is the *result* of
authentication, not a means of performing it, so it can be logged, passed between layers and
recorded in an audit entry without risking disclosure. A reflection test enforces this.

`Capability` is a small closed set — `READ`, `WRITE`, `DELETE_RAW`, `UNDO_DELETE`, `PURGE_RAW` —
rather than a role system, which would be scope creep into M13. The three destructive
capabilities are separate on purpose: holding `DELETE_RAW` must not imply `PURGE_RAW`, because
reversible withdrawal and permanent destruction are different levels of trust.

**Implementation status.** The boundaries are real and enforced. The implementations behind them
are development stubs until Milestone 13, so this build must not be treated as securely
authenticated.

### `SecuritySettingsService`

Read-only `getSettings()`. There is no setter, because changing a policy requires the
authorization and audit machinery. A control that appeared to change a policy
without enforcing or recording the change would be worse than no control at all.

---

## Versioning

Contracts are versioned with the project (`0.1.0-SNAPSHOT`). Provenance strings carry their
own versions (`sfs-dna/0.1`, `sfs-rules/0.1`) so an artifact produced today remains
identifiable after the formats change.

UI keeps this module **deliberately minimal**: only what the UI needs.the process is to stop, 
report the dependency and the affected components, and wait for authorization
— not to widen the interface silently.
