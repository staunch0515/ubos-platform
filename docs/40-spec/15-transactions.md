---
id: SPEC-15
title: "Transactions: Processes, Sessions, Change Sets"
status: complete
phase: P4
depends_on: [SPEC-02, SPEC-10, SPEC-11, SPEC-12, SPEC-13, SPEC-14, ADR-004]
sources: [UP, FU, LC, UB, UC, US, LS]
---

# Transactions: Processes, Sessions, Change Sets

## 0. Chapter header

- **Scope:** This chapter defines how every change happens:
  - the Process lifecycle;
  - the unit-of-work Session with its identity map;
  - ChangeSets and the staging operations;
  - the pre-flush pipeline and the single atomic flush;
  - optimistic concurrency, idempotency, provenance and version anchoring;
  - dry runs, and the direct `Mutate` path.
- **MOD:** `TX`
- **depends_on:** SPEC-02, SPEC-10, SPEC-11, SPEC-12, SPEC-13, SPEC-14, ADR-004.
- **Terms used:** TERM-Process, TERM-Session, TERM-ChangeSet, TERM-Flush, TERM-Cas, TERM-IdempotencyKey, TERM-Provenance, TERM-DryRun, TERM-ExecutionContext, TERM-OwnData, TERM-EffectiveData.
- **Origin summary:**
  - Verdicts: VRD-04-01…05, VRD-01-03, VRD-01-06.
  - UP: the process as the unit of change (CON-UP-017).
  - LC: change sets and the atomic commit step (CON-LC-012, CON-LC-022), the request envelope and idempotency (CON-LC-003, CON-LC-026), handler versions in the log (CON-LC-041).
  - US: the unit-of-work session and flush-on-success (CON-US-020, CON-US-021, CON-US-024).
  - UC: execution is commit, anchoring and provenance, idempotent worker (CON-UC-020, CON-UC-021, CON-UC-032).
  - LS: ChangeSet as the only write unit, the atomic pipeline, dry run (CON-LS-019, CON-LS-041, CON-LS-026).
  - UB: batch commit with CAS, self data (CON-UB-021, CON-UB-015).

---

## 1. Concepts

### 1.1 One process, one session, one flush

```
request ──► Process (id, principal, context, operation, clock)
               │
               ├─ Session (identity map: entity → loaded version + staged own data)
               │     ▲ reads (effective, read-your-writes)     │ staging (create/update/delete/…)
               │     │                                         ▼
               │  logic, actions, pipelines, slots  ──►  ChangeSets (one per entity × branch)
               │
               ├─ pre-flush: transform → before_* → compute → validate → cascade → authorise
               │
               └─ Flush: ONE database transaction ─► versions, heads, registry, index,
                                                      process_log, lineage, outbox
```

- Nothing reaches the store before the flush.
- A failure anywhere before or during the flush leaves the store unchanged, except for the FAILED or REJECTED audit row (REQ-STO-009). This makes "script failure means nothing written" hold (CON-US-024).

### 1.2 Process states

```
CREATED ─► RUNNING ─► PRE_FLUSH ─► FLUSHING ─► COMPLETED
              │            │            │
              │            │            └─► FAILED (store error, HEAD_CONFLICT)
              │            └─► REJECTED (validation or authorisation) / FAILED (slot error)
              └─► FAILED (logic error, timeout, limit) / REJECTED (operation denied)
```

- Only the terminal states are persisted in `process_log.status`: COMPLETED, FAILED and REJECTED.
- A dry run ends in `DRY_RUN_DONE`, which is not persisted.

---

## 2. Model

```yaml
ENT-Process:
  purpose: In-memory process object; persisted as one ENT-ProcessLogRow (SPEC-11) at its end.
  origin: [VRD-04-01, CON-UP-017, CON-UC-022, CON-LC-003]
  fields:
    - {name: process_id, type: uuid, required: true, description: "UUID v7, assigned at start"}
    - {name: kind, type: "enum{MUTATE|EMIT|QUERY|JOB|SYSTEM|BOOT|MERGE|ADMIN}", required: true, description: "Process kind"}
    - {name: operation, type: uri, required: true, description: "Operation URI with version (Native mutate for Mutate)"}
    - {name: operation_commit, type: commit_id?, required: false, description: "Version of the operation definition that ran (recorded as DEPENDS_ON_FUNC)"}
    - {name: tenant, type: tenant_code, required: true, description: "Host tenant"}
    - {name: principal, type: Principal, required: true, description: "Acting principal (SPEC-22)"}
    - {name: on_behalf_of, type: uuid?, required: false, description: "Delegating principal"}
    - {name: context, type: ResolvedContext, required: true, description: "Context, branch or pin, env vars (SPEC-16)"}
    - {name: branch, type: branch_name, required: true, description: "Default write branch"}
    - {name: level, type: "enum{DRAFT|PUBLISH}", required: true, description: "Validation level (SPEC-18); default by branch kind"}
    - {name: now, type: timestamp, required: true, description: "Process clock, fixed at start; exposed to logic as ctx.now"}
    - {name: request_id, type: string?, required: false, description: "UBTP request id"}
    - {name: idempotency_key, type: string?, required: false, description: "Client key"}
    - {name: parent_process_id, type: uuid?, required: false, description: "Starting process"}
    - {name: job_id, type: uuid?, required: false, description: "Job this process is an attempt of"}
    - {name: app_id, type: uuid?, required: false, description: "Initiating app/package"}
    - {name: dry_run, type: bool, required: true, description: "Discard flush"}
    - {name: state, type: "enum{CREATED|RUNNING|PRE_FLUSH|FLUSHING|COMPLETED|FAILED|REJECTED|DRY_RUN_DONE}", required: true, description: "§1.2"}
    - {name: provenance, type: "list<ProvenanceRecord>", required: true, description: "Recorded dependencies"}
    - {name: emits, type: "list<Emit>", required: true, description: "Collected emits (SPEC-19)"}
    - {name: warnings, type: "list<error>", required: true, description: "Non-fatal notices (e.g. URI.RENAMED, META.INSTANCES_AFFECTED)"}
```

