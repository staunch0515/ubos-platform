---
id: UBS-SYS-CTL-05
title: Control Plane — State Machines
status: draft
phase: PH-4
depends_on: [UBS-SYS-CTL-02]
---

# Control Plane — State Machines

## 1. Migration operation

| From | Event | To |
|---|---|---|
| — | start (approved) | exporting |
| exporting | export snapshot imported on target | catching-up |
| catching-up | lag under threshold | pausing |
| catching-up | error | aborted (source unaffected) |
| pausing | writes paused on source | final-sync |
| final-sync | roots equal | cutting-over |
| final-sync | roots differ or timeout | aborted (writes resumed on source) |
| cutting-over | routing updated and target serving | retiring |
| retiring | safety window elapsed | succeeded (source data purged) |

## 2. Upgrade wave

| From | Event | To |
|---|---|---|
| — | create | planned |
| planned | start | canary |
| canary | gates pass for the soak time | early |
| early | gates pass | general |
| general | all targets done | completed |
| canary, early, general | gate fails | halted (auto rollback of the failing stage) |
| halted | operator resume after fix | the halted stage |
| halted | operator abort | aborted |

## 3. Node (registry view)

| From | Event | To |
|---|---|---|
| — | enrolment | active |
| active | 3 missed heartbeats | missing |
| missing | heartbeat | active |
| active | drain for maintenance | draining |
| draining | drained | maintenance |
| maintenance | back | active |
| any | decommission | retired |

## 4. Tenant placement

| From | Event | To |
|---|---|---|
| — | placement decision | placed |
| placed | migration start | migrating |
| migrating | success | placed (new Cell) |
| migrating | abort | placed (old Cell) |
