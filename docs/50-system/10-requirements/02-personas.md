---
id: UBS-REQ-02
title: Personas
status: review
phase: ALL
depends_on: [UBS-REQ-01]
---

# Personas

Personas are grouped by product layer (UBS-REQ-01 §5). Every requirement names the
personas it serves. **Concepts hidden** lists the terms the user interface MUST NOT show
to that persona.

## 1. Business layer

### PER-BusinessUser — Business User
- **Segment:** any organisation using UBOS applications
- **Role:** employee who creates and updates records and completes tasks
- **Goals:** 1. Finish tasks quickly. 2. Trust that what they see is current. 3. Find past records and their history without asking IT.
- **Pains today:** 1. Many screens across systems. 2. Uncertainty over which version of a document or price applies.
- **Skills:** business depth medium · technical depth none
- **Primary systems:** WSP
- **Concepts visible to this persona:** record, form, task, draft, submitted, published, history, attachment
- **Concepts hidden from this persona:** branch, merge, commit, overlay, class, instruction, projection
- **Success signal:** at least 90% task completion without help in usability tests; median task time at or below the legacy baseline

### PER-Approver — Approver
- **Segment:** any organisation
- **Role:** manager or designated approver in a segregation-of-duties chain
- **Goals:** 1. See exactly what changes and why before approving. 2. Approve from anywhere, including mobile and messages. 3. Be protected by an evidence trail.
- **Pains today:** 1. Approving a PDF without seeing the underlying data change. 2. Approval chains buried in email.
- **Skills:** business depth high · technical depth none
- **Primary systems:** WSP
- **Concepts visible to this persona:** change summary, before/after, approval, delegation, evidence
- **Concepts hidden from this persona:** branch, merge, commit hash, instruction
- **Success signal:** median approval decision time under 2 minutes, with the diff visible in 100% of approvals

### PER-FundAccountant — Fund Accountant
- **Segment:** asset manager, fund administrator (first vertical)
- **Role:** calculates NAV and fees and books and reconciles fund accounts
- **Goals:** 1. Produce NAV on time. 2. Correct past errors (as-of corrections) without corrupting later periods. 3. Explain every number.
- **Pains today:** 1. Back-dated corrections done by hand. 2. Spreadsheets used for fee calculations. 3. Reconciliation breaks with no lineage.
- **Skills:** business depth high · technical depth low
- **Primary systems:** WSP, Smart Grid (OFFICE)
- **Concepts visible to this persona:** effective date, as-of view, correction, reversal, posting, lineage
- **Concepts hidden from this persona:** branch, merge, instruction, projection
- **Success signal:** a back-dated correction is completed with automatic restatement in under 30 minutes; zero manual journal re-entry

### PER-FundOperationsManager — Fund Operations Manager
- **Segment:** asset manager, fund administrator
- **Role:** owns fund operations processes, dealing cut-offs and service levels
- **Goals:** 1. See process status and exceptions live. 2. Change operating rules safely. 3. Pass audits and examinations.
- **Pains today:** 1. No single view of exceptions. 2. Rule changes deployed by IT tickets.
- **Skills:** business depth high · technical depth low
- **Primary systems:** WSP (War Room), STU (read-only simulation results)
- **Concepts visible to this persona:** dashboard, exception, rule sheet (read), simulation result, release
- **Concepts hidden from this persona:** branch, commit hash, instruction
- **Success signal:** exception backlog visible in real time; rule-change lead time reduced from weeks to days

### PER-InvestorServicesClerk — Investor Services Clerk
- **Segment:** transfer agent, fund administrator
- **Role:** processes subscriptions, redemptions, transfers and investor KYC updates
- **Goals:** 1. Process dealing requests before cut-off. 2. Keep the investor register accurate. 3. Avoid rework.
- **Pains today:** 1. Re-keying from emails and PDFs. 2. Late corrections.
- **Skills:** business depth medium · technical depth none
- **Primary systems:** WSP
- **Concepts visible to this persona:** order, cut-off, dealing date, status, document
- **Concepts hidden from this persona:** branch, merge, commit, class
- **Success signal:** straight-through processing of at least 80% of dealing requests in the pilot

