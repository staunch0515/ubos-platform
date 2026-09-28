---
id: ANA-UB-08
title: "UB Highlights"
status: complete
phase: P1
depends_on: [ANA-UB-01, ANA-UB-02, ANA-UB-03, ANA-UB-04, ANA-UB-05, ANA-UB-06, ANA-UB-07]
sources: [UB]
---

# UB — Highlights

### HL-UB-001 — Entity as a logical reference with context; context itself an entity
- **Summary:** An entity is (identity, tenant, branch, time); named contexts are entities mapping a slug to tenant + branch; every API and URI addresses a context.
- **Why it stands out:** Gives one coherent answer to multi-tenancy, environments, drafts and time travel; the most precise conceptual foundation in the corpus.
- **Concepts:** CON-UB-010, CON-UB-017, CON-UB-005
- **Reuse recommendation:** adopt
- **Sources:** [UB:docs/The essence of entities.md], [UB:backend/src/main/java/com/ubos/ese/lifecycle/ContextEnvironmentProvider.java]
- **Tags:** identity, context, multi-tenancy

### HL-UB-002 — Tenant = database, Type = table, Slug = key, `_extends` = row schema
- **Summary:** Identity `(tenant, type, slug)` with physical UUID; coarse type folders; polymorphic rows via inheritance; canonical URI `ubos://tenant[/branch]/type/slug[.intent][;params]` with syntax sugar and aliases.
- **Why it stands out:** Combines SQL-like order (tables, keys, partitions) with NoSQL flexibility (per-row schema); the clearest addressing model.
- **Concepts:** CON-UB-011, CON-UB-018
- **Reuse recommendation:** adopt (fuse URI grammar with FU/US/LS in P2 topic 3)
- **Sources:** [UB:backend/src/main/java/com/ubos/core/api/UbosUri.java], [UB:docs/UBOS协议的综合处理.md]
- **Tags:** identity, protocol

### HL-UB-003 — Semantic type system from a single root
- **Summary:** Root → meta type → abstract primitives → abstract business bases → concrete semantic types → tenant extensions; types carry validation, masking, widgets, units.
- **Why it stands out:** Validation, UI and privacy derive from types; changing one type definition updates the whole system.
- **Concepts:** CON-UB-013, CON-UB-057
- **Reuse recommendation:** adopt
- **Sources:** [UB:docs/sys_boot.md], [UB:data/boot/init.json]
- **Tags:** meta-model, type-system

