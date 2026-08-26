# Deletion and Destructive-Operation Design

Application & API Layer
**Status:** Implemented and tested.

This document records the design of raw-data deletion in SFS: the lifecycle, the four controls
that guard it, and the reasoning behind the decisions that are not obvious from the code.

---

## 1. Why this needed a design

UI exposed deletion as a button in a locally served web page.API exposes
every operation over HTTP so the system is usable without the UI. That changes the risk profile
of exactly one operation: releasing raw bytes is irreversible, and an API turns it into something
any HTTP client can invoke.

Four properties are therefore required of destructive operations:

1. **Authentication** — the caller's identity must be established.
2. **Authorization** — that identity must hold the specific capability.
3. **Explicit confirmation** — validated at the application boundary, not in a browser dialog.
4. **Reversible by design** — ordinary deletion must not destroy anything.

None of these may rest on network binding. A deployment restriction reduces exposure; it does not
authenticate anyone, and a design that depends on it stops being correct the moment the binding
changes.

---

## 2. The lifecycle question

SFS already has a state in which raw bytes are gone: **`MEMORIZED`**.

`MEMORIZED` is not a deletion feature. It is the product concept — raw bytes released, meaning
retained, record still searchable and reconstructable. It is also **irreversible by design**,
because SFS reconstruction is semantic and approximate rather than byte-exact.

This creates a tension with requirement 4. Two wrong answers were available:

- **Make `MEMORIZED` reversible.** This would mean secretly retaining the raw bytes that
  memorization exists to release, contradicting the V1 scope lock and falsifying the central
  claim of the system.
- **Add a separate `PURGED` state.** This would leave *two* states both meaning "raw bytes are
  gone", which is precisely the kind of contradictory duplicate path that makes a lifecycle
  unmaintainable.

The resolution is that the required terminal state — *raw data permanently removed, Semantic DNA
retained* — **is the existing definition of `MEMORIZED`**. It needed no new name. Only one
genuinely new state was required.

---

## 3. The lifecycle

```
ANALYZED
   |
   | delete   (authenticated + authorized + confirmed)
   v
SOFT_DELETED               raw bytes RETAINED, withdrawn from normal use, fully reversible
   |
   +-- undo   (authenticated + authorized) --------> ANALYZED
   |
   +-- purge  (authenticated + authorized + confirmed)
                   |
                   v
              MEMORIZED    raw bytes RELEASED, Semantic Record survives, irreversible
```

| State | Raw bytes | Reversible | Produced by |
|---|---|---|---|
| `ANALYZED` | Present | — | Analysis |
| `SOFT_DELETED` | **Present** | **Yes** | Ordinary deletion |
| `MEMORIZED` | Released | **No** | Purge |

Three properties follow from the shape:

- **Deletion destroys nothing.** It withdraws the object; the bytes stay.
- **Purge is reachable only from `SOFT_DELETED`.** No single call takes a live file to permanent
  destruction: it always takes two deliberate, separately authorized steps.
- **The pre-existing safety rule is unchanged.** Deletion is permitted only from `ANALYZED`, so
  raw bytes never start down the path toward release before a validated Semantic Record exists.

---

## 4. The four controls

Enforced in a fixed order in `FileApplicationService`: authenticate, authorize, confirm, then
validate the lifecycle. The order matters — a caller learns "you are not permitted" before
learning anything about whether the object exists or what state it is in.

### 4.1 Authentication

`AuthenticationService` resolves an opaque credential to a `Principal`, or to nothing. There is
no anonymous-principal fallback: code holding a `Principal` may assume identity was established,
and code without one must refuse.

The credential is opaque so a later implementation can accept a bearer token, session identifier
or signed assertion without changing the contract or any caller. It travels in the
`X-SFS-Credential` header — not a body field that might be logged, not a query parameter that
would reach access logs and browser history.

`Principal` carries **no credential**: no password, token, secret or key. It is the *result* of
authentication, not a means of performing it, so it can be logged, passed between layers and
recorded in an audit entry without risk of disclosure. A reflection test enforces this.

### 4.2 Authorization

