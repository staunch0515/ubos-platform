---
id: SPEC-10
title: "Architecture Overview"
status: complete
phase: P4
depends_on: [SPEC-00, SPEC-01, SPEC-02, ADR-001, ADR-003, ADR-004]
sources: [UP, FU, LC, UB, UC, US, LS, UW]
---

# Architecture Overview

## 0. Chapter header

- **Scope:** This chapter defines the platform's structure:
  - its layers and components;
  - the crates and their dependency rules;
  - the runtime roles and topology;
  - the path of a request from a protocol message to commits and events;
  - the editions.
  Every other chapter details one component named here.
- **MOD:** `ARCH`
- **depends_on:** SPEC-01, SPEC-02, ADR-001, ADR-003, ADR-004.
- **Terms used:** TERM-Kernel, TERM-Host, TERM-Client, TERM-Ubtp, TERM-Process, TERM-Session, TERM-Flush, TERM-Outbox, TERM-RuntimeStore, TERM-Edition, TERM-TopologyRole, TERM-SixTables.
- **Origin summary:** This chapter comes from:
  - VRD-19-01 (kernel is a library, hosts are thin), VRD-19-02 (editions), VRD-19-03 (topology roles);
  - VRD-14-04 (shells use the protocol only), VRD-15-01/02 (UBTP and bindings), VRD-20-01…05 (stack);
  - HL-UB layering (intent / rules / facts, CON-UB-001), UP's three-layer backend (CON-UP-001), US's layered kernel (CON-US-002), LS's orchestrator (CON-LS-004), UC's loader → runner → committer (CON-UC-006).

---

## 1. Concepts

### 1.1 The platform in one paragraph

The platform stores every artifact as a versioned entity in six tables (SPEC-11). It gives
meaning to entities through a self-describing type system (SPEC-13). It changes them only
through processes that stage change sets and flush them atomically (SPEC-15). It runs
business logic as sandboxed Rhai behind a syscall ABI (SPEC-17). It orchestrates work with
pipelines and action state machines (SPEC-19). It serves five clients over one protocol,
UBTP (SPEC-24, ADR-003).

### 1.2 Design principles

| # | Principle | Consequence | Origin |
|---|---|---|---|
| P1 | Everything is an entity | Types, logic, views, policies, jobs, contexts and packages are rows in the same six tables | CON-UP-013, CON-UC-040, VRD-01-01 |
| P2 | One write path | All writes go through a process, a session and one flush. No component writes the store directly. | CON-LS-041, CON-UC-041, VRD-04-02 |
| P3 | The kernel is business-agnostic | The store knows no business vocabulary; business meaning comes from metadata | CON-UB-001 (BPU) |
| P4 | Judging is pure | Rules, guards and schema checks read injected data and do no I/O | CON-UB-001 (ESE), ADR-002 |
| P5 | Logic acts through syscalls only | Scripts have no host access. Side effects are syscalls or returned emits. | VRD-05-02, VRD-06-05 |
| P6 | Clients use the protocol only | No client reads the database. The desktop client reaches its embedded kernel through the in-process binding. | VRD-14-04, ADR-003 |
| P7 | Versions are immutable | Caches keyed by commit ID never need invalidation | CON-LS-044, CON-UB-045 |
| P8 | Deny by default | Every operation and read is authorised; invisible data is reported as not found | VRD-10-02, REQ-CONV-052 |
| P9 | AI uses the same paths as people | Agents are principals; AI-built definitions go through drafts and approval | VRD-16-01, VRD-16-03 |
| P10 | One core, two editions | The same kernel, packages and protocol run on SQLite and on PostgreSQL | VRD-19-02 |

### 1.3 Layers

The layers of SPEC-00 §2, as runtime components:

