---
id: SPEC-21
title: "Query and Search"
status: complete
phase: P4
depends_on: [SPEC-11, SPEC-12, SPEC-13, SPEC-14, SPEC-15, SPEC-19]
sources: [UP, FU, LC, UB, UC, LS]
---

# Query and Search

## 0. Chapter header

- **Scope:** This chapter covers:
  - the criteria query model: types, filters, text, relationship joins, as-of, sort, cursor paging, projection, expansion;
  - how criteria queries compile onto the derived index and the branch and tenant layers;
  - `Query` entities (saved criteria, query pipelines, native queries);
  - history queries (history, blame, diff, lineage, audit);
  - vector similarity with asynchronous embeddings;
  - result security, consistency and caching.
- **MOD:** `QRY`
- **depends_on:** SPEC-11, SPEC-12, SPEC-13, SPEC-14, SPEC-15, SPEC-19.
- **Terms used:** TERM-SearchIndex, TERM-CriteriaQuery, TERM-AsOf, TERM-QueryPipeline, TERM-Embedding, TERM-Blame, TERM-Diff, TERM-Provenance.
- **Origin summary:**
  - Verdicts: VRD-12-01…05, VRD-21-02.
  - UB: the typed index, `SearchCriteria` and the inheritance index (CON-UB-042), embeddings per version (CON-UB-065).
  - LS: the generic criteria reader (CON-LS-043) and the index written on commit (CON-LS-042).
  - FU: blame, global search, audit queries and metrics (CON-FU-041…044).
  - UC: containment and name search (CON-UC-046), hydration (CON-UC-051), typed lineage (CON-UC-042).
  - LC: the query engine as named operations (CON-LC-002).
  - UP: the commit-scoped property index (CON-UP-042).

---

## 1. Concepts

- **Snapshots are the truth, and the index is how to find them.** A query first finds **candidate entities** through registry, head and index rows, without reading snapshots. It then loads the matching versions and composes effective data for the page only (REQ-STO-020).
- **What is indexed.**
  - The index holds **own data** of each version: stored required defaults and stored `compute` results are included (SPEC-11 REQ-STO-007).
  - Values a version only *inherits* from prototypes are not indexed. Filtering on them uses a bounded post-filter (REQ-QRY-006).
- **Visibility.** A query sees the entities its reader could `entity.get`:
  - resolved through the tenant layers (tenant, then root);
  - through the branch chain (§1.2 of SPEC-14);
  - at a point in time;
  - filtered by read policies and masked.
- **Queries are operations.** Saved and named queries are `Query` entities with URIs, versions, permissions and optional audit (LC).

---

## 2. Model

```yaml
ENT-CriteriaQuery:
  purpose: Declarative query over entities (TERM-CriteriaQuery); used by the Query op, query.find, views and saved queries.
  origin: [VRD-12-02, CON-UB-042, CON-LS-043, CON-FU-042]
  fields:
    - {name: types, type: "list<uri<Type>>", required: true, description: "Entity types (at least one)"}
    - {name: include_subtypes, type: bool?, required: false, description: "Include descendants (default true)"}
    - {name: branch, type: branch_name?, required: false, description: "Default: context branch"}
    - {name: as_of, type: "{time?: timestamp, tag?: slug}?", required: false, description: "Point-in-time query (TERM-AsOf)"}
    - {name: where, type: FilterExpr?, required: false, description: "Filter tree (§2.1)"}
    - {name: text, type: string?, required: false, description: "Text search over searchable text paths, slug and title (REQ-QRY-005)"}
    - {name: joins, type: "list<Join>?", required: false, description: "Relationship/reference joins (§2.2)"}
    - {name: sort, type: "list<{path, dir - enum{ASC|DESC}, nulls? - enum{FIRST|LAST}}>?", required: false, description: "Default [$modified_at DESC]"}
    - {name: page, type: "{size - int32 (1..500, default 50), cursor? - string}?", required: false, description: "Keyset paging"}
    - {name: fields, type: "list<string>?", required: false, description: "Projection of effective data paths (default all)"}
    - {name: expand, type: "int32 | list<string>?", required: false, description: "Reference hydration depth or paths (REQ-META-020)"}
    - {name: count, type: bool?, required: false, description: "Also return the total count (capped, REQ-QRY-008)"}
    - {name: include_deleted, type: bool?, required: false, description: "Include DELETED registry rows (requires ubos.qry.deleted)"}
    - {name: consistent, type: bool?, required: false, description: "Pin all pages to the first page's point in time (default true)"}
```

