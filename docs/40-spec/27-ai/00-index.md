---
id: SPEC-27
title: "AI"
status: complete
phase: P4
depends_on: [SPEC-13, SPEC-16, SPEC-17, SPEC-19, SPEC-21, SPEC-22, SPEC-24, SPEC-28, SPEC-30]
sources: [UP, FU, LC, UB, UC, US, LS, UW]
---

# AI

## 0. Chapter header

- **Scope:** This chapter covers every AI role in the platform, and each role uses the governed paths of the other chapters:
  - AI providers and the AI gateway (routing, privacy filter, accounting, recording);
  - AI syscalls;
  - the **builder** loop (requirement → package → validation → draft → approval);
  - **agents** as principals with contexts, tools and budgets;
  - the **copilot** (natural language → proposed intents);
  - question answering over data (RAG);
  - **explanations** from traces;
  - AI-native i18n;
  - AI-readiness artifacts for external coding agents.
- **MOD:** `AI`
- **depends_on:** SPEC-13, SPEC-16, SPEC-17, SPEC-19, SPEC-21, SPEC-22, SPEC-24, SPEC-28, SPEC-30.
- **Terms used:** TERM-AiProvider, TERM-AiBuilder, TERM-Agent, TERM-Copilot, TERM-Explanation, TERM-Embedding, TERM-Intent, TERM-Package, TERM-DryRun, TERM-Principal.
- **Origin summary:**
  - Verdicts: VRD-16-01…07, VRD-12-04, VRD-10-06, VRD-22-01.
  - UP: AI Architect and the copilot console (CON-UP-056, 061).
  - UB: the builder loop and data flywheel, AI i18n, embeddings, deployment strategy, house-rule prompts (CON-UB-063…067).
  - UC: the LLM syscall, the front-end copilot contract, the agent runtime, the AI kernel roadmap, prompt-driven development (CON-UC-058, 060, 062, 064, 065).
  - US: neuro-symbolic integration and explanations from traces (CON-US-060, 061), the three-piece delivery (CON-US-063).
  - LC: AI as a product feature with human review and token cost (CON-LC-053, 062).
  - LS: the prompt contract (CON-LS-063).
  - FU: local LLM and embedding configuration (CON-FU-061).
  - UW: an AI-built app (CON-UW-061).

## Parts of this chapter

This chapter is split into files (WRITING-RULES R2.2). Read them in order.

| File | Sections |
|---|---|
| `00-index.md` (this file) | §0, §1, §6, §7, §8 |
| `01-model.md` | Model — §2 |
| `02-behavior.md` | Behavior — §3 |
| `03-interfaces-nfr.md` | Interfaces and non-functional — §4, §5 |

## 1. Concepts

**"UBOS uses a brain, it does not host one"** (UB, UC). Models are external services configured as entities. The platform is the deterministic anchor. It validates, sandboxes, versions, audits and asks humans before AI output becomes binding (US "left brain / right brain").

| Role | What the AI does | Guardrails from other chapters |
|---|---|---|
| Runtime capability | logic calls `ai.*` syscalls | EXTERNAL effect, dry-run blocking, permissions, budget, recording (SPEC-17) |
| Builder | writes packages (types, actions, logic, views, scenarios) | meta-schema, Rhai static checks, dry run, draft branch, scenarios, human approval (SPEC-13, 17, 28, 22) |
| Agent | acts over time with its own identity and memory | principal policies, approvals, budgets, audit (SPEC-22, 16, 30) |
| Copilot | turns text into proposed intents | never executes; the user confirms; normal intent path (SPEC-19) |
| Answerer | answers questions over data, with citations | read permissions, masking, similarity search (SPEC-21) |
| Explainer | explains outcomes from traces | masked audit input (SPEC-30) |
| Translator | fills i18n on demand | versioned I18nKey entities, human review |

## 6. Acceptance

| REQ | Criterion |
|---|---|
| REQ-AI-001…003 | Routing, budgets, recording; extract with repair; privacy filter by locality and sensitivity |
| REQ-AI-010, 011 | Builder produces a reviewable draft package with scenarios; approval-only publish; flywheel export |
| REQ-AI-020…022 | Agent principal and audit; tool restriction; autonomy modes and budgets |
| REQ-AI-030, 031 | Copilot proposes only; answers cite versions and respect permissions |
| REQ-AI-040 | AI and fallback explanations |
| REQ-AI-050 | On-demand translation with staleness |
| REQ-AI-060 | External agent packages validate |

## 7. Implementation notes

- **Reference material:**
  - UP copilot [UP:ubos-copilot/app.py].
  - UC LLM syscall [UC:src/kernel/syscalls/ai.rs#L5-L22], copilot contract [UC:docs/front/front_03.md], agent positioning [UC:docs/plan/Plan_10.md].
  - UB builder and i18n designs [UB:docs/ai-dev.md], [UB:docs/I18n.md].
  - US neuro-symbolic design [US:docs/front/FullSlice.md].
  - FU Ollama config [FU:backend/src/main/resources/application.yml].
- **Divergences resolved:**
  - UC's copilot rendered components directly. Here it proposes intents, and the server renders.
  - UP's AI committed Groovy straight to the kernel. Here AI output goes through package validation, drafts and approval.
- **Clients:** implement provider kinds with `reqwest`. Use the OpenAI-compatible dialect for Ollama and vLLM. Tool definitions follow each provider's function-calling format, generated from operation JSON Schemas.

## 8. Open questions

None. Deferred: fine-tuning pipelines (the export exists, the training is external), and multi-agent coordination beyond Jobs and Workflows.
