---
id: SPEC-17
title: "Logic Runtime"
status: complete
phase: P4
depends_on: [SPEC-02, SPEC-13, SPEC-15, SPEC-16, ADR-001, ADR-002]
sources: [UP, LC, UB, UC, US, LS]
---

# Logic Runtime

## 0. Chapter header

- **Scope:** This chapter covers:
  - the `Logic` entity and the `Native` handler descriptor;
  - Rhai engine configuration and the execution modes (sync, job, dry run, eval);
  - the versioned **syscall ABI v1** (namespaces, signatures, profiles, checks, errors);
  - the layered sandbox and its limits;
  - the compile cache and module imports by URI;
  - the native handler registry and the v1 natives;
  - error handling, determinism and replay;
  - the syntax check.
- **MOD:** `RT`
- **depends_on:** SPEC-02, SPEC-13, SPEC-15, SPEC-16, ADR-001, ADR-002.
- **Terms used:** TERM-Logic, TERM-Syscall, TERM-SyscallAbi, TERM-Sandbox, TERM-NativeHandler, TERM-Emit, TERM-ExecutionMode, TERM-ExecutionContext, TERM-Session, TERM-DryRun.
- **Origin summary:**
  - Verdicts: VRD-05-01…06, VRD-10-06, VRD-20-02.
  - UP: logic as data, capability injection, the "immune system" sandbox, the compile cache, time-travel execution, the versioned kernel API (CON-UP-020…024, 029).
  - UC: the syscall ABI, the thread-local bridge, ephemeral eval, module import, runtime guards, the LLM syscall (CON-UC-004, 005, 025, 029, 030, 060).
  - US: execution modes, the syntax check, scripts as line arrays (CON-US-003, 005, 018).
  - LS: emit-only scripts and `native::` functions (CON-LS-021, 061).
  - UB: Action → Logic indirection and the script context as SDK (CON-UB-029, 030).
  - LC: native handler library and cross-cutting handlers (CON-LC-005, 025).

---

## 1. Concepts

### 1.1 Logic is data, capabilities are syscalls

```
      Action / Pipeline step / Slot / Hook / Cron / Job        (who calls; SPEC-13, 19, 20)
                         │  references by URI (never by body)       (VRD-05-04)
                         ▼
      Logic entity (rhai source, schemas, capabilities, limits)   or   Native handler (Rust)
                         │  runs inside
                         ▼
      Sandbox: Rhai engine, profile, limits, deadline
                         │  the ONLY way out
                         ▼
      Syscall ABI v1: entity.* query.* rel.* emit.* job.* ai.* memory.* ui.* log.* ...
                         │  policy check · provenance · session staging · trace node
                         ▼
      Kernel services (SPEC-12…22)
```

- Logic never touches the database, the network or the file system directly.
- A script reads through the session and stages writes in the session. Other effects are requested as **emits** or through audited syscalls.

### 1.2 Execution modes

| Mode | Runs in | Flush | Typical caller | Corpus |
|---|---|---|---|---|
| `SYNC` | the caller's process and session | the caller's flush | action stages, pipeline steps, slots | US sync executor |
| `JOB` | a new process, started by a worker | own flush | async emits, hooks, cron, long work | UC worker |
| `DRY_RUN` | a process with `dry_run = true` | none | previews, AI builder, tests | LS dry run |
| `EVAL` | a new process of kind EMIT, with an ad-hoc script | own flush (or dry run) | console/REPL, CLI `ubos eval` | UC eval, US CLI |

---

## 2. Model

```yaml
ENT-Logic:
  purpose: Executable code as a versioned entity (TERM-Logic).
  origin: [VRD-05-01, CON-UP-020, CON-UC-013, CON-US-018, CON-UB-029]
  fields:
    - name: language
      type: enum{RHAI}
      required: true
      description: Script language; only RHAI in version 1 (ADR-001; field open for WASM later).
    - name: abi
      type: string
      required: true
      description: Syscall ABI version targeted, e.g. "v1".
    - name: kind
      type: enum{FUNCTION|MODULE}
      required: true
      description: FUNCTION has an entry point; MODULE only exports functions for import.
    - name: source
      type: rhai
      required: true
      description: Source text; a list of lines is accepted and joined with "\n" (CON-US-018).
    - name: entry
      type: string?
      required: false
      description: FUNCTION - name of the entry function taking (params); absent = the whole script is the body and `params` is a scope constant.
    - name: input_schema
      type: json_schema?
      required: false
      description: Schema of params; defaults applied, violations fail RT.INPUT_INVALID.
    - name: output_schema
      type: json_schema?
      required: false
      description: Schema of the return value; violations fail RT.OUTPUT_INVALID.
    - name: effect
      type: enum{NONE|READ|STAGE|EXTERNAL}
      required: true
      description: Declared maximum effect - NONE (pure computation), READ (reads data), STAGE (stages writes/emits), EXTERNAL (http, ai, mail). Determines the highest profile it may run with (REQ-RT-005).
    - name: capabilities
      type: list<string>
      required: true
      description: Syscall namespaces used (e.g. ["entity", "query", "emit"]); others are not registered for this logic (least privilege).
    - name: imports
      type: list<uri<Logic>>?
      required: false
      description: MODULE logic this logic may import.
    - name: limits
      type: LogicLimits?
      required: false
      description: Overrides within host maxima - {max_operations, timeout_ms, max_call_depth, max_string, max_array, max_map}.
    - name: pure
      type: bool?
      required: false
      description: Declares effect NONE or READ and no dependence on memory/env; used for cross-tenant execution (REQ-CTX-011). Derived - effect in {NONE, READ}.
    - name: description
      type: text?
      required: false
      description: What the logic does (required for AI-authored logic, SPEC-27).
  invariants:
    - The source compiles under the engine configuration of REQ-RT-003 (checked at commit, REQ-RT-006).
    - effect = NONE implies capabilities ⊆ {log, time, json, decimal, uri, i18n, vars}.
    - A MODULE has no entry, no input/output schema.
```

