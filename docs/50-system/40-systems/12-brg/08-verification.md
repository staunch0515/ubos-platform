---
id: UBS-SYS-BRG-08
title: Bridge — Verification Plan
status: draft
phase: PH-2
depends_on: [UBS-SYS-BRG-02]
---

# Bridge — Verification Plan

| Suite | Content | Method |
|---|---|---|
| SUITE-BRG-MIG | reference migration corpus (fund administrator legacy extracts): inference accuracy, idempotency, history load, reconciliation | SCN, CONF |
| SUITE-BRG-ADAPT | financial file adapters on sample corpora; crash-resume | CONF, FAULT |
| SUITE-BRG-EXPORT | exactly-once CDC export under faults; schema evolution; masking | FAULT, SEC |

| Phase | BRG evidence |
|---|---|
| PH-2 | a design partner's legacy data migrated with a signed reconciliation report showing zero unexplained differences |
| PH-3 | CDC export to Iceberg verified exactly-once; analytics engine with row filters passes the isolation suite |
