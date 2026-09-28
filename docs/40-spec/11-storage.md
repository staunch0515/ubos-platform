---
id: SPEC-11
title: "Storage Model and Store Traits"
status: complete
phase: P4
depends_on: [SPEC-02, SPEC-10, ADR-001, ADR-004]
sources: [UP, FU, LC, UB, UC, US, LS]
---

# Storage Model and Store Traits

## 0. Chapter header

- **Scope:** This chapter defines three things:
  - The physical model: the six tables with columns, keys, indexes, partitions and edition differences.
  - The database-resident technical tables.
  - The store traits, the runtime store and the blob store.

  It does **not** define what a commit means for branches (SPEC-14), how a flush is orchestrated (SPEC-15) or how queries are compiled (SPEC-21). It defines the primitives those chapters use.
- **MOD:** `STO`
- **depends_on:** SPEC-02, SPEC-10, ADR-001, ADR-004.
- **Terms used:** TERM-SixTables, TERM-EntityInstance, TERM-Version, TERM-Commit, TERM-Snapshot, TERM-OwnData, TERM-Head, TERM-Height, TERM-Tombstone, TERM-EntityStatus, TERM-SearchIndex, TERM-Process, TERM-Provenance, TERM-Outbox, TERM-RuntimeStore, TERM-Edition.
- **Origin summary:**
  - Tables and columns: VRD-01-01…06 (UB schema v2.1, CON-UB-040…044; LS SQLite dialect and traits, CON-LS-006, CON-LS-040…043; UC "six tables only" and the atomic write unit, CON-UC-040…043).
  - Commit write sequence: UP (CON-UP-044).
  - Additions marked **(add)** below are NEW columns. They are needed by later chapters and fixed here, so the schema is final for version 1.

---

## 1. Concepts

- **Existence vs state.**
  - `entity_instance` records that an entity exists: its identity, locator, lifecycle and custody.
  - `entity_version` records its states: immutable snapshots.
  - `branch_head` records which state is current on each branch (UP: "the registry does not care about versions, only about existence", CON-UP-010).
- **Own data is stored.** A snapshot holds the entity's own data (TERM-OwnData), including `_extends`. Effective data is computed (SPEC-13) and never stored.
- **Derived data is rebuildable.** `search_index` and `embedding` can be rebuilt from snapshots and type definitions at any time.
- **Evidence is append-only.**
  - `entity_version`, `process_log` and `process_commit_map` rows are never updated or deleted by the kernel.
  - The one exception is filling `embedding` from null (REQ-STO-003).
  - Archiving (REQ-STO-015) moves rows; it does not change them.
- **View tenant.** A head and its versions belong to the tenant that wrote them (the *view tenant*). Usually this is the entity's own tenant. When a tenant patches a root-package entity, the patch is stored under that tenant (REQ-STO-006).

---

## 2. Model

Physical names carry the prefix `ubos_` (REQ-CONV-061). The types below use SPEC-02 §6
notation. §2.8 maps them to SQL columns.

### 2.1 entity_instance

```yaml
ENT-EntityInstanceRow:
  purpose: Registry of entity existence, locator, lifecycle and custody (table ubos_entity_instance).
  origin: [VRD-01-01, VRD-01-04, VRD-10-07, CON-UB-012, CON-UB-044, CON-UP-010, CON-FU-012]
  fields:
    - name: id
      type: uuid
      required: true
      description: Entity ID (TERM-EntityId). Primary key. Never reused.
    - name: tenant_id
      type: tenant_code
      required: true
      description: Owning tenant of the entity.
    - name: entity_type
      type: type_code
      required: true
      description: Type code of the entity (SPEC-02 §3.4).
    - name: slug
      type: slug
      required: true
      description: Current slug; changes only by a RENAME commit.
    - name: status
      type: enum{ACTIVE|DELETED|ARCHIVED|LOCKED}
      required: true
      description: Lifecycle status (TERM-EntityStatus); reflects the tenant's main branch.
    - name: terminator_commit_id
      type: commit_id?
      required: false
      description: The DELETE commit on main that ended the entity; null unless status is DELETED.
    - name: created_commit_id
      type: commit_id
      required: true
      description: (add) First commit of the entity on any branch.
    - name: display_name
      type: string(255)?
      required: false
      description: Denormalised display name for lists, from the type's display-name property.
    - name: tags
      type: list<string>
      required: true
      description: Denormalised tags for filtering; empty list allowed.
    - name: owner_id
      type: uuid?
      required: false
      description: Entity ID of the owning principal (custody, transferable without a new version).
    - name: visibility
      type: enum{PUBLIC|INTERNAL|PRIVATE}
      required: true
      description: Coarse visibility default for policies (SPEC-22); default INTERNAL.
    - name: package_id
      type: uuid?
      required: false
      description: (add) Entity ID of the Package that installed the entity; null for user data.
    - name: created_at
      type: timestamp
      required: true
      description: Time of the first commit.
    - name: last_modified_at
      type: timestamp
      required: true
      description: Time of the last commit on any branch, or of the last registry change.
  invariants:
    - UNIQUE (tenant_id, lower(entity_type), lower(slug)) among rows with status != DELETED.
    - status = DELETED  <=>  terminator_commit_id IS NOT NULL.
    - entity_type, tenant_id and id never change after insert.
  relations:
    - has_versions -> ENT-EntityVersionRow (1..n)
    - has_heads -> ENT-BranchHeadRow (0..n)
```

### 2.2 entity_version

```yaml
ENT-EntityVersionRow:
  purpose: One immutable version (commit) of one entity on one branch (table ubos_entity_version).
  origin: [VRD-01-01, VRD-01-02, CON-UB-040, CON-UP-011, CON-LC-042, CON-FU-016]
  fields:
    - name: commit_id
      type: commit_id
      required: true
      description: Identity of the version; from the global commit sequence (REQ-STO-013).
    - name: tenant_id
      type: tenant_code
      required: true
      description: (add) View tenant that wrote the version (REQ-STO-006).
    - name: entity_id
      type: uuid
      required: true
      description: The entity (entity_instance.id).
    - name: branch_name
      type: branch_name
      required: true
      description: Branch the version was committed on.
    - name: parent_commit_id
      type: commit_id?
      required: false
      description: Previous head on this branch (or the fork point for the first commit on a new branch); null for the very first version.
    - name: merge_parent_commit_id
      type: commit_id?
      required: false
      description: (add) Second parent for MERGE commits (the source head).
    - name: action
      type: enum{CREATE|UPDATE|DELETE|RENAME|COPY|MERGE|REVERT}
      required: true
      description: (add) Kind of change (TERM-Commit); DELETE marks a tombstone.
    - name: snapshot
      type: json
      required: true
      description: Own data in canonical JSON (REQ-STO-004); for DELETE, the last own data before deletion.
    - name: snapshot_hash
      type: bytes
      required: true
      description: (add) SHA-256 of the canonical snapshot bytes (32 bytes).
    - name: type_commit_id
      type: commit_id?
      required: false
      description: (add) Commit of the type definition used to validate this snapshot; null only for the meta type itself.
    - name: details
      type: json?
      required: false
      description: (add) Action details, e.g. {old_slug,new_slug} for RENAME, {source_uri} for COPY, {reverted_to} for REVERT, {strategy,conflicts_resolved} for MERGE.
    - name: embedding
      type: vector(n)?
      required: false
      description: Optional embedding of this version; n fixed per deployment (default 1536). Filled asynchronously (SPEC-21).
    - name: author_id
      type: uuid
      required: true
      description: Entity ID of the principal that authored the commit.
    - name: message
      type: text?
      required: false
      description: Commit message.
    - name: committed_at
      type: timestamp
      required: true
      description: Time of the flush.
  invariants:
    - Rows are immutable, except embedding, which may be set once from null.
    - parent_commit_id, if set, names a version of the same entity.
    - For action = MERGE, merge_parent_commit_id is set.
  relations:
    - version_of -> ENT-EntityInstanceRow (n..1)
    - produced_by -> ENT-ProcessLogRow via ENT-ProcessCommitMapRow (relation OUTPUT, exactly 1)
```

