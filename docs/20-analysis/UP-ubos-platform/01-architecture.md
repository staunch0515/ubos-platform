---
id: ANA-UP-01
title: "UP Architecture"
status: complete
phase: P1
depends_on: [INV-UP]
sources: [UP]
---

# UP — Architecture

Path prefix used below: `J` = `ubos-server/src/main/java/org/logrum/ubos/`
(references are still written in full per WRITING-RULES R6.1).

## Layer map

| Layer | Package | Responsibility | Depends on |
|---|---|---|---|
| Kernel | `kernel` | Versioned entity storage (registry, commit chain, branch heads, branch hierarchy, relations, process log, audit queries) | Postgres (R2DBC) |
| Engine | `engine` | Execute logic entities (Groovy), sandbox, event dispatch, cron scheduling | Kernel |
| Server | `server` | HTTP API, auth filter, genesis bootstrap | Engine, Kernel |
| Common | `common` | `AuthContext`, context key constant | – |
| Clients | `ubos-shell`, `ubos-web`, `ubos_client`, `ubos-copilot` | UI shells and consoles over the HTTP API | Server API |

Dependency direction is strictly Server → Engine → Kernel, **except** that the Engine hands
Kernel services *into scripts* at runtime, so business logic (data) can call the Kernel
directly (see CON-UP-021).

### CON-UP-001 — Three-layer kernel/engine/server backend
- **What:** The backend is split into a storage kernel, an execution engine and an access server, each a Java package with one-way dependencies.
- **How:**
  - `kernel` knows nothing about scripts or HTTP.
  - `engine` wraps kernel services and exposes them to scripts via bindings.
  - `server` contains only controllers, filter and bootstrap; controllers call kernel or engine services.
  - The first commit set ("Time Travel") is described per layer: "Kernel: find by CommitID; Engine: commit-first strategy; Server: API parameter" — features are designed layer by layer.
- **Why (intent):** Keep the "Git for data" core independent from execution and transport. Commit messages describe features explicitly in these three layers.
- **Sources:** [UP:ubos-server/src/main/java/org/logrum/ubos/], commit `a264c33` ("Time Travel 1. Kernel … 2. Engine … 3. Server …").
- **Tags:** boot, api, scripting

### CON-UP-002 — Fully reactive stack with transient-error retry
- **What:** All I/O is non-blocking (Spring WebFlux + R2DBC + Reactor `Mono`/`Flux`), and every DB call is wrapped with a shared retry policy.
- **How:**
  - `ReactiveRetry.databaseTransientErrors()`: 3 retries, exponential backoff starting at 1 s, only for `TransientDataAccessResourceException`, `R2dbcException`, "Connection reset".
  - Exhausted retries raise "UBOS Database resilience exhausted after N attempts".
  - Auth identity flows through the Reactor `Context` (not thread locals).
- **Why (intent):** High concurrency with few threads; resilience against transient DB faults.
- **Sources:** [UP:ubos-server/src/main/java/org/logrum/ubos/kernel/util/ReactiveRetry.java], [UP:ubos-server/build.gradle] via root `build.gradle`.
- **Tags:** persistence, observability

### CON-UP-003 — Biological metaphor as architecture vocabulary
- **What:** Subsystems are named after a living organism.
- **How:**

| Metaphor | Subsystem | Source |
|---|---|---|
| Digital Lifeform Engine | whole platform | first commit message |
| Genesis | first-boot seeding | `server/bootstrap/Genesis.java` |
| Immune System | script sandbox + timeout kill | `engine/executor/LcmLogicExecutor.java` log text |
| Nervous System / Synapse | event bus / listener trigger | `engine/service/LcmEventService.java` |
| Bio-Clock | cron scheduler | `engine/service/LcmSchedulerService.java` |
| Family tree (家谱) | branch hierarchy | commit `38cb9a7` |

- **Why (intent):** Communicates that the platform is self-describing and self-evolving (it grows by committing new logic/data rather than by redeploying).
- **Sources:** as listed.
- **Tags:** boot, event, scripting

### CON-UP-004 — Deployment triad (DB + kernel + AI copilot)
- **What:** The reference deployment is three containers: PostgreSQL, the Java kernel, and a Python AI copilot that talks to the kernel over HTTP.
- **How:** `docker-compose.yml` services `db` (postgres:15), `ubos-server` (port 8080, R2DBC URL override), `ubos-copilot` (Streamlit port 8501, `UBOS_API_URL`, `OPENAI_API_KEY`); copilot `depends_on` server.
- **Why (intent):** AI is a first-class *client* of the kernel, deployed alongside it, not embedded in it.
- **Sources:** [UP:docker-compose.yml], [UP:Dockerfile], [UP:Dockerfile.frontend].
- **Tags:** deployment, ai

### CON-UP-005 — One API, several client shells
- **What:** Four independent clients consume the same HTTP API.
- **How:**

| Client | Tech | Purpose |
|---|---|---|
| `ubos-shell` | React/TS, zustand, react-draggable | End-user "web OS" desktop rendering server-defined apps |
| `ubos-web` | React/TS | Developer form to commit logic |
| `ubos_client` | React/JS | Earlier variant of `ubos-web` |
| `ubos-copilot` | Python Streamlit | AI architect, app browser, time-travel audit |

- **Why (intent):** The kernel is UI-agnostic; UIs are replaceable skins over the same versioned data.
- **Sources:** [UP:ubos-shell/src/], [UP:ubos-web/src/], [UP:ubos_client/src/], [UP:ubos-copilot/app.py].
- **Tags:** frontend, api

## Request flows

### Flow A — Run a logic entity
1. `POST /api/run/{slug}?branch=&commitId=` with JSON body = context → `RunController`.
2. `UbosRuntimeService.runLogic`: if `commitId` present → load that exact snapshot; else → `getResourceSnapshot("LOGIC", slug, branch)` with branch fallback (CON-UP-041).
3. `LcmLogicExecutor.execute(snapshot, ctx)`: read `content`, compile (cached), bind kernel services, run in sandbox pool with 3 s timeout.
4. Result object is returned as JSON.

### Flow B — Commit an entity version
1. `POST /api/dev/commit` `{type, slug, branch, code, message, processId}` → `DevController` wraps code into `{meta:{type:"groovy"}, content}`.
2. `LcmKernelService.commit`: find-or-create instance by `(type, slug)` → read current head on branch as parent → insert version row → (index) → upsert branch head → link to process if given.
3. Returns new `commitId`.

### Flow C — Boot a client
1. Client `GET /api/boot` → kernel reads VIEW `schema.desktop.default` on `master`.
2. Returns `{mode:"OWNER", desktop_schema}`; if missing → `{mode:"GUEST", desktop_schema:{…, error_hint}}`.
3. Shell switches state machine to `READY` and renders the desktop.

## Process / deployment shape

- Single JVM process, stateless apart from in-memory script cache and scheduled task map.
- Scheduler tasks are rebuilt from data at startup (`@PostConstruct refreshAllTasks`).
- Management actuator endpoints fully exposed, including remote shutdown (dev posture).