### PER-ContractManager — Contract Manager
- **Segment:** legal operations, procurement, sales operations
- **Role:** drafts, negotiates, approves and tracks contracts and obligations
- **Goals:** 1. Contracts always reflect current party data. 2. Clause changes are tracked. 3. Obligations and renewals are never missed.
- **Pains today:** 1. Word redlines with no link to data. 2. Obligations tracked in spreadsheets.
- **Skills:** business depth high · technical depth none
- **Primary systems:** WSP (Live Doc)
- **Concepts visible to this persona:** live document, clause, version, redline, signature, obligation
- **Concepts hidden from this persona:** branch, merge, instruction, projection
- **Success signal:** 100% of executed contracts have a linked data snapshot and signature proof

### PER-Executive — Executive
- **Segment:** any organisation
- **Role:** C-level or department head
- **Goals:** 1. See live performance. 2. Act on exceptions from the dashboard. 3. Trust the numbers.
- **Pains today:** 1. Stale slide decks. 2. No drill-down.
- **Skills:** business depth high · technical depth none
- **Primary systems:** WSP (War Room)
- **Concepts visible to this persona:** dashboard, drill-down, action, approval
- **Concepts hidden from this persona:** every platform-layer term
- **Success signal:** decisions taken from the War Room with full drill-down

### PER-ExternalParty — External Party
- **Segment:** counterparty, investor, supplier or customer of an organisation using UBOS
- **Role:** signs documents, submits requests, views shared records
- **Goals:** 1. Sign and submit without installing software. 2. Keep proof of what was agreed.
- **Pains today:** 1. PDFs with no verifiable link to the agreed data.
- **Skills:** business depth medium · technical depth none
- **Primary systems:** WSP (external portal), NTY (verifier)
- **Concepts visible to this persona:** document, signature, proof, verification result
- **Concepts hidden from this persona:** every builder-layer and platform-layer term
- **Success signal:** a signature completed in under 3 minutes; the proof verifies independently

## 2. Governance and assurance

### PER-ComplianceOfficer — Compliance Officer
- **Segment:** regulated organisations
- **Role:** defines and monitors compliance rules, segregation of duties and retention
- **Goals:** 1. Inject a control across all relevant records without IT projects. 2. Prove the control was active at a past date. 3. Manage legal holds.
- **Pains today:** 1. Controls hard-coded in applications. 2. No proof of when a control applied.
- **Skills:** business depth high · technical depth low
- **Primary systems:** STU (rule sheets), WSP, NTY
- **Concepts visible to this persona:** rule sheet, selector, control, effective period, legal hold, retention, evidence pack
- **Concepts hidden from this persona:** instruction, projection, cell
- **Success signal:** a new control is authored, simulated and published in under 1 day and verifiably active from its effective time

### PER-InternalAuditor — Internal Auditor
- **Segment:** regulated and listed organisations
- **Role:** tests controls and investigates transactions
- **Goals:** 1. Reconstruct any past state. 2. Trace any value to its causes. 3. Sample and export evidence.
- **Pains today:** 1. Logs that cannot be trusted or joined to data.
- **Skills:** business depth high · technical depth medium
- **Primary systems:** WSP (read-only audit views), NTY
- **Concepts visible to this persona:** history, as-of, lineage, actor, rule version, evidence pack, proof
- **Concepts hidden from this persona:** instruction, cell
- **Success signal:** any audit sample is reconstructed with lineage without engineering help

