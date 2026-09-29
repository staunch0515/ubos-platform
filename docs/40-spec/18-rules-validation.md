---
id: SPEC-18
title: "Rules and Validation"
status: complete
phase: P4
depends_on: [SPEC-02, SPEC-13, SPEC-15, SPEC-17, ADR-002]
sources: [FU, LC, UB, UC, US, LS, UW]
---

# Rules and Validation

## 0. Chapter header

- **Scope:** This chapter covers:
  - instance schemas derived from type definitions;
  - validation levels;
  - the rule sources and their evaluation order inside the pre-flush pipeline;
  - built-in and custom constraints (Constraint entities);
  - invariants over PREV/NEW in the Rhai rule subset (ADR-002);
  - the Decision model and how decisions combine (veto, priority, overrides);
  - transition guards and checklists;
  - reference integrity;
  - error and warning formats with paths;
  - on-demand validation for clients.
- **MOD:** `RULE`
- **depends_on:** SPEC-02, SPEC-13, SPEC-15, SPEC-17, ADR-002.
- **Terms used:** TERM-DerivedSchema, TERM-ValidationLevel, TERM-Constraint, TERM-Invariant, TERM-Decision, TERM-Guard, TERM-Slot.
- **Origin summary:**
  - Verdicts: VRD-09-01…06.
  - FU: schema-on-commit (CON-FU-020).
  - LC: staged levels and pluggable constraints over pending plus persisted changes (CON-LC-023, CON-LC-024), SpEL decisions (CON-LC-021).
  - UB: BEL invariants and asset algebra (CON-UB-061, HL-UB-012).
  - US: the EOP calculus, the Decision voting protocol and hot-swappable rules (CON-US-013, CON-US-027, CON-US-062).
  - UC: UODS strictness and strict input wiring (CON-UC-017, CON-UC-014).
  - LS: SchemaProvide slots and blocking mechanisms (CON-LS-052, CON-LS-027).
  - UW: the guardrail checklist (CON-UW-022).

---

## 1. Concepts

### 1.1 Four layers of validation

| Layer | Question | Source | When |
|---|---|---|---|
| L1 Definition | Is the *definition* well formed? | meta-schema (SPEC-13 REQ-META-003) | commit of a definition |
| L2 Structure | Does each entity match its type? | derived JSON Schema + property `validate` slots | pre-flush, per entity |
| L3 Constraints | Do the entities fit together? (uniqueness, required relationships, references) | Constraint entities (built-in kinds or custom logic) | pre-flush, over the whole unit of work plus the store |
| L4 Invariants | Is the *transition* PREV → NEW allowed? (business physics) | type `invariants` in the rule subset | pre-flush, per changed entity |

All four layers return **Decisions**, which are combined by one algorithm (§3.5). Guards on transitions (SPEC-19) and policy conditions (SPEC-22) reuse the same machinery (VRD-09-06).

### 1.2 Levels

| Process level | Rules with `level = ALWAYS` | Rules with `level = PUBLISH` |
|---|---|---|
| DRAFT (draft branches, REQ-TX-003) | run | skipped |
| PUBLISH (all other writes, merges into MAIN/RELEASE) | run | run |

In the derived schema, value checks (type, format, length, range, pattern) are ALWAYS, and `required` is PUBLISH. This lets incomplete objects be saved as drafts (LC DRAFT_SAVE / FINAL_PUBLISH).

---

## 2. Model

```yaml
ENT-InvariantSpec:
  purpose: One invariant on an entity type (inline element of Type.invariants / TypeExtension.invariants).
  origin: [VRD-09-04, CON-UB-061, CON-US-013, ADR-002]
  fields:
    - {name: name, type: string(64), required: true, description: "Unique within the effective type (snake_case)"}
    - {name: expression, type: rhai, required: true, description: "Rule-subset expression (ADR-002) returning bool or a Decision map"}
    - {name: message, type: string, required: true, description: "Message template; ${path} placeholders from new/prev"}
    - {name: severity, type: "enum{ERROR|WARN}", required: true, description: "ERROR rejects; WARN is recorded as a warning"}
    - {name: level, type: "enum{ALWAYS|PUBLISH}", required: true, description: "When it runs (§1.2)"}
    - {name: on, type: "list<enum{CREATE|UPDATE|DELETE}>?", required: false, description: "Actions it applies to; default [CREATE, UPDATE]"}
    - {name: when_changed, type: "list<string>?", required: false, description: "Only evaluate if one of these property paths changed"}
    - {name: path, type: string?, required: false, description: "JSON Pointer reported with a failure (default '')"}
    - {name: priority, type: int32?, required: false, description: "Decision priority (default 100)"}
    - {name: veto, type: bool?, required: false, description: "Decision veto flag (default false)"}
```

