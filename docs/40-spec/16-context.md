---
id: SPEC-16
title: "Context and Execution Context"
status: complete
phase: P4
depends_on: [SPEC-02, SPEC-12, SPEC-14, SPEC-15, ADR-004, ADR-005]
sources: [UP, FU, LC, UB, UC, US, UW]
---

# Context and Execution Context

## 0. Chapter header

- **Scope:** This chapter defines two different objects:
  - The **environment**: the `Context` entity. It covers the tenant, the branch or pin, environment variables, memory, context policies, and the host and sovereign tenants.
  - The **execution**: the per-process `ExecutionContext`. It covers identity, resolved context, session, blackboard variables, the trace tree, budgets and cancellation.

  It also defines how identity propagates implicitly, the system identity, session memory, and the tenant roles of foreign logic.
- **MOD:** `CTX`
- **depends_on:** SPEC-02, SPEC-12, SPEC-14, SPEC-15, ADR-004, ADR-005.
- **Terms used:** TERM-Context, TERM-ExecutionContext, TERM-TraceTree, TERM-Memory, TERM-SovereignTenant, TERM-HostTenant, TERM-SystemIdentity, TERM-Pin, TERM-Principal.
- **Origin summary:**
  - Verdicts: VRD-07-01…05, VRD-08-06, VRD-18-03.
  - UB: Context as an entity (CON-UB-017), ProcessContext as an execution tree (CON-UB-020), context-in/context-out (CON-UB-004).
  - UC: Workspace (CON-UC-015), environment vs. execution and host vs. sovereign (CON-UC-024), Ambassador memory (CON-UC-023), ProcessContext (CON-UC-022), the thread-local bridge (CON-UC-005).
  - FU: environments with pins (CON-FU-031).
  - LC and US: the blackboard (CON-LC-011, CON-US-028).
  - UP: identity through context and the SYSTEM default (CON-UP-030).
  - UW: the working context of project, tenant and branch (CON-UW-011).

---

## 1. Concepts

| | Context (environment) | ExecutionContext (execution) |
|---|---|---|
| What | where work happens | one run of work |
| Lifetime | long, versioned entity | one process |
| Chosen by | user or client (Handshake, URI authority) | kernel, per request or job |
| Holds | tenant roles, branch or pin, env vars, memory, context policies, locale | principal, resolved context, session, variables, trace, budget |
| Stored | `Context` entity in the tenant (on `main`) | audit record + trace (SPEC-30) |
| Corpus | UB Context, UC Workspace/Logrum, FU environment, UW ProjectContext | UB/UC ProcessContext, LC blackboard |

"The Context is the environment, the Process is the execution, and the user lives in the Process" (UC, CON-UC-024).

---

## 2. Model