```yaml
ENT-Native:
  purpose: Descriptor entity of a kernel-implemented handler (TERM-NativeHandler); created at boot, read-only.
  origin: [VRD-05-05, CON-LC-005, CON-LS-061]
  fields:
    - {name: title, type: string, required: true, description: "Name"}
    - {name: description, type: text, required: true, description: "What it does"}
    - {name: input_schema, type: json_schema, required: true, description: "Params"}
    - {name: output_schema, type: json_schema, required: true, description: "Result"}
    - {name: effect, type: "enum{NONE|READ|STAGE|EXTERNAL}", required: true, description: "Effect class"}
    - {name: slots, type: "list<slot_name>?", required: false, description: "Slots it can fill (SPEC-13 §2.6)"}
  invariants:
    - Slug has the form <name>.v<N> (REQ-URI-010); owned by logrums; only the kernel commits these entities.
```

```yaml
ENT-SyscallDescriptor:
  purpose: One entry of the syscall ABI (published by the query rt.abi, REQ-RT-011).
  origin: [VRD-05-02, CON-UC-004, CON-UP-029]
  fields:
    - {name: name, type: string, required: true, description: "namespace.function"}
    - {name: abi, type: string, required: true, description: "ABI version introducing it (v1)"}
    - {name: signature, type: string, required: true, description: "Rhai signature"}
    - {name: profiles, type: "list<enum{FULL|READ_ONLY|PURE|DRY_RUN|RULE}>", required: true, description: "Profiles where it is available"}
    - {name: effect, type: "enum{NONE|READ|STAGE|EXTERNAL}", required: true, description: "Effect class"}
    - {name: permission, type: string?, required: false, description: "Permission checked (SPEC-22), with target"}
    - {name: provenance, type: string?, required: false, description: "Relation recorded (REQ-TX-020)"}
    - {name: errors, type: "list<string>", required: true, description: "Error codes it may raise"}
```

---

## 3. Behavior

### 3.1 Engine and execution

### REQ-RT-001 — Invocation contract
- **Statement:** Invoking Logic *L* (by URI, anchored per REQ-TX-021 in deferred work) with params *p* MUST:
  1) resolve *L* and record DEPENDS_ON_FUNC;
  2) check that the caller may execute *L* (SPEC-22 `logic.execute`);
  3) apply `input_schema` defaults and validate *p* (`RT.INPUT_INVALID`, with paths);
  4) obtain the compiled AST (REQ-RT-007);
  5) create a Rhai call with the registered syscalls for *L*'s capabilities, filtered by the current profile, plus the read-only `ctx` object (API-CTX-002) and `params`;
  6) run it under the limits (REQ-RT-004) in a trace node of kind LOGIC;
  7) validate the return value against `output_schema` (`RT.OUTPUT_INVALID`);
  8) return it, converted to JSON (Rhai `Dynamic` → `serde_json::Value`; `()` becomes `null`).
- **Origin:** VRD-05-01, VRD-05-04, CON-UB-029, CON-US-003, CON-UC-006
- **Acceptance:** Logic with `input_schema {qty: integer ≥ 1}` called with `{qty: 0}` fails before running, with path `$.qty`.
- **Priority:** P0

### REQ-RT-002 — Execution modes
- **Statement:** The runtime MUST support the four modes of §1.2:
  - **SYNC** uses the caller's session and execution context. It adds a nested trace node, with no new process.
  - **JOB** is a Job entity (SPEC-20) whose attempt runs REQ-RT-001 in a new process of kind JOB, with anchors.
  - **DRY_RUN** runs as SYNC or JOB with profile DRY_RUN and no flush (REQ-TX-025).
  - **EVAL** runs an ad-hoc source inside a process of kind EMIT, with operation `ubos://system/Native/eval@v1`. It requires `ubos.rt.eval` (SPEC-22), is always audited with the source digest and source (≤ 64 KiB) in the process log metrics, and defaults to dry run unless `commit: true` is requested.
- **Origin:** CON-US-003, CON-UC-025, CON-US-004, CON-LS-026
- **Acceptance:**
  1) An EVAL without `commit: true` stages writes but does not flush.
  2) An EVAL with it flushes, and the source appears in the audit record.
- **Priority:** P0 (SYNC, JOB, DRY_RUN), P1 (EVAL)

