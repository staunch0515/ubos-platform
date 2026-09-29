---
id: SPEC-32
title: "Non-Functional Requirements Catalogue"
status: complete
phase: P4
depends_on: [SPEC-02, SPEC-10, SPEC-31]
sources: [UB, UC, LC, LS]
---

# Non-Functional Requirements Catalogue

## 0. Chapter header

- **Scope:** This chapter covers four things:
  - the reference hardware and the measurement rules for every NFR;
  - system-level NFRs that cut across chapters (§2);
  - the complete catalogue of the chapter NFRs, grouped by area (§3). It is generated from the chapters, and the chapters stay normative for their own items;
  - the requirements for the benchmark and verification harness (§4).
- **MOD:** `NFR`
- **depends_on:** SPEC-02 (the NFR area registry, §2.3), SPEC-10, SPEC-31.
- **Origin summary:**
  - UB scale notes: hot heads, embeddings, partitions and RLS (D-UB-12, CMP-01).
  - UC robustness self-audit (CON-UC-035).
  - LC Redis high-concurrency proposal (CON-LC-046).
  - LS instrumented kernel (CON-LS-064).
  - The NFR items of SPEC-10 to SPEC-31.

---

## 1. Measurement rules

### 1.1 Reference environment

| Item | Reference |
|---|---|
| Server | 8 vCPU (x86_64, ≥ 3 GHz), 16 GiB RAM, NVMe SSD, Linux |
| PostgreSQL | 16 with pgvector, on a separate host of the same class, `shared_buffers` 4 GiB, same availability zone (network RTT ≤ 0.5 ms) |
| Redis | 7, 2 vCPU, 4 GiB, same zone |
| Personal edition | 4-core laptop class CPU, 16 GiB RAM, NVMe SSD |
| Client network (UX items) | 50 ms RTT, 20 Mbit/s |
| Data set "M" | 1 tenant, 50 types, 1 million entities of the largest type, 10 million versions, 50 million search index rows |
| Data set "L" | 100 tenants, 100 million versions in total, partitioned (REQ-STO-015) |

### 1.2 Rules

- **R1:** Latency targets are measured as p95 (and p99 where stated) over at least 10 000 operations after a warm-up of 1 000. Figures marked "warm cache" assume the relevant caches are populated. "Cold" means fresh caches with warm database buffers.
- **R2:** Unless stated otherwise, targets exclude client network time and the time spent in user logic and AI providers.
- **R3:** Throughput targets are measured with the load mix of §1.3 at a CPU utilisation of at most 70 %.
- **R4:** Each NFR names its data set (M unless stated). Chapter NFRs without one use M.
- **R5:** An NFR target is a release gate: a regression of more than 10 % against the previous release blocks the release (REQ-NFR-003).

### 1.3 Load mix "OLTP-1"

| Share | Operation |
|---|---|
| 55 % | Query (`get` / criteria with 1–2 indexed filters, page 50) |
| 20 % | render (`ui.render` detail or table) |
| 15 % | Emit (action EXECUTE with 1 Rhai step, 1–3 entities written) |
| 7 % | Mutate (1–5 changes) |
| 3 % | subscriptions and events (fan-out to 1 000 sessions) |

---

## 2. System-level NFRs

