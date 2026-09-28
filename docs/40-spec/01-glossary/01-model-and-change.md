---
id: SPEC-01-1
title: "Glossary part 1 — Entities, storage, meta-model, change, context"
status: complete
phase: P3
depends_on: [SPEC-01]
sources: [UP, FU, LC, UB, UC, US, LS, UW]
---

# Glossary part 1 — Entities, storage, meta-model, change, context

Part of SPEC-01. Entry format and rules: `00-index.md` §0.

---

## 1. Entities, identity and addressing

### TERM-Entity
- **Definition:** Any stored business or platform artifact: data record, type definition, logic, rule, view, widget, policy, package, context, job. An entity has a stable ID, a locator, a type and a history of versions. "Everything is an entity."
- **Aliases:** object, business object, record (UP, FU); smart entity (UC); `UbosEntity` (UB); node (LS graph).
- **Not:** a row of a physical table (see TERM-EntityInstance, TERM-Version).
- **Chapter:** SPEC-13
- **Origin:** CON-UP-013, CON-UB-010, CON-UC-010, CON-LS-010, VRD-02-01

### TERM-EntityId
- **Definition:** The immutable UUID of an entity. It is assigned once, never reused and never changed by rename or copy.
- **Aliases:** `entity_id`, `id`, `uuid` (all).
- **Not:** the locator or the slug, which can change.
- **Chapter:** SPEC-12
- **Origin:** CON-FU-016, CON-UC-010, VRD-03-01

### TERM-Locator
- **Definition:** The business key of an entity: the triple (tenant, type, slug). Unique among ACTIVE entities.
- **Aliases:** business key; "Tenant = DB, Type = table, Slug = PK" (UB); `tenant:type:slug` (UC, early).
- **Not:** the URI (a locator plus version selectors and presentation hints).
- **Chapter:** SPEC-12
- **Origin:** CON-UB-011, CON-UC-010, VRD-03-01

### TERM-Slug
- **Definition:** The human-readable, per-(tenant, type) unique name of an entity. It may contain dotted namespace segments. Mutable only through a RENAME commit.
- **Aliases:** code, key, name (UP, LC); PK (UB metaphor).
- **Not:** the display name (free text), the entity ID.
- **Chapter:** SPEC-12
- **Origin:** CON-UP-014, CON-FU-013, CON-FU-016, VRD-03-01

### TERM-Namespace
- **Definition:** A dotted prefix of a slug, type code or property key that groups names and marks ownership, e.g. `acme.crm`. The root package uses no namespace.
- **Aliases:** dotted slug namespace (UP, FU); app scope (LC).
- **Not:** a tenant (a key dimension of storage).
- **Chapter:** SPEC-02, SPEC-23
- **Origin:** CON-UP-014, CON-FU-013, CON-LC-016, VRD-02-07 (Q-006)

### TERM-Uri
- **Definition:** The canonical address of an entity or a version of it: `ubos://{authority}/{Type}/{slug}[?branch=&commit=&tag=&time=&key=&view=]`. URIs are the universal argument of scripts, protocol messages and references.
- **Aliases:** `ubos://` URI (all); URL grammar (LC); identity relation (LS).
- **Not:** an HTTP URL of a transport binding.
- **Chapter:** SPEC-12
- **Origin:** CON-FU-010, CON-UB-018, CON-UC-016, CON-UC-066, CON-LS-016, VRD-03-02

### TERM-Authority
- **Definition:** The first segment of a URI. It is either a context slug (resolved to tenant + branch or pin) or a tenant code.
- **Aliases:** scope (FU); context (UB); tenant (UC, LS).
- **Chapter:** SPEC-12
- **Origin:** CON-UB-005, CON-UB-017, VRD-03-03

### TERM-VersionSelector
- **Definition:** The URI query parameters that choose a version: `branch`, `commit`, `tag`, `time`. `commit` wins over the others.
- **Aliases:** version modes (LS); commit pin (UC); URI modes (FU frontend).
- **Chapter:** SPEC-12
- **Origin:** CON-FU-011, CON-UC-016, CON-LS-016, VRD-03-02

