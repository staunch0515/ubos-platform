---
id: UBS-DEC-000
title: Decision Log
status: complete
phase: ALL
depends_on: [UBS-META-00, UBS-META-06]
---

# Decision Log

Decisions are never deleted. A replaced decision is marked
`Superseded by DEC-<NNN>`.

| ID | Title | Status |
|---|---|---|
| DEC-001 | Location and title of the volume | Accepted |
| DEC-002 | The enterprise-internal worker pool is kept; the public compute grid is excluded | Accepted |
| DEC-003 | US-first compliance, GDPR considered | Accepted |
| DEC-004 | First vertical: fund and asset-management back office plus contract management | Accepted |
| DEC-005 | Six phases, PH-0 … PH-5 | Accepted |
| DEC-006 | Reference durations with an explicit team assumption; phases gated by exit criteria | Accepted |
| DEC-007 | Technology stack of the reference implementation | Accepted |
| DEC-008 | The earlier volume `40-spec` is kept as history | Accepted |
| DEC-009 | Report per batch, in Chinese | Accepted |
| DEC-010 | Writing order, with a confirmation stop after the requirements volume | Accepted |
| DEC-011 | The "six tables only" rule is withdrawn | Accepted |
| DEC-012 | Long-term economic and social narratives are excluded | Accepted |
| DEC-013 | The system decomposition (thirteen systems) | Accepted |
| DEC-014 | PH-1 has two exit gates: PH-1a (engine core) and PH-1b (governance and services) | Accepted |
| DEC-015 | Both first-vertical Buks stay in PH-2 | Accepted |
| DEC-016 | The first commercial form is Server (on-premises and private cloud); hosted Cell follows in PH-4 | Accepted |
| DEC-017 | Architecture-level ID kinds AR (architecture rule) and CTR (inter-system contract) | Accepted |
| DEC-018 | Repository layout and reuse of the research repositories | Accepted |
| DEC-019 | Licensing of the standard, reference implementation and tools | Accepted |
| DEC-020 | Storage engine design: PostgreSQL/SQLite with kernel indexes and prolly trees | Accepted |
| DEC-021 | Policy engine: Cedar-class analysable policies plus relationship tuples | Accepted |

---

### DEC-001 — Location and title of the volume
- **Status:** Accepted · **Date:** 2026-09-29
- **Context:** A new, self-contained specification is needed. It must stay separate from the research volume and the earlier specification.
- **Decision:** The volume lives in `ubos-platform/docs/50-system/` on branch `claude/gifted-edison-cmqbu9`. Its title is *UBOS Business Processing Architecture — System Specification*.
- **Consequences:** `docs/README.md` points to this volume as the current authority. New ID namespaces avoid collisions with `40-spec`.
- **Alternatives considered:** Rewriting `40-spec` in place was rejected because it would lose the history. A new repository was rejected because it would split the documentation.
- **Affects:** UBS-META-00, UBS-META-01

### DEC-002 — The enterprise-internal worker pool is kept; the public compute grid is excluded
- **Status:** Accepted · **Date:** 2026-09-29
- **Context:** The input documents describe a public compute grid paid in tokens. The product owner excluded "compute as currency". Sandboxed batch and AI work on an organisation's own machines has independent value.
- **Decision:** A **Worker role** of the UBOS Node runs sandboxed, verified WASM jobs on machines the organisation owns or controls. It has no payment, token or public participation. It is delivered in PH-4.
- **Consequences:** The `EVT` domain covers worker registration, placement, result verification and data-classification limits.
- **Alternatives considered:** Dropping distributed execution entirely was rejected because it gives up useful batch capacity. A public grid was rejected by DEC-012.
- **Affects:** FR-EVT, UBS-SYS-NOD, PH-4

### DEC-003 — US-first compliance, GDPR considered
- **Status:** Accepted · **Date:** 2026-09-29
- **Context:** The positioning targets US enterprises. The first vertical is regulated financial services.
- **Decision:** The compliance catalogue targets the following, in priority order:
  - SOC 2 (Security, Availability, Confidentiality, Processing Integrity);
  - SEC Rule 17a-4 and Rule 204-2 recordkeeping;
  - FINRA Rule 4511;
  - SOX IT general controls;
  - HIPAA (for a later healthcare vertical);
  - CCPA/CPRA;
  - E-SIGN/UETA;
  - WCAG 2.2 AA and Section 508.

  GDPR is designed for from the start, including erasure through crypto-shredding. NIST SP 800-53 is the control vocabulary. China-specific localisation (Xinchuang) is not a target.
