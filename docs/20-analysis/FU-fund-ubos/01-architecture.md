---
id: ANA-FU-01
title: "FU Architecture"
status: complete
phase: P1
depends_on: [INV-FU, ANA-UP-01]
sources: [FU, UP]
---

# FU — Architecture

Source path prefix: `backend/src/main/java/org/logrum/ubos/` (written in full in references).

## Relation to UP (fact)

28 Java files are byte-identical to `UP` (engine, bootstrap, original controllers, models,
repositories, `LcmRelationService`, `ReactiveRetry`). Everything described in ANA-UP-01…06
for those files applies unchanged and is **not repeated here**. Changed: `LcmKernelService`
(266 → 1,225 lines), `LcmAuditService` (62 → 279), `ScriptSecurityConfig` (whitespace only).
New: 61 files (governance services, URI utilities, console API, security filters, DTOs).

## Layer map

| Layer | Package | Content | New vs UP |
|---|---|---|---|
| Kernel core | `kernel.service.LcmKernelService` | Commit, branch resolution, tenant-aware commit/lookup, search, **merge, revert, create branch, rename, copy, branch diff, draft commit**, snapshot cache | extended |
| Kernel governance | `kernel.service.*` | Approval, Authz, User/Group/Policy, API keys, Webhooks, Environments, Stash, Merge-conflict, History (blame), Metrics, Resource resolver | new |
| Kernel utils | `kernel.util.*` | `UbosUriUtil`, `JsonSchemaValidator` | new |
| Engine | `engine.*` | Groovy executor, events, scheduler | = UP |
| Server (legacy API) | `server.controller.*` | boot/run/dev/view/audit/auth/admin | = UP |
| Server (terminal) | `server.controller.WebConsoleController`, `console.CommandParser` | Text command console | new |
| Web (Studio API) | `web.console.*` | `/api/console/**`, `/api/v1/history`, `/api/v1/stash`, `/merge/conflicts` | new |
| Web security | `web.security.*` | API-key filter, localhost-only console filter | new |
| Client | `frontend/` | "UBOS Studio" IDE + `public/console.html` terminal | new |

### CON-FU-001 — Governance layer built *on top of* the versioned kernel
- **What:** All governance services (approvals, users/groups/policies, API keys, webhooks, schemas) persist their state by calling `LcmKernelService.commit/getResourceSnapshot/search` — they have no tables of their own.
- **How:**
  - Each service owns an entity type constant (`APPROVAL_REQUEST`, `SECURITY_KEY`, `WEBHOOK`, `USER`, `GROUP`, `POLICY`, `SCHEMA`) and stores a JSON payload record on branch `master`.
  - Circular dependency kernel ↔ services is broken by setter injection in `LcmKernelService.init()` (`@PostConstruct`: `schemaValidator.setSchemaFetcher(this::getResourceSnapshot)`, `apiKeyService.setKernelService(this)`, …).
  - Only one new physical table was added (`sys_environment_config`, CON-FU-043).
- **Why (intent):** The kernel is its own database: governance data gains history, audit, branching and time travel "for free".
- **Sources:** [FU:backend/src/main/java/org/logrum/ubos/kernel/service/LcmKernelService.java#L66-L76], [FU:backend/src/main/java/org/logrum/ubos/kernel/service/LcmApprovalService.java], [FU:backend/src/main/java/org/logrum/ubos/kernel/service/LcmUserService.java].
- **Tags:** metadata, permission, versioning

### CON-FU-002 — Three API families over one kernel
- **What:** The same kernel is exposed through (1) the UP generic API, (2) a REST "console" API for the Studio, (3) a text command terminal.
- **How:**

| Family | Prefix | Consumer |
|---|---|---|
| Generic runtime | `/api/boot`, `/api/run`, `/api/dev`, `/api/view`, `/api/audit`, `/api/auth` | apps, copilot (UP) |
| Studio REST | `/api/console/*` (34 endpoints), `/api/v1/history/{id}/blame` (NDJSON), `/api/v1/stash`, `/merge/conflicts` | UBOS Studio (RTK Query) |
| Terminal | `POST /api/console/exec` | `public/console.html` |

- **Why (intent):** Separate *runtime* traffic from *operator/developer* tooling.
- **Sources:** [FU:backend/src/main/java/org/logrum/ubos/web/console/LcmConsoleController.java], [FU:backend/src/main/java/org/logrum/ubos/server/controller/WebConsoleController.java].
- **Tags:** api, cli

### CON-FU-003 — Access boundary filters
- **What:** Two web filters form the outer boundary: API-key authentication with scopes, and localhost-only access to the console.
- **How:**
  - `ApiKeySecurityFilter` (header `X-API-KEY`): public paths (`/actuator/health`, `/api/health`, swagger) skip; no key → pass to other auth; valid key must hold the required scope: `EXTERNAL_COMMIT` for `/api/external/**`, `FULL_ACCESS` for POST/PUT/DELETE/PATCH, else `READ_ONLY`; the key payload is stored as exchange attribute.
  - `LocalhostSecurityFilter`: `/api/console/**` only from 127.0.0.1 / ::1.
- **Why (intent):** Machine-to-machine integration via scoped keys; operator console restricted to the host.
- **Sources:** [FU:backend/src/main/java/org/logrum/ubos/web/security/ApiKeySecurityFilter.java], [FU:backend/src/main/java/org/logrum/ubos/web/security/LocalhostSecurityFilter.java].
- **Tags:** auth, api, integration

## Key flows

### Flow A — Governed batch commit
1. `POST /api/console/batch-commit {entityType, branch, slugs[], jsonPatch, message}`.
2. For each slug: build URI `ubos://{type}/{slug}?branch=` → `LcmAuthzService.isPermitted(user, COMMIT, uri)`; any denial → 403.
3. `startProcess("Console Batch Update")` → commit each slug under the process id (schema validation per commit).
4. Response: "Processed N entities (ProcessID …)".

### Flow B — Approval-gated change
1. `POST /api/console/approval/request {targetUri, content}` → `APPROVAL_REQUEST/REQ-XXXXXXXX` committed with status `PENDING`.
2. `POST /api/console/approval/approve {requestId, approver, action}` → approve: commit `originalPayload` to `targetUri` (author = approver, message suffixed "(Approved by …)"), then commit the request with `APPROVED` + `targetCommitId`; reject: commit `REJECTED` + reason.

### Flow C — Merge
1. `POST /api/console/merge {sourceBranch, targetBranch, type, slugs[]}` → authz `MERGE` per slug on target.
2. Per slug: source snapshot (with inheritance) + target head → deep merge (source wins) → unchanged → skipped; else validate against schema → commit on target.
3. Aggregate `{mergedCount, skippedCount, failedCount, failures[]}`.
