---
id: ANA-UC-08
title: "UC Highlights"
status: complete
phase: P1
depends_on: [ANA-UC-01, ANA-UC-02, ANA-UC-03, ANA-UC-04, ANA-UC-05, ANA-UC-06, ANA-UC-07]
sources: [UC]
---

# UC — Highlights

### HL-UC-001 — Execution is commit: durable job capsules with atomic result and lineage
- **Summary:** Every invocation is a persisted Job entity created before execution; the result, a process log and typed lineage rows are committed in one transaction with an optimistic lock; re-delivery is idempotent.
- **Why it stands out:** Merges job queue, audit log and version control into one mechanism; nothing runs without a trace, nothing is traced without its inputs.
- **Concepts:** CON-UC-013, CON-UC-020, CON-UC-032, CON-UC-042
- **Reuse recommendation:** adopt
- **Sources:** [UC:src/infra/store/writer/runtime.rs], [UC:docs/master/01_first step.md]
- **Tags:** process, versioning, observability

### HL-UC-002 — Version anchoring and provenance
- **Summary:** At enqueue time the Job pins the head commits of its environment and code (`__anchors`); the worker loads exactly those (`load_by_commit`); lineage records `DEPENDS_ON_CTX`, `DEPENDS_ON_FUNC`, `OUTPUT`.
- **Why it stands out:** Deterministic replay and exact "which code, which configuration produced this data" — a data-lineage graph for free; unique among the eight repos in this explicit form.
- **Concepts:** CON-UC-021, CON-UC-022, CON-UC-042
- **Reuse recommendation:** adopt
- **Sources:** [UC:src/kernel/executor/worker/loader.rs], [UC:docs/plan/Plan_15.md]
- **Tags:** versioning, observability

### HL-UC-003 — Syscall ABI for sandboxed logic with a thread-local execution context
- **Summary:** Scripts reach data, files, AI, memory, UI and other jobs only through named `sys_*` functions; the kernel knows the caller from a thread-local context (tenant, user, logs, memory) and enforces rules, emits events and records logs.
- **Why it stands out:** A clean user-space/kernel boundary for logic-as-data; one choke point for tenancy, policy, events and audit.
- **Concepts:** CON-UC-004, CON-UC-005, CON-UC-030
- **Reuse recommendation:** adopt (define the syscall set as a versioned contract in the spec)
- **Sources:** [UC:src/kernel/syscalls/], [UC:src/kernel/context.rs]
- **Tags:** scripting, api, permission

### HL-UC-004 — Environment vs execution; Ambassador sessions with memory
- **Summary:** Workspace = stable environment (sovereign tenant, branch, env vars, memory); Process = transient execution (actor, request, trace); handshake by alias; host vs sovereign tenant; rules for foreign pure vs non-pure logic and data ownership.
- **Why it stands out:** A precise model for multi-turn agents and for vendor code running inside customer tenants.
- **Concepts:** CON-UC-015, CON-UC-023, CON-UC-024
- **Reuse recommendation:** adapt (combine with UB context-as-entity, CON-UB-017; version memory as data)
- **Sources:** [UC:docs/master/35_logrum.md], [UC:docs/plan/Plan_10.md]
- **Tags:** context, multi-tenancy, ai

### HL-UC-005 — Deploy / invoke / eval with async producer–consumer and triggers as entities
- **Summary:** Minimal verbs; `invoke` → 202 + queue; `eval` for console scripts (also recorded); Cron and Hook entities drive scheduler and event bus; child jobs for orchestration.
- **Why it stands out:** A complete serverless-style execution model in very little code, all configured by data.
- **Concepts:** CON-UC-003, CON-UC-025, CON-UC-026, CON-UC-027, CON-UC-050
- **Reuse recommendation:** adopt (add outbox and concurrent workers per CON-UC-035)
- **Sources:** [UC:src/kernel/manager.rs], [UC:src/services/]
- **Tags:** process, events, scheduling

### HL-UC-006 — Lens system: component = f(type chain, mode)
- **Summary:** UI chosen by a matrix entity over entity type × view mode, walking `_extends` with semantic fallback.
- **Why it stands out:** O(1) UI effort for new types, global consistency and graceful degradation; complements UB/US type-driven widgets.
- **Concepts:** CON-UC-055, CON-UC-012
- **Reuse recommendation:** adopt (merge with UB CON-UB-057 and US ui modes in P2 topic 13)
- **Sources:** [UC:docs/front/front_04.md]
- **Tags:** ui-rendering, meta-model