### REQ-RT-003 — Engine configuration
- **Statement:** The Rhai engine (≥ 1.20, features `sync`, `serde`, `decimal`) MUST be configured as follows:
  - **Disabled:** `eval`, the `Engine::on_print`/`on_debug` default output (both are redirected to `log.*`), custom syntax, custom operators, and file-based module resolution.
  - **Module resolver:** a kernel `UriModuleResolver` (REQ-RT-008) only.
  - **Standard packages:** Core, Logic, Arithmetic, BasicString, BasicArray, BasicMap, BasicMath, plus the `decimal` package, and the registered syscalls.
  - **Strict variables:** on, so undeclared variables are compile errors.
  - **Rule profile:** the RULE profile (ADR-002) additionally disables loops, `fn` definitions, `import` and assignment to non-local variables. It is enforced by an AST walk at definition commit.
  - **Reuse:** one engine instance per profile and capability set is built lazily and shared (`Arc`), and per-call state goes in the call's scope/tag (REQ-CTX-005).
- **Origin:** VRD-20-02, VRD-05-03, ADR-002, CON-UC-004, CON-UC-030
- **Acceptance:**
  1) `eval("1+1")` is a compile error.
  2) An undeclared variable is a compile error, and the syntax check reports its line and column.
- **Priority:** P0

### REQ-RT-004 — Limits
- **Statement:** Every logic call MUST run under the limits below. The effective limit is the minimum of the Logic's `limits`, the tenant's configuration and the host maxima. Exceeding a limit fails with `RT.LIMIT` and `details {limit, value}`.

| Limit | Default | Host max | Mechanism |
|---|---|---|---|
| operations per call | 1 000 000 | 50 000 000 | `set_max_operations` + progress callback |
| wall time (SYNC) | 5 000 ms | 30 000 ms | deadline checked in the progress callback and before syscalls (REQ-CTX-013) |
| wall time (JOB) | job timeout (SPEC-20) | 1 h | same |
| call depth | 64 | 128 | `set_max_call_levels` |
| expression depth | 64 / 32 (functions) | 128 / 64 | `set_max_expr_depths` |
| string size | 1 MiB | 16 MiB | `set_max_string_size` |
| array / map size | 100 000 | 1 000 000 | `set_max_array_size` / `set_max_map_size` |
| modules imported | 32 | 64 | `set_max_modules` |
| syscalls per call | 10 000 | 100 000 | counter in the syscall wrapper |
| logic nesting (logic calling logic) | 16 | 32 | counter in the execution context |
| log lines per process | 1 000 | 10 000 | the excess is dropped and counted |
- **Rationale:**
  - UC's 5 000 operations were too low for business logic.
  - UP had a 3 s timeout and UB 5 s.
  - Every limit is enforced by the engine or the wrapper, never by convention.
- **Origin:** VRD-05-03, CON-UP-022, CON-UC-030, CON-UB-030
- **Acceptance:** One test per limit. An infinite loop is stopped at the deadline or the operation limit, whichever comes first, and the process fails with no flush.
- **Priority:** P0

### REQ-RT-005 — Profiles
- **Statement:** The runtime MUST select a profile per call. The profile limits the available syscalls by effect class:

| Profile | Allowed effect classes | Used for |
|---|---|---|
| FULL | NONE, READ, STAGE, EXTERNAL | action execute stage, pipeline steps, jobs, non-pure slots |
| READ_ONLY | NONE, READ | queries, `prepare` stages, read-only contexts (REQ-CTX-004) |
| PURE | NONE, READ (reads recorded) | pure slots (SPEC-13 §2.6) |
| DRY_RUN | as FULL, but EXTERNAL fails with `RT.DRY_RUN_BLOCKED` unless the Context allows it (`ai.allow_in_dry_run`); STAGE writes stay staged | dry runs |
| RULE | NONE plus `lookup`/`changed` (ADR-002) | invariants, guards, policy conditions, hook filters |

  A Logic MUST NOT run with a profile that allows less than its declared `effect` when the logic actually uses those syscalls. Calling an unavailable syscall fails with `RT.CAPABILITY_DENIED`. It is not "function not found", so errors stay informative.
- **Origin:** VRD-05-02, VRD-05-03, REQ-META-016, ADR-002
- **Acceptance:** A `mask` slot implementation calling `entity.update` fails with `RT.CAPABILITY_DENIED`.
- **Priority:** P0

### REQ-RT-006 — Static checks at commit
- **Statement:** Committing a Logic entity MUST perform these checks (a META definition rule, REQ-META-003):
  1) compile the source with the engine for its capabilities;
  2) walk the AST and reject calls to syscall namespaces not listed in `capabilities`, and imports not listed in `imports`;
  3) check that the declared `effect` covers the syscalls used;
  4) check `entry` exists with one parameter, when set.

  Failures raise `RT.COMPILE_ERROR` (with line and column) or `RT.DECLARATION_MISMATCH`.
- **Rationale:** "Static checks at commit" is UP's AST layer, applied through the data model, so bad logic never becomes a version.
- **Origin:** VRD-05-03, CON-UP-022, CON-US-005
- **Acceptance:** Committing logic that calls `http.request` without the `http` capability fails, naming the call site.
- **Priority:** P0

