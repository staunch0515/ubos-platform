---
id: SPEC-11-2
title: "Storage Model and Store Traits — Behavior and non-functional"
status: complete
phase: P4
depends_on: [SPEC-11, SPEC-02, SPEC-10, ADR-001, ADR-004]
sources: [UP, FU, LC, UB, UC, US, LS]
---

# Storage Model and Store Traits — Behavior and non-functional

Part 2 of SPEC-11 (`11-storage/`). Header, concepts and file list: `00-index.md`.

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
  - An UNSET commit removes the head row with CAS on the expected head (API-STO-004 `remove`); `seq_num` restarts at 1 if the branch later commits the entity again.
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