### HL-UB-004 — Universal business ontology as genesis data
- **Summary:** 17 genesis phases deliver Party/Role/Relationship (with validity), contact mechanisms, geography, currency/UOM/calendar, kernel view/logic/security, app manifest, i18n, sequences, config, PKI, Book/Page, IT infrastructure, events and logs.
- **Why it stands out:** A ready foundation (after Silverston's universal data models) so any vertical is assembled by data rather than coded.
- **Concepts:** CON-UB-014, CON-UB-019
- **Reuse recommendation:** adopt as the platform's base domain library (P2 topic 2 / spec domain packs)
- **Sources:** [UB:docs/sys_boot.md]
- **Tags:** domain-pack, meta-model

### HL-UB-005 — Reified execution stack + typed keys + opcode enums
- **Summary:** One `ProcessContext` per request records a tree of handler frames (inputs, outputs, logs, vars, failures); typed `ContextKey<T>` contracts; kernel capabilities as opcode enums composed into higher operations.
- **Why it stands out:** Full, persistable execution trace of every business transaction and a closed kernel instruction set — ideal for audit, debugging and AI analysis.
- **Concepts:** CON-UB-003, CON-UB-004, CON-UB-020
- **Reuse recommendation:** adopt (add fork/join for parallel branches)
- **Sources:** [UB:backend/src/main/java/com/ubos/core/context/ProcessContext.java], [UB:backend/src/main/java/com/ubos/bpu/EntityDataOperation.java]
- **Tags:** context, observability, process

### HL-UB-006 — Draft → publish with per-user branches and real merge strategies
- **Summary:** `draft/{user}` branches created on demand; batch save with CAS; publish = merge (FORCE_SOURCE) + draft cleanup; strategies FAIL_ON_CONFLICT / FORCE_SOURCE / FORCE_TARGET / THREE_WAY_MERGE with common-ancestor search; revert as new commit; diff as JSON Patch.
- **Why it stands out:** The most Git-faithful branch semantics among the Java repos.
- **Concepts:** CON-UB-021 … CON-UB-025
- **Reuse recommendation:** adopt (combine with FU conflict UI and LC staged validation)
- **Sources:** [UB:backend/src/main/java/com/ubos/bpu/handler/MergeBranchHandler.java]
- **Tags:** versioning, workflow

### HL-UB-007 — Three-pass object lifecycle with provenance
- **Summary:** Compose (`_extends`, depth/cycle-safe) → resolve (`{{ubos://…}}`, cross-context, type-aware) → execute (actions only on interaction); `inheritanceChain` provenance; cache raw fragments and assemble JIT.
- **Why it stands out:** Clean separation of inheritance, linking and execution — the core of a metadata-driven runtime.
- **Concepts:** CON-UB-027, CON-UB-028, CON-UB-045
- **Reuse recommendation:** adopt
- **Sources:** [UB:backend/src/main/java/com/ubos/ese/lifecycle/UbosSchemaComposer.java], [UB:backend/src/main/java/com/ubos/ese/lifecycle/UbosReferenceResolver.java]
- **Tags:** meta-model, rehydration

### HL-UB-008 — Enterprise-grade physical design
- **Summary:** Six tables for everything; tenant column; JSONB + GIN; vector embeddings per version; yearly partitions; branch-height CAS; lifecycle status + tombstones with terminator commit; owner vs author; app and billing linkage on processes.
- **Why it stands out:** Addresses scale, audit, AI search and billing from day one.
- **Concepts:** CON-UB-040 … CON-UB-044
- **Reuse recommendation:** adopt / adapt (compare with UC/US 6-table variants, P2 topic 1)
- **Sources:** [UB:backend/src/main/resources/db/migration/V1__init_schema.sql], [UB:docs/database_strong.md]
- **Tags:** persistence, ai, multi-tenancy

### HL-UB-009 — Genesis through the kernel, with dependency ordering and remote store
- **Summary:** Seeds are loaded via kernel operations with topological batches over `_extends`, hash-based dirty checks, a loader branch and publish; bundles can come from a central UBOS Store; a runtime registry keeps live metadata in memory.
- **Concepts:** CON-UB-031
- **Reuse recommendation:** adopt
- **Sources:** [UB:docs/元数据加载器.md], [UB:backend/src/main/java/com/ubos/boot/KernelManagerService.java]
- **Tags:** boot, metadata

### HL-UB-010 — Server-driven UI with versioned layouts and action chains
- **Summary:** Layout entities rendered through resolution/hydration/ACL transformation; UI A/B testing and rollback by branch; chained actions; realtime delta autosave with conflict handling.
- **Concepts:** CON-UB-050 … CON-UB-052
- **Reuse recommendation:** adopt (compare with US widget/ui_modes and UP SDUI, P2 topic 13)
- **Sources:** [UB:docs/动态视图渲染器.md]
- **Tags:** ui-rendering, versioning

### HL-UB-011 — Inheritance-aware editing UX
- **Summary:** Inherited vs overridden values, reset to parent, X-ray per ancestor, lineage tree, pending-changes tray, draft/main console.
- **Why it stands out:** Makes a powerful but invisible meta-model understandable to users.
- **Concepts:** CON-UB-053, CON-UB-015
- **Reuse recommendation:** adopt
- **Sources:** [UB:docs/forntend.md]
- **Tags:** frontend, meta-model

### HL-UB-012 — BEL: constitution invariants, asset algebra, pinned facts
- **Summary:** A small declarative rules language: invariants over PREV/NEW, unit-safe assets (fungible/non-fungible), `PIN` external facts into commits, merge-time flows, commands vs reactions, inherited constitutions, identity guards.
- **Why it stands out:** Business-physics guarantees enforced by the kernel on every commit, regardless of which script or UI made the change.
- **Concepts:** CON-UB-060, CON-UB-061
- **Reuse recommendation:** adapt (as the rules/invariant layer in P2 topic 9)
- **Sources:** [UB:docs/BEL Pro.md]
- **Tags:** dsl, rules, finance

### HL-UB-013 — Action → Logic indirection
- **Summary:** Typed public operations (actions with params and handler URI) decoupled from versioned implementations (logic scripts).
- **Concepts:** CON-UB-029
- **Reuse recommendation:** adopt
- **Sources:** [UB:backend/src/main/java/com/ubos/ese/handler/UbosActionHandler.java]
- **Tags:** api, scripting

### HL-UB-014 — Self-hosting IDE and executable documents
- **Summary:** The IDE and menus are entities (the system edits itself); dry runs, graph walking, hot patching; business pages as executable Markdown with a Wasm state engine.
- **Concepts:** CON-UB-054, CON-UB-055
- **Reuse recommendation:** inspiration-only / adapt
- **Sources:** [UB:docs/IDE.md], [UB:docs/UBOS Smart Document.md]
- **Tags:** frontend, document

### HL-UB-015 — AI builder loop and AI-native i18n
- **Summary:** AI reflects on the meta-model, emits entity bundles, which the kernel validates (dry run, compile) and commits atomically; errors feed a learning loop; translations are generated just in time as entities.
- **Concepts:** CON-UB-063, CON-UB-064, CON-UB-065, CON-UB-066
- **Reuse recommendation:** adapt
- **Sources:** [UB:docs/ai-dev.md], [UB:docs/I18n.md]
- **Tags:** ai, codegen, i18n

### HL-UB-016 — Intent / Rules / Facts separation
- **Summary:** UBOS (intent, events) – ESE (rules, state machines, guards) – BPU (atomic facts, ledger) with strict dependency and "no I/O in rules".
- **Concepts:** CON-UB-001, CON-UB-032
- **Reuse recommendation:** adopt (as the architectural spine in the spec)
- **Sources:** [UB:docs/UBOS Three Layer System.md]
- **Tags:** state-machine, process
