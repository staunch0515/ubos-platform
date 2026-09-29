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
| `03-time-ledger-txn.md` | DSN-DVM-3xx…5xx | timelines, impact analysis, ledgers, balances, processes, flush, signing, CAS, idempotency, outbox, deferred commits, bulk, sagas |
| `04-runtime-rules.md` | DSN-DVM-6xx…7xx | instruction dispatch, Rhai and WASM hosts, metering, journal, replay, analysis, sheets, decisions, flows, approvals, simulation |
| `05-authz-query-proof.md` | DSN-DVM-8xx…9xx | policy engine, relations, row filters, masking, encryption, query planner, views, proofs, export and import |

Every DSN item uses the requirement block. **Systems** always includes DVM. **Origin**
lists the realised FR, NR, STD and AR items.
