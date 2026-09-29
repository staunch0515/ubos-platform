---
id: UBS-SYS-DVM-07
title: DVM — Operations
status: draft
phase: PH-1
depends_on: [UBS-SYS-DVM-04, UBS-SYS-DVM-06]
---

# DVM — Operations

The DVM is a library, so it is deployed inside its hosts (NOD, FRG, browser). This document
lists what operators and host developers must know about running it: failure modes,
recovery, rebuilds, capacity, tuning and the metrics it exports.

## 1. Failure modes

| # | Failure | Detection | Kernel behaviour | Operator action |
|---|---|---|---|---|
| F-01 | database unavailable | adapter error | reads and commits fail with `NOD.STORAGE_UNAVAILABLE`; host enters degraded mode (NR-AVAIL-006) | restore database; no DVM recovery step needed (DSN-DVM-508) |
| F-02 | database read-only (replica promotion in progress) | write error | commits fail; reads continue | wait for promotion |
| F-03 | object store unavailable | blob error | commits needing new large chunks fail before the transaction; reads of large content fail with `NOD.BLOB_UNAVAILABLE` | restore object store |
| F-04 | stored chunk corrupt | hash mismatch on read | read fails with integrity flag; alert | restore chunk from backup or replica; run `verify` |
| F-05 | kernel index diverges from commits | verifier mismatch | reads may be wrong for affected keys; alert | `rebuild index` (IF-DVM-038) |
| F-06 | projection diverges | verifier mismatch | balance reads may be wrong; alert | `rebuild projection` |
| F-07 | clock skew | `NOD.CLOCK_SKEW` on commit | commits refused on that node | fix NTP; commits resume |
| F-08 | key service unavailable | `IAM.KEY_UNAVAILABLE` | encrypted-field reads and signing fail; custodial cached keys continue until TTL | restore key service |
| F-09 | hot branch contention | sequencer queue length, lock wait | latency rises; conflicts rise | enable leader lease; review hot objects (DSN-DVM-514) |
| F-10 | runaway logic | limit breaches, quarantine events | processes aborted; asset quarantined | fix asset; release through change set |
| F-11 | outbox backlog | backlog age metric | commits continue; effects delayed | scale host releasers; inspect dead letters |
| F-12 | node crash mid-flush | — | transaction rolled back by the database | none |
| F-13 | model cache memory pressure | cache eviction rate | more rebuilds of registries; latency rises | raise cache budgets |
| F-14 | migration or bulk process interrupted | suspended state | resumes from checkpoint | `resume` |

## 2. Recovery procedures

| Procedure | Steps | Duration target |
|---|---|---|
| Point-in-time restore of a VAE | 1. restore the database to time T; 2. restore object store versions to T (WORM protects later content); 3. open VAE; 4. run `verify roots` on the last 1,000 commits; 5. rebuild derived tables whose verification fails | NR-DUR-003 |
| Rebuild all derived data | for each derived table in UBS-SYS-DVM-04 §8, rebuild into shadow and swap | 1 hour per 100 M versions on ENV-REF-SERVER |
| Recover from index corruption | `verify indexes` → `rebuild index <name>` for the affected branch | minutes |
| Replace a lost node | none (stateless nodes, AR-012) | — |
| Restore after erasure | key tombstones applied on key-service restore (DSN-DVM-832) | — |

## 3. Continuous verification

The host MUST schedule:
- `verify indexes` on a rolling window (all branches of a tenant every 7 days);
- `verify projections` daily for ledgers;
- `verify sequences` daily;
- `verify roots` on a 1% sample of new commits (recompute the tree path);
- replay of a 0.1% sample of journaled processes (NR-DET-001).

Findings raise alerts and are recorded as operational events (NR-DUR-005).

## 4. Capacity model

| Driver | Unit cost on ENV-REF-SERVER | Notes |
|---|---|---|
| simple commit | 1 storage transaction, ~12 row writes, ~2 KB | group commit amortises fsync |
| version storage | ~1.2 KB per version compressed | NR-COST-001 |
| temporal index | ~280 bytes per segment including indexes | |
| tree | ~log₆₄(n) nodes rewritten per change | |
| ledger append | 1 entry row, 1 sequence update, k delta rows | k = number of projections |
| journal | 0.5–5 KB per process | journal policy |

Sizing rule of thumb for Server: 1 vCPU per 150 commits per second of mixed workload, and
database IOPS ≥ 20 × commits per second.

## 5. Tuning guide

| Symptom | Check | Tuning |
|---|---|---|
| high commit latency, low CPU | lock wait p95 | lower `sequencer.group_window_ms`; check database fsync latency |
| high conflict rate | hot-object report | model with ledgers or sum-of-deltas fields |
| slow polymorphic queries | plan summary | declare field indexes; check subtree re-stamping progress |
| slow balance reads | deltas since checkpoint | lower `ledger.checkpoint_every` |
| memory growth | cache metrics | lower cache budgets |
| slow merge previews | diff size | split change sets |

## 6. Metrics exported

| Metric | Type | Labels |
|---|---|---|
| `dvm_commit_latency_seconds` | histogram | tenant class, profile, step |
| `dvm_commits_total` | counter | outcome |
| `dvm_conflicts_total` | counter | kind (head, read set, key) |
| `dvm_sequencer_queue_depth` | gauge | branch kind |
| `dvm_process_aborts_total` | counter | error code |
| `dvm_cache_hit_ratio` | gauge | cache |
| `dvm_instruction_units_total` | counter | family |
| `dvm_verify_findings_total` | counter | check |
| `dvm_outbox_backlog_age_seconds` | gauge | kind |
| `dvm_replay_divergence_total` | counter | — |

Labels never contain object IDs or field values (NR-SEC-009). Tenant IDs appear only in
per-tenant metrics exported to the tenant's own observability scope.

## 7. Upgrades

- Kernel minor upgrades MUST NOT change content IDs, roots or conformance results
  (NR-COMPAT-004).
- Physical schema migrations MUST be online and additive (AR-030); destructive migrations
  are split into expand and contract releases.
- The genesis artefact and ABI versions are recorded in every commit, so mixed-version
  nodes can run during rolling upgrades within the skew window (NR-COMPAT-002).
