---
id: UBS-REQ-05-06
title: Functional Requirements — RULE (Governance Sheets, Validation, Decisions)
status: draft
phase: PH-1
depends_on: [UBS-REQ-05, UBS-REQ-05-05]
---

# Functional Requirements — RULE

A **Logic Governance Sheet (LGS)** is a versioned object of rule blocks. Each block has a
**selector** that matches objects and **declarations** that set governed properties on the
matched objects. Sheets work like CSS for business rules. They are the L1 declarative tier
of IMP-05.

## CAP-RULE-01 — Governance sheets

### FR-RULE-011 — Sheet objects
- **Statement:** A governance sheet MUST be a Definition object holding ordered rule blocks, plus metadata: `layer`, `priority`, `owner`, `description`, a valid-time period and `scope` (VAE, branch kinds, class set).
- **Rationale:** Rules are data, versioned and governed like any object (EXT-WP §2.2, EXT-TRI "CLS").
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-ComplianceOfficer, PER-BusinessAnalyst
- **Acceptance:**
  1. Given a sheet, when committed on a draft and merged, then it becomes effective on `main` from the merge commit or its valid-from, whichever is later.
- **Verification:** CONF
- **Origin:** EXT-WP, EXT-RFC, EXT-TRI

### FR-RULE-012 — Selector language
- **Statement:** Selectors MUST support:
  - class selectors (matching subclasses) and `*`;
  - attribute conditions with `=`, `!=`, `<`, `<=`, `>`, `>=`, `IN`, `NOT IN`, `MATCHES` and `IS NULL`;
  - tag conditions and lifecycle-state conditions;
  - context conditions (VAE, branch kind, channel, principal role, principal kind);
  - relationship conditions through one reference hop;
  - boolean `AND`, `OR` and `NOT`.
- **Rationale:** Aspect-oriented governance without code (EXT-TRI).
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM, BPA
- **Personas:** PER-ComplianceOfficer
- **Acceptance:**
  1. Given `MoneyMovement[amount > 10000][channel = "api"]`, when a matching API payment is evaluated, then the block matches. For a UI payment it does not.
- **Verification:** CONF
- **Origin:** EXT-RFC 5.1

### FR-RULE-013 — Registered declaration properties
- **Statement:** Declarations MUST use properties from a registry in which each property has:
  - a name, a value schema and a combination rule;
  - an enforcement point (`commit`, `action`, `read`, `view` or `retention`);
  - its owner (kernel or Buk).

  The kernel registry MUST include at least:
  - `audit_level`, `require_signature`, `approvals`, `maker_checker`;
  - `read_only`, `field_required`, `field_default`, `field_visibility`, `mask`;
  - `retention`, `classification`, `notify`, `write_policy` and `rate_limit`.
- **Rationale:** Typed declarations are analysable and prevent silent typos.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-ComplianceOfficer, PER-IsvDeveloper
- **Acceptance:**
  1. Given `audit_levl: strict` (misspelled), when committed, then it fails with `RULE.UNKNOWN_PROPERTY`.
- **Verification:** CONF
- **Origin:** EXT-RFC, NEW

### FR-RULE-014 — Buk-defined properties
- **Statement:** Buks MUST be able to register additional properties with schemas and handlers (L1 or L2) that run at the property's enforcement point.
- **Rationale:** Domain controls (for example `max_issuer_concentration`) without kernel changes.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-IsvDeveloper
- **Acceptance:**
  1. Given a Buk property with a commit-time handler, when a matching object violates it, then the commit fails with the handler's explanation.
- **Verification:** CONF
- **Origin:** NEW

### FR-RULE-015 — Standard grammar
- **Statement:** The LGS textual grammar MUST be defined in ABNF in the BPA standard. It MUST have a lossless JSON representation for storage and tooling.
- **Rationale:** Portability and tool support.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** BPA, DVM
- **Personas:** PER-ThirdPartyImplementer
- **Acceptance:**
  1. Given any valid sheet, when converted text → JSON → text, then the result is semantically identical.
- **Verification:** CONF, PROP
- **Origin:** EXT-RFC

## CAP-RULE-02 — Cascade and explanation

### FR-RULE-021 — Deterministic cascade
- **Statement:** When several blocks set the same property on an object, the winner MUST be determined by, in order:
  1. layer (`platform` < `industry` < `vendor` < `tenant` < `vae` < `unit` < `object`);
  2. selector specificity;
  3. sheet priority;
  4. block order.

  Two candidates that tie after all four criteria MUST cause the sheet commit to fail with `RULE.AMBIGUOUS_CASCADE`.