```
 L6 Interfaces    Protocol dispatcher (UBTP) · UI render pipeline · AI services
 L5 Governance    Authentication · Policy decision point · Masking · Approvals · Tenancy
 L4 Runtime       Context resolver · Logic runtime (Rhai + syscalls + natives) · Rules engine
                  Orchestration (pipelines, action FSM, process instances) · Events/jobs · Query
 L3 Transactions  Process manager · Session (unit of work) · Flush · Idempotency · Provenance · Audit
 L2 Semantics     URI resolver · Type registry · Inheritance resolution · Semantic caches
 L1 Store         Store traits · PostgreSQL backend · SQLite backend · Runtime store · Blob store
 L7 Lifecycle     Package loader · Genesis · Boot · Editions (cross-cutting)
```

- REQ-ARCH-001 below states the dependency rule between these layers.

---

## 2. Model

### 2.1 Crates

```yaml
ENT-Crate:
  purpose: A unit of the implementation workspace (ADR-001, REQ-CONV-080).
  origin: [VRD-19-01, CON-US-001, CON-LS-006]
  fields:
    - name: name
      type: string
      required: true
      description: Crate name with prefix ubos_.
    - name: kind
      type: enum{LIBRARY|HOST|TOOL}
      required: true
      description: Library crates hold logic; hosts are executables; tools support development.
    - name: depends_on
      type: list<string>
      required: true
      description: Allowed crate dependencies (table below).
  invariants:
    - The crate dependency graph is acyclic and matches the table below.
```

| Crate | Kind | Contents | May depend on | Chapter |
|---|---|---|---|---|
| `ubos_proto` | library | UBTP messages, envelope, error object, URI type and parser, spec types (§6 of SPEC-02). No I/O. | – | SPEC-12, SPEC-24 |
| `ubos_store` | library | Store traits, PostgreSQL and SQLite backends (features `enterprise`, `personal`), runtime-store trait and backends, blob-store trait | `ubos_proto` | SPEC-11 |
| `ubos_runtime` | library | Rhai engine configuration, sandbox, syscall registry, rule subset (ADR-002), compile cache | `ubos_proto` | SPEC-17, SPEC-18 |
| `ubos_kernel` | library | Semantics, versioning, transactions, context, rules, orchestration, events and jobs, query, security, tenancy, UI render, AI services, loader, protocol dispatcher | `ubos_proto`, `ubos_store`, `ubos_runtime` | SPEC-12 … SPEC-30 |
| `ubos_server` | host | TCP, WebSocket and HTTP bindings; roles gateway, worker, scheduler, dispatcher (§2.3) | `ubos_kernel` | SPEC-24, SPEC-31 |
| `ubos_cli` | host | Command-line client (remote over UBTP) and local admin commands (embedded kernel) | `ubos_kernel`, `ubos_sdk` | SPEC-26 |
| `ubos_desktop` | host | Tauri 2 app: web shell plus embedded kernel over the in-process binding | `ubos_kernel` | SPEC-26, SPEC-31 |
| `ubos_sdk` | library | Rust UBTP client (TCP, WebSocket, HTTP, in-process) | `ubos_proto` | SPEC-24 |
| `@ubos/sdk` | library (TS) | TypeScript UBTP client, URI library, error types | – | SPEC-24, SPEC-26 |
| `@ubos/shell` | app (TS) | React web shell: cockpit and studio; also packaged for desktop and mobile | `@ubos/sdk` | SPEC-25, SPEC-26 |

### 2.2 Kernel components

```yaml
ENT-KernelComponent:
  purpose: A named module inside ubos_kernel with one responsibility.
  origin: [CON-UB-002, CON-US-002, CON-UC-006]
  fields:
    - name: name
      type: string
      required: true
      description: Module name (table below).
    - name: layer
      type: enum{L1|L2|L3|L4|L5|L6|L7}
      required: true
      description: Layer of §1.3.
    - name: chapter
      type: string
      required: true
      description: Normative chapter.
  invariants:
    - A component calls only components of its own or a lower layer, except through events (REQ-ARCH-001).
```