### HL-UC-007 — Bicameral, menu-less cockpit with strict context hierarchy
- **Summary:** User → Workspace → Command → Stage; Active (intent) vs Passive (push) zones; context headers on every call; context switch resets the stage; script-pushed cards.
- **Why it stands out:** A UX model that mirrors the kernel's context model and prevents cross-tenant mistakes by design.
- **Concepts:** CON-UC-052, CON-UC-053, CON-UC-056, CON-UC-059
- **Reuse recommendation:** adopt
- **Sources:** [UC:docs/front/front_02.md], [UC:docs/plan/Plan_12.md]
- **Tags:** frontend, context

### HL-UC-008 — UODS: strict meta-schema for metadata definitions
- **Summary:** Mandatory fields for objects and properties, type-specific requirements (relation target, enum dict code, numeric scale, primitive/store type), explicit `_extends`, naming regex.
- **Why it stands out:** Turns the self-describing type system into validatable data; a ready rule set for the spec's metadata chapter and for AI-generated metadata.
- **Concepts:** CON-UC-017, CON-UC-018
- **Reuse recommendation:** adopt (as metadata validation rules; compare with UB type chain in P2 topic 2)
- **Sources:** [UC:docs/init_data.md]
- **Tags:** meta-model, validation

### HL-UC-009 — Open entity types with explicit, recursive inheritance
- **Summary:** Type = string partition tag; inheritance only through `_extends` URIs; property lookup walks the chain (depth-limited); the kernel is injected into entities as resolver.
- **Why it stands out:** Dynamic modelling without recompiling, with a single, visible inheritance mechanism.
- **Concepts:** CON-UC-011, CON-UC-012, CON-UC-007
- **Reuse recommendation:** adopt
- **Sources:** [UC:src/kernel/resolution.rs], [UC:src/domain/entity_type.rs]
- **Tags:** meta-model, inheritance

### HL-UC-010 — Universal Resource Browser over `ubos://` with time travel
- **Summary:** Address bar for any resource, JSON at HEAD or any commit, version timeline; `?commit=` in the URI.
- **Concepts:** CON-UC-016, CON-UC-057
- **Reuse recommendation:** adopt
- **Sources:** [UC:ui/components/Tools/UbosResourceBrowser.tsx], [UC:docs/plan/plan_19.md]
- **Tags:** frontend, versioning, protocol

### HL-UC-011 — Policy as code with tenant constitution
- **Summary:** Authorization as a tenant-owned script over (ctx, target, action); root tenant `logrums` with constitutional functions.
- **Concepts:** CON-UC-019, CON-UC-028, CON-UC-034
- **Reuse recommendation:** adapt (fail-closed default; bind into the syscall choke point; compare FU governance-as-data)
- **Sources:** [UC:src/kernel/security.rs], [UC:docs/plan/Plan_13.md]
- **Tags:** permission, governance

### HL-UC-012 — Self-bootstrapping minimum viable system
- **Summary:** Boot ensures the first function (raw entity editor), loads genesis JSON idempotently with auto-`_extends`, and creates the genesis environment; the editor then creates everything else.
- **Concepts:** CON-UC-031, CON-UC-033
- **Reuse recommendation:** adapt (add UB's dependency ordering and UODS validation)
- **Sources:** [UC:src/kernel/bootstrap/]
- **Tags:** boot, metadata

### HL-UC-013 — Recorded non-functional requirements from self-audits
- **Summary:** Atomicity of data + audit, lost-update protection, session write-back, archive size limits, outbox against dual writes, worker concurrency, instruction quotas, unified errors, cross-tenant access audit.
- **Concepts:** CON-UC-035, CON-UC-030
- **Reuse recommendation:** adopt (as NFR seeds)
- **Sources:** [UC:docs/plan/Plan_09.md], [UC:docs/plan/Plan_01.md]
- **Tags:** process, persistence

### HL-UC-014 — Executable Book and agent-OS vision with AI-first development method
- **Summary:** Four-level Executable Book; stateful agent runtime; LLM syscall; copilot that emits UI instructions; prompt-driven, stage-by-stage construction with handoff documents and house rules.
- **Concepts:** CON-UC-058, CON-UC-060, CON-UC-061, CON-UC-062, CON-UC-065
- **Reuse recommendation:** inspiration-only / adapt
- **Sources:** [UC:docs/book.md], [UC:docs/front/front_03.md], [UC:docs/plan/Plan_14.md]
- **Tags:** ai, document, codegen