### 2.3 branch_head

```yaml
ENT-BranchHeadRow:
  purpose: Current commit of an entity on a branch for a view tenant, with height (table ubos_branch_head).
  origin: [VRD-01-03, CON-UB-041, CON-UP-012, CON-FU-012]
  fields:
    - name: tenant_id
      type: tenant_code
      required: true
      description: View tenant (REQ-STO-006).
    - name: entity_id
      type: uuid
      required: true
      description: The entity.
    - name: branch_name
      type: branch_name
      required: true
      description: The branch.
    - name: head_commit_id
      type: commit_id
      required: true
      description: Current version on this branch.
    - name: seq_num
      type: int64
      required: true
      description: Height (TERM-Height); 1 for the first commit on the branch, +1 per commit.
    - name: deleted
      type: bool
      required: true
      description: (add) True when the head is a DELETE commit (a tombstone, possibly masking a parent branch).
    - name: entity_type
      type: type_code
      required: true
      description: (add) Denormalised type code for listing heads by type without a join.
    - name: updated_at
      type: timestamp
      required: true
      description: Time of the last head move.
  invariants:
    - PRIMARY KEY (tenant_id, entity_id, branch_name).
    - deleted = (action of head_commit_id = DELETE).
    - A head moves only by a conditional update on (head_commit_id, seq_num) (REQ-STO-012), or by an audited reset (SPEC-14).
```

### 2.4 search_index

```yaml
ENT-SearchIndexRow:
  purpose: Derived typed attribute index per version (table ubos_search_index).
  origin: [VRD-01-02, VRD-12-01, CON-UB-042, CON-LS-042, CON-UP-042]
  fields:
    - name: commit_id
      type: commit_id
      required: true
      description: The indexed version.
    - name: tenant_id
      type: tenant_code
      required: true
      description: (add) View tenant of the version.
    - name: entity_type
      type: type_code
      required: true
      description: (add) Type code, for type-restricted queries.
    - name: prop_path
      type: string(255)
      required: true
      description: Property path, e.g. email, address.city, lines[].sku (lists use [] and produce one row per element).
    - name: val_text
      type: string(1024)?
      required: false
      description: Text value, normalised per the property's index slot (e.g. lower-case for case-insensitive search).
    - name: val_num
      type: decimal(38,10)?
      required: false
      description: Numeric value.
    - name: val_date
      type: timestamp?
      required: false
      description: Date or timestamp value.
    - name: val_bool
      type: bool?
      required: false
      description: (add) Boolean value.
    - name: val_ref
      type: uuid?
      required: false
      description: (add) Entity ID of the target of a reference property (for joins, SPEC-21).
  invariants:
    - Exactly one val_* column is non-null per row.
    - Rows exist only for properties whose effective definition is searchable (REQ-STO-007).
    - Rows are written in the same flush as their version and are never updated; a re-index writes into a rebuild process (REQ-STO-008).
```

### 2.5 process_log

```yaml
ENT-ProcessLogRow:
  purpose: One append-only audit row per process (table ubos_process_log); the durable part of the audit record (SPEC-30).
  origin: [VRD-04-01, VRD-21-01, CON-UB-043, CON-LC-041, CON-UC-042, CON-LS-041]
  fields:
    - name: process_id
      type: uuid
      required: true
      description: Process ID (UUID v7).
    - name: tenant_id
      type: tenant_code
      required: true
      description: Tenant the process ran in (host tenant).
    - name: operation
      type: uri
      required: true
      description: Operation URI with version, e.g. ubos://system/Action/entity.edit@v1, or ubos://system/Native/mutate@v1 for direct Mutate.
    - name: kind
      type: enum{MUTATE|EMIT|QUERY|JOB|SYSTEM|BOOT|MERGE|ADMIN}
      required: true
      description: (add) Kind of process.
    - name: status
      type: enum{COMPLETED|FAILED|REJECTED}
      required: true
      description: (add) Outcome; REJECTED = denied by policy or validation before running logic.
    - name: principal_id
      type: uuid
      required: true
      description: Acting principal (operator in the corpus).
    - name: on_behalf_of
      type: uuid?
      required: false
      description: (add) Principal a delegated agent or approval acted for.
    - name: app_id
      type: uuid?
      required: false
      description: Initiating app or package (UB billing linkage).
    - name: context_uri
      type: uri?
      required: false
      description: (add) Context entity used, if any.
    - name: branch_name
      type: branch_name
      required: true
      description: (add) Branch the process wrote to or read from.
    - name: request_id
      type: string(64)?
      required: false
      description: (add) UBTP request ID.
    - name: idempotency_key
      type: string(128)?
      required: false
      description: (add) Client idempotency key, if any.
    - name: parent_process_id
      type: uuid?
      required: false
      description: (add) Process that started this one (job attempt, child pipeline, approval apply).
    - name: job_id
      type: uuid?
      required: false
      description: (add) Job entity this process is an attempt of.
    - name: input_digest
      type: bytes?
      required: false
      description: (add) SHA-256 of the canonical request arguments.
    - name: commit_count
      type: int32
      required: true
      description: (add) Number of OUTPUT commits.
    - name: error
      type: json?
      required: false
      description: (add) Error object (SPEC-02 §8) when status != COMPLETED.
    - name: trace_ref
      type: uri?
      required: false
      description: (add) URI of an offloaded trace entity (SPEC-30) when the trace exceeds the inline limit.
    - name: trace_summary
      type: json?
      required: false
      description: (add) Bounded trace tree or its summary (SPEC-30).
    - name: metrics
      type: json?
      required: false
      description: (add) Counters: logic operations, syscalls, rows read, db time, duration.
    - name: billing_commit_id
      type: commit_id?
      required: false
      description: Commit of the billing batch entity that accounted this process (UB).
    - name: started_at
      type: timestamp
      required: true
      description: Process start.
    - name: finished_at
      type: timestamp
      required: true
      description: Process end.
  invariants:
    - One row per process that wrote, failed or was rejected; read-only processes only when read auditing is on (REQ-ARCH-005).
    - Rows are append-only. billing_commit_id may be set once from null by the billing process.
```

