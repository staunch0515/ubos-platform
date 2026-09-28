---
id: CMP-20
title: "Comparison: Technology Stack (input to ADR-001)"
status: complete
phase: P2
depends_on: [CMP-01, CMP-05, CMP-15, CMP-19]
sources: [UP, FU, LC, UB, UC, US, LS, UW]
---

# Comparison: Technology Stack

## 1. Question

Which implementation stack should the unified platform use? The options are Java, Rust or a hybrid. The choice covers the kernel language, script language, database, cache and queue, transport and front end. The user delegated this recommendation (Q-001). The final decision is ADR-001 in P3.

## 2. Candidates

| Stack | Repos | Kernel | Scripts | DB | Other |
|---|---|---|---|---|---|
| **J** Java 17 + Spring Boot WebFlux (reactive) + R2DBC | UP, FU, LC, UB | JVM reactive | Groovy 4 (UP/FU/UB); none (LC handlers) | PostgreSQL 15 (+ pgvector in UB) | Flyway, Caffeine, networknt JSON Schema, JJWT |
| **R** Rust + Tokio + axum + sqlx | UC, US, LS | native async | Rhai 1.16–1.23 | PostgreSQL 16 + pgvector; SQLite (LS) | Redis 7, clap CLI, tokio-util codec |
| **F** Front end | all with UI | React 18/19 + TypeScript + Vite | – | – | zustand / RTK Query, Monaco, cmdk, antd/shadcn/Tailwind, framer-motion |

## 3. Dimension matrix

| Dimension | J (Java/Groovy) | R (Rust/Rhai) |
|---|---|---|
| Fit with "one core, five clients, cross-platform, zero-copy, own binary protocol" | ◐ JVM on desktop/edge is heavy | ● native binaries, embeddable (Tauri, CLI, edge) |
| Embedded edition (SQLite, desktop) | ◐ possible, large footprint | ● proven (LS) |
| Script sandbox | ◐ requires AST restrictions + interrupts (UP) | ● Rhai: no host access by default, operation/depth/size limits |
| Script language power / familiarity | ● Groovy (Java ecosystem) | ◐ Rhai (small, JS-like) |
| Ecosystem for enterprise concerns (JSON Schema, JWT, mail, IAM) | ● mature | ● adequate (jsonschema, jsonwebtoken, lettre, etc.) |
| Reactive complexity | ◐ Reactor adds cognitive load | ◐ async Rust + sync scripts need bridging (UC) |
| Performance, memory, latency | ◐ | ● |
| Amount of concept coverage in the corpus | ● FU governance, UB merge, LC pipelines | ● UC jobs/anchors, US session/FSM, LS protocol/slots/editions |
| AI code generation quality | ● | ● (AI already wrote UC/US/LS) |
| Operational maturity for Web backends | ● Spring | ● axum/Tokio |

## 4. Analysis

- **What the Java repositories contribute.** They carry the richest *governance and versioning features* (FU, UB) and the pipeline engine (LC). These are designs over the same six-table model and port to any language.
- **What the Rust repositories contribute.**
  - They carry the *runtime characteristics* the owner states as goals: embeddability, editions, a binary protocol and native performance.
  - They also carry the sandbox-friendly script engine, and they are where the syscall ABI, jobs, anchors, sessions and protocol were built.
- **Scripts.** Rhai's model (a registered function surface only, with operation limits) matches VRD-05-02/03 directly. Groovy needs defensive engineering to reach the same safety.
- **Front end.** The front end is uncontested: every UI repository uses React + TypeScript + Vite.

## 5. Verdicts

### VRD-20-01 — Recommended kernel stack: Rust (fusion of R; recommendation for ADR-001)
- **Decision:** fusion of the Rust repositories; recommendation for ADR-001.
- **Stack:**
  - Rust (stable, edition 2021+), Tokio, axum (HTTP/WebSocket bindings), tokio-util codec (TCP binding), sqlx (PostgreSQL + SQLite backends behind store traits), serde.
  - Workspace layout as in VRD-19-01.
- **Rationale:** stated platform goals, the embedded edition, safer scripting, and the corpus's most recent kernel mechanisms.
- **Consequences:** Features designed in the Java repositories (FU governance, UB merge/diff, LC pipelines and constraints) are re-specified language-neutrally in P4 and implemented in Rust.

### VRD-20-02 — Script language: Rhai (best-of R), language field kept open (VRD-05-01)
- **Decision:** best-of R.
- **Consequences:** Rhai with the `sync` and `serde` features; engine per worker; registered syscall namespaces; operation, depth, size and time limits. Other languages (e.g. WASM modules) may be added later through the `language` field.

### VRD-20-03 — Data and runtime stores (fusion)
- **Decision:** fusion.
- **Consequences:** PostgreSQL 16 + pgvector (enterprise); SQLite (personal); Redis 7 (enterprise runtime store: queue, cache, sessions, idempotency); in-process equivalents in the personal edition.

### VRD-20-04 — Front end: React + TypeScript + Vite (consensus)
- **Decision:** consensus.
- **Consequences:**
  - State: zustand for UI state, plus a protocol client (SDK) for server state.
  - Editor: Monaco with Rhai language support.
  - Command palette: cmdk.
  - Styling: Tailwind + one component library (choice in P3 conventions).
  - Desktop: Tauri, wrapping the same web shell with an embedded kernel.

### VRD-20-05 — Protocol encoding: JSON first, binary negotiated (→ VRD-15-02)
- **Decision:** as in VRD-15-02.