```yaml
ENT-SessionEntry:
  purpose: One identity-map entry of a session.
  origin: [VRD-04-02, CON-US-020, CON-UB-015]
  fields:
    - {name: entity_id, type: uuid, required: true, description: "Identity (assigned for new entities, REQ-URI-001)"}
    - {name: locator, type: Locator, required: true, description: "tenant, type, slug"}
    - {name: branch, type: branch_name, required: true, description: "Branch the entry is staged on"}
    - {name: state, type: "enum{CLEAN|NEW|DIRTY|DELETED|UNSET|RENAMED}", required: true, description: "Change state"}
    - {name: loaded_commit, type: commit_id?, required: false, description: "Version resolved at first load (base of the change)"}
    - {name: loaded_found_on, type: branch_name?, required: false, description: "Branch the loaded version came from (own or inherited)"}
    - {name: expected, type: Expected, required: true, description: "CAS expectation for the flush (REQ-TX-005)"}
    - {name: own, type: json, required: true, description: "Current own data (loaded or staged)"}
    - {name: details, type: json?, required: false, description: "Action details (rename, copy, revert, merge)"}
    - {name: message, type: text?, required: false, description: "Commit message"}
  invariants:
    - At most one entry per (entity_id, branch) in a session.
```

```yaml
ENT-ChangeSet:
  purpose: The write unit produced from a non-CLEAN session entry at pre-flush (TERM-ChangeSet).
  origin: [VRD-04-02, CON-LS-019, CON-LC-012, CON-UB-021]
  fields:
    - {name: entity_id, type: uuid, required: true, description: "Entity"}
    - {name: locator, type: Locator, required: true, description: "Locator after the change"}
    - {name: branch, type: branch_name, required: true, description: "Target branch"}
    - {name: action, type: "enum{CREATE|UPDATE|DELETE|RENAME|COPY|MERGE|REVERT|UNSET}", required: true, description: "Commit action (UPSERT resolved at staging)"}
    - {name: own, type: json, required: true, description: "New own data ({} for UNSET)"}
    - {name: parent, type: commit_id?, required: false, description: "Parent commit (REQ-VER-001)"}
    - {name: merge_parent, type: commit_id?, required: false, description: "MERGE only"}
    - {name: expected, type: Expected, required: true, description: "Commit | Seq | Absent"}
    - {name: type_commit, type: commit_id?, required: false, description: "Type definition version validated against"}
    - {name: details, type: json?, required: false, description: "Action details"}
    - {name: message, type: text?, required: false, description: "Commit message"}
```

```yaml
ENT-ProvenanceRecord:
  purpose: One dependency of a process on a version (becomes a process_commit_map row).
  origin: [VRD-04-05, CON-UC-021, CON-UC-042, CON-LC-041]
  fields:
    - {name: commit_id, type: commit_id, required: true, description: "Version depended on"}
    - {name: entity_id, type: uuid, required: true, description: "Its entity"}
    - {name: relation, type: "enum{DEPENDS_ON_DATA|DEPENDS_ON_TYPE|DEPENDS_ON_FUNC|DEPENDS_ON_CTX|DEPENDS_ON_POLICY}", required: true, description: "Why it was read (REQ-TX-020)"}
```

```yaml
ENT-IdempotencyRecord:
  purpose: Runtime-store value under idem:{tenant}:{principal}:{key} (ADR-004).
  origin: [VRD-04-04, CON-LC-026, ADR-004]
  fields:
    - {name: state, type: "enum{IN_PROGRESS|DONE}", required: true, description: "FAILED requests are not recorded (a retry runs again)"}
    - {name: request_digest, type: bytes, required: true, description: "SHA-256 over canonical {operation, args, authority}"}
    - {name: process_id, type: uuid, required: true, description: "Process that owns or produced the result"}
    - {name: lease_until, type: timestamp?, required: false, description: "IN_PROGRESS lease (default now + 5 min)"}
    - {name: response, type: json?, required: false, description: "DONE - stored response (≤ 256 KiB; larger responses store a reference to the process)"}
    - {name: created_at, type: timestamp, required: true, description: "First seen"}
```