```yaml
ENT-Context:
  purpose: A versioned environment entity (TERM-Context); addressable as URI authority tenant.slug (ADR-005).
  origin: [VRD-07-01, VRD-08-06, CON-UB-017, CON-UC-015, CON-UC-023, CON-FU-031]
  fields:
    - name: title
      type: string(255)
      required: true
      description: Display name (e.g. "Production").
    - name: kind
      type: enum{ENVIRONMENT|SESSION|AGENT|SYSTEM}
      required: true
      description: ENVIRONMENT = dev/uat/prod-like; SESSION = per-client ambassador session; AGENT = an AI agent's home (SPEC-27); SYSTEM = kernel use.
    - name: mode
      type: enum{BRANCH|PINNED}
      required: true
      description: Follow a branch head or a fixed point.
    - name: branch
      type: branch_name
      required: true
      description: Branch followed (BRANCH) or branch of the point (PINNED).
    - name: pin
      type: PinSpec?
      required: false
      description: PINNED only - {tag - slug} or {at - timestamp} (SPEC-14 REQ-VER-006).
    - name: host_tenant
      type: tenant_code?
      required: false
      description: Tenant whose data the context works in when different from the Context's own tenant (the sovereign tenant) (CON-UC-024).
    - name: env_vars
      type: map<string, EnvValue>?
      required: false
      description: UPPER_SNAKE keys; EnvValue = {value - string} or {secret - uri<Secret>} (SPEC-22).
    - name: memory_policy
      type: enum{NONE|CACHE|DURABLE}
      required: true
      description: NONE = no memory; CACHE = runtime store only; DURABLE = also persisted into this entity. Default NONE for ENVIRONMENT, CACHE for SESSION/AGENT.
    - name: memory_ttl
      type: duration?
      required: false
      description: CACHE lifetime after last use; default PT24H.
    - name: memory
      type: map<string, json>?
      required: false
      description: Durable memory (written by the kernel only, REQ-CTX-010).
    - name: read_only
      type: bool?
      required: false
      description: Reject write processes in this context. Default false.
    - name: policies
      type: list<uri<Policy>>?
      required: false
      description: Context-bound policies, applied in addition to principal policies (SPEC-22).
    - name: default_level
      type: enum{DRAFT|PUBLISH}?
      required: false
      description: Validation level override within REQ-TX-003 rules.
    - name: locale
      type: string?
      required: false
      description: Default BCP 47 locale.
    - name: timezone
      type: string?
      required: false
      description: Default IANA time zone.
    - name: ai
      type: AiContextSpec?
      required: false
      description: {provider - uri<AiProvider>, allow_external - bool, allow_in_dry_run - bool} (SPEC-27).
    - name: owner_principal
      type: uuid?
      required: false
      description: SESSION/AGENT - the principal the session belongs to.
    - name: expires_at
      type: timestamp?
      required: false
      description: SESSION - after this time the context is not resolvable (CTX.EXPIRED).
  invariants:
    - Context entities are committed only on the tenant's main branch.
    - mode = PINNED  <=>  pin is set.
    - env_vars keys match ^[A-Z][A-Z0-9_]{0,63}$ and do not use reserved names (REQ-CTX-003).
    - memory is written only through the kernel (REQ-CTX-010); clients writing it get META.RESERVED_KEY-like error CTX.MEMORY_READ_ONLY.
    - _extends to another Context inherits env_vars, policies, locale, timezone, ai (prototype chain, SPEC-13).
```

```yaml
ENT-ResolvedContext:
  purpose: Result of resolving a Context (or a bare tenant authority) for one process.
  origin: [VRD-07-01, VRD-03-03]
  fields:
    - {name: context_uri, type: uri?, required: false, description: "Context used (absent if bare tenant without default Context)"}
    - {name: context_commit, type: commit_id?, required: false, description: "Version used (recorded as DEPENDS_ON_CTX)"}
    - {name: sovereign_tenant, type: tenant_code, required: true, description: "Tenant owning the Context (or the bare tenant)"}
    - {name: host_tenant, type: tenant_code, required: true, description: "Tenant whose data is read by default; = sovereign unless host_tenant set"}
    - {name: branch, type: branch_name, required: true, description: "Branch"}
    - {name: point, type: timestamp?, required: false, description: "Pinned time (PINNED)"}
    - {name: env, type: "map<string, EnvValue>", required: true, description: "Composed env vars incl. platform vars (REQ-CTX-003)"}
    - {name: policies, type: "list<uri>", required: true, description: "Context policies"}
    - {name: read_only, type: bool, required: true, description: "Effective read-only flag (PINNED contexts are always read-only)"}
    - {name: memory_policy, type: "enum{NONE|CACHE|DURABLE}", required: true, description: "Memory policy"}
    - {name: locale, type: string, required: true, description: "Effective locale"}
    - {name: timezone, type: string, required: true, description: "Effective time zone"}
```