- **Rationale:** Predictable outcomes (SCN-009 A1).
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM, BPA
- **Personas:** PER-ComplianceOfficer
- **Acceptance:**
  1. Given the cascade vector suite, when evaluated, then all winners match the expected results.
- **Verification:** CONF, PROP
- **Origin:** EXT-TRI, EXT-RFC

### FR-RULE-022 — Explanation of effective declarations
- **Statement:** For any object and property, the platform MUST explain the effective value: every matching block, its sheet version, specificity and layer, and why the winner won.
- **Rationale:** Rules must be explainable (principle P4).
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-ComplianceOfficer, PER-BusinessUser
- **Acceptance:**
  1. Given SCN-009 step 4, when explained, then the trace lists each candidate and the winner.
- **Verification:** CONF
- **Origin:** NEW

### FR-RULE-023 — Locked declarations
- **Statement:** A declaration MAY be marked `locked`. A locked value MUST NOT be overridden by any higher layer. Attempts MUST fail at authoring with `RULE.LOCKED_OVERRIDE`.
- **Rationale:** Compliance controls must survive tenant customisation.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-ComplianceOfficer
- **Acceptance:**
  1. Given a vendor-layer `audit_level: strict locked`, when a tenant sheet sets `audit_level: basic`, then the tenant sheet commit fails.
- **Verification:** CONF
- **Origin:** NEW

### FR-RULE-024 — Evaluation performance
- **Statement:** Effective-declaration evaluation MUST use selector indexes so that its cost grows with the number of candidate blocks for the object's class chain, not with the total number of sheets.
- **Rationale:** Tenants may have thousands of blocks.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-KernelEngineer
- **Acceptance:**
  1. Given 10,000 blocks, of which 20 are candidates for a class, when evaluated, then the latency is within the NR-PERF bound for 20 candidates.
- **Verification:** BENCH
- **Origin:** NEW

## CAP-RULE-03 — Validation and invariants

### FR-RULE-031 — Object validations
- **Statement:** Classes, extensions and sheets MUST be able to declare L1 validations on objects. Validations are evaluated on every commit that creates or changes an object.
- **Rationale:** Declarative validation (IMP-05).
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-BusinessAnalyst
- **Acceptance:**
  1. Given `end_date >= start_date`, when violated, then the commit fails with the validation explanation.
- **Verification:** CONF
- **Origin:** L40:SPEC-18

### FR-RULE-032 — Cross-object invariants
- **Statement:** Invariants MAY reference other objects through queries. They MUST declare their scope (the classes and relationships whose changes can affect them), and the DVM MUST evaluate them in the commit snapshot whenever an in-scope change occurs.
- **Rationale:** Limits and consistency rules span objects.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-BusinessAnalyst, PER-FundAccountant
- **Acceptance:**
  1. Given "sum of allocations per fund = 100%", when one allocation changes so that the sum is 98%, then the commit fails.
- **Verification:** CONF, PROP
- **Origin:** L40:SPEC-18

### FR-RULE-033 — Severity levels
- **Statement:** Validations MUST declare severity `error` (blocks the commit), `warning` (commits and records the warning, and the UI shows it) or `info`.
- **Rationale:** Not every rule is blocking.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-BusinessAnalyst
- **Acceptance:**
  1. Given a warning, when violated, then the commit succeeds, and the warning is attached to it.
- **Verification:** CONF
- **Origin:** NEW

### FR-RULE-034 — All violations at once
- **Statement:** A failed commit MUST report all error-level violations found, not only the first, up to a configurable maximum (default 100).
- **Rationale:** Users and agents fix everything in one pass (SCN-307).
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-BusinessUser, PER-AiAgent
- **Acceptance:**
  1. Given 3 violations, when committed, then the error lists 3 items.
- **Verification:** CONF
- **Origin:** L40:SPEC-18

### FR-RULE-035 — Validation preview
- **Statement:** Clients MUST be able to validate a proposed change without committing, and receive the same results a commit would produce.
- **Rationale:** Immediate form feedback and agent self-checks.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM, SDK
- **Personas:** PER-BusinessUser, PER-AiAgent
- **Acceptance:**
  1. Given a draft object, when validated in preview and then committed without changes, then the results are identical.
