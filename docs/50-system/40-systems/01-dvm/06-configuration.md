---
id: UBS-SYS-DVM-06
title: DVM — Configuration
status: draft
phase: PH-1
depends_on: [UBS-SYS-DVM-02]
---

# DVM — Configuration

The DVM is configured by the host through a typed `DvmConfig` structure. Keys are grouped by
sub-area. "Reload" says whether a change takes effect without reopening the VAE. Keys that
change business behaviour (for example approval thresholds) are NOT configuration: they are
business data in sheets and decisions (AR-027).

| Key | Default | Range | Reload | Design |
|---|---|---|---|---|
| `store.inline_chunk_max_bytes` | 8192 | 1024–65536 | no | DSN-DVM-003 |
| `store.partitions` | 64 | 16–512 | no (migration) | DSN-DVM-010 |
| `store.cold_rehydrate` | on-read | never, on-read | yes | DSN-DVM-012 |
| `store.sweep_min_age_hours` | 24 | 1–720 | yes | DSN-DVM-013 |
| `sequencer.group_max_batch` | 64 | 1–1024 | yes | DSN-DVM-006 |
| `sequencer.group_window_ms` | 2 | 0–50 | yes | DSN-DVM-006 |
| `sequencer.leader_lease` | off | off, on | yes | DSN-DVM-006 |
| `sequencer.clock_skew_limit_ms` | 500 | 10–5000 | yes | DSN-DVM-301 |
| `tree.node_cache_mb` | 256 | 16–16384 | yes | DSN-DVM-007 |
| `cache.versions_mb` | 512 | 0–65536 | yes | DSN-DVM-009 |
| `cache.definitions_mb` | 64 | 0–4096 | yes | DSN-DVM-009 |
| `cache.compiled_mb` | 128 | 0–4096 | yes | DSN-DVM-009 |
| `cache.enabled` | true | true, false | yes | DSN-DVM-009 (false for verification) |
| `model.max_depth` | 12 | 4–32 | no | DSN-DVM-103 |
| `model.max_traits` | 8 | 0–32 | no | DSN-DVM-103 |
| `branch.max_inheritance_depth` | 8 | 1–16 | no | DSN-DVM-260 |
| `merge.max_conflicts_reported` | 10000 | 100–1000000 | yes | DSN-DVM-243 |
| `time.retroactive_grace_ms` | 0 | 0–86400000 | yes | DSN-DVM-320 |
| `impact.max_depth` | 10 | 1–50 | yes | DSN-DVM-321 |
| `impact.max_nodes` | 100000 | 1000–10000000 | yes | DSN-DVM-321 |
| `ledger.checkpoint_every` | 1000 | 100–100000 | yes | DSN-DVM-420 |
| `txn.max_retries` | 5 | 0–10 | yes | DSN-DVM-513 |
| `txn.write_summary_retention_s` | 600 | 60–3600 | yes | DSN-DVM-512 |
| `txn.idempotency_retention_days` | 7 | 1–90 | yes | DSN-DVM-531 |
| `txn.pending_change_expiry_days` | 30 | 1–365 | yes | DSN-DVM-552 |
| `bulk.batch_size` | 1000 | 10–10000 | yes | DSN-DVM-560 |
| `process.max_staged_objects` | 10000 | 100–100000 | yes (per profile) | DSN-DVM-507 |
| `process.max_staged_bytes` | 64 MB | 1–512 MB | yes | DSN-DVM-507 |
| `process.max_ledger_entries` | 50000 | 100–500000 | yes | DSN-DVM-507 |
| `profile.<name>.wall_ms` | strict 500, standard 3000, batch 600000, simulation 60000, agent 10000 | 10–3600000 | yes | DSN-DVM-620 |
| `profile.<name>.memory_mb` | strict 64, standard 512, batch 2048, simulation 1024, agent 256 | 16–8192 | yes | DSN-DVM-620 |
| `profile.<name>.fuel` | derived from wall time | — | yes | DSN-DVM-620 |
| `runtime.pool_threads` | CPU cores | 1–256 | no | DSN-DVM-617 |
| `runtime.epoch_tick_ms` | 10 | 1–100 | no | DSN-DVM-623 |
| `runtime.quarantine_threshold` | 50% of ≥ 20 runs | 10–100% | yes | DSN-DVM-624 |
| `journal.default_policy` | on-derivation | always, on-derivation, sampled, never | yes | DSN-DVM-633 |
| `journal.sample_rate` | 0.01 | 0–1 | yes | DSN-DVM-633 |
| `authz.relation_max_depth` | 4 | 1–8 | yes | DSN-DVM-811 |
| `query.default_scan_budget` | 100000 rows | 1000–10000000 | yes | DSN-DVM-903 |
| `query.cursor_ttl_minutes` | 60 | 1–1440 | yes | DSN-DVM-908 |
| `proof.skip_link_base` | 2 | 2–16 | no | DSN-DVM-933 |
| `export.max_memory_mb` | 512 | 64–8192 | yes | DSN-DVM-950 |

## Validation

The host MUST validate `DvmConfig` at start and on reload against the ranges above
(NR-OPER-003). Invalid values MUST prevent start or reject the reload, with the key and
the allowed range in the error.

## Edition presets

| Preset | Differences from defaults |
|---|---|
| Server | defaults |
| Cell | `tree.node_cache_mb` 2048, `cache.versions_mb` 8192, `sequencer.leader_lease` on |
| Box | `tree.node_cache_mb` 64, `cache.versions_mb` 128, `runtime.pool_threads` 2 |
| Mobile and browser | `tree.node_cache_mb` 16, `cache.versions_mb` 32, `profile.standard.memory_mb` 128, `journal.default_policy` sampled |
