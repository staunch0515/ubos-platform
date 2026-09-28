---
id: ANA-FU-03
title: "FU Runtime"
status: complete
phase: P1
depends_on: [ANA-FU-02, ANA-UP-03]
sources: [FU]
---

# FU — Runtime Mechanisms

Script execution, event bus, scheduler and genesis are identical to UP (CON-UP-020…030).
This file covers what FU adds: validation, governance workflows and Git operations.

## A. Validation

### CON-FU-020 — Schema-on-commit with schemas as versioned entities
- **What:** Every commit is validated against a JSON Schema stored as a `SCHEMA` entity whose slug is the target entity type.
- **How:**
  1. `commit()` step 0 → `JsonSchemaValidator.validate(type, slug, json)`.
  2. `SCHEMA` entities themselves are exempt ("to allow bootstrapping").
  3. Schema fetched from `SCHEMA/{TYPE}@master` via the kernel (with branch fallback); compiled as draft-07 (networknt); cached 5 minutes per type.
  4. No schema defined → validation passes (opt-in strictness).
  5. Committing a `SCHEMA` clears that type's cache entry.
  6. Merge results are validated before the merge commit.
  7. `POST /api/console/schema/commit` pre-compiles the schema and rejects invalid JSON Schema.
  8. Draft/stash commits bypass validation (`commitWithoutValidation`, "must never be used for real branches").
