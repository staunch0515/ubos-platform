---
id: ANA-UB-07-01
title: "UB Digest 1: Vision, Layers, Entity Definition, Context, Protocol"
status: complete
phase: P1
depends_on: [ANA-UB-07]
sources: [UB]
---

# UB Digest — Part 1: Vision, Layers, Entity, Context, Protocol

Documents covered: `三块拼图.md` (Three Puzzle Pieces), `The essence of entities.md`,
`UBOS Three Layer System.md`, `design.md`, `UbosContext.md`, `Rehydration.md`,
`ProcessContext.md`, `UBOS协议的综合处理.md` (Comprehensive Handling of the UBOS Protocol).
All are Chinese conversation logs between the owner and an AI "professor"; content is
translated and condensed; code blocks are summarized, not reproduced.

---

## D-UB-01 `三块拼图.md` — The three puzzle pieces (86 lines)

The system consists of three cooperating layers of *language*:

| Piece | Name | Who writes | What | Stored as |
|---|---|---|---|---|
| 1 Foundation | **BEL 2.0** — "the Creator's language" | System architect | **Schema** (what exists: tenants, orders, invoices), **Constitution** (what must never happen: stock ≥ 0, amount ≥ 0), **Asset** (what is money, what is goods) | `SCHEMA` entities |
| 2 Instructions | **"FoxPro Script"** — "the manager's language" | Business experts / operations | `ACTION` (business action, e.g. publish bidding task), `GIVEN` (inputs), `GUARD` (risk checks, e.g. balance sufficient), `TRANSFER` (fund movement: collect, freeze) — like official "red-header" directives | `ACTION` entities |
| 3 Interface | **Executable Book** — "the employee's desk" | Documentation staff | Markdown pages with text (rules), data references `{{ VAR total_sales }}`, buttons bound to actions `{{ BUTTON ACTION "PostTask" }}` | `BOOK`/`PAGE` entities |

Closed loop example ("post a bidding task"): Constitution `Balance >= 0` defined in BEL →
script `ACTION "PostTask"` with `GUARD balance > 100` and `TRANSFER 100 TO Platform` → book
page "Task Hall" with a button bound to `PostTask` → user click runs the script; **the kernel
enforces the constitution and blocks any transfer that would make a balance negative**.
Implementation order advised: the 6 tables + entity state operation with Git rollback; then
an instruction interpreter adding `GIVEN`, `GUARD`, `TRANSFER`, `VAR` to the script context.
Metaphor: a three-layer hamburger (physics / business logic / UI).

## D-UB-02 `The essence of entities.md` — "An entity is a logical reference with context" (98 lines)

- Formalized: **Entity = Identity + Context + Reference**; materialized entity =
  **(UUID, Tenant, Branch, Timestamp)**.
- *Logical reference*: storage holds only commits and JSON blobs; an entity is a **handle / pointer (head)** to a commit, like a Git branch ref.
- *Context*: a request for `Order:123` must answer **which tenant** (space), **which branch** (version), **which time** (history).
- Per-layer forms of the same entity:

| Layer | Form |
|---|---|
| L4 Protocol | URI `ubos://tenant/branch/Order:123` — an address |
| L3 Engine (ESE) | rich object `StateContext<Order>` — loaded, context frozen |
| L2 BPU | branch head `Ref(Order:123) → Commit` — a cursor |
| L1 Kernel | aggregate id `"Order:123"` — an index key |

- Consequences: changing an entity = **moving a pointer** to a new commit; **"ghost entities"** are legitimate (exists on draft, absent on main); references drift, commits are eternal.

## D-UB-03 `UBOS Three Layer System.md` — OS-ification and the UBOS–ESE–BPU layering (670 lines)

### Part A — Making UBOS an operating system (six proposals)
1. **VFS**: "everything is an entity and every entity has a path": `/sys/kernel`, `/sys/drivers`, `/bin/<logic>`, `/home/<tenant>/data/…`, `/dev/smtp`; API `GET /api/vfs/read?path=…`.
2. **Process management**: each script execution becomes a process entity with PID, PCB (user, status RUNNING/SLEEPING/ZOMBIE, CPU time, memory), syscalls `ps`, `kill -9`, `top`; a task manager in the Studio.
3. **Drivers & mounts**: external systems are devices; a driver entity (`sys.driver.github`) + mount (`/mnt/github/my_repo`); `ctx.findOne("/mnt/…")` is intercepted and forwarded; storage location becomes configuration.
4. **Package manager** (`upm install crm-core`): an App = bundle of entities (meta entities, logic, UI pages) with dependencies and post-install scripts → app store.
5. **Pipes**: chainable actions `sys.act.export | sys.act.transform_csv | sys.act.email_to_boss`; output of A becomes input of B; standardized I/O in the script context.
6. **User vs kernel space**: Groovy user space limited to `ctx.*`, compile-time blacklist, quotas (e.g. 1,000 rows, 3 s), `KernelPanic` circuit breaker.
Target stack: Hardware (JVM, Postgres, WebFlux) → Kernel (VFS, process, drivers) → Syscalls (`UbosScriptContext`: open/read/exec/kill) → Userland (scripts, meta entities, UI entities) → Shell (Studio).

