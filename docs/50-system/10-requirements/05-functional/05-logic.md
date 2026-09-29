---
id: UBS-REQ-05-05
title: Functional Requirements — LOGIC (Instruction Set, Runtime, Sandbox)
status: complete
phase: PH-1
depends_on: [UBS-REQ-05, UBS-REQ-05-04]
---

# Functional Requirements — LOGIC

## CAP-LOGIC-01 — Instruction set

### FR-LOGIC-011 — Seven instruction families
- **Statement:** The DVM MUST expose business-logic capabilities only as instructions grouped into seven families:
  - `commit` (write);
  - `read` (read state and history);
  - `run` (invoke ports, logic and decisions);
  - `emit` (events and notifications);
  - `search` (queries);
  - `call` (connectors);
  - `ask` (AI models).

  Each instruction MUST declare its name (`family.operation`), ABI version, parameter and result schemas, error codes, determinism class and metering cost.
- **Rationale:** A small, fixed instruction set is the "hardened mechanism" of the BPA (EXT-BPU, EXT-TRI "five syscalls").
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM, BPA
- **Personas:** PER-LogicDeveloper, PER-ThirdPartyImplementer
- **Acceptance:**
  1. Given the published instruction catalogue, when compared with the runtime's registered instructions, then they match exactly, and every instruction belongs to one family.
- **Verification:** CONF, INSP
- **Origin:** EXT-TRI, EXT-RFC, EXT-BPU, L40:SPEC-17

### FR-LOGIC-012 — ABI versioning and compatibility
- **Statement:** The instruction ABI MUST be versioned as `MAJOR.MINOR`. Minor versions MUST only add instructions or optional parameters. A major version MUST remain supported for at least 24 months after its successor is released. Every logic asset MUST declare the ABI version it targets.
- **Rationale:** Logic written today must run for years, the way software runs on a fixed processor instruction set.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM, BPA
- **Personas:** PER-LogicDeveloper, PER-IsvDeveloper
- **Acceptance:**
  1. Given an asset targeting ABI 1.2 on a kernel supporting 1.5, when run, then it behaves as on 1.2.
  2. Given an asset targeting 1.6 on a 1.5 kernel, when loaded, then it fails with `LOGIC.ABI_UNSUPPORTED`.
- **Verification:** CONF
- **Origin:** L40:SPEC-17

### FR-LOGIC-013 — Minimum instruction catalogue
- **Statement:** ABI 1.0 MUST include at least the following instructions:

  | Family | Instructions |
  |---|---|
  | `commit` | `create`, `update`, `end`, `append`, `reverse`, `adjust`, `link`, `unlink`, `timeline` |
  | `read` | `get`, `history`, `timeline`, `diff`, `effective`, `explain` |
  | `run` | `port`, `super`, `logic`, `decision`, `validate` |
  | `emit` | `event`, `notify` |
  | `search` | `query`, `aggregate`, `text`, `vector`, `traverse` |
  | `call` | `connector` |
  | `ask` | `model` |

  `commit.timeline` covers the operations of FR-TIME-022. `search.text` and `search.vector` MAY return `LOGIC.NOT_AVAILABLE` before their capability phases (PH-2 and PH-3).
- **Rationale:** An explicit minimum makes conformance testable.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM, BPA
- **Personas:** PER-ThirdPartyImplementer
- **Acceptance:**
  1. Given the conformance vectors for each listed instruction, when run on the reference DVM, then all pass.
- **Verification:** CONF
- **Origin:** L40:SPEC-17, EXT-TRI

### FR-LOGIC-014 — Instructions are the only effect path
- **Statement:** Logic MUST NOT reach any resource (storage, network, file system, clock, randomness, environment, other tenants) except through instructions and the context. Authorization MUST be enforced at every instruction.
- **Rationale:** Complete mediation.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-SecurityOfficer
- **Acceptance:**
  1. Given the sandbox escape test suite, when run against L2 and L3 runtimes, then no test reaches a resource outside the instructions.
- **Verification:** SEC, CONF
- **Origin:** EXT-RFC, L40:SPEC-17

