---
id: ANA-UP-06
title: "UP Extensibility & AI"
status: complete
phase: P1
depends_on: [ANA-UP-03]
sources: [UP]
---

# UP — Extensibility & AI

## Extension points

| Extension point | How to extend | Redeploy needed |
|---|---|---|
| Business logic | Commit `LOGIC` entity (Groovy) | No |
| UI / apps | Commit `VIEW` entity (UI schema, desktop icons) | No |
| Event reactions | Commit `LISTENER` entity `{trigger, content}` | No |
| Scheduled jobs | Commit `CRON` entity `{cron, target, params}` + refresh | No |
| Access policy | Commit `CONFIG policy.user.<id>` | No |
| Types | Commit `TYPE_DEF` entity | No |
| Regional/tenant variants | Create branch in `sys_branch_config`, commit overrides only | No |
| UI components | Add to `ComponentRegistry` in shell | Yes (client) |
| Kernel capabilities for scripts | Add binding in `LcmLogicExecutor` | Yes (server) |

### CON-UP-060 — Extension by commit (no redeploy)
- **What:** All business-level extension happens by committing entities of known types.
- **How:** see table; the platform code contains no business logic except the generic engine.
- **Why (intent):** The platform is a runtime; business is content.
- **Sources:** Genesis, `LcmEventService`, `LcmSchedulerService`, `AuthWebFilter`.
- **Tags:** plugin, scripting, metadata

### CON-UP-061 — AI Architect: LLM authors logic commits
- **What:** An LLM turns natural-language requirements into Groovy logic that calls the kernel API, i.e. the AI writes platform content.
- **How:** Copilot system prompt: "You are the Chief Architect of UBOS. Your task is to convert natural language requirements into UBOS Groovy script logic. [Example Commit]: `kernel.commit("DATA_TYPE", "slug", "branch", jsonStr, "ai", "msg", null).block()`". Model `gpt-4o` via OpenAI SDK; tool-calling was planned ("AI is accessing Kernel Tools…"; `get_llm_tools` placeholder). Commit `c124e5b` "增加 LLM" (add LLM).
- **Why (intent):** Because logic is data behind a small stable API, an AI can safely extend the system (with the sandbox of CON-UP-022 as guard and branches for isolation).
- **Sources:** [UP:ubos-copilot/app.py].
- **Tags:** ai, copilot, codegen

### CON-UP-062 — The kernel API as the single contract for humans, scripts and AI
- **What:** The same small set of verbs (`commit`, `startProcess`, `getResourceSnapshot`, `search`, relations, events) is used by controllers, by stored scripts and by the AI prompt.
- **Why (intent):** One contract to document, version (CON-UP-029) and secure.
- **Sources:** [UP:ubos-server/src/main/java/org/logrum/ubos/kernel/service/LcmKernelService.java], [UP:ubos-copilot/app.py].
- **Tags:** api, ai, scripting

## AI integration summary

| Aspect | Status in UP |
|---|---|
| LLM as builder (codegen) | Designed; prompt present; tool loop placeholder |
| LLM as runtime service inside scripts | None |
| Embeddings / vector search | None |
| AI-readable audit | Process timeline via audit API |
