---
id: UBS-SYS-DVM-01
title: DVM — Context and Contracts
status: draft
phase: PH-1
depends_on: [UBS-SYS-DVM-00, UBS-ARC-02]
---

# DVM — Context and Contracts

```
                 ┌───────────── hosts ─────────────┐
   NOD (Server/Cell/Box/Worker)   FRG (CLI, sim)   Browser host
                 │ CTR-001 host API    ▲ CTR-002 host services
                 ▼                     │
           ┌───────────────────── DVM ──────────────────────┐
           │ txn ▸ runtime ▸ rules ▸ authz ▸ query ▸ proof  │
           │ model ▸ version ▸ time ▸ ledger ▸ tree ▸ store │
           └──────┬──────────────────────────────┬──────────┘
                  │ CTR-005 (storage adapter)     │ CTR-006 (via host blob service)
             PostgreSQL / SQLite            object store
```

The same diagram as a table:

| Neighbour | Direction | Contract | What flows |
|---|---|---|---|
| NOD, FRG, browser host | host → DVM | CTR-001 | contexts, invocations, reads, queries, merges, replays, exports |
| NOD, FRG, browser host | DVM → host | CTR-002 | clock, entropy, blobs, secret-backed connector calls, model calls, outbox release, job enqueue, telemetry, signing |
| PostgreSQL or SQLite | DVM → store | CTR-005 | transactional flushes, snapshot reads |
| object store | DVM → host → store | CTR-006 | large chunks, cold tier |

## Design constraints inherited

| Constraint | Source |
|---|---|
| no I/O except through traits | AR-032 |
| authoritative writes only through the flush | AR-004, CST-003 |
| kernel indexes maintained in the flush | AR-016 |
| one policy engine | AR-024 |
| caches keyed by content or invalidated by commits | AR-028 |
| hashes over the stored form | AR-020 |
| tenant scoping in every table | AR-017 |