| Component | Layer | Responsibility | Chapter |
|---|---|---|---|
| `uri` | L2 | parse, normalise, resolve authorities and selectors | SPEC-12 |
| `semantic` | L2 | type registry, meta-schema check, inheritance resolution, commit-keyed caches | SPEC-13 |
| `version` | L2/L3 | branches, heads, overlay reads, merge, revert, diff | SPEC-14 |
| `tx` | L3 | process manager, session, change sets, flush, CAS, idempotency, provenance | SPEC-15 |
| `context` | L4 | Context entities, execution context, memory, identity propagation | SPEC-16 |
| `logic` | L4 | invoke logic, syscall implementations, native handler registry | SPEC-17 |
| `rules` | L4 | derived schemas, levels, constraints, invariants, decisions, guards | SPEC-18 |
| `flow` | L4 | pipelines, action state machines, process instances, emit dispatch | SPEC-19 |
| `events` | L4 | outbox relay, hooks, cron scheduler, jobs, webhooks, subscriptions | SPEC-20 |
| `query` | L4 | criteria queries, history, blame, lineage, vectors, query pipelines | SPEC-21 |
| `security` | L5 | authentication, policy decision point, masking, approvals, API keys | SPEC-22 |
| `tenancy` | L5 | tenants, root overlay, resolution order, cross-tenant rules | SPEC-23 |
| `dispatch` | L6 | UBTP message handling, protocol sessions, streaming | SPEC-24 |
| `ui` | L6 | widget resolution, render pipeline, pushed UI | SPEC-25 |
| `ai` | L6 | AI providers, AI syscalls, builder loop, copilot, explanations | SPEC-27 |
| `packages` | L7 | package loader, genesis, boot | SPEC-28 |
| `obs` | cross | audit record, trace tree, telemetry | SPEC-30 |

### 2.3 Runtime roles

```yaml
ENT-RuntimeRole:
  purpose: A function a running node performs (TERM-TopologyRole).
  origin: [VRD-19-03, CON-UC-003]
  fields:
    - name: role
      type: enum{GATEWAY|WORKER|SCHEDULER|DISPATCHER|DATABASE|RUNTIME_STORE|AI_PROVIDER}
      required: true
      description: The role (table below).
    - name: scalable
      type: bool
      required: true
      description: Whether several instances may run at once.
  invariants:
    - SCHEDULER and DISPATCHER use leader election (a lease in the runtime store) when more than one instance runs.
```

| Role | Does | Scales | Process |
|---|---|---|---|
| GATEWAY | Terminates the transport bindings, holds protocol sessions, runs synchronous processes | horizontally | `ubos_server --role gateway` |
| WORKER | Takes jobs from the queue and runs them as processes | horizontally | `ubos_server --role worker` |
| SCHEDULER | Fires Cron entities, sweeps stale jobs (ADR-004) | one leader | `ubos_server --role scheduler` |
| DISPATCHER | Relays outbox events to hooks, subscriptions and webhooks | one leader per partition | `ubos_server --role dispatcher` |
| DATABASE | Holds the six tables | vertical, read replicas | PostgreSQL or SQLite |
| RUNTIME_STORE | Holds transient technical state | vertical or cluster | Redis or in-process |
| AI_PROVIDER | Serves model calls | external | configured by AiProvider entities |

- `ubos_server --role all` runs every role in one process (development and small installations).
- The personal edition always runs all roles in one process.

---

## 3. Behavior

### REQ-ARCH-001 — Layer dependency rule
- **Statement:** A kernel component MUST call only components of its own layer or lower layers (§1.3, §2.2). Upward communication MUST go through events (outbox) or through return values (emits).
- **Rationale:** This keeps the store and the semantics business-agnostic and testable in isolation (UB's "UBOS → ESE → BPU" direction).
- **Origin:** CON-UB-001, CON-UP-001, CON-US-002
- **Acceptance:**
  1) A dependency check in CI fails on an upward module import.
  2) `ubos_store` compiles and passes its tests without `ubos_kernel`.
- **Priority:** P0

