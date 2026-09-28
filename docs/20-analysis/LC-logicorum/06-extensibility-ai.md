---
id: ANA-LC-06
title: "LC Extensibility & AI"
status: complete
phase: P1
depends_on: [ANA-LC-03]
sources: [LC]
---

# LC — Extensibility & AI

| Extension point | Mechanism | Redeploy |
|---|---|---|
| New business operation | Add `*.process.json` composing existing handlers | No (hot reload in dev) |
| New entity type | Add `*.schema.json` (title = type) | No |
| New relationship type | Add entry to `*.rtp.json` | No (loaded at start) |
| New constraint kind | Implement `BusinessConstraintValidator` bean | Yes |
| New capability | Implement `ProcessStepHandler` bean (versioned name) | Yes |
| New query | Add named `.sql` + query process | No (hot reload) |
| Email content | Add template under `data/email/<name>/` (`{{placeholder}}` syntax) | No |

### CON-LC-060 — Two-speed extension model
- **What:** Configuration (processes, schemas, relationship types, SQL, templates) changes without code; new primitive capabilities are added as code (handlers, validators) with versioned names.
- **Why (intent):** Keep the handler library small and stable while business composition moves fast.
- **Sources:** [LC:data/], [LC:src/main/java/com/logicorum/handler/].
- **Tags:** plugin, process

### CON-LC-061 — Declarative, data-driven test cases
- **What:** Test scenarios are JSON files listing process invocations with bodies; ordered folders define suites.
- **How:** `data/testcase/0_init-app/`, `1_normal/`, `t_002_two_entity`, `t_003_two_process`, `t_004_delete`; each entry `{processName, version, description, body}`; example creates `USER_V1`, `QUIZ_V1`, `RELATIONSHIP_V1 (OWNS)`, `QUESTION_V1` in one `commit` process with `runOnLevel: DRAFT_SAVE`. Loaded by `TestEntryService`.
- **Why (intent):** Tests are written in the same language as the API (process calls), by non-programmers or AI.
- **Sources:** [LC:data/testcase/], [LC:src/test/java/com/logicorum/service/TestEntryService.java].
- **Tags:** dsl, process

### CON-LC-062 — AI as a product feature (requirements)
- **What:** The admin requirements specify AI cover generation from title/description, AI question generation from selected reference materials or video subtitles (count, difficulty, type) with mandatory human review, and monitoring of AI token cost.
- **Sources:** [LC:docs/Main.md].
- **Tags:** ai, domain-pack

### CON-LC-063 — Coding rules for AI-assisted development
- **What:** Commit `[dev/lcm-#7]` records rules: "never use System.out/System.err, always use the logger; generally no comments, if needed English only; no Chinese in code or comments."
- **Sources:** LC commit "[dev/lcm-#7] 1. 不能用System.out…" (2025-11-26).
- **Tags:** codegen

## AI summary

| Aspect | LC status |
|---|---|
| AI in platform runtime | none |
| AI in product (domain) | specified: generation of covers/questions, token cost tracking |
| AI in development | frontends generated from requirement text; coding rules for AI |
