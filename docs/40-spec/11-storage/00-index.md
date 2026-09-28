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

## Parts of this chapter

This chapter is split into files (WRITING-RULES R2.2). Read them in order.

| File | Sections |
|---|---|
| `00-index.md` (this file) | §0, §1, §6, §7, §8 |
| `01-model.md` | Model — §2 |
| `02-behavior.md` | Behavior and non-functional — §3, §5 |
| `03-interfaces.md` | Interfaces — §4 |

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