### FR-LOGIC-015 — One ABI for all tiers and hosts
- **Statement:** L2 scripts, L3 components and in-process SDK hosts MUST use the same instruction semantics and error codes.
- **Rationale:** Logic moves between tiers without behaviour change.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM, SDK
- **Personas:** PER-LogicDeveloper
- **Acceptance:**
  1. Given the same instruction vector executed from L2 and (in PH-3) L3, when compared, then the results are identical.
- **Verification:** CONF
- **Origin:** IMP-05

## CAP-LOGIC-02 — Execution context

### FR-LOGIC-021 — Immutable context
- **Statement:** Every execution MUST receive an immutable context `ctx` containing:
  - `principal`, `on_behalf_of`, `tenant`, `vae` and `branch`;
  - `asof`, `time` and `now`;
  - `locale` and `time_zone`;
  - `process_id`, `trigger` and `input`;
  - `profile` and `budget_remaining`.
- **Rationale:** A clean separation of input (`ctx`) and capability (`sys`) (EXT-TRI Book III).
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-LogicDeveloper
- **Acceptance:**
  1. Given logic that assigns to `ctx.principal`, when compiled or run, then it fails with `LOGIC.CONTEXT_IMMUTABLE`.
- **Verification:** CONF
- **Origin:** EXT-TRI, L40:SPEC-16

### FR-LOGIC-022 — No self-declared identity
- **Statement:** Instructions MUST take principal, tenant and VAE only from the context. Instructions MUST NOT accept them as parameters, except the explicit delegation instructions governed by policy.
- **Rationale:** Scripts must not choose who they are (L40 hard rule).
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-SecurityOfficer
- **Acceptance:**
  1. Given a read instruction with a `tenant` parameter, when called, then it fails with `LOGIC.PARAMETER_FORBIDDEN`.
- **Verification:** CONF, SEC
- **Origin:** L40:SPEC-16

### FR-LOGIC-023 — Least-privilege capability gateway
- **Statement:** The `sys` gateway exposed to an asset MUST contain only the instruction families and targets declared by the asset and allowed by its profile. Undeclared use MUST fail at load or at call time.
- **Rationale:** Capability-based security at asset granularity.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-SecurityOfficer, PER-LogicDeveloper
- **Acceptance:**
  1. Given an asset declaring only `read` and `commit` on `Invoice`, when it calls `call.connector`, then it fails with `LOGIC.FORBIDDEN_CAPABILITY`.
- **Verification:** CONF, SEC
- **Origin:** L40:SPEC-17

### FR-LOGIC-024 — Context propagation
- **Statement:** Nested runs, emitted events and enqueued jobs MUST propagate the originating context: principal chain, process lineage, and `asof` unless overridden explicitly. Budgets MUST be shared along synchronous nesting.
- **Rationale:** Attribution and resource control across chains.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-LogicDeveloper, PER-InternalAuditor
- **Acceptance:**
  1. Given a nested call consuming budget, when the parent continues, then `budget_remaining` reflects the child's consumption.
- **Verification:** CONF
- **Origin:** L40:SPEC-16

## CAP-LOGIC-03 — L1 expressions

### FR-LOGIC-031 — Expression language
- **Statement:** The platform MUST provide a typed, side-effect-free, always-terminating expression language. It MUST support:
  - arithmetic on exact decimals;
  - comparison and boolean operators;
  - text, date, period and money functions;
  - field paths through references;
  - null-safe navigation and conditional expressions;
  - collection functions (`any`, `all`, `sum`, `count`, `min`, `max`, `filter`, `map`) with bounded iteration.
- **Rationale:** Most rules should be declarative, analysable and authorable by analysts (IMP-05).
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM, BPA
- **Personas:** PER-BusinessAnalyst
- **Acceptance:**
  1. Given `sum(lines, l -> l.amount) <= credit_limit`, when evaluated on an order, then the result is boolean and deterministic.
- **Verification:** CONF, PROP
- **Origin:** IMP-05, L40:SPEC-18

### FR-LOGIC-032 — Expression use sites
- **Statement:** L1 expressions MUST be usable in computed fields, defaults, constraints, guards, selector conditions, declaration values, decision-table cells, view visibility and enablement conditions, due-date rules and matching rules.
- **Rationale:** One language everywhere lowers learning cost.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-BusinessAnalyst, PER-BusinessArchitect
- **Acceptance:**
  1. Given the same expression used as a guard and as a view condition, when evaluated on the same object, then both give the same result.