### 2.1 Filters

```
FilterExpr := { all: [FilterExpr…] } | { any: [FilterExpr…] } | { not: FilterExpr } | Leaf
Leaf       := { path: string, op: Op, value?: json, values?: [json…] }
Op         := EQ | NE | GT | GE | LT | LE | BETWEEN | IN | NOT_IN | LIKE | PREFIX | EXISTS | NOT_EXISTS
            | REF      (reference equals an entity; value = URI)
            | CONTAINS (list property contains value)
```

**Paths:**
- property paths as indexed (`email`, `address.city`, `lines[].sku`);
- `_state`, which is always indexed for lifecycle types;
- registry pseudo-paths:

| Pseudo-path | Source | Ops |
|---|---|---|
| `$slug` | entity_instance.slug | EQ, IN, LIKE, PREFIX |
| `$type` | entity_type (exact, ignoring `include_subtypes`) | EQ, IN |
| `$status` | entity_instance.status | EQ, IN |
| `$tags` | entity_instance.tags | CONTAINS |
| `$owner` | owner_id (URI of principal) | EQ, IN |
| `$created_at`, `$modified_at` | timestamps | comparison, BETWEEN |
| `$id` | entity ID | EQ, IN |
| `$branch` | branch where the visible version was found | EQ |

**Operator semantics:**
- `LIKE` uses `%` and `_` wildcards and is case-insensitive when the property's `index` slot normalises to lower case (the default for text).
- `BETWEEN` includes both ends.
- Comparisons on money compare amounts of the same currency only (`QRY.UNIT_MISMATCH` otherwise).

### 2.2 Joins

```
Join := { alias: string,
          via: { property: path }                                   // reference property of the root type
             | { relationship: uri<RelationshipType>, direction: OUT|IN|BOTH, at?: timestamp|"now" },
          types?: [uri<Type>], where?: FilterExpr, required?: bool (default true) }
```

- A join restricts the roots to those whose related entity matches `where`.
- Joined entities are not returned unless `expand` names the alias.
- Paths in `sort` and `fields` may use `alias.path` for joins with at most one match per root. Otherwise the result is `QRY.AMBIGUOUS_JOIN`.
- The maximum is 3 joins per query.

```yaml
ENT-Query:
  purpose: A named, versioned read operation (TERM-QueryPipeline generalised) - ubos://{t}/Query/{slug}@vN.
  origin: [VRD-12-05, CON-LC-002, CON-LC-001]
  fields:
    - {name: title, type: string, required: true, description: "Name"}
    - {name: kind, type: "enum{CRITERIA|PIPELINE|NATIVE}", required: true, description: "Saved criteria template, QUERY pipeline, or kernel query"}
    - {name: criteria, type: json?, required: false, description: "CRITERIA - CriteriaQuery template; ${params.x} placeholders substituted as JSON values (never as text)"}
    - {name: pipeline, type: "uri<Pipeline>?", required: false, description: "PIPELINE - a mode QUERY pipeline"}
    - {name: native, type: "uri<Native>?", required: false, description: "NATIVE - kernel query (root package only)"}
    - {name: params_schema, type: json_schema?, required: false, description: "Parameters"}
    - {name: permission, type: string?, required: false, description: "Default query.<slug>"}
    - {name: cache, type: "{ttl - duration, scope - enum{PRINCIPAL|POLICY_SET|TENANT}}?", required: false, description: "Result caching (REQ-QRY-011)"}
    - {name: audit, type: bool?, required: false, description: "Write a process row per execution (read auditing)"}
```

```yaml
ENT-QueryResult:
  purpose: Result page of a criteria or named query.
  origin: [VRD-12-02, CON-LS-043]
  fields:
    - {name: items, type: "list<{uri, entity_id, commit_id, branch, type, data}>", required: true, description: "Canonical URI with commit; data = projected, expanded, masked effective data"}
    - {name: next_cursor, type: string?, required: false, description: "Opaque; absent on the last page"}
    - {name: total, type: "{value - int64, exact - bool}?", required: false, description: "When count requested"}
    - {name: point, type: timestamp, required: true, description: "Point in time the page reflects"}
    - {name: warnings, type: "list<error>", required: true, description: "e.g. QRY.POST_FILTER_USED"}
```