- **Verification:** CONF
- **Origin:** NEW

## CAP-RULE-04 — Decision tables

### FR-RULE-041 — Decision tables
- **Statement:** The platform MUST support decision tables with typed inputs and outputs, rows of L1 conditions, and the hit policies `unique`, `first`, `priority` and `collect` (with `sum`, `min`, `max` and `count`).
- **Rationale:** Fee tiers, clause selection and routing (SCN-105, SCN-201).
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-BusinessAnalyst
- **Acceptance:**
  1. Given a tiered fee table with policy `first`, when evaluated for each boundary value, then the expected tier is chosen.
- **Verification:** CONF, PROP
- **Origin:** L40:SPEC-18

### FR-RULE-042 — Completeness and overlap checks
- **Statement:** At authoring time the platform MUST report gaps (input combinations with no matching row) and overlaps that violate the hit policy, for inputs whose domains are enumerable or range-based.
- **Rationale:** Most decision-table bugs are gaps and overlaps.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM, FRG
- **Personas:** PER-BusinessAnalyst
- **Acceptance:**
  1. Given a table missing the range 1M–2M, when checked, then the gap is reported.
- **Verification:** CONF
- **Origin:** NEW

### FR-RULE-043 — Versioned and bound
- **Statement:** Decision tables MUST be bitemporal Definition objects, and every evaluation MUST be recorded in the rule binding (FR-TIME-061).
- **Rationale:** Reproducibility of decisions.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-ComplianceOfficer
- **Acceptance:**
  1. Given a fee accrual, when inspected, then the table version used is listed.
- **Verification:** CONF
- **Origin:** IMP-02

### FR-RULE-044 — DMN interchange
- **Statement:** The platform SHOULD import and export decision tables in DMN 1.4 format for the supported subset, and report unsupported constructs.
- **Rationale:** Interoperability with existing rule assets.
- **Priority:** Could · **Phase:** PH-3 · **Systems:** BRG, STU
- **Personas:** PER-BusinessAnalyst
- **Acceptance:**
  1. Given a DMN table in the supported subset, when imported and exported, then the evaluation results are unchanged.
- **Verification:** CONF
- **Origin:** IMP-12

## CAP-RULE-05 — Policy decisions with veto

### FR-RULE-051 — Action decisions
- **Statement:** Before any action executes, the platform MUST evaluate applicable decisions (from sheets, action declarations and Buk policies). Each decision returns `allow`, `deny` or `require` (additional steps: approval, signature, reason, step-up authentication, second person).
- **Rationale:** Governance of what may happen, not only of what data is valid.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-ComplianceOfficer
- **Acceptance:**
  1. Given `require_signature` for refunds above 10,000, when a refund of 12,000 is invoked without a signature, then the response is `require: signature` with an explanation.
- **Verification:** CONF
- **Origin:** L40:SPEC-18

### FR-RULE-052 — Veto combination
- **Statement:** Decisions MUST combine so that any `deny` wins, and `require` steps accumulate (the union of all required steps). Only when no decision denies and every requirement is satisfied is the action allowed.
- **Rationale:** Deny-overrides is the safe combination.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-ComplianceOfficer
- **Acceptance:**
  1. Given one decision that allows and one that denies, when combined, then the result is `deny` with both explanations.
- **Verification:** CONF, PROP
- **Origin:** L40:SPEC-18

### FR-RULE-053 — Decisions apply to every principal kind
- **Statement:** Decisions MUST apply equally to human, service and agent principals. Decisions MAY add conditions on the principal kind.
- **Rationale:** AI goes through the same governance (principle P3).
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-AgentSupervisor
- **Acceptance:**
  1. Given a rule "agents require approval for any write to FeeSchedule", when an agent invokes the action, then `require: approval` is returned.
- **Verification:** CONF
- **Origin:** IMP-10

## CAP-RULE-06 — Attach, detach and effective periods

### FR-RULE-061 — Effective periods for sheets
- **Statement:** A sheet MUST be effective only within its valid-time period and from the transaction time of the commit that attached it.
- **Rationale:** Controls have effective dates (SCN-106).
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-ComplianceOfficer
- **Acceptance:**
  1. Given a sheet valid from next quarter, when objects are evaluated today, then it does not apply. At `asof` next quarter it does.
- **Verification:** CONF
- **Origin:** IMP-02

