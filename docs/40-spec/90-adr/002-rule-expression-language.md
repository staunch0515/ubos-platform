---
id: ADR-002
title: "ADR-002: Rule expression language"
status: complete
phase: P3
depends_on: [CMP-09, ADR-001]
sources: [UB, US, LC, LS, UC, UW]
---

# ADR-002: Rule expression language

- **Status:** accepted (2026-09-28). The user chose a Rhai subset (Q-007).

## Context

- Invariants over PREV/NEW (VRD-09-04), guards on transitions (VRD-09-06), policy conditions (VRD-10-02) and hook filters (VRD-11-02) need a small expression language.
- Such expressions MUST be:
  - side-effect free;
  - deterministic (no clock, random numbers or I/O);
  - bounded in cost;
  - statically checkable at definition commit;
  - readable by AI and by people.
- Corpus approaches:
  - UB BEL is a designed business-physics language with INVARIANT/WARN, asset algebra and pinned facts (CON-UB-061, HL-UB-012). It has no implementation.
  - US EOP is a designed calculus (CON-US-013).
  - LC constraints are Java classes (CON-LC-024).
  - UW guardrails are hard-coded checklists (CON-UW-022).
- Full Rhai is already the Logic language (ADR-001).

## Options considered

### Option A — restricted Rhai subset
- **Pros:**
  - One language for authors, editors (Monaco grammar) and AI prompts.
  - Same engine and limits as Logic.
  - Rhai lets the host disable features and register only chosen functions, so the subset is enforced by engine configuration, not by convention.
- **Cons:**
  - Static analysis must be written by us: an AST walk that rejects forbidden nodes.
  - The subset is less standard than CEL.

### Option B — CEL (Common Expression Language)
- **Pros:** designed for exactly this, with a standard specification and cost estimation.
- **Cons:**
  - A second language beside Rhai.
  - Rust implementations are less mature.
  - Unit-safe decimal semantics need extensions.

### Option C — JSONLogic
- **Pros:** pure data, trivially safe, easy to generate.
- **Cons:** poor readability for non-trivial rules; weak typing; decimals are awkward.

## Decision

Option A. The **rule subset** of Rhai is defined as follows (SPEC-18 is normative for the details):

| Aspect | Rule |
|---|---|
| Form | A single expression, or a block of `let` bindings followed by one final expression. The result is `bool`, or a Decision map `#{allow, reason, priority, veto}` (VRD-09-05). |
| Allowed | literals, arrays, maps; `let`; `if`/`else` expressions; comparison, logical and arithmetic operators; property access and indexing; `in`; closures only as arguments to registered pure functions (`all`, `any`, `sum`, `count`, `map`, `filter`) |
| Forbidden | `fn` definitions, `import`, loops (`for`, `while`, `loop`), assignment to anything outside a local `let`, `eval`, string interpolation of code, `print`/`debug`, custom operators |
| Variables | `prev` and `new` (effective snapshots, `()` when absent), `ctx` (read-only: `principal`, `tenant`, `branch`, `now` fixed at process start), `params` (action or policy inputs) |
| Functions | a registered pure library: decimal and unit-safe money (`dec`, `money`, `convert`), dates, strings, regex match, `len`, `lookup(uri, key)` (read-only, pinned to the process read set and recorded as provenance), `changed(path)` |
| Limits | max operations 10 000, max expression depth 32, max string 64 KiB, max array/map 10 000 items, no timeout needed beyond the operation limit |
| Checking | Rule definitions are parsed and AST-checked at definition commit. A violation fails with `RULE.EXPRESSION_INVALID` and a path. |
| Evaluation | A shared, pre-configured Rhai engine with the subset flags. Compiled ASTs are cached by definition commit ID. |

## Consequences

- SPEC-18 defines invariants, guards and decision combination on this subset. SPEC-22 uses it for policy conditions and SPEC-20 for hook filters.
- The Monaco language support gains a "rule" mode that marks forbidden constructs.
- Full BEL features (merge-time flows, oracle pins, constitutions) remain deferred (VRD-09-04).
- A later ADR may add CEL as a second `language` for rules without changing the rule entity shape.

## Origin

Q-007, VRD-09-04, VRD-09-05, VRD-09-06, VRD-10-02, VRD-11-02, CON-UB-061, CON-US-013, HL-UB-012.
