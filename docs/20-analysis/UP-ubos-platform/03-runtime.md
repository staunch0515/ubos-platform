---
id: ANA-UP-03
title: "UP Runtime"
status: complete
phase: P1
depends_on: [ANA-UP-02]
sources: [UP]
---

# UP — Runtime Mechanisms

### CON-UP-020 — Logic-as-data execution
- **What:** Business logic is a `LOGIC` entity whose snapshot contains source code; it is resolved by slug + branch (or commit id) and executed on demand.
- **How:**
  - Snapshot shape: `{ "engine"|"meta":{"type":"groovy"}, "content": "<groovy source>" }`.
  - `UbosRuntimeService.runLogic(slug, branch, commitId, ctx)` selects the source (commit id wins over branch), then `LcmLogicExecutor.execute(snapshot, ctx)`.
  - Invoked by HTTP (`/api/run`), by UI buttons (action = logic slug), by events (listeners), by cron jobs.
- **Why (intent):** Change behavior at runtime by committing, with history, branches and rollback identical to data.
- **Sources:** [UP:ubos-server/src/main/java/org/logrum/ubos/engine/service/UbosRuntimeService.java], [UP:ubos-server/src/main/java/org/logrum/ubos/engine/executor/LcmLogicExecutor.java].
- **Tags:** scripting, versioning

### CON-UP-021 — Kernel capability injection into scripts
- **What:** Scripts receive a fixed set of named capabilities.
- **How:**

| Binding | Object | Capability |
|---|---|---|
| `ctx` | request context map (+ `currentUser`) | inputs |
| `log` | SLF4J logger | logging |
| `kernel` | `LcmKernelService` | read/search/commit/startProcess |
| `jsonUtils` | Jackson `ObjectMapper` | JSON |
| `dispatcher` | `LcmEventService` | publish events |
| `relations` | `LcmRelationService` | create/query relations |

  Commit message `6c4443f`: "inject LcmKernelService into Groovy scripts".