### 2.6 process_commit_map

```yaml
ENT-ProcessCommitMapRow:
  purpose: Typed lineage between processes and versions (table ubos_process_commit_map).
  origin: [VRD-04-05, VRD-21-02, CON-UC-021, CON-UC-042, CON-LS-041]
  fields:
    - name: process_id
      type: uuid
      required: true
      description: The process.
    - name: commit_id
      type: commit_id
      required: true
      description: A version the process produced or depended on.
    - name: relation
      type: enum{OUTPUT|DEPENDS_ON_DATA|DEPENDS_ON_TYPE|DEPENDS_ON_FUNC|DEPENDS_ON_CTX|DEPENDS_ON_POLICY}
      required: true
      description: Lineage relation (TERM-Provenance); SPEC-15 defines when each is recorded.
    - name: entity_id
      type: uuid
      required: true
      description: (add) Entity of the commit (denormalised for lineage queries).
    - name: created_at
      type: timestamp
      required: true
      description: Flush time.
  invariants:
    - PRIMARY KEY (process_id, commit_id, relation).
    - Every entity_version row has exactly one OUTPUT row.
```

### 2.7 Database-resident technical tables

These tables are not business or metadata state (REQ-CONV-060). They live in the schema
`ubos_rt` (PostgreSQL) or in the same SQLite file with the prefix `rt_`, because they must
be written in the same database transaction as a flush.

```yaml
ENT-OutboxRow:
  purpose: Transactional outbox of events and emits awaiting dispatch (table ubos_rt.rt_outbox).
  origin: [VRD-11-01, CON-LC-027, CON-UC-003]
  fields:
    - name: seq
      type: int64
      required: true
      description: Monotonic sequence (identity).
    - name: tenant_id
      type: tenant_code
      required: true
      description: Tenant of the process.
    - name: process_id
      type: uuid
      required: true
      description: Producing process.
    - name: kind
      type: enum{EVENT|EMIT|JOB_SIGNAL}
      required: true
      description: Domain event, deferred emit, or signal to enqueue a job.
    - name: payload
      type: json
      required: true
      description: Event or emit body (SPEC-20).
    - name: created_at
      type: timestamp
      required: true
      description: Flush time.
    - name: claimed_by
      type: string(64)?
      required: false
      description: Dispatcher instance holding the row.
    - name: claimed_until
      type: timestamp?
      required: false
      description: Claim lease expiry.
  invariants:
    - Rows are deleted after successful dispatch; they are never evidence (the process log is).
```

- **Migration bookkeeping.** The migration tool's own table (e.g. `_sqlx_migrations`) is allowed and holds only schema versions.

### 2.8 Column mapping per edition

| Spec type | PostgreSQL 16 (enterprise) | SQLite 3 (personal) |
|---|---|---|
| `uuid` | `uuid` | `TEXT` (36 chars, lower case) |
| `commit_id`, `int64` | `bigint` (`commit_id`: `GENERATED ALWAYS AS IDENTITY` on entity_version) | `INTEGER` (`commit_id`: allocated by the commit sequence, REQ-STO-013) |
| `int32` | `integer` | `INTEGER` |
| `tenant_code`, `type_code`, `slug`, `branch_name`, `string(n)` | `varchar(n)` (64 for tenant, 200 for slug and type code, 255 for branch) | `TEXT` |
| `text` | `text` | `TEXT` |
| `enum{…}` | `varchar(16)` + `CHECK` | `TEXT` + `CHECK` |
| `json` | `jsonb` | `TEXT` (canonical JSON), queried with JSON1 functions |
| `bytes` | `bytea` | `BLOB` |
| `bool` | `boolean` | `INTEGER` 0/1 |
| `decimal(38,10)` | `numeric(38,10)` | `TEXT` (canonical decimal) + `REAL` shadow column `val_num_f` for range scans |
| `timestamp` | `timestamptz` | `TEXT` RFC 3339 UTC, fixed width (sortable) |
| `list<string>` (tags) | `text[]` + GIN | `TEXT` (JSON array) |
| `vector(n)` | `vector(n)` (pgvector) + HNSW index | `BLOB` (little-endian f32); no index (REQ-STO-014) |
| `uri` | `text` | `TEXT` |

### 2.9 Indexes

| Table | Index | Purpose |
|---|---|---|
| entity_instance | PK `id` | identity |
| entity_instance | UNIQUE `(tenant_id, lower(entity_type), lower(slug)) WHERE status <> 'DELETED'` | locator (VRD-03-01) |
| entity_instance | `(tenant_id, entity_type, last_modified_at DESC)` | recent lists |
| entity_instance | GIN `tags` (PG) | tag filter |
| entity_instance | `(owner_id)` | custody queries ("my data") |
| entity_instance | `(tenant_id, package_id)` | package uninstall and diff |
| entity_version | PK `(commit_id, committed_at)` (PG, partitioned); PK `commit_id` (SQLite) | identity |
| entity_version | `(entity_id, branch_name, commit_id DESC)` | history |
| entity_version | GIN `snapshot jsonb_path_ops` (PG, optional per tenant size) | containment queries |
| entity_version | HNSW `embedding vector_cosine_ops` (PG) | similarity |
| branch_head | PK `(tenant_id, entity_id, branch_name)` | head lookup, CAS |
| branch_head | `(tenant_id, branch_name, entity_type) INCLUDE (head_commit_id, deleted)` | list heads by type |
| branch_head | `(head_commit_id)` | reverse lookup |
| search_index | `(tenant_id, entity_type, prop_path, val_text)`, `(…, val_num)`, `(…, val_date)`, `(…, val_ref)` | typed filters |
| search_index | `(commit_id)` | per-version fetch and rebuild |
| process_log | PK `(process_id, started_at)` (PG); PK `process_id` (SQLite) | identity |
| process_log | `(tenant_id, started_at DESC)`, `(principal_id, started_at DESC)`, `(job_id)`, `(tenant_id, idempotency_key)` | audit queries |
| process_commit_map | PK `(process_id, commit_id, relation)` | lineage forward |
| process_commit_map | `(commit_id, relation)`, `(entity_id, created_at DESC)` | lineage backward, per-entity audit |

---

## 3. Behavior

### REQ-STO-001 — Six tables, fixed schema
- **Statement:** The store MUST contain exactly the six tables of §2.1–§2.6, with the columns, keys and invariants given, plus the technical tables of §2.7. Adding a column to the six tables is a kernel schema change (REQ-CONV-064), never a package change.
- **Origin:** VRD-01-01, CON-UC-040, CON-UB-040, CON-LS-040
- **Acceptance:**
  1) A schema-introspection test compares the database with the spec for both editions.
  2) Installing any package leaves the schema unchanged.
