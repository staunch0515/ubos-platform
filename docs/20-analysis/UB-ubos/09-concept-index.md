---
id: ANA-UB-09
title: "UB Concept Index"
status: complete
phase: P1
depends_on: [ANA-UB-01, ANA-UB-02, ANA-UB-03, ANA-UB-04, ANA-UB-05, ANA-UB-06]
sources: [UB]
---

# UB — Concept Index

| ID | Name | File | Tags | Highlight |
|---|---|---|---|---|
| CON-UB-001 | Intent / Rules / Facts layering | 01 | process, state-machine, versioning | HL-UB-016 |
| CON-UB-002 | SPI-first kernel contracts | 01 | plugin, api | – |
| CON-UB-003 | Operation enums as instruction set | 01 | api, process, versioning | HL-UB-005 |
| CON-UB-004 | Context-in / context-out handlers | 01 | context, process, observability | HL-UB-005 |
| CON-UB-005 | Context-addressed HTTP API | 01 | api, multi-tenancy | HL-UB-001 |
| CON-UB-006 | OS-ification blueprint | 01 | plugin, integration, scripting | – |
| CON-UB-010 | Entity = identity + context + reference | 02 | identity, context, versioning | HL-UB-001 |
| CON-UB-011 | Tenant = DB, Type = table, Slug = PK | 02 | identity, schema, multi-tenancy | HL-UB-002 |
| CON-UB-012 | Registry lifecycle and custody metadata | 02 | entity, permission, persistence | HL-UB-008 |
| CON-UB-013 | Layered semantic type system | 02 | meta-model, type-system, ui-rendering, rules | HL-UB-003 |
| CON-UB-014 | Universal business ontology | 02 | meta-model, domain-pack, org-model, finance, document | HL-UB-004 |
| CON-UB-015 | UbosEntity effective vs self data | 02 | entity, versioning, meta-model | HL-UB-011 |
| CON-UB-016 | State DTO vs effective entity | 02 | versioning, entity | – |
| CON-UB-017 | Context is an entity | 02 | context, multi-tenancy, versioning | HL-UB-001 |
| CON-UB-018 | `ubos://` URI grammar | 02 | identity, protocol | HL-UB-002 |
| CON-UB-019 | Templates and instances (Book/Page) | 02 | document, domain-pack, ui-rendering | HL-UB-004 |
| CON-UB-020 | ProcessContext execution stack | 03 | context, observability, process | HL-UB-005 |
| CON-UB-021 | Batch commit with head CAS | 03 | versioning, persistence, process | HL-UB-006 |
| CON-UB-022 | Draft → publish lifecycle | 03 | versioning, workflow | HL-UB-006 |
| CON-UB-023 | Merge strategies incl. three-way | 03 | versioning | HL-UB-006 |
| CON-UB-024 | Revert as new commit | 03 | versioning | HL-UB-006 |
| CON-UB-025 | Diff as JSON Patch | 03 | versioning | HL-UB-006 |
| CON-UB-026 | Micro-commit service | 03 | versioning, persistence | – |
| CON-UB-027 | Three-pass object lifecycle | 03 | meta-model, rehydration, protocol | HL-UB-007 |
| CON-UB-028 | Evaluation with read-through cache | 03 | cache, rehydration | HL-UB-007 |
| CON-UB-029 | Action → Logic indirection | 03 | scripting, api, workflow | HL-UB-013 |
| CON-UB-030 | Script context as SDK | 03 | scripting, api | – |
| CON-UB-031 | Genesis ingestion through the kernel | 03 | boot, metadata | HL-UB-009 |
| CON-UB-032 | Entity state machine contract (design) | 03 | state-machine, rules, workflow | HL-UB-016 |
| CON-UB-033 | Instance entity and network boot (design) | 03 | boot, deployment, auth | – |
| CON-UB-040 | Six-table partitioned AI-ready model | 04 | persistence, schema, ai, multi-tenancy | HL-UB-008 |
| CON-UB-041 | Branch height and head CAS | 04 | versioning, persistence | HL-UB-008 |
| CON-UB-042 | Automatic typed search index | 04 | search, query | – |
| CON-UB-043 | Process log with app and billing linkage | 04 | process, observability, finance | HL-UB-008 |
| CON-UB-044 | Tombstones, status, owner vs author | 04 | versioning, permission, entity | HL-UB-008 |
| CON-UB-045 | Cache raw fragments, JIT assembly | 04 | cache, rehydration | HL-UB-007 |
| CON-UB-046 | Context export/import archives | 04 | integration, deployment, versioning | – |
| CON-UB-047 | Snapshot + delta, micro-commit squash | 04 | versioning, persistence | – |
| CON-UB-050 | SDUI render pipeline, versioned layouts | 05 | ui-rendering, view, versioning | HL-UB-010 |
| CON-UB-051 | Action protocol with chains | 05 | ui-rendering, workflow, permission | HL-UB-010 |
| CON-UB-052 | Smart form state and realtime deltas | 05 | frontend, form, versioning | HL-UB-010 |
| CON-UB-053 | Inheritance-aware Studio | 05 | frontend, meta-model, versioning | HL-UB-011 |
| CON-UB-054 | Self-hosting Meta-IDE | 05 | frontend, scripting, metadata | HL-UB-014 |
| CON-UB-055 | Executable Book / Smart Document | 05 | document, ui-rendering, dsl | HL-UB-014 |
| CON-UB-056 | Server-rendered entity pages | 05 | ui-rendering, api | – |
| CON-UB-057 | Types drive widgets, formatting, masking | 05 | type-system, ui-rendering, permission | HL-UB-003 |
| CON-UB-060 | Three language layers | 06 | dsl, rules, document | HL-UB-012 |
| CON-UB-061 | BEL | 06 | dsl, rules, finance, versioning | HL-UB-012 |
| CON-UB-062 | Apps as entity bundles (design) | 06 | plugin, deployment, domain-pack | – |
| CON-UB-063 | AI builder loop | 06 | ai, codegen, copilot | HL-UB-015 |
| CON-UB-064 | AI-native JIT i18n | 06 | ai, i18n | HL-UB-015 |
| CON-UB-065 | Embeddings stored with versions | 06 | ai, search | HL-UB-015 |
| CON-UB-066 | LLM deployment strategy, `ctx.ai()` | 06 | ai, integration, scripting | HL-UB-015 |
| CON-UB-067 | House-rule prompts | 06 | ai, codegen | – |
