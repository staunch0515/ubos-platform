---
id: ANA-FU-08
title: "FU Highlights"
status: complete
phase: P1
depends_on: [ANA-FU-01, ANA-FU-02, ANA-FU-03, ANA-FU-04, ANA-FU-05, ANA-FU-06]
sources: [FU]
---

# FU — Highlights

FU inherits all UP highlights (HL-UP-001…012) for the shared kernel. Listed here are only
FU's own contributions, ranked by expected value.

### HL-FU-001 — Governance as versioned data
- **Summary:** Users, groups, policies, schemas, API keys, webhooks and approval requests are all entities in the version chain; services are thin layers over the kernel.
- **Why it stands out:** Access rules, contracts and integrations get history, diff, branch, revert and audit with zero extra storage design; one mechanism instead of a dozen admin tables.
- **Concepts:** CON-FU-001, CON-FU-014, CON-FU-015
- **Reuse recommendation:** adopt
- **Sources:** [FU:backend/src/main/java/org/logrum/ubos/kernel/service/]
- **Tags:** metadata, permission, versioning

### HL-FU-002 — Schema-on-commit with versioned schemas
- **Summary:** A `SCHEMA/{TYPE}` entity (JSON Schema) validates every commit of that type; opt-in, cached, bootstrap-safe, bypassed only for drafts.
- **Why it stands out:** Data contracts evolve with the data, can be branched per tenant, and merged results are validated too.
- **Concepts:** CON-FU-020
- **Reuse recommendation:** adopt (compare with LC JSON Schema per type and US/UB meta-model types in P2 topic 9)
- **Sources:** [FU:backend/src/main/java/org/logrum/ubos/kernel/util/JsonSchemaValidator.java]
- **Tags:** schema, rules

### HL-FU-003 — Approval as deferred, URI-targeted commit
- **Summary:** A request stores the target URI and payload; approval executes the commit as the approver and records the resulting commit id; every state change is a commit.
- **Why it stands out:** Generic four-eyes control for *any* resource without per-object workflow code.
- **Concepts:** CON-FU-021
- **Reuse recommendation:** adopt
- **Sources:** [FU:backend/src/main/java/org/logrum/ubos/kernel/service/LcmApprovalService.java]
- **Tags:** workflow, permission

### HL-FU-004 — IAM-style policies over `ubos://` resources
- **Summary:** Roles grant actions; priority-ordered policies with glob resource patterns and branch/type conditions refine them; explicit DENY wins.
- **Why it stands out:** Fine-grained, data-defined authorization whose subject space is the platform's own URI space (branches and entity types are first-class conditions).
- **Concepts:** CON-FU-022
- **Reuse recommendation:** adapt (evaluate group hierarchy, time windows, per-record conditions in the spec)
- **Sources:** [FU:backend/src/main/java/org/logrum/ubos/kernel/service/LcmAuthzService.java], [FU:backend/src/main/java/org/logrum/ubos/web/console/dto/PolicyPayload.java]
- **Tags:** permission, org-model

### HL-FU-005 — Merge with three-way conflict detection
- **Summary:** Per-entity merge between branches (source-wins deep merge) plus a three-way, path-level conflict detector accepting JSON or URIs, with a resolver UI.
- **Why it stands out:** Branching business data is only useful if branches can be reconciled; FU is the only Java repo with a complete merge story.
- **Concepts:** CON-FU-025, CON-FU-026, CON-FU-052
- **Reuse recommendation:** adopt (spec should use three-way merge by default, source-wins as a strategy)
- **Sources:** [FU:backend/src/main/java/org/logrum/ubos/kernel/service/LcmMergeConflictService.java]
- **Tags:** versioning

### HL-FU-006 — Environments as pointers (branch head or pinned commit)
- **Summary:** DEV/UAT/STAGING/PROD map to a branch or are pinned to a commit; promotion and freeze are pointer moves.
- **Why it stands out:** Release management of business logic/config without deployments.
- **Concepts:** CON-FU-031
- **Reuse recommendation:** adopt (generalize pinning to a release/tag set of many entities)
- **Sources:** [FU:backend/src/main/resources/db/migration/V6__add_environment.sql]
- **Tags:** deployment, versioning

