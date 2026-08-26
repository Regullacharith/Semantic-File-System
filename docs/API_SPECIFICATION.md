# SFS V1 — API Specification

**API version:** `v1`
**Document status:** **FROZEN ** This document defines the contract; the
implementation phases conform to it. Any deviation discovered during implementation is reported
as a change request against this document, not applied silently. `ApiContractFreezeTest` parses
this file and fails the build if the implementation and the contract drift apart.
**Base path:** `/api/v1`
**Media type:** `application/json;charset=UTF-8`
**Binding:** `127.0.0.1` by default.

---

## 1. Scope of the freeze

This task freezes **what the API is**, not how it is built. Frozen here: the operation list,
paths, methods, request and response semantics, status codes and the error model.

The acceptance criterion driving the whole design is: *every required V1 operation can be called
without the UI.* The inventory in §3 is derived from the existing contract services mapped to
HTTP. Nothing is invented beyond what those services expose, with one deliberate exception: the
file lifecycle gains reversible deletion, restore and purge, because exposing destructive
operations over HTTP requires a model that a single mistaken request cannot make irreversible
(§6.3, and `DELETION_DESIGN.md`).

**Not frozen, and deliberately deferred:** field-level JSON key names beyond those shown,
pagination cursors, ETag/caching, rate limits, and content negotiation beyond JSON.

---

## 2. Design rules

| # | Rule | Reason |
|---|---|---|
| 1 | Read operations are `GET`; state changes are `POST` | A state change must never be reachable by a link, prefetch or crawler. This already holds for reconstruction . |
| 2 | `GET` is side-effect free | A `GET` that mutates makes retries unsafe. |
| 3 | Paths are nouns; the method carries the verb | Except sub-resource actions, which are `POST /{id}/{action}` — the smallest deviation that keeps lifecycle actions explicit. |
| 4 | Every error is the same JSON shape | A caller writes one error handler. |
| 5 | Every error carries a stable machine-readable `code` | Callers must branch on codes, never on prose. Prose may be reworded; codes may not. |
| 6 | No response ever contains a plaintext sensitive value | The UI structural guarantee extends to the API. |
| 7 | Long-running work returns a job, never a blocking call | API requirement; already true of reconstruction. |
| 8 | Absent optional data is `200` with an explaining body, not `404` | A `REGISTERED` file legitimately has no Semantic DNA. That is a state, not a missing resource. |
| 9 | The API never invents an operation the domain cannot honour | E.g. there is no "cancel job": nothing behind the API can cancel one. |
| 10 | Every destructive operation requires authentication, authorization and object-bound confirmation | Three independent controls. No single mistake — a stolen session, an over-broad permission, a replayed request body — is sufficient on its own. |
| 11 | Deletion is reversible; only `purge` destroys | Raw bytes are released only by a separate, separately authorized operation reachable only from the deleted state. |

---

## 3. Operation inventory

20 operations. The **Source** column shows the contract method each one exposes, proving the API
adds no domain behaviour.

