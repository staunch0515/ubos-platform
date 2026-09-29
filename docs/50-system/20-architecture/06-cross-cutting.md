---
id: UBS-ARC-06
title: Cross-Cutting Concerns
status: draft
phase: ALL
depends_on: [UBS-ARC-01, UBS-ARC-04, UBS-ARC-05]
---

# Cross-Cutting Concerns

## 1. Security architecture

### 1.1 Trust boundaries

| Boundary | Between | Controls |
|---|---|---|
| B1 Client edge | clients ↔ NOD | TLS 1.3, authentication (OIDC, SAML, passkeys, API keys), session policies, rate limits |
| B2 Kernel boundary | NOD ↔ DVM | attested execution context (AR-007); the DVM re-authorises every instruction |
| B3 Logic sandbox | DVM ↔ logic assets | instruction-only effects, profiles, static analysis, uncatchable termination |
| B4 Tenant boundary | tenant ↔ tenant | row-level security, per-tenant keys, isolated caches and indexes, quotas |
| B5 Egress | platform ↔ external systems | egress allow-list, classification checks, recorded exchanges |
| B6 Operator boundary | operators and CTL ↔ tenant data | no business APIs for CTL (AR-006); break-glass with dual control and audit |
| B7 Federation | organisation ↔ organisation | DID-bound mTLS, trust relationships, signature verification |
| B8 Device | device ↔ server | device keys, clearance scopes, remote wipe |

### 1.2 Enforcement points

| Enforcement point | Checks | Decision source |
|---|---|---|
| UBTP handler | authentication, session, scopes, rate limits | NOD |
| Instruction | policy decision (RBAC, ABAC, ReBAC), branch rights, write policy | DVM policy engine (Cedar-class) |
| Query executor | row filters, masking | DVM policy engine |
| View resolver | field visibility, action availability | DVM |
| Egress channels (UBS-ARC-04 §7) | classification and masking | DVM policy engine |
| Admin endpoint | operator roles, dual control for break-glass | NOD and CTL |

### AR-024 — One policy engine
- **Rule:** All authorization decisions for business resources MUST be made by the DVM policy engine over one policy set per VAE (roles, attributes, relations, conditions). Systems MUST NOT embed separate authorization logic for business resources.
- **Realises:** FR-IAM-041, FR-IAM-042, FR-IAM-081
- **Rationale:** Analysable, explainable, reviewable access.

### AR-025 — Break-glass access
- **Rule:** Emergency operator access to tenant data MUST require dual approval, time-boxing, full session recording and tenant notification. It MUST be impossible for CTL service credentials (AR-006).
- **Realises:** CR-SOC2-002, CST-017
- **Rationale:** Support needs without silent access.

## 2. Identity and keys

| Principal kind | Authentication | Signing key location |
|---|---|---|
| human | OIDC or SAML SSO, passkeys, TOTP (local) | device key store or platform key service (per policy) |
| service (BRG, NTY, AGT per tenant) | mTLS client certificate plus service token | KMS or HSM |
| agent | issued by AGT, bound to its agent definition | KMS or HSM (server-side signing on behalf, co-signed by node) |
| external | e-mail verification, SSO federation, passkeys | platform key service (seal signing) |
| node | mTLS certificate bound to its DID | HSM or KMS (Server option, Cell mandatory) |

## 3. Error model

### AR-026 — Uniform error object
- **Rule:** Every system MUST return errors as:
  - `code` (`<SYS or DOM>.<UPPER_SNAKE>`, registered in the error registry of the standard);
  - `message` (template key and parameters);
  - `explanation[]` (FR-AUD-031);
  - `retryable` (boolean);
  - `correlation` (trace and process IDs).

  Codes are never reused with a different meaning. Renaming a code is a breaking change.
- **Realises:** FR-AUD-031, FR-INT-023, NR-USE-006
- **Rationale:** Predictable handling by clients, agents and operators.

| Error class | Retryable | Examples |
|---|---|---|
| Validation | no | `MODEL.CONSTRAINT_VIOLATED`, `RULE.DECLARATION_UNSATISFIED` |
| Concurrency | yes (after refresh) | `TXN.HEAD_MOVED`, `TXN.READ_SET_CHANGED` |
| Authorization | no | `IAM.DENIED`, `IAM.NOT_FOUND` |
| Limits | no (for the same input) | `LOGIC.LIMIT_EXCEEDED`, `AI.BUDGET_EXHAUSTED`, `OPS.QUOTA_EXCEEDED` |
| Availability | yes | `NOD.STORAGE_UNAVAILABLE`, `LOGIC.CONNECTOR_UNAVAILABLE`, `SYNC.UPSTREAM_UNAVAILABLE` |
| Integrity | no (investigate) | `PROOF.SIGNATURE_INVALID`, `SYNC.INTEGRITY_FAILURE` |

## 4. Observability

| Signal | Content | Rule |
|---|---|---|
| Traces | spans per UBTP request, process, instruction family, flush, job, connector call, model call | carry trace ID, process ID, tenant hash (not name), VAE hash |
| Metrics | RED per API; saturation per store, queue, projection lag, sync lag, anchoring lag, gateway latency and cost | documented catalogue (NR-OBS-002) |
| Logs | structured JSON, no business values | secret scanning in CI (NR-SEC-009) |
| Security events | hash-chained log plus SIEM export | FR-AUD-012, FR-AUD-013 |
| Business audit | from commits and processes, not logs | FR-AUD-011 |

