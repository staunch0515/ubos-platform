---
id: UBS-SYS-AGT-00
title: Agent Hub — Overview
status: draft
phase: PH-3
depends_on: [UBS-SYS-00, UBS-SYS-NOD-00, UBS-ARC-01]
---

# Agent Hub (AGT) — Overview

## 1. Purpose

The Agent Hub is the governed intelligence layer. It provides:
- a model gateway: one path to all model providers, with classification enforcement,
  redaction, routing, metering and recording;
- an agent runtime: agents are principals with scope and budget that work on their own
  branches and submit change sets, never merging themselves;
- tool exposure: ports and queries as typed tools, including an MCP server for external AI
  clients acting with delegated rights;
- BPU knowledge: knowledge objects and retrieval per class, with temporal awareness and
  citations;
- Copilot, the AI builder, evaluation and monitoring, and governed evolution proposals.

AI output is always untrusted input (AR-029): it enters the system only as proposals that
pass the same validation, decisions and approvals as human work.

## 2. Responsibilities and non-responsibilities

| Responsible for | Not responsible for |
|---|---|
| model routing, provider adapters, redaction, AI metering | deciding what may be written (DVM) |
| agent definitions, runs, budgets, kill switch | merging agent work (human approvers) |
| tool catalogue generation and MCP server | tool semantics (ports and queries in DVM) |
| retrieval pipelines and embeddings orchestration | vector index storage (NOD adapters) |
| copilot sessions and proposal drafting | UI (WSP) |
| evaluation suites, quality and cost monitoring | billing (CTL) |

## 3. Personas served

PER-AiAgent, PER-AgentSupervisor, PER-BusinessUser (Copilot), PER-BusinessArchitect (AI
builder), PER-ComplianceOfficer (AI governance), PER-PlatformOperator.

## 4. Phase map

| Phase | AGT scope |
|---|---|
| PH-0 … PH-2 | none in production; interfaces reserved (`ask.model` returns `LOGIC.NOT_AVAILABLE`) |
| PH-3 | full AGT: gateway, agent runtime with branches and budgets, kill switch, tools and MCP, knowledge and retrieval, Copilot, AI builder, evaluations, governed evolution |
| PH-4 | cost controls integrated with CTL budgets; self-hosted inference option for Cells |
| PH-5 | cross-organisation agent delegation through federation (read-only tools) |

## 5. Deployment

AGT runs as a separate service (`ubos-agent-hub`) per Server (optional) or per Cell (shared,
tenant-isolated). It accesses business data only through NOD with agent or delegated
principals (CTR-021), never through the database.

## 6. Files

| File | Content |
|---|---|
| `01-context.md` | context |
| `02-functions.md` | DSN-AGT-* |
| `03-interfaces.md` | IF-AGT-* |
| `04-data.md` | agent definitions, runs, recordings |
| `05-state-machines.md` | agent run, proposal |
| `06-configuration.md` | routing, budgets, policies |
| `07-operations.md` | operations and incident handling |
| `08-verification.md` | AGT verification plan |