| # | Method | Path | Operation | Source |
|---|---|---|---|---|
| 1 | `GET` | `/api/v1/files` | List files | `FileService.listFiles` |
| 2 | `GET` | `/api/v1/files/{objectId}` | Get one file | `FileService.findByObjectId` |
| 3 | `POST` | `/api/v1/files` | Import a text file | `FileService.importFile` |
| 4 | `POST` | `/api/v1/files/{objectId}/analyze` | Request analysis | `FileService.requestAnalysis` |
| 5 | `DELETE` | `/api/v1/files/{objectId}` | Reversible deletion | `FileService.softDelete` |
| 5a | `POST` | `/api/v1/files/{objectId}/undo-delete` | Restore | `FileService.undoDelete` |
| 5b | `POST` | `/api/v1/files/{objectId}/purge` | Permanent purge | `FileService.purgeRawData` |
| 6 | `GET` | `/api/v1/objects/{objectId}/dna` | Get Semantic DNA | `SemanticRecordService.findSemanticDna` |
| 7 | `POST` | `/api/v1/search` | Semantic search | `SearchService.search` |
| 8 | `POST` | `/api/v1/reconstructions` | Start reconstruction | `ReconstructionService.requestReconstruction` |
| 9 | `GET` | `/api/v1/reconstructions` | List jobs | `ReconstructionService.listJobs` |
| 10 | `GET` | `/api/v1/reconstructions/{jobId}` | Get job status | `ReconstructionService.findJob` |
| 11 | `GET` | `/api/v1/reconstructions/{jobId}/artifact` | Download artifact | `ReconstructionService.findArtifact` |
| 12 | `GET` | `/api/v1/evaluations` | List evaluations | `EvaluationService.listEvaluations` |
| 13 | `GET` | `/api/v1/evaluations/{jobId}` | Get fidelity report | `EvaluationService.findEvaluation` |
| 14 | `GET` | `/api/v1/security/settings` | Get security policy | `SecuritySettingsService.getSettings` |
| 15 | `GET` | `/api/v1/jobs/{jobId}` | Unified job status | §9 |
| 16 | `GET` | `/api/v1/health` | Liveness | new |
| 17 | `GET` | `/api/v1/version` | API and contract versions | new |
| 18 | `GET` | `/api/v1/search` | Semantic search (alias) | `SearchService.search` |

Three notes on the deviations:

- **#7/#18 search accepts both `POST` and `GET`.** `POST` is the primary form: query text can be
  long and should not land in logs or history. A `GET ?q=` alias is **also provided** for
  convenient `curl` and browser testing. Both map to the same `SearchService.search` call and
  return an identical body. The logging rule in §10 accounts for this: the search query string is
  excluded from request logging on both forms.
- **#15 duplicates #10 today.** M02 requires a job/status API, and reconstruction is currently
  the only job type. Analysis becomes a job in M03/M04. Freezing `/jobs/{jobId}` now means later
  job types do not force a new path shape on callers.
- **#16/#17 are new but not features.** Health supports the Definition of Exit's observability
  requirement; version lets a client detect contract drift.

---

## 4. Status codes