### REQ-ARCH-002 — Kernel as a library
- **Statement:** The kernel MUST be a library crate with no transport, no process entry point and no global mutable state apart from explicit handles. Hosts MUST construct it through `KernelBuilder` (API-ARCH-001) and MUST NOT contain business logic.
- **Rationale:** The same kernel serves the server, desktop, CLI and tests (VRD-19-01).
- **Origin:** VRD-19-01, CON-US-001, CON-LS-006
- **Acceptance:**
  1) An integration test builds a kernel on an in-memory SQLite store and runs a scenario without any host.
  2) Host crates contain no references to entity types other than configuration.
- **Priority:** P0

### REQ-ARCH-003 — Single write path
- **Statement:** Every write of the six tables MUST happen inside a flush (SPEC-15) of a process. The flush MUST be the only code that calls the store's write traits. Genesis, package loading, migrations of data and administrative repairs use the same path with the system identity.
- **Rationale:** Every change is then versioned, audited, indexed and linked to its process (LS "only commit_changes", UC atomic write unit).
- **Origin:** CON-LS-041, CON-UC-041, CON-UB-031, VRD-04-01, VRD-04-02
- **Acceptance:**
  1) The store write traits are visible only to the `tx` module (crate-private re-export).
  2) Every commit has a `process_commit_map` row with relation `OUTPUT`.
- **Priority:** P0

### REQ-ARCH-004 — Request lifecycle
- **Statement:** A UBTP request that may write (Mutate, Emit) MUST pass through these stages in order. A stage that fails ends the request with the error of that stage.

| # | Stage | Component | Output | Chapter |
|---|---|---|---|---|
| 1 | Decode frame into an envelope and message | `dispatch` | `Envelope`, `Message` | SPEC-24 |
| 2 | Authenticate: protocol session principal, or API key/token on the HTTP binding | `security` | `Principal` | SPEC-22 |
| 3 | Resolve the context authority into tenant, branch or pin | `context`, `uri` | `ResolvedContext` | SPEC-16, SPEC-12 |
| 4 | Idempotency check (if a key is given): replay, reject mismatch, or reserve | `tx` | reservation | SPEC-15 |
| 5 | Open a process and an execution context (identity, tenant, context, trace root) | `tx`, `context` | `Process`, `ExecutionContext` | SPEC-15, SPEC-16 |
| 6 | Resolve the target: entity URI (Mutate) or named operation URI and version (Emit) | `uri`, `semantic`, `flow` | target definition | SPEC-12, SPEC-19 |
| 7 | Authorise the operation on the target | `security` | decision | SPEC-22 |
| 8 | Run: direct changes (Mutate), or action stages and pipeline steps with logic in the sandbox (Emit). Reads go through the session and are recorded as provenance. Writes become change sets. Side effects become emits. | `flow`, `logic`, `tx` | change sets, emits | SPEC-17, SPEC-19 |
| 9 | Validate the change sets at the requested level: derived schema, constraints, invariants, decisions | `rules` | decisions | SPEC-18 |
| 10 | Flush in one database transaction: CAS on heads, versions, heads, registry, derived index, process log, commit map, outbox rows | `tx` | commits | SPEC-15, SPEC-11 |
| 11 | Post-commit: complete the idempotency record, update head caches, hand emits and outbox events to the dispatcher | `tx`, `events` | – | SPEC-15, SPEC-20 |
| 12 | Respond: `Result` (sync) or `Ack` with a job URI (async), plus process ID and commit IDs | `dispatch` | response | SPEC-24 |

- **Rationale:** One sequence for all writes gives uniform audit, security and idempotency (LC request envelope, UC loader → runner → committer, LS orchestrator routing).
- **Origin:** CON-LC-003, CON-UC-006, CON-LS-004, VRD-15-01, VRD-15-04
- **Acceptance:**
  1) The trace tree of any write request shows stages 5–11 as spans in this order.
  2) Failure injection at each stage leaves no partial commit.
  3) A failed process still produces an audit record with status FAILED (SPEC-30).
- **Priority:** P0