| ID | Requirement | Target | Measured by |
|---|---|---|---|
| NFR-PERF-900 | Throughput of one SINGLE_NODE (reference server) under OLTP-1 | ≥ 1 500 requests/s, p95 ≤ 100 ms | load harness |
| NFR-PERF-901 | Write throughput of one PostgreSQL primary (Emit/Mutate only) | ≥ 800 processes/s, ≥ 2 000 commits/s | load harness |
| NFR-PERF-902 | Personal edition write throughput | ≥ 150 processes/s | load harness |
| NFR-SCAL-900 | Horizontal scaling of gateways and workers | throughput grows ≥ 0.8× per added node up to 8 nodes before the database saturates | load harness |
| NFR-SCAL-901 | Hot-entity contention (one entity written by 100 clients) | no errors other than `VER.HEAD_CONFLICT`; ≥ 200 successful commits/s on the entity | contention test (UB hot-head note) |
| NFR-AVAIL-900 | Availability of SCALED deployments (excluding the database's own SLA) | 99.9 % monthly | synthetic probes |
| NFR-AVAIL-901 | Loss of any single non-database node | no failed committed work; client reconnect ≤ 10 s | chaos test |
| NFR-DUR-900 | Committed process durability | no committed process lost after a crash of any component except the database storage | crash test |
| NFR-DUR-901 | Outbox and jobs | no lost events or jobs after runtime-store loss (REQ-EVT-026) | chaos test |
| NFR-SEC-900 | Tenant isolation | 0 cross-tenant reads in the isolation suite on both editions (REQ-TEN-030) | isolation suite |
| NFR-SEC-901 | Default deny | every permission and resource not explicitly granted is denied | property tests (REQ-SEC-013) |
| NFR-PRIV-900 | Personal data minimisation in non-business channels | no personal data in telemetry, logs or metrics labels; masked in traces and AI prompts | leak scans |
| NFR-PORT-900 | Edition parity | 100 % of shared scenarios pass on both editions (REQ-DEP-001) | CI matrix |
| NFR-AIR-900 | Machine readability of the platform | meta-schema, ABI, URI conformance, CLI commands, OpenAPI and package format available as versioned fixtures (REQ-AI-060) | fixture check |
| NFR-OPS-900 | Observability coverage | every request has a request log line, a trace span, and (if writing) an audit record | coverage test |
| NFR-OPS-901 | Diagnosability | every error returned to a client carries a code, and a process or trace ID when a process ran | protocol tests |
| NFR-UX-900 | Perceived responsiveness of the web cockpit | 90 % of interactions give visible feedback ≤ 100 ms and complete ≤ 1 s | RUM in staging |

---

## 3. Catalogue of chapter NFRs

This section is generated from the chapters. To change an item, edit the chapter and regenerate this section (REQ-NFR-004).

### 3.1 Performance (PERF)

| ID | Requirement | Target | Defined in |
|---|---|---|---|
| NFR-PERF-001 | Overhead of stages 1–7 and 11–12 of REQ-ARCH-004, excluding logic and database time | p95 ≤ 5 ms on the reference server (SPEC-32) | SPEC-10 |
| NFR-PERF-010 | Head read by locator (cache miss) | p95 ≤ 2 ms (PG), ≤ 1 ms (SQLite) | SPEC-11 |
| NFR-PERF-011 | Flush of 1 entity with 20 indexed paths | p95 ≤ 10 ms (PG) | SPEC-11 |
| NFR-PERF-012 | Flush of 100 entities in one process | p95 ≤ 250 ms (PG) | SPEC-11 |
| NFR-PERF-020 | Parse + normalise (no registry) | ≤ 5 µs per URI (Rust), ≤ 50 µs (TS) | SPEC-12 |
| NFR-PERF-021 | Resolve with warm caches | p95 ≤ 0.5 ms | SPEC-12 |
| NFR-PERF-030 | Effective type definition, warm cache | p95 ≤ 50 µs | SPEC-13 |
| NFR-PERF-031 | Effective type definition, cold (chain of 6, 30 properties) | p95 ≤ 5 ms | SPEC-13 |
| NFR-PERF-032 | Effective data of an instance with 2 prototypes, warm type cache | p95 ≤ 1 ms + reads | SPEC-13 |
| NFR-PERF-040 | Resolve on a 3-level branch chain, warm head cache | p95 ≤ 0.3 ms | SPEC-14 |
| NFR-PERF-041 | Branch status for 10 000 own heads | ≤ 1 s, no snapshot reads | SPEC-14 |
| NFR-PERF-042 | Publish of a draft with 100 entities (fast-forward) | p95 ≤ 1 s (PG) | SPEC-14 |
| NFR-PERF-050 | Mutate of 1 entity end to end (HTTP binding, PG, warm caches, no logic) | p95 ≤ 25 ms | SPEC-15 |
| NFR-PERF-051 | Emit of an action with 1 Rhai step writing 3 entities | p95 ≤ 60 ms | SPEC-15 |
| NFR-PERF-060 | Context resolution, warm cache | p95 ≤ 50 µs | SPEC-16 |
| NFR-PERF-061 | Execution-context creation | ≤ 20 µs | SPEC-16 |
| NFR-PERF-070 | Call overhead of a cached trivial logic (no syscalls) | p95 ≤ 50 µs | SPEC-17 |
| NFR-PERF-071 | Syscall overhead (wrapper, excluding the service) | ≤ 5 µs | SPEC-17 |
| NFR-PERF-072 | Compile of a 500-line script | ≤ 20 ms | SPEC-17 |
| NFR-PERF-080 | Structural validation of an entity with 30 properties (cached schema) | p95 ≤ 200 µs | SPEC-18 |
| NFR-PERF-081 | Invariant evaluation (cached AST) | p95 ≤ 50 µs per invariant | SPEC-18 |
| NFR-PERF-082 | UNIQUE check with index | p95 ≤ 2 ms (PG) | SPEC-18 |
| NFR-PERF-090 | Pipeline step overhead (excluding the implementation) | ≤ 20 µs | SPEC-19 |
| NFR-PERF-091 | Available-actions listing for a target | p95 ≤ 5 ms | SPEC-19 |
| NFR-PERF-092 | Signal delivery to an instance (sync, PG) | p95 ≤ 50 ms | SPEC-19 |
| NFR-PERF-100 | Criteria query, 1 indexed filter, page 50, 1 M entities of the type (PG) | p95 ≤ 50 ms | SPEC-21 |
| NFR-PERF-101 | Same on a 2-level branch chain | p95 ≤ 80 ms | SPEC-21 |
| NFR-PERF-102 | Count with an indexed filter up to the cap | p95 ≤ 200 ms | SPEC-21 |
| NFR-PERF-103 | Similarity k=10 over 1 M vectors (PG, HNSW) | p95 ≤ 100 ms | SPEC-21 |
| NFR-PERF-110 | `authorize` with warm caches (principal set, policies) | p95 ≤ 100 µs | SPEC-22 |
| NFR-PERF-111 | Principal-set cache invalidation after a role change | ≤ 5 s cluster-wide (via events) | SPEC-22 |
| NFR-PERF-120 | Tenant provisioning (without optional packages) | ≤ 5 s | SPEC-23 |
| NFR-PERF-130 | Protocol overhead per request (decode, dispatch, encode) | ≤ 100 µs (JSON), ≤ 40 µs (msgpack) for 2 KiB messages | SPEC-24 |
| NFR-PERF-140 | Genesis (system + ontology, ~400 definitions) on PostgreSQL | ≤ 15 s; on SQLite ≤ 5 s | SPEC-28 |
| NFR-PERF-141 | No-op reinstall of a 1 000-item package | ≤ 2 s (hash comparison only) | SPEC-28 |
| NFR-PERF-150 | Audit overhead in the flush (row + lineage + inline trace) | ≤ 15 % of flush time | SPEC-30 |
| NFR-PERF-160 | Commit → event on subscriber session | p95 ≤ 200 ms | SPEC-20 |
| NFR-PERF-161 | Commit → hook job started (idle workers) | p95 ≤ 1 s | SPEC-20 |
| NFR-PERF-170 | `ui.render` of an entity detail with warm caches | p95 ≤ 30 ms (server) | SPEC-25 |
| NFR-PERF-171 | Table render, 50 rows, 8 columns | p95 ≤ 80 ms (server) | SPEC-25 |
| NFR-PERF-190 | Gateway overhead excluding the provider | ≤ 10 ms | SPEC-27 |

### 3.43 Scalability (SCAL)

| ID | Requirement | Target | Defined in |
|---|---|---|---|
| NFR-SCAL-010 | Versions per tenant with no degradation of head reads | ≥ 100 million (partitioned) | SPEC-11 |
| NFR-SCAL-040 | Branches per tenant | ≥ 10 000 (drafts included) | SPEC-14 |
| NFR-SCAL-100 | Indexed rows | ≥ 1 billion `search_index` rows with partitioning (REQ-STO-015) | SPEC-21 |
| NFR-SCAL-120 | Tenants per enterprise installation (shared isolation) | ≥ 10 000 | SPEC-23 |
| NFR-SCAL-130 | Concurrent protocol sessions per gateway node | ≥ 20 000 (WebSocket/TCP) | SPEC-24 |
| NFR-SCAL-160 | Dispatched events per second per enterprise installation | ≥ 5 000 (16 partitions) | SPEC-20 |

### 3.50 Availability (AVAIL)

| ID | Requirement | Target | Defined in |
|---|---|---|---|
| NFR-AVAIL-001 | Gateway and worker instances are stateless apart from protocol sessions | Losing one gateway drops only its connections | SPEC-10 |
| NFR-AVAIL-050 | Idempotency across runtime-store loss | no duplicate execution within process-log retention (REQ-TX-015 step 6) | SPEC-15 |
| NFR-AVAIL-130 | Session resume after a gateway restart | subscriptions restored from the runtime store; events retained per REQ-PROTO-011 | SPEC-24 |
| NFR-AVAIL-160 | Scheduler failover | new leader within 15 s, no duplicate fires | SPEC-20 |
| NFR-AVAIL-210 | Planned downtime for minor upgrades (SCALED) | 0 (rolling) | SPEC-31 |

### 3.56 Durability (DUR)

| ID | Requirement | Target | Defined in |
|---|---|---|---|
| NFR-DUR-010 | Committed flush survives process crash | PG `synchronous_commit = on`; SQLite WAL + `synchronous = FULL` | SPEC-11 |
| NFR-DUR-011 | Backup/restore point-in-time | PG PITR; SQLite online backup API; runtime store not backed up | SPEC-11 |
| NFR-DUR-050 | No partial process | 0 partial flushes under fault injection | SPEC-15 |
| NFR-DUR-150 | Audit record for every committed process | 100 % (no commit without a record) | SPEC-30 |
| NFR-DUR-160 | Lost events or jobs after crash or runtime-store loss | 0 | SPEC-20 |
| NFR-DUR-210 | Recovery point objective (enterprise with WAL archiving) | ≤ 5 min | SPEC-31 |
| NFR-DUR-211 | Recovery time objective (restore of 100 GB) | ≤ 2 h | SPEC-31 |

### 3.64 Security properties (SEC)

| ID | Requirement | Target | Defined in |
|---|---|---|---|
| NFR-SEC-070 | Logic cannot reach the host | no file, process, network or environment access except through syscalls (engine has no such packages) | SPEC-17 |
| NFR-SEC-110 | Default deny | 100 % of unknown permissions and resources denied | SPEC-22 |
| NFR-SEC-111 | Secret material in logs, traces, audit | none (automated scan in CI scenarios) | SPEC-22 |
| NFR-SEC-120 | Cross-tenant leakage | 0 in the isolation test suite on both editions | SPEC-23 |

### 3.69 Privacy (PRIV)

| ID | Requirement | Target | Defined in |
|---|---|---|---|
| NFR-PRIV-060 | Secrets never in trace, logs or audit | 0 occurrences in a secret-leak scan of traces | SPEC-16 |
| NFR-PRIV-110 | Masking consistent across bindings, queries, traces and AI prompts | the same mask output everywhere | SPEC-22 |
| NFR-PRIV-120 | Tenant purge completeness | no remaining rows, runtime keys or orphan blobs | SPEC-23 |
| NFR-PRIV-150 | Personal data in telemetry | none (IDs and codes only) | SPEC-30 |
| NFR-PRIV-190 | Secret and SECRET-sensitivity data sent to providers | never | SPEC-27 |

### 3.75 Portability (PORT)

| ID | Requirement | Target | Defined in |
|---|---|---|---|
| NFR-PORT-001 | Platforms for the server host | Linux x86_64 and aarch64 | SPEC-10 |
| NFR-PORT-002 | Platforms for the desktop host | Windows, macOS, Linux | SPEC-10 |
| NFR-PORT-010 | Identical snapshot bytes across editions | 100 % (REQ-STO-004) | SPEC-11 |
| NFR-PORT-130 | Same semantics across bindings | the scenario suite is identical on TCP, WS, HTTP and in-process | SPEC-24 |
| NFR-PORT-180 | Supported platforms | evergreen browsers; Windows/macOS/Linux desktop; iOS 16+/Android 10+ | SPEC-26 |
| NFR-PORT-210 | Server binary | static, no system libraries beyond libc (musl) | SPEC-31 |

### 3.82 AI-readiness (AIR)

| ID | Requirement | Target | Defined in |
|---|---|---|---|
| NFR-AIR-001 | URIs are human-readable in prompts and audit | Locator form by default (REQ-URI-007, REQ-URI-012) | SPEC-12 |
| NFR-AIR-010 | Definitions readable by AI | Meta-schema published; `description` recommended on every type and property; `meta.explain` available | SPEC-13 |
| NFR-AIR-070 | ABI is machine-readable | `rt.abi` descriptors with signatures and examples; used in AI prompts (SPEC-27) | SPEC-17 |
| NFR-AIR-090 | Actions, pipelines and workflows are complete descriptions | AI can generate a feature as a package of definitions + logic (VRD-22-02, SPEC-27) | SPEC-19 |
| NFR-AIR-140 | Packages are complete, text-based feature descriptions | manifests, entity JSON, `.rhai`, scenarios, README; the AI builder emits this format (SPEC-27) | SPEC-28 |
| NFR-AIR-190 | Builder success on the reference requirement set (P1 exit) | ≥ 70 % valid packages within 3 repair rounds | SPEC-27 |
| NFR-AIR-200 | Every ontology type and property has a `description` | 100 % (builder RAG, REQ-AI-031) | SPEC-29 |

### 3.90 Operability (OPS)

| ID | Requirement | Target | Defined in |
|---|---|---|---|
| NFR-OPS-001 | Start to ready with warm caches (root tenant only) | ≤ 10 s enterprise, ≤ 3 s personal | SPEC-10 |
| NFR-OPS-060 | Trace overhead at SUMMARY level | ≤ 5 % of process time | SPEC-16 |
| NFR-OPS-140 | Hot reload latency (file save → visible on draft) | ≤ 2 s | SPEC-28 |
| NFR-OPS-150 | Telemetry overhead at default sampling | ≤ 3 % CPU | SPEC-30 |
| NFR-OPS-190 | AI cost visibility | tokens and cost per tenant, task and principal available daily | SPEC-27 |
| NFR-OPS-210 | Time to a running SINGLE_NODE from the compose file | ≤ 5 min on a clean machine | SPEC-31 |

### 3.97 User experience (UX)

| ID | Requirement | Target | Defined in |
|---|---|---|---|
| NFR-UX-080 | All validation errors in one response | 100 % of independent failures reported together | SPEC-18 |
| NFR-UX-170 | Time to first interactive form in the web shell after a click | ≤ 300 ms on the reference network | SPEC-25 |
| NFR-UX-171 | Accessibility | WCAG 2.1 AA for standard widgets | SPEC-25 |
| NFR-UX-180 | Web cockpit cold start to READY (cached assets) | ≤ 2 s | SPEC-26 |
| NFR-UX-181 | Omnibar results after typing pause | ≤ 150 ms | SPEC-26 |
| NFR-UX-182 | Dock update after server event | ≤ 1 s | SPEC-26 |
| NFR-UX-183 | Keyboard-only operation of cockpit core flows | 100 % (omnibar, automator, dock) | SPEC-26 |
| NFR-UX-200 | Every ontology type renders with derived layouts and sensible widgets | 100 % without custom Views | SPEC-29 |


---

## 4. Verification harness

### REQ-NFR-001 — Benchmark suite
- **Statement:** The implementation MUST provide:
  - micro-benchmarks for every NFR marked µs or ms in §3 (Rust `criterion`);
  - a load harness that replays OLTP-1 against any binding;
  - a data-set generator for M and L.

  All three run in CI on every release candidate, on the reference environment (§1.1), and publish the results with the release notes.
- **Origin:** VRD-22-03, CON-UC-035
- **Acceptance:** The release pipeline shows each NFR with its measured value and PASS/FAIL.
- **Priority:** P1

### REQ-NFR-002 — Chaos and durability tests
- **Statement:** A test suite MUST run in CI and inject the following faults during load:
  - kill -9 of gateways, workers, dispatchers and the scheduler;
  - runtime-store flush or restart;
  - database failover (enterprise);
  - network partitions between roles.

  After each fault, the suite verifies NFR-DUR-900/901 and NFR-AVAIL-901 with the consistency checks of REQ-DEP-030.
- **Origin:** ADR-004, REQ-STO-011, REQ-EVT-026
- **Acceptance:** The suite passes for 10 consecutive nightly runs before a release.
- **Priority:** P1

### REQ-NFR-003 — Regression budget
- **Statement:** A measured regression of more than 10 % against the previous release on any P0-chapter NFR MUST block the release, unless an ADR accepts it with a reason.
- **Origin:** NEW: keeps the targets meaningful
- **Acceptance:** A deliberately slowed flush path fails the gate.
- **Priority:** P2

### REQ-NFR-004 — Catalogue integrity
- **Statement:** NFR IDs are unique across the specification (REQ-CONV-004). Every chapter NFR appears in §3, and every §3 row points to an existing chapter item. A documentation check (P5 cross-reference validation) enforces this.
- **Origin:** VRD-22-04, SPEC-00 §6
- **Acceptance:** The P5 validation reports 0 missing or dangling NFR IDs.
- **Priority:** P1

## 5. Acceptance

| REQ | Criterion |
|---|---|
| REQ-NFR-001 | Benchmarks and load harness produce a release report |
| REQ-NFR-002 | Chaos suite green for 10 nights |
| REQ-NFR-003 | Regression gate blocks a slowed build |
| REQ-NFR-004 | 0 missing or dangling NFR IDs |

## 6. Open questions

None. Targets are initial engineering goals derived from the corpus scale notes and the stack (ADR-001). They are to be confirmed by the first benchmark run and, if changed, recorded by ADR.
