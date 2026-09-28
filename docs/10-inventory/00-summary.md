---
id: INV-SUMMARY
title: "Inventory Summary: Eight Repositories"
status: complete
phase: P0
depends_on: [INV-UP, INV-LC, INV-UB, INV-LS, INV-US, INV-UC, INV-UW, INV-FU]
sources: [UP, LC, UB, LS, US, UC, UW, FU]
---

# Inventory Summary

## 1. At a glance

| Key | Repo | Backend | Frontend | Script engine | Storage | Main code lines | Design-doc lines | Commits | Active span |
|---|---|---|---|---|---|---|---|---|---|
| `LC` | logicorum | Java/Spring WebFlux | React (2 domain apps) | – (Java step handlers) | Postgres R2DBC, EAV + version chain | Java 9.2k · TS 21.6k | 2.3k | 45 | 2025-11-09 → 11-26 |
| `UP` | ubos-platform | Java/Spring WebFlux | React shell + Python copilot | Groovy | Postgres R2DBC, version chain | Java 1.7k · TS/JS 1.0k · Py 0.3k | ~0 | 28 | 2025-11-29 → 12-10 |
| `FU` | fund-ubos | Java/Spring WebFlux | React "UBOS Studio" IDE | Groovy | Postgres R2DBC, version chain | Java 9.9k · TS 13.1k | 0.5k | 28 | 2025-12-05 → 12-12 |
| `UB` | ubos | Java/Spring WebFlux | placeholder | Groovy | Postgres + pgvector, partitioned, multi-tenant | Java 8.7k · SQL 2.8k | **16.7k** | 3 | 2025-12-25 (snapshot) |
| `UW` | ubos_web | – | React 19 UX prototype | – | – | TS 1.4k | 0 | 2 | 2025-12-28 |
| `UC` | ubos_core | Rust/axum (1 crate) | React UI | Rhai | Postgres + pgvector, Redis, 6 tables | Rust 4.1k · TS 5.1k | **21.5k** | 62 | 2025-12-30 → 2026-01-06 |
| `US` | ubos-system | Rust/axum (workspace) + CLI | React "bicameral" client | Rhai | Postgres, 6 tables | Rust 5.3k · TS 7.4k | **24.1k** | 10 | 2026-01-10 → 01-18 |
| `LS` | logrum-system | Rust/Tokio TCP (binary protocol) | – | Rhai | SQLite + Postgres backends, 6 tables | Rust 3.8k | 0.2k | 11 | 2026-01-18 → 01-27 |

Total: ~160k lines, of which ~65k lines of design documents (mostly Chinese).

## 2. Families (observed, not ranked)

The user states the repos are **peers** (Q-002). The families below are factual groupings by
shared code/schema, used only to plan analysis and avoid duplicated effort.

| Family | Members | Evidence |
|---|---|---|
| Java "LCM" kernel | `LC`, `UP`, `FU` | Table prefix `lcm_*` (`lcm_entity_instance`, `lcm_entity_version_chain`, `lcm_entity_branch_head`, `lcm_process_commit_log`, `lcm_process_entity_map`); `UP`/`FU` share package `org.logrum.ubos` and 44 identical files |
| Java "BPU/ESE" kernel | `UB` | Table prefix `bpu_*`; packages `bpu`/`ese`; 16 identical files with `FU`, 15 with `UP` |
| Rust "BPU" kernel | `UC`, `US`, `LS` | Same 6 `bpu_*` tables; Rhai; `ubos://` URIs; `UC`/`US` share 82 identical files; `US`/`LS` share workspace layout (`ubos_proto`/`ubos_store`/`ubos_kernel`/`ubos_server`) and tagline |
| UX concept | `UW` | Standalone frontend prototype |

## 3. Shared-file matrix (byte-identical files, excluding lock/IDE files)

| Pair | Identical files |
|---|---|
| `US` ↔ `UC` | 82 |
| `FU` ↔ `UP` | 44 |
| `FU` ↔ `UB` | 16 |
| `UB` ↔ `UP` | 15 |
| all other pairs | ≤ 3 |

Consequence for P1: where files are identical, the analysis is written once (in the repo
analyzed first) and referenced from the other; differences get full treatment.

## 4. Cross-cutting ideas already visible (to be verified in P1)

