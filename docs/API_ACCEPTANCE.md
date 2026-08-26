**Application & API Layer**
**Build:** `com.sfs:sfs-parent:0.1.0-SNAPSHOT`
---

### Definition of Exit

| # | Requirement | Status | Evidence |
|---|---|---|---|
| 1 | Automated tests pass from a clean build | **Met** | **585 tests, 0 failures**, verified . |
| 2 | Contracts versioned and documented | **Met** | `API_SPECIFICATION.md` (frozen, machine-enforced by `ApiContractFreezeTest`), `CONTRACTS.md`,        `DELETION_DESIGN.md`, `GET /api/v1/version`. |
| 3 | Observability captures failures and performance | **Partially met** | Failures logged with status and path; **still no timing or metrics**. |

---

## 3. Test results

```
mvn -B clean install
sfs-core   :  37 tests
sfs-app    : 176 tests
sfs-ui     : 372 tests
TOTAL      : 585 tests, 0 failures, 0 errors
BUILD SUCCESS
```

Growth

| Suite | Tests | Purpose |
|---|---:|---|
| `ApiContractFreezeTest` | 18 | Parses the spec and fails if the frozen contract drifts |
| `RequestValidationTest` | 16 | Request DTO validation at the boundary |
| `ResponseSafetyTest` | 11 | Reflection guards against leaking values or aggregate scores |
| `SecurityContractTest` | 14 | `Principal` carries no credential; capability separation |
| `ApiErrorResponseTest` | 47 | Error model completeness and determinism |
| `FileApplicationServiceTest` | 34 | Authentication, authorization, confirmation, lifecycle |
| Other application services | 45 | Orchestration and guards |
| `JobRegistryTest` | 8 | Job tracking and orphan handling |
| `ApiErrorHandlingTest` | 12 | Live HTTP error behaviour |
| `DeletionSecurityApiTest` | 18 | **Destructive operations over HTTP** |
| `ApplicationacceptanceTest` | 14 | **application acceptance, server** |

---

##  Defects found by live testing

Three issues passed compilation and unit tests, and were exposed only by exercising a running
server. They are recorded because each one is a class of bug the test suite could not see.

1. **`405` on an API path returned the HTML error page.** A JSON client asking for the wrong
   method received a full HTML document. Fixed by routing `/api/**` errors to a JSON responder,
   while browser paths keep the styled HTML page.
2. **A refused reconstruction returned `201 Created`.** The frozen spec says `422`. Returning
   "Created" for a policy refusal is the exact confusion between *refused* and *failed* the
   contract was written to prevent.
3. **Loopback binding was documented but never configured.** `API_SPECIFICATION.md` stated
   `Binding: 127.0.0.1 by default`; `application.properties` contained no `server.address`, so
   the application bound to every interface. Now set, and verified by a test that reads the
   resolved configuration rather than the prose.

The third is the most instructive: documentation asserted a control that did not exist. Tests now
verify configuration, not claims.

---

## state of each subsystem

The API exposes mocks.

| Subsystem |
|---|---|
| **Search** | Keyword substring over 4 fixtures. `meeting minutes` returns **0 results** despite `meeting-notes.txt` existing. |
| **Reconstruction** | Formats DNA fixtures; generates no language. Synchronous behind an async-shaped API. |
| **Evaluation** | Fixed constants. Measures nothing. |
| **Security policy view** | `GET /api/v1/security/settings` reports intended configuration and returns `"enforced": false`. | 
| **Authentication / authorization** | **Boundaries are real and enforced.** Implementations are development stubs with no credential of any kind in the codebase. |
| **Storage** | In-memory. **All data lost on restart**, including soft-deleted state and the job registry. |

---

## V1 scope compliance

| Constraint | Status |
|---|---|
| Text files only | **Held** — non-UTF-8 upload rejected with `415 UNSUPPORTED_MEDIA_TYPE` |
| No new adapters | **Held** |
| No physical semantic filesystem | **Held** |
| No mandatory large LLM | **Held** — no model dependency |
| Byte-exact recovery not claimed | **Held** — artifact keeps the `NOT THE ORIGINAL FILE` banner; purge is documented as irreversible for this reason |
| "Reconstruction Rules" terminology | **Held** |
| No deferred feature added | **Held** — no identity provider, OAuth, RBAC engine, TLS, audit platform, pagination or cancellation |

**Dependencies:** one change
`spring-boot-starter-web` to `spring-boot-starter-webmvc`. **No new third-party dependency was
added.** proved unnecessary (Jackson already transitive);  were declined.

---

##  Known gaps and risks

| # | Item | Severity | Note |
|---|---|---|---|
| 1 | **Authentication/authorization implementations are stubs** | **Medium** | The boundaries, capability separation and enforcement are real and tested. What M13 supplies is the implementation behind the interfaces. This build must not be described as securely authenticated. |
| 2 | No TLS | Medium | Plain HTTP. A genuine reason not to serve untrusted networks  |
| 3 | **No performance measurement** | Medium | Second milestone with this gap. Failures are logged; timings are not. |
| 4 | Soft-deleted state is in-memory | Expected | A restart loses it, like all V1 storage. |
| 5 | Job registry is in-memory | Expected | Non-terminal jobs are marked `FAILED` at startup with an explicit reason — never silently dropped. |
| 6 | Search query text in URLs | Low | The `GET ?q=` alias places query text in access logs and history. Excluded from application logging; external proxies are not covered. |
| 7 | UI read paths still call contracts directly | Low | Deliberate; see `API_PLAN.md` §10. Destructive UI actions *do* go through the guarded application layer. |
| 8 | No pagination | Low | `GET /api/v1/files` returns everything. Fine at fixture scale. |

---

