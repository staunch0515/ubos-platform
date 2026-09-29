---
id: UBS-SYS-AGT-01
title: Agent Hub — Context
status: draft
phase: PH-3
depends_on: [UBS-SYS-AGT-00, UBS-ARC-02]
---

# Agent Hub — Context

```
 WSP Copilot ─CTR-024─┐        external MCP clients ─CTR-022─┐
                      ▼                                      ▼
            ┌────────────────────────── AGT ─────────────────────────┐
            │ gateway ▸ runtime ▸ tools/MCP ▸ knowledge ▸ evals      │
            └──┬──────────────────────────┬────────────────┬─────────┘
   CTR-020 ▲   │ CTR-021 (agent principal)│ CTR-023         │ config
   (NOD asks)  ▼                          ▼                 ▼
              NOD ──▶ DVM            model providers       CTL (budgets, allow-lists)
```

| Neighbour | Direction | Contract | What flows |
|---|---|---|---|
| NOD | NOD → AGT | CTR-020 | `ask.model`, embeddings, copilot model calls |
| NOD | AGT → NOD | CTR-021 | agent branches, tool calls, previews, change sets |
| external AI clients | client → AGT | CTR-022 | MCP tools and resources |
| providers | AGT → provider | CTR-023 | inference and embeddings |
| WSP | WSP → AGT (via NOD) | CTR-024 | copilot sessions |
| CTL | CTL → AGT | configuration | provider allow-lists, budgets |
| FRG | AGT → FRG (via NOD) | CTR-012 | checks for AI builder drafts |