- **Verification:** CONF
- **Origin:** IMP-05

### FR-LOGIC-033 — Static typing of expressions
- **Statement:** Expressions MUST be type-checked against the model when the owning object is committed. Type errors MUST be rejected with the position and the expected and actual types.
- **Rationale:** Errors at authoring time, not in production.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM, FRG
- **Personas:** PER-BusinessAnalyst
- **Acceptance:**
  1. Given `amount > "100"` on a money field, when committed, then it fails with `LOGIC.TYPE_ERROR` at the operator position.
- **Verification:** CONF
- **Origin:** IMP-05

### FR-LOGIC-034 — Standardised semantics
- **Statement:** The grammar, typing rules, evaluation order, rounding and error semantics of L1 expressions MUST be defined normatively in the BPA standard and covered by conformance vectors.
- **Rationale:** Expressions must evaluate identically on every implementation.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** BPA
- **Personas:** PER-ThirdPartyImplementer
- **Acceptance:**
  1. Given the expression vector suite, when run on the reference DVM, then 100% pass.
- **Verification:** CONF
- **Origin:** EXT-BPU

### FR-LOGIC-035 — Versioned function library
- **Statement:** The expression function library MUST be versioned with the ABI. Buks MAY register additional pure functions implemented in L2 or L3, subject to verification.
- **Rationale:** Domain functions (for example day-count conventions) without changing the language.
- **Priority:** Should · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-IsvDeveloper
- **Acceptance:**
  1. Given a Buk registering `daycount_act360`, when used in an expression, then it is type-checked and evaluated.
- **Verification:** CONF
- **Origin:** NEW

## CAP-LOGIC-04 — L2 typed scripts

### FR-LOGIC-041 — Scripts are logic assets
- **Statement:** L2 logic MUST be stored as LogicAsset objects that contain Rhai source, declared input and output types, the target ABI, declared capabilities and tests.
- **Rationale:** Code is data: it is versioned, reviewed, merged and proven like any object.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-LogicDeveloper
- **Acceptance:**
  1. Given a script change merged into `main`, when history is read, then the old and new versions and their diff are available.
- **Verification:** CONF
- **Origin:** EXT-TRI, L40:SPEC-17

### FR-LOGIC-042 — Type checking against the model
- **Statement:** Before verification, L2 scripts MUST be checked against type definitions generated from the effective model. Accessing unknown fields, wrong types or undeclared instructions MUST fail the check.
- **Rationale:** Untyped scripts fail at run time (PER-LogicDeveloper pain).
- **Priority:** Must · **Phase:** PH-1 · **Systems:** FRG, DVM
- **Personas:** PER-LogicDeveloper
- **Acceptance:**
  1. Given a script reading `order.amout` (misspelled), when checked, then it fails with `LOGIC.UNKNOWN_FIELD`.
- **Verification:** CONF
- **Origin:** IMP-05

### FR-LOGIC-043 — Modules and pinned imports
- **Statement:** Scripts MUST be able to import other logic assets by URI. Imports MUST resolve to exact versions recorded in the execution record. Import cycles MUST be rejected.
- **Rationale:** Reuse with reproducibility.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-LogicDeveloper
- **Acceptance:**
  1. Given a script importing `lib/fees`, when executed, then the record lists the version of `lib/fees` used.
- **Verification:** CONF
- **Origin:** L40:SPEC-17

### FR-LOGIC-044 — Business errors
- **Statement:** Scripts MUST be able to raise business errors with a registered code, a message and explanation entries. Kernel errors MUST be distinguishable from business errors, and limit terminations MUST NOT be catchable.
- **Rationale:** Structured errors for users, agents and tests.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-LogicDeveloper
- **Acceptance:**
  1. Given `raise("FUND.PRICE_MISSING", …)`, when surfaced to the caller, then the code and explanation are preserved.
- **Verification:** CONF
- **Origin:** L40:SPEC-02

### FR-LOGIC-045 — Script size and complexity limits
- **Statement:** The platform MUST enforce maximum source size, AST depth and function count per script, as defined by the profile.
- **Rationale:** Resource protection at load time.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-SecurityOfficer
- **Acceptance:**
  1. Given a script exceeding the AST depth limit, when loaded, then it fails with `LOGIC.SCRIPT_TOO_COMPLEX`.