### REQ-RT-007 — Compile cache
- **Statement:** Compiled ASTs MUST be cached by (logic commit ID, engine profile key). Entries are immutable and evicted LRU (default 10 000 entries). Identical sources in different commits MAY share the AST through a content-hash index (UP). Nothing is ever invalidated by time.
- **Origin:** VRD-05-03, CON-UP-023, CON-LS-044
- **Acceptance:** A second call of the same logic version performs no compilation (a metric counts compiles).
- **Priority:** P0

### REQ-RT-008 — Module imports by URI
- **Statement:** `import "<uri>" as m;` MUST work as follows:
  - It resolves the URI (canonical or short form) to a Logic of kind MODULE, in the process's read tenants.
  - The version is anchored at first use in the process (DEPENDS_ON_FUNC).
  - The module is compiled or loaded from the cache and evaluated once per process.
  - Imports not declared in `imports` fail at commit (REQ-RT-006) and at run time (`RT.IMPORT_DENIED`).
  - Cyclic imports fail with `RT.IMPORT_CYCLE`.
  - A module's functions run with the **importer's** capabilities and profile.
- **Origin:** VRD-05-01, CON-UC-029, CON-UC-066
- **Acceptance:** Logic importing `ubos://acme/Logic/lib.money@v1` uses its `round2` function, and the lineage shows the module commit.
- **Priority:** P1

### REQ-RT-009 — Errors in scripts
- **Statement:**
  - A syscall error MUST be raised into Rhai as a runtime error carrying a map `#{code, category, message, path, details, retryable}`. Scripts may catch it with `try { … } catch (e) { … }`.
  - An uncaught error, or `throw value`, ends the call with `RT.SCRIPT_ERROR`. The `details` contain `{message, line, column, logic_uri, commit_id, cause}`, and `cause` is the syscall error map when there is one.
  - Errors from limits (`RT.LIMIT`, `TX.TIMEOUT`) and from cancellation MUST NOT be catchable.
- **Origin:** CON-UC-030, CON-UB-004 (failNode), NEW: uncatchable limits
- **Acceptance:**
  1) A script can catch `URI.NOT_FOUND` and create the entity instead.
  2) A caught `RT.LIMIT` is impossible: the process still fails.
- **Priority:** P0

### REQ-RT-010 — Determinism and replay
- **Statement:** Logic execution MUST be deterministic given its anchored inputs:
  - time comes only from `ctx.now`;
  - randomness only from `crypto.random_bytes` / `crypto.uuid_v7`, with values recorded in the trace node;
  - external results (`http.*`, `ai.*`, `mail.*`) are recorded in the trace at DETAILED level;
  - map iteration order is the key order (Rhai BTreeMap).

  A **replay** (P2) of a process re-runs the same logic versions on the same read versions (from provenance) with the recorded external results, and compares the change sets and outputs.
- **Rationale:** UP's time-travel execution and UC's anchors, made exact.
- **Origin:** VRD-05-06, CON-UP-024, CON-UC-021
- **Acceptance:** Running logic "as of commit N" (`dry_run` with the anchored Logic commit) gives the same result on two runs.
- **Priority:** P0 (determinism rules), P2 (replay tool)

### 3.2 Syscall ABI v1

### REQ-RT-011 — ABI publication and versioning
- **Statement:**
  - The kernel MUST publish the ABI as a list of `SyscallDescriptor`s through the query `ubos://system/Query/rt.abi@v1`, and in `spec-fixtures/syscalls-v1.json`.
  - A Logic's `abi` selects the version. `v1` functions keep their signatures and semantics for the platform's lifetime.
  - Changes come as new functions or as `v2` namespaces registered side by side (UP `kernel.v1.commit` / `kernel.v2.commit`).
  - Deprecation is announced in the descriptor. Removal requires an ADR and a migration tool.
- **Origin:** VRD-05-02, CON-UP-029, CON-UC-004
- **Acceptance:** Logic declaring `abi: "v1"` keeps running unchanged after a `v2` function is added.
- **Priority:** P0

### REQ-RT-012 — Common syscall wrapper
- **Statement:** Every syscall MUST be registered through one wrapper. The wrapper:
  1) checks the profile and capability (REQ-RT-005);
  2) checks the deadline and cancellation, and counts the call;
  3) converts arguments from Rhai values (URIs accepted in short form, REQ-URI-017);
  4) calls the kernel service with the execution context, which checks policies (SPEC-22) and records provenance (REQ-TX-020);
  5) opens a SYSCALL trace node at DETAILED level;
  6) converts the result or error (REQ-RT-009).

  Syscalls are synchronous from the script's view. They bridge to async kernel services on the blocking pool (CON-UC-005).
- **Origin:** VRD-05-02, VRD-10-03, CON-UC-005
- **Acceptance:** A policy that denies `entity.read` on a type makes `entity.get` fail inside logic with `URI.NOT_FOUND` (REQ-CONV-052). The trace shows the denied syscall.
- **Priority:** P0

### REQ-RT-013 — Syscall catalogue v1
- **Statement:** ABI v1 MUST provide the following functions. Types: `uri` = string (short forms allowed), `map` = Rhai map, `dec` = Rhai Decimal. The effect classes (E) are N = NONE, R = READ, S = STAGE, X = EXTERNAL.