```yaml
ENT-ExecutionContext:
  purpose: Per-process execution state (TERM-ExecutionContext); passed to every kernel call and syscall, never constructed by scripts.
  origin: [VRD-07-02, VRD-07-03, CON-UB-020, CON-UC-022, CON-UC-005, CON-LC-011]
  fields:
    - {name: process_id, type: uuid, required: true, description: "Owning process (SPEC-15)"}
    - {name: trace_id, type: string, required: true, description: "W3C trace id (SPEC-30)"}
    - {name: principal, type: Principal, required: true, description: "{id, kind USER|SERVICE|API_KEY|AGENT|SYSTEM, tenant, roles snapshot, scopes}"}
    - {name: on_behalf_of, type: Principal?, required: false, description: "Delegating principal (approvals, agents)"}
    - {name: app_id, type: uuid?, required: false, description: "Initiating package/app"}
    - {name: context, type: ResolvedContext, required: true, description: "Environment"}
    - {name: data_tenant, type: tenant_code, required: true, description: "Tenant that receives writes (REQ-CTX-011)"}
    - {name: read_tenants, type: "list<tenant_code>", required: true, description: "Ordered tenants readable by resolution (REQ-CTX-011, SPEC-23)"}
    - {name: branch, type: branch_name, required: true, description: "Write branch"}
    - {name: now, type: timestamp, required: true, description: "Fixed process clock (REQ-TX-002)"}
    - {name: profile, type: "enum{FULL|READ_ONLY|PURE|DRY_RUN}", required: true, description: "Syscall profile (SPEC-17)"}
    - {name: vars, type: "map<string, json>", required: true, description: "Blackboard variables (REQ-CTX-008)"}
    - {name: trace, type: TraceTree, required: true, description: "Execution node tree (REQ-CTX-009)"}
    - {name: budget, type: Budget, required: true, description: "Remaining operations, time (deadline), rows, AI tokens"}
    - {name: cancel, type: CancellationToken, required: true, description: "Set on timeout, shutdown or client cancel"}
    - {name: request, type: RequestMeta, required: true, description: "request_id, client kind, client ip, user agent, protocol session id"}
    - {name: memory, type: MemoryHandle?, required: false, description: "Lazy handle to context memory (REQ-CTX-010)"}
    - {name: locale, type: string, required: true, description: "Principal preference > context > tenant default"}
  invariants:
    - principal, on_behalf_of, context, data_tenant, read_tenants, now are immutable for the process.
    - Scripts can read identity fields (ctx.principal, ctx.tenant) but never set them (REQ-CTX-006).
```

```yaml
ENT-TraceNode:
  purpose: One node of the execution tree (TERM-TraceTree).
  origin: [VRD-07-02, VRD-21-03, CON-UB-020, CON-US-029]
  fields:
    - {name: id, type: int32, required: true, description: "Sequential within the process"}
    - {name: parent, type: int32?, required: false, description: "Parent node"}
    - {name: kind, type: "enum{OPERATION|STAGE|STEP|LOGIC|SLOT|SYSCALL|RULE|QUERY|FLUSH|EMIT|AI}", required: true, description: "What ran"}
    - {name: name, type: string, required: true, description: "URI or syscall name"}
    - {name: commit_id, type: commit_id?, required: false, description: "Version of the definition that ran"}
    - {name: started_at, type: timestamp, required: true, description: "Start (monotonic offsets allowed)"}
    - {name: duration_us, type: int64?, required: false, description: "Duration"}
    - {name: status, type: "enum{RUNNING|OK|ERROR|VETO|SKIPPED}", required: true, description: "Outcome"}
    - {name: input, type: json?, required: false, description: "Input digest or value (≤ 4 KiB, masked)"}
    - {name: output, type: json?, required: false, description: "Output digest or value (≤ 4 KiB, masked)"}
    - {name: error, type: error?, required: false, description: "Error object"}
    - {name: logs, type: "list<{level, message, at}>?", required: false, description: "Script/log lines attached to this node (≤ 100 per node)"}
    - {name: mutations, type: "list<{path, from, to, by}>?", required: false, description: "Field changes caused here (US causal trace), when tracing level is DETAILED"}
```

---

## 3. Behavior

