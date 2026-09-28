---
id: ANA-LC-09
title: "LC Concept Index"
status: complete
phase: P1
depends_on: [ANA-LC-01, ANA-LC-02, ANA-LC-03, ANA-LC-04, ANA-LC-05, ANA-LC-06]
sources: [LC]
---

# LC — Concept Index

| ID | Name | File | Tags | Highlight |
|---|---|---|---|---|
| CON-LC-001 | API is a catalogue of named, versioned processes | 01 | api, process | HL-LC-001 |
| CON-LC-002 | Command/query split with shared step model | 01 | process, query, api | HL-LC-008 |
| CON-LC-003 | Request envelope: idempotency → execute → audit | 01 | api, observability | HL-LC-006 |
| CON-LC-004 | Definitions as files with hot-reloading loader | 01 | metadata, boot | – |
| CON-LC-005 | Steps are named beans from a handler library | 01 | process, plugin | HL-LC-001 |
| CON-LC-010 | Process definition format | 02 | process, dsl, metadata | HL-LC-001 |
| CON-LC-011 | Process context as a blackboard | 02 | context, process | – |
| CON-LC-012 | Change set + dynamic model (unit of work) | 02 | entity, process, versioning | HL-LC-002 |
| CON-LC-013 | Versioning by name for types, processes, handlers | 02 | versioning, api | HL-LC-007 |
| CON-LC-014 | Relationship as versioned entity + type catalogue | 02 | relationship, entity, metadata, versioning | HL-LC-003 |
| CON-LC-015 | Entity schemas: JSON Schema + extensions | 02 | schema, rules, relationship | HL-LC-004 |
| CON-LC-016 | App scoping | 02 | multi-tenancy, identity | – |
| CON-LC-017 | Access model as relationships | 02 | permission, relationship, org-model | HL-LC-013 |
| CON-LC-018 | Template vs publication domain pattern | 02 | entity, versioning, domain-pack | HL-LC-011 |
| CON-LC-020 | Command execution algorithm | 03 | process, workflow | HL-LC-001 |
| CON-LC-021 | Flow control by named jumps and decision steps | 03 | workflow, rules, dsl | HL-LC-009 |
| CON-LC-022 | Atomic persistence of all change sets | 03 | persistence, versioning, process | HL-LC-002 |
| CON-LC-023 | Staged validation levels | 03 | rules, schema, workflow | HL-LC-004 |
| CON-LC-024 | Two-layer validation with pluggable constraints | 03 | rules, relationship, schema | HL-LC-004, HL-LC-005 |
| CON-LC-025 | Generic cross-cutting handlers | 03 | process, plugin, rules | HL-LC-006 |
| CON-LC-026 | Idempotency keys with response replay | 03 | api, persistence | HL-LC-006 |
| CON-LC-027 | Scheduled work invokes processes; email outbox | 03 | event, process, integration | HL-LC-006 |
| CON-LC-028 | Composite business handlers (QuizBucks) | 03 | finance, domain-pack, process | HL-LC-011 |
| CON-LC-029 | Remote steps (designed) | 03 | integration, process | – |
| CON-LC-040 | Typed EAV snapshot per commit | 04 | persistence, schema, query | HL-LC-012 |
| CON-LC-041 | Process log records handler versions | 04 | process, versioning, observability | HL-LC-007 |
| CON-LC-042 | Commit types and soft delete | 04 | versioning, entity | – |
| CON-LC-043 | Operational tables beside the version store | 04 | persistence, auth, observability | – |
| CON-LC-044 | Dual audit: DB log + JSON transaction files | 04 | observability, versioning | HL-LC-006 |
| CON-LC-045 | Branch as per-request staging area | 04 | versioning | – |
| CON-LC-046 | Redis caching strategy proposal | 04 | cache, finance | – |
| CON-LC-050 | URL grammar and response contract | 05 | api | – |
| CON-LC-051 | End-user app (QuizBucks) | 05 | frontend, domain-pack, finance | HL-LC-011 |
| CON-LC-052 | Admin app | 05 | frontend, domain-pack, ai, i18n | HL-LC-011 |
| CON-LC-053 | Conversational requirements → generated apps | 05 | ai, codegen, frontend | – |
| CON-LC-060 | Two-speed extension model | 06 | plugin, process | – |
| CON-LC-061 | Declarative data-driven test cases | 06 | dsl, process | HL-LC-010 |
| CON-LC-062 | AI as a product feature | 06 | ai, domain-pack | – |
| CON-LC-063 | Coding rules for AI-assisted development | 06 | codegen | – |