- **Priority:** P0

### REQ-STO-002 — Registry lifecycle and locator reuse
- **Statement:** The registry row MUST be inserted by the first commit of an entity on any branch.
  - The row's `id` is the entity ID chosen by the session (SPEC-15).
  - A commit for a locator that already has a non-DELETED row MUST reuse that row's `id`, so no second identity is created ("ID split" guard).
  - `last_modified_at` is updated by every commit.
  - `status`/`terminator_commit_id` change only as follows:

| Event | Effect on the registry row |
|---|---|
| DELETE commit on the tenant's `main` | `status := DELETED`, `terminator_commit_id := commit` |
| Restoring commit on `main` after deletion (a REVERT or UPDATE whose parent is the tombstone) | `status := ACTIVE`, `terminator_commit_id := null`, provided no other non-DELETED row holds the locator; otherwise `URI.LOCATOR_TAKEN` |
| Administrative archive/lock/unlock (SPEC-22) | `status := ARCHIVED` / `LOCKED` / `ACTIVE`, recorded as a process of kind ADMIN |
| RENAME commit on `main` | `slug := new slug` |
| Custody transfer (SPEC-22) | `owner_id := new owner`, recorded as a process of kind ADMIN |

- **Rationale:**
  - This separates existence from state (UP) and gives lists and custody queries without JSON parsing (UB).
  - Locator reuse prevents the "ID split" UC guards against.
  - Registry-only changes (status, owner) are still audited through a process of kind ADMIN (VRD-10-07).
- **Origin:** CON-UP-010, CON-UB-012, CON-UB-044, CON-UC-041, CON-FU-016, VRD-01-04
- **Acceptance:**
  1) Two sessions that create the same locator concurrently produce one entity ID; one of the sessions fails with CONFLICT.
  2) Delete then restore on main returns the entity to ACTIVE with the same ID.
  3) After a delete, a new entity with the same locator gets a new ID.
- **Priority:** P0

### REQ-STO-003 — Immutable versions
- **Statement:** An `entity_version` row MUST NOT be updated or deleted by any kernel operation. The only exception is setting `embedding` once, from null, by the embedding job (SPEC-21). Archiving to cold storage (REQ-STO-015) moves partitions without changing rows.
- **Origin:** VRD-01-02, CON-LS-044, CON-UB-040
- **Acceptance:** A database trigger (PostgreSQL) or a store-level check (SQLite) rejects updates other than setting a null embedding.
- **Priority:** P0

### REQ-STO-004 — Canonical snapshots and hashes
- **Statement:** A snapshot MUST be stored as canonical JSON:
  - object keys sorted by Unicode code point;
  - no insignificant whitespace;
  - numbers in shortest round-trip form;
  - decimals of `decimal(p,s)` properties as strings (REQ-CONV-037).

  This follows RFC 8785 (JCS). `snapshot_hash` MUST be SHA-256 over these bytes. A commit whose snapshot hash equals the parent's MAY be skipped by the session as a no-op (SPEC-15), unless its action is RENAME, COPY, MERGE or REVERT.
- **Rationale:**
  - Stable hashes allow no-op detection and integrity checks.
  - They also allow comparisons across editions (JSONB reorders keys, so the canonical form is computed by the kernel, not the database).
- **Origin:** NEW: cross-edition equality; CON-UP-023 (content hash cache)
- **Acceptance:** The same logical payload yields byte-identical snapshots and equal hashes on PostgreSQL and SQLite.
- **Priority:** P0

### REQ-STO-005 — Heads and heights
- **Statement:** Each commit on branch *b* for view tenant *t* MUST do the following:
  - A first commit inserts `branch_head(t, e, b)` with `seq_num = 1`.
  - A later commit moves the head by a conditional update (REQ-STO-012) that sets `seq_num = seq_num + 1`.
  - `deleted` MUST equal whether the new head is a DELETE commit.
  - A branch fork does not copy heads. Reads fall back along the branch parent chain (SPEC-14), so a head exists on a branch only after a commit there.
- **Origin:** VRD-01-03, CON-UB-041, CON-UC-041, VRD-08-01
- **Acceptance:**
  1) After *n* commits on a branch, `seq_num = n`.
  2) A newly created branch has no heads until its first commit.
- **Priority:** P0

### REQ-STO-006 — View tenant and root overlays
- **Statement:** `entity_version.tenant_id`, `branch_head.tenant_id` and `search_index.tenant_id` MUST hold the view tenant (the tenant whose process wrote the version).
  - For entities of the tenant itself, the view tenant equals `entity_instance.tenant_id`.
  - A tenant *t* MAY commit a version of an entity owned by the root tenant `logrums` (a patched platform entity, VRD-18-02). That version and its head carry `tenant_id = t`, and the root's rows stay unchanged.
  - Reads resolve heads in the order of SPEC-23: tenant draft → tenant branch → tenant's heads of the root entity → root's heads.
  - A tenant MUST NOT write versions of entities owned by any tenant other than itself and `logrums`.
- **Rationale:**
  - This realises "overlay of the root package, scoped to the tenant" with no copies and no data in the root tenant's rows.
  - Row-level security by `tenant_id` stays simple: a tenant sees its own rows and `logrums` rows.
- **Origin:** VRD-18-01, VRD-18-02, VRD-08-02, CON-UP-041
- **Acceptance:**
  1) Tenant A patches a root type. Tenant B still reads the root version.
  2) Removing A's head ("reset to inherit") restores the root version for A.
  3) Writing a version of tenant B's entity from tenant A fails with `TEN.CROSS_TENANT_WRITE`.
- **Priority:** P0

### REQ-STO-007 — Derived index at flush
- **Statement:** For each version written, the flush MUST insert search index rows for every property path whose **effective** property definition (SPEC-13) has `searchable = true`.
  - Values are typed by the property's primitive, and text is normalised by the index slot.
  - List elements produce one row each, with `[]` in the path.
  - Reference properties produce `val_ref` rows.
  - Nested object properties are indexed only where the nested property is searchable.
  - A DELETE version produces no index rows.
- **Rationale:** Indexing rules come from type metadata, not from "all top-level fields" (VRD-01-02). This implements UB's design and LS's code (CON-LS-042).
- **Origin:** VRD-01-02, VRD-12-01, CON-UB-042, CON-LS-042, CON-UC-017
- **Acceptance:**
  1) A property marked not searchable produces no rows.
  2) A money property produces a `val_num` row with the scaled decimal.
  3) A reference produces a `val_ref` row with the target's entity ID.
- **Priority:** P0

