# — Application & API Layer: Plan

 Application & API Layer


---

## 1. What API LAYER  changes

UI built screens against contracts, with controllers calling mock services directly:

```
Controllers ──▶ Contracts ──▶ Mock services
```

API inserts the layer the specification requires, and makes every operation callable
**without the UI**:

```
UI Controllers ─┐
                ├──▶ Application Services ──▶ Contracts ──▶ Mocks ( replaces with  subsystems)
REST API       ─┘         │
                          ├── authentication boundary
                          ├── authorization boundary
                          ├── confirmation validation
                          └── lifecycle validation
```

Three consequences worth stating plainly:

1. **The REST API is the new product surface.** "Every required V1 operation can be called
   without the UI" means a real HTTP/JSON API, not an internal interface.
2. **Orchestration moves out of controllers.** Today `ObjectController` injects two services and
   combines them — modest, but exactly the domain logic API forbids in controllers.
3. **Exposing write operations over HTTP raises the stakes on deletion.** In UI the only way to
   delete was a button in a local browser. An API turns that into something any HTTP client can
   invoke. The security model for destructive operations therefore has to be settled *API*.

---

## 2. Phase plan

 | Title | Deliverable |
|---|---|---|
|Module skeleton + `ObjectId` value object | `sfs-app` module; validated `ObjectId` |
|API contracts (operation freeze) | Written API specification; the frozen list of V1 operations, including the deletion lifecycle and security model |
|Request/response models (DTOs) | Request and response DTOs, the typed error model, and the confirmation contract |
|Service orchestration + security boundaries | Application services; authentication, authorization and capability contracts |
|Validation and error handling | REST controllers, deterministic error codes, safe messages, `@RestControllerAdvice` |
|Job/status API | Job registry, queryable status, restart/fail semantics |
|API integration tests + acceptance report | Full-stack API tests without the UI; M02 acceptance report |

Ordering rationale: the specification's own development sequence is *freeze operations → DTOs →
service interfaces → controllers → validation → job tracking → integration tests*.

Security is not a separate. It is part of the contract freeze, the DTOs and
the orchestration layer, because a security model bolted on after the endpoints exist
tends to leave the original unguarded path in place beside the new one.

---

## 3. Mapping to API acceptance 

|  |  | How it will be proven |
|---|---|---|
| Every required V1 operation callable without the UI || Integration tests issuing pure JSON HTTP calls, no HTML |
| Invalid inputs return deterministic errors | | Same bad input → same error code and shape, asserted |
| Long-running jobs can be queried || `GET /api/v1/jobs/{id}` returns status through the lifecycle |
| Protected values never appear in API logs | | Log-capturing tests asserting no secret material |
| No controller contains domain logic | | Extended `ArchitectureBoundaryTest` |
| Object IDs and file paths validated | | `ObjectId` rejects malformed input at construction |
| Errors typed and safe to expose |  | No stack trace, class name or internal path in any response |
| Destructive operations are guarded | | Authentication, authorization and confirmation each independently refuse; live HTTP tests |

---

## 4. Deletion and the security model

The single largest design decision.Its what makes deletion remotely reachable.

**Requirement.** Raw-data deletion must require authentication, authorization and explicit
confirmation, and must be reversible by design. None of those may rest on loopback binding,
a UI-only dialog, or an immediately irreversible operation.

**The lifecycle question.** SFS already has a state that removes raw bytes: `MEMORIZED`. It is
not a deletion feature — it is the product concept, and it is irreversible by design because
reconstruction is semantic and approximate rather than byte-exact. Making it reversible would
mean retaining the bytes it exists to release.

The resolution is that `MEMORIZED` **is** the permanent terminal state, so only one new state is
needed:

```
ANALYZED --delete--> SOFT_DELETED --purge--> MEMORIZED
                          |                      |
                          +--undo--> ANALYZED    +-- raw bytes released
                                                 +-- Semantic Record survives
```

Rationale is documented in full in `DELETION_DESIGN.md`. The consequences for this plan:

- Ordinary deletion is reversible and destroys nothing.
- Purge is a separate operation, reachable only from `SOFT_DELETED`, so no single call can take
  a live file to permanent destruction.
- `DELETE_RAW` and `PURGE_RAW` are distinct capabilities; holding one does not confer the other.
- Loopback binding is retained as a **deployment restriction**, not as a security control, and
  is actually configured rather than merely asserted in prose.

**Scope discipline.** API establishes the *boundaries*: `AuthenticationService`,
`AuthorizationService`, `Principal`, `Capability`. The implementations behind them are
development stubs. No identity provider, OAuth, RBAC engine, TLS or audit platform is built here
—. What matters is that the API is never architecturally defined as
"unauthenticated deletion".

---

## 5. Risks identified up front

| # | Risk | Proposal |
|---|---|---|
| 1 | **A write API is a bigger exposure than a local UI.** Anyone who can reach the port can import and delete. | Settle the four-part deletion model. Bind to loopback as a defence-in-depth restriction, never as the security mechanism. |
| 2 | Stub authentication could be mistaken for real authentication | State it plainly in the spec, the contracts document and the acceptance , and keep credentials out of the codebase entirely. |
| 3 | **Job restartability** conflicts with in-memory storage — a restart loses the registry. | Implement the state machine and mark orphaned jobs failed on startup.  |
| 4 | Double maintenance of UI and API paths | Both call the same application service. No logic in either controller type. |
| 5 | DTOs could leak protected values | Reuse the UI reflection-test approach against every response DTO. |
| 6 | Scope creep (File Lifecycle) | Application services orchestrate only; lifecycle rules stay in contracts. |

---
### Consequences of the module decision

`sfs-ui` currently injects contract services straight into controllers. With `sfs-app` in place,
UI controllers depend on application services for anything that must be guarded. The mock
services stay in `sfs-ui/mock/` and are wired into application services by Spring configuration;
moving them was considered and rejected as churn with no functional gain.

---

##  Design note — UI controllers and DTOs

The UI controllers continue to read from contracts directly rather than returning API DTOs.

**Reason.** The Thymeleaf templates bind to contract types (`dna.hasEmbedding`,
`evaluation.isAvailable`, `file.canAnalyze` and around twenty more). Migrating the read paths to
API DTOs would require rewriting every template expression and roughly a hundred passing tests,
for no functional gain: the M02 criterion is that operations are callable *without* the UI, which
the REST API satisfies.

**Where the UI does go through the application layer:** destructive operations. Deletion and
restore are guarded actions, so the UI calls the same application service the API does, and the
same authentication, authorization, confirmation and lifecycle checks apply. Purge is
deliberately not exposed in the UI at all.

**When to revisit.** When  replaces the mock services with real subsystems, the UI read paths
should migrate in the same task, so templates change once against real data rather than twice.