```yaml
ENT-EmbeddingSpec:
  purpose: Type.embedding - what feeds version embeddings (VRD-12-04).
  origin: [VRD-12-04, CON-UB-065]
  fields:
    - {name: fields, type: "list<string>", required: true, description: "Paths concatenated (formatted by the format slot) into the embedding text"}
    - {name: provider, type: "uri<AiProvider>?", required: false, description: "Default: tenant's embedding provider (SPEC-27)"}
    - {name: include_title, type: bool?, required: false, description: "Prefix with title/type (default true)"}
    - {name: max_chars, type: int32?, required: false, description: "Truncation (default 8000)"}
```

---

## 3. Behavior

### 3.1 Criteria queries

### REQ-QRY-001 — Query execution
- **Statement:** Executing a CriteriaQuery *q* in execution context *x* MUST:
  1) validate *q* (schema, known types, operators valid for the path's primitive, join limits);
  2) expand `types` to their subtypes (inheritance index, API-META-001 `subtypes`);
  3) determine the point: `as_of`, the cursor's pinned point, or `x.now` for consistent paging;
  4) compile a plan (REQ-QRY-002) and run it through `ReaderOps::search` to get candidate (entity, commit) pairs in sort order;
  5) apply post-filters (REQ-QRY-006) and read authorisation (REQ-QRY-007);
  6) load the page's versions, compose effective data (REQ-META-020), project `fields`, `expand` and mask;
  7) inside a process, overlay the session's staged entries (REQ-TX-004);
  8) return a QueryResult.

  Reads record DEPENDS_ON_DATA only for the items **returned** to logic, not for candidates.
- **Origin:** VRD-12-02, CON-UB-042, CON-LS-043
- **Acceptance:** A query for `Customer` with `address.city EQ "Hangzhou"`, sorted by `name`, returns effective, masked customers including subtype `VipCustomer`, and reads no snapshot outside the page.
- **Priority:** P0

### REQ-QRY-002 — Visibility across branches and tenants
- **Statement:** The plan MUST select, for each entity, the version the resolver would return (SPEC-14 §1.2, SPEC-23 layering) at the point *t*:
  - rank the (tenant, branch) pairs of the read order (tenant draft … tenant main, then root main);
  - for each entity, take the most specific pair that has a version at or before *t* (the head when *t* = now);
  - drop the entity if that version is DELETE, and move on to the next pair if it is UNSET.

  Index filters apply to the **selected** version's `commit_id` only. A match on a masked parent version MUST NOT leak a result.
- **Rationale:** Correct overlay semantics: an entity changed on a draft is judged by its draft value, and a deletion on the draft hides it.
- **Origin:** VRD-08-01, VRD-08-02, REQ-VER-003, REQ-STO-006
- **Acceptance:**
  1) On a draft that renamed a customer's city, the query for the old city does not return it, and the query for the new city does.
  2) A draft deletion hides the customer.
- **Priority:** P0

### REQ-QRY-003 — Point-in-time queries
- **Statement:** With `as_of.time` (or a POINT tag), selection MUST use the latest version at or before the time on each pair (REQ-QRY-002). The FROZEN semantics of FEATURE and RELEASE branches still apply. Registry pseudo-paths reflect the current registry, except `$status`, which is derived from the selected version (DELETE → DELETED). Queries with `as_of` are read-only and MAY be cached permanently (REQ-QRY-011).
- **Origin:** VRD-12-03, CON-UP-024, REQ-VER-006
- **Acceptance:** A query `as_of` last month returns the customers as they were, including ones deleted since.
- **Priority:** P1

### REQ-QRY-004 — Sorting and paging
- **Statement:** Sorting MUST be stable. The kernel appends `$id ASC` as a final tie-breaker.
  - Sort paths must be indexed (single-valued properties) or registry pseudo-paths. Otherwise the result is `QRY.SORT_NOT_INDEXED`.
  - Paging is keyset-based. The opaque cursor encodes the last sort key, the entity ID, the point (when `consistent`) and a digest of *q*.
  - A cursor used with a different query fails with `QRY.CURSOR_INVALID`.
  - Page size is at most 500.
