---
id: ANA-UC-09
title: "UC Concept Index"
status: complete
phase: P1
depends_on: [ANA-UC-01, ANA-UC-02, ANA-UC-03, ANA-UC-04, ANA-UC-05, ANA-UC-06]
sources: [UC]
---

# UC — Concept Index

| ID | Name | File | Tags | Highlight |
|---|---|---|---|---|
| CON-UC-001 | Micro-kernel + user-space scripts | 01 | plugin, scripting, process | – |
| CON-UC-002 | Architecture grown in documented stages | 01 | process, metadata | HL-UC-014 |
| CON-UC-003 | Async producer/consumer split | 01 | process, integration, scheduling | HL-UC-005 |
| CON-UC-004 | Syscall layer as kernel ABI | 01 | scripting, api, permission | HL-UC-003 |
| CON-UC-005 | Sync/async bridge + thread-local context | 01 | scripting, context, multi-tenancy | HL-UC-003 |
| CON-UC-006 | Loader → Runner → Committer | 01 | process, observability | HL-UC-001 |
| CON-UC-007 | Smart entity (kernel as resolver) | 01 | entity, meta-model, rehydration | HL-UC-009 |
| CON-UC-010 | Entity: UUID + locator + payload + head | 02 | identity, entity, versioning | – |
| CON-UC-011 | Open entity type | 02 | meta-model, type-system, schema | HL-UC-009 |
| CON-UC-012 | Explicit `_extends` inheritance | 02 | meta-model, inheritance, rehydration | HL-UC-009 |
| CON-UC-013 | Logic (mould) / Job (cast) | 02 | scripting, process, versioning | HL-UC-001 |
| CON-UC-014 | Strict input wiring / schema validation | 02 | validation, scripting | – |
| CON-UC-015 | Workspace environment entity | 02 | context, multi-tenancy, versioning | HL-UC-004 |
| CON-UC-016 | `ubos://` URI with commit pin | 02 | identity, protocol | HL-UC-010 |
| CON-UC-017 | UODS meta-schema | 02 | meta-model, validation, schema | HL-UC-008 |
| CON-UC-018 | Genesis semantic ontology | 02 | meta-model, type-system, domain-pack | HL-UC-008 |
| CON-UC-019 | `logrums` root tenant + constitution | 02 | multi-tenancy, governance, permission | HL-UC-011 |
| CON-UC-020 | Execution is commit | 03 | versioning, process, observability | HL-UC-001 |
| CON-UC-021 | Version anchoring + provenance | 03 | versioning, observability | HL-UC-002 |
| CON-UC-022 | Holographic ProcessContext | 03 | context, observability | HL-UC-002 |
| CON-UC-023 | Ambassador mode + memory | 03 | context, ai, multi-tenancy | HL-UC-004 |
| CON-UC-024 | Environment vs execution; host vs sovereign | 03 | context, multi-tenancy, permission | HL-UC-004 |
| CON-UC-025 | Ephemeral eval | 03 | scripting, api | HL-UC-005 |
| CON-UC-026 | Orchestration by child jobs | 03 | workflow, process | HL-UC-005 |
| CON-UC-027 | Cron and Hook entities | 03 | events, scheduling, workflow | HL-UC-005 |
| CON-UC-028 | Policy as code | 03 | permission, scripting, governance | HL-UC-011 |
| CON-UC-029 | Import of Logic modules | 03 | scripting, plugin | – |
| CON-UC-030 | Runtime guards + log capture | 03 | observability, scripting | HL-UC-013 |
| CON-UC-031 | Direct commit syscall + raw editor | 03 | persistence, scripting, api | HL-UC-012 |
| CON-UC-032 | Idempotent worker | 03 | process, idempotency | HL-UC-001 |
| CON-UC-033 | Genesis bootstrap | 03 | boot, metadata | HL-UC-012 |
| CON-UC-034 | Constitutional functions (design) | 03 | governance, permission | HL-UC-011 |
| CON-UC-035 | Robustness self-audit (NFRs) | 03 | process, persistence, observability | HL-UC-013 |
| CON-UC-040 | Six tables only | 04 | persistence, schema, meta-model | – |
| CON-UC-041 | Atomic write unit + ID-split guard | 04 | persistence, versioning | HL-UC-001 |
| CON-UC-042 | Process→commit typed lineage | 04 | observability, versioning, process | HL-UC-002 |
| CON-UC-043 | Redis roles | 04 | cache, events, scheduling | – |
| CON-UC-044 | VFS as entities | 04 | persistence, document | – |
| CON-UC-045 | Branches structural, main-only reads | 04 | versioning, context | – |
| CON-UC-046 | Search (name / JSONB containment) | 04 | search, query | – |
| CON-UC-050 | Minimal verb API | 05 | api, protocol | HL-UC-005 |
| CON-UC-051 | Reference hydration by `_id` | 05 | rehydration, api, protocol | – |
| CON-UC-052 | Bicameral UI | 05 | frontend, ui-rendering, events | HL-UC-007 |
| CON-UC-053 | Menu-less cockpit, context hierarchy | 05 | frontend, context, multi-tenancy | HL-UC-007 |
| CON-UC-054 | Schema-to-UI executor | 05 | ui-rendering, form, metadata | – |
| CON-UC-055 | Lens system | 05 | ui-rendering, meta-model, type-system | HL-UC-006 |
| CON-UC-056 | Script-pushed cards | 05 | ui-rendering, scripting, events | HL-UC-007 |
| CON-UC-057 | Universal Resource Browser | 05 | frontend, versioning, protocol | HL-UC-010 |
| CON-UC-058 | Front-end copilot contract | 05 | ai, frontend, copilot | HL-UC-014 |
| CON-UC-059 | Submit → poll → present | 05 | frontend, process | HL-UC-007 |
| CON-UC-060 | LLM syscall | 06 | ai, integration, scripting | HL-UC-014 |
| CON-UC-061 | Executable Book levels | 06 | document, ai, dsl | HL-UC-014 |
| CON-UC-062 | Stateful agent runtime | 06 | ai, context | HL-UC-014 |
| CON-UC-063 | Stdlib + killer demo (design) | 06 | scripting, domain-pack | – |
| CON-UC-064 | AI-native kernel roadmap (design) | 06 | ai, search, codegen | – |
| CON-UC-065 | Prompt-driven development | 06 | ai, codegen, process | HL-UC-014 |
| CON-UC-066 | URI as universal argument | 06 | protocol, integration, versioning | – |
| CON-UC-067 | "One core, five clients" target | 06 | protocol, deployment | – |
