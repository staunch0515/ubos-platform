---
id: SPEC-11-1
title: "Storage Model and Store Traits — Model"
status: complete
phase: P4
depends_on: [SPEC-11, SPEC-02, SPEC-10, ADR-001, ADR-004]
sources: [UP, FU, LC, UB, UC, US, LS]
---

# Storage Model and Store Traits — Model

Part 1 of SPEC-11 (`11-storage/`). Header, concepts and file list: `00-index.md`.

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
      type: enum{CREATE|UPDATE|DELETE|RENAME|COPY|MERGE|REVERT|UNSET}
      required: true
      description: (add) Kind of change (TERM-Commit); DELETE marks a tombstone; UNSET records "reset to inherit" (the branch no longer has an own version; SPEC-14 REQ-VER-004).
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
    - For action = UNSET, snapshot is {} and no head points to the version (its head row is removed).
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
    - No head points to an UNSET version.
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