### PER-RegulatorExaminer — Regulator or External Examiner
- **Segment:** SEC or FINRA examiner, external auditor
- **Role:** examines books and records during an examination or audit
- **Goals:** 1. Receive complete, verifiable records within deadlines. 2. Verify integrity without trusting the firm's operators.
- **Pains today:** 1. Exports without integrity guarantees.
- **Skills:** business depth high · technical depth medium
- **Primary systems:** NTY (examiner view, verifier tool)
- **Concepts visible to this persona:** record, time of recording, effective time, signature, Merkle proof, anchor
- **Concepts hidden from this persona:** builder-layer terms except in exported metadata
- **Success signal:** an evidence pack verifies with the open verifier tool with no vendor involvement

### PER-SecurityOfficer — Security Officer
- **Segment:** any organisation (CISO office)
- **Role:** owns identity, access, key management and security monitoring
- **Goals:** 1. Least privilege, provably. 2. Answer "who can do what" instantly. 3. Detect and contain incidents.
- **Pains today:** 1. Opaque permission models. 2. Scripts with hidden privileges.
- **Skills:** business depth medium · technical depth high
- **Primary systems:** CTL, STU (policies), NOD
- **Concepts visible to this persona:** principal, role, policy, relationship, key, session, sandbox profile, audit event
- **Concepts hidden from this persona:** —
- **Success signal:** an access review for any resource is produced in under 1 minute from policy analysis

## 3. Builder layer

### PER-BusinessArchitect — Business Architect
- **Segment:** enterprise IT, partners
- **Role:** models classes, lifecycles, rule sheets, views and Buks
- **Goals:** 1. Model the business once and reuse it. 2. Change safely with simulation. 3. Upgrade vendor Buks without losing overlays.
- **Pains today:** 1. Every change is a code project. 2. Customisations break upgrades.
- **Skills:** business depth high · technical depth medium
- **Primary systems:** STU, FRG (through Studio)
- **Concepts visible to this persona:** class, inheritance, field, kind (Definition or Ledger), lifecycle, action, rule sheet, view, change set, simulation, release, overlay, Buk
- **Concepts hidden from this persona:** instruction ABI internals, cell
- **Success signal:** a new business object with lifecycle and views is released in under 1 day

### PER-BusinessAnalyst — Business Analyst
- **Segment:** enterprise business units
- **Role:** writes decision tables, computed fields, validation rules and reports
- **Goals:** 1. Express rules without code. 2. Test rules against real cases.
- **Pains today:** 1. Waiting for developers. 2. Rules scattered in spreadsheets.
- **Skills:** business depth high · technical depth low
- **Primary systems:** STU, WSP
- **Concepts visible to this persona:** decision table, expression, rule sheet, test case, simulation, change set
- **Concepts hidden from this persona:** branch, commit, instruction, WASM
- **Success signal:** at least 80% of the pilot's business rules are authored at the L1 declarative tier

### PER-ImplementationConsultant — Implementation Consultant
- **Segment:** system integrators and partners
- **Role:** configures Buks for customers, migrates data, trains users
- **Goals:** 1. Repeatable implementations. 2. Fast data migration. 3. Customer overlays that survive upgrades.
- **Pains today:** 1. Bespoke code per customer.
- **Skills:** business depth high · technical depth medium
- **Primary systems:** STU, BRG (import), FRG
- **Concepts visible to this persona:** overlay, Buk, import mapping, change set, release
- **Concepts hidden from this persona:** instruction internals
- **Success signal:** a standard implementation in under 6 weeks for a mid-size fund administrator

### PER-DataAnalyst — Data Analyst
- **Segment:** any organisation
- **Role:** builds reports and analytics over business data
- **Goals:** 1. Query consistent, versioned data. 2. Export to the analytics stack with lineage.
- **Pains today:** 1. Exports without history or definitions.
- **Skills:** business depth medium · technical depth medium
- **Primary systems:** BRG, WSP (reports)
- **Concepts visible to this persona:** projection, dataset, as-of, snapshot, lineage, export
- **Concepts hidden from this persona:** instruction, cell
- **Success signal:** an analytical dataset is refreshed incrementally with version and lineage columns

