---
id: CMP-05
title: "Comparison: Logic Execution"
status: complete
phase: P2
depends_on: [CMP-02, CMP-04]
sources: [UP, FU, LC, UB, UC, US, LS]
---

# Comparison: Logic Execution

## 1. Question

How is business logic authored, stored, loaded, sandboxed and executed? How does it access kernel capabilities without redeployment?

## 2. Candidates

| KEY | Approach | Refs | Summary |
|---|---|---|---|
| UP | Groovy logic entities, "immune system" sandbox, content-hash compile cache, versioned kernel API, time-travel execution | CON-UP-020…024, 029, HL-UP-004, 006, 007, 012 | The most mature sandbox and API versioning |
| FU | Same engine as UP | – | – |
| LC | Handler library in code; pipelines configured as data | CON-LC-005, 025, 060 | "Two-speed" model: code handlers + declarative composition |
| UB | Action → Logic (Groovy) indirection; script context SDK | CON-UB-029, 030, HL-UB-013 | Public typed operation separate from versioned implementation |
| UC | Rhai logic entities; syscall ABI; thread-local context; op limits; DB module import | CON-UC-004, 005, 013, 029, 030, HL-UC-003 | Scripts are user space, the kernel is reached through `sys_*` |
| US | UC + session-backed syscalls; sync executor; scripts as line arrays | CON-US-003, 018, 021 | Transactional semantics for scripts |
| LS | Behaviour slots; scripts emit and receive result lists; `native::` functions | CON-LS-014, 021, 061, HL-LS-003 | Logic attached to relation types, generic processes |

## 3. Dimension matrix

| Dimension | UP | LC | UB | UC | US | LS |
|---|---|---|---|---|---|---|
| Logic stored as versioned entity | ● | – (code) | ● | ● | ● | ● |
| Hot change without deploy | ● | process only | ● | ● | ● | ● |
| Sandbox: AST / resource limits | ● AST + interrupt + timeout | n/a | – | ● op limit | ● op limit | – |
| Capability surface defined | kernel API v1/v2 | beans | script context | syscalls | syscalls | ctx.emit + native |
| API versioning | ● | handler `_V1` | – | – | – | – |
| Compiled cache | ● hash | n/a | – | – | – | commit-keyed |
| Time-travel execution | ● | – | – | anchors | – | – |
| Public contract separate from implementation | – | process catalogue | ● action→logic | input_schema | action→logic | slot→behaviour |
| Purity (side effects as returned intents) | – | – | – | – | – | ● emits |
| Performance / embeddability | JVM | JVM | JVM | ● Rust | ● | ● |

## 4. Analysis

- **Logic as data** is shared by all repositories except LC. LC's point still holds: a library of well-tested native handlers, composed by data, is safer for core mechanics (HL-LC-001).
- **Safety.**
  - UP's layered sandbox has AST restrictions, interrupt injection, a timeout and a compile cache (HL-UP-006).
  - UC/US have only an operation counter.
  - Both are needed; which one applies depends on the stack (ADR-001).
- **Capability surface.**
  - UC's syscall ABI (HL-UC-003) and UP's versioned kernel API (HL-UP-012) are the same idea at different maturity; they should combine as a *versioned* syscall ABI.
  - UB's script context and LS's `native::` namespace are further instances of it.
- **Where writes go.** In UC scripts, writes go straight to the store. In US they are staged in the session. In LS they are returned as intents (`emits`, `Mutate` instructions). *Interpretation:* staged writes (VRD-04-02) plus returned emits for cross-entity signals give the most analysable scripts.
- **Contract vs implementation.** UB's Action → Logic indirection (HL-UB-013) and LS's slot → Behavior both decouple a stable public contract from versioned implementations.

## 5. Verdicts

### VRD-05-01 — Logic entity contract, language-tagged (fusion UP/UC/US)
- **Decision:** fusion.
- **Consequences:**
  - Logic entity = `{language, api_version, script (string or line array), input_schema (JSON Schema), output_schema, purity: pure | effectful, timeout, limits}`.
  - Primary language is fixed by ADR-001.
  - Scripts are addressable and importable as modules by URI (UC import).

### VRD-05-02 — Versioned syscall ABI is the only capability surface (fusion UC + UP)
- **Decision:** fusion.
- **Consequences:**
  - Capabilities are grouped in namespaces: `db` (get, query, history, stage), `rel`, `event` (emit), `job` (invoke async/sync), `file`, `ai`, `memory`, `ui` (push), `log`, `time`, `crypto`.
  - Each namespace is versioned (`v1`, `v2`); old versions stay available (UP).
  - Identity and tenant are implicit from the execution context (UC thread-local).
  - Every call is policy-checked and auditable.

### VRD-05-03 — Layered sandbox (fusion UP + UC)
- **Decision:** fusion.
- **Consequences:**
  - Static checks at commit (compile, forbidden constructs).
  - Runtime limits: operations, wall-clock timeout with cancellation, memory, call depth.
  - Compile cache keyed by logic commit id / content hash.
  - Compile errors are returned by a syntax-check endpoint (US).

### VRD-05-04 — Public operations bind to implementations indirectly (fusion UB + LS)
- **Decision:** fusion.
- **Consequences:**
  - Actions (public, typed, permissioned) reference Logic by URI or by property reference (`logic_ref`).
  - Behaviour slots on relation types reference Behavior/Logic implementations.
  - Consumers never reference a script body directly.

### VRD-05-05 — Native handlers for kernel mechanics, registered by URI (best-of LC, adapted)
- **Decision:** best-of LC, adapted.
- **Consequences:**
  - Core steps (persist, validate, authorize, idempotency, index, notify) are native handlers with URIs (`ubos://system/Native/persist@v1`), usable in pipelines exactly like script logic.
  - This is LC's "two-speed" extension model.

### VRD-05-06 — Deterministic, replayable execution (fusion UP time-travel + UC anchors + LS emits)
- **Decision:** fusion.
- **Consequences:**
  - A process records the logic and context commits used (VRD-04-05).
  - Any logic can be executed as of a given commit (dry run).
  - Scripts return cross-entity effects as emits, which the kernel dispatches.
  - A dry-run mode runs logic with staging only and never flushes.