### TERM-KeyPath
- **Definition:** The `key` URI parameter: a dot/array path into a snapshot, e.g. `key=address.lines[0]`.
- **Aliases:** key path (FU).
- **Chapter:** SPEC-12
- **Origin:** CON-FU-010, VRD-03-02

### TERM-ShortForm
- **Definition:** A relative or aliased reference accepted in UI and scripts (`Type/slug`, `self`, `@draft`, `system`). Always normalised to a canonical URI before storage or audit.
- **Aliases:** alias, relative URI (UB design, FU frontend).
- **Chapter:** SPEC-12
- **Origin:** VRD-03-04

---

---

## 2. Storage and versioning

### TERM-EntityInstance
- **Definition:** The registry row of an entity (`entity_instance`): ID, locator, status, owner, display name, tags, visibility, terminator commit.
- **Aliases:** instance table (UP, UB); registry (UB).
- **Not:** a version or snapshot.
- **Chapter:** SPEC-11
- **Origin:** CON-UB-012, CON-UB-040, VRD-01-01

### TERM-Version
- **Definition:** One immutable state of one entity on one branch, stored as a full JSON snapshot in `entity_version`, identified by a commit ID.
- **Aliases:** commit node (UP); version row (UB/UC); typed EAV snapshot (LC).
- **Not:** a Process (which may produce many versions).
- **Chapter:** SPEC-11, SPEC-14
- **Origin:** CON-UP-011, CON-UB-040, CON-LC-040, VRD-01-02

### TERM-Commit
- **Definition:** The act and the record of writing one new version. Its commit ID (int64) is the identifier of that version. A commit has a parent commit (or none), an action (CREATE, UPDATE, DELETE, RENAME, COPY, MERGE, REVERT, UNSET) and belongs to exactly one process.
- **Aliases:** version (UB); micro-commit (UB, fine-grained); execution commit (UC).
- **Not:** a database transaction (one flush may write many commits atomically).
- **Chapter:** SPEC-14
- **Origin:** CON-UP-011, CON-UB-021, CON-UC-020, CON-LC-042, VRD-01-03

### TERM-Snapshot
- **Definition:** The complete JSON payload of an entity at one version. The snapshot is the source of truth; indexes are derived from it.
- **Aliases:** payload, `data`, content (all); state DTO (UB).
- **Chapter:** SPEC-11
- **Origin:** CON-UP-040, CON-UB-016, VRD-01-02

### TERM-OwnData
- **Definition:** The properties an entity stores itself, excluding inherited and default values. Writes store only own data.
- **Aliases:** self data (UB); raw fragment (UB).
- **Not:** effective data.
- **Chapter:** SPEC-13
- **Origin:** CON-UB-015, CON-UB-045, VRD-02-05

### TERM-EffectiveData
- **Definition:** The view of an entity after inheritance resolution: defaults, inherited values and own data composed root → head.
- **Aliases:** effective entity (UB); composite entity / merged view (US); assembled node (LS).
- **Chapter:** SPEC-13
- **Origin:** CON-UB-015, CON-US-012, CON-LS-023, VRD-02-05

### TERM-Branch
- **Definition:** A named line of versions, itself an entity with a parent branch and a kind (`main`, `release`, `feature`, `draft`, `overlay`). Reads fall back to the parent branch when the entity has no version on the branch.
- **Aliases:** universe (UB); branch config (UP); stash (FU, for drafts); per-request staging (LC).
- **Chapter:** SPEC-14
- **Origin:** CON-UP-041, CON-FU-028, CON-UB-041, CON-LC-045, VRD-08-01

### TERM-Head
- **Definition:** The latest commit of an entity on a branch, stored in `branch_head` with the branch height.
- **Aliases:** ref pointer (UP); head commit (UC).
- **Chapter:** SPEC-14
- **Origin:** CON-UP-012, CON-UB-041, VRD-01-03

### TERM-Height
- **Definition:** A per-(entity, branch) monotonically increasing counter stored with the head (`seq_num`). Used for compare-and-swap.
- **Aliases:** `seq_num`, block height (UB).
- **Chapter:** SPEC-14
- **Origin:** CON-UB-041, VRD-01-03