```yaml
ENT-Constraint:
  purpose: A pluggable business rule entity (TERM-Constraint).
  origin: [VRD-09-03, CON-LC-024, CON-LS-027, CON-US-062]
  fields:
    - {name: kind, type: "enum{UNIQUE|REQUIRED_RELATIONSHIP|RELATIONSHIP_ENDPOINTS|REFERENCE_INTEGRITY|CARDINALITY|CUSTOM}", required: true, description: "Built-in kind or CUSTOM"}
    - {name: target_type, type: "uri<Type>", required: true, description: "Entity type it applies to (and subtypes)"}
    - {name: config, type: json, required: true, description: "Kind-specific configuration (§2.1)"}
    - {name: impl, type: "uri?", required: false, description: "CUSTOM only - Logic (effect READ) or Native returning Decision(s)"}
    - {name: level, type: "enum{ALWAYS|PUBLISH}", required: true, description: "When it runs"}
    - {name: severity, type: "enum{ERROR|WARN}", required: true, description: "Effect of a failure"}
    - {name: priority, type: int32?, required: false, description: "Default 100"}
    - {name: veto, type: bool?, required: false, description: "Default false"}
    - {name: message, type: string, required: true, description: "Message template"}
    - {name: enabled, type: bool?, required: false, description: "Default true; disabling is a versioned change (hot swap, CON-US-062)"}
  invariants:
    - A Constraint applies when the type's effective rule_refs contain it, or when it is attached by a TypeExtension.
```

### 2.1 Built-in constraint kinds

| Kind | `config` | Checks | Origin |
|---|---|---|---|
| UNIQUE | `{paths: [..], scope: TENANT\|BRANCH, case_insensitive?: bool, include_subtypes?: bool}` | No other ACTIVE entity of the type (and subtypes) visible on the branch chain, or in the unit of work, has equal values at all paths | CON-LC-024 (isUnique), property `unique` |
| REQUIRED_RELATIONSHIP | `{relationship_type, direction: SOURCE\|TARGET, min_count, max_count?, at?: NOW\|VALID_FROM}` | Count of relationships of that type for the entity, from staged plus persisted ones, validity-aware, within bounds | CON-LC-024 (mandatoryRelationships) |
| RELATIONSHIP_ENDPOINTS | `{}` (reads the RelationshipType) | Relationship source and target types are allowed; validity rules; metadata schema | CON-LC-024 (RelationshipEntityCheck), REQ-META-031 |
| REFERENCE_INTEGRITY | `{paths?: [..]}` | Reference values resolve on the branch to an entity of the declared target type | REQ-META-030 |
| CARDINALITY | `{path, min?, max?}` | List length at a path within bounds | NEW |
| CUSTOM | `{…}` passed to `impl` | Anything, returning Decision(s) | VRD-09-03 |

```yaml
ENT-Decision:
  purpose: Outcome of one rule evaluation (TERM-Decision).
  origin: [VRD-09-05, CON-US-027, CON-LS-027]
  fields:
    - {name: allow, type: bool, required: true, description: "Rule satisfied"}
    - {name: severity, type: "enum{ERROR|WARN|INFO}", required: true, description: "Effect when allow=false"}
    - {name: code, type: string, required: true, description: "Error code, e.g. RULE.UNIQUE, RULE.INVARIANT, or a tenant code ACME.*"}
    - {name: message, type: string, required: true, description: "Rendered message (locale of the principal)"}
    - {name: path, type: string, required: true, description: "JSON Pointer into the entity ('' for whole entity)"}
    - {name: target, type: uri, required: true, description: "Entity concerned"}
    - {name: source, type: string, required: true, description: "Rule source - type URI#invariant, Constraint URI, slot binding, guard, policy"}
    - {name: priority, type: int32, required: true, description: "100 normal, 500 business critical, 900 security, 999 constitutional"}
    - {name: veto, type: bool, required: true, description: "A denying veto aborts at once and cannot be overridden"}
    - {name: overrides, type: "list<string>?", required: false, description: "Sources of lower-priority denials this allow cancels (REQ-RULE-031)"}
```

