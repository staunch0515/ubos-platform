---
id: ANA-FU-06
title: "FU Extensibility & AI"
status: complete
phase: P1
depends_on: [ANA-FU-03, ANA-UP-06]
sources: [FU]
---

# FU — Extensibility & AI

All UP extension points apply (ANA-UP-06). FU adds the following.

| Extension point | Mechanism | Redeploy |
|---|---|---|
| Data contract for a type | Commit `SCHEMA/{TYPE}` | No |
| Access control | Commit `USER` / `GROUP` / `POLICY` | No |
| Outbound integration | Commit `WEBHOOK` | No |
| Inbound integration | Generate `SECURITY_KEY` with scope `EXTERNAL_COMMIT` (paths `/api/external/**`) | No |
| Change control | Route changes through `APPROVAL_REQUEST` | No |
| Release pointers | Edit environment mapping / pin | No |
| Terminal commands | Add `case` in `WebConsoleController` | Yes |

### CON-FU-060 — Integration boundary: scoped keys in, signed webhooks out
- **What:** External systems write through API-key-authenticated endpoints and are notified through signed webhooks; both are configured as versioned entities.
- **Concepts:** CON-FU-003, CON-FU-023, CON-FU-024.
- **Why (intent):** Integration is governed by the same data-driven, auditable mechanism as everything else.
- **Sources:** [FU:backend/src/main/java/org/logrum/ubos/web/security/ApiKeySecurityFilter.java], [FU:backend/src/main/java/org/logrum/ubos/kernel/service/WebhookService.java].
- **Tags:** integration, plugin

### CON-FU-061 — Local-LLM and embedding configuration
- **What:** Configuration declares a local Ollama endpoint with an LLM (`qwen2:7b`) for "reasoning, code generation" and an embedding model (`nomic-embed-text`) "to generate pgvector vectors". No Java code consumes it in FU.
- **Why (intent):** Planned on-premise AI (no external API) and vector search over entities.
- **Sources:** [FU:backend/src/main/resources/application.yml] (`ollama.*`).
- **Tags:** ai

## AI summary

| Aspect | FU status |
|---|---|
| AI copilot | not present (UP's Python copilot not carried over) |
| Local LLM / embeddings | configured only |
| AI-relevant structure | URI addressing, schemas and approvals make AI-proposed changes *reviewable*: an AI can submit an `APPROVAL_REQUEST` against a URI and a human approves (Interpretation) |