### REQ-STO-008 — Index rebuild
- **Statement:** The kernel MUST provide an administrative rebuild of `search_index` for a tenant, a type, or a set of commits. The rebuild runs as a process of kind ADMIN. It inserts rows into a staging area, then replaces the rows of the affected commits in one transaction. Rebuilding MUST NOT change any version or head.
- **Rationale:** Changing a property's `searchable` flag, or a bug fix in an index slot, requires re-derivation (derived data is rebuildable).
- **Origin:** NEW: consequence of VRD-01-02
- **Acceptance:** Dropping all index rows of a tenant and rebuilding yields the same rows (set equality).
- **Priority:** P1

### REQ-STO-009 — Process log rows
- **Statement:** The flush MUST insert exactly one `process_log` row with status COMPLETED for a process that commits. A process that fails or is rejected MUST get one row with status FAILED or REJECTED, written in a separate short transaction after its rollback. Process log rows MUST NOT be updated, except setting `billing_commit_id` once.
- **Origin:** VRD-04-01, VRD-21-01, CON-UB-043, CON-LS-041
- **Acceptance:**
  1) Every committed process has one COMPLETED row.
  2) A validation failure yields one REJECTED row with the error causes, and no versions.
- **Priority:** P0

### REQ-STO-010 — Lineage rows
- **Statement:** The flush MUST insert an OUTPUT row for every version it writes. It also inserts one row per recorded dependency, with the relations defined in SPEC-15. Dependencies are de-duplicated per (process, commit, relation).
- **Origin:** VRD-04-05, CON-UC-021, CON-UC-042
- **Acceptance:** A process that reads a Context, a Logic entity and two data entities and writes one entity has 1 OUTPUT row, 1 DEPENDS_ON_CTX row, 1 DEPENDS_ON_FUNC row and 2 DEPENDS_ON_DATA rows.
- **Priority:** P0

### REQ-STO-011 — Transactional outbox
- **Statement:** Events and deferred emits produced by a process MUST be inserted into `rt_outbox` in the same database transaction as the flush. Dispatchers claim rows with a lease (`claimed_by`, `claimed_until`) and delete them after successful hand-off (SPEC-20). An expired claim may be taken by another dispatcher.
- **Rationale:** This replaces UC's dual write to PostgreSQL and Redis, which its own design review criticised (CON-UC-003).
- **Origin:** VRD-11-01, CON-LC-027
- **Acceptance:** Killing a dispatcher after it claims rows but before it deletes them leads to redelivery after the lease expires. Consumers are idempotent by event ID (SPEC-20).
- **Priority:** P0

### REQ-STO-012 — Compare-and-swap on heads
- **Statement:** A head move MUST be a conditional update:
  ```sql
  UPDATE ubos_branch_head
     SET head_commit_id = :new, seq_num = seq_num + 1, deleted = :del, updated_at = :now
   WHERE tenant_id = :t AND entity_id = :e AND branch_name = :b
     AND head_commit_id = :expected_head;          -- or seq_num = :expected_seq
  ```
  - An update of zero rows MUST abort the whole flush with `VER.HEAD_CONFLICT`. The `details` include the expected and actual head and height.
  - A first commit inserts the head. A unique violation on that insert is also `VER.HEAD_CONFLICT`.
  - The flush uses READ COMMITTED isolation (PostgreSQL) or a single `BEGIN IMMEDIATE` transaction (SQLite).
- **Origin:** VRD-01-03, VRD-04-03, CON-UB-021, CON-UB-041
- **Acceptance:** Of two concurrent flushes with the same expected head, one succeeds and the other fails with `VER.HEAD_CONFLICT`. No lost update occurs under a 100-client stress test.
- **Priority:** P0

### REQ-STO-013 — Commit sequence
- **Statement:** Commit IDs MUST be allocated from one monotonically increasing sequence per database:
  - PostgreSQL: an identity column on `entity_version`.
  - SQLite: `INTEGER PRIMARY KEY` allocation inside the write transaction.

  Commit IDs are unique across tenants and branches. They are ordered by allocation, not strictly by commit time. Consumers that need a stream use the outbox, never gaps in commit IDs.
- **Origin:** CON-UB-040, CON-LS-040, REQ-CONV-031
- **Acceptance:** Commit IDs are unique under concurrent load, and a test confirms that no code relies on gap-free IDs.
- **Priority:** P0

### REQ-STO-014 — Edition differences
- **Statement:** Both editions MUST implement every store trait. They differ only in the following ways:

| Feature | Enterprise (PostgreSQL) | Personal (SQLite) |
|---|---|---|
| JSON | `jsonb`, GIN containment | `TEXT`, JSON1 functions; containment by scan |
| Vector similarity | pgvector HNSW | brute-force cosine over candidate set, max 10 000 versions per query; else `QRY.VECTOR_UNSUPPORTED` |
| Partitions and archiving | yes (REQ-STO-015) | no |
| Row-level security | yes (REQ-STO-016) | no (single process, kernel enforcement only) |
| Concurrency | many writers, row-level CAS | one writer at a time (`BEGIN IMMEDIATE`), WAL readers |
| Tenants | many | many allowed; default single tenant `local` |
| Runtime store | Redis | in-process + `rt_*` tables |
- **Origin:** VRD-01-05, VRD-19-02, CON-LS-006, CON-LS-040
- **Acceptance:** The shared store conformance suite passes on both editions. Personal-edition vector queries above the limit return the documented error.
- **Priority:** P0 (enterprise), P1 (personal)

### REQ-STO-015 — Partitioning and archiving (enterprise)
- **Statement:** In the enterprise edition, the following tables MUST be range-partitioned by year: `entity_version` (by `committed_at`), `search_index` (by `commit_id` ranges aligned to the version partitions), `process_log` (by `started_at`) and `process_commit_map` (by `created_at`).
  - A partition older than the tenant's retention setting MAY be detached and moved to cold storage by an ADMIN process.
  - Versions that are heads MUST stay online. The archiver copies them forward into a current-year "retained heads" partition before detaching, which requires a primary key on `(commit_id, committed_at)`.
  - Reads of archived versions fail with `STO.ARCHIVED` and include the archive locator.
- **Origin:** CON-UB-040, D-UB-12 scale notes (CMP-01)
- **Acceptance:** Detaching an old partition leaves all current heads readable. A historical read of an archived commit returns `STO.ARCHIVED`.
- **Priority:** P2

### REQ-STO-016 — Row-level security (enterprise)
- **Statement:** In the enterprise edition, every table MUST have a row-level security policy that allows rows where `tenant_id = current_setting('ubos.tenant')` or `tenant_id = 'logrums'`. The last case is read-only for non-root sessions. The kernel sets `ubos.tenant` per transaction. Cross-tenant operations (SPEC-23) use a kernel role with explicit, audited bypass.
- **Rationale:** This is defence in depth beside kernel enforcement (VRD-18-05).
- **Origin:** VRD-18-05, D-UB-12
- **Acceptance:** A raw SQL query with `ubos.tenant = 'a'` cannot see tenant `b` rows.
- **Priority:** P1