### 3.1 Context entity

### REQ-CTX-001 — Default contexts
- **Statement:** Creating a tenant (SPEC-23) MUST create `Context/default` (kind ENVIRONMENT, mode BRANCH, branch `main`, memory_policy NONE). Creating other Contexts is an ordinary Mutate or action on the tenant's main branch. Deleting `default` is forbidden (`CTX.DEFAULT_REQUIRED`).
- **Origin:** VRD-07-01, VRD-18-03, CON-UC-015 (genesis workspace)
- **Acceptance:** `ubos://acme/Invoice/inv-1` resolves through `Context/default`, and its process records DEPENDS_ON_CTX on that version.
- **Priority:** P0

### REQ-CTX-002 — Resolving a context
- **Statement:** Resolving authority `t[.c]` MUST do the following:
  1) Load `Context/c`, or `Context/default` when no slug is given, from tenant *t*'s `main` head. If `default` does not exist, use a built-in fallback: branch `main`, no env and no policies.
  2) Compose it with its prototype chain (SPEC-13).
  3) Check `expires_at`.
  4) Build the `ResolvedContext`:
     - `sovereign_tenant = t`;
     - `host_tenant = host_tenant ?? t`;
     - the branch and point;
     - the env vars (REQ-CTX-003);
     - `read_only = read_only || mode = PINNED`.
  5) Record DEPENDS_ON_CTX with the Context version.

  A missing context fails with `URI.CONTEXT_NOT_FOUND`, and an expired one with `CTX.EXPIRED`. Resolution is cached by (tenant, slug, Context head commit) and invalidated by commits of the Context or its prototypes.
- **Origin:** VRD-07-01, VRD-03-03, ADR-005, CON-UB-017
- **Acceptance:**
  1) A PINNED Context on tag `release-2026.09` resolves to read-only with that point.
  2) Changing its env var takes effect on the next request.
- **Priority:** P0

### REQ-CTX-003 — Environment variables
- **Statement:** The effective env MUST be composed from the Context's prototype chain (nearest wins), then overlaid with these **platform variables**, which are read-only and reserved:

| Variable | Value |
|---|---|
| `UBOS_TENANT` | sovereign tenant |
| `UBOS_HOST_TENANT` | host tenant |
| `UBOS_BRANCH` | branch |
| `UBOS_CONTEXT` | canonical Context URI |
| `UBOS_CONTEXT_KIND` | Context kind |
| `UBOS_IS_AMBASSADOR` | `"true"` when host ≠ sovereign (UC `IS_AMBASSADOR`) |
| `UBOS_EDITION` | `personal` or `enterprise` |

  Secret values (`{secret: uri}`) MUST NOT be materialised in the env map. Logic reads them only through `secret.get(name)` (SPEC-17), which checks policy, records DEPENDS_ON_DATA on the Secret version, and masks the value in traces and logs.
- **Origin:** VRD-07-01, CON-UC-015, CON-UC-023, NEW: secret indirection
- **Acceptance:** `ctx.env.UBOS_BRANCH` returns the branch. A secret never appears in the trace or in the audit record.
- **Priority:** P0

### REQ-CTX-004 — Context restrictions
- **Statement:** The context MUST restrict work as follows:
  - A process whose resolved context is `read_only` MUST reject Mutate, Emit of write-capable operations, and any staging, with `CTX.READ_ONLY`. Queries and pure actions (SPEC-19 `effect: NONE`) are allowed.
  - Context `policies` MUST be evaluated in addition to the principal's policies (SPEC-22). Both must allow.
- **Rationale:** Frozen releases and production guards are data, not code (FU pinned environments).
- **Origin:** VRD-08-06, VRD-07-01, CON-FU-031
- **Acceptance:** A Mutate against `acme.release` (PINNED) fails with `CTX.READ_ONLY`.
- **Priority:** P0

### 3.2 Execution context

