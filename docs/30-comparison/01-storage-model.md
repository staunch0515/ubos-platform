---
id: CMP-01
title: "Comparison: Versioned Storage Model"
status: complete
phase: P2
depends_on: [CMP-00]
sources: [UP, FU, LC, UB, UC, US, LS]
---

# Comparison: Versioned Storage Model

## 1. Question

How does the platform physically store every artifact (data, metadata, logic, UI, configuration) so that it is versioned, branchable, auditable, searchable and multi-tenant, with a fixed schema?

## 2. Candidates

| KEY | Approach | Refs | Summary |
|---|---|---|---|
| UP | 3+4 tables, full JSON snapshots | CON-UP-040, 042, 044, HL-UP-001 | Instance, version chain (full snapshot + parent), branch head; search index, branch config, process log/map, relation table added by migrations; no tenant column |
| FU | UP schema + tenant-aware kernel | CON-FU-012, 015, 016, HL-FU-012 | UP tables with tenant scope, tombstone commits, stable UUID with mutable slug |
| LC | Typed EAV per commit | CON-LC-040, 042, 043, HL-LC-012 | Version content stored as typed attribute rows per commit; commit types; operational tables beside |
| UB | Schema v2.1, six tables, partitioned | CON-UB-040…047, HL-UB-008 | Tenant column, locator unique, status/tombstone/owner, JSONB + GIN, vector(1536), yearly partitions, `seq_num` CAS, app/billing on process log |
| UC | Schema v2.1 used as-is, "6 tables only" rule | CON-UC-040, 041, 042 | Same DDL as UB; subset of columns used; typed process→commit map |
| US | Same as UC | CON-US-040 | Byte-identical DDL |
| LS | Schema v2.1, SQLite dialect + PG backend | CON-LS-006, 040, 041, 042 | Same logical tables behind store traits; commit writes index and lineage atomically |

## 3. Dimension matrix

| Dimension | UP | FU | LC | UB | UC/US | LS |
|---|---|---|---|---|---|---|
| Fixed generic schema | ● | ● | ● | ● | ● (rule) | ● |
| Snapshot format | full JSON | full JSON | typed EAV | full JSONB | full JSONB | full JSON (TEXT/JSONB) |
| Historical read cost | O(1) | O(1) | O(attrs) join | O(1) | O(1) | O(1) |
| Attribute query | index table (stub) | index + slug | native SQL | index table (typed) | name/containment | index populated |
| Tenant column | – | ● | app scoping | ● | ● | ● (single tenant) |
| Lifecycle status / tombstone | – | tombstone | commit type DELETE | ● + terminator | status only | status only |
| Concurrency control | – | – | optimistic handler | head CAS `seq_num` | head check + seq | seq only |
| Partitioning / archiving | – | – | – | ● yearly | ● (DDL) | – (SQLite) |
| Vector embedding per version | – | – | – | ● | column unused | BLOB |
| Backend portability | PG | PG | PG | PG | PG | SQLite + PG |
| AI-friendliness (one JSON per version) | ● | ● | ○ | ● | ● | ● |
| Fit with "everything is an entity" | ● | ● | ● | ● | ● | ● |

## 4. Analysis

- **Convergence (fact).** Five of seven backend repositories share the same logical model: instance registry, version chain, branch head, search index, process log and process-entity map. UB, UC, US and LS use the identical v2.1 DDL. The model is the corpus's strongest consensus (UP HL-UP-001 → UB HL-UB-008).
- **Snapshot vs EAV.**
  - Full JSON snapshots (UP/FU/UB/UC/US/LS) give O(1) time travel, schema-free evolution, and a single document that AI and clients can read.
  - LC's typed EAV makes attribute and temporal queries plain SQL (HL-LC-012), but it fragments documents and makes nested structures awkward.
  - *Interpretation:* keep the JSON snapshot as the truth and derive EAV rows into the search index, which is UB's index design, implemented in LS (CON-LS-042).
- **Registry richness.** UB's instance row carries status, `terminator_commit_id`, display name, tags, owner and visibility, so list views and custody queries need no JSON parsing. FU and UB agree on deletion by tombstone commit (CON-FU-015, CON-UB-044).
- **Concurrency.**
  - UB defines per-(entity, branch) head CAS with `seq_num` ("block height per universe", CON-UB-041).
  - UC compares head commit ids before commit (CON-UC-020).
  - LC has an optimistic-lock handler.
  - *Interpretation:* per-branch CAS is the most precise of these.
