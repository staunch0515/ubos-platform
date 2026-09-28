---
id: SPEC-01
title: "Glossary"
status: complete
phase: P3
depends_on: [SPEC-00]
sources: [UP, FU, LC, UB, UC, US, LS, UW]
---

# Glossary

## 0. How to use this glossary

- This file fixes **one vocabulary** for the specification and for every implementation (VRD-22-04).
- Each term has a stable ID, `TERM-<Name>`. Chapters link to terms by that ID.
- **Aliases** list the words the eight source repositories use for the same idea. An alias is
  never used in the spec or in implementation code, except to quote a source.
- **Not** lists nearby ideas the term must not be confused with.
- **Chapter** names the spec chapter that defines the term normatively. The glossary gives a
  short definition only; when the two differ, the chapter wins.
- **Origin** lists the observations (CON/HL) and verdicts (VRD) the term comes from.
- Implementations MUST NOT introduce synonyms. A new term is added here first.

Entry format:

```
### TERM-<Name>
- **Definition:** ...
- **Aliases:** <corpus word> (<KEY>), ...
- **Not:** ...
- **Chapter:** SPEC-NN
- **Origin:** CON-..., VRD-...
```

Term groups:
1. Entities, identity and addressing (§1)
2. Storage and versioning (§2)
3. Meta-model (§3)
4. Change and transactions (§4)
5. Context and execution (§5)
6. Logic runtime (§6)
7. Rules and validation (§7)
8. Orchestration (§8)
9. Events and asynchronous work (§9)
10. Query and search (§10)
11. Security and tenancy (§11)
12. Protocol (§12)
13. UI and client shells (§13)
14. AI (§14)
15. Packages, boot and deployment (§15)
16. Development method (§16)

Alias quick map (§17) lists every corpus word in one table.

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
- **Definition:** The act and the record of writing one new version. Its commit ID (int64) is the identifier of that version. A commit has a parent commit (or none), an action (CREATE, UPDATE, DELETE, RENAME, COPY, MERGE, REVERT) and belongs to exactly one process.
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
- **Definition:** A named, immutable pointer to a commit (e.g. a release label). Usable as a version selector.
- **Chapter:** SPEC-14
- **Origin:** CON-FU-010, CON-LS-016

### TERM-Pin
- **Definition:** Binding a Context or a reference to a fixed commit instead of a branch head.
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

---

## 6. Logic runtime

### TERM-Logic
- **Definition:** An entity holding executable code (`language`, `source`, `entry`, input/output schema, required capabilities). Language `rhai` in version 1.
- **Aliases:** SysFunction (UC, early); script (UP, UB); `sys_function` (UC genesis); mould (UC metaphor).
- **Not:** a Job (a requested execution) or a Process (an execution record).
- **Chapter:** SPEC-17
- **Origin:** CON-UP-020, CON-UC-013, CON-US-018, VRD-05-01

### TERM-Syscall
- **Definition:** A kernel function registered into the script engine under a namespace (`entity.*`, `query.*`, `ctx.*`, `ai.*`, …). Syscalls are the only way logic reaches data or the outside world.
- **Aliases:** kernel API (UP); capability injection (UP); script context as SDK (UB).
- **Chapter:** SPEC-17
- **Origin:** CON-UC-004, CON-UP-021, CON-UP-029, CON-UB-030, VRD-05-02

### TERM-SyscallAbi
- **Definition:** The versioned set of syscall signatures (`v1`, `v2`, …). Logic declares the ABI version it targets.
- **Chapter:** SPEC-17
- **Origin:** CON-UP-029, CON-UC-004, VRD-05-02

### TERM-Sandbox
- **Definition:** The layered containment of logic: no host access except syscalls, operation/depth/size/time limits, policy checks per syscall, output size caps.
- **Aliases:** immune system (UP); runtime guards (UC).
- **Chapter:** SPEC-17
- **Origin:** CON-UP-022, CON-UC-030, VRD-05-03, VRD-10-06