### REQ-CTX-005 — Creation and propagation
- **Statement:** The kernel MUST create exactly one `ExecutionContext` per process (REQ-TX-002) and pass it explicitly to kernel services.
  - Within async code, it is carried in a Tokio task-local.
  - Within Rhai, it is attached to the engine call (a per-call tag or scope handle), so syscalls obtain it without script arguments.
  - Worker threads MUST clear it after each execution.
- **Rationale:** UC's thread-local bridge, generalised to async tasks and blocking pools.
- **Origin:** VRD-07-02, CON-UC-005, CON-UP-030
- **Acceptance:** Two concurrent processes on one worker thread pool never observe each other's principal (stress test with interleaved syscalls).
- **Priority:** P0

### REQ-CTX-006 — Implicit identity
- **Statement:**
  - Syscalls MUST take the principal, tenant and branch from the execution context. No syscall accepts a principal or tenant argument that changes *who* acts. Cross-tenant syscalls (SPEC-23) take a *target* tenant and are checked against grants.
  - Scripts MAY read `ctx.principal`, `ctx.tenant`, `ctx.branch`, `ctx.now`, `ctx.env`, `ctx.locale` and `ctx.process_id`.
  - Assigning to them fails with `RT.READ_ONLY_CONTEXT`.
- **Origin:** VRD-07-03, CON-UC-005, CON-UC-024, CON-UP-030
- **Acceptance:** A script that sets `ctx.tenant = "other"` fails. A script calling `entity.get` on another tenant's entity without a grant gets `URI.NOT_FOUND`.
- **Priority:** P0

### REQ-CTX-007 — System identity
- **Statement:** Every tenant MUST have a service principal `ServiceAccount/system` (SPEC-22), created with the tenant. Triggered work (cron, hooks, event handlers, sweepers) MUST run:
  - as the trigger's declared `run_as` principal, which is checked when the trigger is committed (SPEC-20); or
  - by default, as the tenant's system principal, with `on_behalf_of` = the principal whose commit caused the trigger, when there is one.

  Kernel maintenance runs as `ubos://logrums/ServiceAccount/kernel`. System principals are subject to policies like any other principal. They are not superusers.
- **Origin:** VRD-07-03, VRD-11-05, CON-UP-030 (SYSTEM), CON-LC-027 (SYSTEM_INTERNAL_ROLE)
- **Acceptance:** A hook firing after a user's commit shows principal `system` and on_behalf_of = that user in its audit record.
- **Priority:** P0

### REQ-CTX-008 — Blackboard variables
- **Statement:** The execution context MUST provide `vars`, a map shared by all steps of one process (LC blackboard). Variable scopes:
  - process-wide (`vars.set("x", v)`);
  - step-local (automatically cleared when the step ends, SPEC-19).

  The pipeline definitions name the keys steps read and write (SPEC-19). Values are JSON, with at most 16 MiB in total. `vars` are included in the trace at DETAILED level only (masked).
- **Origin:** VRD-07-02, CON-LC-011, CON-US-028
- **Acceptance:** Step 1 writes `vars.user_id`, and step 3 reads it. A step-local var is gone after its step.
- **Priority:** P0

### REQ-CTX-009 — Trace tree
- **Statement:** The execution context MUST record a trace tree (ENT-TraceNode):
  - Each operation, stage, step, logic call, slot, rule, query, flush, emit and AI call opens a node, and so does each syscall at DETAILED level.
  - Logs from scripts (`log.info`, …) attach to the current node.
  - Levels are `SUMMARY` (operations, stages, steps, errors) and `DETAILED` (everything). The default is SUMMARY; `DETAILED` can be set by the Context env `UBOS_TRACE=DETAILED` or per request by principals with `ubos.trace.detailed`.
  - Limits: 10 000 nodes, 1 MiB serialised. Beyond them, children are summarised (count, total duration, first error).
  - The tree is persisted per SPEC-30: inline in `trace_summary` up to 64 KiB, otherwise offloaded and referenced by `trace_ref`.