- **Consequences:** Crypto-shredding, legal hold, WORM retention and segregation of duties become Must requirements.
- **Alternatives considered:** A global-neutral catalogue was rejected as too vague to verify.
- **Affects:** UBS-REQ-08, NR-SEC, NR-PRIV

### DEC-004 — First vertical: fund and asset-management back office plus contract management
- **Status:** Accepted · **Date:** 2026-09-29
- **Context:** The architecture needs a flagship "whole product" to prove its value. Regulated finance values immutability, bitemporality and proofs most. One of the source repositories came from the fund domain.
- **Decision:** The first vertical Buk set is:
  1. Fund Operations: fund and share-class master data; subscriptions and redemptions; NAV and fee calculation with bitemporal corrections; investor register; compliance rules.
  2. Contract Management: authoring, negotiation, approval, signature, obligations, amendments.

  Both ship in PH-2 and are hardened in PH-3.
- **Consequences:** Scenarios SCN-1xx (fund operations) and SCN-2xx (contracts) are reference scenarios for verification.
- **Alternatives considered:** Generic CRM was rejected as weakly differentiated. Healthcare was rejected because of heavier regulation for a first pilot.
- **Affects:** UBS-REQ-04, PH-2, PH-3

### DEC-005 — Six phases, PH-0 … PH-5
- **Status:** Accepted · **Date:** 2026-09-29
- **Decision:** The phases are:
  - PH-0 Foundation;
  - PH-1 Kernel;
  - PH-2 Application;
  - PH-3 Trust and AI;
  - PH-4 Platform and Ecosystem;
  - PH-5 Federation.

  Each phase has a verification plan and binary exit criteria.
- **Consequences:** Every requirement carries exactly one phase.
- **Alternatives considered:** Three coarse phases were rejected because verification gates would be too far apart.
- **Affects:** `50-phases/`, every FR/NR/CR

### DEC-006 — Reference durations with an explicit team assumption; phases gated by exit criteria
- **Status:** Accepted · **Date:** 2026-09-29
- **Decision:** Each phase states a non-normative reference duration. The assumption is a core team of 8–12 engineers, growing to 15–20 by PH-4. A phase ends only when every exit criterion is evidenced, not on a date.
- **Affects:** `50-phases/`

### DEC-007 — Technology stack of the reference implementation
- **Status:** Accepted · **Date:** 2026-09-29
- **Decision:**
  - **Kernel and servers:** Rust stable with Tokio, axum and sqlx.
  - **Storage:** PostgreSQL 16+ (Server, Cell) and SQLite (Box, browser, mobile).
  - **Runtime store:** Redis-compatible, non-authoritative.
  - **Object storage:** S3-compatible.
  - **Logic:** Rhai for L2 scripts; a WASM component-model runtime (wasmtime class) for L3.
  - **Clients:** TypeScript and React; Tauri 2 for desktop and mobile; kernel compiled to WASM for local-first web.
  - **Policy:** a Cedar-class policy engine.
  - **Formal models:** TLA+.

  The BPA standard remains language-neutral.
- **Alternatives considered:** Java/JVM was rejected for the reference implementation. The reasons are the embedded, mobile and WASM targets, sandbox control and determinism. Java implementations remain possible as third-party BPA implementations.
- **Affects:** all systems

### DEC-008 — The earlier volume `40-spec` is kept as history
- **Status:** Accepted · **Date:** 2026-09-29
- **Decision:** `docs/40-spec/` and the research folders stay unchanged. This volume cites them only as `L40:<ID>` in Origin fields.
- **Affects:** UBS-META-01 §6

### DEC-009 — Report per batch, in Chinese
- **Status:** Accepted · **Date:** 2026-09-29
- **Decision:** Writing proceeds in batches of about 5–10 files. After each batch the AI validates, commits, pushes and reports to the product owner in Chinese.
- **Affects:** UBS-META-03

### DEC-010 — Writing order, with a confirmation stop after the requirements volume
- **Status:** Accepted · **Date:** 2026-09-29
- **Decision:** The order is: meta → requirements → (**stop for confirmation**) → architecture → standard → systems → phases → verification → indexes.
- **Affects:** UBS-META-03

### DEC-011 — The "six tables only" rule is withdrawn
- **Status:** Accepted · **Date:** 2026-09-29
- **Context:** The earlier rule allowed exactly six authoritative tables. It kept the kernel simple, but it forced ledger state, bitemporal indexes, content-addressed objects, job state and sync state into shapes that do not fit them.
- **Decision:**
  1. The kernel MAY define any number of **system tables**. Each is documented with its purpose, owner system, write path and retention.
  2. **Business classes** do not create DDL by default. They remain metadata stored as versioned objects.
  3. A class MAY declare **typed projection tables** for performance or reporting. Projections are non-authoritative and can always be rebuilt from authoritative state.
  4. Authoritative state is written only by the commit path.
