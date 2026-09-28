---
id: CMP-09
title: "Comparison: Validation, Constraints and Rules"
status: complete
phase: P2
depends_on: [CMP-02, CMP-04, CMP-05]
sources: [UP, FU, LC, UB, UC, US, LS, UW]
---

# Comparison: Validation, Constraints and Rules

## 1. Question

How are data validity, business constraints and invariants expressed and enforced? When are they enforced (draft, publish, every commit), and how do rule outcomes combine?

## 2. Candidates

| KEY | Approach | Refs | Summary |
|---|---|---|---|
| UP | Type `validator_ref` → logic URI | CON-UP-016 | Validation as logic |
| FU | Schema-on-commit: `SCHEMA/{TYPE}` JSON Schema entity, cached, drafts bypass | CON-FU-020, HL-FU-002 | Versioned structural validation |
| LC | JSON Schema + pluggable business constraints (unique, mandatory relationship cardinality over pending + persisted, relationship type checks) at DRAFT_SAVE or FINAL_PUBLISH | CON-LC-015, 023, 024, HL-LC-004, 005 | Staged validation |
| UB | Types carry regex/masking/scale; BEL: INVARIANT/WARN over PREV/NEW, asset algebra with units, PIN external facts, merge-time flows, inherited constitutions (design) | CON-UB-013, 061, HL-UB-012 | Business-physics invariants |
| UC | UODS meta-schema; input JSON Schema (design); policy script | CON-UC-014, 017 | Definition validation |
| US | EOP invariants/derivations/guards (design); slice voting with veto (design) | CON-US-013, 027 | Formal model |
| LS | ValidationSlot, TransformInputSlot, DefaultValueSlot; mechanisms schema/prerequisite (design) | CON-LS-013, 022, 027 | Validation as slots in a pipeline |
| UW | Guardrail checklist on transition | CON-UW-022 | UX |

## 3. Dimension matrix

| Dimension | FU | LC | UB | UC | US | LS |
|---|---|---|---|---|---|---|
| Structural (JSON Schema) | ● | ● | type attrs | design | – | slot |
| Derived from semantic types | – | – | ● | ● | ● | ◐ |
| Cross-entity / graph constraints | – | ● | BEL flows | – | – | – |
| Invariants over before/after | – | – | ● (design) | – | ● (design) | – |
| Levels (draft vs publish) | bypass drafts | ● | – | – | – | – |
| Warnings (soft) | – | – | ● WARN | – | – | – |
| Outcome combination | first error | collect | reject/tag | bool | ● vote/veto | list of results |
| Units / precision safety | – | – | ● assets | scale | – | – |

## 4. Analysis

- **Four validation layers** appear across the repositories. No repository has all four.
  - (1) *Definition validation*: UODS (UC).
  - (2) *Structural instance validation*: JSON Schema (FU, LC), best derived from semantic types (UB/UC).
  - (3) *Business constraints across entities*: LC's constraints on relationship cardinality and uniqueness, over staged and persisted data.
  - (4) *Invariants* over the transition from PREV to NEW (UB BEL, US EOP), including unit-safe money arithmetic.
- **Timing.**
  - LC's DRAFT_SAVE/FINAL_PUBLISH levels are the clearest timing model.
  - FU's draft bypass is a special case of it.
- **Combining outcomes.**
  - US's decision objects (allow, reason, priority, veto) give a principled way to combine many rule sources.
  - LS's result lists and LC's collected errors are simpler versions of the same idea.

## 5. Verdicts

### VRD-09-01 — Instance schema is derived from type definitions, validated on commit (fusion FU + UB/UC)
- **Decision:** fusion.
- **Consequences:**
  - The kernel compiles a JSON Schema per type commit from `properties` and semantic types (regex, scale, min/max, required, enum via `dict_code`).
  - The schema is cached by type commit.
  - An explicit JSON Schema override is allowed (FU).
  - Validation runs inside the unit of work before flush.

### VRD-09-02 — Validation levels (best-of LC)
- **Decision:** best-of LC.
- **Consequences:**
  - Every rule declares `level` ∈ {DRAFT, PUBLISH, ALWAYS}.
  - Draft branches run DRAFT and ALWAYS rules; publish and main-branch writes run all rules.

### VRD-09-03 — Business constraints as pluggable rule entities (fusion LC + LS)
- **Decision:** fusion.
- **Consequences:**
  - Built-in constraint kinds: unique, required relationship with cardinality (pending + persisted, validity-aware), allowed relationship endpoints, reference integrity.
  - Custom constraints are Logic implementations bound to the validation slot of a type or relation type.

### VRD-09-04 — Invariants over PREV/NEW with hard and soft outcomes (adapt UB BEL + US EOP)
- **Resolution (Q-007, 2026-09-28):** the expression language is a restricted Rhai subset (ADR-002).
- **Decision:** adapt.
- **Consequences:**
  - Types may declare `invariants [{name, expression, severity: error | warn, level}]` in a restricted, side-effect-free expression language evaluated with `prev` and `new`.
  - Warnings tag the commit metadata.
  - Money and quantity use unit-safe decimal semantics (UB asset algebra).
  - Full BEL (merge-time flows, oracle pins, constitutions) is left for a later spec version.
- **Open:** Q-007.

### VRD-09-05 — Rule outcomes are decisions combined with veto and priority (best-of US design)
- **Decision:** best-of US design.
- **Consequences:**
  - Every validation, permission or prerequisite source returns `{allow, reason, priority, veto}`.
  - Any veto with deny aborts the operation.
  - All reasons are collected and returned with paths for the UI (CMP-13).

### VRD-09-06 — Guards on transitions reuse the same rule machinery (fusion UB/US/UW)
- **Decision:** fusion.
- **Consequences:** FSM transition guards (VRD-06-03) are rule expressions or rule entities. The UI shows unmet guards as a checklist (UW).

## 6. Open questions

- Q-007: Choice of the restricted expression language for invariants and guards: the script language subset (Rhai expression), CEL, or JSONLogic? It must be sandboxed and analysable for AI.
