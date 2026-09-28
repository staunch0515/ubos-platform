---
id: CMP-12
title: "Comparison: Query and Search"
status: complete
phase: P2
depends_on: [CMP-01, CMP-02]
sources: [UP, FU, LC, UB, UC, US, LS, UW]
---

# Comparison: Query and Search

## 1. Question

How do users, scripts and AI find and filter entities? This covers attribute queries, full text, relationships, history and semantic similarity, over a generic JSON-snapshot store.

## 2. Candidates

| KEY | Approach | Refs | Summary |
|---|---|---|---|
| UP | Commit-scoped property index (write stub); search by type/branch/filter | CON-UP-042 | First index design |
| FU | Global search (index text + slug); field-level blame by JSONB window functions; process audit queries; metrics | CON-FU-040…044, HL-FU-009 | History analytics |
| LC | Typed EAV → plain SQL for attributes and temporal validity; query engine over processes (GET) | CON-LC-040, 002, HL-LC-012, 008 | Queryability first |
| UB | Typed index at commit (text/num/date); SearchCriteria (EQ/GT/LT/BETWEEN/LIKE/IN, sort, paging); inheritance index design; vector(1536) + ivfflat | CON-UB-042, 065, HL-UB-008 | Richest query model |
| UC/US | Name ILIKE; JSONB `@>` (design); history syscall | CON-UC-046, 031 | Minimal |
| LS | Index populated on every commit; criteria reader | CON-LS-042, 043 | Implemented index |
| UW | Fuzzy client-side search over features and entities | CON-UW-021 | UX |

## 3. Dimension matrix

| Dimension | UP | FU | LC | UB | UC | LS |
|---|---|---|---|---|---|---|
| Typed attribute filters | ◐ | ◐ | ● | ● | ◐ | ◐ |
| Index populated on commit | stub | stub | native | ● | – | ● |
| Full text | – | ● ILIKE | – | LIKE | ILIKE name | – |
| Relationship traversal | edges | – | ● SQL | design | – | – |
| History / blame queries | audit | ● | temporal | history | ● history | – |
| Vector similarity | – | config | – | ● | column | – |
| Query as a declared operation | – | – | ● GET process | – | eval | Query op |

## 4. Analysis

- **Snapshot-first requires a derived index.** UB's design (typed, on commit, criteria DTO) is the most complete, and LS has proved it at write time. LC shows why typed columns matter: range queries and temporal validity.
- **History queries** are a strength unique to this architecture. Blame and diff are possible because versions are immutable (FU HL-FU-009).
- **Semantic search.**
  - Only UB stores embeddings per version.
  - UB's own notes prefer HNSW and asynchronous embedding in a separate structure.
  - The six-table rule allows the embedding column but not a separate table. *Interpretation:* the column is sufficient if it is filled asynchronously.
- **Queries as operations.** LC's query engine gives read operations the same process catalogue, permissions and logging as commands.

## 5. Verdicts

### VRD-12-01 — Typed derived index driven by type metadata (fusion UB + LS + LC)
- **Decision:** fusion.
- **Consequences:**
  - Properties flagged `is_searchable` or `indexed` are flattened on commit into `(commit_id, prop_path, val_text | val_num | val_date | val_bool)`.
  - Relationship entities index `source`, `target`, `type` and validity.
  - An inheritance index lists the type's ancestors to support "all descendants of type X".

### VRD-12-02 — Criteria query model (best-of UB)
- **Decision:** best-of UB.
- **Consequences:**
  - Queries have the shape `{context, types (incl. descendants), filters [{path, op ∈ EQ,NE,GT,GE,LT,LE,BETWEEN,LIKE,IN,EXISTS}], relationship joins, as_of (commit/time), sort, page}`.
  - The same model is exposed to scripts (`db.query`), the API and the protocol `Query` op.
  - Results are subject to read policies and masking (VRD-10-03).

### VRD-12-03 — History as a query dimension (fusion FU + UC)
- **Decision:** fusion.
- **Consequences:** Queries can include `history(uri)`, `blame(uri, path)`, `diff(uri_a, uri_b)`, `as_of`, and process lineage (`processes producing / consuming commit`).

### VRD-12-04 — Vector similarity per version, embedded asynchronously (adapt UB)
- **Decision:** adapt UB.
- **Consequences:**
  - An embedding policy per type states which fields to embed and which model.
  - Embeddings are filled by an asynchronous job after commit.
  - Query op `similar(text or vector, types, k)`.
  - The personal edition may omit it.

### VRD-12-05 — Read operations as declared query pipelines (best-of LC)
- **Decision:** best-of LC.
- **Consequences:** Named queries are pipelines in `query` mode (VRD-06-02): permissioned, logged, cacheable.