### Part B — Three layers: UBOS (intention) / ESE (rules) / BPU (facts)
| Layer | Role | Duties | Rule |
|---|---|---|---|
| **UBOS** | Commander | capture intent (UI), assemble context (user, tenant, entity), **dispatch events** | never modifies data; no `if status == PAID` code |
| **ESE** (Entity State Engine) | Judge & arbiter | hydrate state; run **state machine** (guards: state allowed? stock? permission?); compute side effects; emit atomic **commands** to BPU | all business complexity lives here; pure in-memory |
| **BPU** (Business Processing Unit) | Executioner & scribe | ACID execution of command packages; **immutable ledger** (append-only log); materialize current-state projections | never asks business questions |
Transfer example: event `Transfer(A,B,100)` → ESE checks `A.balance ≥ 100`, emits `[Debit(A,100), Credit(B,100)]` → BPU commits and returns commit id. Suggested: ESE as an **actor per entity id**; BPU as append-only log.

### Part C — Development paradigm "Define – Orchestrate – Commit"
- Gradle modules `bpu-core` → `ese-engine` → `ubos-kernel` → `ubos-apps`, dependencies one-way; BPU must not know "order".
- BPU: generic `EntityRepository`/`JournalRepository`, generic `PersistCommand(entityId, changeSet, version)`, CAS/optimistic retry, audit.
- ESE: developers write **state definitions** instead of services (DSL/fluent API: from state, on event, guard, actions, to state); pure-function tests (state + event ⇒ deterministic result).
- UBOS: metadata-driven UI; thin controllers translating HTTP to ESE events (`POST /api/order/123/ship` → `ese.dispatch("ORDER","123","SHIP",payload)`); ACL decides visible buttons; view projection.
- Roles: core Java devs build engine/driver/guard/action components; business experts configure state machines; frontend builds generic renderers.
- Red lines: no BPU writes bypassing ESE; ESE does no I/O (data injected via context); UBOS contains no business logic.

### Part D — The "legal contract" interfaces (ESE)
Interfaces `StateContext<S,E,P>` (current state, event, payload, read-only view), `EntityStateConfig` DSL (transitions with guards and actions), an order example (`CREATED --PAY[amount>0]--> PAID`, action: decrement inventory), and `DefaultEntityStateEngine` main loop: load → find transition → run guards → collect actions → one BPU call. Advantages: logic purity (no repositories reachable), atomicity by design, **UI derives allowed buttons from the registered transitions**.

## D-UB-04 `design.md` — Four-layer architecture and refinements (850 lines)

### Four layers (as implemented direction)
| Layer | Name | Classes | Duty |
|---|---|---|---|
| L1 | Core Atomic | `EntityVersioningService`, `CommitHandler` | isolate DB; atomic write of instance + version + log + search index; optimistic lock (CAS); input `EntityChange`, output `CommitResult` |
| L2 | Git Transaction | `EntityStateManager` | turn business intent (publish) into branch/merge ops; multi-entity lists for atomicity; carry commit context; input `DraftSaveRequest` |
| L3 | Evaluation Service | `EntityEvaluationService`, `EntityAssembler`, `EntityCacheManager` | read-through cache; assemble `_extends` inheritance and references; decide Live vs Draft; output **`UbosEntity`** |
| L4 | Protocol | `UbosProtocolResolver`, `UbosUri` | resolve `ubos://`; callers never care about UUID vs slug |
Refinements: (A) **schema validation** between L2 and L1; (B) **async event bus / transactional outbox** after commit (`EntityCommittedEvent` → cache invalidation, AI embedding, notifications); (C) **search service** over `bpu_entity_search_index` returning identities then assembled entities.

### Decisions recorded
- **`inheritanceChain`**: ordered list [self, parent, …, root] built during assembly; used for provenance of each property, loop detection, UI breadcrumbs and `instanceOf` checks. Example: `sys.config.base` → `biz.role.employee` (vacation 10, allowance 500) → `biz.role.manager` (allowance 2000, approval_limit) → `user.zhang_san` (vacation 15).
- **Cache**: needed because assembly makes reads compute-heavy (3–5 lookups + deep merge); use **Caffeine** (in-process) rather than Redis to keep "zero external dependency".
- **Cache granularity**: cache **raw per-entity JSON (`JsonNode`)**, not assembled `UbosEntity` — so updating a parent never requires invalidating descendants; assemble just-in-time ("JIT assembly is cheaper than cascade invalidation").
- **`SearchCriteria`**: list of typed filters (EQ, GT, LT, BETWEEN, LIKE, IN) over metadata and indexed properties, plus sort and paging (a list, not a map, to allow several conditions per field).
- **`EntityStateDTO` vs `UbosEntity`**: DTO = storage view carrying *both* live and draft snapshots (for editors); `UbosEntity` = single effective version after assembly (for execution/rendering). `DTO + intent (branch/commit) + assembler = UbosEntity`.
- Review notes: distinguish 400 vs 404; read-only transactions; never hard-code `SYSTEM` in commit context (audit loss); enforce tenant checks in diff/merge; audit every write intent.