```yaml
ENT-MutateChange:
  purpose: One element of UBTP Mutate.changes (SPEC-24) - direct entity change.
  origin: [VRD-15-01, CON-LS-019, CON-LC-012]
  fields:
    - {name: uri, type: uri, required: true, description: "Target entity (short forms allowed, normalised)"}
    - {name: action, type: "enum{CREATE|UPDATE|UPSERT|DELETE|RENAME|COPY|UNSET}", required: true, description: "Requested action"}
    - {name: data, type: json?, required: false, description: "Full own data (CREATE, UPDATE, UPSERT)"}
    - {name: merge_patch, type: json?, required: false, description: "RFC 7396 merge patch on own data (UPDATE)"}
    - {name: patch, type: "list<json>?", required: false, description: "RFC 6902 JSON Patch on own data (UPDATE)"}
    - {name: expected_head, type: string?, required: false, description: "Commit id (digits) the client based its change on"}
    - {name: expected_seq, type: string?, required: false, description: "Alternative to expected_head"}
    - {name: new_slug, type: slug?, required: false, description: "RENAME target / COPY target slug"}
    - {name: source, type: uri?, required: false, description: "COPY source"}
    - {name: id, type: uuid?, required: false, description: "Proposed id for CREATE (REQ-URI-001)"}
    - {name: message, type: text?, required: false, description: "Commit message"}
  invariants:
    - Exactly one of data, merge_patch, patch for UPDATE; data for CREATE/UPSERT.
```

---

## 3. Behavior

### 3.1 Processes

### REQ-TX-001 — Every write runs in a process
- **Statement:** Every change to the six tables MUST happen inside exactly one process, written by that process's flush (REQ-ARCH-003). This covers Mutate, Emit, jobs, merges, genesis and package loads, admin changes and approvals. A process writes at most one flush. Several flushes need several processes (e.g. a job that commits in stages uses child processes).
- **Origin:** VRD-04-01, CON-UP-017, CON-UC-020
- **Acceptance:** Every version row joins exactly one COMPLETED process through an OUTPUT lineage row.
- **Priority:** P0

### REQ-TX-002 — Process start
- **Statement:** Starting a process MUST:
  - assign a UUID v7;
  - fix the process clock `now`;
  - capture the principal and on-behalf-of principal, the resolved context (SPEC-16), the write branch (default the context's branch) and the validation level;
  - record the operation URI, and resolve the operation's definition version, recorded as DEPENDS_ON_FUNC.

  Logic MUST read time only from `now` (`ctx.now`). Wall-clock syscalls are not provided (SPEC-17), which keeps the execution replayable (VRD-05-06).
- **Origin:** VRD-04-01, VRD-05-06, CON-UC-022
- **Acceptance:** Two calls of `ctx.now` in one process return the same value. The process log `started_at` equals `now`.
- **Priority:** P0

### REQ-TX-003 — Validation level by branch
- **Statement:** The default level MUST be DRAFT for writes to DRAFT branches and PUBLISH for all others. A request MAY ask for DRAFT on a non-DRAFT branch only if the principal holds `ubos.tx.draft_level` on that branch (SPEC-22). Merges into MAIN and RELEASE always use PUBLISH (REQ-VER-010).
- **Origin:** VRD-09-02, VRD-08-03, CON-LC-023
- **Acceptance:** A draft save with a missing required property succeeds. The same save on main fails with `RULE.VALIDATION_FAILED`.
- **Priority:** P0

### 3.2 Session

### REQ-TX-004 — Identity map and read-your-writes
- **Statement:** The session MUST keep one entry per (entity, branch).
  - The first read of an entity loads its resolved version (SPEC-14 §1.2) and records it as `loaded_commit`.
  - Later reads return the entry's current own data composed into effective data (SPEC-13 REQ-META-020), including staged changes.
  - Entities created in the session are readable by locator and ID at once.
  - Queries (SPEC-21) run inside a process see committed data, overlaid with the session's staged entries of the queried type. This is best-effort for filters: staged entries are re-checked against the criteria in memory.
- **Rationale:** Logic sees its own changes (US), and each entity has one identity within a process.
- **Origin:** VRD-04-02, CON-US-020, CON-US-021
- **Acceptance:**
  1) Logic that creates `Invoice/inv-9` and then calls `entity.get("Invoice/inv-9")` gets the staged data.
  2) A query for invoices in the same process includes it.
- **Priority:** P0