### HL-FU-007 — Private draft branches inheriting the base
- **Summary:** Each user edits in `draft/{user}/{base}`, which inherits everything from the base branch; drafts skip validation until promoted.
- **Why it stands out:** Safe "save anytime" editing on top of branch inheritance, no copying.
- **Concepts:** CON-FU-030
- **Reuse recommendation:** adopt (unify naming; compare with UB draft→publish lifecycle in P2 topic 8)
- **Sources:** [FU:backend/src/main/java/org/logrum/ubos/kernel/service/LcmStashService.java]
- **Tags:** versioning, workflow

### HL-FU-008 — Canonical URI with commit, tag and key-path; extended modes
- **Summary:** `ubos://scope/type/slug?branch&commit&tag&key` addresses any version and any field; the client grammar adds ctx, self, alias, tag, commit and remote-host modes.
- **Why it stands out:** One string addresses versions, fields and (planned) other nodes; used by policies, approvals and merge inputs.
- **Concepts:** CON-FU-010, CON-FU-011
- **Reuse recommendation:** adopt / fuse with UB, US, LS URI designs (P2 topic 3)
- **Sources:** [FU:backend/src/main/java/org/logrum/ubos/kernel/util/UbosUriUtil.java], [FU:frontend/src/utils/useUbosUri.ts]
- **Tags:** identity, protocol

### HL-FU-009 — Pointer-based branch diff and SQL field-level blame
- **Summary:** Branch status from head comparison (NEW/MODIFIED/DELETED); per-commit changed-keys via JSONB window functions.
- **Why it stands out:** Cheap, database-native answers to "what differs" and "who changed this field".
- **Concepts:** CON-FU-040, CON-FU-041
- **Reuse recommendation:** adopt
- **Sources:** [FU:backend/src/main/java/org/logrum/ubos/kernel/service/LcmHistoryService.java]
- **Tags:** versioning, observability

### HL-FU-010 — UBOS Studio: IDE for business data
- **Summary:** Namespace tree, entity grid with batch ops, Monaco editor, time-travel slider with blame, diff/merge UI, governance managers, operational dashboard.
- **Why it stands out:** The most complete operator/developer experience of all repos.
- **Concepts:** CON-FU-050…054
- **Reuse recommendation:** adapt (as the "admin/dev" shell in P2 topic 14)
- **Sources:** [FU:frontend/src/components/]
- **Tags:** frontend

### HL-FU-011 — Hashed, scoped API keys and HMAC-signed webhooks as data
- **Summary:** Show-once keys stored as SHA-256 with scopes/expiry; webhooks with HMAC signature, timeout and retry.
- **Why it stands out:** Standard integration hygiene expressed through the entity model.
- **Concepts:** CON-FU-003, CON-FU-023, CON-FU-024, CON-FU-060
- **Reuse recommendation:** adopt
- **Sources:** [FU:backend/src/main/java/org/logrum/ubos/kernel/service/ApiKeyService.java]
- **Tags:** integration, auth

### HL-FU-012 — Stable UUID identity with mutable slug (rename/copy as commits)
- **Summary:** Rename moves the slug and records a RENAME commit; copy creates a new entity from any URI.
- **Why it stands out:** Refactoring of namespaces without losing history.
- **Concepts:** CON-FU-016, CON-FU-029
- **Reuse recommendation:** adopt
- **Sources:** [FU:backend/src/main/java/org/logrum/ubos/kernel/service/LcmKernelService.java#L891-L1017]
- **Tags:** identity, versioning

### HL-FU-013 — Git-like web terminal
- **Summary:** `commit / show / checkout / exists / search` commands over HTTP with ANSI output.
- **Why it stands out:** Keyboard-first, scriptable access to the kernel.
- **Concepts:** CON-FU-055
- **Reuse recommendation:** inspiration-only (compare with US CLI and command palettes, P2 topic 14)
- **Sources:** [FU:backend/src/main/java/org/logrum/ubos/server/controller/WebConsoleController.java]
- **Tags:** cli

### HL-FU-014 — Metrics derived from the version store
- **Summary:** Activity/health metrics computed from commits and cache counters.
- **Why it stands out:** No separate telemetry store needed for business activity.
- **Concepts:** CON-FU-044
- **Reuse recommendation:** adapt
- **Sources:** [FU:backend/src/main/java/org/logrum/ubos/kernel/service/LcmMetricsService.java]
- **Tags:** observability