### TERM-Cas
- **Definition:** Compare-and-swap on the head: a write names the expected head (or height) and fails with a conflict if it changed.
- **Aliases:** head CAS (UB); head check (UC); optimistic lock (LC).
- **Chapter:** SPEC-14, SPEC-15
- **Origin:** CON-UB-021, CON-UB-041, VRD-01-03, VRD-04-03

### TERM-Tombstone
- **Definition:** A DELETE commit that ends an entity's life on a branch. The registry records status DELETED and `terminator_commit_id`; history stays readable.
- **Aliases:** soft delete (FU, LC); terminator (UB).
- **Chapter:** SPEC-14
- **Origin:** CON-FU-015, CON-UB-044, CON-LC-042, VRD-01-04

### TERM-EntityStatus
- **Definition:** Registry lifecycle status: ACTIVE, DELETED, ARCHIVED, LOCKED.
- **Chapter:** SPEC-11
- **Origin:** CON-UB-012, VRD-01-04

### TERM-Overlay
- **Definition:** A branch kind that lets a tenant or user change a base (usually the root package) without copying it. Writes create versions on the overlay; deletes write a masking tombstone on the overlay only.
- **Aliases:** branch hierarchy with recursive fallback (UP).
- **Chapter:** SPEC-14, SPEC-23
- **Origin:** CON-UP-041, VRD-08-02, VRD-18-02

### TERM-Draft
- **Definition:** A per-user branch that inherits a base branch. Publishing a draft is a merge into the base followed by cleanup of the draft.
- **Aliases:** stash (FU); draft → publish lifecycle (UB).
- **Not:** the DRAFT validation level (TERM-ValidationLevel).
- **Chapter:** SPEC-14
- **Origin:** CON-FU-030, CON-UB-022, VRD-08-03

### TERM-Merge
- **Definition:** Combining a source branch into a target branch using a strategy (source-wins, target-wins, three-way). Three-way merge finds the common ancestor and reports conflicts per field path.
- **Chapter:** SPEC-14
- **Origin:** CON-FU-025, CON-FU-026, CON-UB-023, VRD-08-04

### TERM-Revert
- **Definition:** Undoing a change by writing a **new** commit whose snapshot equals an earlier version.
- **Not:** reset (TERM-Reset).
- **Chapter:** SPEC-14
- **Origin:** CON-UB-024, VRD-08-05

### TERM-Reset
- **Definition:** An administrative operation that moves a head pointer back without writing a new version. Restricted and audited.
- **Aliases:** revert by moving the head pointer (FU).
- **Chapter:** SPEC-14
- **Origin:** CON-FU-027, VRD-08-05

### TERM-Diff
- **Definition:** The difference between two versions or two branch heads, expressed as JSON Patch (RFC 6902).
- **Chapter:** SPEC-14
- **Origin:** CON-FU-040, CON-UB-025

### TERM-Blame
- **Definition:** For each field path of a snapshot, the commit and author that last changed it.
- **Chapter:** SPEC-21
- **Origin:** CON-FU-041, CON-FU-051

### TERM-Tag
- **Definition:** A named, immutable entity (type `Tag`) that marks either one commit of one entity (COMMIT tag) or a point of a whole branch (POINT tag: branch + timestamp, e.g. a release). Usable as a version selector.
- **Chapter:** SPEC-14
- **Origin:** CON-FU-010, CON-LS-016

### TERM-Pin
- **Definition:** Binding a Context to a fixed point (a branch and a timestamp, or a Tag) instead of the moving branch head, or binding a reference to a fixed commit (`?commit=`).
- **Aliases:** commit pin (UC); environment → pinned commit (FU); version anchoring (UC).
- **Chapter:** SPEC-16
- **Origin:** CON-FU-031, CON-UC-016, CON-UC-021, VRD-08-06

### TERM-SixTables
- **Definition:** The fixed physical schema: `entity_instance`, `entity_version`, `branch_head`, `search_index`, `process_log`, `process_commit_map`. All business and metadata state lives in these tables.
- **Aliases:** schema v2.1 (UB/UC/US/LS); "6 tables only" (UC).
- **Chapter:** SPEC-11
- **Origin:** CON-UB-040, CON-UC-040, CON-LS-040, VRD-01-01