- **Origin:** CON-UB-042, NEW: stable pages over immutable versions
- **Acceptance:** Paging through 10 000 customers while others are being inserted returns each pre-existing customer exactly once when `consistent` is true.
- **Priority:** P0

### REQ-QRY-005 — Text search
- **Statement:** `text` MUST match case-insensitively against the `val_text` of searchable text properties, the slug and the `title`, using contains semantics. Results are ordered by relevance (exact slug or title first, then prefix, then contains) unless `sort` is given.
  - PostgreSQL uses a `pg_trgm` GIN index on `lower(val_text)`.
  - SQLite uses LIKE over the candidate set, within the scan limit.
  - A global search across all types (FU) is a query with `types: [Entity]`, restricted to types the reader may list.
- **Origin:** CON-FU-042, CON-UC-046, VRD-12-01
- **Acceptance:** Searching "hang" finds a customer titled "Hangzhou Tea Co." and an address with city "Hangzhou".
- **Priority:** P1

### REQ-QRY-006 — Post-filters and scan limit
- **Statement:** A filter on a path that is not indexed, including values inherited only from prototypes, MUST be evaluated as a post-filter on effective data. This is allowed only when the other, indexed conditions reduce the candidates to at most 10 000 (configurable per tenant, max 100 000). Otherwise the query fails with `QRY.SCAN_LIMIT` and names the path to index. Queries using post-filters return the warning `QRY.POST_FILTER_USED`.
- **Origin:** NEW: honest limits of snapshot + derived index (CMP-12 analysis)
- **Acceptance:** A filter on a non-searchable property over a type with 50 000 instances and no other condition fails with `QRY.SCAN_LIMIT`.
- **Priority:** P0

### REQ-QRY-007 — Security in queries
- **Statement:** Read policies MUST be applied to queries (SPEC-22):
  - Conditions a policy expresses over indexed paths or registry fields (tenant, type, owner, tags, `_state`) are compiled into the plan, so that pages and counts are correct.
  - Other conditions are applied as post-filters. If such post-filtering may make counts inexact, `total.exact` is false.
  - Entities the reader cannot read are omitted. They are never returned as errors.
  - Masking is applied per property, after projection.
  - A filter or sort on a property the reader cannot read fails with `QRY.PATH_FORBIDDEN`, so that no value is inferred through filtering.
- **Origin:** VRD-12-02, VRD-10-03, REQ-CONV-052
- **Acceptance:**
  1) A user limited to own records gets only them, with a correct total.
  2) Filtering on `salary` without the permission fails with `QRY.PATH_FORBIDDEN`.
- **Priority:** P0

### REQ-QRY-008 — Counts and aggregates
- **Statement:** `count: true` MUST return the total, capped at 100 000. Above the cap, `exact` is false and the value is the cap. Aggregations (`group_by` on an indexed path with measures `count`, `sum`, `min`, `max`, `avg` over `val_num` paths) are P2 extensions of the same plan, subject to REQ-QRY-007.
- **Origin:** CON-FU-044, NEW
- **Acceptance:** A count of 250 000 matching entities returns `{value: 100000, exact: false}`.
- **Priority:** P1 (count), P2 (aggregations)

### REQ-QRY-009 — Joins
- **Statement:**
  - A `via.property` join MUST use `search_index.val_ref` of the root's reference property.
  - A `relationship` join MUST use the Relationship entities' indexed `source`, `target`, `relationship_type` and validity (`valid_from ≤ at < valid_to`, with open ends allowed). Symmetric relationship types match both directions.
  - `required: false` makes a join optional (it filters nothing and only provides expansion).
  - Join visibility follows REQ-QRY-002 and REQ-QRY-007 for the joined entities.
- **Origin:** VRD-12-02, VRD-02-03, CON-LC-014
- **Acceptance:** "Persons who WORK_FOR an Organization in Hangzhou now" returns the right persons, and ended employments are excluded.
- **Priority:** P1

### 3.2 Named queries