### REQ-TX-005 — Expected heads
- **Statement:** Each non-CLEAN entry MUST carry an expectation for CAS (REQ-STO-012):
  1) If the request or logic supplies `expected_head` or `expected_seq`, that value is used. It must equal the head that the version at load time came from; otherwise the request fails with `VER.HEAD_CONFLICT` immediately.
  2) Otherwise, if the entity has an own head on the branch at load, `Expected::Commit(loaded_commit)` is used.
  3) Otherwise (inherited or new), `Expected::Absent` is used.

  A type MAY declare `require_expected_head: true` (a SPEC-13 display/behaviour option). Client Mutates of such types must then supply the head explicitly (`TX.EXPECTED_HEAD_REQUIRED`).
- **Rationale:** Every write names its base (UB), and long-lived UIs can detect lost updates across requests.
- **Origin:** VRD-04-03, VRD-01-03, CON-UB-021, CON-UC-020
- **Acceptance:** A UI that loaded commit 40 and saves after another user committed 41 gets `VER.HEAD_CONFLICT` with `{expected: "40", actual: "41"}`.
- **Priority:** P0

### REQ-TX-006 — Staging operations
- **Statement:** The session MUST provide these staging operations. Each is validated only structurally at staging time; full validation runs at pre-flush.

| Operation | Effect on the entry | Resulting action |
|---|---|---|
| `create(locator or uri, own, id?)` | new entry, state NEW; fails `URI.LOCATOR_TAKEN` if resolvable | CREATE |
| `upsert(uri, own)` | update if resolvable, else create | UPDATE / CREATE |
| `update(uri, own)` | replace own data | UPDATE (or CREATE if the entity exists only by inheritance on this branch and the branch is not MAIN: an overriding version with action UPDATE and parent = inherited version) |
| `patch(uri, merge_patch or json_patch)` | apply to current own data | UPDATE |
| `set_effective(uri, desired)` | own data := `own_data_for(desired, inherited, …)` (REQ-META-022) | UPDATE |
| `delete(uri)` | state DELETED | DELETE (masking tombstone on non-MAIN, REQ-VER-004) |
| `unset(uri)` | state UNSET | UNSET |
| `rename(uri, new_slug)` | state RENAMED, `details {old_slug, new_slug}` | RENAME |
| `copy(source_uri, target_locator)` | new entry with the source's effective own data, `details {source_uri}` | COPY |
| `revert(uri, to_commit)` / merge ops | via SPEC-14 | REVERT / MERGE |

  Several operations on the same entry in one process MUST collapse into **one** change set, which gives one commit per entity and branch per process. The collapse follows these rules:
  - CREATE followed by UPDATE is a CREATE with the final data.
  - CREATE followed by DELETE drops the entry.
  - UPDATE followed by DELETE is a DELETE.
  - RENAME combined with UPDATE is a RENAME commit carrying the new data.
- **Origin:** VRD-04-02, CON-LS-019, CON-LC-012, CON-UB-015
- **Acceptance:**
  1) Logic that updates the same invoice three times produces one version.
  2) Create then delete in one process writes nothing for that entity.
- **Priority:** P0

### REQ-TX-007 — Status and write-mode checks at staging
- **Statement:** Staging MUST reject writes early:
  - to ARCHIVED or LOCKED entities (REQ-STO-021), unless the process kind is ADMIN;
  - to branches whose `write_mode` forbids them (REQ-VER-005);
  - of versions of another tenant's entities (REQ-STO-006).

  These checks are repeated inside the flush transaction.
- **Origin:** REQ-STO-021, REQ-VER-005, REQ-STO-006
- **Acceptance:** Updating a LOCKED entity fails at staging with `STO.ENTITY_LOCKED`, before any logic continues.
- **Priority:** P0

### 3.3 Pre-flush and flush

### REQ-TX-010 — Pre-flush pipeline
- **Statement:** When the operation's logic has finished, the process MUST run the following steps over the non-CLEAN entries:
  1) **transform:** property `transform` slots on changed values.
  2) **before_\*:** type-level `before_create`, `before_update` and `before_delete` slots. They may stage further changes and emits. Steps 1–2 repeat for newly staged or changed entries until nothing changes, at most 8 rounds (`TX.PREFLUSH_NOT_CONVERGING`).
  3) **defaults and compute:** required defaults at CREATE (REQ-META-012) and WRITE-mode `compute` slots.
  4) **validate** at the process level (SPEC-18). All failures are collected.
  5) **cascade:** `cascade` slots, whose staged changes pass through steps 1–4 for the affected entries, at most 8 rounds.
  6) **authorise** each change set for its action and fields (SPEC-22, write checks and `access` slots).
  7) **dependency_check** for DELETE and UNSET entries.

  A veto or error in any step ends the process: REJECTED for validation and authorisation, FAILED for slot errors. No flush happens.