- **Operational tables.**
  - UC's rule ("6 tables, never add tables", CON-UC-040) keeps business, metadata and technical state in one model.
  - LC keeps idempotency keys, request logs and an outbox in extra tables (CON-LC-043), and UC keeps queues in Redis.
  - *Interpretation:* the rule should apply to business and metadata state. Transient technical state (queues, caches, idempotency records) needs a declared home.
- **Portability.** LS proves the same model runs on SQLite (edge/personal) and PostgreSQL (enterprise) behind store traits (CON-LS-006).
- **Scale notes.** UB records hot-head, embedding, partition and RLS concerns (D-UB-12), which are the only explicit scalability analysis in the corpus.

## 5. Verdicts

### VRD-01-01 — Canonical physical model: the six-table schema v2.1 (fusion UB + LS + UC)
- **Decision:** fusion — UB's schema v2.1 as the logical model, UC's "six tables for business/meta state" rule, LS's backend abstraction.
- **Rationale:**
  - It is the consensus design and already carries tenant, status, owner, partitions, vectors and billing hooks.
  - One model for all artifacts keeps AI generation and tooling uniform.
- **Consequences:** Spec defines ENT for `entity_instance`, `entity_version`, `branch_head`, `search_index`, `process_log` and `process_commit_map` with the v2.1 columns. Table prefix and naming are fixed in P3 conventions.
- **Dropped:**
  - UP's separate `sys_branch_config` table; branch configuration becomes entities (see VRD-08-01).
  - UP's `lcm_entity_relation` edge table; relations become entities (see VRD-02-03).

### VRD-01-02 — Full JSON snapshot is the source of truth; attribute index is derived (fusion UP/UB + LC)
- **Decision:** fusion.
- **Rationale:** O(1) time travel and document integrity (UP/UB), with LC's queryability recovered through a derived typed index populated at commit (UB design, LS implementation).
- **Consequences:**
  - The search index row carries `(commit_id, prop_path, val_text | val_num | val_date | val_bool)`.
  - Indexing rules come from type metadata (`is_searchable`, CON-UC-017), not "all top-level fields".
- **Dropped:** EAV as primary storage (LC).

### VRD-01-03 — Per-branch head CAS with branch height (best-of UB)
- **Decision:** best-of UB.
- **Rationale:** Precise optimistic concurrency that never blocks other branches; composes with UC's execution commit check.
- **Consequences:**
  - Every write names its expected head (or height).
  - A mismatch fails with a conflict error that clients can merge or retry.
- **Dropped:** unconditional head overwrite (UP/LS).

### VRD-01-04 — Deletion is a tombstone commit plus registry status (fusion UB + FU)
- **Decision:** fusion.
- **Consequences:**
  - Status values: ACTIVE, DELETED, ARCHIVED, LOCKED.
  - `terminator_commit_id` is recorded on delete; history stays readable.
- **Dropped:** hard deletes.

### VRD-01-05 — Storage backend abstraction with two editions (best-of LS)
- **Decision:** best-of LS.
- **Consequences:**
  - The store is a set of operation traits (instance, version, head, index, process log, lineage, reader).
  - The PostgreSQL edition has JSONB, partitions and a vector index; the SQLite edition has TEXT JSON and no partitions.
  - The spec states which features degrade in the personal edition (vector search, partition archiving).

### VRD-01-06 — Technical state outside the six tables must be declared (NEW)
- **Decision:** NEW.
- **Rationale:** Resolves the tension between UC's "never add tables" rule and LC's and UC's need for queues, idempotency records and caches.
- **Consequences:**
  - Business and metadata state lives only in the six tables.
  - Transient technical state (job queue, event bus, cache, idempotency keys, rate limits) lives in a declared runtime store (e.g. Redis, or technical tables in a separate schema).
  - Nothing in that runtime store is authoritative; anything that must be audited becomes an entity.
- **Open:** Q-005.

## 6. Open questions

- Q-005: Idempotency records and job records — entity (auditable, versioned) or technical store (fast, TTL)? Candidate answer: jobs as entities (UC, CON-UC-013); idempotency keys technical with TTL (LC, CON-LC-026).