## 5. Configuration

| Level | Stored as | Governed by |
|---|---|---|
| Node configuration | declarative files or CTL-applied documents, versioned | FR-OPS-091 |
| Tenant and VAE configuration | versioned Definition objects | change sets (FR-TEN-054) |
| Buk configuration | versioned objects declared by Buks | install plan (FR-PKG-034) |
| Feature entitlements | plan and licence objects | CTL and EXC (FR-BILL-021) |

### AR-027 — Configuration that affects business behaviour is business data
- **Rule:** Any configuration that changes business outcomes (time zone, base currency, calendars, rule parameters, retention) MUST be a versioned, governed object in the VAE, not a node setting.
- **Realises:** FR-TEN-054, FR-TIME-061
- **Rationale:** It must be auditable and reproducible like rules.

## 6. Multi-tenancy summary

| Mechanism | Where |
|---|---|
| Tenant in every key and row-level security | authoritative store (AR-017) |
| Per-tenant keys | encryption (UBS-ARC-04 §5) |
| Per-tenant quotas and fairness | NOD schedulers and rate limiters (FR-TEN-024) |
| Per-tenant service principals | platform services (AR-009) |
| Placement and migration | CTL (FR-TEN-071, FR-TEN-072) |

## 7. Performance strategy

| Technique | Purpose | Correctness guard |
|---|---|---|
| Kernel indexes maintained in the flush | O(1) head reads, indexed bitemporal reads | pure function of authoritative state (AR-016) |
| Effective-class and effective-sheet caches | avoid recomputing inheritance and cascades | invalidated by commits touching the chain (FR-MODEL-025) |
| Per-branch group commit | amortise flush cost under contention | CAS per object, and commit order preserved |
| Prolly-tree roots | incremental root update O(changes × log n); fast diffs | history independence (NR-DET-003) |
| Chunk deduplication and compression | storage cost | content hashes over the stored form (AR-020) |
| Async projections and indexes | heavy derived data off the commit path | watermarks (FR-LEDG-033) |
| Read replicas | history and analytics reads | watermarked, never for CAS bases (AR-013) |
| Precompiled logic caches | low invocation overhead | keyed by asset version hash |

### AR-028 — Correctness before caching
- **Rule:** Every cache MUST be keyed by content hashes or commit IDs, or invalidated by commit events. A cache miss or a cache loss MUST NEVER change results.
- **Realises:** NR-DET-001, FR-EVT-032
- **Rationale:** Determinism.

## 8. Resilience patterns

| Pattern | Applied to |
|---|---|
| Idempotent retries with backoff and jitter | all contracts that change state (AR-010) |
| Circuit breakers | connectors, model providers, TSAs, notification providers |
| Bulkheads | per-tenant worker pools, per-connector concurrency |
| Leases with fencing tokens | schedule leadership, job leases, migration cut-over |
| Graceful degradation | optional services (NR-AVAIL-005) |
| Read-only mode | storage write outage (NR-AVAIL-006) |

## 9. AI governance chain

| Control | Where |
|---|---|
| Agent is a principal with scope and budget | IAM and AGT (FR-AI-021) |
| Agent writes only on its branch | DVM branch policy (FR-AI-031) |
| Model access only through the gateway with classification enforcement | AGT (FR-AI-011, FR-AI-013) |
| Every model call and tool call recorded | journal and AGT logs (FR-AI-014, FR-AI-044) |
| Human or policy approval for merges | DVM change sets (FR-AI-025) |
| Revert by run or item | DVM (FR-AI-035) |
| Evaluation gates for agent-definition changes | AGT (FR-AI-081) |

### AR-029 — AI output is untrusted input
- **Rule:** Every output of a model (answers, tool arguments, proposed values, generated artefacts) MUST be treated as untrusted user input. It MUST be validated, authorised and approved like any other input before it affects authoritative state.
- **Realises:** FR-AI-094, FR-AI-062
- **Rationale:** Principle P3.

## 10. Evolution and compatibility

| Surface | Versioning | Compatibility rule |
|---|---|---|
| BPA standard | `MAJOR.MINOR.PATCH` | FR-STD-041 |
| Instruction ABI | `MAJOR.MINOR` | FR-LOGIC-012 |
| UBTP | `MAJOR.MINOR` with negotiation | NR-COMPAT-002 |
| Storage format | internal version with online migration | FR-OPS-032 |
| Export format | versioned with the standard | FR-STD-042 |
| Widget catalogue | `MAJOR.MINOR` | FR-UX-014 |
| Buks | semantic versions | CAP-PKG-02 |
| Classes | schema versions with lenses | CAP-MODEL-08 |

### AR-030 — Additive by default
- **Rule:** Changes to every versioned surface in section 10 MUST be additive within a major version. Anything else requires a major version and a migration path verified by conformance or scenario suites.
- **Realises:** NR-COMPAT-001 … NR-COMPAT-004
- **Rationale:** Long-lived customer investments.