```yaml
ENT-GuardSpec:
  purpose: Preconditions of a transition or action (TERM-Guard), shown as a checklist.
  origin: [VRD-09-06, CON-UW-022, CON-LS-027]
  fields:
    - {name: name, type: string(64), required: true, description: "Identifier"}
    - {name: title, type: string, required: true, description: "Checklist label (UW guardrail)"}
    - {name: expression, type: "rhai?", required: false, description: "Rule-subset expression over entity, params, principal"}
    - {name: rule, type: "uri?", required: false, description: "Or a Constraint to evaluate"}
    - {name: severity, type: "enum{ERROR|WARN}", required: true, description: "ERROR blocks the transition"}
    - {name: priority, type: int32?, required: false, description: "Default 100"}
    - {name: veto, type: bool?, required: false, description: "Default false"}
  invariants:
    - Exactly one of expression and rule.
```

---

## 3. Behavior

### 3.1 Derived schemas

### REQ-RULE-001 — Schema derivation
- **Statement:** For every effective entity type, the kernel MUST derive a JSON Schema (draft 2020-12) as follows:
  1) `type: object`. `additionalProperties: false`, unless the type is `open`. Reserved keys (REQ-CONV-023) are allowed.
  2) For each effective property:
     - the schema fragment from its `schema` slot, whose default is `schema.from_constraints@v1` (REQ-META-015);
     - `type` from the primitive (§2.7 of SPEC-13);
     - the constraints: pattern, minimum/maximum, maxLength, format, enum from `values`, or from the `dict` entries resolved at derivation time;
     - for `many: true`, the fragment is wrapped in `{type: array, items: …}`;
     - for references, `{type: string, format: ubos-uri}`.
  3) `required` lists the required properties, and is applied at PUBLISH only (REQ-RULE-002).
  4) The type's `schema_extra` (SPEC-13) is combined with `allOf` (FU's explicit schema).

  The schema is cached by chain fingerprint (REQ-META-040). A version records the `type_commit_id` it was validated against (REQ-VER-001).
- **Origin:** VRD-09-01, CON-FU-020, CON-UC-017, CON-LS-052, CON-LC-015
- **Acceptance:**
  1) A type with an `Email` property rejects `"x@"` at both levels.
  2) A missing required property is accepted at DRAFT and rejected at PUBLISH.
  3) `meta.schema {type}` returns the derived schema.
- **Priority:** P0

### REQ-RULE-002 — Level-dependent structural validation
- **Statement:** Structural validation MUST run the derived schema without `required` at DRAFT level, and with `required` at PUBLISH level. It MUST also run the property `validate` slots (CHAIN, REQ-META-011) on every **present** value. Both run on the **own data composed with defaults** of each staged entity:
  - absent inherited keys count as present if an ancestor layer supplies them;
  - `required` is satisfied by effective data, not only by own data.
- **Rationale:** An instance that inherits a required value from its template is valid (SPEC-13 prototype chain).
- **Origin:** VRD-09-01, VRD-09-02, CON-LC-023
- **Acceptance:** A Page that inherits a required `book_title` from its Book template passes PUBLISH validation without storing it.
- **Priority:** P0

### 3.2 Evaluation order

### REQ-RULE-010 — Rule pipeline
- **Statement:** The `validate` step of the pre-flush pipeline (REQ-TX-010 step 4) MUST evaluate the rules in this order, collecting all Decisions:
  1) structural (REQ-RULE-002), per staged entity;
  2) reference syntax and, at PUBLISH, REFERENCE_INTEGRITY for reference properties (REQ-META-030);
  3) constraints that apply to the staged entities' types, per entity or per unit of work as the kind requires (REQ-RULE-020);
  4) invariants of changed entities (REQ-RULE-025);
  5) combination (REQ-RULE-030).

  Evaluation of steps 3 and 4 is skipped for an entity whose step 1 produced an ERROR at that entity's root. Each rule sees:
  - **NEW:** the effective data of the staged entity, with all staged changes of the process applied;
  - **PREV:** the effective data of the loaded version, or `()` for CREATE;
  - the **unit of work:** all staged entries (LC "validators see all pending changes").
- **Origin:** VRD-09-01…05, CON-LC-024, CON-LS-027
- **Acceptance:** A quiz and its required owner relationship created in the same Mutate pass REQUIRED_RELATIONSHIP.
- **Priority:** P0