**entity** (SPEC-13, SPEC-15)

| Function | E | Semantics |
|---|---|---|
| `entity.get(uri) -> map` | R | effective data with `_meta` (REQ-META-020); `URI.NOT_FOUND` if absent or invisible |
| `entity.get(uri, opts) -> map` | R | `opts: #{expand, own, view}` |
| `entity.try_get(uri) -> map or ()` | R | like `get`, returns `()` when not found |
| `entity.exists(uri) -> bool` | R | resolution without data |
| `entity.create(uri, data) -> string` | S | stage CREATE; returns canonical URI |
| `entity.update(uri, data)` | S | stage full own-data replacement |
| `entity.patch(uri, merge_patch)` | S | RFC 7396 merge patch on own data |
| `entity.set(uri, path, value)` | S | set one property path in own data |
| `entity.upsert(uri, data) -> string` | S | update or create |
| `entity.delete(uri)` / `entity.unset(uri)` | S | stage DELETE / UNSET |
| `entity.rename(uri, new_slug)` / `entity.copy(src, target) -> string` | S | RENAME / COPY |
| `entity.history(uri, limit) -> array` | R | version metadata (REQ-VER-032) |
| `entity.explain(uri, key) -> map` | R | source layer of an effective key (API-META-004) |
| `entity.new_slug(type) -> string` | N | slug per REQ-CONV-016 |

**query** (SPEC-21)

| Function | E | Semantics |
|---|---|---|
| `query.find(criteria) -> array` | R | criteria query; results are effective data (projected) |
| `query.count(criteria) -> int` | R | count |
| `query.named(uri, args) -> dynamic` | R | named query pipeline |
| `query.similar(uri_or_text, opts) -> array` | R | vector similarity |

**rel** (SPEC-13 §2.5)

| Function | E | Semantics |
|---|---|---|
| `rel.link(type, source, target, opts) -> string` | S | create Relationship (opts: valid_from, valid_to, metadata) |
| `rel.unlink(rel_uri)` / `rel.end(rel_uri, at)` | S | delete / end validity |
| `rel.of(uri, opts) -> array` | R | relationships of an entity (type?, direction, at?) |

**version** (SPEC-14)

| Function | E | Semantics |
|---|---|---|
| `version.diff(a, b) -> array` | R | JSON Patch |
| `version.revert(uri, commit)` | S | stage REVERT |

**emit** (SPEC-19, SPEC-20)

| Function | E | Semantics |
|---|---|---|
| `emit.event(type, payload)` | S | domain event through the outbox |
| `emit.action(target_uri, action_uri, event, args, opts)` | S | deferred action invocation (sync after flush or as a job) |
| `emit.notify(principal_uri, message)` | S | user notification (SPEC-20) |

**job / process / logic**

| Function | E | Semantics |
|---|---|---|
| `job.submit(operation_uri, args, opts) -> string` | S | create a Job entity (anchored); returns the Job URI |
| `job.status(job_uri) -> map` | R | state, attempts, result reference |
| `logic.call(logic_uri, params) -> dynamic` | as callee | SYNC call of another Logic in the same process (nesting limit) |
| `process.call(authority, operation_uri, args) -> map` | X | child process in another context (REQ-CTX-012); not atomic with the caller |

**file** (SPEC-11 REQ-STO-018)

| Function | E | Semantics |
|---|---|---|
| `file.put(content_base64, meta) -> string` | S | store a blob and stage a File entity; returns its URI |
| `file.read(file_uri) -> string` | R | base64 content (≤ 16 MiB) |
| `file.text(file_uri) -> string` | R | UTF-8 content |

**ai** (SPEC-27)

| Function | E | Semantics |
|---|---|---|
| `ai.complete(prompt, opts) -> string` | X | text completion through the Context's AiProvider |
| `ai.chat(messages, opts) -> map` | X | chat with tool-free messages |
| `ai.extract(text, json_schema, opts) -> map` | X | structured extraction validated against the schema |
| `ai.embed(text) -> array` | X | embedding vector |

**memory / vars / secret** (SPEC-16)

| Function | E | Semantics |
|---|---|---|
| `memory.get(k)`, `memory.set(k, v)`, `memory.delete(k)`, `memory.cas(k, exp, v)`, `memory.persist()` | R/S | Context memory (REQ-CTX-010) |
| `vars.get(k)`, `vars.set(k, v)`, `vars.local(k, v)` | N | blackboard (REQ-CTX-008) |
| `secret.get(name) -> string` | R | secret env value, masked in traces (REQ-CTX-003) |

**ui** (SPEC-25)

| Function | E | Semantics |
|---|---|---|
| `ui.push(target, instruction)` | S | pushed UI instruction (card, toast, dialog, navigate), delivered after the flush |
| `ui.render(uri, mode) -> map` | R | render payload of an entity (for generator stages) |

**http / mail** (external; require the capability AND a Context allowlist policy)

| Function | E | Semantics |
|---|---|---|
| `http.request(method, url, opts) -> map` | X | `opts: #{headers, body, timeout_ms ≤ 10000}`; returns `#{status, headers, body}`; hosts must match the Context's `http_allow` policy; response ≤ 4 MiB |
| `mail.send(message)` | S | staged as an outbox emit (delivered after the flush) |