- **Rationale:** This fixes one order for LS's mechanism pipeline (resolution, policy, prerequisite, schema, transformation, implement), in which judging steps are pure and staging steps come first.
- **Origin:** VRD-09-01, VRD-02-04, CON-LS-008, CON-LS-027, CON-LC-020
- **Acceptance:** A `before_update` slot that sets `total` is visible to validation. A `cascade` that updates a parent order is validated too.
- **Priority:** P0

### REQ-TX-011 — Flush algorithm
- **Statement:** The flush MUST execute in one store transaction (API-STO-001), bound to the host tenant, in this order:
  1) **Order and lock.** Sort the change sets by (entity_id, branch). For each one, lock the head row, or confirm its absence.
     - PostgreSQL: `SELECT … FOR UPDATE` on `branch_head`; for absent heads, rely on the unique constraint.
     - SQLite: the `BEGIN IMMEDIATE` transaction is exclusive.
  2) **CAS pre-check.** Compare all expectations. If any mismatch, abort with **one** `VER.HEAD_CONFLICT` error that lists every conflicting entity in `details.conflicts`.
  3) **Registry.** Upsert or lock the registry rows (REQ-STO-002: locator reuse, status rules, rename).
  4) **No-op filter.** Drop UPDATE change sets whose canonical snapshot hash equals the parent's (REQ-STO-004).
  5) **Versions.** Insert the versions, which yields the commit IDs.
  6) **Heads.** Insert, advance or remove heads (REQ-STO-005, REQ-STO-012).
  7) **Registry updates.** `touch` (last_modified, display_name from `title` or the `summary` slot, tags); status on DELETE and restore on `main`; slug on RENAME on `main`.
  8) **Index.** Insert search index rows (REQ-STO-007).
  9) **Process log.** Insert the COMPLETED row (REQ-STO-009).
  10) **Lineage.** Insert OUTPUT and dependency rows (REQ-STO-010, REQ-TX-020).
  11) **Outbox.** Insert rows (REQ-STO-011) for:
      - `entity.committed` events, one per commit;
      - a `process.completed` event;
      - deferred emits (SPEC-19);
      - job signals for Jobs created in this process (SPEC-20).
  12) **Commit.**

  On any error: roll back, set the process state to FAILED, and write the FAILED row in a new transaction (REQ-STO-009).
- **Rationale:** This combines the orders of UP, LS, UC and LC (instance → version → head → index → log → lineage) with UB's CAS and the outbox.
- **Origin:** VRD-04-02, VRD-04-03, VRD-11-01, CON-LS-041, CON-UC-041, CON-LC-022, CON-UP-044
- **Acceptance:**
  1) A fault injected after each step leaves no rows of the process in the six tables, apart from the FAILED process row.
  2) A two-entity conflict reports both entities.
- **Priority:** P0

### REQ-TX-012 — Empty processes
- **Statement:** A write-capable process that ends with no change sets (after the no-op filter) and no emits MUST NOT open a store write transaction. It completes with `commit_count = 0` and writes a process log row only if the operation declares `audit_always: true` (e.g. approvals, admin checks) or read auditing is on.
- **Origin:** NEW: avoids log noise; consistent with REQ-ARCH-005
- **Acceptance:** An idempotent "save without change" returns COMPLETED with no commits and no process row.
- **Priority:** P1

### REQ-TX-013 — Conflict handling and retry
- **Statement:** The kernel MUST NOT retry a process on its own after `VER.HEAD_CONFLICT`, with two exceptions:
  - An operation may declare `retry_on_conflict: n` (n ≤ 3). The kernel then re-runs the whole process from a fresh session, with a new process ID, the same idempotency reservation and `parent_process_id` = the failed one.
  - Jobs retry per their policy (SPEC-20).

  Clients receive the conflict with `retryable: true` and SHOULD reload and re-apply.
- **Origin:** VRD-04-03, CON-UP-002 (transient retry)
- **Acceptance:** An operation with `retry_on_conflict: 2` succeeds under a single concurrent conflicting write, and its audit shows two processes.
- **Priority:** P1

### 3.4 Idempotency

### REQ-TX-015 — Idempotency protocol
- **Statement:** For a request with an idempotency key *k*:
  1) Compute the digest *d* over the canonical `{operation, args, authority}`.
  2) `set_nx` the record `idem:{tenant}:{principal}:{k}` as `IN_PROGRESS {d, process_id, lease_until = now + 5 min}`.
  3) If the record exists:
     - DONE with digest *d*: return the stored response with `replayed: true` and no new process.
     - DONE with a different digest: fail with `TX.IDEMPOTENCY_MISMATCH`.
     - IN_PROGRESS with a live lease: fail with `TX.IDEMPOTENCY_IN_PROGRESS` (CONFLICT, retryable).
     - IN_PROGRESS with an expired lease: take over by CAS and run.
  4) On COMPLETED, store DONE with the response and a TTL of 24 h (tenant-configurable up to 7 d).
  5) On FAILED or REJECTED, delete the record, so that a retry runs again.
  6) If the runtime store is unavailable or lost the key, and `process_log` has a COMPLETED row with the same `(tenant, idempotency_key)` whose `input_digest` equals *d*, return a minimal replay `{process_id, commits, replayed: true, partial: true}` instead of running again.
