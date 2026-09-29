---
id: UBS-SYS-FED-05
title: Federation — State Machines
status: draft
phase: PH-5
depends_on: [UBS-SYS-FED-02]
---

# Federation — State Machines

## Trust relationship

| From | Event | To |
|---|---|---|
| — | propose | proposed |
| proposed | all parties accept | active |
| proposed | reject or expiry | rejected |
| active | notice to end | ending (notice period) |
| ending | notice elapsed | ended (final copies retained) |

## Shared-object proposal

| From | Event | To |
|---|---|---|
| — | submit | open |
| open | signature | open (count increases) |
| open | required signatures reached | effective (replicated to all parties) |
| open | any party rejects | rejected |
| open | conflicting proposal became effective first | superseded |
