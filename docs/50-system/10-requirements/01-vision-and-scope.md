---
id: UBS-REQ-01
title: Vision, Problem and Scope
status: complete
phase: ALL
depends_on: [UBS-META-00, UBS-META-06]
---

# Vision, Problem and Scope

## 1. Problem statement

Enterprises run their business on state that changes over time: contracts, orders,
positions, prices, policies and approvals. Today this state is spread across three kinds
of tools, and each fails differently.

| Tool family | What goes wrong |
|---|---|
| Packaged applications and SaaS (ERP, CRM, fund administration systems) | 1. History is overwritten by `UPDATE`; audit trails are side logs that drift from the data. 2. Customisation forks the vendor's product or waits for the vendor's roadmap. 3. Upgrades break customisations. 4. Data and logic are locked into the vendor's formats. |
| Low-code platforms and workflow tools | 1. Rules live inside flows and scripts with no version discipline. 2. There is no safe way to trial a change on real data before publishing it. 3. The platform does not prove what state was valid at a past date or which rule version produced a result. |
| Office documents and spreadsheets | 1. Documents are dead snapshots that go stale. 2. Formulas are hidden, copied and inconsistent. 3. Nothing links the signed contract to the data it describes. 4. There is no trustworthy history. |

Two new pressures make the problem acute:
1. **Regulators and auditors** demand reproducibility. They ask what the firm knew at a
   given date, who changed it, under which rule, and whether it can be proven.
2. **AI agents** are entering business operations. Enterprises do not let agents act on
   systems of record, because they cannot review, simulate, bound or undo what an agent
   does.

## 2. Vision

> **UBOS makes business state trustworthy.** Business state is typed and inheritable,
> versioned like code, bitemporal, governed by declarative rules and provable with
> cryptography. People and AI agents change it through the same reviewed, simulated and
> reversible paths.

UBOS achieves this by standardising the **mechanism** of business state change as a
published architecture, the **BPA**. The BPA runs on a reference engine, the **DVM**, and
a standard business unit, the **BPU**. The **policy** (classes, rules, flows, views) stays
fully changeable as versioned data.

## 3. Product principles

| # | Principle | Meaning for requirements |
|---|---|---|
| P1 | Hardened mechanism, soft policy | Kernel semantics are fixed, versioned and verified. Business behaviour is data. |
| P2 | History is never rewritten | Definition state gains new versions. Ledger state gains reversals. Nothing is destroyed except by governed crypto-shredding for privacy law. |
| P3 | Every change is reviewable before it takes effect | Drafts, change sets, simulation and approval apply to people, integrations and agents alike. |
| P4 | Every result is explainable | Any value can be traced to its inputs, the rule versions and the actor that produced it. |
| P5 | Every state is provable | Signed commits and Merkle roots let a third party verify integrity without trusting the operator. |
| P6 | Local-first and portable | The same engine runs on a laptop, a server, a cluster or in a browser. Data can always be exported in open formats. |
| P7 | Progressive disclosure | Business users see forms, tasks and "draft → submit → published". Builders see classes and rules. Only platform engineers see branches and merges. |
| P8 | Open standards at the edges | JSON Schema, OpenAPI, CloudEvents, OpenTelemetry, OIDC, SCIM, MCP, Parquet/Iceberg. |

## 4. Value propositions by segment

| Segment | Primary value | Proof point (verified in pilots) |
|---|---|---|
| Regulated financial services (first vertical, DEC-004) | Reproducible books and records; bitemporal corrections; evidence packs for examinations | An examiner question ("state of fund X as known on date D") is answered in under 5 minutes, with a verifiable evidence pack |
| Enterprise operations (contracts, procurement, master data) | Change safety: draft, review, simulate and publish rules and master data without breaking operations | A rule change is simulated against 12 months of history before publication |
| ISVs and implementation partners | Build vertical Buks once; customer overlays survive upgrades | A vendor upgrade merges into 10 customised tenants with conflicts reported, not lost |
| AI-forward operations teams | Agents act inside a governed sandbox: branch, simulate, review, merge or roll back | An agent's change set of 100 edits is reviewed as one diff and reverted in one action |
| Individuals and small teams | A personal, offline-capable business workbench that grows into the enterprise edition without migration | A Box user joins an organisation and syncs without data conversion |

## 5. Product layers (progressive disclosure)