- **Consequences:** The storage design in `UBS-SYS-DVM` lists all tables. The conformance suite checks that projections can be rebuilt.
- **Alternatives considered:** Keeping six tables was rejected (product owner instruction). Per-class physical tables for all classes were rejected because they break metadata-driven evolution.
- **Affects:** FR-ANL, UBS-SYS-DVM, UBS-SYS-NOD

### DEC-012 — Long-term economic and social narratives are excluded
- **Status:** Accepted · **Date:** 2026-09-29
- **Decision:** The following are excluded:
  - "the end of the corporation";
  - the intent economy and reverse consumer marketplace;
  - rebate advertising;
  - compute as currency, tokens and the public compute grid.

  Fiat licensing and billing remain in scope.
- **Affects:** UBS-META-00 §3.2

### DEC-013 — The system decomposition (thirteen systems)
- **Status:** Accepted · **Date:** 2026-09-29
- **Decision:** The system codes are BPA, DVM, FRG, NOD, CTL, WSP, STU, AGT, EXC, FED, NTY, BRG and SDK, as defined in `UBS-META-06` §5.
- **Affects:** `20-architecture/`, `40-systems/`

### DEC-014 — PH-1 has two exit gates: PH-1a (engine core) and PH-1b (governance and services)
- **Status:** Accepted · **Date:** 2026-09-29
- **Context:** PH-1 holds 341 requirements (OQ-001). A single exit gate would come too late to catch kernel design errors.
- **Decision:** PH-1 keeps its phase code in requirement blocks, but has two sequential exit gates with their own verification plans and exit criteria (`EXIT-1-*` items are tagged `gate: 1a` or `gate: 1b`). The gate of a PH-1 requirement is determined by its domain, with the listed exceptions:

  | Gate | Domains | Exceptions moved into this gate |
  |---|---|---|
  | **PH-1a Engine core** | MODEL, VER, TIME, LEDG, TXN, LOGIC, PROOF, FILE, ANL, STD, DEV (except CAP-DEV-06) | CAP-IAM-03 (principals and keys), CAP-IAM-09 (secrets and keys), CAP-TEN-01 and CAP-TEN-02 (tenants, VAEs, isolation), CAP-OPS-01 and CAP-OPS-02 (bootstrap, backup), CAP-PKG-07 (genesis), CAP-LOC-03 (time zones) |
  | **PH-1b Governance and services** | RULE, FLOW, EVT, QRY, IAM, TEN, AUD, INT, PKG, OPS, LOC, SYNC, BILL, and CAP-DEV-06 (SDKs) | — |

  PH-1a is verified through the in-process binding and the conformance runner. PH-1b adds UBTP bindings and SDKs.
- **Consequences:** The phase plan (`50-phases/PH-1`) has two gates. Requirements depend only on the same or an earlier gate. A PH-1a requirement that needs a PH-1b capability is a defect to be reported as an open question.
- **Alternatives considered:** Renumbering all phases (PH-1 … PH-6) was rejected because it would churn 960 requirement IDs' phase fields and the scenario map. Keeping one gate was rejected (OQ-001).
- **Affects:** UBS-PH-1, 60-verification, UBS-REQ-04 (PH-1 scenarios are assigned to gates in the phase plan)

### DEC-015 — Both first-vertical Buks stay in PH-2
- **Status:** Accepted · **Date:** 2026-09-29
- **Context:** OQ-002 asked whether to defer one vertical.
- **Decision:** Fund Operations and Contract Management both ship in PH-2. Contract Management is lighter, and it is the proving ground for Live Doc, templates and electronic signatures. Fund Operations proves bitemporality, ledgers and restatement.
- **Consequences:** The PH-2 verification plan includes both scenario families (SCN-1xx and SCN-2xx).
- **Alternatives considered:** Deferring contracts was rejected because Office and signature capabilities would lack a real-world driver in PH-2.
- **Affects:** UBS-PH-2, DEC-004

### DEC-016 — The first commercial form is Server (on-premises and private cloud); hosted Cell follows in PH-4
- **Status:** Accepted · **Date:** 2026-09-29
- **Context:** OQ-003. Regulated first customers prefer controlled deployments, and a multi-tenant hosted service needs the Control Plane (PH-4).
- **Decision:** The first sellable product is the Server edition, deployed by the customer or a partner in their data centre or private cloud, with UBOS-provided support. A single-tenant managed Server operated by UBOS for design partners is allowed from PH-3. Multi-tenant Cells start in PH-4.
- **Consequences:** Operability (NR-OPER-*), air-gapped installation (FR-OPS-013) and offline licences (FR-PKG-083) gain importance. SOC 2 scope in PH-4 covers the Cell service.
- **Alternatives considered:** SaaS-first was rejected because it needs the Control Plane and SOC 2 earlier.
- **Affects:** UBS-REQ-01 §6, UBS-PH-2, UBS-PH-3, UBS-PH-4