### REQ-ARCH-005 — Read lifecycle
- **Statement:** A Query request MUST pass stages 1–3, 5 (read-only process, no flush) and 7. It then runs the query against the resolved branch or pin and applies masking (SPEC-22). Reads MUST NOT write the six tables. A read-only process writes an audit record only when the tenant's audit policy requires read auditing (SPEC-30).
- **Origin:** VRD-12-02, VRD-10-03, VRD-21-01
- **Acceptance:**
  1) A query produces no rows in `process_log` unless read auditing is enabled.
  2) Masked fields are masked in every binding.
- **Priority:** P0

### REQ-ARCH-006 — Asynchronous work
- **Statement:** Work that is long, retried or triggered runs as a Job entity (ADR-004). This covers async emits, hook and cron firings, embeddings and webhook delivery. The request's process creates the Job in its flush. A worker runs each attempt as its own process, which records the job's commit as `DEPENDS_ON_DATA`.
- **Rationale:** "Audit by default": the job exists before any queue signal (UC).
- **Origin:** CON-UC-003, CON-UC-032, VRD-11-03, VRD-15-04
- **Acceptance:**
  1) Killing the runtime store after the flush loses no job: the sweeper re-enqueues it.
  2) A job's attempts are visible as processes linked to the Job entity.
- **Priority:** P0

### REQ-ARCH-007 — Roles in one process or many
- **Statement:** Every runtime role (§2.3) MUST be able to run in the same process as the others, and GATEWAY and WORKER MUST be able to run as independent processes against shared DATABASE and RUNTIME_STORE instances. Leader-only roles MUST hold a lease in the runtime store and give it up on shutdown.
- **Origin:** VRD-19-03, CON-UC-003
- **Acceptance:** A scenario suite passes both with `--role all` and with separate gateway, worker, scheduler and dispatcher processes.
- **Priority:** P1

### REQ-ARCH-008 — Editions from one code base
- **Statement:** The personal and enterprise editions MUST be built from the same crates with Cargo features (`personal`, `enterprise`). Behaviour differences MUST be limited to those listed in SPEC-31, for example vector search degraded, no partitions, single tenant by default, and an in-process runtime store.
- **Origin:** VRD-19-02, CON-LS-062
- **Acceptance:** The shared scenario suite passes on both editions; edition-specific scenarios are tagged.
- **Priority:** P0 (enterprise), P1 (personal)

### REQ-ARCH-009 — Configuration
- **Statement:** A host MUST read its technical configuration (database URL, runtime-store URL, bind addresses, roles, limits, telemetry) from a TOML file and environment variables with the prefix `UBOS_`. Environment variables override the file. Business configuration (tenants, contexts, policies, AI providers) MUST be entities, never host configuration.
- **Rationale:** Operators need a small, standard surface, while business behaviour stays versioned (P1).
- **Origin:** NEW: required for deployability; consistent with CON-UC-040.
- **Acceptance:** A host starts from environment variables alone, and no business type or policy appears in the configuration schema.
- **Priority:** P0

### REQ-ARCH-010 — Startup order
- **Statement:** On start, a host MUST perform these steps in order:
  1) load configuration;
  2) open the store and check the schema version (running migrations only when `--migrate` is given or the edition is personal);
  3) open the runtime store;
  4) build the kernel;
  5) run boot (SPEC-28): genesis when the root tenant is empty, then package checks;
  6) warm the semantic caches for the root tenant;
  7) start the roles.
  The gateway MUST NOT accept connections before step 6 completes.
- **Origin:** VRD-17-03, CON-LS-001, CON-UP-027
- **Acceptance:** Health endpoints report `starting` until step 6 and `ready` afterwards.
- **Priority:** P0

