---
id: UBS-ARC-01
title: The System Family
status: draft
phase: ALL
depends_on: [UBS-ARC-00, UBS-REQ-01, UBS-REQ-03]
---

# The System Family

## 1. Layered view

```
┌──────────────────────────────── Experience layer ────────────────────────────────┐
│  WSP Workspace (web, desktop, mobile, portal)     STU Studio (builder)            │
├──────────────────────────────── Intelligence layer ──────────────────────────────┤
│  AGT Agent Hub (model gateway, agents, tools/MCP, copilot, builder, evolution)    │
├──────────────────────────────── Platform services layer ─────────────────────────┤
│  EXC Buk Exchange   BRG Bridge (integration, analytics exit)   NTY Notary         │
│  FED Federation     CTL Control Plane (fleet, tenants, billing)                   │
├──────────────────────────────── Host layer ──────────────────────────────────────┤
│  NOD UBOS Node: Box | Server | Cell node | Worker        SDK client libraries      │
├──────────────────────────────── Engine layer ────────────────────────────────────┤
│  DVM reference kernel (store, model, versions, time, ledger, txn, runtime, rules, │
│      authorization, proofs)                                                        │
├──────────────────────────────── Standard layer ──────────────────────────────────┤
│  BPA standard + conformance suite          FRG Logic Forge (toolchain)            │
└───────────────────────────────────────────────────────────────────────────────────┘
```

The diagram in table form (lower layers never depend on higher layers):

| Layer | Systems | Depends on |
|---|---|---|
| Standard | BPA, FRG | — (FRG embeds the DVM as a library for checks and simulation) |
| Engine | DVM | BPA (implements it) |
| Host | NOD, SDK | DVM |
| Platform services | EXC, BRG, NTY, FED, CTL | NOD (through UBTP and admin contracts), DVM (as a library where stated) |
| Intelligence | AGT | NOD, DVM instructions |
| Experience | WSP, STU | SDK only |

## 2. Architecture rules for layering

### AR-001 — Downward-only dependencies
- **Rule:** A system MUST depend only on systems in the same or lower layers. Upward communication happens only through events or subscriptions defined as contracts.
- **Realises:** NR-MAINT-003
- **Rationale:** Keeps the kernel independent of products (EXT-TRI "less is more").

### AR-002 — The kernel is a library
- **Rule:** The DVM MUST be delivered as an embeddable library with no network listeners, no user interface and no business rules. Hosts (NOD, FRG, the browser kernel) provide I/O, scheduling and transport.
- **Realises:** FR-STD-061, FR-SYNC-081, CST-005
- **Rationale:** One engine for every edition and for third-party embedding.

### AR-003 — Clients speak only through the SDK
- **Rule:** WSP, STU and every external client MUST reach NOD only through the SDK over UBTP, or through the generated REST facade (which is itself UBTP-backed). No client reads storage directly.
- **Realises:** CST-006, FR-UX-011
- **Rationale:** One enforcement path for authorization and validation.

### AR-004 — Only the commit path writes authoritative state
- **Rule:** Authoritative state MUST be written only by the DVM commit path inside a process. Services (BRG, AGT, NTY, FED, CTL) change business state only by invoking ports or instructions through NOD.
- **Realises:** CST-003, FR-TXN-011
- **Rationale:** Single source of truth and complete audit.

### AR-005 — Platform services hold no business truth
- **Rule:** EXC, BRG, NTY, FED, AGT and CTL MAY keep their own operational state (registries, delivery logs, receipts), but MUST NOT hold a second authoritative copy of tenant business data. Anything they must retain for evidence is committed back into the tenant's VAE (for example anchor receipts as ledger entries).
- **Realises:** FR-PROOF-042, CST-017
- **Rationale:** No drift between truth and service copies.

### AR-006 — The Control Plane is blind to business data
- **Rule:** CTL credentials MUST be unable to call business ports, read objects or decrypt tenant data keys. CTL manages placement, versions, licences, health and metering only.
- **Realises:** FR-OPS-081, CST-017
- **Rationale:** Operator trust boundary.

## 3. The thirteen systems