## 4. Platform layer

### PER-LogicDeveloper — Logic Developer
- **Segment:** enterprise IT, partners
- **Role:** writes L2 scripts and L3 WASM components and connectors
- **Goals:** 1. Typed, testable logic. 2. A fast local loop. 3. Clear limits and errors.
- **Pains today:** 1. Untyped scripts that fail at runtime. 2. No simulation against real history.
- **Skills:** business depth medium · technical depth high
- **Primary systems:** FRG, SDK, STU
- **Concepts visible to this persona:** every term
- **Concepts hidden from this persona:** —
- **Success signal:** the edit–check–simulate loop takes under 10 seconds for a typical Buk

### PER-IsvDeveloper — ISV Developer
- **Segment:** software vendors building vertical Buks
- **Role:** builds, versions, licenses and supports Buks
- **Goals:** 1. Build once, sell to many. 2. Upgrade customers safely. 3. Get paid.
- **Pains today:** 1. Per-customer forks. 2. No standard licensing or metering.
- **Skills:** business depth high · technical depth high
- **Primary systems:** FRG, EXC, STU
- **Concepts visible to this persona:** every term
- **Concepts hidden from this persona:** —
- **Success signal:** a Buk upgrade rolled out to all customers with conflicts reported per overlay

### PER-KernelEngineer — Kernel Engineer
- **Segment:** the UBOS core team
- **Role:** implements and maintains the DVM, Node and protocols
- **Goals:** 1. Correctness proven by conformance, simulation and formal models. 2. Performance within NR targets.
- **Pains today:** —
- **Skills:** business depth low · technical depth high
- **Primary systems:** DVM, NOD, BPA, FRG
- **Concepts visible to this persona:** every term
- **Concepts hidden from this persona:** —
- **Success signal:** zero correctness regressions escaping the conformance and simulation suites

### PER-ThirdPartyImplementer — Third-Party BPA Implementer
- **Segment:** a vendor embedding the DVM or implementing the BPA in another language
- **Role:** builds a BPA-compliant engine or product
- **Goals:** 1. An unambiguous standard. 2. A complete conformance suite. 3. Certification.
- **Pains today:** —
- **Skills:** business depth low · technical depth high
- **Primary systems:** BPA, FRG (conformance runner)
- **Concepts visible to this persona:** every standard term
- **Concepts hidden from this persona:** —
- **Success signal:** conformance passes without clarification requests for Must clauses

### PER-TenantAdministrator — Tenant Administrator
- **Segment:** customer IT
- **Role:** administers users, roles, VAEs, installed Buks and settings for an organisation
- **Goals:** 1. Onboard and offboard users automatically. 2. Install and upgrade Buks safely. 3. Control costs.
- **Pains today:** 1. Manual user management. 2. Risky upgrades.
- **Skills:** business depth medium · technical depth medium
- **Primary systems:** CTL (tenant console), STU
- **Concepts visible to this persona:** user, group, role, VAE, Buk, release, usage, licence
- **Concepts hidden from this persona:** instruction, cell internals
- **Success signal:** SCIM provisioning is live, and upgrades complete with zero unplanned downtime

### PER-PlatformOperator — Platform Operator
- **Segment:** UBOS operations team, partner operators, customer SRE for Server
- **Role:** operates nodes, cells, backups, upgrades and monitoring
- **Goals:** 1. Meet availability and durability objectives. 2. Recover predictably. 3. Place and move tenants.
- **Pains today:** —
- **Skills:** business depth low · technical depth high
- **Primary systems:** NOD, CTL
- **Concepts visible to this persona:** node, cell, tenant placement, backup, restore point, upgrade wave, SLO
- **Concepts hidden from this persona:** business data content (the operator MUST NOT read it)
- **Success signal:** SLOs met; restore drills pass every quarter