### TERM-NativeHandler
- **Definition:** A kernel function addressed by URI (`ubos://system/Native/<name>@vN`) that implements a mechanic (persist, validate, authorize, index) and can be used wherever logic can.
- **Aliases:** handler bean (LC); `native::` host function (LS).
- **Chapter:** SPEC-17
- **Origin:** CON-LC-005, CON-LC-025, CON-LS-061, VRD-05-05

### TERM-Emit
- **Definition:** A side-effect request returned by logic instead of performed directly (write, notify, schedule, push UI, call). The kernel validates, authorises and dispatches emits.
- **Aliases:** emits as intents (LS); effects as returned intents (US).
- **Not:** the UBTP `Emit` operation (TERM-UbtpOperation), which invokes a named operation.
- **Chapter:** SPEC-17, SPEC-19
- **Origin:** CON-LS-021, CON-LS-025, CON-US-008, VRD-06-05

### TERM-ExecutionMode
- **Definition:** How logic runs: `sync` (inside the caller's process), `job` (asynchronously as a Job), `dry_run`.
- **Aliases:** three execution modes (US); ephemeral eval (UC).
- **Chapter:** SPEC-17
- **Origin:** CON-US-003, CON-UC-025

---

## 7. Rules and validation

### TERM-DerivedSchema
- **Definition:** The JSON Schema of instances of a type, compiled from its properties and their semantic types at each type commit.
- **Aliases:** schema-on-commit (FU); schema from SchemaProvide slots (LS).
- **Chapter:** SPEC-18
- **Origin:** CON-FU-020, CON-LS-052, VRD-09-01

### TERM-ValidationLevel
- **Definition:** The strictness at which rules run: `DRAFT` (structural only, partial data allowed) and `PUBLISH` (all rules).
- **Aliases:** DRAFT_SAVE / FINAL_PUBLISH (LC).
- **Chapter:** SPEC-18
- **Origin:** CON-LC-023, VRD-09-02

### TERM-Constraint
- **Definition:** A pluggable business rule entity that checks data across entities (uniqueness, cardinality, referential rules) over staged and persisted state.
- **Chapter:** SPEC-18
- **Origin:** CON-LC-024, VRD-09-03

### TERM-Invariant
- **Definition:** A rule over the transition from `PREV` to `NEW` state, written in the restricted Rhai subset (ADR-002), with severity `error` (reject the commit) or `warn` (tag the commit metadata).
- **Aliases:** BEL INVARIANT/WARN (UB); EOP (US).
- **Chapter:** SPEC-18
- **Origin:** CON-UB-061, CON-US-013, VRD-09-04

### TERM-Decision
- **Definition:** The outcome of a rule: `allow` (bool), reason, priority, veto flag. Decisions from many rules combine by veto first, then priority.
- **Aliases:** slice voting (US).
- **Chapter:** SPEC-18
- **Origin:** CON-US-027, VRD-09-05

### TERM-Guard
- **Definition:** A rule evaluated before a transition fires. Guards reuse constraints, invariants and decisions.
- **Aliases:** guardrail checklist (UW).
- **Chapter:** SPEC-18, SPEC-19
- **Origin:** CON-UW-022, VRD-09-06

---

## 8. Orchestration

### TERM-Pipeline
- **Definition:** A versioned definition of ordered steps (logic or native handlers) with named jumps and decisions, invoked as a named operation.
- **Aliases:** process definition (LC); process blueprint (LS); workflow (UC genesis).
- **Not:** TERM-Process (an execution record).
- **Chapter:** SPEC-19
- **Origin:** CON-LC-001, CON-LC-010, CON-LC-021, CON-LS-017, VRD-06-02

### TERM-Step
- **Definition:** One element of a pipeline: a reference to logic or a native handler plus input mapping, output mapping and flow control.
- **Chapter:** SPEC-19
- **Origin:** CON-LC-005, CON-LS-020, VRD-06-02

### TERM-Action
- **Definition:** A public, named operation on an entity type (create, edit, approve, submit…) with a state-machine transition and stage steps. Actions bind to logic indirectly.
- **Aliases:** `sys_action` (UC genesis); action chain (UB); Action type (US).
- **Chapter:** SPEC-19
- **Origin:** CON-UB-029, CON-UB-051, CON-US-017, CON-LS-018, VRD-06-03

### TERM-ActionType
- **Definition:** The type hierarchy of actions (base action, entity action, transition action, query action). Transitions are inherited along it.
- **Chapter:** SPEC-19
- **Origin:** CON-US-017, VRD-06-03

### TERM-Transition
- **Definition:** A change of an entity's lifecycle state caused by an action, with guard, stage steps and a transition response.
- **Aliases:** Action FSM (US); entity state machine (UB).
- **Chapter:** SPEC-19
- **Origin:** CON-US-022, CON-UB-032, VRD-06-03

### TERM-Stage
- **Definition:** One of the three phases of an action: `prepare` (build UI/data), `validate`, `execute`.
- **Aliases:** three-stage action protocol (LS).
- **Chapter:** SPEC-19
- **Origin:** CON-LS-018, CON-LS-022, VRD-06-03

### TERM-ProcessInstance
- **Definition:** A durable, long-running orchestration stored as an entity: current state, waiting signals, history.
- **Aliases:** process instance (LS); orchestration by child jobs (UC).
- **Chapter:** SPEC-19
- **Origin:** CON-LS-017, CON-UC-026, VRD-06-04

### TERM-Signal
- **Definition:** A message delivered to a waiting process instance (approval, timer, external event).
- **Chapter:** SPEC-19
- **Origin:** VRD-06-04

### TERM-Intent
- **Definition:** A client-side, typed request expressed in business words (open view, run action, start task). Intents map to UBTP operations.
- **Aliases:** typed intent (UW); command grammar (UW).
- **Chapter:** SPEC-19, SPEC-26
- **Origin:** CON-UW-012, CON-UW-020, VRD-06-06

---

## 9. Events and asynchronous work

### TERM-Outbox
- **Definition:** Rows of domain events written in the same flush as the commits and later delivered to subscribers.
- **Aliases:** email outbox (LC).
- **Chapter:** SPEC-20
- **Origin:** CON-LC-027, VRD-11-01

### TERM-Event
- **Definition:** A fact emitted by the commit path (e.g. `entity.committed`, `process.completed`) with type, subject URI, commit and payload.
- **Chapter:** SPEC-20
- **Origin:** CON-UP-025, VRD-11-01

### TERM-Hook
- **Definition:** An entity that subscribes logic or a pipeline to events matching a filter.
- **Aliases:** SysHook (UC, early); event bus declared as data (UP).
- **Chapter:** SPEC-20
- **Origin:** CON-UP-025, CON-UC-027, VRD-11-02

### TERM-Cron
- **Definition:** An entity that schedules logic or a pipeline by a cron expression.
- **Aliases:** SysCron (UC, early); scheduler declared as data (UP).
- **Chapter:** SPEC-20
- **Origin:** CON-UP-026, CON-UC-027, VRD-11-02

### TERM-Job
- **Definition:** An entity that requests asynchronous execution of logic or a pipeline, with state (QUEUED, RUNNING, SUCCEEDED, FAILED, DEAD), attempts and result reference. Workers pick jobs; each attempt runs as a process.
- **Aliases:** Job as execution record / cast (UC); Logrum (UC, early).
- **Chapter:** SPEC-20 (ADR-004)
- **Origin:** CON-UC-013, CON-UC-032, VRD-11-03

### TERM-Worker
- **Definition:** A runtime role that takes jobs from the queue and runs them.
- **Aliases:** consumer (UC); worker two-step commit (US).
- **Chapter:** SPEC-20
- **Origin:** CON-UC-003, CON-US-025

### TERM-Webhook
- **Definition:** A versioned entity describing an outbound HTTP notification, signed with HMAC.
- **Chapter:** SPEC-20, SPEC-22
- **Origin:** CON-FU-024, VRD-10-05

### TERM-DeadLetter
- **Definition:** The terminal state of a job or event delivery that exhausted its retries.
- **Chapter:** SPEC-20
- **Origin:** VRD-11-03

### TERM-Subscription
- **Definition:** A protocol-level registration by a client to receive events or changes matching a filter.
- **Chapter:** SPEC-20, SPEC-24
- **Origin:** CON-LS-002, VRD-11-04

---

## 10. Query and search

### TERM-SearchIndex
- **Definition:** The derived, typed attribute index (`search_index`) written at commit for searchable properties: `(commit_id, prop_path, val_text | val_num | val_date | val_bool)`.
- **Aliases:** commit-scoped property index (UP); automatic typed index (UB).
- **Chapter:** SPEC-11, SPEC-21
- **Origin:** CON-UP-042, CON-UB-042, CON-LS-042, VRD-12-01

### TERM-CriteriaQuery
- **Definition:** A declarative query over a type: filters on property paths, relationship joins, sorting, paging, projection, `as_of`.
- **Aliases:** generic criteria reader (LS).
- **Chapter:** SPEC-21
- **Origin:** CON-LS-043, VRD-12-02

### TERM-AsOf
- **Definition:** A time or commit bound on a query that returns data as it was then.
- **Chapter:** SPEC-21
- **Origin:** CON-UP-024, VRD-12-03

### TERM-QueryPipeline
- **Definition:** A read operation declared as a pipeline with only read steps.
- **Aliases:** query process (LC).
- **Chapter:** SPEC-21
- **Origin:** CON-LC-002, VRD-12-05

### TERM-Embedding
- **Definition:** A vector computed per version of an entity, asynchronously, for similarity search and retrieval.
- **Chapter:** SPEC-21, SPEC-27
- **Origin:** CON-UB-065, VRD-12-04

---

## 11. Security and tenancy

### TERM-Tenant
- **Definition:** A key dimension of all stored data and an isolation boundary. Each tenant has a code.
- **Aliases:** scope (FU); DB in "Tenant = DB" (UB metaphor).
- **Not:** a namespace; a Context.
- **Chapter:** SPEC-23
- **Origin:** CON-FU-012, CON-UB-011, VRD-18-01

### TERM-RootTenant
- **Definition:** The tenant `logrums` that holds platform packages (genesis) which all tenants inherit. The authority alias `system` resolves to it.
- **Aliases:** `sys` tenant (UC, early); constitution holder (UC).
- **Chapter:** SPEC-23
- **Origin:** CON-UC-019, VRD-18-01

### TERM-Principal
- **Definition:** The acting identity of a request: a user, an API key, an agent or the system identity.
- **Chapter:** SPEC-22
- **Origin:** CON-UP-018, VRD-10-02

### TERM-Role
- **Definition:** A named set of permissions granted to principals through Relationship entities.
- **Chapter:** SPEC-22
- **Origin:** CON-FU-022, CON-LC-017, VRD-10-02

### TERM-Policy
- **Definition:** An IAM-style entity: effect (allow/deny), principals, actions, resource URI patterns, conditions. Deny by default; explicit deny wins.
- **Aliases:** SysPolicy (UC, early); policy as code (UC); policy rules over URIs (FU).
- **Chapter:** SPEC-22
- **Origin:** CON-FU-022, CON-UC-028, CON-UP-052, VRD-10-02

### TERM-ApiKey
- **Definition:** A scoped, expiring machine credential stored as a versioned secret entity (hash only).
- **Chapter:** SPEC-22
- **Origin:** CON-FU-023, VRD-10-05

### TERM-ApprovalRequest
- **Definition:** A deferred commit: a staged change set stored as an entity, applied by the kernel when approved.
- **Chapter:** SPEC-22
- **Origin:** CON-FU-021, VRD-10-04

### TERM-Custody
- **Definition:** The registry fields that separate the **owner** (responsible principal) from the **author** of each commit, plus visibility.
- **Aliases:** owner vs author (UB).
- **Chapter:** SPEC-22
- **Origin:** CON-UB-044, VRD-10-07

### TERM-Masking
- **Definition:** Hiding or partially hiding values on read according to the property's mask slot and the reader's permissions.
- **Chapter:** SPEC-22
- **Origin:** CON-UB-057, VRD-10-03

---

## 12. Protocol

### TERM-Ubtp
- **Definition:** The UBOS Transfer Protocol: one message model with the operations Handshake, Mutate, Emit, Query, Subscribe (and Unsubscribe), bound to TCP, WebSocket and HTTP.
- **Aliases:** UBTP (LS, US).
- **Chapter:** SPEC-24
- **Origin:** CON-LS-002, CON-US-006, VRD-15-01

### TERM-UbtpOperation
- **Definition:** One of the UBTP message kinds. `Mutate` writes entities directly; `Emit` invokes a named operation (action, pipeline) by URI; `Query` reads; `Subscribe` registers for pushes; `Handshake` opens a protocol session.
- **Chapter:** SPEC-24
- **Origin:** CON-LS-002, VRD-15-01

### TERM-Envelope
- **Definition:** The common frame of every UBTP message: protocol version, request ID, context authority, idempotency key, encoding, auth.
- **Aliases:** request envelope (LC).
- **Chapter:** SPEC-24
- **Origin:** CON-LC-003, CON-LS-002

### TERM-ProtocolSession
- **Definition:** The state a server keeps for one connected client after Handshake: principal, context, subscriptions, encoding.
- **Aliases:** session context (LS).
- **Chapter:** SPEC-24
- **Origin:** CON-LS-003

### TERM-TransportBinding
- **Definition:** A mapping of UBTP to a transport: TCP (length-prefixed frames), WebSocket, HTTP routes.
- **Chapter:** SPEC-24
- **Origin:** VRD-15-02

### TERM-NamedOperation
- **Definition:** An action, pipeline or query addressed by a versioned URI (e.g. `ubos://system/Action/entity.edit@v1`).
- **Aliases:** catalogue of named, versioned processes (LC); operation enum (UB).
- **Chapter:** SPEC-24
- **Origin:** CON-LC-001, CON-UB-003, VRD-15-03

---

## 13. UI and client shells

### TERM-Mode
- **Definition:** The presentation purpose of a UI element: `view`, `edit`, `create`, `list`, `filter`, `cell`.
- **Aliases:** `ui_modes` (US); lens (UC).
- **Chapter:** SPEC-25
- **Origin:** CON-US-015, CON-UC-055, VRD-13-01

### TERM-Widget
- **Definition:** A UI component entity addressed by URI. The widget for a property is `f(type chain, mode)`.
- **Aliases:** UI-slot behavior (LS); widget as `ubos://` resource (US).
- **Chapter:** SPEC-25
- **Origin:** CON-LS-053, CON-US-051, CON-US-058, VRD-13-01

### TERM-View
- **Definition:** A versioned entity describing a screen: layout type, regions, bound queries and actions.
- **Aliases:** screen (UC `view_screen`); layout (UB); view engine (US).
- **Chapter:** SPEC-25
- **Origin:** CON-UB-050, CON-US-054, VRD-13-03

### TERM-RenderPayload
- **Definition:** The server-produced description of a UI (resolved widgets, data, actions) that a client renders.
- **Aliases:** SDUI payload (UB, UP).
- **Chapter:** SPEC-25
- **Origin:** CON-UB-050, CON-UP-055, VRD-13-02

### TERM-Projection
- **Definition:** Viewing and editing an entity through any ancestor type; writes merge back into the entity.
- **Aliases:** slice projection / merge (US).
- **Chapter:** SPEC-25
- **Origin:** CON-US-026, VRD-13-04

### TERM-PushedUi
- **Definition:** A UI instruction (card, dialog, toast, navigation) sent by the server or logic to a client.
- **Aliases:** server-pushed UI (LS); script-pushed cards (UC).
- **Chapter:** SPEC-25
- **Origin:** CON-LS-051, CON-UC-056, VRD-13-06

### TERM-Cockpit
- **Definition:** The end-user shell: a bicameral layout with a context/navigation side and an active work zone, an omnibar and a task dock; menu-less.
- **Aliases:** Web OS shell (UP); bicameral UI (UC); workspace column + task dock (UW).
- **Chapter:** SPEC-26
- **Origin:** CON-UC-052, CON-UC-053, CON-UW-001, CON-UP-054, VRD-14-01

### TERM-Omnibar
- **Definition:** The single command/search entry of the cockpit that turns typed text into intents.
- **Aliases:** command bar (UW); command palette.
- **Chapter:** SPEC-26
- **Origin:** CON-UC-053, CON-UW-050, VRD-14-01

### TERM-Dock
- **Definition:** The cockpit area that holds running and pending tasks (passive and active modes).
- **Aliases:** task dock, dock item (UW).
- **Chapter:** SPEC-26
- **Origin:** CON-UW-013, CON-UW-052

### TERM-Studio
- **Definition:** The builder shell for types, logic, views, packages, branches and governance, with time travel, diff and blame.
- **Aliases:** UBOS Studio IDE (FU); Meta-IDE (UB); action automator (US).
- **Chapter:** SPEC-26
- **Origin:** CON-FU-050, CON-UB-053, CON-UB-054, VRD-14-02

### TERM-Client
- **Definition:** One of the five programs that use the platform through UBTP only: web, desktop, mobile, CLI, SDK/agent.
- **Chapter:** SPEC-26, SPEC-31 (ADR-003)
- **Origin:** CON-UC-067, CON-US-064, VRD-19-04

### TERM-IdeIntegration
- **Definition:** Developer tooling that exposes entities as a virtual file system (`ubos://` FS) inside an IDE. Not one of the five clients.
- **Aliases:** VS Code `ubos://` FS (US); VFS as entities (UC).
- **Chapter:** SPEC-26 (ADR-003)
- **Origin:** CON-US-057, CON-UC-044, VRD-14-03

---

## 14. AI

### TERM-AiProvider
- **Definition:** An entity configuring a model endpoint (kind, model, limits, cost).
- **Chapter:** SPEC-27
- **Origin:** CON-FU-061, CON-UB-066

### TERM-AiBuilder
- **Definition:** The loop in which AI generates a package of definitions, dry-runs it, stores it on a draft branch and waits for approval.
- **Aliases:** AI Architect (UP); AI builder loop (UB).
- **Chapter:** SPEC-27
- **Origin:** CON-UP-061, CON-UB-063, VRD-16-01

### TERM-Agent
- **Definition:** A principal driven by an AI model, with its own Context and memory, acting only through syscalls and UBTP.
- **Aliases:** stateful agent (UC).
- **Chapter:** SPEC-27
- **Origin:** CON-UC-062, VRD-16-03

### TERM-Copilot
- **Definition:** The assistant inside shells that turns natural language into intents for the user to confirm; never writes directly.
- **Aliases:** copilot console (UP); front-end copilot contract (UC).
- **Chapter:** SPEC-27
- **Origin:** CON-UP-056, CON-UC-058, VRD-16-04

### TERM-Explanation
- **Definition:** A human-readable account of why a decision or result happened, built from trace trees and decisions.
- **Chapter:** SPEC-27
- **Origin:** CON-US-061, VRD-16-06

---

## 15. Packages, boot and deployment

### TERM-Package
- **Definition:** The unit of installation and delivery: a manifest plus entities (types, actions, logic, views, rules, seed data) and scenarios.
- **Aliases:** bundle, app (UB design); feature (UW catalog).
- **Chapter:** SPEC-28
- **Origin:** CON-UB-062, CON-US-063, VRD-17-01, VRD-22-02

### TERM-Genesis
- **Definition:** The root package that creates the meta-model, the ontology and the platform types in the root tenant on first boot.
- **Aliases:** genesis bootstrap (UP, UC); genesis bootloader (LS); boot data (UB).
- **Chapter:** SPEC-28
- **Origin:** CON-UP-027, CON-UC-033, CON-LS-024, CON-UB-031, VRD-17-01

### TERM-Loader
- **Definition:** The kernel component that validates a package against the meta-schema and writes its entities through normal processes in dependency order.
- **Chapter:** SPEC-28
- **Origin:** CON-UB-031, CON-LC-004, VRD-17-02

### TERM-Kernel
- **Definition:** The platform library (store, meta-model, transactions, runtime, rules, orchestration, security). Hosts embed it.
- **Aliases:** engine (UP); micro-kernel (UC).
- **Chapter:** SPEC-10
- **Origin:** CON-UP-001, CON-UC-001, VRD-19-01

### TERM-Host
- **Definition:** A thin executable that embeds the kernel and exposes transport bindings (server, desktop app, CLI).
- **Aliases:** workspace server (LS); apps (US).
- **Chapter:** SPEC-10, SPEC-31
- **Origin:** CON-LS-001, CON-US-001, VRD-19-01

### TERM-TopologyRole
- **Definition:** A runtime function a node performs: gateway, worker, scheduler, dispatcher, database, runtime store.
- **Chapter:** SPEC-31
- **Origin:** CON-UC-003, VRD-19-03

---

## 16. Development method

### TERM-Scenario
- **Definition:** A file of ordered UBTP messages with expected results, shipped in a package and runnable by the CLI. The acceptance unit of a feature.
- **Aliases:** scenario runner (US); declarative test cases (LC).
- **Chapter:** SPEC-28
- **Origin:** CON-US-004, CON-US-043, CON-LC-061, VRD-22-02

### TERM-InstructionFile
- **Definition:** The repository file that tells implementing agents the conventions and rules (`AGENTS.md` / `CLAUDE.md`), derived from `00-meta/AI-GUIDE.md`.
- **Aliases:** coding rules (LC); house rules (UB).
- **Chapter:** SPEC-02
- **Origin:** CON-LC-063, CON-UB-067, CON-UC-065, VRD-22-01

---

## 17. Alias quick map

Corpus word → spec term. Words with several meanings map to several terms, chosen by the source.

| Corpus word | Source | Spec term |
|---|---|---|
| Logrum (execution record) | UC early | TERM-Process / TERM-Job |
| Logrum (environment, session) | UC | TERM-Context |
| logrum-system (product) | LS | the platform (no term) |
| Workspace (environment) | UC, US | TERM-Context |
| Workspace (protocol server) | LS | TERM-Host |
| Workspace column | UW | TERM-Cockpit (active zone) |
| SysFunction, sys_function, script | UC, UP, UB | TERM-Logic |
| Job (execution record, "cast") | UC | TERM-Process (record) / TERM-Job (async request) |
| SysCron / SysHook / SysPolicy | UC early | TERM-Cron / TERM-Hook / TERM-Policy |
| sys tenant | UC early | TERM-RootTenant (`logrums`) |
| process (definition) | LC | TERM-Pipeline |
| process blueprint | LS | TERM-Pipeline |
| process instance | LS | TERM-ProcessInstance |
| process (business change) | UP | TERM-Process |
| ProcessContext, blackboard | UB, UC, LC, US | TERM-ExecutionContext |
| universe | UB | TERM-Branch |
| seq_num, block height | UB | TERM-Height |
| stash | FU | TERM-Draft |
| environment | FU | TERM-Context (with TERM-Pin) |
| scope | FU | TERM-Authority / TERM-Tenant |
| app scope | LC | TERM-Namespace |
| UODS, metadata specification | UC, US | TERM-MetaSchema |
| BehaviorType, behaviour | LS, US | TERM-Slot / TERM-Behavior |
| RelationType (prop) | LS, US | TERM-Property (with its slots) |
| relationship type | LC | TERM-RelationshipType |
| composite entity, effective entity | US, UB | TERM-EffectiveData |
| self data | UB | TERM-OwnData |
| BEL, EOP | UB, US | TERM-Invariant |
| slice voting | US | TERM-Decision |
| lens, ui_modes | UC, US | TERM-Mode |
| SDUI | UP, UB | TERM-RenderPayload |
| bicameral UI, Web OS shell | UC, UP | TERM-Cockpit |
| command bar | UW | TERM-Omnibar |
| Studio, Meta-IDE | FU, UB | TERM-Studio |
| AI Architect | UP | TERM-AiBuilder |
| Ambassador, Embassy | UC | TERM-HostTenant / TERM-Memory |
| handler bean, native:: | LC, LS | TERM-NativeHandler |
| kernel API, script context | UP, UB | TERM-Syscall |
| immune system | UP | TERM-Sandbox |
| bundle, app | UB | TERM-Package |
| typed intent, command grammar | UW | TERM-Intent |
