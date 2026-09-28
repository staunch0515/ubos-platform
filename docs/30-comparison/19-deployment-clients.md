---
id: CMP-19
title: "Comparison: Deployment Topology and Clients"
status: complete
phase: P2
depends_on: [CMP-01, CMP-15, CMP-14]
sources: [UP, FU, LC, UB, UC, US, LS, UW]
---

# Comparison: Deployment Topology and Clients

## 1. Question

How is the platform packaged and deployed, from a single laptop to an enterprise cluster? How do the "five clients" share one kernel?

## 2. Candidates

| KEY | Approach | Refs | Summary |
|---|---|---|---|
| UP | Triad: PostgreSQL + kernel + AI copilot (Docker Compose); one API, several shells | CON-UP-004, 005 | AI as deployed client |
| FU | Kernel + Studio; localhost console | CON-FU-002, 003 | Operator deployment |
| LC | Backend + Vercel-hosted frontends | inventory | Cloud frontends |
| UB | Instance entity; network boot with signed permits; OS-ification blueprint (design) | CON-UB-006, 033 | Node fleet design |
| UC | Single binary (API + worker + scheduler + event bus) + pgvector + Redis (Compose) | CON-UC-001, 003 | Monolith with async split |
| US | Cargo workspace: kernel library reused by server and CLI; desktop (Tauri) and client SDK planned; web | CON-US-001, 064, HL-US-009 | "One core, many clients" in code |
| LS | Personal (SQLite) / enterprise (PostgreSQL) editions; TCP server | CON-LS-006, 062, HL-LS-005 | Editions |
| UW | Static frontend | – | – |

## 3. Dimension matrix

| Dimension | UP | UC | US | LS |
|---|---|---|---|---|
| Kernel as embeddable library | – | – | ● | ● |
| Editions (personal/edge vs enterprise) | – | – | – | ● |
| Async workers separable | – | ● (same binary) | ● | – |
| Native/desktop client | – | – | planned | TCP clients |
| AI service as separate client | ● | – | – | – |
| Container deployment | ● | ● | – | – |

## 4. Analysis

- **"One core, five clients"** is stated in UC, US and LS but not defined. From the corpus, the five clients can be read as:
  - (1) web browser shell;
  - (2) desktop app (Tauri, planned in US);
  - (3) CLI/terminal (US CLI, FU terminal);
  - (4) IDE/VS Code (US design);
  - (5) programmatic SDK/agents (US SDK stub, UP copilot, UC Ambassador).
  - Mobile is implied by "cross-platform". *Interpretation:* the list must be confirmed (Q-008).
- **Kernel as a library.** US and LS show it can be embedded, which enables a desktop app with a local SQLite edition that syncs with a server by branches (UP/FU semantics).
- **Scale-out.** UC separates stateless API nodes from workers through a queue, which is the basis for horizontal scaling.

## 5. Verdicts

### VRD-19-01 — Kernel is a library; hosts are thin (best-of US + LS)
- **Decision:** best-of US and LS.
- **Consequences:** Crates or modules: `proto` (messages and types), `store` (traits + backends), `kernel` (semantic, runtime, syscalls, rules), `hosts` (server, worker, CLI, desktop), `sdk` (client libraries).

### VRD-19-02 — Editions (best-of LS)
- **Decision:** best-of LS.
- **Consequences:**
  - **Personal/edge:** embedded kernel + SQLite + in-process queue and cache, single user or tenant, optional sync.
  - **Enterprise:** server + workers + PostgreSQL/pgvector + Redis, multi-tenant, RLS.
  - Same packages and protocol in both.

### VRD-19-03 — Topology roles (fusion UC + UP)
- **Decision:** fusion.
- **Consequences:** Roles:
  - gateway/API (protocol bindings);
  - worker pool (jobs, events, schedules);
  - database;
  - runtime store;
  - optional AI provider (external).
- All roles can run in one process (dev) or be scaled independently.

### VRD-19-04 — The five clients (NEW, confirm)
- **Decision:** NEW, pending confirmation.
- **Consequences:** Web shell, desktop app, CLI/terminal, IDE extension, SDK/agent. All use UBTP (VRD-15-01).
- **Open:** Q-008.

### VRD-19-05 — Offline/edge sync by branches (NEW, later version)
- **Decision:** NEW; deferred.
- **Consequences:** Deferred: personal editions sync with a server as branch push/pull using merge semantics (VRD-08-04).

## 6. Open questions

- Q-008: Confirm the "five clients" list (web, desktop, CLI, IDE, SDK/agent; or mobile instead of IDE?).