| Layer | Personas (see UBS-REQ-02) | Main systems | Vocabulary shown |
|---|---|---|---|
| **Business layer** | business users, approvers, fund and contract staff, executives, external parties | Workspace | record, form, task, draft, submitted, published, history, document, signature |
| **Builder layer** | business architects, analysts, consultants, compliance officers | Studio, Workspace | class, field, rule sheet, lifecycle, view, change set, simulation, release, Buk |
| **Platform layer** | logic developers, ISV developers, kernel engineers, operators | Forge, Node, Control Plane, SDK | branch, merge, commit, overlay, instruction, profile, projection, cell |

## 6. Editions and deployment forms

| Edition | Host | Storage | Primary users | First phase |
|---|---|---|---|---|
| **Box** | desktop (Windows, macOS, Linux) and mobile | SQLite, local object store | individuals, field workers, small teams | PH-1 (headless), PH-3 (full app) |
| **Server** | customer data centre or private cloud | PostgreSQL, S3-compatible store, runtime store | enterprises | PH-1 |
| **Cell** | UBOS-operated or partner-operated cloud | PostgreSQL per cell, tiered object store | multi-tenant SaaS customers | PH-4 |
| **Browser kernel** | web browser (WASM, local SQLite) | origin-private file system | local-first web users | PH-5 |
| **Worker** | organisation-owned machines | none authoritative | batch and AI jobs (DEC-002) | PH-4 |

## 7. Goals and success measures

Success measures become `MET-*` metrics in the phase plans.

| # | Goal | Success measure | Phase |
|---|---|---|---|
| G1 | Prove the mechanism | 100% of BPA conformance vectors pass on the reference DVM on PostgreSQL and on SQLite | PH-1 |
| G2 | Prove an application can be built without kernel changes | The fund-operations and contract Buks run with zero kernel code specific to them | PH-2 |
| G3 | Prove regulated value | One design partner in asset management accepts the pilot, with reproducibility and evidence metrics met | PH-3 |
| G4 | Prove governed AI | At least 80% of agent change sets in the pilot are accepted after review, and 100% are revertible | PH-3 |
| G5 | Prove the platform | At least 3 third-party Buks are published; at least 1 third-party BPA implementation or embedding passes conformance | PH-4 |
| G6 | Prove federation | Two independent organisations operate a shared contract BPU across their own nodes | PH-5 |

## 8. Scope summary

### 8.1 In scope (by domain)

The requirements volume covers all domains listed in `UBS-META-01` §4.1:
- model, versioning, time, ledger and transactions;
- logic, rules, flows, events and query;
- identity and tenancy, audit, user experience and the Office suite;
- AI, packages, integration and analytics;
- proof and sync;
- operations, billing, developer experience and migration;
- notifications, files, signatures and localisation;
- governance of the standard itself.

### 8.2 Out of scope

1. The excluded narratives listed in `UBS-META-00` §3.2 and DEC-012.
2. High-frequency trading, order matching and payment switching. Throughput beyond the
   targets in the NR catalogue is not a goal.
3. General-purpose OLAP inside the kernel. Analytics leave through the Bridge (IMP-09).
4. IoT time-series ingestion at sensor frequency.
5. Content-management websites and e-commerce storefronts.
6. Replacing the core ledgers of banks or insurers. UBOS may run around them and
   integrate with them.
7. Tokenised assets, blockchains as a system of record, and cryptocurrency payments.
   Anchoring a Merkle root to an external timestamping service, including a public chain,
   is allowed as a proof mechanism only.

## 9. Competitive frame

| Category | Examples | UBOS difference |
|---|---|---|
| Enterprise application platforms | Salesforce Platform, ServiceNow, Microsoft Power Platform | Versioned and bitemporal state; branch-and-merge change management; provable history; a governed agent sandbox; an open standard with portable engines |
| Versioned databases | Dolt, TerminusDB, XTDB | A business semantics layer: classes with inheritance, lifecycles, rules, UI, packages, AI, all governed |
| Workflow engines | Temporal, Camunda | Workflows are one element of a BPU, bound to versioned, bitemporal business state |
| Low-code builders | Retool, OutSystems, Appian | Change safety, reproducibility and proofs; progressive disclosure instead of one builder view |
| Fund administration systems | Vendor-specific platforms | Built as Buks on an open architecture; customer overlays; bitemporal NAV corrections and evidence packs |