- **Verification:** CONF
- **Origin:** L40:SPEC-17

## CAP-LOGIC-05 — L3 WASM components

### FR-LOGIC-051 — WASM components through a standard interface
- **Statement:** The platform MUST execute WASM components that import the instruction ABI through a published component interface (WIT) and export declared functions or ports.
- **Rationale:** Polyglot, high-performance logic for heavy computation and ISVs (IMP-05, EXT-RFC).
- **Priority:** Must · **Phase:** PH-3 · **Systems:** DVM
- **Personas:** PER-LogicDeveloper, PER-IsvDeveloper
- **Acceptance:**
  1. Given a Rust-built component implementing a port, when invoked, then it runs, and its instructions are authorised and metered like L2.
- **Verification:** CONF, SEC
- **Origin:** EXT-RFC, EXT-TRI, IMP-05

### FR-LOGIC-052 — Language-agnostic toolchain support
- **Statement:** Forge MUST support building components from Rust and at least one other language with component-model tooling, and MUST verify that the component imports only the ABI interface.
- **Rationale:** The ABI, not the language, is the contract.
- **Priority:** Should · **Phase:** PH-3 · **Systems:** FRG
- **Personas:** PER-IsvDeveloper
- **Acceptance:**
  1. Given a component importing WASI sockets, when packaged, then it fails with `FRG.FORBIDDEN_IMPORT`.
- **Verification:** CONF
- **Origin:** IMP-05

### FR-LOGIC-053 — Fuel and memory metering for components
- **Statement:** Component execution MUST be metered in fuel units mapped to the profile's operation budget and MUST be limited in linear memory.
- **Rationale:** Same safety envelope as L2.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** DVM
- **Personas:** PER-SecurityOfficer
- **Acceptance:**
  1. Given an infinite loop in a component, when run, then it terminates with `LOGIC.LIMIT_EXCEEDED`.
- **Verification:** CONF, SEC
- **Origin:** EXT-RFC

### FR-LOGIC-054 — Signed and verified components
- **Statement:** Components MUST be signed by their publisher. Only components in the `verified` state (CAP-LOGIC-08) MUST run on protected branches and on workers.
- **Rationale:** Supply-chain integrity.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** DVM, FRG
- **Personas:** PER-SecurityOfficer
- **Acceptance:**
  1. Given an unsigned component, when invoked on `main`, then it fails with `LOGIC.UNVERIFIED_ASSET`.
- **Verification:** CONF, SEC
- **Origin:** EXT-RFC

### FR-LOGIC-055 — Deterministic component subset
- **Statement:** Components MUST run without threads, host clocks, host randomness or ambient I/O. Floating-point NaN results MUST be canonicalised.
- **Rationale:** Determinism and replay (CAP-LOGIC-07).
- **Priority:** Must · **Phase:** PH-3 · **Systems:** DVM
- **Personas:** PER-KernelEngineer
- **Acceptance:**
  1. Given the same component and inputs on two machines, when run, then the outputs are bit-identical.
- **Verification:** CONF, PROP
- **Origin:** NEW

## CAP-LOGIC-06 — Sandbox and metering

### FR-LOGIC-061 — Execution profiles
- **Statement:** The platform MUST provide the execution profiles `strict`, `standard`, `batch`, `simulation` and `agent`. Each profile limits:
  - operations or fuel, memory and wall time;
  - instruction calls, commit size and connector calls.

  The `standard` profile MUST default to 3,000 ms CPU and 512 MB memory per invocation. Tenants MAY tighten profiles. They MAY loosen them only within plan entitlements.
- **Rationale:** Predictable resource envelopes (EXT-RFC 3.2).
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM, NOD
- **Personas:** PER-SecurityOfficer, PER-PlatformOperator
- **Acceptance:**
  1. Given each profile, when the limit vectors run, then termination happens at the declared limits.
- **Verification:** CONF
- **Origin:** EXT-RFC, L40:SPEC-17

### FR-LOGIC-062 — Uncatchable termination and rollback
- **Statement:** Reaching any limit, and cancellation, MUST terminate the execution in a way the logic cannot intercept. The enclosing process MUST abort without committing anything or releasing any outbox item.
- **Rationale:** Circuit breaker semantics (SCN-010).
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-SecurityOfficer
- **Acceptance:**
  1. Given SCN-010, when executed, then no commit, event or job results.