### TERM-RuntimeStore
- **Definition:** The declared, non-authoritative store for transient technical state: job queue signals, event bus, cache, idempotency keys (with TTL), rate limits, session memory cache. Redis in the enterprise edition; in-process in the personal edition.
- **Aliases:** Redis roles (UC); operational tables (LC); technical tables.
- **Not:** the six tables. Anything that must be audited is an entity, not runtime-store state.
- **Chapter:** SPEC-11 (ADR-004)
- **Origin:** CON-UC-043, CON-LC-043, VRD-01-06

### TERM-Edition
- **Definition:** A build of the platform for a deployment class: **personal** (SQLite, in-process runtime store, embedded) and **enterprise** (PostgreSQL + pgvector, Redis, multi-node).
- **Chapter:** SPEC-31
- **Origin:** CON-LS-006, CON-LS-062, VRD-01-05, VRD-19-02

---

---

## 3. Meta-model

### TERM-Type
- **Definition:** An entity (of the meta type) that defines a kind of entity: its properties, slots, actions, views and rules. Any registered type code is a logical table.
- **Aliases:** entity type (all); `TYPE_DEF` (UP); open entity type (UC); Entity Type as relation set (LS, US).
- **Chapter:** SPEC-13
- **Origin:** CON-UP-016, CON-UB-013, CON-UC-011, CON-LS-015, VRD-02-01

### TERM-TypeCode
- **Definition:** The slug of a Type entity, used as the `{Type}` segment of URIs, e.g. `Invoice`, `acme.crm.Lead`.
- **Aliases:** `entity_type`, `code` (all).
- **Chapter:** SPEC-02, SPEC-13
- **Origin:** VRD-02-02

### TERM-MetaType
- **Definition:** The type whose instances are types (`Type`). It is its own type, which closes the model.
- **Aliases:** `sys.meta.type` (UB); `meta` (UC); meta bootstrap (LS).
- **Chapter:** SPEC-13
- **Origin:** CON-UB-013, CON-LS-011, VRD-02-01

### TERM-RootType
- **Definition:** The single root of the type hierarchy (`Entity`). Every type extends it directly or indirectly.
- **Aliases:** `sys.entity.root` (UB); `root` (UC).
- **Chapter:** SPEC-13
- **Origin:** CON-UB-013, VRD-02-01

### TERM-Extends
- **Definition:** The `_extends` field of a type or an instance: the parent(s) it inherits from, given as type codes or URIs. Inheritance is always explicit.
- **Aliases:** explicit inheritance (UC); refine chain (LS).
- **Chapter:** SPEC-13
- **Origin:** CON-UC-012, CON-LS-014, VRD-02-05

### TERM-Property
- **Definition:** A named field of a type, declared with a semantic type and optional slot overrides. Payload keys are short property names.
- **Aliases:** field, attribute (all); relation type / prop (US, LS).
- **Chapter:** SPEC-13
- **Origin:** CON-US-010, CON-LS-012, VRD-02-04, VRD-02-07

### TERM-SemanticType
- **Definition:** A type that describes the meaning and format of a value (Email, Money, Percentage, Iban) rather than its storage primitive. Business properties reference semantic types, never raw primitives.
- **Aliases:** business type (UC); semantic primitive (US `sys_primitive`); layered semantic type (UB).
- **Chapter:** SPEC-13, SPEC-29
- **Origin:** CON-UB-013, CON-UC-018, CON-US-011, VRD-02-01

### TERM-Primitive
- **Definition:** An abstract base value type with a storage type: string, text, number, integer, boolean, date, timestamp, map, list, relation.
- **Chapter:** SPEC-13
- **Origin:** CON-UC-018, VRD-02-01

### TERM-MetaSchema
- **Definition:** The JSON Schema that every definition (type, property, action, view, package) must satisfy. It is published for AI tools and enforced by the loader and on every definition commit.
- **Aliases:** UODS (UC); metadata specification v1.0 (US).
- **Chapter:** SPEC-13
- **Origin:** CON-UC-017, CON-US-014, VRD-02-02

### TERM-Slot
- **Definition:** A named, contract-defined point of behaviour of a property or type (e.g. validate, mask, format, index, ui, default, compute). A slot is filled by a Logic entity or a native handler. The slot catalogue is closed.
- **Aliases:** behaviour slot; BehaviorType (LS); behaviour (US).
- **Chapter:** SPEC-13
- **Origin:** CON-LS-012, CON-LS-013, CON-US-010, VRD-02-04

