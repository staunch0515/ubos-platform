---
id: INV-LC
title: "Inventory: logicorum"
status: complete
phase: P0
depends_on: [META-TEMPLATES]
sources: [LC]
---

# Inventory: logicorum (`LC`)

## 1. Identity

| Field | Value |
|---|---|
| Key | `LC` |
| Repository | staunch0515/logicorum |
| Languages | Java 17 (backend), TypeScript/React (2 apps), JSON (process/schema/relationship definitions) |
| Build | Gradle single project; npm/Vite per app |
| Size (source lines) | Java 9,157 · TS/TSX 21,598 · JSON 2,429 · SQL 229 · MD 2,338 |
| Commits | 45 |
| Commit span | 2025-11-09 → 2025-11-26 (+ 2026-09-29 housekeeping) |
| Root Java package | `com.logicorum` |
| Workflow | Ticketed branches (`LCM-1000`…`LCM-1005`, `dev/lcm-#7`), PRs |

## 2. Top-level layout

```
logicorum/
  src/main/java/com/logicorum/   Backend: generic process engine + handlers
  data/
    database/init_db/            DDL (000..011)
    database/sql/, search_sqls/  Named SQL statements loaded at runtime
    processes/                   *.process.json  — business processes as step pipelines
    schemas/                     *.schema.json   — entity JSON Schemas (auth, finance, quiz)
    relationships/               *.rtp.json      — relationship type definitions
    email/                       email templates
    testcase/                    data-driven test cases (*.test.json)
  apps/quiz_admin_app/           React admin dashboard (QuizBucks)
  apps/quiz_bucks_app/           React end-user app (QuizBucks)
  config/                        application.yaml profiles (default, staunch, supabase)
  docs/                          design notes (Chinese)
  vercel.json                    frontend deployment
```

## 3. Modules / packages

| Name | Path | Role |
|---|---|---|
| `api` | `com/logicorum/api/ProcessController.java` | Single generic endpoint that runs a named process |
| `core` | `com/logicorum/core/` | `LogicExecutionEngine`, `ProcessDefinition(Loader)`, `Step`, `ProcessStepHandler`, `HandlerResolver`, `SchemaDefinitionLoader`, `RelationshipConfigLoader`, `SqlStatementLoader`, `QueryEngine` |
| `handler` | `com/logicorum/handler/` | ~34 reusable step handlers (validation, persistence, auth, JWT, email, cascade delete, optimistic lock, decision, reward…) |
| `model` | `com/logicorum/model/` | `ProcessContext`, `EntityChangeSet`, `DynamicModel`, `CommitType`, `ValidationLevel`, `RequestMetadata` |
| `service` | `com/logicorum/service/` | Entity commit, query commit, idempotency, JSON schema, audit, login session, email, token, environment |
| `validation` | `com/logicorum/validation/` | Business constraint manager; mandatory/relationship/unique-attribute validators |
| `security` | `com/logicorum/security/` | JWT filter, BCrypt token hashing |
| `scheduler` | `com/logicorum/scheduler/` | Email job, session cleanup |
| `quiz_*_app` | `apps/` | Domain frontends built with shadcn/ui, Redux Toolkit Query |

## 4. Tech stack

| Layer | Technology |
|---|---|
| Runtime | Java 17, Spring Boot 3.3 WebFlux + Security |
| Persistence | PostgreSQL via R2DBC (pool); Supabase profile; EAV table `lcm_entity_attribute_value` |
| Validation | JSON Schema (networknt 1.5.2 + java-json-tools) |
| Auth | JJWT 0.11.5, BCrypt |
| Mail | spring-boot-starter-mail + template queue table |
| Frontend | React, Vite, Tailwind, shadcn/radix, Redux Toolkit (RTK Query), recharts |
| Deploy | Vercel (frontend) |

## 5. Design documents

| Path | Lang | Topic |
|---|---|---|
| `docs/README.md` | EN | Overview |
| `docs/Main.md` | ZH | QuizBucks product requirements (app + admin) |
| `docs/relationship-types.md` | ZH | Relationship types as single source of truth |
| `docs/schema.md` | ZH | Schema/model merging technique |
| `docs/finance.md` | ZH | Double-entry bookkeeping beyond schema validation |
| `docs/Redis.md` | ZH | High-concurrency caching with R2DBC |
| `docs/RTK.md` | ZH | RTK Query usage |
| `docs/Google.md` | EN/ZH | Google registration flow |
| `docs/Supabase/readme.md` | EN | Supabase setup |
| `docs/email-template.md` | ZH | Placeholder format for email templates |
| `apps/*/src/API_INTEGRATION.md`, `QUICKSTART.md`, `CHANGELOG.md`, `README.md` | ZH/EN | Frontend integration guides |

## 6. Entry points

| Entry | Path |
|---|---|
| Server main | `src/main/java/com/logicorum/LogicorumApplication.java` |
| Process API | `api/ProcessController.java` → `core/LogicExecutionEngine.java` |
| DB init | `data/DatabaseInitService.java` + `data/database/init_db/*.sql` |
| Apps | `apps/quiz_admin_app/src/main.tsx`, `apps/quiz_bucks_app/src/main.tsx` |

## 7. Data assets

| Asset | Path | Notes |
|---|---|---|
| DDL | `data/database/init_db/000..011` | entity_instance, process_commit_log, entity_version_chain, process_entity_map, **entity_attribute_value (EAV)**, branch_head, **process_idempotency**, verification_token, login_session, request_log, email_dispatch_queue |
| Processes | `data/processes/**/*.process.json` (19) | auth (login/logout/register/verify), commit, management, sponsor (quiz draft/publish), taker (sign-up/in, start quiz, submit answer) |
| Schemas | `data/schemas/**/*.schema.json` (11) | USER, ROLE, RELATIONSHIP, PROCESS, LOGIN_RECORD, WALLET, TRANSACTION, QUIZ, QUESTION, OPTION, USER_QUIZ_ATTEMPT — all `_V1` versioned |
| Relationship types | `data/relationships/*.rtp.json` | typed relations with allowed source/target entity types, symmetric flag, metadata schema (e.g. `HAS_ROLE` with validFrom/validTo) |
| Named SQL | `data/database/sql/*.sql`, `search_sqls/default.sql` | |
| Test cases | `data/testcase/**/*.test.json` | declarative, data-driven tests |

## 8. First impression

- A **declarative process engine**: every API call is a named, versioned process (`AUTH_REGISTER_V1`) composed of reusable **step handlers** configured by `stepMetadata` in JSON.
- Entities are **JSON-Schema-defined** with version suffixes in their type names; storage is **EAV + version chain + branch head**.
- **Relationship types are first-class metadata** with source/target constraints and their own metadata schema (temporal validity).
- Production-grade cross-cutting concerns exist as generic steps: idempotency, optimistic locking, request audit, email queue.
- The only repo validated against a **real vertical application** (QuizBucks: quiz-for-cash, with wallets and transactions).

## 9. Analysis reading plan (P1)

1. `data/processes/**` (all), `data/schemas/**`, `data/relationships/**`.
2. `core/*` (engine, loaders, step contract).
3. `handler/*` — catalog every handler and its metadata keys.
4. `model/ProcessContext.java`, `EntityChangeSet.java`, `service/impl/R2dbcEntityCommitService.java`, `IdempotencyService`.
5. `validation/*`.
6. `data/database/init_db/*`.
7. `docs/*.md` (translate + digest).
8. `apps/*` structure (API integration docs, store slices) — UI concepts only.
9. `data/testcase/**` (declarative testing concept).