- **Origin:** VRD-07-02, VRD-21-03, CON-UB-020, CON-UC-022, CON-US-029
- **Acceptance:** A failed step appears as an ERROR node with its error and the logs emitted before the failure.
- **Priority:** P0

### REQ-CTX-010 — Memory
- **Statement:** For contexts with memory_policy CACHE or DURABLE:
  1) The first `memory.get` or `memory.set` in a process loads `mem:{context}` from the runtime store. On a miss, it loads from the Context's durable `memory` property.
  2) Writes are buffered in the execution context as per-key changes.
  3) When the process is COMPLETED, the changed keys are merged into `mem:{context}` per key (last writer wins), and the TTL is refreshed. FAILED and REJECTED processes discard their memory changes.
  4) If the policy is DURABLE, or the logic called `memory.persist()`, the process also stages an UPDATE of the Context entity's `memory` property. That update goes into the **same flush** as the business changes, with CAS on the Context head.
  5) `memory.cas(key, expected, value)` provides strict updates. On mismatch it fails with `CTX.MEMORY_CONFLICT`.

  Memory values are JSON, with at most 1 MiB per context in the cache and 256 KiB durable.
- **Rationale:**
  - UC's Ambassador memory, made consistent: failed runs do not leak state.
  - Durable memory commits atomically with the work that produced it (VRD-07-04).
- **Origin:** VRD-07-04, CON-UC-023, CON-UC-043
- **Acceptance:**
  1) A failed process leaves memory unchanged.
  2) A DURABLE context shows a new Context version per completed process that changed memory.
  3) After a runtime-store flush, memory is recovered from the durable version.
- **Priority:** P1

### REQ-CTX-011 — Data tenant and read tenants
- **Statement:** The kernel MUST compute `data_tenant` and `read_tenants` for each process:

| Situation | data_tenant | read_tenants (in order) |
|---|---|---|
| Normal (host = sovereign = t) | t | t, logrums |
| Ambassador context (sovereign S, host H), logic owned by H or root | H | H, logrums |
| Ambassador context, **pure** logic owned by S (Logic `pure: true`) | H | H, S (read-only, only entities S shares), logrums |
| Ambassador context, **effectful** logic owned by S | S | S, H (only with a grant policy in H for S), logrums |

  Writes always go to `data_tenant`, which makes S's effectful writes "invisible to H unless shared" (VRD-18-04). Every cross-tenant read is authorised against a grant (SPEC-22/23) and recorded as DEPENDS_ON_DATA.
- **Origin:** VRD-07-05, VRD-18-04, CON-UC-024
- **Acceptance:**
  1) A vendor's effectful logic running in a customer's embassy writes entities with tenant = vendor.
  2) The customer's queries do not return them.
  3) The vendor's pure logic computes over customer data and writes nothing of its own.
- **Priority:** P1

### REQ-CTX-012 — One context per process
- **Statement:** A process MUST have exactly one resolved context. Work in another context is a **separate process**:
  - asynchronously, through a Job whose `context` is set (SPEC-20); or
  - synchronously, through `process.call(context_uri, operation, args)` (SPEC-17). This runs a child process with its own flush, a `parent_process_id`, and no atomicity with the caller.
- **Origin:** NEW: keeps atomicity and audit per context clear; CON-UC-026 (child jobs)
- **Acceptance:** `process.call` into another context produces two process log rows linked by `parent_process_id`.
- **Priority:** P1

### REQ-CTX-013 — Deadlines and cancellation
- **Statement:** The execution context MUST carry a deadline, which is the smaller of the request timeout and the job timeout, and a cancellation token. It is checked:
  - by the Rhai progress callback, at least every 10 000 operations;
  - before every syscall;
  - by store calls, through statement timeouts.

  Cancellation ends the process as FAILED with `TX.TIMEOUT` or `TX.ABORTED`, and nothing is flushed.