### TERM-Behavior
- **Definition:** The implementation bound to a slot. Default behaviours come from the semantic type (regex → validate, masking rule → mask, `ui_modes` → ui) and can be overridden per property.
- **Aliases:** behavior (LS, US); type attribute (UB/UC `validation_regex`, `masking_rule`).
- **Chapter:** SPEC-13
- **Origin:** CON-LS-014, CON-UB-057, VRD-02-04

### TERM-ReferenceProperty
- **Definition:** A property of primitive `relation` with a `target` type that stores a canonical URI.
- **Aliases:** relation property (UB, UC); reference hydration by `_id` (UC).
- **Chapter:** SPEC-13
- **Origin:** CON-UC-051, VRD-02-03

### TERM-Relationship
- **Definition:** An entity of type `Relationship` that links a source and a target with a relationship type, validity (`valid_from`, `valid_to`) and metadata. Used for links with attributes or lifecycle, including access grants.
- **Aliases:** typed relation (UP, unversioned edge); relationship entity (LC).
- **Chapter:** SPEC-13
- **Origin:** CON-UP-015, CON-LC-014, CON-LC-017, VRD-02-03

### TERM-RelationshipType
- **Definition:** A catalogue entry that governs Relationship entities: allowed endpoint types, symmetry, validity required, metadata schema.
- **Aliases:** relationship type catalogue (LC); RelationType (LS, a different idea: see TERM-Slot and TERM-Property).
- **Chapter:** SPEC-13
- **Origin:** CON-LC-014, VRD-02-03

### TERM-InheritanceResolution
- **Definition:** The four-step algorithm that turns stored entities into effective entities: (1) assemble the `_extends` chain, (2) compose root → head, (3) resolve references, (4) execute actions only on interaction.
- **Aliases:** three-pass lifecycle (UB); type/relation assembly (LS); kernel as resolver (UC).
- **Chapter:** SPEC-13
- **Origin:** CON-UB-027, CON-LS-023, CON-UC-007, VRD-02-05

### TERM-Ontology
- **Definition:** The base domain library shipped in genesis: party/role/relationship, contact and geo, reference data, documents, security, IT, measures, PKI, kernel logic, UI.
- **Aliases:** universal business ontology (UB); genesis semantic ontology (UC).
- **Chapter:** SPEC-29
- **Origin:** CON-UB-014, CON-UC-018, VRD-02-06

### TERM-Template
- **Definition:** An entity used as a prototype for other entities through `_extends` (e.g. a Book template and its instances).
- **Aliases:** template vs publication (LC); Book/Page (UB).
- **Chapter:** SPEC-13
- **Origin:** CON-UB-019, CON-LC-018

---

---

## 4. Change and transactions

### TERM-Process
- **Definition:** The unit of business change and audit. Every write-bearing request runs as one process: it has an ID, an initiator, a context, an operation URI, a status, a trace, and produces zero or more commits recorded in `process_commit_map`.
- **Aliases:** process (UP); process log entry (UB, LC); Job as execution record (UC); Logrum as execution record (UC, early).
- **Not:** a Pipeline (a definition) or a ProcessInstance (a durable, long-running orchestration).
- **Chapter:** SPEC-15
- **Origin:** CON-UP-017, CON-UB-043, CON-UC-020, VRD-04-01

### TERM-Session
- **Definition:** The unit-of-work inside a process: an identity map of loaded entities plus staged change sets. Nothing is written until the single flush.
- **Aliases:** unit-of-work session (US); dynamic model (LC).
- **Not:** a protocol session (TERM-ProtocolSession) or a login session.
- **Chapter:** SPEC-15
- **Origin:** CON-US-020, CON-LC-012, VRD-04-02

### TERM-ChangeSet
- **Definition:** The staged change of one entity inside a session: action, expected head, new own data, provenance.
- **Aliases:** ChangeSet (LC, LS).
- **Chapter:** SPEC-15
- **Origin:** CON-LC-012, CON-LS-019, VRD-04-02