### REQ-RULE-011 — Purity of rules
- **Statement:** Rule evaluation MUST be side-effect free:
  - Invariants and guards run in the RULE profile.
  - CUSTOM constraints and `validate` slots run in PURE.
  - Reads they perform are recorded as provenance (DEPENDS_ON_DATA).
  - Rules MUST NOT stage changes or emit. An attempt fails with `RT.CAPABILITY_DENIED`.
- **Origin:** CON-UB-001 (ESE pure), ADR-002, REQ-RT-005
- **Acceptance:** A custom constraint calling `entity.update` fails and the process is FAILED.
- **Priority:** P0

### 3.3 Constraints

### REQ-RULE-020 — Built-in constraint semantics
- **Statement:** The built-in kinds (§2.1) MUST be evaluated over **staged plus persisted** state on the process's branch chain (SPEC-14 §1.2):
  - **UNIQUE:**
    - Look up the search index (`val_*` at the paths) for ACTIVE heads resolvable on the branch chain, excluding the entity itself.
    - Also check the staged entries.
    - Properties used by UNIQUE MUST be `searchable`, and the meta-schema enforces this.
  - **REQUIRED_RELATIONSHIP:** count staged Relationship entries plus persisted ACTIVE ones whose validity contains `now` (or `valid_from` when `at: VALID_FROM`), and staged deletions subtract. Compare the count with the bounds.
  - **RELATIONSHIP_ENDPOINTS:** REQ-META-031.
  - **REFERENCE_INTEGRITY:** each reference resolves (REQ-URI-009) to an entity whose type is the target type or a subtype.
  - **CARDINALITY:** the list length is within bounds.
- **Origin:** VRD-09-03, CON-LC-024
- **Acceptance:** One test per kind, including a pending-plus-persisted case and a validity-expired relationship that is not counted.
- **Priority:** P0 (UNIQUE, REFERENCE_INTEGRITY), P1 (others)

### REQ-RULE-021 — Uniqueness under concurrency
- **Statement:** A UNIQUE check MUST be race-free:
  - **PostgreSQL:** during the flush, before inserting the versions, the kernel takes a transaction-scoped advisory lock on `hash(tenant, type root, paths, normalised values)` for every staged entity with a UNIQUE constraint. It then re-runs the index lookup. A violation fails the flush with `RULE.VALIDATION_FAILED` (cause `RULE.UNIQUE`).
  - **SQLite:** the exclusive write transaction already serialises flushes, and the re-check runs inside it.
- **Rationale:** Uniqueness checked only before the flush is not safe with concurrent writers.
- **Origin:** NEW: correctness of VRD-09-03 under concurrency
- **Acceptance:** 50 concurrent creates with the same unique email produce exactly 1 success.
- **Priority:** P0

### REQ-RULE-022 — Custom constraints
- **Statement:** A CUSTOM constraint MUST reference a Logic or Native whose output is a Decision, or a list of Decisions, per entity. The kernel calls it once per staged entity of the target type with `params = {new, prev, action, config, unit_of_work: [uri…]}`. The implementation may read other staged entities with `entity.get`, which sees the session. Missing Decision fields are defaulted (severity, priority and veto from the Constraint; target = the entity).
- **Origin:** VRD-09-03, CON-US-027
- **Acceptance:** A custom credit-limit constraint returns `allow=false, path=/total` and the error shows that path.
- **Priority:** P1

### REQ-RULE-023 — Hot-swappable rules
- **Statement:** Constraints, invariants and guards are versioned entities or definition fields. Changing, disabling or rolling one back MUST take effect for the next process, with no restart. Which rule versions a process used is visible in its provenance (DEPENDS_ON_TYPE for type invariants, DEPENDS_ON_DATA for Constraint entities).
- **Origin:** CON-US-062, VRD-05-06
- **Acceptance:** Disabling a Constraint (commit `enabled: false`) makes the next save pass, and reverting the commit re-enables it.
- **Priority:** P0

### 3.4 Invariants