- **Verification:** CONF, FAULT
- **Origin:** EXT-TRI, L40:SPEC-17

### FR-LOGIC-063 — Metering records
- **Statement:** Every execution MUST produce a metering record: operations or fuel, peak memory, wall time, instruction counts by family, connector and model usage. It MUST be attributed to tenant, VAE, principal, asset version and process.
- **Rationale:** Cost attribution, capacity planning and billing (CAP-BILL-01).
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-PlatformOperator, PER-TenantAdministrator
- **Acceptance:**
  1. Given 1,000 executions, when metering is aggregated, then the totals equal the sum of the records.
- **Verification:** CONF
- **Origin:** NEW

### FR-LOGIC-064 — Quarantine of failing assets
- **Statement:** When an asset version exceeds a configured failure or limit-termination rate within a window, the platform MUST quarantine it for the VAE. Invocations then fail fast with `LOGIC.ASSET_QUARANTINED` until an authorised user releases it, and an alert is raised.
- **Rationale:** Adaptive immunity (EXT-TRI Book II).
- **Priority:** Should · **Phase:** PH-2 · **Systems:** DVM, NOD
- **Personas:** PER-PlatformOperator, PER-LogicDeveloper
- **Acceptance:**
  1. Given 20 limit terminations in 5 minutes with a threshold of 10, when the next invocation arrives, then it fails fast, and an alert exists.
- **Verification:** CONF, FAULT
- **Origin:** EXT-TRI

### FR-LOGIC-065 — Tenant isolation in execution
- **Statement:** Executions of different tenants MUST NOT share mutable state, caches of business data or engine instances with residual state.
- **Rationale:** Isolation (CAP-TEN-02).
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-SecurityOfficer
- **Acceptance:**
  1. Given two tenants running the same asset concurrently, when the isolation suite runs, then no value written by one is observable by the other.
- **Verification:** SEC, SIM
- **Origin:** L40:SPEC-23

## CAP-LOGIC-07 — Determinism and replay

### FR-LOGIC-071 — Non-determinism only through the context or recorded instructions
- **Statement:** Time MUST come only from `ctx.now`, randomness and identifiers only from context-provided generators, and external results only from `call` and `ask`. All such values MUST be recorded in the execution journal.
- **Rationale:** Replayability (SCN-011) and hardware-like determinism (EXT-BPU).
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-KernelEngineer, PER-InternalAuditor
- **Acceptance:**
  1. Given logic calling `ctx.now` twice, when run, then both calls return the same instant for the process. Both values are journaled.
- **Verification:** CONF, PROP
- **Origin:** L40:SPEC-17

### FR-LOGIC-072 — Execution journal
- **Statement:** Every process MUST keep an execution journal containing:
  - the logic versions, inputs and read snapshots (commit IDs);
  - the recorded non-deterministic values, connector exchanges and model responses;
  - the outputs.

  It MUST be retained as long as the process's commits, subject to retention policy.
- **Rationale:** Evidence and replay.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-InternalAuditor
- **Acceptance:**
  1. Given a process, when its journal is read, then every element needed for replay is present.
- **Verification:** CONF
- **Origin:** L40:SPEC-17

### FR-LOGIC-073 — Replay
- **Statement:** The platform MUST replay any process from its journal without side effects and MUST report "identical" or the first divergence (instruction, expected value, actual value).
- **Rationale:** SCN-011.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM, FRG
- **Personas:** PER-InternalAuditor, PER-KernelEngineer
- **Acceptance:**
  1. Given SCN-011, when replayed, then the result is "identical", and no state changes.
- **Verification:** SCN, CONF
- **Origin:** L40:SPEC-17

### FR-LOGIC-074 — Cross-implementation determinism
- **Statement:** Given the same ABI version, logic, inputs and journal, every conforming implementation MUST produce identical outputs.
- **Rationale:** Portability of the BPU between engines.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** BPA, DVM
- **Personas:** PER-ThirdPartyImplementer
- **Acceptance:**
  1. Given the determinism vectors, when run on two implementations, then all outputs match.
- **Verification:** CONF
- **Origin:** EXT-BPU