- **Origin:** VRD-05-03, CON-UP-022, CON-UC-030
- **Acceptance:** A script with an infinite loop is stopped at the deadline, and the process row shows `TX.TIMEOUT`.
- **Priority:** P0

### REQ-CTX-014 — Locale and time zone
- **Statement:** The effective locale and time zone MUST be taken from the first set value in this order: principal preference → Context → tenant default → `en` / `UTC`. They affect formatting (the `format` slot), i18n and date calculations in logic. They never affect stored values, which are UTC (REQ-CONV-034).
- **Origin:** NEW: required by SPEC-25/27; VRD-16-07
- **Acceptance:** The same money value renders with the user's locale separators, and the stored value is unchanged.
- **Priority:** P1

### 3.3 Clients and contexts

### REQ-CTX-015 — Context selection by clients
- **Statement:** A client MUST select a context in the UBTP Handshake (`context` = authority). This becomes the protocol session's default context (SPEC-24). A single request MAY override it by using an addressing URI with another authority, if the principal may access that context. Clients SHOULD display the active tenant, context and branch permanently, and warn before writes to protected branches.
- **Rationale:** UC's rule that the front end selects a Workspace first, UW's context switcher, and UC Plan_17's branch warning.
- **Origin:** CON-UC-015, CON-UC-045, CON-UW-011, CON-UW-051
- **Acceptance:** After a Handshake with `acme.uat`, `Invoice/inv-1` resolves in `acme.uat`, and a request with `ubos://acme.prod/…` uses prod.
- **Priority:** P0

### REQ-CTX-016 — Session contexts
- **Statement:** `ctx.session.open({host_tenant?, alias, memory_policy?, ttl?})` MUST find or create a Context of kind SESSION in the caller's (sovereign) tenant with slug `session.<principal-slug>.<alias>`, owned by the caller and with `expires_at = now + ttl` (default 7 days). It returns its authority. Opening an existing session renews `expires_at`. Sessions with a host tenant require a grant in the host (SPEC-23).
- **Origin:** CON-UC-023 (ambassador connect), VRD-16-03
- **Acceptance:** Two connects with the same alias return the same session context, and memory persists between them.
- **Priority:** P1

---

## 4. Interfaces

### API-CTX-001 — ContextService
- **Kind:** rust trait (`ubos_kernel::context`)
- **Signature / shape:**
```rust
#[async_trait]
pub trait ContextService: Send + Sync {
    async fn resolve(&self, principal: &Principal, authority: &Authority) -> Result<ResolvedContext, UbosError>;
    async fn open_session(&self, x: &ExecutionContext, req: SessionOpen) -> Result<Authority, UbosError>;
    fn execution_context(&self, p: &Process, rc: ResolvedContext, req: RequestMeta,
                         profile: Profile, budget: Budget) -> ExecutionContext;
    async fn memory(&self, x: &ExecutionContext) -> Result<MemoryHandle, UbosError>;
}
impl MemoryHandle {
    pub fn get(&mut self, key: &str) -> Option<Value>;
    pub fn set(&mut self, key: &str, v: Value);
    pub fn delete(&mut self, key: &str);
    pub fn cas(&mut self, key: &str, expected: Option<&Value>, v: Value) -> Result<(), UbosError>;
    pub fn persist(&mut self);                      // mark durable write for this process
}
```
- **Origin:** CON-UC-015, CON-UC-023

### API-CTX-002 — Script-visible context (Rhai)
- **Kind:** Rhai object `ctx` (read-only), plus syscall namespaces `memory.*`, `vars.*`, `log.*`, `secret.*` (signatures in SPEC-17)
- **Signature / shape:**
```rhai
ctx.process_id      // string
ctx.principal       // #{ id, kind, slug, tenant, roles: [...] }
ctx.on_behalf_of    // () or principal map
ctx.tenant          // data tenant
ctx.host_tenant     // host tenant
ctx.branch          // write branch
ctx.context         // canonical Context URI or ()
ctx.now             // RFC 3339 string (fixed)
ctx.env             // map of non-secret env vars incl. UBOS_* platform vars
ctx.locale; ctx.timezone
```
- **Origin:** VRD-07-03, CON-US-014 (`ctx.identity`), CON-UP-030

