---
id: ANA-FU-00
title: "FU Overview"
status: complete
phase: P1
depends_on: [INV-FU, ANA-UP-00]
sources: [FU]
---

# FU (fund-ubos) — Overview

## Vision as stated by the repository

- README: **"Universal Business OS – A Git-like Logic Engine."**
- Commits rename the system "from Logrum to UBOS" and fix the product name "Universal Business OS (UBOS)".
- Terminal banner: **"Universal Business OS – Git for Data."**
- `UBOS_STUDIO.md`: "a professional IDE-style web console for managing the entity data of UBOS (Git-for-Data)."

## Problem it addresses

UP proved that business logic, UI and data can live in a Git-like store. FU addresses what
an organization needs *around* such a store before trusting it: **who may change what**
(roles, policies), **what shape data must have** (schemas), **who must approve** (approvals),
**how branches are reconciled** (merge, conflicts), **which version runs where**
(environments), **how outside systems connect** (API keys, webhooks), and **how people work
with it daily** (an IDE, drafts, time travel, blame, dashboards, a terminal).

## Design in one paragraph

FU keeps UP's kernel and engine unchanged and grows the kernel service into a full
**Git-for-data API** (tenant-aware commit, merge, three-way conflicts, revert, branch from
commit, rename, copy, branch diff, blame, drafts). Every governance concern is stored **as
entities in the same version chain** (`SCHEMA`, `USER`, `GROUP`, `POLICY`, `SECURITY_KEY`,
`WEBHOOK`, `APPROVAL_REQUEST`), addressed by a canonical **`ubos://scope/type/slug?branch&
commit&tag&key` URI**. Commits are validated against versioned JSON Schemas; authorization
combines roles with IAM-style URI policies; approvals are deferred commits; environments
point to branches or pinned commits. **UBOS Studio** (React/antd/Monaco/RTK Query) and a
**Git-like web terminal** make the system operable.

## File map

| File | Content | Key IDs |
|---|---|---|
| `01-architecture.md` | Relation to UP, layers, API families, filters, flows | CON-FU-001…003 |
| `02-core-model.md` | URI grammars, tenants, namespaces, governance entities, tombstones, rename | CON-FU-010…016 |
| `03-runtime.md` | Schema validation, approvals, authz, keys, webhooks, merge, conflicts, revert, branches, copy, drafts, environments, cache | CON-FU-020…032 |
| `04-data-persistence.md` | Schema delta, branch diff, blame, search, audit, metrics, caches | CON-FU-040…044 |
| `05-interface-ui.md` | Studio API, Studio IDE, time travel, diff/merge UI, managers, terminal | CON-FU-050…056 |
| `06-extensibility-ai.md` | Integration boundary, AI configuration | CON-FU-060…061 |
| `07-design-docs-digest.md` | Studio docs, RTK docs, commit history, Javadoc statements | – |
| `08-highlights.md` | 14 highlights | HL-FU-001…014 |
| `09-concept-index.md` | Flat index | – |

## Top highlights

1. HL-FU-001 Governance as versioned data.
2. HL-FU-002 Schema-on-commit with versioned schemas.
3. HL-FU-003 Approval as deferred, URI-targeted commit.
4. HL-FU-004 IAM-style policies over `ubos://` resources.
5. HL-FU-005 Merge with three-way conflict detection.
6. HL-FU-006 Environments as pointers.

## Notes for later phases (facts to reconcile in P2)

- Two default branch names coexist (`master` in kernel/SQL, `main` in `UbosUriUtil` and stash).
- Two draft naming conventions (`draft/{user}/{base}` backend, `stash/{user}/{entityId}` frontend).
- Two commit query keys (`commit=` backend, `cid=` frontend).
- Tenant-aware and tenant-less code paths coexist in the kernel.
- Revert = head move (reset semantics), not an inverse commit.
- Webhook dispatch and search-index population are designed but not wired.