`AuthorizationService` answers a separate question from authentication. Treating any
authenticated caller as permitted is the specific mistake this split exists to prevent.

Capabilities are a small closed set rather than a role system:

| Capability | Grants |
|---|---|
| `READ` | Read files, records, jobs, reports |
| `WRITE` | Import and request analysis |
| `DELETE_RAW` | Reversible deletion |
| `UNDO_DELETE` | Restore |
| `PURGE_RAW` | Permanent release of raw bytes |

The three destructive capabilities are deliberately separate. **Holding `DELETE_RAW` does not
confer `PURGE_RAW`**: reversible withdrawal and permanent destruction are different levels of
trust, and collapsing them would mean anyone able to tidy up files could also destroy them
irrecoverably.

### 4.3 Explicit confirmation

Confirmation is **object-bound**, not a flag:

```json
{ "confirmObjectId": "sfs-obj-0001-a1b2c3d4" }
```

A bare `{"confirm": true}` was rejected as a design. It is equally true for every object, so a
request body copied from one call and replayed against a different Object ID still satisfies it.
Requiring the caller to name the object means confirmation cannot be satisfied by accident, by a
replayed body, or by a client that sets a flag once and forgets it.

Confirmation is validated at the application boundary. A browser dialog is a convenience for
humans and is absent entirely when the API is called directly, so it is not a control.

**Restore requires no confirmation.** It destroys nothing, and demanding confirmation for safe
corrective actions trains callers to confirm reflexively — which weakens confirmation where it
actually matters.

### 4.4 Lifecycle validation

Every transition is checked against `FileStatus`. Invalid transitions are refused with
`409 INVALID_STATE_TRANSITION`: purging a live object, restoring one that was never deleted,
deleting twice, purging twice, or restoring after purge.

---

## 5. API surface

| Operation | Method and path | Capability | Confirmation | From state |
|---|---|---|---|---|
| Delete | `DELETE /api/v1/files/{objectId}` | `DELETE_RAW` | Required | `ANALYZED` |
| Restore | `POST /api/v1/files/{objectId}/undo-delete` | `UNDO_DELETE` | Not required | `SOFT_DELETED` |
| Purge | `POST /api/v1/files/{objectId}/purge` | `PURGE_RAW` | Required | `SOFT_DELETED` |

Failure codes: `401 AUTHENTICATION_REQUIRED`, `403 NOT_PERMITTED`,
`400 CONFIRMATION_REQUIRED`, `400 CONFIRMATION_MISMATCH`, `409 INVALID_STATE_TRANSITION`.

**Purge is not exposed in the UI.** Irreversible destruction requires a distinct capability and
an explicit object-bound confirmation; adding a button for it would invite exactly the accidental
one-click destruction this design removes. Deletion and restore are available in the UI.

---

## 6. Network binding

`server.address` defaults to `127.0.0.1`, set in `application.properties` and verified by a test
that reads the resolved configuration rather than the documentation.

**This is a deployment restriction, not the security model.** Authentication, authorization and
confirmation are enforced by the application layer and apply on every interface. Binding reduces
the exposed surface during development; it authenticates nobody, and the architecture stays
correct if the binding changes.

Transport remains plain HTTP with no TLS, which is a genuine reason not to serve untrusted
networks.

---
**enforced and tested now:**

- The lifecycle, including reversibility and two-step destruction.
- The authentication and authorization *boundaries*, and their enforcement in the application
  layer.
- Capability separation, including `DELETE_RAW` not implying `PURGE_RAW`.
- Object-bound confirmation.
- Loopback binding.

**Development stubs **

- The *implementations* behind `AuthenticationService` and `AuthorizationService`. The mock maps
  a caller-supplied role name to a capability set. It performs no cryptography, contacts no
  identity provider and verifies no secret. **No credential is hard-coded anywhere** — there is
  no password, token or API key in the codebase.

**Therefore:** this build must not be described as securely authenticated. What API guarantees is
that the API is not *architecturally* defined as unauthenticated deletion, replaces
implementations behind stable interfaces without redesigning the deletion architecture.
 durable storage for the soft-deleted state (M08), and a production
audit platform.
