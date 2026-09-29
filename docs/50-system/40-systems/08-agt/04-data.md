---
id: UBS-SYS-AGT-04
title: Agent Hub — Data
status: draft
phase: PH-3
depends_on: [UBS-SYS-AGT-02]
---

# Agent Hub — Data

Agent definitions, knowledge objects and evaluation suites are business data in the tenant's
VAE (governed, versioned). AGT keeps operational data in its own tenant-partitioned tables:

| Table | Purpose | Retention |
|---|---|---|
| `model_call` | recorded calls (redacted request, response, metering) | tenant policy, default 1 year |
| `agent_run` | runs with trigger, branch, state, budget use | tenant policy |
| `agent_step` | plan–act–observe steps and tool calls | as runs |
| `provider` | provider registry with terms, regions, allowed classifications | permanent |
| `eval_result` | evaluation results per agent version and model | permanent |

### DAT-AgentRun
- **Purpose:** One execution of an agent.
- **Kind:** System
- **Stored in:** `agent_run`, mirrored as a System-kind object for supervisors
- **Schema:**
```yaml
DAT-AgentRun:
  run_id:      { type: uuid, required: true, description: identity }
  agent:       { type: uri, required: true, description: agent definition version }
  principal:   { type: did, required: true, description: agent principal }
  trigger:     { type: object, required: true, description: event, schedule, task or user request }
  branch:      { type: string, required: true, description: agent branch }
  state:       { type: enum, required: true, description: running | waiting_input | submitted | completed | stopped | failed | killed }
  budget_used: { type: object, required: true, description: tokens, cost, tool calls, wall time }
  change_set:  { type: uri, required: false, description: submitted change set }
```
- **Invariants:**
  1. All commits of a run are on its branch.
- **Satisfies:** FR-AI-021, FR-AI-031, FR-AI-044