### API-CTX-003 — Named operations
- **Kind:** UBTP Emit/Query targets
- **Signature / shape:**

| URI | Args | Result |
|---|---|---|
| `ubos://system/Action/context.session_open@v1` | `{host_tenant?, alias, memory_policy?, ttl?}` | authority |
| `ubos://system/Query/context.resolve@v1` | `{authority}` | ResolvedContext without secrets |
| `ubos://system/Query/context.memory@v1` | `{authority}` | memory map (owner or admin only) |
- **Origin:** CON-UC-023

### Error codes (CTX)

| Code | Category | Meaning |
|---|---|---|
| `CTX.READ_ONLY` | FORBIDDEN | write in a read-only or pinned context |
| `CTX.EXPIRED` | NOT_FOUND | session context expired |
| `CTX.DEFAULT_REQUIRED` | CONFLICT | attempt to delete `Context/default` |
| `CTX.MEMORY_CONFLICT` | CONFLICT | memory CAS failed |
| `CTX.MEMORY_LIMIT` | LIMIT | memory size exceeded |
| `CTX.MEMORY_READ_ONLY` | INVALID | client attempted to write the `memory` property |
| `CTX.RESERVED_VAR` | INVALID | env var uses a reserved `UBOS_` name |

---

## 5. Non-functional

| ID | Requirement | Target |
|---|---|---|
| NFR-PERF-060 | Context resolution, warm cache | p95 ≤ 50 µs |
| NFR-PERF-061 | Execution-context creation | ≤ 20 µs |
| NFR-PRIV-060 | Secrets never in trace, logs or audit | 0 occurrences in a secret-leak scan of traces |
| NFR-OPS-060 | Trace overhead at SUMMARY level | ≤ 5 % of process time |

---

## 6. Acceptance

| REQ | Criterion |
|---|---|
| REQ-CTX-001…004 | Default Context; pinned resolution; platform vars; secret masking; read-only rejection |
| REQ-CTX-005…009 | Isolation stress test; immutable identity; system principal with on_behalf_of; blackboard scopes; trace on failure |
| REQ-CTX-010 | Memory consistency on failure; durable versions; recovery after cache loss |
| REQ-CTX-011, 012 | Tenant table cases; child process linkage |
| REQ-CTX-013, 014 | Deadline stop; locale formatting |
| REQ-CTX-015, 016 | Handshake default and override; session reuse |

---

## 7. Implementation notes

- **Reference code:**
  - UC Workspace [UC:src/domain/workspace.rs#L10-L76], workspace manager [UC:src/kernel/workspace_manager.rs#L19-L90], thread context [UC:src/kernel/context.rs#L6-L27], ProcessContext [UC:src/kernel/process_context.rs#L9-L87].
  - UB ProcessContext and ExecutionNode [UB:backend/src/main/java/com/ubos/core/context/ProcessContext.java], [UB:backend/src/main/java/com/ubos/core/context/ExecutionNode.java].
  - UB Context resolution [UB:backend/src/main/java/com/ubos/ese/lifecycle/ContextEnvironmentProvider.java].
  - LC blackboard [LC:src/main/java/com/logicorum/model/ProcessContext.java].
- **Gaps closed:**
  - UC's step 4 (host env vars honoured by reads) was never wired (CON-UC-024). Here REQ-CTX-011 fixes it.
  - UC wrote memory back even without a durable commit. Here memory is atomic with the process.
- **Rhai handles:** use `Engine::call_fn_with_options` with a `tag` or a custom `Scope` constant that holds an `Arc<ExecContextRef>`. Syscalls downcast it. Never store the context in a global.

## 8. Open questions

None. Deferred to later versions: context templates in the marketplace (packages provide Context prototypes, which works today through `_extends`).