### REQ-ARCH-011 — Health and readiness
- **Statement:** The server host MUST expose `GET /health/live` (process up) and `GET /health/ready` (store, runtime store and caches ready; the role's own checks) on the HTTP binding. These endpoints need no authentication and return no tenant data.
- **Origin:** NEW: operability (SPEC-32 NFR-OPS).
- **Acceptance:** Stopping the database turns `ready` to 503 within 5 seconds, while `live` stays 200.
- **Priority:** P1

### REQ-ARCH-012 — Graceful shutdown
- **Statement:** On SIGTERM a host MUST:
  1) stop accepting new requests and jobs;
  2) let running processes finish, up to a configurable drain timeout (default 30 s);
  3) release leases, flush telemetry and exit.
  A process cut off by the timeout does not flush. Its job, if any, stays RUNNING until the sweeper re-queues it.
- **Origin:** NEW: operability.
- **Acceptance:** A shutdown during load leaves no partial commit and no lost job.
- **Priority:** P1

---

## 4. Interfaces

### API-ARCH-001 — KernelBuilder and Kernel
- **Kind:** rust trait / struct
- **Signature / shape:**
```rust
pub struct KernelBuilder { /* private */ }
impl KernelBuilder {
    pub fn new(config: KernelConfig) -> Self;
    pub fn store(self, store: Arc<dyn Store>) -> Self;              // SPEC-11 API-STO-001
    pub fn runtime_store(self, rt: Arc<dyn RuntimeStore>) -> Self;  // SPEC-11 API-STO-009
    pub fn blob_store(self, blobs: Arc<dyn BlobStore>) -> Self;     // SPEC-11 API-STO-010
    pub fn clock(self, clock: Arc<dyn Clock>) -> Self;              // injectable for replay
    pub fn native(self, uri: &str, handler: Arc<dyn NativeHandler>) -> Self; // SPEC-17
    pub fn ai_client(self, client: Arc<dyn AiClient>) -> Self;      // SPEC-27
    pub async fn build(self) -> Result<Kernel, KernelError>;
}

pub struct Kernel { /* Arc inner; cheap to clone */ }
impl Kernel {
    /// Handle one UBTP request; responses are streamed (Ack, Op*, Result | Error).
    pub fn handle(&self, session: &ProtocolSession, req: Request)
        -> impl Stream<Item = Response> + Send;
    pub async fn open_session(&self, hs: Handshake, auth: Credentials)
        -> Result<ProtocolSession, UbosError>;                      // SPEC-24
    pub async fn boot(&self, opts: BootOptions) -> Result<BootReport, UbosError>; // SPEC-28
    pub fn worker(&self) -> WorkerHandle;       // SPEC-20
    pub fn scheduler(&self) -> SchedulerHandle; // SPEC-20
    pub fn dispatcher(&self) -> DispatcherHandle; // SPEC-20
    pub async fn shutdown(&self, drain: Duration);
}
```
- **Semantics:**
  - `build` validates the configuration and the store schema version.
  - `handle` never panics. Every failure becomes an `Error` response with the SPEC-02 §8 shape.
- **Origin:** VRD-19-01, CON-UB-002 (SPI-first contracts)

### API-ARCH-002 — Host configuration schema
- **Kind:** file (TOML) + environment
- **Signature / shape:**
```toml
[server]
roles = ["all"]                 # gateway | worker | scheduler | dispatcher | all
tcp_bind = "0.0.0.0:9876"       # UBTP TCP binding (LS default port)
http_bind = "0.0.0.0:8080"      # HTTP + WebSocket bindings
[store]
url = "postgres://…"            # or "sqlite:///path/ubos.db"
max_connections = 32
[runtime_store]
url = "redis://…"               # or "memory:" / "sqlite:///path/ubos-rt.db"
[blob_store]
url = "file:///var/lib/ubos/blobs"   # or "s3://bucket/prefix"
[limits]
request_timeout_ms = 30000
drain_timeout_ms = 30000
[telemetry]
otlp_endpoint = ""
log_format = "json"
```
- **Semantics:** Each key has an environment override `UBOS_<SECTION>_<KEY>` in upper case, e.g. `UBOS_STORE_URL`.
- **Origin:** REQ-ARCH-009

