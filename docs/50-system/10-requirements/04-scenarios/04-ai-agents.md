---
id: UBS-REQ-04-04
title: Scenarios — Governed AI
status: review
phase: PH-3
depends_on: [UBS-REQ-04]
---

# Scenarios — Governed AI (SCN-301 … SCN-307)

All AI scenarios share the rules below. They are made normative in the AI functional
requirements.
- Agents are principals. They never hold raw credentials.
- Agents write only to their own agent branches. Merge requires governance.
- All model calls go through the model gateway and are recorded.

### SCN-301 — Reconciliation agent proposes adjustments for review
- **Goal:** An agent clears routine reconciliation breaks, and a human approves its change set in minutes.
- **Personas:** PER-AiAgent, PER-AgentSupervisor, PER-FundAccountant
- **Systems:** AGT, DVM, WSP
- **Phase:** PH-3
- **Preconditions:** 1. Agent `recon-bot` has role `recon-proposer`, scope "fund GLOBAL-EQ, classes StatementLine, CashMovement (read), ReconMatch (write)", a budget of 200k tokens per day and 500 commits per day. 2. SCN-108 left 40 open breaks.
- **Main flow:**
  1. At 19:00 the agent is scheduled → Agent Hub opens branch `agent/recon-bot/2026-10-09`.
  2. The agent reads the breaks through tools, matches 31 of them with explanations, and proposes 4 adjustment entries.
  3. The agent runs simulation on its branch → all rules pass. The change set is submitted with a summary written by the agent and its confidence per item.
  4. The supervisor opens the review → the diff groups items by rule and confidence. The supervisor accepts 33 items, rejects 2 with reasons, and merges.
- **Alternate and failure flows:**
  - A1. The supervisor rejects the whole change set → the branch is closed, and the rejection reasons are fed back to the agent's evaluation store.
- **Postconditions (observable):** 1. Each merged item records the agent principal, the model and prompt version, and the approving supervisor. 2. The item can be reverted individually or as a set.
- **Business rules exercised:** FR-AI-021, FR-AI-031, FR-AI-041, FR-VER-031, FR-UX-061, FR-LEDG-071
- **Verification:** SCN, PILOT, USE

### SCN-302 — Agent is stopped by scope, budget and kill switch
- **Goal:** An agent can never act outside its grant, and it can be stopped instantly.
- **Personas:** PER-AgentSupervisor, PER-SecurityOfficer
- **Systems:** AGT, DVM
- **Phase:** PH-3
- **Preconditions:** 1. Agent `recon-bot` as in SCN-301.
- **Main flow:**
  1. The agent attempts to write `InvestorAccount` (out of scope) → denied with `IAM.DENIED`, and the attempt is logged as a scope violation.
  2. The agent reaches 100% of its daily token budget → subsequent model calls fail with `AI.BUDGET_EXHAUSTED`, and the open run ends gracefully with a partial change set.
  3. The supervisor presses "stop" during a run → all in-flight tool calls are cancelled within 2 seconds, and the agent's branch is frozen.
- **Alternate and failure flows:**
  - A1. The agent tries to merge its own branch → denied. Merge requires a human or policy approver.
- **Postconditions (observable):** 1. Security reports list every violation with context.
- **Business rules exercised:** FR-AI-021, FR-IAM-041, FR-AI-011, FR-BILL-051
- **Verification:** SCN, SEC

### SCN-303 — Copilot drafts a contract amendment
- **Goal:** A user describes a change in natural language, and the copilot prepares a correct draft for review.
- **Personas:** PER-ContractManager
- **Systems:** AGT, WSP, DVM
- **Phase:** PH-3
- **Preconditions:** 1. An executed agreement `SA-2026-031` exists.
- **Main flow:**
  1. The user asks "Reduce the monthly fee to 18,000 from November and extend the term by one year."
  2. The copilot proposes an amendment draft: fee version valid from 2026-11-01, end date +1 year, affected obligations listed, and clause text changes rendered in the Live Doc.
  3. The user reviews the diff and adjusts the effective date → the draft is saved. Nothing is committed to the agreement until the normal amendment flow completes.
- **Alternate and failure flows:**
  - A1. The request is ambiguous (for example "from next month" at a month boundary) → the copilot asks a clarification question instead of guessing.
- **Postconditions (observable):** 1. The copilot interaction is linked to the draft for audit.
- **Business rules exercised:** FR-AI-061, FR-AI-051, FR-OFFICE-011, FR-TIME-021
- **Verification:** SCN, USE