- **Rationale:**
  - This is LC's protocol (5-minute stale takeover) placed in the runtime store (ADR-004).
  - The process log is a durable fallback beyond the TTL and across runtime-store loss.
- **Origin:** VRD-04-04, ADR-004, CON-LC-026, CON-LC-003
- **Acceptance:**
  1) A duplicate within the TTL returns the same response and creates no second process.
  2) A different payload with the same key fails with `TX.IDEMPOTENCY_MISMATCH`.
  3) After a runtime-store flush, a duplicate is still not re-executed (fallback 6).
- **Priority:** P0

### 3.5 Provenance and anchoring

### REQ-TX-020 — Recording dependencies
- **Statement:** The session and the kernel MUST record a ProvenanceRecord for every version the process used:

| Relation | Recorded when | Corpus name |
|---|---|---|
| DEPENDS_ON_DATA | an entity version is read through the session (including resolved references, prototypes and query results actually returned to logic) | READ (VRD-04-05) |
| DEPENDS_ON_TYPE | a type, TypeExtension, Behavior or Dictionary version is used for effective data or validation | DEPENDS_ON_TYPE |
| DEPENDS_ON_FUNC | the operation definition, and every Logic, Pipeline or Action version executed | DEPENDS_ON_FUNC (UC), DEPENDS_ON_LOGIC |
| DEPENDS_ON_CTX | the Context entity version used | DEPENDS_ON_CTX (UC), DEPENDS_ON_CONTEXT |
| DEPENDS_ON_POLICY | each Policy, Role or grant Relationship version that decided an authorisation | NEW |

  Records are de-duplicated. If a process records more than 10 000 records, the kernel keeps FUNC, CTX, TYPE and POLICY records, drops the excess DATA records, and sets `metrics.provenance_truncated = true`.
- **Rationale:** "Which code and which environment produced this version" is queryable (UC). Policy lineage explains access decisions (SPEC-22, SPEC-27 explanations).
- **Origin:** VRD-04-05, VRD-21-02, CON-UC-021, CON-UC-042, CON-LC-041
- **Acceptance:** The lineage of a process that ran Logic `calc.tax@v2` on Context `acme.prod` and read two customers shows FUNC, CTX and two DATA records with the exact commits.
- **Priority:** P0

### REQ-TX-021 — Anchoring for deferred execution
- **Statement:** When a process creates work that runs later (a Job, a deferred emit, an ApprovalRequest), it MUST pin **anchors**:
  - the commit of the operation or Logic definition;
  - the Context commit;
  - the branch;
  - the type chain fingerprint of the target type.

  The later process MUST execute the anchored versions, never the latest ones. If an anchored version is unreadable, the process fails with `TX.ANCHOR_MISSING`. A Job MAY declare `anchor: LATEST` for maintenance jobs; this is recorded in the process log.
- **Rationale:** Deterministic replay and "error scene reconstruction" (UC, "strictly no load_latest").
- **Origin:** VRD-04-05, VRD-05-06, CON-UC-021
- **Acceptance:** A job enqueued before a Logic update runs the old Logic version, and its lineage shows the old commit.
- **Priority:** P0

### 3.6 Dry run

### REQ-TX-025 — Dry run
- **Statement:** Any Mutate or Emit MAY be requested with `dry_run: true`. The process then runs every step up to and including pre-flush (REQ-TX-010), but performs no flush. It returns:
  - the change sets, as JSON Patch diffs against their bases;
  - all decisions and validation results;
  - the emits that would be dispatched (not dispatched);
  - warnings and the trace summary.

  A dry run writes no process log row, reserves no idempotency key, and runs slots and logic under a profile that fails external side-effect syscalls (e.g. `http.*`, `ai.*` unless the Context allows it) with `RT.DRY_RUN_BLOCKED`.
- **Rationale:** "Dry run as a first-class mode" (LS). It is also the basis of AI building (SPEC-27) and UI previews.
- **Origin:** CON-LS-026, CON-UC-025, VRD-16-01
- **Acceptance:** A dry run of an invoice posting returns the diffs of 3 entities and 1 pending emit, and the store is unchanged.
- **Priority:** P0

### 3.7 Direct Mutate

### REQ-TX-030 — Mutate path
- **Statement:** UBTP `Mutate{changes[], expected_heads?, idempotency_key?, dry_run?, level?, message?}` MUST run as a process of kind MUTATE, with operation `ubos://system/Native/mutate@v1`.
  - Each `MutateChange` (§2) is resolved (REQ-URI-009, intent Write or Create) and staged (REQ-TX-006).
  - The request is authorised per change with the permission `entity.<action>` on the target (SPEC-22).
  - The process then runs pre-flush and flush.
  - Mutate never runs Action logic. Business operations with state machines MUST be invoked with `Emit` (SPEC-19).
  - A type MAY declare `mutate: DENY` (direct writes only through actions) or `mutate: ADMIN_ONLY`.