- **Why (intent):** Data contracts that evolve with history and branches like the data they govern.
- **Sources:** [FU:backend/src/main/java/org/logrum/ubos/kernel/util/JsonSchemaValidator.java], [FU:backend/src/main/java/org/logrum/ubos/kernel/service/LcmKernelService.java#L445-L491], [FU:backend/src/main/java/org/logrum/ubos/web/console/LcmConsoleController.java#L818-L875].
- **Tags:** schema, rules, versioning

## B. Governance workflows

### CON-FU-021 — Approval = deferred commit stored as a versioned request
- **What:** A change can be proposed against a target URI and executed only when approved.
- **How:**

| From | Event | To | Effect |
|---|---|---|---|
| – | request(targetUri, content, author, msg) | PENDING | commit `APPROVAL_REQUEST/REQ-xxxx` |
| PENDING | approve(approver) | APPROVED | commit `originalPayload` to `targetUri` as approver ("… (Approved by X)"), store `targetCommitId`, `resolvedAt` |
| PENDING | reject(approver, reason) | REJECTED | store `rejectionReason` |
| PENDING | cancel(requestedBy) | CANCELLED | only the original requester may cancel |
| non-PENDING | any | error | "Request is not pending" |

  Every state transition is itself a commit, so the request's history is its audit log.
- **Why (intent):** Four-eyes principle over any resource, expressed purely with kernel primitives.
- **Sources:** [FU:backend/src/main/java/org/logrum/ubos/kernel/service/LcmApprovalService.java], [FU:backend/src/main/java/org/logrum/ubos/web/console/dto/ApprovalRequestPayload.java].
- **Tags:** workflow, state-machine, permission, versioning

### CON-FU-022 — Authorization: roles + policy rules over URIs
- **What:** A two-stage authorization check: role grants the action, then policies may restrict it per resource.
- **How:**
  - Actions: `COMMIT`, `REVERT`, `MERGE`, `READ`, `APPROVE`, `DELETE`, `CREATE_BRANCH`.
  - Roles → actions: `ADMIN` all; `EDITOR` commit/revert/merge/read; `VIEWER` read; `APPROVER` read/approve.
  - User → groups (`USER.groups[]`); group → `roles[]` + `policies[]` (+ `parentGroup`, not evaluated).
  - Evaluation `isPermitted(user, action, uri)`: user disabled → deny; no role grants action → deny; collect enabled policies of all groups, sort by `priority` desc; for each rule matching action (`*` allowed) and resource (glob over the URI string) and conditions (`branches`, `entityTypes`; `timeWindow` declared): `DENY` → immediate deny; `ALLOW` → remember; no matching rule → allow (role decides).
  - Bootstrap mode: if the user is unknown **and no users exist**, allow.
  - Applied before batch commit and merge, per slug (any denial → HTTP 403).
- **Why (intent):** AWS-IAM-like policies whose subjects are `ubos://` resources, stored as versioned data.
- **Sources:** [FU:backend/src/main/java/org/logrum/ubos/kernel/service/LcmAuthzService.java], [FU:backend/src/main/java/org/logrum/ubos/web/console/dto/PolicyPayload.java], [FU:backend/src/main/java/org/logrum/ubos/web/console/dto/GroupPayload.java].
- **Tags:** permission, auth, org-model

### CON-FU-023 — API keys as versioned secrets
- **What:** Integration credentials are `SECURITY_KEY` entities storing only a hash.
- **How:** Generate 32 random bytes → `ubos_<base64url>`; store SHA-256 hex; plaintext returned once; scope (`READ_ONLY` default, `FULL_ACCESS`, `EXTERNAL_COMMIT`); `expiresAt`; revoke = commit with `active=false` + cache eviction; validation searches keys by hash, 5-minute cache.
- **Why (intent):** Key lifecycle (create, revoke, expire) is audited in the version chain.
- **Sources:** [FU:backend/src/main/java/org/logrum/ubos/kernel/service/ApiKeyService.java].
- **Tags:** auth, integration

### CON-FU-024 — Outbound webhooks as versioned configuration
- **What:** `WEBHOOK` entities subscribe external URLs to kernel events filtered by entity type.
- **How:** Match `triggerEvent` + `entityType`; POST JSON `{event, timestamp, entityType, slug, branch, commitId, author, message}`; headers `X-UBOS-Event`, `X-UBOS-Hook`, `X-UBOS-Signature: sha256=<HMAC-SHA256(secretToken)>`; per-hook `timeoutMs`; retry with backoff `retryCount` times on 5xx/429/network errors; failures swallowed (fire-and-forget on bounded-elastic). `sendWebhookAsync` exists but no commit path calls it yet.
- **Why (intent):** Integrate external systems with signed, retried notifications configured as data.
- **Sources:** [FU:backend/src/main/java/org/logrum/ubos/kernel/service/WebhookService.java], [FU:backend/src/main/java/org/logrum/ubos/web/console/dto/WebhookEntityPayload.java].
- **Tags:** integration, event

## C. Git operations on business data

### CON-FU-025 — Merge (per entity, source-wins deep merge)
- **What:** Merge copies changes of selected entities from a source branch into a target branch.
- **How:** Source snapshot resolved *with branch inheritance*; target head (or `{}`); merge rules: empty target → source; empty source → target; arrays → source replaces; objects → recursive deep merge, source wins on conflicts, target-only keys kept; merged == target → "skipped"; validate merged JSON; commit to target with message `Merge 'src' into 'tgt' for type/slug`. Batch returns merged/skipped/failed lists. Commit `c76fa02` "merger success".
- **Why (intent):** Promote work between branches (e.g. region → master, draft → main).
- **Sources:** [FU:backend/src/main/java/org/logrum/ubos/kernel/service/LcmKernelService.java#L710-L878].
- **Tags:** versioning

### CON-FU-026 — Three-way conflict detection by field path
- **What:** Given base/ours/theirs, compute an auto-merged document or a list of conflicting paths.
- **How:** Recursive over the union of keys; per path: changed-in-ours vs base, changed-in-theirs vs base; both changed and different → conflict triplet `{base, ours, theirs}` at dotted path; otherwise pick the changed side. Inputs may be raw JSON **or `ubos://` URIs** (resolved first). Endpoint `POST /merge/conflicts`; UI `MergeConflictResolver`.
- **Why (intent):** Real merge semantics beyond "source wins"; URIs make comparing any two versions trivial.
- **Sources:** [FU:backend/src/main/java/org/logrum/ubos/kernel/service/LcmMergeConflictService.java], [FU:frontend/src/components/MergeConflictResolver.tsx].
- **Tags:** versioning

### CON-FU-027 — Revert by moving the head pointer
- **What:** Revert sets the branch head of an entity back to an older commit of that entity, without writing a new commit.
- **How:** Verify commit belongs to the entity → `UPDATE lcm_entity_branch_head SET head_commit_id`. Later commits remain in the chain but are no longer reachable from the head.
- **Why (intent):** Instant rollback. (Differs from Git `revert`, which writes an inverse commit; this is closer to `reset`.)
- **Sources:** [FU:backend/src/main/java/org/logrum/ubos/kernel/service/LcmKernelService.java#L571-L627].
- **Tags:** versioning

### CON-FU-028 — Branch creation from a commit, registered in the inheritance tree
- **What:** A new branch is registered with a parent and seeded with one head.
- **How:** Insert into `sys_branch_config(branch, parent, description)` (parent defaults to `master`) and create a head for the base commit's entity. All other entities are visible through inheritance.
- **Why (intent):** Cheap branches (copy-on-write via inheritance).
- **Sources:** [FU:backend/src/main/java/org/logrum/ubos/kernel/service/LcmKernelService.java#L629-L677].
- **Tags:** versioning

### CON-FU-029 — Copy entity from any URI
- **What:** Create a new entity (new slug, any branch) from any addressable version.
- **How:** Resolve source URI (branch/inheritance or commit) → commit its JSON to `(type, targetSlug, targetBranch)`.
- **Sources:** [FU:backend/src/main/java/org/logrum/ubos/kernel/service/LcmKernelService.java#L990-L1017].
- **Tags:** versioning

### CON-FU-030 — Per-user draft branches ("stash")
- **What:** Each user edits in a private branch that inherits the base branch; drafts skip schema validation.
- **How:** Backend branch name `draft/{user}/{baseBranch}` (created on demand with parent = base); `saveDraft(user, entityId, base, payload)` → `commitWithoutValidation`; `getDraftPayload` reads through inheritance (draft or base). Frontend helper names drafts `stash/{user}/{entityId}` (a second convention). Endpoints `/api/v1/stash`; UI `DraftManager`.
- **Why (intent):** Safe work-in-progress without polluting shared branches; promotion by merge.
- **Sources:** [FU:backend/src/main/java/org/logrum/ubos/kernel/service/LcmStashService.java], [FU:frontend/src/utils/draftHelpers.ts].
- **Tags:** versioning, workflow

### CON-FU-031 — Environments mapped to branch head or pinned commit
- **What:** Named runtime environments (DEV, UAT, STAGING, PROD) point either at a branch head or at a pinned commit.
- **How:** `sys_environment_config(env_name PK, mapped_branch, mapped_commit_id FK→version_chain, description, updated_at)`; `resolveActiveCommit(env)` → mode `BRANCH_HEAD` or `PINNED`; `pin/unpin`; CRUD endpoints under `/api/console/environment*`.
- **Why (intent):** Release management: promote by moving environment pointers, freeze PROD on a known commit (SQL comment: "pin to a specific commit for stable testing").
- **Sources:** [FU:backend/src/main/resources/db/migration/V6__add_environment.sql], [FU:backend/src/main/java/org/logrum/ubos/kernel/service/LcmEnvironmentService.java].
- **Tags:** deployment, versioning

### CON-FU-032 — Snapshot cache with hit/miss metrics
- **What:** In-memory cache of resolved snapshots keyed `tenantId::uri`.
- **How:** `ConcurrentHashMap`; commit-pinned reads bypass; hits/misses/size exposed to metrics (CON-FU-044) and the Studio cache manager.
- **Sources:** [FU:backend/src/main/java/org/logrum/ubos/kernel/service/LcmKernelService.java#L79-L112].
- **Tags:** cache