- **Why (intent):** Scripts are full participants: they can orchestrate multi-entity business operations (see CON-UP-028).
- **Sources:** [UP:ubos-server/src/main/java/org/logrum/ubos/engine/executor/LcmLogicExecutor.java#L79-L85].
- **Tags:** scripting, api

### CON-UP-022 — "Immune system" script sandbox
- **What:** Two-layer protection: static AST restrictions at compile time plus a runtime time limit with interruption.
- **How:**
  - Static (`SecureASTCustomizer`): disallowed receivers `Runtime`, `Class`, `ClassLoader`, `ProcessBuilder`, `Thread`, `ThreadGroup`; disallowed imports `java.io.File*`, `java.nio.*`, `java.net.Socket/ServerSocket`, `java.sql.*`, `java.util.concurrent.*`, `java.lang.reflect.*`; method definitions disallowed.
  - Runtime: `ThreadInterrupt` AST transformation injects interrupt checks into loops; execution in a dedicated pool; `future.get(3000 ms)`; on timeout `future.cancel(true)` and error "Logic killed by Immune System (Timeout)".
  - Commit `8f7657c`: "implement static blacklist and runtime timeout for immune system"; `10b4c9b`: "kill the loop thread".
- **Why (intent):** Logic authored at runtime (possibly by AI) must not endanger the host.
- **Sources:** [UP:ubos-server/src/main/java/org/logrum/ubos/engine/config/ScriptSecurityConfig.java], [UP:ubos-server/src/main/java/org/logrum/ubos/engine/executor/LcmLogicExecutor.java#L92-L113].
- **Tags:** scripting, permission

### CON-UP-023 — Compiled script cache by content hash
- **What:** Compiled script classes are cached by the hash of the source text.
- **How:** `scriptCache.computeIfAbsent(hash(content), parseClass)`; `invalidateCache()` clears all. New commits produce new content → new cache key automatically.
- **Why (intent):** Avoid recompilation on every call while staying correct across versions (content-addressed).
- **Sources:** [UP:ubos-server/src/main/java/org/logrum/ubos/engine/executor/LcmLogicExecutor.java].
- **Tags:** cache, scripting

### CON-UP-024 — Time-travel execution
- **What:** Any logic can be executed exactly as it was at a given commit.
- **How:** `?commitId=` on `/api/run/{slug}` → `getSnapshotByCommit` bypasses branch resolution.
- **Why (intent):** Reproduce past behavior, debug, audit, A/B compare logic versions.
- **Sources:** [UP:ubos-server/src/main/java/org/logrum/ubos/server/controller/RunController.java], commit `a264c33`.
- **Tags:** versioning, scripting

### CON-UP-025 — Event bus declared as data ("nervous system")
- **What:** Event handlers are `LISTENER` entities; publishing an event finds listeners by their `trigger` property and runs them asynchronously.
- **How:** `publish(eventName, payload)` → `kernel.search("LISTENER", "master", {trigger: eventName})` → for each snapshot `logicExecutor.execute(snapshot, payload)` on bounded-elastic scheduler; failures are logged and isolated. Scripts publish via `dispatcher`.
- **Why (intent):** Add reactions to business events without code deployment; listeners are versioned like everything else.
- **Sources:** [UP:ubos-server/src/main/java/org/logrum/ubos/engine/service/LcmEventService.java].
- **Tags:** event, scripting

### CON-UP-026 — Scheduler declared as data ("bio-clock")
- **What:** Cron jobs are `CRON` entities `{cron, target, params}`; the scheduler is rebuilt from the `master` heads.
- **How:** `refreshAllTasks()` cancels all futures, queries CRON heads on master, schedules `runLogic(target, "master", null, params)` with Spring `CronTrigger`; task key `target::cron`.
- **Why (intent):** Scheduling is configuration data with history, not code.
- **Sources:** [UP:ubos-server/src/main/java/org/logrum/ubos/engine/service/LcmSchedulerService.java].
- **Tags:** event, scripting

### CON-UP-027 — Genesis bootstrap with strategies
- **What:** First boot seeds the world *through the normal kernel commit API* ("Igniting with Pure Data"); a configurable strategy decides whether to seed.
- **How:**
  - `ubos.genesis.strategy`: `SAFE` (default; seed only if `type.meta` missing), `RESET` (truncate core tables, re-seed), `UPDATE` (documented: add only missing data; not implemented).
  - Genesis commits, as author `system.genesis` on `master`: meta type, money type, desktop VIEW, greeting LOGIC, admin USER.
  - `POST /api/admin/ops/reset-world` triggers RESET at runtime.
- **Why (intent):** The initial world is data with history like any other data; reset is a first-class operation for development.
- **Sources:** [UP:ubos-server/src/main/java/org/logrum/ubos/server/bootstrap/Genesis.java], [UP:ubos-server/src/main/java/org/logrum/ubos/server/bootstrap/SystemBootstrapper.java], [UP:ubos-server/src/main/resources/application.yml].
- **Tags:** boot

### CON-UP-028 — Multi-entity business operation orchestrated in a script
- **What:** A logic script opens a process and commits several entities under it, producing one auditable business operation.
- **How:** `commit_onboard.json` (`logic.hr.onboard`): `procId = kernel.startProcess("Employee Onboarding", "sys_admin")` → `kernel.commit("DATA_USER", "user."+empId, "master", json, "sys_admin", "Create Profile", procId)` → `kernel.commit("DATA_SALARY", "salary."+empId, …, procId)` → returns `{status, processId, msg:"Employee onboarded with full audit trail."}`.
- **Why (intent):** The business transaction boundary and audit trail are expressed in data, not in DB transactions.
- **Sources:** [UP:commit_onboard.json].
- **Tags:** process, scripting

### CON-UP-029 — Versioned kernel API for scripts
- **What:** The kernel surface exposed to scripts is versioned so old scripts keep working.
- **How:** Commit `17a4d0d`: "API version control: `kernel.v1.commit` and `kernel.v2.commit` coexist; method overloading: keep a 6-parameter `commit` (delegating to the 7-parameter one with `processId=null`) for compatibility with old scripts." The overload exists in code; the `v1/v2` namespaces are described only in the message.
- **Why (intent):** Logic stored as data lives forever; the host API must be backward compatible.
- **Sources:** [UP:ubos-server/src/main/java/org/logrum/ubos/kernel/service/LcmKernelService.java#L133-L136], commit `17a4d0d`.
- **Tags:** api, scripting, versioning

### CON-UP-030 — Request identity through reactive context
- **What:** The caller's identity travels in the reactive context and is injected into script context.
- **How:** `AuthWebFilter` → `contextWrite(AuthContext)`; `LcmLogicExecutor` reads it via `Mono.deferContextual` and puts it in `ctx.currentUser`; default `SYSTEM` identity for background jobs.
- **Why (intent):** Scripts can make identity-aware decisions without extra parameters.
- **Sources:** [UP:ubos-server/src/main/java/org/logrum/ubos/engine/executor/LcmLogicExecutor.java#L50-L60].
- **Tags:** context, auth