### API-ARCH-003 — Health endpoints
- **Kind:** http
- **Signature / shape:** `GET /health/live` → `200 {"status":"live"}`; `GET /health/ready` → `200 {"status":"ready","checks":{…}}` or `503`.
- **Origin:** REQ-ARCH-011

### Error codes (ARCH)

| Code | Category | Meaning |
|---|---|---|
| `ARCH.NOT_READY` | UNAVAILABLE | kernel is still booting |
| `ARCH.SHUTTING_DOWN` | UNAVAILABLE | host is draining |
| `ARCH.SCHEMA_VERSION` | INTERNAL | store schema version does not match the kernel |
| `ARCH.CONFIG_INVALID` | INTERNAL | configuration rejected at start |

---

## 5. Non-functional

| ID | Requirement | Target |
|---|---|---|
| NFR-PERF-001 | Overhead of stages 1–7 and 11–12 of REQ-ARCH-004, excluding logic and database time | p95 ≤ 5 ms on the reference server (SPEC-32) |
| NFR-PORT-001 | Platforms for the server host | Linux x86_64 and aarch64 |
| NFR-PORT-002 | Platforms for the desktop host | Windows, macOS, Linux |
| NFR-OPS-001 | Start to ready with warm caches (root tenant only) | ≤ 10 s enterprise, ≤ 3 s personal |
| NFR-AVAIL-001 | Gateway and worker instances are stateless apart from protocol sessions | Losing one gateway drops only its connections |

---

## 6. Acceptance

| REQ | Criterion |
|---|---|
| REQ-ARCH-001 | The CI dependency check passes, and `ubos_store` is tested standalone |
| REQ-ARCH-002 | The embedded-kernel scenario test passes without a host |
| REQ-ARCH-003 | Store write traits are private to `tx`; every commit has an OUTPUT lineage row |
| REQ-ARCH-004 | Trace spans follow the stage order; fault injection leaves no partial commit; FAILED audit records exist |
| REQ-ARCH-005 | Queries write nothing unless read auditing is on; masking is applied |
| REQ-ARCH-006 | Runtime-store loss after a flush loses no job |
| REQ-ARCH-007 | The scenario suite passes in both deployment shapes |
| REQ-ARCH-008 | The scenario suite passes on both editions |
| REQ-ARCH-009…012 | Configuration, startup, health and shutdown tests pass |

---

## 7. Implementation notes

- **Reference implementations in the corpus:**
  - US workspace layout: [US:Cargo.toml] (CON-US-001).
  - LS store feature switch: [LS:crates/ubos_store/src/backend/mod.rs] (CON-LS-006).
  - LS orchestrator routing by operation: [LS:crates/ubos_kernel/src/engine/orchestrator/mod.rs] (CON-LS-004).
  - UC job pipeline: [UC:src/kernel/executor/worker/] (CON-UC-006).
  - UB SPI interfaces: [UB:backend/src/main/java/com/ubos/core/api/] (CON-UB-002).
- **Avoid** UC's `Mutex<Kernel>`, which serialised the whole API (CON-UC-003 design note). The kernel is `Send + Sync`, with fine-grained state behind `Arc`, and the store is accessed through a pool.
- **Blocking scripts:** Rhai runs on a dedicated blocking pool sized by configuration. The async executor never blocks (REQ-CONV-084, CON-UC-005).
- **Suggested build order** (VRD-22-03):
  1) `ubos_proto`;
  2) `ubos_store` (SPEC-11);
  3) `uri` and `semantic` (SPEC-12, SPEC-13);
  4) `version` and `tx` (SPEC-14, SPEC-15);
  5) `context` and `logic` (SPEC-16, SPEC-17);
  6) `rules` and `flow` (SPEC-18, SPEC-19);
  7) `security`, `query` and `dispatch` (SPEC-22, SPEC-21, SPEC-24);
  8) `packages` (SPEC-28).

  Verify each step with its chapter's acceptance section.

## 8. Open questions

None. Deferred: VRD-19-05 (offline sync by branches, later version, SPEC-31).