### SCN-304 — AI builder generates a Buk draft
- **Goal:** A business architect goes from written requirements to a reviewed, installable Buk draft.
- **Personas:** PER-BusinessArchitect, PER-LogicDeveloper
- **Systems:** AGT, STU, FRG
- **Phase:** PH-3
- **Preconditions:** 1. A requirements note describes an "Asset Loan" process: classes, lifecycle, approval rules and two views.
- **Main flow:**
  1. The architect submits the note to the builder → the builder produces a draft Buk: classes extending base ontology classes, the lifecycle, L1 rules, views, knowledge text and 6 scenarios.
  2. Forge checks run automatically → 2 issues are found (a missing inverse relationship name, an unreachable state). The builder fixes them in a second iteration.
  3. The architect reviews the change set in Studio, edits one rule and installs it into a sandbox VAE → the scenarios pass.
- **Alternate and failure flows:**
  - A1. The builder proposes a class that duplicates `Party` → the ontology lint flags it and suggests extension instead.
- **Postconditions (observable):** 1. The Buk records which parts were AI-generated and which human edited them.
- **Business rules exercised:** FR-AI-071, FR-PKG-061, FR-DEV-011, FR-MODEL-091
- **Verification:** SCN, USE

### SCN-305 — External AI client uses MCP tools with delegated rights
- **Goal:** A user's own AI assistant operates UBOS through MCP with the user's delegated permissions, never more.
- **Personas:** PER-BusinessUser, PER-AiAgent
- **Systems:** AGT, DVM
- **Phase:** PH-3
- **Preconditions:** 1. The user authorises an external MCP client with a scoped, expiring delegation (read contracts, create drafts).
- **Main flow:**
  1. The client lists tools → it receives typed tools for the permitted class ports and queries only.
  2. The client creates a draft agreement → it lands in an agent branch attributed to "client X on behalf of user U".
  3. The user reviews and submits the draft in Workspace.
- **Alternate and failure flows:**
  - A1. The delegation expires → the tool calls fail with `IAM.DELEGATION_EXPIRED`.
- **Postconditions (observable):** 1. The audit shows the delegation chain for every call.
- **Business rules exercised:** FR-AI-041, FR-IAM-041, FR-AI-031
- **Verification:** SCN, SEC

### SCN-306 — BPU proposes its own evolution
- **Goal:** Recurring validation failures lead to a proposed rule improvement that goes through governance.
- **Personas:** PER-BusinessArchitect, PER-BusinessAnalyst
- **Systems:** AGT, DVM, STU
- **Phase:** PH-3
- **Preconditions:** 1. Over 30 days, 120 `DealingOrder` submissions failed validation with manually corrected account numbers in the same pattern.
- **Main flow:**
  1. The evolution analyser (an agent attached to the class knowledge) detects the pattern and opens an evolution branch that proposes a normalisation rule and a test case.
  2. The analyser simulates the rule on the 120 historical cases → 118 would pass. Two ambiguous cases are listed.
  3. The analyst reviews, adjusts the rule and merges through normal approval.
- **Alternate and failure flows:**
  - A1. The analyst rejects the proposal → the pattern is recorded as "known, rejected" and not proposed again within the suppression period.
- **Postconditions (observable):** 1. The class never changes without a merge by an authorised principal.
- **Business rules exercised:** FR-AI-101, FR-RULE-071, FR-RULE-081
- **Verification:** SCN, INSP

### SCN-307 — Rejected change set with actionable feedback
- **Goal:** When a change set fails validation or review, the agent receives structured feedback and can retry within budget.
- **Personas:** PER-AiAgent, PER-AgentSupervisor
- **Systems:** AGT, DVM
- **Phase:** PH-3
- **Preconditions:** 1. An agent submits a change set that violates an invariant on 3 of 50 items.
- **Main flow:**
  1. The submission is rejected with a machine-readable error list (item, rule, explanation, suggested fields).
  2. The agent fixes the 3 items and resubmits → it passes validation and waits for review.
- **Alternate and failure flows:**
  - A1. A retry limit is reached → the change set is escalated to the supervisor with the history of attempts.
- **Postconditions (observable):** 1. The attempts and their feedback are stored for evaluation.
- **Business rules exercised:** FR-AI-031, FR-AUD-031, FR-AI-081
- **Verification:** SCN