### FR-LOGIC-075 — Floating point discipline
- **Statement:** Values stored in `decimal` or `money` fields MUST NOT be computed through binary floating point. When scripts use floating point for other purposes, the operations MUST follow IEEE 754 without fast-math, and results MUST be canonicalised before storage.
- **Rationale:** Correctness and determinism.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM, FRG
- **Personas:** PER-LogicDeveloper
- **Acceptance:**
  1. Given a script that assigns a float to a money field, when checked, then it fails with `LOGIC.FLOAT_TO_DECIMAL`.
- **Verification:** CONF
- **Origin:** NEW

## CAP-LOGIC-08 — Logic asset lifecycle

### FR-LOGIC-081 — Asset states
- **Statement:** Logic assets (L2 scripts, L3 components, registered functions, connectors' mapping logic) MUST have the lifecycle `draft → committed → verified → deprecated → retired`.
- **Rationale:** EXT-RFC 3.3.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-LogicDeveloper
- **Acceptance:**
  1. Given a new asset, when committed, then its state is `committed`. After passing verification, it is `verified`.
- **Verification:** CONF
- **Origin:** EXT-RFC

### FR-LOGIC-082 — Only verified logic runs on protected branches
- **Statement:** On `main`, `release` and `overlay` branches, only assets in `verified` or `deprecated` state MUST execute. `committed` assets MAY execute on `draft`, `feature`, `sandbox`, `agent` and `evolution` branches.
- **Rationale:** Production runs only reviewed, checked logic.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-SecurityOfficer
- **Acceptance:**
  1. Given an unverified asset merged to `main` by mistake, when invoked, then it fails with `LOGIC.UNVERIFIED_ASSET`.
- **Verification:** CONF
- **Origin:** EXT-RFC

### FR-LOGIC-083 — Verification gate
- **Statement:** An asset becomes `verified` only when all of the following hold:
  1. static analysis passes;
  2. type check passes;
  3. declared tests pass;
  4. declared capabilities are within the target profile;
  5. the verifying principal (a human or the automated pipeline under policy) signs the verification record.
- **Rationale:** "Pre-silicon verification" of business logic (EXT-BPU).
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM, FRG
- **Personas:** PER-LogicDeveloper, PER-SecurityOfficer
- **Acceptance:**
  1. Given a failing test, when verification is requested, then it is refused with the failing test listed.
- **Verification:** CONF
- **Origin:** EXT-RFC, EXT-BPU

### FR-LOGIC-084 — Deprecation and retirement
- **Statement:** Invoking a `deprecated` asset MUST succeed and record a warning. A `retired` asset MUST NOT execute except for replay.
- **Rationale:** Orderly evolution.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-LogicDeveloper
- **Acceptance:**
  1. Given a retired asset, when replay of an old process needs it, then it runs in replay mode only.
- **Verification:** CONF
- **Origin:** EXT-RFC

### FR-LOGIC-085 — Atomic hot swap
- **Statement:** A new asset version MUST take effect for processes that start after the commit that makes it effective. Processes already running MUST complete with the version they started with.
- **Rationale:** Zero-downtime logic deployment (EXT-TRI "atomic swaps").
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-LogicDeveloper, PER-PlatformOperator
- **Acceptance:**
  1. Given a long-running process when a new version is merged, when both complete, then the old process's record shows the old version, and new processes show the new version.
- **Verification:** CONF, SIM
- **Origin:** EXT-TRI

## CAP-LOGIC-09 — Static analysis

### FR-LOGIC-091 — Forbidden patterns
- **Statement:** Static analysis MUST reject assets that use:
  - dynamic code evaluation or host reflection;
  - undeclared instructions or unbounded recursion without a declared bound;
  - imports outside the ABI;
  - any other pattern on the platform's forbidden list, which is versioned with the ABI.
- **Rationale:** Innate immunity (EXT-TRI Book II).
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM, FRG
- **Personas:** PER-SecurityOfficer
- **Acceptance:**
  1. Given each forbidden-pattern vector, when analysed, then it is rejected with the rule ID.
- **Verification:** CONF, SEC
- **Origin:** EXT-TRI, L40:SPEC-17

### FR-LOGIC-092 — Capability inference
- **Statement:** Static analysis MUST infer the instruction families and targets an asset may use and MUST reject assets whose inferred capabilities exceed their declaration.
- **Rationale:** Declarations must be truthful for least privilege.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** FRG, DVM
- **Personas:** PER-SecurityOfficer
- **Acceptance:**
  1. Given an asset that writes `Payment` but declares only `Invoice`, when analysed, then it fails with `LOGIC.CAPABILITY_UNDECLARED`.
- **Verification:** CONF
- **Origin:** NEW

### FR-LOGIC-093 — Complexity findings
- **Statement:** Static analysis SHOULD report cyclomatic complexity, loop nesting and estimated worst-case operations as findings, with thresholds that can block verification by policy.
- **Rationale:** It catches most runaway risks before run time.
- **Priority:** Should · **Phase:** PH-2 · **Systems:** FRG
- **Personas:** PER-LogicDeveloper
- **Acceptance:**
  1. Given a policy threshold of 20 and an asset with complexity 35, when verified, then verification is blocked.
- **Verification:** CONF
- **Origin:** NEW

### FR-LOGIC-094 — Findings are recorded
- **Statement:** Analysis findings MUST be stored with the asset version and shown in Studio and in the verification record.
- **Rationale:** Traceable verification.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM, STU
- **Personas:** PER-LogicDeveloper
- **Acceptance:**
  1. Given a verified asset, when inspected, then its findings list (possibly empty) is present.
- **Verification:** CONF
- **Origin:** NEW

## CAP-LOGIC-10 — Connectors

### FR-LOGIC-101 — Connector objects
- **Statement:** A connector MUST be a Definition object that declares:
  - endpoint and protocol (HTTPS, SFTP, SMTP, a message broker), authentication method and a credential reference (never the secret);
  - allowed operations with request and response schemas;
  - timeouts, rate limits and retry policy;
  - the data classifications allowed to leave.
- **Rationale:** External access is governed, versioned and reviewable (EXT-RFC CONNECTOR assets).
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM, BRG
- **Personas:** PER-LogicDeveloper, PER-SecurityOfficer
- **Acceptance:**
  1. Given a connector that allows `public` and `internal` only, when a payload includes a `personal` field, then the call fails with `LOGIC.CLASSIFICATION_EGRESS_DENIED`.
- **Verification:** CONF, SEC
- **Origin:** EXT-RFC

### FR-LOGIC-102 — No other network path
- **Statement:** Logic MUST NOT perform network I/O except `call.connector` on a connector allowed for the asset.
- **Rationale:** Network isolation (EXT-RFC 3.2).
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-SecurityOfficer
- **Acceptance:**
  1. Given the sandbox escape suite for network access, when run, then every attempt fails.
- **Verification:** SEC
- **Origin:** EXT-RFC

### FR-LOGIC-103 — Recorded exchanges
- **Statement:** Every connector call MUST record request and response, with fields redacted according to the connector's redaction rules, in the execution journal.
- **Rationale:** Replay and audit.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-InternalAuditor
- **Acceptance:**
  1. Given a call returning a rate, when replayed, then the recorded rate is used, and no network call occurs.
- **Verification:** CONF
- **Origin:** L40:SPEC-17

### FR-LOGIC-104 — Egress allow-list
- **Statement:** Each VAE MUST have an egress allow-list of hosts and protocols. Connector endpoints outside it MUST be rejected at commit.
- **Rationale:** Defence in depth against data exfiltration.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM, NOD
- **Personas:** PER-SecurityOfficer
- **Acceptance:**
  1. Given host `evil.example` not on the list, when a connector targeting it is committed, then it fails with `LOGIC.EGRESS_NOT_ALLOWED`.
- **Verification:** CONF, SEC
- **Origin:** NEW

### FR-LOGIC-105 — Connector resilience
- **Statement:** Connectors MUST apply their retry, backoff and circuit-breaker policies. An open breaker MUST fail calls fast with `LOGIC.CONNECTOR_UNAVAILABLE`.
- **Rationale:** External failures must not cascade.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM, BRG
- **Personas:** PER-PlatformOperator
- **Acceptance:**
  1. Given 10 consecutive failures, when the breaker opens, then calls fail fast until the half-open probe succeeds.
- **Verification:** CONF, FAULT
- **Origin:** EXT-WP (reactive retry)