### REQ-STO-017 — Runtime store
- **Statement:** The runtime store MUST implement API-STO-009 with the key spaces below. Losing all of its content MUST NOT lose any committed business fact (ADR-004).

| Key space | Content | TTL | Chapter |
|---|---|---|---|
| `idem:{tenant}:{principal}:{key}` | idempotency record | 24 h default, max 7 d | SPEC-15 |
| `queue:{tenant}:{priority}` | job ID signals | none | SPEC-20 |
| `lease:{name}` | leader and claim leases | seconds | SPEC-10, SPEC-20 |
| `cache:head:{tenant}:{branch}:{entity}` | head commit | minutes, invalidated on flush | SPEC-13 |
| `cache:snap:{commit}` | raw snapshot | LRU (immutable) | SPEC-13 |
| `mem:{context}` | Context memory cache | per memory policy | SPEC-16 |
| `rate:{tenant}:{principal}:{window}` | rate-limit counters | window | SPEC-22 |
| `sess:{session}` | protocol session state | idle timeout | SPEC-24 |
| `sub:{session}` | subscriptions of a session | session lifetime | SPEC-20 |
- **Origin:** ADR-004, VRD-01-06, CON-UC-043
- **Acceptance:** A scenario suite passes, then the runtime store is flushed, then the suite passes again. Jobs resume through the sweeper and heads are re-read from the database.
- **Priority:** P0

### REQ-STO-018 — Blob store
- **Statement:** Binary content larger than 64 KiB, and any file content, MUST be stored in the blob store (API-STO-010), addressed by SHA-256. It is referenced from snapshots by a `File` entity (SPEC-29) or a property of semantic type `FileRef`.
  - Blobs are immutable and de-duplicated by hash.
  - Blobs are deleted only by a garbage-collection ADMIN process, after no retained version references them.
  - Backends: file system (both editions) and S3-compatible (enterprise).
- **Origin:** CON-UB-002 (BlobStorageProvider), CON-UC-044 (VFS as entities), REQ-CONV-044
- **Acceptance:** Uploading the same file twice stores one blob. GC removes a blob only after all referencing versions are archived or purged.
- **Priority:** P1

### REQ-STO-019 — Migrations and schema version
- **Statement:** Schema changes MUST be delivered as ordered migration files, one concept per file (CON-UP-043), per edition. The kernel MUST refuse to start when the database schema version differs from its compiled version (`ARCH.SCHEMA_VERSION`), unless migration is requested (REQ-ARCH-010).
- **Origin:** CON-UP-043, CON-LS-040
- **Acceptance:** Starting a newer kernel on an older schema without `--migrate` fails with `ARCH.SCHEMA_VERSION`.
- **Priority:** P0

### REQ-STO-020 — Registry-only reads
- **Statement:** Listing entities (type, tags, owner, status, display name, last modified) MUST be answerable from `entity_instance` and `branch_head` without reading snapshots.
- **Origin:** CON-UB-012
- **Acceptance:** The query plan for a list-by-type request touches no `entity_version` rows when no property filter is given.
- **Priority:** P1

### REQ-STO-021 — Status enforcement
- **Statement:** The store layer MUST expose the status. The kernel (SPEC-15) MUST enforce it as follows:
  - ARCHIVED entities are read-only.
  - LOCKED entities accept no commits except from processes of kind ADMIN.
  - DELETED entities accept only a restoring commit (REQ-STO-002).
- **Origin:** CON-UB-044, VRD-01-04
- **Acceptance:** An UPDATE on a LOCKED entity by a normal process fails with `STO.ENTITY_LOCKED`.
- **Priority:** P0

---

## 4. Interfaces

Kernel-internal Rust traits in `ubos_store`. Write methods take a transaction handle and are
visible only to the `tx` module (REQ-ARCH-003). All methods are `async`. `StoreResult<T>` is
`Result<T, StoreError>`.

### API-STO-001 — Store and transactions
- **Kind:** rust trait
- **Signature / shape:**
```rust
#[async_trait]
pub trait Store: Send + Sync {
    async fn begin(&self, tenant: &TenantCode) -> StoreResult<Box<dyn StoreTx>>; // sets ubos.tenant (RLS)
    fn reader(&self) -> &dyn ReaderOps;
    fn edition(&self) -> Edition;              // Personal | Enterprise
    async fn schema_version(&self) -> StoreResult<u32>;
}
#[async_trait]
pub trait StoreTx: Send {
    fn instances(&mut self) -> &mut dyn InstanceOps;
    fn versions(&mut self) -> &mut dyn VersionOps;
    fn heads(&mut self) -> &mut dyn HeadOps;
    fn index(&mut self) -> &mut dyn IndexOps;
    fn process_log(&mut self) -> &mut dyn ProcessLogOps;
    fn lineage(&mut self) -> &mut dyn LineageOps;
    fn outbox(&mut self) -> &mut dyn OutboxOps;
    async fn commit(self: Box<Self>) -> StoreResult<()>;
    async fn rollback(self: Box<Self>) -> StoreResult<()>;
}
```
- **Semantics:** A transaction is bound to one tenant for RLS. Cross-tenant writes are impossible except for patched root entities (REQ-STO-006). Dropping a `StoreTx` without committing rolls it back.
- **Origin:** CON-LS-006, VRD-01-05

### API-STO-002 — InstanceOps
- **Kind:** rust trait
- **Signature / shape:**
```rust
#[async_trait]
pub trait InstanceOps: Send {
    /// Insert or reuse by locator; returns the effective entity id (ID-split guard).
    async fn upsert(&mut self, row: &NewInstance) -> StoreResult<Uuid>;
    async fn lock_for_update(&mut self, id: Uuid) -> StoreResult<InstanceRow>;
    async fn set_status(&mut self, id: Uuid, status: EntityStatus,
                        terminator: Option<CommitId>) -> StoreResult<()>;
    async fn rename(&mut self, id: Uuid, new_slug: &Slug) -> StoreResult<()>; // LOCATOR_TAKEN on clash
    async fn touch(&mut self, id: Uuid, at: Timestamp,
                   display_name: Option<&str>, tags: Option<&[String]>) -> StoreResult<()>;
    async fn set_owner(&mut self, id: Uuid, owner: Option<Uuid>) -> StoreResult<()>;
}
```
- **Semantics:** `upsert` raises `URI.LOCATOR_TAKEN` if the given `id` differs from an existing non-DELETED row with the same locator and the session did not intend reuse.
- **Origin:** CON-UC-041, CON-UP-044