### TERM-Flush
- **Definition:** The single atomic write of all change sets of a session: CAS checks, versions, heads, registry, derived index, process log, commit map and outbox, in one database transaction.
- **Aliases:** atomic commit pipeline (LS); atomic write unit (UC); batch commit (UB).
- **Chapter:** SPEC-15
- **Origin:** CON-LS-041, CON-UC-041, CON-UB-021, CON-LC-022, VRD-04-02

### TERM-IdempotencyKey
- **Definition:** A client-supplied key that makes a mutating request safe to retry. The first result is stored in the runtime store with a TTL and replayed for repeats.
- **Chapter:** SPEC-15 (ADR-004)
- **Origin:** CON-LC-026, CON-UC-032, VRD-04-04

### TERM-Provenance
- **Definition:** The typed record of which versions a process read (DEPENDS_ON_DATA, DEPENDS_ON_FUNC, DEPENDS_ON_TYPE, …) and which it wrote (OUTPUT).
- **Aliases:** version anchoring (UC); typed lineage (UC).
- **Chapter:** SPEC-15, SPEC-30
- **Origin:** CON-UC-021, CON-UC-042, VRD-04-05

### TERM-DryRun
- **Definition:** Executing a process fully but discarding the flush, returning the would-be change sets, validation results and emits.
- **Chapter:** SPEC-15
- **Origin:** CON-LS-026, CON-UC-025, VRD-04-02

### TERM-AuditRecord
- **Definition:** The immutable record of one process: who, when, context, operation, inputs digest, result, commits, trace reference, app and billing linkage.
- **Aliases:** process log (UB, LC); dual audit (LC).
- **Chapter:** SPEC-30
- **Origin:** CON-UB-043, CON-LC-044, VRD-21-01

---

---

## 5. Context and execution

### TERM-Context
- **Definition:** An entity describing an environment: sovereign tenant, host tenant, branch or pinned commit, environment variables, memory policy and security policies. A context slug can be a URI authority.
- **Aliases:** Workspace (UC); Logrum as environment/session (UC); environment (FU); Context entity (UB); project + tenant + branch (UW).
- **Not:** the execution context (TERM-ExecutionContext); the LS "workspace" (a server, see TERM-Host).
- **Chapter:** SPEC-16
- **Origin:** CON-UB-017, CON-UC-015, CON-FU-031, CON-UW-011, VRD-07-01

### TERM-ExecutionContext
- **Definition:** The in-memory state of one running process: identity, tenant, context, branch, session, variables (blackboard), trace tree, limits. Scripts never pass identity or tenant explicitly.
- **Aliases:** ProcessContext (UB, UC); blackboard (LC, US); holographic context (UC); context-in/context-out (UB).
- **Chapter:** SPEC-16
- **Origin:** CON-UB-020, CON-UC-022, CON-LC-011, CON-US-028, VRD-07-02

### TERM-TraceTree
- **Definition:** The tree of calls a process made (logic, syscalls, rules, steps) with inputs, outputs and timing, bounded in size and offloaded when large.
- **Aliases:** execution stack (UB); causal slice trace (US).
- **Chapter:** SPEC-30
- **Origin:** CON-UB-020, CON-US-029, VRD-21-03

### TERM-Memory
- **Definition:** Key-value state attached to a Context that survives between executions. Cached in the runtime store; persisted as entity versions when the context's memory policy says durable.
- **Aliases:** Ambassador memory (UC).
- **Chapter:** SPEC-16
- **Origin:** CON-UC-023, VRD-07-04

### TERM-SovereignTenant
- **Definition:** The tenant that owns a piece of logic and whose data it may touch by default.
- **Chapter:** SPEC-16, SPEC-23
- **Origin:** CON-UC-024, VRD-07-05

### TERM-HostTenant
- **Definition:** The tenant in whose context foreign logic runs (the caller's tenant).
- **Aliases:** Embassy/Ambassador (UC metaphor).
- **Chapter:** SPEC-16, SPEC-23
- **Origin:** CON-UC-024, VRD-07-05

### TERM-SystemIdentity
- **Definition:** The identity under which triggered work (cron, hook, event handler) runs when no user initiated it.
- **Chapter:** SPEC-16
- **Origin:** VRD-11-05