| Code | Meaning in SFS | Used by |
|---|---|---|
| `200 OK` | Success, body follows | All reads; analyze, delete, undo-delete, purge |
| `201 Created` | Resource created; `Location` header set | Import (#3), start reconstruction (#8) |
| `400 Bad Request` | Malformed syntax or invalid field | All |
| `404 Not Found` | Addressed resource does not exist | Path-parameter operations |
| `405 Method Not Allowed` | Wrong method for a real path | All |
| `409 Conflict` | Valid request, wrong state | #4, #5 |
| `413 Payload Too Large` | Upload exceeds the limit | #3 |
| `415 Unsupported Media Type` | Not JSON, or not a text file | #3, all POSTs |
| `422 Unprocessable Entity` | Syntactically valid, semantically refused | #8 (protected references) |
| `500 Internal Server Error` | Unexpected failure | All |

Two distinctions that matter, both inherited from M01:

- **`409` vs `422`.** `409` is a lifecycle conflict — analyzing an already-analyzed file. `422`
  is a policy refusal — the request was understood and deliberately refused. A rejected
  reconstruction is **not** a failure and must not read as one.
- **`404` vs `200`-with-explanation.** A file that exists but has no DNA yet returns `200` with
  `present: false`. Only a genuinely unknown identifier is `404`. Rule 8 in §2.

---

## 5. Error model

Every non-2xx response, without exception, uses this shape:

```json
{
  "code": "OBJECT_ID_INVALID",
  "message": "Object ID must match the format sfs-obj-<4 digits>-<alphanumeric suffix>.",
  "status": 400,
  "timestamp": "2026-08-20T14:35:12Z",
  "path": "/api/v1/files/not-an-id",
  "details": []
}
```

| Field | Type | Notes |
|---|---|---|
| `code` | string | **Stable.** Callers branch on this. Never reworded. |
| `message` | string | Human-readable. May be reworded; never contains a secret, stack trace, class name or internal path. |
| `status` | int | Mirrors the HTTP status. |
| `timestamp` | string | ISO-8601 UTC. |
| `path` | string | Requested path, echoed safely (escaped, length-capped). |
| `details` | array | Zero or more `{ "field": "...", "issue": "..." }`. Empty, never null. |

### Frozen error codes

| Code | Status | Meaning |
|---|---|---|
| `VALIDATION_FAILED` | 400 | One or more fields invalid; see `details` |
| `CONFIRMATION_REQUIRED` | 400 | A destructive operation was called without confirmation |
| `CONFIRMATION_MISMATCH` | 400 | The confirmation named a different object |
| `AUTHENTICATION_REQUIRED` | 401 | No valid credential was supplied |
| `NOT_PERMITTED` | 403 | Authenticated, but the capability is not held |
| `OBJECT_ID_INVALID` | 400 | Malformed Object ID |
| `JOB_ID_INVALID` | 400 | Malformed job ID |
| `REQUEST_MALFORMED` | 400 | Body is not readable JSON |
| `FILE_NOT_FOUND` | 404 | No file with that Object ID |
| `JOB_NOT_FOUND` | 404 | No job with that ID |
| `EVALUATION_NOT_FOUND` | 404 | No evaluation for that job |
| `ARTIFACT_NOT_AVAILABLE` | 404 | Job exists but has no artifact |
| `METHOD_NOT_ALLOWED` | 405 | Wrong method |
| `INVALID_STATE_TRANSITION` | 409 | Operation not permitted from the current status |
| `PAYLOAD_TOO_LARGE` | 413 | Upload exceeds the limit |
| `UNSUPPORTED_MEDIA_TYPE` | 415 | Wrong content type, or not UTF-8 text |
| `RECONSTRUCTION_REFUSED` | 422 | Refused on policy grounds, e.g. protected references |
| `INTERNAL_ERROR` | 500 | Unexpected failure; details are logged, not returned |

**Determinism requirement.** The same invalid input must always produce the same `code` and
`status`. This is an acceptance criterion and will be asserted by repeating identical bad
requests and comparing responses.

---

## 6. Operations

Only non-obvious semantics are specified here.
### 6.1 `POST /api/v1/files` — import

Two content types accepted:

- `multipart/form-data` with a `file` part — mirrors the UI path.
- `application/json` with `{ "fileName": "...", "content": "..." }` — needed for a UI-free caller.

Rules: UTF-8 text only (else `415`); size limit enforced (else `413`); file name must not contain
a path separator or `..` (else `400 VALIDATION_FAILED`). **Import does not trigger analysis** —
`201` with `Location: /api/v1/files/{objectId}` and status `REGISTERED`.

### 6.2 `POST /api/v1/files/{objectId}/analyze`

Permitted only from `REGISTERED` or `FAILED`; otherwise `409 INVALID_STATE_TRANSITION`.

### 6.3 Deletion, restore and purge

Deletion is a **two-stage** design. Ordinary deletion destroys nothing; a separate purge
operation releases raw bytes permanently.

```
ANALYZED --DELETE /files/{id}--> SOFT_DELETED --POST /files/{id}/purge--> MEMORIZED
                                      |                                      |
                                      +--POST /files/{id}/undo-delete-->     +-- raw bytes released
                                         ANALYZED                            +-- Semantic Record survives
```

`MEMORIZED` is the terminal purge state. It is the pre-existing SFS state meaning "raw bytes
released, Semantic Record survives", so no second state with the same meaning was invented.

**All three operations require authentication.** Delete and purge additionally require
object-bound confirmation. Capabilities are distinct: `DELETE_RAW`, `UNDO_DELETE` and
`PURGE_RAW`. Holding `DELETE_RAW` does **not** confer `PURGE_RAW`.

| Operation | Method and path | Capability | Confirmation | From state |
|---|---|---|---|---|
| Delete | `DELETE /api/v1/files/{objectId}` | `DELETE_RAW` | Required | `ANALYZED` |
| Restore | `POST /api/v1/files/{objectId}/undo-delete` | `UNDO_DELETE` | Not required | `SOFT_DELETED` |
| Purge | `POST /api/v1/files/{objectId}/purge` | `PURGE_RAW` | Required | `SOFT_DELETED` |

Restore requires no confirmation because it destroys nothing; demanding confirmation for safe
corrective actions trains callers to confirm reflexively, weakening it where it matters.

**Credential.** Supplied in the `X-SFS-Credential` header — not a body field that might be
logged, and not a query parameter that would reach access logs and history.

**Confirmation body** for delete and purge:

```json
{ "confirmObjectId": "sfs-obj-0001-a1b2c3d4" }
```

The confirmation must name the object in the path. A bare `{"confirm": true}` was rejected as a
design: it is equally true for every object, so a body copied from one call and replayed against
a different Object ID would still pass.

**Purge is irreversible.** After purge, `undo-delete` returns `409`. SFS reconstruction is
semantic and approximate, so the original bytes cannot be recovered by any means.

**No unguarded destructive path exists.** There is deliberately no endpoint that releases raw
bytes in a single call. Any such route would bypass the two-step lifecycle and the capability
separation above, so a test asserts that `/files/{objectId}/delete-raw` — the shape such an
endpoint would most plausibly take — is not routable.

### 6.4 `GET /api/v1/reconstructions/{jobId}/artifact`

The one non-JSON response: `text/plain;charset=UTF-8`, `Content-Disposition: attachment`,
filename `<base>.reconstructed.<jobId>.txt`, and the `NOT THE ORIGINAL FILE` banner retained.
`404 ARTIFACT_NOT_AVAILABLE` if the job is rejected, failed or incomplete.

### 6.5 `GET /api/v1/objects/{objectId}/dna`

`200` with `present: false` and a `reason` when the file exists but has no DNA. `404` only when
the Object ID is unknown.

### 6.6 `GET /api/v1/security/settings`

Read-only. **There is no `PUT` or `PATCH`.** Policy edits require the authorization and audit
machinery; an endpoint that appeared to change policy without enforcing it would be worse
than none.

---

## 7. Request models

| Model | Fields | Validation |
|---|---|---|
| `FileImportRequest` | `fileName`, `content`, `contentType?` | Name non-blank, no separator or `..`; content non-empty UTF-8; size limit |
| `SearchRequest` | `text`, `maxResults?` | Text 1–500 chars; `maxResults` 1–100, default 20 |
| `ReconstructionRequest` | `objectId` | Valid `ObjectId` |
| `EvaluationRequest` | `jobId` | Valid job ID |

`EvaluationRequest`evaluation is retrieved by path parameter
. It is frozen as a model for symmetry and future filtering; it is **not** currently a
request body. Flagged rather than silently omitted.

---

## 8. Response models

| Model | Purpose | Key fields |
|---|---|---|
| `FileResponse` | One file | `objectId`, `fileName`, `status`, `sizeBytes`, `importedAt`, `allowsAnalysis`, `allowsSemanticDeletion` |
| `SemanticRecordResponse` | Semantic DNA | `objectId`, `present`, `reason?`, `concepts`, `entities`, `relationships`, `structure`, `protectedReferences`, `fidelityProfile` |
| `JobStatusResponse` | Job state | `jobId`, `objectId`, `status`, `submittedAt`, `completedAt?`, `provenance`, `hasArtifact`, `findings` |
| `FidelityReportResponse` | Fidelity | `jobId`, `objectId`, `availability`, `reason?`, `dimensions[]`, `criticalFactsPreserved`, `criticalFactsTotal`, `evaluator` |
| `SearchResponse` | Results | `query`, `searched`, `totalResults`, `elapsedMillis`, `results[]` |
| `SecuritySettingsResponse` | Policy | `typePolicies[]`, `keyStorageDescription`, mandatory-protection flags, `auditEvents[]` |
| `ErrorResponse` | Errors | §5 |

Three invariants carried over from UI and **binding on the API**:

1. `protectedReferences` expose **no plaintext value** — reference and type only.
2. `FidelityReportResponse` has **no aggregate or overall score field**. Six dimensions are
   reported separately; a blended number invites the unsupported "N% fidelity" claim.
3. Where availability is not `AVAILABLE`, there is a `reason` and **no** dimension scores — not
   zeros, not nulls-rendered-as-zero.

---

## 9. Job semantics

Statuses: `PENDING`, `RUNNING`, `COMPLETED`, `REJECTED`, `FAILED`.

- `REJECTED` ≠ `FAILED`. Rejected is a policy refusal and must not be retried identically.
- Only `COMPLETED` exposes an artifact.
- Provenance is mandatory on every job.
- **Restartability (Failure Rule).** Jobs are in-memory in V1, so a restart loses the registry.
  Frozen behaviour: any job not `COMPLETED`/`REJECTED` at startup is reported `FAILED` with a
  reason. **Silent disappearance is prohibited.** .

`GET /api/v1/jobs/{jobId}` returns the same payload as
`GET /api/v1/reconstructions/{jobId}` while reconstruction is the only job type.

---

## 10. Security posture

| Control | V1 state |
|---|---|
| Authentication | **Required for all destructive operations.** The boundary (`AuthenticationService`) is real and enforced; the implementation behind it is a development stub . |
| Authorization | **Required and distinct from authentication.** Capability checks (`AuthorizationService`) are enforced; the policy implementation is a development stub . |
| Confirmation | **Required and enforced at the application layer** for delete and purge. Not a UI dialog. |
| Reversibility | Deletion is reversible. Only `purge` destroys, and only from the deleted state. |
| Transport | Plain HTTP on loopback. No TLS. |
| Binding | `127.0.0.1`, set by `server.address` in `application.properties` and verified by test. A deployment restriction, not a security control. |
| Secrets in responses | Structurally impossible — no field can hold one |
| Secrets in logs | Prohibited; asserted by tests |
| Request logging | Path, method, status, duration. **Never** body content, query text or file content. The `q` parameter of `GET /api/v1/search` is redacted before any path is logged |

**The honest statement.** Loopback binding is a **deployment restriction, not the security
model**. Authentication, authorization and confirmation are enforced by the application layer and
apply on every interface; the design stays correct if the binding changes.

What is deferred is the *implementation* behind the authentication and authorization boundaries:
both are development stubs, so this build must not be treated as securely authenticated. The
contracts, capability separation, confirmation enforcement and reversible lifecycle are real and
tested now, replaces implementations without redesigning the deletion architecture.

Transport is still plain HTTP with no TLS, which remains a genuine reason not to serve untrusted
networks . This is recorded as a limitation, not a warning buried in code.

---

## 11. Explicitly out of scope

Deferred, to prevent accidental scope creep: authentication, authorization, TLS, rate limiting,
pagination, ETag/caching, HATEOAS links, bulk operations, job cancellation, webhooks/callbacks,
WebSocket or SSE progress streaming, an OpenAPI-generated client, and API keys.

None of these are needed by the API acceptance criteria.

---

### Accepted risks

One decision above still increases exposure and is recorded so it is not lost:

- **Search query text in URLs.** A `GET` alias places query text in access logs, browser history
  and `Referer` headers. SFS search text may contain sensitive terms. Mitigated by excluding the
  query from application request logging; not mitigated in any external proxy log.

### Deliberately deferred

- **Authentication and authorization implementations.** The boundaries, capability separation and
  enforcement are real and tested; the implementations behind them are development stubs until
  M13. This build must not be described as securely authenticated. It is, however, not
  architecturally defined as unauthenticated: the guarded path exists and is enforced.