### REQ-RULE-025 — Invariant evaluation
- **Statement:** For each staged entity whose action is in the invariant's `on`, and whose changed paths intersect `when_changed` (when given), the kernel MUST evaluate `expression` in the RULE profile:
  - The variables are `prev`, `new`, `ctx` (read-only: principal, tenant, branch, now), `params` (the operation arguments, or `()`) and `action`.
  - The functions are those of ADR-002 (decimal/money, dates, strings, regex, `len`, `all`/`any`/`sum`/`count`/`map`/`filter`, `lookup(uri, key)`, `changed(path)`).
  - A `bool` result becomes a Decision with the spec's fields. A map result is taken as the Decision, with defaults filled.
  - An evaluation error is an ERROR decision with code `RULE.INVARIANT_ERROR`.

  WARN failures do not reject. They are added to the process warnings and stored in the commit's `details.warnings` (VRD-09-04: "warnings tag the commit metadata").
- **Origin:** VRD-09-04, CON-UB-061, ADR-002
- **Acceptance:**
  1) `new.balance >= dec("0")` rejects an overdraft with a path.
  2) A WARN invariant on large discounts shows in `details.warnings` of the version.
- **Priority:** P0

### REQ-RULE-026 — Unit-safe money
- **Statement:** The rule library MUST treat money values as `money(amount, currency)` pairs. Arithmetic between different currencies fails with `RULE.UNIT_MISMATCH`. Measures with units (`unit` constraint) follow the same rule (UB asset algebra).
- **Origin:** VRD-09-04, CON-UB-061
- **Acceptance:** `money("10","EUR") + money("5","USD")` fails with `RULE.UNIT_MISMATCH`.
- **Priority:** P1

### 3.5 Decisions

### REQ-RULE-030 — Combination algorithm
- **Statement:** Given all Decisions *D* of a validation or guard evaluation, the kernel MUST combine them as follows:
  1) If some *d* ∈ *D* has `allow = false ∧ veto = true`: the result is **DENY**. Evaluation stops as soon as the first such decision appears (short-circuit). The reported causes are that decision plus those already collected.
  2) Remove every denial *d* for which some allow decision *a* has `a.priority > d.priority` and `d.source ∈ a.overrides` (REQ-RULE-031).
  3) If some remaining *d* has `allow = false ∧ severity = ERROR`: the result is **DENY**, with all such decisions as causes, sorted by priority (desc) and then path.
  4) Otherwise the result is **ALLOW**, with warnings = the remaining `allow = false ∧ severity = WARN` decisions.

  DENY in validation becomes `RULE.VALIDATION_FAILED`, with `causes` = the denying decisions as error objects (code, message, path, target, details.source) (REQ-CONV-050). The process is REJECTED.
- **Rationale:** US's voting protocol, made concrete: vetoes (security, constitution) dominate; otherwise every reason is collected for the UI.
- **Origin:** VRD-09-05, CON-US-027
- **Acceptance:**
  1) Two independent ERROR failures are both reported.
  2) A priority-999 veto stops evaluation.
  3) A priority-500 allow with `overrides: ["Constraint/credit.limit"]` cancels that constraint's denial.
- **Priority:** P0

### REQ-RULE-031 — Overrides
- **Statement:** Only CUSTOM constraints, invariants (through the map result) and guards MAY emit allow decisions with `overrides`. An override MUST name the exact source identifiers it cancels. It cannot cancel a veto. Every override that took effect is recorded in the process warnings, with code `RULE.OVERRIDDEN`, and in the trace.
- **Origin:** CON-US-027 (priority weighting), NEW: explicit, audited overrides
- **Acceptance:** An override of a veto has no effect. An applied override appears in the audit record.
- **Priority:** P2

### 3.6 Guards

### REQ-RULE-040 — Guard evaluation and checklists
- **Statement:** Evaluating the guards of a transition or action (SPEC-19) for target *e*, params *p* and principal *u* MUST:
  - evaluate each GuardSpec (expression in RULE, or referenced Constraint);
  - combine the results with REQ-RULE-030;
  - return a **checklist** `[{name, title, met: bool, severity, message?}]`, in declaration order.

  A DENY blocks the transition with `RULE.GUARD_FAILED` (causes = the unmet ERROR guards). Clients receive the checklist in the action's prepare stage, so they can show unmet guards before execution (UW guardrail).
- **Origin:** VRD-09-06, CON-UW-022, CON-LS-027, CON-UB-032
- **Acceptance:** A "resolve issue" transition with an unmet "QA sign-off" guard returns a checklist with `met: false`, and execution fails with `RULE.GUARD_FAILED`.
- **Priority:** P0

### 3.7 Reference integrity on delete