### API-STO-003 — VersionOps
- **Kind:** rust trait
- **Signature / shape:**
```rust
#[async_trait]
pub trait VersionOps: Send {
    async fn insert(&mut self, v: &NewVersion) -> StoreResult<CommitId>;
    /// Only allowed transition: embedding NULL -> value.
    async fn set_embedding(&mut self, commit: CommitId, emb: &[f32]) -> StoreResult<()>;
}
pub struct NewVersion {
    pub tenant_id: TenantCode, pub entity_id: Uuid, pub branch: BranchName,
    pub parent: Option<CommitId>, pub merge_parent: Option<CommitId>,
    pub action: CommitAction, pub snapshot: CanonicalJson, pub snapshot_hash: [u8; 32],
    pub type_commit_id: Option<CommitId>, pub details: Option<serde_json::Value>,
    pub author_id: Uuid, pub message: Option<String>, pub committed_at: Timestamp,
}
```
- **Origin:** CON-UB-040, REQ-STO-003

### API-STO-004 — HeadOps
- **Kind:** rust trait
- **Signature / shape:**
```rust
#[async_trait]
pub trait HeadOps: Send {
    /// First commit on the branch; HEAD_CONFLICT if a head exists.
    async fn insert(&mut self, h: &NewHead) -> StoreResult<()>;
    /// CAS move; HEAD_CONFLICT if expected does not match (REQ-STO-012).
    async fn advance(&mut self, key: &HeadKey, expected: Expected, new_head: CommitId,
                     deleted: bool, at: Timestamp) -> StoreResult<i64 /* new seq */>;
    /// Remove a head (reset to inherit, draft cleanup); CAS on expected.
    async fn remove(&mut self, key: &HeadKey, expected: Expected) -> StoreResult<()>;
    /// Administrative reset (SPEC-14); audited by the caller's ADMIN process.
    async fn force_set(&mut self, key: &HeadKey, head: CommitId, deleted: bool) -> StoreResult<()>;
}
pub struct HeadKey { pub tenant_id: TenantCode, pub entity_id: Uuid, pub branch: BranchName }
pub enum Expected { Commit(CommitId), Seq(i64), Absent }
```
- **Origin:** CON-UB-041, VRD-08-02, VRD-08-05

### API-STO-005 — IndexOps, ProcessLogOps, LineageOps, OutboxOps
- **Kind:** rust trait
- **Signature / shape:**
```rust
#[async_trait] pub trait IndexOps: Send {
    async fn insert_rows(&mut self, rows: &[IndexRow]) -> StoreResult<()>;
    async fn replace_for_commits(&mut self, commits: &[CommitId], rows: &[IndexRow]) -> StoreResult<()>;
}
#[async_trait] pub trait ProcessLogOps: Send {
    async fn insert(&mut self, row: &ProcessLogRow) -> StoreResult<()>;
    async fn set_billing(&mut self, process: Uuid, billing: CommitId) -> StoreResult<()>;
}
#[async_trait] pub trait LineageOps: Send {
    async fn insert(&mut self, rows: &[LineageRow]) -> StoreResult<()>; // de-duplicated
}
#[async_trait] pub trait OutboxOps: Send {
    async fn push(&mut self, rows: &[NewOutboxRow]) -> StoreResult<()>;
}
```
- **Origin:** REQ-STO-007…011

### API-STO-006 — ReaderOps
- **Kind:** rust trait
- **Signature / shape:**
```rust
#[async_trait]
pub trait ReaderOps: Send + Sync {
    async fn instance(&self, id: Uuid) -> StoreResult<Option<InstanceRow>>;
    async fn instance_by_locator(&self, loc: &Locator) -> StoreResult<Option<InstanceRow>>; // non-DELETED
    async fn head(&self, key: &HeadKey) -> StoreResult<Option<HeadRow>>;
    async fn heads_for(&self, tenants: &[TenantCode], entity: Uuid, branches: &[BranchName])
        -> StoreResult<Vec<HeadRow>>;                     // one round trip for fallback chains
    async fn version(&self, commit: CommitId) -> StoreResult<Option<VersionRow>>;
    async fn versions(&self, commits: &[CommitId]) -> StoreResult<Vec<VersionRow>>;
    async fn history(&self, entity: Uuid, branch: &BranchName, range: CommitRange, page: Page)
        -> StoreResult<Vec<VersionMeta>>;                 // without snapshots
    async fn version_at(&self, entity: Uuid, branch: &BranchName, time: Timestamp)
        -> StoreResult<Option<CommitId>>;                 // latest commit <= time (TERM-AsOf)
    async fn parent_chain(&self, commit: CommitId, limit: u32) -> StoreResult<Vec<VersionMeta>>;
    async fn list_heads(&self, filter: &HeadFilter, page: Page) -> StoreResult<Vec<HeadListItem>>;
    async fn search(&self, plan: &IndexQueryPlan) -> StoreResult<Vec<HeadListItem>>; // SPEC-21 compiles plans
    async fn process(&self, id: Uuid) -> StoreResult<Option<ProcessLogRow>>;
    async fn lineage(&self, q: &LineageQuery) -> StoreResult<Vec<LineageRow>>;
    async fn similar(&self, q: &VectorQuery) -> StoreResult<Vec<(CommitId, f32)>>;
}
```
- **Semantics:** Readers never see uncommitted data. `list_heads` and `search` return registry and head data without snapshots (REQ-STO-020).
- **Origin:** CON-LS-043, CON-UB-042, CON-FU-041

### API-STO-007 — Outbox claiming
- **Kind:** rust trait
- **Signature / shape:**
```rust
#[async_trait]
pub trait OutboxClaim: Send + Sync {
    async fn claim(&self, dispatcher: &str, max: u32, lease: Duration) -> StoreResult<Vec<OutboxRow>>;
    async fn complete(&self, seqs: &[i64]) -> StoreResult<()>;     // delete rows
    async fn release(&self, seqs: &[i64]) -> StoreResult<()>;      // give back early
}
```
- **Origin:** REQ-STO-011

### API-STO-008 — Store conformance suite
- **Kind:** test contract
- **Signature / shape:** A crate `ubos_store::conformance` exposes `run_all(store: Arc<dyn Store>)`. It covers REQ-STO-001…021 and runs against each backend in CI.
- **Origin:** VRD-01-05, VRD-22-03

### API-STO-009 — RuntimeStore
- **Kind:** rust trait
- **Signature / shape:**
```rust
#[async_trait]
pub trait RuntimeStore: Send + Sync {
    async fn get(&self, key: &str) -> RtResult<Option<Bytes>>;
    async fn set(&self, key: &str, val: Bytes, ttl: Option<Duration>) -> RtResult<()>;
    async fn set_nx(&self, key: &str, val: Bytes, ttl: Option<Duration>) -> RtResult<bool>;
    async fn cas(&self, key: &str, expected: Option<Bytes>, val: Bytes, ttl: Option<Duration>) -> RtResult<bool>;
    async fn del(&self, key: &str) -> RtResult<()>;
    async fn incr(&self, key: &str, by: i64, ttl: Option<Duration>) -> RtResult<i64>;
    async fn push(&self, queue: &str, item: Bytes) -> RtResult<()>;
    async fn pop(&self, queues: &[&str], wait: Duration) -> RtResult<Option<(String, Bytes)>>;
    async fn publish(&self, channel: &str, msg: Bytes) -> RtResult<()>;
    async fn subscribe(&self, channels: &[&str]) -> RtResult<BoxStream<'static, (String, Bytes)>>;
    async fn acquire_lease(&self, name: &str, holder: &str, ttl: Duration) -> RtResult<bool>;
    async fn release_lease(&self, name: &str, holder: &str) -> RtResult<()>;
}
```
- **Semantics:** Implementations: Redis 7 (enterprise), and an in-process implementation with optional `rt_*` persistence (personal). Every value is non-authoritative (ADR-004).
- **Origin:** ADR-004, CON-UC-043