| Code | System | Layer | Role | Owns | Does not own | Introduced |
|---|---|---|---|---|---|---|
| BPA | BPA standard and conformance suite | Standard | The instruction-set manual of business processing | normative clauses, grammars, schemas, conformance vectors, profiles, certification rules | any implementation | PH-0 |
| FRG | Logic Forge | Standard | Toolchain: check, type, test, simulate, pack, verify, publish, run conformance | CLI, language server, analyser, simulator, packer, conformance runner | runtime hosting of production VAEs | PH-0 (runner), PH-1 (CLI) |
| DVM | DVM reference kernel | Engine | Executes the BPA: store, model, versions, bitemporal time, ledgers, transactions, runtime, rules, authorization decisions, proofs | all authoritative semantics | transport, UI, scheduling threads, secrets storage | PH-1 |
| NOD | UBOS Node | Host | Hosts the DVM: UBTP endpoints, jobs and schedules, connectors, secrets, telemetry, sync endpoints, editions and roles | process lifecycle, I/O, runtime store, node configuration | business rules, tenant placement decisions (CTL) | PH-1 |
| SDK | Client SDKs | Host | Typed clients for UBTP (TypeScript, Rust; later Python, Java) | client protocol handling, local caches, offline queue (with the browser kernel) | server semantics | PH-1 |
| WSP | Workspace | Experience | End-user product: shell, forms, lists, history, review, Live Doc, Smart Grid, War Room, Action Messages, portal | renderers, client state | business rules | PH-2 |
| STU | Studio | Experience | Builder product: modelling, sheets, lifecycles, views, change-set review, simulations, Buk authoring | builder UX | runtime semantics (delegated to DVM and FRG) | PH-2 |
| AGT | Agent Hub | Intelligence | Model gateway, agent principals and runs, tools and MCP, copilot, builder, evaluation, evolution analysers | agent definitions, run orchestration, model routing, evaluation stores | merge authority, business truth | PH-3 |
| NTY | Notary | Platform services | Anchoring, evidence packs, examiner views, verifier | anchor schedule, TSA integration, pack builder, open verifier | records themselves | PH-3 |
| BRG | Bridge | Platform services | Connectors runtime, file adapters, imports and mappings, CDC export, analytics engine | adapter runtime, export watermarks, analytical datasets | authoritative records | PH-2 |
| EXC | Buk Exchange | Platform services | Registry of Buks, verification badges, licences, fiat billing and payouts for publishers | registry, licence issuance, payout ledgers | customer business data | PH-4 |
| CTL | Control Plane | Platform services | Fleet, cells, tenant lifecycle and placement, upgrade waves, metering, plans, billing | fleet state, tenant directory, usage ledgers | tenant business data | PH-4 (basic tenant admin console from PH-2 inside NOD) |
| FED | Federation | Platform services | VAE registry, DID resolution, trust relationships, cross-node transport, shared BPUs | registry, trust objects (with VAEs), relay | business truth of either party | PH-5 |

## 4. Responsibility boundaries that are often confused

| Topic | Owner | Not the owner | Reason |
|---|---|---|---|
| Deciding whether an action is allowed | DVM (policy decision) | NOD, WSP | The decision belongs with the state it protects (AR-004). |
| Enforcing transport authentication | NOD | DVM | The DVM receives an authenticated context. |
| Holding secret values | NOD (secret store) | DVM, AGT, logic | FR-IAM-091. |
| Running schedules and jobs | NOD | DVM | The DVM defines schedule and job objects; NOD owns clocks and threads. |
| Deciding merge results | DVM | STU, AGT | STU and AGT present and request; the DVM decides. |
| Choosing a model provider | AGT (gateway) | DVM | `ask.model` is delegated by NOD to AGT. |
| Placing tenants on cells | CTL | NOD | Fleet-wide view. |
| Running conformance vectors | FRG (runner) | BPA | BPA defines, FRG executes. |
| Verifying proofs for third parties | NTY verifier (open source) | DVM | Independence from the engine. |
| Producing view payloads | DVM (view resolution) + NOD (delivery) | WSP | Clients render only. |

## 5. Editions as compositions of systems

| Edition (DEC-016) | Composition |
|---|---|
| Box | NOD(Box) embedding DVM + WSP desktop/mobile shell + optional FRG |
| Server | NOD(Server) cluster embedding DVM + WSP web + STU + BRG + optional AGT and NTY (from PH-3) |
| Managed Server | Server operated by UBOS for one tenant (from PH-3) |
| Cell | NOD(Cell) nodes + CTL + BRG + AGT + NTY shared services (PH-4) |
| Worker | NOD(Worker role) with DVM runtime only, no authoritative store (PH-4) |
| Browser kernel | DVM compiled to WASM inside WSP web + SDK (PH-5) |
| Federation participant | Any Server or Cell with FED enabled (PH-5) |

## 6. Phase introduction of systems

| Phase | New systems or major extensions |
|---|---|
| PH-0 | BPA (v0 draft, formal models, vectors), FRG (conformance runner) |
| PH-1a | DVM core, NOD in-process host and Box headless, FRG CLI basics |
| PH-1b | NOD Server with UBTP, SDK (TypeScript, Rust), DVM governance (rules, flow, events, IAM) |
| PH-2 | WSP web, STU, BRG (connectors, imports), first vertical Buks, NOD HA and DR |
| PH-3 | AGT, NTY, WSP Office suite (grid, war room, messages), desktop and mobile, SYNC (Box–Server), L3 WASM |
| PH-4 | CTL, Cell, EXC, Worker role, certification programme, tiering |
| PH-5 | FED, cross-organisation shared BPUs, browser kernel |