### REQ-RULE-045 — Target deletion behaviour
- **Statement:** When an entity is deleted (DELETE or UNSET on the branch where it was visible), the kernel MUST find referencing entities on the same branch chain through `search_index.val_ref`, and apply each reference property's `on_target_delete`:
  - **RESTRICT:** an ERROR decision `RULE.REFERENCED` listing up to 20 referrers.
  - **NULLIFY:** stage an update that sets the reference to `null` (or removes it from a list), using the cascade step of REQ-TX-010.
  - **IGNORE:** nothing. Later resolution returns `_error: URI.NOT_FOUND` for the dangling reference.

  Only references in searchable properties are found. The meta-schema requires `searchable: true` when `on_target_delete ≠ IGNORE`.
- **Origin:** NEW: completes VRD-02-03; CON-LS-013 (DependencyCheckSlot, CascadeUpdateSlot)
- **Acceptance:**
  1) Deleting a Customer referenced by an Invoice with RESTRICT fails and names the invoice.
  2) With NULLIFY, the invoice gets a new version with `customer: null` in the same process.
- **Priority:** P1

### 3.8 Client support

### REQ-RULE-050 — On-demand validation
- **Statement:** The query `ubos://system/Query/rule.validate@v1 {uri or type, data, action, level}` MUST run REQ-RULE-010 for a single entity as a dry evaluation, with no process log row. It returns `{result: ALLOW|DENY, decisions}`. Clients use it for field-level feedback, and MAY also pre-validate with the derived schema (`meta.schema`). The server remains authoritative (REQ-CONV-086).
- **Origin:** VRD-09-05, VRD-13-02, CON-LS-022 (stage-3 validation)
- **Acceptance:** A form shows the server's invariant message next to the field before submit.
- **Priority:** P1

### REQ-RULE-051 — Message rendering
- **Statement:** Rule messages MUST be rendered in the principal's locale:
  - an i18n key (`i18n:` prefix), when given, resolves through SPEC-27 i18n;
  - `${…}` placeholders are filled from `new`, `prev`, `params` and the Decision's fields, with masking applied to sensitive values.

  Raw messages are English (REQ-CONV-070).
- **Origin:** REQ-CTX-014, VRD-16-07
- **Acceptance:** A message `"Balance ${new.balance} below zero"` renders the masked value for a reader without permission.
- **Priority:** P1

---

## 4. Interfaces

### API-RULE-001 — RuleEngine
- **Kind:** rust trait (`ubos_kernel::rules`)
- **Signature / shape:**
```rust
#[async_trait]
pub trait RuleEngine: Send + Sync {
    async fn validate(&self, x: &ExecutionContext, s: &Session, level: Level) -> Result<Outcome, UbosError>; // REQ-RULE-010
    async fn guards(&self, x: &ExecutionContext, s: &Session, target: &str, guards: &[GuardSpec],
                    params: &Value) -> Result<(Outcome, Vec<ChecklistItem>), UbosError>;          // REQ-RULE-040
    async fn validate_one(&self, x: &ExecutionContext, req: ValidateRequest) -> Result<Outcome, UbosError>; // REQ-RULE-050
    fn derived_schema(&self, t: &EffectiveType, level: Level) -> Arc<serde_json::Value>;            // REQ-RULE-001
    fn combine(decisions: Vec<Decision>) -> Outcome;                                                 // REQ-RULE-030 (pure)
}
pub enum Outcome { Allow { warnings: Vec<Decision> }, Deny { causes: Vec<Decision>, warnings: Vec<Decision> } }
```
- **Origin:** CON-LC-024 (BusinessConstraintManager), CON-US-027

### API-RULE-002 — Named operations
- **Kind:** UBTP Query targets
- **Signature / shape:**

| URI | Args | Result |
|---|---|---|
| `ubos://system/Query/rule.validate@v1` | `{uri?, type?, data, action, level?}` | `{result, decisions}` |
| `ubos://system/Query/rule.guards@v1` | `{target, action, event, params?}` | checklist |
| `ubos://system/Query/rule.explain@v1` | `{process_id}` | the decisions of a past process (from the trace) |
- **Origin:** CON-UW-022, CON-US-061

### Example type invariants and constraint

