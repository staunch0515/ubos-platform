---
id: UBS-SYS-BRG-05
title: Bridge — State Machines
status: draft
phase: PH-2
depends_on: [UBS-SYS-BRG-02]
---

# Bridge — State Machines

## Import run

| From | Event | To |
|---|---|---|
| — | start | parsing |
| parsing | parsed | mapping |
| mapping | mapped (dry run) | previewed |
| previewed | apply | loading (bulk process batches) |
| loading | all batches committed | reconciling |
| reconciling | totals match | completed (signed report) |
| reconciling | differences | completed with differences (report lists them) |
| any | fatal error | failed (committed batches remain; report shows coverage) |

## Export watermark

| From | Event | To |
|---|---|---|
| at cseq N | new commits available | exporting range N+1…M |
| exporting | snapshot committed | at cseq M |
| exporting | failure | at cseq N (range retried) |
