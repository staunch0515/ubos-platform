---
id: UBS-SYS-DVM-02
title: DVM — Design Requirements Index
status: draft
phase: PH-1
depends_on: [UBS-SYS-DVM-01]
---

# DVM — Design Requirements Index

| File | Range | Sub-area |
|---|---|---|
| `01-storage.md` | DSN-DVM-0xx | storage engine, content identity vs physical form, kernel indexes, sequencer, prolly tree, snapshots, caches |
| `02-model-versioning.md` | DSN-DVM-1xx…2xx | model registry, validation plans, lenses, commits, branches, change sets, merge, diff, revert, inheritance |
| `03a-time-ledger.md` | DSN-DVM-3xx…4xx | timelines, impact analysis, rule bindings, ledgers, balances, projections, reports, periods, matching |
| `03b-transactions.md` | DSN-DVM-5xx | processes, flush, concurrency, signing, idempotency, lineage, outbox, deferred commits, bulk, sagas |
| `04-runtime.md` | DSN-DVM-6xx | instruction dispatch, expression engine, Rhai and WASM hosts, profiles, metering, journal, replay, logic assets, connectors, `ask.model` |
| `05-rules-flow.md` | DSN-DVM-7xx | sheets, cascade, decisions, decision tables, lifecycles, approvals, tasks, delegation, workflows, simulation, rule tests |
| `06-authz-query-proof.md` | DSN-DVM-8xx…9xx | policy engine, relations, row filters, masking, encryption, query planner, views, proofs, export and import |

Every DSN item uses the requirement block. **Systems** always includes DVM. **Origin**
lists the realised FR, NR, STD and AR items.