```json
{
  "code": "acme.Account", "kind": "ENTITY", "_extends": ["ubos://logrums/Type/Entity"],
  "properties": [
    { "name": "number", "type": "ubos://logrums/Type/Code", "required": true, "unique": "TENANT", "searchable": true },
    { "name": "balance", "type": "ubos://logrums/Type/Money", "required": true },
    { "name": "credit_limit", "type": "ubos://logrums/Type/Money" }
  ],
  "invariants": [
    { "name": "no_overdraft", "severity": "ERROR", "level": "ALWAYS", "path": "/balance",
      "expression": "new.balance + (new.credit_limit ?? money(\"0\", new.balance.currency)) >= money(\"0\", new.balance.currency)",
      "message": "Balance would exceed the credit limit" },
    { "name": "large_change", "severity": "WARN", "level": "PUBLISH", "when_changed": ["/balance"],
      "expression": "prev == () || abs(new.balance.amount - prev.balance.amount) < dec(\"100000\")",
      "message": "Balance changed by more than 100 000" }
  ]
}
```

### Error codes (RULE)

| Code | Category | Meaning |
|---|---|---|
| `RULE.VALIDATION_FAILED` | INVALID | combined DENY; `causes` hold the decisions |
| `RULE.GUARD_FAILED` | INVALID | unmet guards; `details.checklist` |
| `RULE.SCHEMA` | INVALID (cause) | structural violation |
| `RULE.REQUIRED` | INVALID (cause) | required property missing at PUBLISH |
| `RULE.UNIQUE` | INVALID (cause) | uniqueness violated |
| `RULE.RELATIONSHIP_REQUIRED` | INVALID (cause) | required relationship count out of bounds |
| `RULE.REFERENCE` | INVALID (cause) | reference integrity |
| `RULE.REFERENCED` | INVALID (cause) | delete restricted by referrers |
| `RULE.INVARIANT` | INVALID (cause) | invariant failed |
| `RULE.INVARIANT_ERROR` | INVALID (cause) | invariant evaluation error |
| `RULE.UNIT_MISMATCH` | INVALID (cause) | money or unit mismatch |
| `RULE.EXPRESSION_INVALID` | INVALID | rule expression violates ADR-002 (at definition commit) |
| `RULE.OVERRIDDEN` | (warning) | an override took effect |

---

## 5. Non-functional

| ID | Requirement | Target |
|---|---|---|
| NFR-PERF-080 | Structural validation of an entity with 30 properties (cached schema) | p95 ≤ 200 µs |
| NFR-PERF-081 | Invariant evaluation (cached AST) | p95 ≤ 50 µs per invariant |
| NFR-PERF-082 | UNIQUE check with index | p95 ≤ 2 ms (PG) |
| NFR-UX-080 | All validation errors in one response | 100 % of independent failures reported together |

---

## 6. Acceptance

| REQ | Criterion |
|---|---|
| REQ-RULE-001, 002 | Derived schema content; level behaviour; inherited required values |
| REQ-RULE-010, 011 | Order and visibility of pending changes; purity enforcement |
| REQ-RULE-020…023 | Built-in kinds; concurrency uniqueness; custom constraint paths; hot swap via versions |
| REQ-RULE-025, 026 | Invariant ERROR/WARN with commit warnings; money unit safety |
| REQ-RULE-030, 031 | Combination cases: multi-error, veto short-circuit, override |
| REQ-RULE-040 | Guard checklist and blocking |
| REQ-RULE-045 | RESTRICT / NULLIFY / IGNORE behaviours |
| REQ-RULE-050, 051 | On-demand validation; localised, masked messages |

---

## 7. Implementation notes

- **Reference code:**
  - LC constraint manager and validators [LC:src/main/java/com/logicorum/validation/BusinessConstraintManager.java], levels [LC:src/main/java/com/logicorum/model/ValidationLevel.java].
  - FU validator [FU:backend/…/JsonSchemaValidator] (CON-FU-020).
- **Design references:**
  - UB BEL [UB:docs/BEL Pro.md].
  - US voting [US:docs/front/FullSlice.md].
- **JSON Schema:** use the `jsonschema` crate with draft 2020-12. Register a custom format `ubos-uri` that checks the grammar only. Existence is checked by REFERENCE_INTEGRITY.
- **Search-index dependency:** UNIQUE and RESTRICT/NULLIFY rely on searchable paths. The meta-schema forces `searchable: true` for such properties (SPEC-13 REQ-META-004 extended).
- **BEL features not included** (deferred): merge-time flows, oracle pins, constitution inheritance and identity-based packet guards (CON-UB-061). They can be layered later on the same Decision model.

## 8. Open questions

None. Deferred to later versions: the full BEL feature set (VRD-09-04).
