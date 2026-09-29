---
id: UBS-SYS-AGT-05
title: Agent Hub — State Machines
status: draft
phase: PH-3
depends_on: [UBS-SYS-AGT-02]
---

# Agent Hub — State Machines

## Agent run

| From | Event | To |
|---|---|---|
| — | trigger | running (branch opened) |
| running | tool needs human input | waiting_input (task created) |
| waiting_input | answer received | running |
| running | preview and validation pass, submit | submitted (change set open) |
| running | budget or step limit | stopped (partial branch kept) |
| running | error | failed |
| any non-terminal | kill switch | killed |
| submitted | change set merged or rejected | completed (feedback stored) |

## Copilot proposal

| From | Event | To |
|---|---|---|
| — | generated | offered |
| offered | user accepts | drafted (draft or change set created by the user's action) |
| offered | user dismisses | dismissed (feedback stored) |
| offered | underlying object changes | stale |
