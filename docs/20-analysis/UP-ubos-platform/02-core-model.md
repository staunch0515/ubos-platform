---
id: ANA-UP-02
title: "UP Core Model"
status: complete
phase: P1
depends_on: [ANA-UP-01]
sources: [UP]
---

# UP — Core Model

## Model overview

```
EntityInstance (id, entity_type, slug)            -- identity, never versioned
   │ 1..n
   ▼
VersionChain  (commit_id, entity_id, branch_name, parent_commit_id, snapshot_data JSON, author_id, message, committed_at)
   ▲
   │ head_commit_id
BranchHead    (entity_id, branch_name) → head_commit_id

BranchConfig  (branch_name → parent_branch)        -- branch family tree
ProcessLog    (process_id, process_name, operator_id, started_at)
ProcessEntityMap (process_id, commit_id)           -- process groups commits
Relation      (source_entity_id, target_entity_id, relation_type)
SearchIndex   (commit_id, prop_name, val_text, val_num)
```

Textual description: an entity is an identity row; its content lives only in commits; each
branch of each entity has one head pointer; branches form an inheritance tree; processes
group commits; relations are typed edges between entity identities.

### CON-UP-010 — Identity = (entity_type, slug)
- **What:** Each entity has a UUID `id` and a business identity `(entity_type, slug)` that is unique.
- **How:** `lcm_entity_instance(id VARCHAR(64) PK, entity_type VARCHAR(50), slug VARCHAR(255), created_at, UNIQUE(entity_type, slug))`. The kernel resolves every read/write by `(type, slug)`; instances are created lazily on first commit ("find or create").
- **Why (intent):** SQL comment: "Entity registry — does not care about versions, only about existence." Separates *existence* from *state*.
- **Sources:** [UP:ubos-server/src/main/resources/db/migration/V1__init_ubos_schema.sql], [UP:ubos-server/src/main/java/org/logrum/ubos/kernel/service/LcmKernelService.java#L138-L172].
- **Tags:** identity, entity

### CON-UP-011 — Commit node with full snapshot and parent pointer
- **What:** Every change is an immutable commit row holding the full JSON state of the entity on one branch.
- **How:** `lcm_entity_version_chain(commit_id BIGSERIAL PK, entity_id, branch_name, parent_commit_id, snapshot_data TEXT (JSON), author_id, message, committed_at)`. On commit the current branch head becomes `parent_commit_id` (null for the first commit).
- **Why (intent):** SQL comment: "Version chain — core storage, Git commit node." Full snapshots make every historical state directly readable (time travel) without replay.
- **Sources:** [UP:ubos-server/src/main/resources/db/migration/V1__init_ubos_schema.sql], [UP:ubos-server/src/main/java/org/logrum/ubos/kernel/model/LcmEntityVersionChain.java].
- **Tags:** versioning, persistence

### CON-UP-012 — Branch head as a ref pointer
- **What:** `(entity_id, branch_name) → head_commit_id`, like Git refs.
- **How:** `lcm_entity_branch_head` PK `(entity_id, branch_name)`; updated via `INSERT … ON CONFLICT DO UPDATE` after each commit. Reads join head → version to get the current snapshot.
- **Why (intent):** SQL comment: "Branch pointer table (like Git refs)". O(1) access to current state per branch.
- **Sources:** [UP:ubos-server/src/main/resources/db/migration/V1__init_ubos_schema.sql], [UP:ubos-server/src/main/java/org/logrum/ubos/kernel/repository/LcmVersionRepository.java].
- **Tags:** versioning

### CON-UP-013 — Everything is a typed entity (code, UI, config, users, types)
- **What:** One storage model holds all kinds of platform artifacts, distinguished only by the free-form `entity_type` string.
- **How:** Observed types:

| entity_type | Meaning | Example slug | Where |
|---|---|---|---|
| `LOGIC` | Groovy script `{engine|meta, content}` | `logic.sys.greeting`, `logic.hr.onboard` | Genesis, `commit_onboard.json` |
| `VIEW` | UI schema | `schema.desktop.default`, `view.country.form` | Genesis, copilot |
| `DATA`, `DATA_USER`, `DATA_SALARY` | business records | `user.<empId>`, `salary.<empId>` | `commit_onboard.json` |
| `CRON` | schedule `{cron, target, params}` | – | `LcmSchedulerService` |
| `LISTENER` | event handler `{trigger, content}` | – | `LcmEventService` |
| `CONFIG` | policy `{policyBranch}` | `policy.user.<userId>` | `AuthWebFilter` |
| `USER` | account | `user_admin` | Genesis |
| `TYPE_DEF` | type definition | `type.meta`, `type.primitive.money` | Genesis |

- **Why (intent):** A single versioning/branching/audit mechanism covers the whole platform, so logic, UI and configuration gain history, branches and time travel for free.
- **Sources:** [UP:ubos-server/src/main/java/org/logrum/ubos/server/bootstrap/Genesis.java], [UP:commit_onboard.json], [UP:ubos-web/src/types/ubos.ts] (`'LOGIC' | 'VIEW' | 'DATA' | 'CRON'`).
- **Tags:** entity, metadata

### CON-UP-014 — Dotted slug namespaces
- **What:** Slugs are hierarchical dotted names that encode category and ownership.
- **How:** `logic.calc.tax`, `logic.hr.onboard`, `logic.wallet.withdraw`, `schema.desktop.default`, `type.primitive.money`, `policy.user.A001`, `view.country.form`.
- **Why (intent):** SQL comment: "business unique identifier, e.g. 'logic.calc.tax'". Human-readable, stable keys usable from scripts, UI actions and URIs.
- **Sources:** [UP:ubos-server/src/main/resources/db/migration/V1__init_ubos_schema.sql], Genesis.
- **Tags:** identity

### CON-UP-015 — Typed relations between entity identities
- **What:** A generic edge table connects two entity identities with a relation type.
- **How:** `lcm_entity_relation(id, source_entity_id FK, target_entity_id FK, relation_type, created_at)` + index `(source_entity_id, relation_type)`. API: `createRelation(sourceSlug, targetSlug, type)` (both must exist), `getRelatedEntities(sourceSlug, type) → target slugs`. Relations attach to identities, not to commits (not versioned, not branched). Exposed to scripts as `relations`.
- **Why (intent):** SQL comment examples: `OWNS`, `BELONGS_TO`, `APPROVES` — a graph over business objects.
- **Sources:** [UP:ubos-server/src/main/resources/db/migration/V5__add_entity_relations.sql], [UP:ubos-server/src/main/java/org/logrum/ubos/kernel/service/LcmRelationService.java].
- **Tags:** relationship

### CON-UP-016 — Type definitions as entities with URI-referenced validators
- **What:** Types are themselves committed entities; a type can reference a validator logic by URI.
- **How:** Genesis commits `TYPE_DEF type.meta` = `{name:"Meta Type Definition", fields:[{name:"slug",type:"string",required:true},{name:"schema",type:"json"}]}` and `TYPE_DEF type.primitive.money` = `{name:"Currency Amount", precision:2, currency:"USD", validator_ref:"ubos://ctx/logic/validate.money"}`.
- **Why (intent):** Early form of a self-describing meta-model and of `ubos://` addressing (fully developed in `UB`, `US`, `LS`).
- **Sources:** [UP:ubos-server/src/main/java/org/logrum/ubos/server/bootstrap/Genesis.java].
- **Tags:** meta-model, type-system

### CON-UP-017 — Process as the unit of business change
- **What:** A *process* is a named business operation that groups the commits it caused across many entities.
- **How:** `lcm_process_commit_log(process_id PK, process_name, operator_id, started_at)` + `lcm_process_entity_map(process_id, commit_id)`. `kernel.startProcess(name, operator)` returns an id; `commit(..., processId)` links the commit. Audit API lists processes and returns, per process, every commit with entity slug/type, branch, message, author, time, snapshot.
- **Why (intent):** SQL comment: "Process log — records *who*, *in which scenario*, initiated this change"; "associate one business operation with multiple commits". Enables a business-level audit trail rather than a row-level one.
- **Sources:** [UP:ubos-server/src/main/resources/db/migration/V4__add_process_log.sql], [UP:ubos-server/src/main/java/org/logrum/ubos/kernel/service/LcmKernelService.java#L178-L200], [UP:ubos-server/src/main/java/org/logrum/ubos/kernel/service/LcmAuditService.java], [UP:commit_onboard.json].
- **Tags:** process, versioning, observability

### CON-UP-018 — Auth context model
- **What:** A minimal identity `AuthContext{userId, username, role, tenantId}` is attached to every request.
- **How:** Built in `AuthWebFilter`, stored in Reactor context under `AuthContext.class`, read by `ViewController` and `LcmLogicExecutor` (injected into script `ctx.currentUser`). Default when absent: `SYSTEM/SYSTEM_USER/SYSTEM/MASTER`. The `tenantId` field is populated from a policy's branch (CON-UP-052), i.e. *tenant = branch*.
- **Why (intent):** Identity determines which branch of the world a user sees.
- **Sources:** [UP:ubos-server/src/main/java/org/logrum/ubos/common/AuthContext.java], [UP:ubos-server/src/main/java/org/logrum/ubos/server/config/AuthWebFilter.java].
- **Tags:** auth, multi-tenancy, context