### REQ-QRY-010 — Query entities
- **Statement:** Running a named query `Query{named: {uri, args}}` MUST:
  1) resolve the Query entity (anchored, DEPENDS_ON_FUNC);
  2) validate the args;
  3) authorise `query.<slug>` (or the declared permission);
  4) execute by kind:
     - CRITERIA: substitute the parameters as JSON values into the template, then REQ-QRY-001;
     - PIPELINE: run the QUERY pipeline in READ_ONLY (REQ-FLOW-001);
     - NATIVE: call the kernel handler.

  If `audit` is true, a process row of kind QUERY is written. Kernel queries used throughout the spec (`meta.*`, `version.*`, `branch.status`, `rt.*`, `rule.*`, …) are NATIVE Query entities in `logrums`.
- **Origin:** VRD-12-05, CON-LC-002, CON-LC-001
- **Acceptance:**
  1) `acme.open_invoices@v1 {customer}` returns the customer's unpaid invoices.
  2) A principal without `query.acme.open_invoices` is refused.
- **Priority:** P0

### REQ-QRY-011 — Result caching
- **Statement:**
  - A named query MAY declare `cache {ttl, scope}`. Results are cached in the runtime store under (query commit, args digest, scope key, point bucket), where the scope key is the principal, the fingerprint of the principal's effective policy set, or the tenant.
  - Queries pinned by `as_of` or a commit are cacheable without TTL.
  - Cache hits still apply masking for the current reader when the scope is wider than PRINCIPAL.
  - Cached results carry `point` so that clients know their age.
- **Origin:** VRD-12-05, CON-LS-044, CON-LC-046
- **Acceptance:** A dashboard query with a 60 s TTL executes once per minute per policy set under load.
- **Priority:** P2

### 3.3 History and lineage

### REQ-QRY-020 — History queries
- **Statement:** The kernel MUST provide these NATIVE queries. Each is authorised as a read of the entity, and history reads require `entity.history`.

| Query | Result |
|---|---|
| `version.history@v1 {uri, from?, to?, include_inherited?, page}` | versions (REQ-VER-032) |
| `version.get@v1 {uri with commit}` | own + effective data of that version |
| `version.diff@v1 {a, b, mode}` | JSON Patch (REQ-VER-030) |
| `version.blame@v1 {uri, path?, depth?}` | per top-level key (or per path under `path`, up to `depth`): `{path, commit_id, author, committed_at, process_id, action}` of the last change, computed from consecutive snapshots (FU LAG method) |
| `version.timeline@v1 {uri, path}` | all values of one path over time with authors |
- **Origin:** VRD-12-03, CON-FU-041, CON-FU-051, CON-UC-031
- **Acceptance:** Blame on a customer shows that `address.city` was last changed by user u-7 in process p-9, and `name` by genesis.
- **Priority:** P1

### REQ-QRY-021 — Lineage and audit queries
- **Statement:** The kernel MUST provide:

| Query | Result |
|---|---|
| `lineage.of_commit@v1 {commit, direction: PRODUCED_BY\|CONSUMED_BY, depth ≤ 5}` | the process that produced the commit (OUTPUT), and recursively its dependencies; or the processes that depended on it |
| `lineage.of_process@v1 {process_id}` | the process row, its OUTPUT commits, its dependencies by relation |
| `audit.processes@v1 {principal?, operation?, kind?, status?, from, to, entity?, page}` | process rows (FU audit queries) |
| `audit.entity@v1 {uri, from?, to?}` | processes that touched the entity (OUTPUT or DEPENDS_ON) |

  Lineage traversal applies read authorisation on every node. Nodes that are not visible are shown as `{hidden: true}`, without details.
- **Origin:** VRD-12-03, VRD-21-02, CON-UC-042, CON-FU-043
- **Acceptance:** From an invoice version, `PRODUCED_BY` depth 2 shows the approving process, the Logic commit it ran and the Context commit.
- **Priority:** P1

### 3.4 Vectors

### REQ-QRY-030 — Embedding pipeline
- **Statement:** For types with an `embedding` spec, each committed non-DELETE version MUST get an `embedding` job (SPEC-20, delivery JOB) that:
  - builds the embedding text from the spec's fields, formatted by `format` slots, with sensitive properties excluded unless the spec names them and the provider is internal (SPEC-27 privacy);
  - calls `ai.embed`;
  - sets `entity_version.embedding` once (REQ-STO-003).

  A version whose text equals its parent's text (by hash) copies the parent's vector without an AI call. Failures are retried per the job policy, and a version left without an embedding is excluded from similarity search.