- **Rationale:** Generic CRUD stays available for data maintenance and tools (UC `/commit`, FU Studio), while governed types force actions.
- **Origin:** VRD-15-01, CON-LS-004, CON-UC-031
- **Acceptance:**
  1) Mutate of a type with `mutate: DENY` fails with `TX.MUTATE_DENIED`.
  2) A 3-change Mutate commits atomically.
- **Priority:** P0

### REQ-TX-031 — Process result
- **Statement:** A completed process MUST return the following result: `{process_id, status, commits: [{uri (canonical with commit), entity_id, commit_id, action, branch}], result (operation output, SPEC-19), warnings, replayed?}`. A failed or rejected process returns the error object (SPEC-02 §8) with `process_id` set.
- **Origin:** VRD-15-01, CON-LS-041
- **Acceptance:** Every commit URI in a result resolves to exactly that version.
- **Priority:** P0

### 3.8 Limits

### REQ-TX-035 — Process limits
- **Statement:** A process MUST respect these limits, configurable per tenant within host maxima:
  - change sets ≤ 10 000;
  - total snapshot bytes staged ≤ 64 MiB;
  - session entries ≤ 50 000;
  - pre-flush rounds ≤ 8 (+ 8 cascade);
  - synchronous wall time ≤ `request_timeout` (default 30 s);
  - job wall time ≤ the job's timeout (SPEC-20).

  Exceeding a limit fails with `TX.LIMIT` and `details.limit`.
- **Origin:** NEW: bounded resources; CON-UC-035 (robustness NFRs)
- **Acceptance:** A Mutate with 10 001 changes fails with `TX.LIMIT`.
- **Priority:** P1

---

## 4. Interfaces

### API-TX-001 — ProcessManager
- **Kind:** rust trait (`ubos_kernel::tx`)
- **Signature / shape:**
```rust
#[async_trait]
pub trait ProcessManager: Send + Sync {
    async fn begin(&self, spec: ProcessSpec) -> Result<ProcessHandle, UbosError>;
}
pub struct ProcessSpec {
    pub kind: ProcessKind, pub operation: String, pub principal: Principal,
    pub context: ResolvedContext, pub branch: Option<BranchName>, pub level: Option<Level>,
    pub request_id: Option<String>, pub idempotency_key: Option<String>, pub args_digest: Option<[u8; 32]>,
    pub parent_process_id: Option<Uuid>, pub job_id: Option<Uuid>, pub app_id: Option<Uuid>,
    pub anchors: Option<Anchors>, pub dry_run: bool,
}
pub struct ProcessHandle { /* owns Process + Session + ExecutionContext */ }
impl ProcessHandle {
    pub fn ctx(&self) -> &ExecutionContext;
    pub fn session(&mut self) -> &mut Session;
    pub fn emit(&mut self, e: Emit);
    pub fn warn(&mut self, w: UbosError);
    pub async fn finish(self, result: serde_json::Value) -> Result<ProcessResult, UbosError>; // pre-flush + flush
    pub async fn abort(self, err: UbosError) -> UbosError;                                   // FAILED/REJECTED row
}
```
- **Semantics:** `begin` performs the idempotency step (REQ-TX-015) and may return `Err(Replay(response))`. `finish` runs REQ-TX-010 and REQ-TX-011, or the dry-run variant.
- **Origin:** CON-UP-017 (startProcess), CON-UC-006 (loader → runner → committer)

### API-TX-002 — Session
- **Kind:** rust struct
- **Signature / shape:**
```rust
impl Session {
    pub async fn get(&mut self, uri: &str, opts: ResolveOpts) -> Result<EffectiveEntity, UbosError>;
    pub async fn get_own(&mut self, uri: &str) -> Result<OwnView, UbosError>;
    pub async fn create(&mut self, uri: &str, own: Value, id: Option<Uuid>) -> Result<Uuid, UbosError>;
    pub async fn upsert(&mut self, uri: &str, own: Value) -> Result<Uuid, UbosError>;
    pub async fn update(&mut self, uri: &str, own: Value, expected: Option<Expected>) -> Result<(), UbosError>;
    pub async fn patch(&mut self, uri: &str, patch: Patch, expected: Option<Expected>) -> Result<(), UbosError>;
    pub async fn set_effective(&mut self, uri: &str, desired: Value, pin_keys: &[String]) -> Result<(), UbosError>;
    pub async fn delete(&mut self, uri: &str, expected: Option<Expected>) -> Result<(), UbosError>;
    pub async fn unset(&mut self, uri: &str) -> Result<(), UbosError>;
    pub async fn rename(&mut self, uri: &str, new_slug: &Slug) -> Result<(), UbosError>;
    pub async fn copy(&mut self, source: &str, target: &str) -> Result<Uuid, UbosError>;
    pub fn record(&mut self, p: ProvenanceRecord);
    pub fn staged(&self) -> impl Iterator<Item = &SessionEntry>;
}
```
- **Origin:** CON-US-020, CON-US-021