**Pure libraries** (available in every profile, including RULE)

| Namespace | Functions |
|---|---|
| `log` | `debug/info/warn/error(msg)` → trace node logs |
| `time` | `parse`, `format(ts, pattern, tz?)`, `add(ts, duration)`, `diff(a, b)`, `date(ts, tz)`, `today(tz)` (from `ctx.now`) |
| `decimal` | `dec(str or number)`, `round(d, scale, mode)`, arithmetic on Decimal, `money(amount, currency)` with unit-safe add/sub (ADR-002) |
| `json` | `parse`, `stringify`, `pointer(v, ptr)`, `merge_patch(a, p)`, `patch(a, ops)` |
| `uri` | `parse(s)`, `canonical(s)`, `locator(s)`, `with_commit(s, c)` |
| `i18n` | `t(key, args)` in the effective locale |
| `crypto` | `sha256(s)`, `hmac_sha256(secret_name, s)`, `random_bytes(n)` (recorded), `uuid_v7()` (recorded) |
| `text` | `slugify`, `template(tpl, map)` (no code execution) |

- **Origin:** VRD-05-02, CON-UC-004, CON-UP-021, CON-UB-030, CON-US-021, CON-LS-061, CON-UC-060
- **Acceptance:**
  1) Every row has a descriptor in `rt.abi`.
  2) Each function has a positive test and a profile-denied test.
- **Priority:** P0 (entity, query, rel, emit, job, logic, vars, log, time, decimal, json, uri); P1 (file, ai, memory, secret, ui, http, mail, process, version, crypto, i18n, text)

### REQ-RT-014 — Emits are the only cross-process effects
- **Statement:** Logic MUST NOT perform effects that outlive the process except through:
  - staged writes (flushed atomically);
  - emits (`emit.*`, `ui.push`, `mail.send`, `job.submit`), which are written to the outbox in the flush and dispatched after it (SPEC-20);
  - EXTERNAL syscalls (`http`, `ai`, `process.call`), which are recorded and discouraged in STAGE logic whose result must be atomic.

  If a process fails, its emits are discarded.
- **Rationale:** "Scripts only give orders, the engine does the running" (LS). Effects become analysable and are never half-applied.
- **Origin:** VRD-05-06, VRD-06-05, CON-LS-021, CON-LS-025, CON-US-008
- **Acceptance:** A script that emits a notification and then throws produces no notification.
- **Priority:** P0

### 3.3 Natives

### REQ-RT-020 — Native handler registry
- **Statement:** Natives MUST be Rust implementations of `NativeHandler` (API-RT-002), registered by URI at kernel build (API-ARCH-001). For each registered native, the kernel creates or updates its `Native` descriptor entity in `logrums` at boot. Natives are invoked exactly like Logic (REQ-RT-001): same schemas, profiles, trace and provenance. A native's DEPENDS_ON_FUNC record points to its descriptor version, whose `description` includes the kernel version.
- **Origin:** VRD-05-05, CON-LC-005, CON-LC-025, CON-LS-061
- **Acceptance:** A pipeline step referencing `ubos://system/Native/validate.constraints@v1` runs in the same way as a script step and appears in the trace and lineage.
- **Priority:** P0

### REQ-RT-021 — Native catalogue v1
- **Statement:** The kernel MUST ship at least these natives:

| Native (`ubos://system/Native/…`) | E | Purpose | Chapter |
|---|---|---|---|
| `mutate@v1` | S | direct Mutate operation | SPEC-15 |
| `eval@v1` | S | ad-hoc evaluation | here |
| `entity.load_merge@v1` | S | load an entity, overlay partial input, stage the update (LC `entityLoadAndMergeHandler`) | SPEC-19 |
| `entity.create_from@v1` | S | create from a params map with type defaults | SPEC-19 |
| `validate.constraints@v1`, `validate.dict@v1`, `validate.required@v1` | N/R | validate slots | SPEC-13, SPEC-18 |
| `schema.from_constraints@v1` | N | schema slot | SPEC-13, SPEC-18 |
| `mask.pattern@v1` | N | mask slot | SPEC-13, SPEC-22 |
| `index.primitive@v1` | N | index slot | SPEC-11 |
| `ui.modes@v1` | N | ui slot | SPEC-25 |
| `format.template@v1`, `format.value@v1` | N | summary and format slots | SPEC-13, SPEC-25 |
| `transform.trim@v1`, `transform.lower@v1`, `transform.hash_bcrypt@v1` | N | transform slots (e.g. passwords, CON-UC-018) | SPEC-13 |
| `default.now@v1`, `default.principal@v1`, `default.sequence@v1` | R/S | default slots (dates, current user, numbering sequences) | SPEC-13 |
| `rule.evaluate@v1` | R | evaluate constraints and invariants of a set of change sets | SPEC-18 |
| `authorize@v1` | R | explicit policy check step | SPEC-22 |
| `fsm.transition@v1` | S | apply an action transition | SPEC-19 |
| `notify.email@v1`, `notify.webhook@v1` | X | delivery handlers used by dispatchers | SPEC-20 |
| `ai.generate_package@v1` | X | AI builder step | SPEC-27 |
| `package.load@v1` | S | package loader | SPEC-28 |
- **Origin:** VRD-05-05, CON-LC-025, CON-LS-014 (MaskLast4, TransformHash, ComputeAge), HL-LC-001
- **Acceptance:** `rt.natives` lists all of them, with schemas.
- **Priority:** P0 (those used by P0 chapters), P1 (others)