### API-STO-010 — BlobStore
- **Kind:** rust trait
- **Signature / shape:**
```rust
#[async_trait]
pub trait BlobStore: Send + Sync {
    async fn put(&self, data: BoxStream<'static, Bytes>, expected_sha256: Option<[u8; 32]>)
        -> BlobResult<BlobRef /* sha256, size */>;
    async fn get(&self, sha256: &[u8; 32]) -> BlobResult<BoxStream<'static, Bytes>>;
    async fn exists(&self, sha256: &[u8; 32]) -> BlobResult<bool>;
    async fn delete(&self, sha256: &[u8; 32]) -> BlobResult<()>;   // GC only
}
```
- **Origin:** REQ-STO-018

### Error codes (STO)

| Code | Category | Meaning |
|---|---|---|
| `STO.UNAVAILABLE` | UNAVAILABLE | database not reachable; retryable |
| `STO.ARCHIVED` | NOT_FOUND | version is in an archived partition; `details.archive` locates it |
| `STO.ENTITY_LOCKED` | CONFLICT | entity status LOCKED |
| `STO.ENTITY_ARCHIVED` | CONFLICT | entity status ARCHIVED (read-only) |
| `STO.SNAPSHOT_TOO_LARGE` | LIMIT | snapshot above the configured maximum (default 1 MiB) |
| `STO.IMMUTABLE_VIOLATION` | INTERNAL | attempt to change an immutable row |
| `STO.RUNTIME_UNAVAILABLE` | UNAVAILABLE | runtime store not reachable |
| `STO.BLOB_NOT_FOUND` | NOT_FOUND | blob hash unknown |
| `STO.BLOB_HASH_MISMATCH` | INVALID | uploaded content does not match expected hash |

(`VER.HEAD_CONFLICT` is defined in SPEC-14; `URI.LOCATOR_TAKEN` in SPEC-12.)

---

## 5. Non-functional

| ID | Requirement | Target (reference hardware in SPEC-32) |
|---|---|---|
| NFR-PERF-010 | Head read by locator (cache miss) | p95 ≤ 2 ms (PG), ≤ 1 ms (SQLite) |
| NFR-PERF-011 | Flush of 1 entity with 20 indexed paths | p95 ≤ 10 ms (PG) |
| NFR-PERF-012 | Flush of 100 entities in one process | p95 ≤ 250 ms (PG) |
| NFR-SCAL-010 | Versions per tenant with no degradation of head reads | ≥ 100 million (partitioned) |
| NFR-DUR-010 | Committed flush survives process crash | PG `synchronous_commit = on`; SQLite WAL + `synchronous = FULL` |
| NFR-DUR-011 | Backup/restore point-in-time | PG PITR; SQLite online backup API; runtime store not backed up |
| NFR-PORT-010 | Identical snapshot bytes across editions | 100 % (REQ-STO-004) |

---

## 6. Acceptance

| REQ | Criterion |
|---|---|
| REQ-STO-001 | Schema introspection matches the spec on both editions |
| REQ-STO-002 | Concurrent create yields one ID; delete/restore keeps the ID; locator reuse after delete gets a new ID |
| REQ-STO-003 | Update trigger or check rejects changes other than a first embedding |
| REQ-STO-004 | Cross-edition byte equality of snapshots and hashes |
| REQ-STO-005 | Heights count commits; new branches have no heads |
| REQ-STO-006 | Root patch isolation between tenants; reset to inherit; cross-tenant write rejected |
| REQ-STO-007 | Searchable-only indexing; typed rows; `val_ref` for references |
| REQ-STO-008 | Rebuild yields an equal row set |
| REQ-STO-009 | COMPLETED/REJECTED/FAILED rows as specified |
| REQ-STO-010 | Lineage row counts per relation |
| REQ-STO-011 | Outbox redelivery after an expired claim |
| REQ-STO-012 | CAS stress test: no lost update |
| REQ-STO-013 | Unique commit IDs; no reliance on gap-free IDs |
| REQ-STO-014 | Conformance suite on both editions; documented vector limit |
| REQ-STO-015 | Partition detach keeps heads readable |
| REQ-STO-016 | RLS isolation by `ubos.tenant` |
| REQ-STO-017 | Suite passes across a runtime-store flush |
| REQ-STO-018 | Blob de-duplication and GC rule |
| REQ-STO-019 | Schema version mismatch refuses start |
| REQ-STO-020 | List plan avoids snapshots |
| REQ-STO-021 | Status enforcement errors |

---

## 7. Implementation notes

- **Reference DDL:**
  - UB V1 [UB:backend/src/main/resources/db/migration/V1__init_schema.sql] (partitions, GIN, vector index, `seq_num`).
  - LS SQLite dialect [LS:docs/db/sqlite/V1_init.sql].
  - UC DDL [UC:docs/db/V1_init.sql].
  - Rename the `bpu_` prefix to `ubos_` and add the columns marked (add).
- **Reference write paths:**
  - LS `commit_changes` [LS:crates/ubos_store/src/engine/writer.rs#L19-L77] (process log → instance → version → head → index → lineage in one transaction).
  - UC atomic unit [UC:src/infra/store/writer/atomic.rs#L8-L76].
  - SPEC-15 fixes the order and adds CAS and the outbox.
- **Reference reader:** LS `find_entities` / `load_entity_head` [LS:crates/ubos_store/src/engine/reader.rs] (CON-LS-043).
- **Canonical JSON:** use an RFC 8785 implementation (e.g. `serde_jcs`). Never trust `jsonb::text` for hashing.
- **SQLite settings:** `journal_mode=WAL`, `synchronous=FULL`, `foreign_keys=ON`, `busy_timeout=5000`. Use a single writer connection, and a pool for readers.
- **PostgreSQL:** set `ubos.tenant` with `SET LOCAL` inside each transaction. Use `pgvector` ≥ 0.7 for HNSW.
- **Search index volume:** only searchable paths are indexed. Types SHOULD mark at most ~20 paths searchable (SPEC-13 guidance).

## 8. Open questions

None.
- Deferred: VRD-19-05 (sync), which would add push/pull of heads using these traits (SPEC-31).
- Deferred to SPEC-23: dedicated schemas or partitions for large tenants (VRD-18-05).