### API-TX-003 — UBTP Mutate message
- **Kind:** UBTP message (SPEC-24)
- **Signature / shape:**
```json
{ "op": "Mutate", "request_id": "r-17", "idempotency_key": "k-9a",
  "dry_run": false, "level": "PUBLISH", "message": "fix address",
  "changes": [
    { "uri": "Customer/c-001", "action": "UPDATE",
      "merge_patch": { "address": { "city": "Hangzhou" } }, "expected_head": "41" },
    { "uri": "Note/n-77", "action": "CREATE", "data": { "title": "call back", "customer": "ubos://acme/Customer/c-001" } }
  ] }
```
- **Semantics:** REQ-TX-030 and REQ-TX-031. The response is `Result{process result}` or `Error`.
- **Origin:** VRD-15-01

### Error codes (TX)

| Code | Category | Meaning |
|---|---|---|
| `TX.IDEMPOTENCY_MISMATCH` | CONFLICT | same key, different request |
| `TX.IDEMPOTENCY_IN_PROGRESS` | CONFLICT (retryable) | same key still running |
| `TX.EXPECTED_HEAD_REQUIRED` | INVALID | type requires explicit expected head |
| `TX.PREFLUSH_NOT_CONVERGING` | INTERNAL | before_*/cascade loop exceeded 8 rounds |
| `TX.ANCHOR_MISSING` | NOT_FOUND | anchored version unreadable |
| `TX.MUTATE_DENIED` | FORBIDDEN | type forbids direct Mutate |
| `TX.LIMIT` | LIMIT | process limit exceeded |
| `TX.TIMEOUT` | UNAVAILABLE | process exceeded its time budget |
| `TX.ABORTED` | INTERNAL | process aborted by shutdown or admin |

(`VER.HEAD_CONFLICT`, `RULE.VALIDATION_FAILED`, `SEC.*` and `RT.*` come from their chapters.)

---

## 5. Non-functional

| ID | Requirement | Target |
|---|---|---|
| NFR-PERF-050 | Mutate of 1 entity end to end (HTTP binding, PG, warm caches, no logic) | p95 ≤ 25 ms |
| NFR-PERF-051 | Emit of an action with 1 Rhai step writing 3 entities | p95 ≤ 60 ms |
| NFR-DUR-050 | No partial process | 0 partial flushes under fault injection |
| NFR-AVAIL-050 | Idempotency across runtime-store loss | no duplicate execution within process-log retention (REQ-TX-015 step 6) |

---

## 6. Acceptance

| REQ | Criterion |
|---|---|
| REQ-TX-001…003 | Every version has one process; fixed clock; level by branch |
| REQ-TX-004…007 | Read-your-writes; expectation rules; collapse rules; early status checks |
| REQ-TX-010…013 | Pre-flush order; flush fault injection; empty process; retry declaration |
| REQ-TX-015 | Duplicate, mismatch, in-progress, takeover and fallback cases |
| REQ-TX-020, 021 | Lineage records per relation; anchored job runs the old logic |
| REQ-TX-025 | Dry run returns diffs and emits; store unchanged; external syscalls blocked |
| REQ-TX-030, 031 | Mutate atomicity and deny; result URIs resolve |
| REQ-TX-035 | Limits enforced |

---

## 7. Implementation notes

- **Reference code:**
  - US session [US:crates/ubos_kernel/src/session/manager.rs#L9-L141], sync executor [US:crates/ubos_kernel/src/executor/sync.rs#L68-L93].
  - LS writer [LS:crates/ubos_store/src/engine/writer.rs#L19-L77], ChangeSet [LS:crates/ubos_proto/src/change_set.rs#L10-L52].
  - LC engine and persistence step [LC:src/main/java/com/logicorum/core/LogicExecutionEngine.java], idempotency [LC:src/main/java/com/logicorum/service/IdempotencyService.java].
  - UC runtime writer [UC:src/infra/store/writer/runtime.rs#L11-L98], loader with anchors [UC:src/kernel/executor/worker/loader.rs#L27-L87].
- **Gaps closed:**
  - US flushed entity by entity in separate transactions and never checked `loaded_commit_id` (CON-US-025).
  - UC committed jobs and business writes separately in US-style flows.
  - Here the flush is one transaction, with CAS on every entry.
- **Deadlocks:** sorting change sets by (entity_id, branch) before locking gives every flush the same lock order.
- **Rhai bridge:** the session lives in the execution context. Syscalls call it synchronously through a handle that blocks on the async store from the blocking pool (REQ-CONV-084, CON-UC-005).

## 8. Open questions

None. Deferred: a saga across several processes (compensation) is expressed with Process Instances (SPEC-19), not with distributed transactions.