### 3.4 Authoring support

### REQ-RT-030 — Syntax check
- **Statement:** The kernel MUST provide `ubos://system/Query/rt.check@v1 {source, capabilities?, abi?, profile?}`. It compiles and statically checks the source (REQ-RT-006) without running it, and returns `{ok, diagnostics: [{line, column, severity, message, code}]}`. It needs only the permission to author logic in some tenant, and runs no user code.
- **Origin:** CON-US-005, VRD-05-03
- **Acceptance:** A missing brace is reported with its line and column, and the web editor shows a marker (SPEC-26).
- **Priority:** P1

### REQ-RT-031 — Source forms
- **Statement:** `source` MUST accept a string or an array of strings, which are joined with `"\n"`. The kernel stores exactly what was committed. The canonical JSON keeps the form given. Editors (SPEC-26) may convert between the two forms without changing the meaning.
- **Origin:** CON-US-018
- **Acceptance:** The same logic committed as a string and as lines compiles to the same AST, and the content hash of the joined text is equal.
- **Priority:** P1

---

## 4. Interfaces

### API-RT-001 — LogicRuntime
- **Kind:** rust trait (`ubos_runtime`)
- **Signature / shape:**
```rust
pub trait LogicRuntime: Send + Sync {
    /// Blocking; call from the blocking pool. Implements REQ-RT-001 steps 4–8.
    fn run(&self, x: &ExecCtxRef, logic: &CompiledLogic, params: Value, profile: Profile) -> Result<Value, UbosError>;
    fn compile(&self, source: &str, decl: &LogicDecl) -> Result<CompiledLogic, Vec<Diagnostic>>;
    fn check(&self, source: &str, decl: &LogicDecl) -> Vec<Diagnostic>;
    fn abi(&self, version: &str) -> &[SyscallDescriptor];
}
pub struct CompiledLogic { pub commit_id: Option<CommitId>, pub ast: Arc<rhai::AST>, pub decl: LogicDecl, pub content_hash: [u8; 32] }
pub struct LogicDecl { pub abi: String, pub kind: LogicKind, pub entry: Option<String>, pub effect: Effect,
                       pub capabilities: Vec<String>, pub imports: Vec<String>, pub limits: LogicLimits }
```
- **Origin:** CON-UC-006 (runner), CON-UP-023

### API-RT-002 — NativeHandler
- **Kind:** rust trait
- **Signature / shape:**
```rust
#[async_trait]
pub trait NativeHandler: Send + Sync {
    fn descriptor(&self) -> NativeDescriptor;            // uri, title, description, schemas, effect, slots
    async fn call(&self, x: &mut ExecCtxMut<'_>, params: Value) -> Result<Value, UbosError>;
}
```
- **Semantics:** `ExecCtxMut` gives the native the same capabilities as syscalls (session, services), subject to the declared effect.
- **Origin:** CON-LC-005 (ProcessStepHandler), CON-UB-002 (NativeActionHandler)

### API-RT-003 — Syscall registration
- **Kind:** rust macro / builder
- **Signature / shape:**
```rust
syscalls::register(&mut engine, SyscallSpec {
    name: "entity.get", abi: "v1", effect: Effect::Read,
    profiles: &[Full, ReadOnly, Pure, DryRun],
    handler: |x: &ExecCtxRef, uri: &str| -> Result<Dynamic, UbosError> { x.kernel().entity_get(x, uri, Default::default()) },
});
```
- **Semantics:** Registration generates the `SyscallDescriptor` (REQ-RT-011) and applies the wrapper (REQ-RT-012). Namespaced names are exposed as Rhai modules (`entity::get` is also accepted), and the dotted form `entity.get` is provided through a static-module object.
- **Origin:** CON-UC-004

### API-RT-004 — Named operations
- **Kind:** UBTP targets
- **Signature / shape:**

| URI | Args | Result |
|---|---|---|
| `ubos://system/Action/logic.run@v1` | `{logic: uri, params, dry_run?, mode: SYNC\|JOB}` | result or Job URI |
| `ubos://system/Action/rt.eval@v1` | `{source, capabilities?, commit?: bool}` | result, logs, staged diff |
| `ubos://system/Query/rt.check@v1` | `{source, capabilities?, abi?, profile?}` | diagnostics |
| `ubos://system/Query/rt.abi@v1` | `{abi?}` | syscall descriptors |
| `ubos://system/Query/rt.natives@v1` | – | native descriptors |
- **Origin:** CON-UC-025, CON-US-005, CON-UP-020 (`/api/run`)

### Example logic