- **Origin:** VRD-12-04, CON-UB-065
- **Acceptance:** Committing 100 documents produces 100 embeddings asynchronously. An unchanged-text update reuses the vector.
- **Priority:** P2

### REQ-QRY-031 — Similarity query
- **Statement:** `query.similar {text | vector | uri, types, k ≤ 100, where?, min_score?}` MUST:
  1) embed the text if needed, using the same provider/model as the target types (`QRY.EMBEDDING_MODEL_MISMATCH` if the types differ);
  2) search the visible versions (REQ-QRY-002, current point only) by cosine similarity, applying `where` and security;
  3) return `[{uri, score, data}]`.

  PostgreSQL uses the HNSW index with post-filtering and over-fetch (k × 4). SQLite uses a brute-force scan up to 10 000 candidates, else `QRY.VECTOR_UNSUPPORTED` (REQ-STO-014).
- **Origin:** VRD-12-04, VRD-16-05, CON-UB-065
- **Acceptance:** "Contracts similar to this one" returns the 5 nearest visible contracts, each with a score.
- **Priority:** P2

### 3.5 Hydration and live results

### REQ-QRY-040 — Expansion
- **Statement:** `expand` MUST hydrate references per REQ-META-020 step 4, with read authorisation per referenced entity, a maximum depth of 3, and at most 1 000 expanded entities per page. The hydrated form is `{_uri, _type, _id, …data}`. A reference that is forbidden or missing becomes `{_uri, _error}`.
- **Origin:** CON-UC-051, VRD-02-05
- **Acceptance:** Invoices with `expand: ["customer"]` include masked customer data in each item.
- **Priority:** P0

### REQ-QRY-041 — Live queries
- **Statement:** A client MAY subscribe to a criteria query (SPEC-24 Subscribe `{query}`). The kernel re-evaluates membership on `entity.committed` events of the query's types in the subscriber's visibility, and pushes `{added, changed, removed}` deltas (URIs with commits). The client re-reads the data. The limit is 50 live queries per protocol session.
- **Origin:** VRD-11-04, CON-LS-028 (reactor)
- **Acceptance:** A kanban view subscribed to open issues receives a `changed` delta when an issue's `_state` changes.
- **Priority:** P2

---

## 4. Interfaces

### API-QRY-001 — QueryService
- **Kind:** rust trait (`ubos_kernel::query`)
- **Signature / shape:**
```rust
#[async_trait]
pub trait QueryService: Send + Sync {
    async fn find(&self, x: &ExecutionContext, s: Option<&Session>, q: CriteriaQuery) -> Result<QueryResult, UbosError>;
    async fn count(&self, x: &ExecutionContext, q: CriteriaQuery) -> Result<Total, UbosError>;
    async fn named(&self, x: &ExecutionContext, uri: &str, args: Value, page: Option<Page>) -> Result<Value, UbosError>;
    async fn similar(&self, x: &ExecutionContext, q: SimilarQuery) -> Result<Vec<Scored>, UbosError>;
    fn compile(&self, x: &ExecutionContext, q: &CriteriaQuery) -> Result<IndexQueryPlan, UbosError>; // REQ-QRY-002
}
```
- **Origin:** CON-LS-043, CON-UB-042

### API-QRY-002 — UBTP Query message
- **Kind:** UBTP message (SPEC-24)
- **Signature / shape:**
```json
{ "op": "Query", "request_id": "q-5",
  "criteria": {
    "types": ["ubos://acme/Type/acme.Invoice"],
    "where": { "all": [ { "path": "_state", "op": "IN", "values": ["SUBMITTED", "APPROVED"] },
                        { "path": "total", "op": "GT", "value": "1000.00" },
                        { "path": "customer", "op": "REF", "value": "Customer/c-001" } ] },
    "sort": [ { "path": "$modified_at", "dir": "DESC" } ],
    "page": { "size": 20 }, "fields": ["number", "total", "_state", "customer"],
    "expand": ["customer"], "count": true } }

{ "op": "Query", "request_id": "q-6", "named": { "uri": "ubos://acme/Query/open_invoices@v1", "args": { "customer": "Customer/c-001" } } }
```
- **Semantics:** The response is `Result{QueryResult}`. `as_of` goes inside `criteria`.
- **Origin:** VRD-15-01