### FR-RULE-062 — Time-boxed attachments
- **Statement:** A sheet attachment MAY declare an end. After the end the sheet MUST stop applying without further action, and an event MUST be emitted.
- **Rationale:** Campaigns and temporary controls (EXT-TRI "Black Friday" example).
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-FundOperationsManager
- **Acceptance:**
  1. Given an attachment for 24 hours, when the period ends, then evaluation no longer includes the sheet.
- **Verification:** CONF
- **Origin:** EXT-TRI

### FR-RULE-063 — Scoped attachment
- **Statement:** A sheet MUST be attachable to a VAE, a set of branch kinds or a set of classes, and its effective scope MUST be queryable.
- **Rationale:** Localised controls (for example the Japan compliance pack).
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-ComplianceOfficer
- **Acceptance:**
  1. Given a sheet scoped to VAE `tokyo`, when evaluated in VAE `hq`, then it does not apply.
- **Verification:** CONF
- **Origin:** EXT-TRI

## CAP-RULE-07 — Simulation and impact analysis

### FR-RULE-071 — Historical simulation of rule changes
- **Statement:** The platform MUST simulate a change set containing rule changes over a historical window. It re-evaluates the affected validations, decisions, declarations and derived results with the new rules against historical states, and reports the differences from actual outcomes.
- **Rationale:** Change safety (SCN-106).
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM, FRG
- **Personas:** PER-ComplianceOfficer, PER-BusinessAnalyst
- **Acceptance:**
  1. Given SCN-106, when simulated, then 14 historical breaches on 9 dates are reported.
- **Verification:** SCN, CONF
- **Origin:** L40:SPEC-18, NEW

### FR-RULE-072 — Simulation budgets
- **Statement:** Simulations MUST run under the `simulation` profile with a budget. When the budget is exhausted, they MUST return partial results with coverage information.
- **Rationale:** Large histories are expensive.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-BusinessAnalyst
- **Acceptance:**
  1. Given a budget smaller than required, when simulated, then the result states the covered date range and asks to narrow it.
- **Verification:** CONF
- **Origin:** NEW

### FR-RULE-073 — Simulation runs are evidence
- **Statement:** Every simulation run MUST be stored as an object with parameters, versions, results and coverage, and linked to the change set it evaluated.
- **Rationale:** Approvers rely on it, and auditors review it.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-Approver, PER-InternalAuditor
- **Acceptance:**
  1. Given an approved change set, when inspected, then its simulation runs are linked.
- **Verification:** CONF
- **Origin:** NEW

### FR-RULE-074 — Live impact count
- **Statement:** For a rule change, the platform MUST report the number of current objects whose effective declarations or validation outcomes would change, by class.
- **Rationale:** A quick blast-radius estimate before a full simulation.
- **Priority:** Should · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-ComplianceOfficer
- **Acceptance:**
  1. Given a threshold change, when analysed, then counts per class are returned.
- **Verification:** CONF
- **Origin:** NEW

## CAP-RULE-08 — Rule testing

### FR-RULE-081 — Test cases for rules
- **Statement:** Sheets, decision tables, validations and decisions MUST support attached test cases: an input object or context and the expected effective declarations, decision or validation outcome.
- **Rationale:** Rules are code and deserve tests.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM, FRG
- **Personas:** PER-BusinessAnalyst
- **Acceptance:**
  1. Given a test expecting `approvals: 2`, when the rule changes so that it yields 1, then the test fails.
- **Verification:** CONF
- **Origin:** NEW

### FR-RULE-082 — Tests gate merges
- **Statement:** Merging a change set into a protected branch MUST run all rule tests affected by the change and MUST be blocked if any fails.
- **Rationale:** Regression safety.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-BusinessAnalyst, PER-ComplianceOfficer
- **Acceptance:**
  1. Given a failing affected test, when merge is requested, then it fails with `RULE.TESTS_FAILED`, listing the tests.
- **Verification:** CONF
- **Origin:** NEW

### FR-RULE-083 — Rule coverage
- **Statement:** The platform SHOULD report coverage: the rule blocks, table rows and validations exercised by tests.
- **Rationale:** Shows untested rules.
- **Priority:** Should · **Phase:** PH-2 · **Systems:** FRG
- **Personas:** PER-BusinessAnalyst
- **Acceptance:**
  1. Given a table with 12 rows and tests exercising 10, when coverage is reported, then 2 rows are listed as uncovered.
- **Verification:** CONF
- **Origin:** NEW