## D-UB-05 `UbosContext.md` — The script context as SDK (354 lines)

- Goal: **"Context is the SDK"** — a facade that makes scripts read like a DSL.
- Pain points solved: automatic tenant; short constructors (`ctx.id(…)`); **hide Mono/Flux** (context blocks internally, scripts get plain lists/objects); built-in JSON, logging, HTTP.
- Class `UbosScriptContext` (package `…engine.script`) is "the only bridge between scripts and Java"; the executor builds it instead of binding loose variables.
- Benefits: lower skill barrier; context-aware (current user/tenant); **anti-corruption layer** — kernel API changes do not break hundreds of stored scripts.
- Outcome: Java kernel only schedules and executes ("stable like a CPU"); all features (export, calculation, approval) become scripts; next steps: import script via `ctx.commit`, extend context with HTTP/crypto as needed.

## D-UB-06 `Rehydration.md` — From dead JSON to live objects (340 lines)

- **Four-step resurrection**: Locate (reference + context → commit) → Project (snapshot view) → Bind (JSON → typed class) → **Weave** (inject **smart references** `EntityRef<T>` that carry the parent's evaluation context instead of eagerly loading related data).
- `LiveEntity<T>` carries its context; `EvaluationContext{branch, allowFallback, …}`.
- **Context propagation for references**: *strict inheritance* (related entity must exist on the same branch) vs **overlay/fallback** (recommended: look in `dev`, fall back to `main` — "Docker layering").
- Metaphor: JSON = flesh, class = skeleton, context = soul; the assembler is the wizard.

## D-UB-07 `ProcessContext.md` — The reified execution stack (1,049 lines)

Owner's requirements: no `new ProcessContext` inside handlers (single instance passed through);
handler input and output are both the context; context holds a result area (success and
failure) and is **persisted after processing**; record each handler's inputs, intermediate
messages, values and outputs, including nested handler calls; rich logging helpers. Model:
"the Java instance is an entity, the tenant is an entity, `UbosContext` = virtual environment
fixing at least tenant + branch (also an entity); execution = `UbosContext` + user + process".
- **Execution tree**: `enter(name, input)` pushes an `ExecutionNode`; `exit(output)` pops; `failNode(err)`; `putVar/getVar` scoped to the current node; `log()` attached to the current node; the root node serialized = full "holographic" record of the request.
- **Typed keys** `ContextKey<T>` replace string keys (compile-time type safety, self-documenting contracts).
- **Functional executor** (`CtxOps.run`) wraps enter/try/log/exit/fail boilerplate; `ActionExecutor` interface uses a default template method so implementations only write `doExecute`.
- Naming discussion: "Reified Call Stack", "user-space PCB", "virtual execution context", "black box"; mapping node = stack frame, current pointer = program counter, enter/exit = push/pop, attributes = local variables, `EntityOperation` enum = opcodes, failNode = exception unwinding.
- WebFlux compatibility: non-blocking I/O preserved; **mutable shared context forbids naive parallelism** → use `concatMap`, or `fork()` a child context for parallel branches.

## D-UB-08 `UBOS协议的综合处理.md` — Object lifecycle engine and URI design (1,066 lines)

### Three-pass object lifecycle
| Pass | Name | Handles | Output |
|---|---|---|---|
| 1 Composition ("compiler") | `SchemaComposer` | `_extends` chain via `ubos://` (local or remote), deep merge, recursion with cycle detection and depth cap | static composed object |
| 2 Resolution ("linker") | `ReferenceResolver` | type-aware: text fields → interpolate `{{ubos://…}}`; `EntityRef` fields → replace with target JSON; `ACTION`/`FUNCTION` fields → resolved to descriptors only | runtime object |
| 3 Execution ("runtime") | `ActionExecutor` | actions run only on user interaction (the **Executable Book** is a document containing action blocks) | effects |
Loaders: `RawEntityLoader` (version store → raw JSON; for backup/diff) and `EffectiveEntityLoader` (URI → raw → Pass 1 → Pass 2; for UI/logic).

### URI syntax sugar
Canonical `ubos://tenant/slug` stays in storage; human-facing short forms: **`@tenant/slug`** (recommended, NPM-scope style), `tenant:slug` (Docker style), bare `slug` (implicit current tenant / system library), plus **system aliases** (`"Relation"` → `@sys_boot/sys.type.relation`); a normalizer converts on read/write.

### Entity type = table name
- `entity_type` becomes a coarse **PascalCase resource class** ("folder"): `User`, `Order`, `Invoice`, `Book`, `Page`; slug unique within `(tenant, type)`.
- Mapping: **Tenant = database, Type = table, Slug = primary key, Instance = row, `_extends` schema = row definition** — "a table whose rows may each have different columns".
- Canonical URI finalized as **`ubos://{Tenant}/{Type}/{Slug}`** (e.g. `ubos://ubos_store/Page/boot_welcome`).
- Benefits: small-cardinality type column for partitioning/indexing, id collision isolation per type, REST-like routes (`/app/User/admin@sys.com`).