### API-QRY-003 — Script interface
- **Kind:** syscalls (SPEC-17 REQ-RT-013)
- **Signature / shape:** `query::find(criteria_map)`, `query::count(criteria_map)`, `query::named(uri, args)` and `query::similar(x, opts)` take the same shapes as JSON maps.
- **Origin:** VRD-12-02

### Error codes (QRY)

| Code | Category | Meaning |
|---|---|---|
| `QRY.INVALID` | INVALID | malformed query (causes with paths) |
| `QRY.OP_NOT_SUPPORTED` | INVALID | operator not valid for the path's primitive |
| `QRY.SORT_NOT_INDEXED` | INVALID | sort path not indexed or multi-valued |
| `QRY.SCAN_LIMIT` | LIMIT | post-filter candidate set too large |
| `QRY.PATH_FORBIDDEN` | FORBIDDEN | filter or sort on an unreadable property |
| `QRY.CURSOR_INVALID` | INVALID | cursor does not match the query |
| `QRY.AMBIGUOUS_JOIN` | INVALID | multi-match join used in sort or fields |
| `QRY.UNIT_MISMATCH` | INVALID | money comparison across currencies |
| `QRY.VECTOR_UNSUPPORTED` | LIMIT | similarity not available at this size or edition |
| `QRY.EMBEDDING_MODEL_MISMATCH` | INVALID | types embedded with different models |
| `QRY.POST_FILTER_USED` | (warning) | a post-filter was applied |

---

## 5. Non-functional

| ID | Requirement | Target |
|---|---|---|
| NFR-PERF-100 | Criteria query, 1 indexed filter, page 50, 1 M entities of the type (PG) | p95 ≤ 50 ms |
| NFR-PERF-101 | Same on a 2-level branch chain | p95 ≤ 80 ms |
| NFR-PERF-102 | Count with an indexed filter up to the cap | p95 ≤ 200 ms |
| NFR-PERF-103 | Similarity k=10 over 1 M vectors (PG, HNSW) | p95 ≤ 100 ms |
| NFR-SCAL-100 | Indexed rows | ≥ 1 billion `search_index` rows with partitioning (REQ-STO-015) |

---

## 6. Acceptance

| REQ | Criterion |
|---|---|
| REQ-QRY-001…009 | Criteria scenarios incl. subtypes, drafts, as-of, stable paging, text, post-filter limits, security with counts, joins |
| REQ-QRY-010, 011 | Named query kinds, permissions, caching |
| REQ-QRY-020, 021 | History, blame, timeline, lineage, audit queries with hidden nodes |
| REQ-QRY-030, 031 | Async embeddings with reuse; similarity with security |
| REQ-QRY-040, 041 | Expansion limits; live query deltas |

---

## 7. Implementation notes

- **Reference code:**
  - UB search [UB:backend/src/main/java/com/ubos/ese/handler/EntitySearchHandler.java], [UB:backend/src/main/java/com/ubos/core/dto/SearchCriteria.java].
  - LS reader [LS:crates/ubos_store/src/engine/reader.rs].
  - FU history/blame [FU:backend/src/main/java/org/logrum/ubos/kernel/service/LcmHistoryService.java], search [FU:backend/src/main/java/org/logrum/ubos/kernel/service/LcmKernelService.java#L321-L437], audit [FU:backend/src/main/java/org/logrum/ubos/kernel/service/LcmAuditService.java].
- **Plan shape (PostgreSQL):** a CTE `visible` selects `DISTINCT ON (entity_id)` over `branch_head` rows of the read order, joined to a rank table (tenant, branch → rank) and ordered by rank. For `as_of`, the same is done over `entity_version` with `committed_at ≤ t`. Filters are `EXISTS` sub-selects on `search_index` by `commit_id`. Sorting joins the sort path's index row, or registry columns.
- **SQLite:** the same SQL shape without `DISTINCT ON`. Use `ROW_NUMBER() OVER (PARTITION BY entity_id ORDER BY rank)`.
- **Security compilation:** policies (SPEC-22) expose a `to_filter()` for conditions over indexed paths, and the rest are post-filters.

## 8. Open questions

None. Deferred to P2: aggregations (REQ-QRY-008), live queries (REQ-QRY-041), result caching (REQ-QRY-011), vectors (REQ-QRY-030/031).