| Idea | Seen in |
|---|---|
| **Git for Data**: append-only commit chain, branch heads, parent commits, time travel | all backends (`LC`, `UP`, `FU`, `UB`, `UC`, `US`, `LS`) |
| **Process = unit of business change** (process log ↔ commit map) | `LC`, `UP`, `FU`, `UB`, `UC`, `US`, `LS` |
| **Logic as versioned data** (scripts stored and executed at runtime) | `UP`, `FU`, `UB` (Groovy); `UC`, `US`, `LS` (Rhai) |
| **Declarative step pipelines** (process JSON → reusable handlers) | `LC` |
| **Self-describing meta-model** (root → meta type → types, explicit `_extends`) | `UB`, `UC`, `US` |
| **Pure relational composition** (entity = UUID + relations; Behavior → RelationType → EntityType) | `LS`, `US` (whitepaper v2.0) |
| **`ubos://` URI addressing** | `UB`, `FU`, `UC`, `US`, `LS` |
| **Fixed physical schema** ("only 6 tables") | `UC`, `US`, `LS` |
| **Branch inheritance / environments / workspaces** as tenancy mechanism | `UP`, `FU` (environments), `UC` (workspace/Logrum) |
| **Draft → publish** entity lifecycle | `UB` |
| **Merge, conflict resolution, stash, revert** | `FU`, `UB` |
| **Governance**: approvals, policies, API keys, webhooks | `FU`; policy-as-code in `UC`/`US` |
| **Metadata-driven UI** (schema renderer, ui_modes, widget registry, actions with param schemas) | `UP`, `UB` (docs), `US`, `UC` |
| **Kernel syscalls for scripts** (db, fs/VFS, ai, memory, task, ui) | `UC`, `US` |
| **AI**: LLM copilot, vector embeddings, LLM syscalls, AI i18n, AI-driven development | `UP`, `UB`, `UC`, `US` |
| **Novel UX shells**: OS desktop, IDE studio, bicameral (active + passive), intent command bar, unified dock | `UP`, `FU`, `US`/`UC`, `UW` |
| **Custom binary protocol / multi-client ("one kernel, five clients")** | `LS` (implemented), `US`/`UC` (stated) |
| **Idempotency, optimistic locking, request audit, declarative tests** | `LC` |
| **Enterprise storage**: tenant partitioning, yearly partitions, JSONB GIN, pgvector | `UB`, `UC`, `US` |

## 5. P1 analysis order

Rationale: build vocabulary progressively (simple kernel → governance → declarative
processes → rich meta-model → Rust kernels → radical relational model → UX), and analyze
code-sharing pairs adjacently.

| Order | Key | Reason |
|---|---|---|
| 1 | `UP` | Smallest kernel; defines the base Git-for-Data vocabulary |
| 2 | `FU` | Extends `UP` kernel with governance and Studio; diff-based analysis |
| 3 | `LC` | Different paradigm (declarative process pipelines), real vertical app |
| 4 | `UB` | Largest Java design corpus; meta-model, context, protocol, rendering |
| 5 | `UC` | Reference Rust kernel with build log |
| 6 | `US` | Extends `UC` corpus; most complete Rust kernel + whitepaper v2.0 |
| 7 | `LS` | Most radical relational/behavior model; binary protocol |
| 8 | `UW` | UX concept prototype |

Large-document budget: `UB` (16.7k), `UC`+`US` (~26k unique) will need multiple sessions;
digest files (`07-design-docs-digest.md`) will be split per WRITING-RULES R2.2.

## 6. Preliminary comparison topics (to be fixed at the start of P2)

1. Versioned storage model (commit chain, branches, heads, partitioning)
2. Meta-model & type system (inheritance, composition, relations)
3. Identity & addressing (`slug`, UUID, `ubos://` URI, tenant)
4. Business change unit (process, change set, transaction, idempotency)
5. Logic execution (Groovy / Rhai / Java handlers / behavior slots / syscalls)
6. Process orchestration & actions (step pipelines, transitions, 3-stage actions, jobs)
7. Context model (UbosContext, ProcessContext, Workspace, environment)
8. Branching semantics (inheritance, merge, conflict, stash, draft/publish)
9. Validation & constraints (JSON Schema, relationship constraints, policies)
10. Security & governance (auth, authz, approvals, API keys, policy-as-code, sandbox)
11. Events, scheduling, async jobs, webhooks
12. Query & search (search index, JSONB, vector)
13. Metadata-driven UI (renderers, widgets, ui_modes, views)
14. Client shells & UX paradigms (desktop, studio, bicameral, intent bar, dock)
15. API & protocol (REST, generic process endpoint, binary TCP, ambassador)
16. AI integration (copilot, embeddings, LLM syscalls, AI i18n, AI-driven dev)
17. Boot & genesis (seed data, meta-bootstrapping)
18. Multi-tenancy & environments
19. Deployment topology & clients ("one kernel, five clients")
20. Technology stack (Java vs Rust vs hybrid) → ADR-001