### DEC-017 — Architecture-level ID kinds AR (architecture rule) and CTR (inter-system contract)
- **Status:** Accepted · **Date:** 2026-09-29
- **Context:** The architecture volume states cross-system rules and contracts that are not system-internal design requirements.
- **Decision:** Two ID kinds are added:
  - `AR-<NNN>`: a normative architecture rule, defined in `20-architecture`;
  - `CTR-<NNN>`: a contract between two systems (caller, callee, protocol, guarantees), defined in `20-architecture`. Its operations are detailed as `IF-*` items in the system chapters.
- **Affects:** UBS-META-01, tools/check_system.py

### DEC-018 — Repository layout and reuse of the research repositories
- **Status:** Accepted · **Date:** 2026-09-29
- **Context:** The eight research repositories contain useful code but diverge from the specification.
- **Decision:** The reference implementation lives in a new monorepo (`ubos`), with separate repositories for the standard (`bpa-standard`), the open verifier (`ubos-verify`) and first-party Buks (`ubos-buks`). Research-repository code may be ported only under the rules of UBS-ARC-07 §6.
- **Consequences:** Clean dependency graph (AR-031). Porting is traceable.
- **Alternatives considered:** Extending `ubos-system` in place was rejected because its layout predates the 13-system decomposition. It remains the first porting source.
- **Affects:** UBS-ARC-07

### DEC-019 — Licensing of the standard, reference implementation and tools
- **Status:** Accepted · **Date:** 2026-09-29 (confirmed by the product owner as proposed)
- **Context:** The ARM-style positioning needs an open standard, a trustworthy open verifier and a commercially licensable engine. The whitepaper (EXT-WP) used AGPLv3.
- **Decision:**
  - BPA text under CC BY 4.0;
  - vectors, schemas, SDKs and verifier under Apache-2.0;
  - kernel, node and Forge under AGPL-3.0 as an open edition, with a commercial licence for embedding and hosted use without AGPL obligations;
  - first-party Buks under a commercial licence.
- **Consequences:** Third parties can implement the standard freely. Embedding vendors buy commercial licences (FR-STD-061). Legal review is required before publication.
- **Alternatives considered:** Fully proprietary was rejected because it contradicts the standard strategy. Apache-2.0 for everything was rejected because it removes the embedding licence revenue.
- **Affects:** UBS-ARC-07, FR-STD-062

### DEC-020 — Storage engine design: PostgreSQL/SQLite with kernel indexes and prolly trees
- **Status:** Accepted · **Date:** 2026-09-29
- **Context:** The design needs content addressing, fast head and bitemporal reads, Merkle roots per commit and gap-free ledgers, on standard databases.
- **Decision:** Authoritative content (chunks, versions, entries, commits, refs) is stored in PostgreSQL or SQLite tables, with large chunks in the object store. Derived kernel indexes (head, temporal, relationship, unique key) and prolly-tree nodes are maintained in the flush transaction (AR-016). Commits on one branch go through a per-branch sequencer with group commit. Ledger sequences are allocated from row-locked counters inside the flush.
- **Consequences:** Standard operations tooling (backups, replication). ASM-003 is validated in PH-1a benchmarks. Fallback: a log-structured object layer in front of PostgreSQL.
- **Alternatives considered:** A custom storage engine was rejected for operational risk. Pure event sourcing with replay was rejected because read latency would be too high.
- **Affects:** UBS-ARC-04, UBS-SYS-DVM

### DEC-021 — Policy engine: Cedar-class analysable policies plus relationship tuples
- **Status:** Accepted · **Date:** 2026-09-29
- **Context:** IMP-06 requires analysable policies and relationship-based permissions.
- **Decision:** Authorization policies use a Cedar-compatible language, evaluated in-process by the DVM. Relations come from the model's authorization relations, materialised in the relationship index and exposed to the policy engine as entity parents and attributes. Policy analysis (who-can) uses the language's analysis tooling.
- **Consequences:** No separate authorization service. Consistency is guaranteed by snapshot evaluation (FR-IAM-053).
- **Alternatives considered:** OPA/Rego was rejected because it is harder to analyse. A Zanzibar-style external service was rejected because of consistency and operational cost.
- **Affects:** UBS-ARC-06, UBS-SYS-DVM
