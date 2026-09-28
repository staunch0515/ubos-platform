---
id: ANA-LC-05
title: "LC Interfaces & UI"
status: complete
phase: P1
depends_on: [ANA-LC-01]
sources: [LC]
---

# LC — Interfaces & UI

### CON-LC-050 — URL grammar and response contract
- **What:** `/{api}/{version}/{domain}/{operation}`; responses are derived from the process context.
- **How:** If a step put `response_data` into state, it is returned verbatim with the mapped status. Otherwise `{status, messages[], data: <state minus response_data>, finalChanges: {"TYPE:id": fields}}`. Errors: `{status: VALIDATION_ERROR | INTERNAL_SERVER_ERROR | CONFLICT, message}`.
- **Why (intent):** Uniform envelope for every process; the process decides its public output.
- **Sources:** [LC:src/main/java/com/logicorum/api/ProcessController.java] (`mapContextToResponse`).
- **Tags:** api

### CON-LC-051 — End-user app (QuizBucks)
- **What:** Mobile-style React app for quiz takers.
- **How (from `docs/Main.md` requirements and app):** home feed of quiz "ads" with reward amount, publisher, completion counts (last hour/24h/week/month/all), advanced filters (country/region, amount, completions, publish time, publisher; sort by amount/completions); quiz detail with reference materials (text, links, embedded video), passing score, time limit; one question per page; result page with per-question correctness, score, pass, earned amount; bottom bar Home / History / Account / Messages / Settings; history with resume-within-time-limit; account ledger with filters; messages read/unread with keyword/date search; profile, change password, help center, privacy policy; registration with location detection, bank account and currency.
- **Sources:** [LC:docs/Main.md], [LC:apps/quiz_bucks_app/src/].
- **Tags:** frontend, domain-pack, finance

### CON-LC-052 — Admin app (content factory + publishing + finance)
- **What:** Web admin for sponsors/administrators.
- **How:** Dashboard (active campaigns, today's payouts, live takers, top publications, AI token usage); Quiz Content Factory (template list, editor with AI cover and AI question generation, internal version numbers); Publishing Management (create publication from template with region, period, reward; pause/resume); Analytics; Finance; Message Center; advanced search. Built with RTK Query (cache tiers long/medium/short/polling, tag invalidation, optimistic updates), mock API mode, i18n (`LanguageContext`, `translations.ts`), theme context.
- **Sources:** [LC:docs/Main.md], [LC:apps/quiz_admin_app/src/README.md], [LC:apps/quiz_admin_app/src/API_INTEGRATION.md].
- **Tags:** frontend, domain-pack, ai, i18n

### CON-LC-053 — Requirements written conversationally, apps generated
- **What:** `docs/Main.md` contains the owner's product requirements in natural language followed by AI-structured requirement documents and a tech-stack instruction ("use React + RTK Query + TypeScript + React Hook Form + Zod/Yup + Ant Design + Radix + Tailwind to generate this web system"); the apps carry "Figma Make" attributions.
- **Why (intent):** Frontends are produced from requirement text by AI tools; the backend stays generic.
- **Sources:** [LC:docs/Main.md], [LC:apps/quiz_admin_app/src/Attributions.md].
- **Tags:** ai, codegen, frontend
