---
id: ANA-FU-09
title: "FU Concept Index"
status: complete
phase: P1
depends_on: [ANA-FU-01, ANA-FU-02, ANA-FU-03, ANA-FU-04, ANA-FU-05, ANA-FU-06]
sources: [FU]
---

# FU — Concept Index

Shared-with-UP concepts are indexed in ANA-UP-09 and not repeated.

| ID | Name | File | Tags | Highlight |
|---|---|---|---|---|
| CON-FU-001 | Governance layer built on the versioned kernel | 01 | metadata, permission, versioning | HL-FU-001 |
| CON-FU-002 | Three API families over one kernel | 01 | api, cli | – |
| CON-FU-003 | Access boundary filters (API key scopes, localhost console) | 01 | auth, api, integration | HL-FU-011 |
| CON-FU-010 | Canonical `ubos://` URI (backend grammar) | 02 | identity, protocol | HL-FU-008 |
| CON-FU-011 | Extended URI modes (frontend grammar) | 02 | identity, protocol, integration | HL-FU-008 |
| CON-FU-012 | Tenant-scoped identity | 02 | multi-tenancy, identity | – |
| CON-FU-013 | Namespaces from dotted slugs | 02 | identity, frontend | HL-FU-010 |
| CON-FU-014 | Governance entity catalogue | 02 | metadata, auth, permission, integration | HL-FU-001 |
| CON-FU-015 | Soft deletion by tombstone commit | 02 | versioning, entity | HL-FU-001 |
| CON-FU-016 | Stable identity, mutable slug | 02 | identity, versioning | HL-FU-012 |
| CON-FU-020 | Schema-on-commit with versioned schemas | 03 | schema, rules, versioning | HL-FU-002 |
| CON-FU-021 | Approval = deferred commit | 03 | workflow, state-machine, permission, versioning | HL-FU-003 |
| CON-FU-022 | Roles + policy rules over URIs | 03 | permission, auth, org-model | HL-FU-004 |
| CON-FU-023 | API keys as versioned secrets | 03 | auth, integration | HL-FU-011 |
| CON-FU-024 | Outbound webhooks as versioned configuration | 03 | integration, event | HL-FU-011 |
| CON-FU-025 | Merge (source-wins deep merge) | 03 | versioning | HL-FU-005 |
| CON-FU-026 | Three-way conflict detection by field path | 03 | versioning | HL-FU-005 |
| CON-FU-027 | Revert by moving the head pointer | 03 | versioning | – |
| CON-FU-028 | Branch creation from a commit | 03 | versioning | – |
| CON-FU-029 | Copy entity from any URI | 03 | versioning | HL-FU-012 |
| CON-FU-030 | Per-user draft branches (stash) | 03 | versioning, workflow | HL-FU-007 |
| CON-FU-031 | Environments → branch head or pinned commit | 03 | deployment, versioning | HL-FU-006 |
| CON-FU-032 | Snapshot cache with metrics | 03 | cache | – |
| CON-FU-040 | Branch diff by comparing heads | 04 | versioning, query | HL-FU-009 |
| CON-FU-041 | Field-level blame in SQL | 04 | versioning, observability, query | HL-FU-009 |
| CON-FU-042 | Global search (index text + slug) | 04 | search, query | – |
| CON-FU-043 | Process audit queries | 04 | process, observability | – |
| CON-FU-044 | Metrics from the version store | 04 | observability | HL-FU-014 |
| CON-FU-050 | UBOS Studio IDE | 05 | frontend, versioning | HL-FU-010 |
| CON-FU-051 | Time-travel slider with blame | 05 | frontend, versioning | HL-FU-010 |
| CON-FU-052 | Visual diff and conflict resolution | 05 | frontend, versioning | HL-FU-005 |
| CON-FU-053 | Branch status view | 05 | frontend, versioning | HL-FU-009 |
| CON-FU-054 | Governance managers | 05 | frontend, permission, integration, observability | HL-FU-010 |
| CON-FU-055 | Git-like web terminal | 05 | cli, frontend | HL-FU-013 |
| CON-FU-056 | Client-side tenant context | 05 | multi-tenancy, frontend | – |
| CON-FU-060 | Integration boundary: keys in, webhooks out | 06 | integration, plugin | HL-FU-011 |
| CON-FU-061 | Local-LLM and embedding configuration | 06 | ai | – |