```rhai
// ubos://acme/Logic/invoice.post.v1   effect: STAGE, capabilities: [entity, query, emit, decimal, log]
fn main(params) {
    let inv = entity::get(params.invoice);                // effective data, provenance recorded
    if inv._state != "APPROVED" { throw #{ code: "ACME.NOT_APPROVED", message: "Invoice not approved" }; }
    let total = dec("0");
    for line in inv.lines { total += dec(line.amount); }
    entity::patch(params.invoice, #{ total: total.to_string(), posted_at: ctx.now });
    let acct = entity::get(inv.account);
    entity::patch(inv.account, #{ balance: (dec(acct.balance) + total).to_string() });
    emit::event("acme.invoice.posted", #{ invoice: params.invoice, total: total.to_string() });
    log::info(`posted ${params.invoice} total ${total}`);
    #{ total: total.to_string() }
}
```

### Error codes (RT)

| Code | Category | Meaning |
|---|---|---|
| `RT.COMPILE_ERROR` | INVALID | source does not compile (line, column) |
| `RT.DECLARATION_MISMATCH` | INVALID | capabilities/effect/imports/entry inconsistent with the source |
| `RT.INPUT_INVALID` / `RT.OUTPUT_INVALID` | INVALID | schema violation |
| `RT.SCRIPT_ERROR` | INVALID | uncaught script error or `throw` |
| `RT.CAPABILITY_DENIED` | FORBIDDEN | syscall not available for the logic or profile |
| `RT.DRY_RUN_BLOCKED` | FORBIDDEN | external syscall in a dry run |
| `RT.LIMIT` | LIMIT | sandbox limit exceeded (not catchable) |
| `RT.IMPORT_DENIED` / `RT.IMPORT_CYCLE` | INVALID | module import rules |
| `RT.READ_ONLY_CONTEXT` | INVALID | assignment to `ctx` fields |
| `RT.HTTP_DENIED` | FORBIDDEN | host not allowed by the Context policy |
| `RT.EXTERNAL_FAILED` | UNAVAILABLE | external call failed (retryable per details) |
| `RT.NATIVE_NOT_FOUND` | NOT_FOUND | unregistered native URI |

---

## 5. Non-functional

| ID | Requirement | Target |
|---|---|---|
| NFR-PERF-070 | Call overhead of a cached trivial logic (no syscalls) | p95 ≤ 50 µs |
| NFR-PERF-071 | Syscall overhead (wrapper, excluding the service) | ≤ 5 µs |
| NFR-PERF-072 | Compile of a 500-line script | ≤ 20 ms |
| NFR-SEC-070 | Logic cannot reach the host | no file, process, network or environment access except through syscalls (engine has no such packages) |
| NFR-AIR-070 | ABI is machine-readable | `rt.abi` descriptors with signatures and examples; used in AI prompts (SPEC-27) |

---

## 6. Acceptance

| REQ | Criterion |
|---|---|
| REQ-RT-001, 002 | Invocation steps; mode behaviours; eval audited |
| REQ-RT-003…007 | Engine configuration; limit tests; profile denials; commit-time static checks; compile cache metric |
| REQ-RT-008…010 | Module import with lineage; catchable vs uncatchable errors; deterministic re-run |
| REQ-RT-011…014 | ABI publication and compatibility; wrapper behaviour; catalogue coverage; emits discarded on failure |
| REQ-RT-020, 021 | Natives run like logic; catalogue listed |
| REQ-RT-030, 031 | Syntax check diagnostics; source forms |

---

## 7. Implementation notes

- **Reference code:**
  - UC syscalls [UC:src/kernel/syscalls/mod.rs#L12-L32], factory and limits [UC:src/kernel/factory.rs#L20-L71], module resolver [UC:src/kernel/resolver.rs#L16-L58], eval [UC:src/kernel/executor/ephemeral.rs#L14-L74].
  - US sync executor [US:crates/ubos_kernel/src/executor/sync.rs#L19-L94], syntax check [US:apps/ubos_server/src/api/handlers/syntax.rs].
  - LS Rhai adapter [LS:crates/ubos_kernel/src/script/adapter.rs#L26-L48].
  - UP sandbox (a design reference for the layers) [UP:ubos-server/src/main/java/org/logrum/ubos/engine/config/ScriptSecurityConfig.java].
- **Syscall naming:** the corpus's `sys_db_get` becomes `entity.get`, `sys_db_commit` becomes `entity.upsert`, `sys_db_history` becomes `entity.history`, `sys_resolve_prop` becomes `entity.explain`, `sys_memory_*` becomes `memory.*`, `sys_invoke_async` becomes `job.submit`, `sys_file_*` becomes `file.*`, `sys_ai_chat` becomes `ai.chat`, and `sys_push_card` becomes `ui.push`. `native::emit_signal` maps to `emit.*` and `native::load_entity` to `entity.get`.
- **Rhai money:** use the `decimal` feature (`rust_decimal`). Never use f64 for amounts (REQ-CONV-037).
- **Threading:** run scripts with `tokio::task::spawn_blocking`, or on a dedicated `rayon`-like pool sized by `UBOS_RT_THREADS`. Inside syscalls, use `Handle::block_on` only from that pool, never from async workers.

## 8. Open questions

None.
- Deferred: WASM as a second `language` (later version, via ADR).
- Deferred to P2: the replay tool (REQ-RT-010).
- Deferred to P2: LS behaviour refine chains beyond config inheritance.