### PER-PersonalUser — Personal User
- **Segment:** individuals, freelancers, micro-businesses
- **Role:** runs a personal business workbench on the Box edition
- **Goals:** 1. Work offline. 2. Own the data. 3. Join an organisation later without migration.
- **Pains today:** 1. SaaS lock-in. 2. Scattered files and spreadsheets.
- **Skills:** business depth medium · technical depth low
- **Primary systems:** NOD (Box), WSP
- **Concepts visible to this persona:** record, document, grid, history, backup, sync
- **Concepts hidden from this persona:** cell, instruction
- **Success signal:** installation and first record in under 10 minutes

### PER-FieldWorker — Field Worker
- **Segment:** auditors on site, inspectors, sales representatives
- **Role:** captures and updates records offline
- **Goals:** 1. Work without a network. 2. Sync without losing work.
- **Pains today:** 1. Paper forms re-keyed later.
- **Skills:** business depth medium · technical depth none
- **Primary systems:** NOD (Box, mobile), WSP
- **Concepts visible to this persona:** offline, synced, conflict to resolve
- **Concepts hidden from this persona:** branch, merge, commit
- **Success signal:** zero lost edits after a sync following 5 days offline

## 5. AI

### PER-AiAgent — AI Agent (non-human principal)
- **Segment:** agents operated by an organisation or supplied by a Buk
- **Role:** performs routine work (reconciliation, triage, drafting, data quality) within a budget
- **Goals:** 1. Access typed tools and knowledge. 2. Simulate before proposing. 3. Receive clear feedback on rejection.
- **Pains today:** 1. Unstructured APIs. 2. No safe sandbox. 3. All-or-nothing permissions.
- **Skills:** business depth depends on configuration · technical depth: n/a
- **Primary systems:** AGT, DVM (through instructions), NOD
- **Concepts visible to this persona:** the class model, ports, knowledge, agent branch, change set, simulation result, budget
- **Concepts hidden from this persona:** credentials and keys (agents never hold raw secrets)
- **Success signal:** change-set acceptance rate at least 80%; zero actions outside the granted scope

### PER-AgentSupervisor — Agent Supervisor
- **Segment:** operations teams using agents
- **Role:** configures agents, reviews their change sets and monitors cost and quality
- **Goals:** 1. Delegate safely. 2. Review efficiently. 3. Stop an agent instantly.
- **Pains today:** 1. No review model for AI output on systems of record.
- **Skills:** business depth high · technical depth low
- **Primary systems:** AGT, WSP
- **Concepts visible to this persona:** agent, budget, scope, change set, diff, simulation, accept, reject, revert
- **Concepts hidden from this persona:** branch internals, instruction
- **Success signal:** median review time per change set under 3 minutes

## 6. Ecosystem operations

### PER-ExchangeOperator — Exchange Operator
- **Segment:** UBOS company
- **Role:** runs the Buk Exchange: vetting, listing, disputes, payouts
- **Goals:** 1. Only verified Buks are listed. 2. Accurate metering and payouts.
- **Pains today:** —
- **Skills:** business depth medium · technical depth medium
- **Primary systems:** EXC, CTL
- **Concepts visible to this persona:** Buk, publisher, verification badge, licence, usage, payout
- **Concepts hidden from this persona:** customer business data
- **Success signal:** zero listed Buks without a passing verification record

### PER-StandardsSteward — Standards Steward
- **Segment:** the body governing the BPA (initially the UBOS company)
- **Role:** maintains the standard, versioning, errata, the conformance suite and certification
- **Goals:** 1. A stable, unambiguous standard. 2. Credible certification.
- **Pains today:** —
- **Skills:** business depth medium · technical depth high
- **Primary systems:** BPA
- **Concepts visible to this persona:** every standard term
- **Concepts hidden from this persona:** —
- **Success signal:** every standard release ships with complete conformance vectors and a change log
