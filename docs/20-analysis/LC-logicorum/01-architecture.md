---
id: ANA-LC-01
title: "LC Architecture"
status: complete
phase: P1
depends_on: [INV-LC]
sources: [LC]
---

# LC — Architecture

Source path prefix: `src/main/java/com/logicorum/` (written in full in references).

## Layer map

| Layer | Package / folder | Responsibility |
|---|---|---|
| API | `api.ProcessController` | One generic endpoint family; idempotency; request audit; response mapping |
| Engine | `core.LogicExecutionEngine` (commands), `core.ReadOnlyQueryEngine` (queries) | Load process definition, validate input, authorize, run step pipeline |
| Definition loading | `core.AbstractConfigLoader` + `ProcessDefinitionLoader`, `SchemaDefinitionLoader`, `RelationshipConfigLoader`, `SqlStatementLoader` | Load JSON/SQL definitions from `data/`, cache, hot-reload in dev |
| Step library | `handler.*` (~34 `ProcessStepHandler` beans) | Reusable business/technical steps configured by `stepMetadata` |
| Validation | `validation.*`, `service.JsonSchemaService` | JSON Schema + pluggable business constraints |
| Services | `service.*` | Commit, idempotency, audit, sessions, tokens, email, transaction log |
| Declarative assets | `data/processes`, `data/schemas`, `data/relationships`, `data/database/sql`, `data/email`, `data/testcase` | The business definition of the application |
| Clients | `apps/quiz_bucks_app`, `apps/quiz_admin_app` | Domain frontends (React + RTK Query) |

### CON-LC-001 — The API is a catalogue of named, versioned processes
- **What:** There are no per-resource controllers; every call names a process.
- **How:** `POST /api/{version}/{process}/{processName}` → command engine; `GET /api/{version}/{process}/{processName}` → read-only query engine. Process id = `UPPER(process + "_" + processName + "_" + version)`, e.g. `/api/v1/auth/register` → `AUTH_REGISTER_V1`. Body (POST) or query parameters (GET) become the initial process state.
- **Why (intent):** Adding a business capability = adding a process definition file; the HTTP surface is stable.
- **Sources:** [LC:src/main/java/com/logicorum/api/ProcessController.java].
- **Tags:** api, process

### CON-LC-002 — Command/query split with a shared step model
- **What:** Writes and reads run through two engines that share the same process/step format.
- **How:** `LogicExecutionEngine` (transactional; input schema defaults and validation; authorization; change-set initialization; persistence via a step) vs `ReadOnlyQueryEngine` (no change sets; logs a `QUERY_<name>` entry in the process log). Both support step jumps.
- **Why (intent):** CQRS without two programming models.
- **Sources:** [LC:src/main/java/com/logicorum/core/LogicExecutionEngine.java], [LC:src/main/java/com/logicorum/core/ReadOnlyQueryEngine.java].
- **Tags:** process, query, api

### CON-LC-003 — Request envelope: idempotency → execute → audit
- **What:** Every POST is wrapped in an idempotency guard and every request (GET/POST, success or failure) is written to a request log.
- **How:** `Idempotency-Key` header → `checkOrStartProcess` (see CON-LC-026) → on `NEW` run the engine → `completeProcess` / `failProcess` → `RequestAuditService` writes `lcm_request_log` (method, path, process id, IP, user agent, duration, HTTP status, process status, payload, response summary). Client IP and user agent are injected into the process state.
- **Why (intent):** Safe retries from mobile clients; full request-level audit independent from data-level audit.
- **Sources:** [LC:src/main/java/com/logicorum/api/ProcessController.java], [LC:src/main/java/com/logicorum/service/impl/R2dbcRequestAuditService.java].
- **Tags:** api, observability

### CON-LC-004 — Definitions as files with a generic, hot-reloading loader
- **What:** Processes, schemas, SQL statements and relationship types are files loaded by one abstract loader.
- **How:** `AbstractConfigLoader<T>`: glob pattern → parse → cache by upper-cased key; `getConfig(id, reload=true)` re-reads the single file when `app.run.mode=dev` (`EnvironmentService.isDevelopmentMode`). Patterns: `data/processes/**/*.process.json` (key `processName`), `data/schemas/**/*.schema.json` (key `title`), `data/database/sql/**/*.sql` (key = path with dots), `data/relationships/**/*.rtp.json` (key `typeName`).
- **Why (intent):** Edit business definitions and see the effect immediately; definitions are reviewable text in the repo.
- **Sources:** [LC:src/main/java/com/logicorum/core/AbstractConfigLoader.java], [LC:src/main/java/com/logicorum/core/ProcessDefinitionLoader.java], [LC:src/main/java/com/logicorum/core/SqlStatementLoader.java].
- **Tags:** metadata, boot

### CON-LC-005 — Steps are named beans from a handler library
- **What:** A step references a handler by bean name; handlers are Spring components implementing one interface.
- **How:** `interface ProcessStepHandler { Mono<ProcessContext> execute(ProcessContext ctx, Map<String,Object> stepMetadata); }`; resolved by `applicationContext.getBean(lowerFirst(handlerName))`. Handler names carry versions (`persistenceSaveHandlerV1`, `userRoleProvisioningHandlerV2`).
- **Why (intent):** A closed set of tested building blocks, recombined declaratively.
- **Sources:** [LC:src/main/java/com/logicorum/core/ProcessStepHandler.java], [LC:src/main/java/com/logicorum/core/HandlerResolver.java].
- **Tags:** process, plugin

## Request flow (command)

1. `POST /api/v1/auth/register` + optional `Idempotency-Key`.
2. Idempotency check; inject `clientIp`, `userAgent`.
3. Engine: new `ProcessContext(appId)`, `targetBranchName` (default `draft`), copy payload into `state`.
4. Load `AUTH_REGISTER_V1`; apply `inputSchema` defaults; validate input against `inputSchema`.
5. `authorizationHandler` checks `requiredRoles`.
6. Build change sets from payload `changes[]` (if present).
7. Run steps in order (or by jumps): e.g. provisioning → schema validation → persistence → email.
8. Status → HTTP (SUCCESS 200, INTERRUPTED 400, FAILED 500); idempotency completion; request log.
