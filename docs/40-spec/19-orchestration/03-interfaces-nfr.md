---
id: SPEC-19-3
title: "Orchestration — Interfaces and non-functional"
status: complete
phase: P4
depends_on: [SPEC-19, SPEC-13, SPEC-15, SPEC-16, SPEC-17, SPEC-18]
sources: [UP, LC, UB, UC, US, LS, UW]
---

# Orchestration — Interfaces and non-functional

Part 3 of SPEC-19 (`19-orchestration/`). Header, concepts and file list: `00-index.md`.

---

## 4. Interfaces

### API-FLOW-001 — FlowEngine
- **Kind:** rust trait (`ubos_kernel::flow`)
- **Signature / shape:**
```rust
#[async_trait]
pub trait FlowEngine: Send + Sync {
    async fn run_pipeline(&self, p: &mut ProcessHandle, pipeline: &str, args: Value) -> Result<Value, UbosError>;
    async fn available_actions(&self, x: &ExecutionContext, target: &str) -> Result<Vec<ActionRef>, UbosError>;
    async fn describe(&self, x: &ExecutionContext, action: &str, target: Option<&str>) -> Result<TransitionResponse, UbosError>;
    async fn prepare(&self, x: &ExecutionContext, req: ActionCall) -> Result<TransitionResponse, UbosError>;
    async fn execute(&self, p: &mut ProcessHandle, req: ActionCall) -> Result<TransitionResponse, UbosError>;
    async fn dialogue(&self, x: &ExecutionContext, req: DialogueCall) -> Result<TransitionResponse, UbosError>;
    async fn start_instance(&self, p: &mut ProcessHandle, workflow: &str, args: Value) -> Result<String, UbosError>;
    async fn signal(&self, p: &mut ProcessHandle, instance: &str, signal: &str, args: Value) -> Result<Value, UbosError>;
    async fn resolve_intent(&self, x: &ExecutionContext, intent: Intent) -> Result<OperationPlan, UbosError>;
}
pub struct ActionCall { pub action: String, pub target: Option<String>, pub args: Value,
                        pub base_commit: Option<CommitId>, pub dialogue_state: Option<String>,
                        pub dialogue_token: Option<String>, pub event: Option<String> }
```
- **Origin:** CON-US-022, CON-LS-004, CON-LC-020

### API-FLOW-002 — UBTP Emit shapes
- **Kind:** UBTP message (SPEC-24)
- **Signature / shape:**
```json
{ "op": "Emit", "request_id": "r-40", "idempotency_key": "k-77",
  "target": "Invoice/inv-1", "signal": "ubos://acme/Action/invoice.approve@v1",
  "stage": "EXECUTE", "args": { "comment": "ok" }, "base_commit": "41" }

{ "op": "Emit", "request_id": "r-41", "target": "ubos://acme/Pipeline/auth.register@v1",
  "args": { "email": "a@acme.com", "name": "Ann" } }

{ "op": "Emit", "request_id": "r-42", "target": "ubos://acme/ProcessInstance/pi-9f3",
  "signal": "payment.received", "args": { "amount": "120.00" } }
```
- **Semantics:**
  - `target` is the entity (for actions) or the operation (pipeline, workflow, instance).
  - `signal` is an action URI, a signal name or `start`.
  - `stage` defaults to EXECUTE.
- **Origin:** VRD-15-01

### API-FLOW-003 — Example definitions
- **Kind:** data (package files, SPEC-28)
- **Signature / shape:**
```json
{ "type": "Lifecycle", "slug": "acme.invoice", "data": {
    "initial": "DRAFT",
    "states": { "DRAFT": {"title": "Draft"}, "SUBMITTED": {"title": "Submitted", "editable": false},
                "APPROVED": {"title": "Approved", "editable": false}, "POSTED": {"title": "Posted", "editable": false, "terminal": true} },
    "transitions": [ {"event": "submit", "from": ["DRAFT"], "to": "SUBMITTED"},
                     {"event": "approve", "from": ["SUBMITTED"], "to": "APPROVED",
                      "guards": [{"name": "has_lines", "title": "Invoice has lines", "severity": "ERROR", "expression": "len(new.lines) > 0"}]},
                     {"event": "post", "from": ["APPROVED"], "to": "POSTED"} ] } }

{ "type": "TransitionAction", "slug": "acme.invoice.approve.v1", "data": {
    "title": "Approve", "target_type": "ubos://acme/Type/acme.Invoice", "target_mode": "INSTANCE",
    "effect": "STAGE", "lifecycle_event": "approve", "execute": "ubos://acme/Logic/invoice.on_approve@v1",
    "ui": {"placement": ["TOOLBAR"], "style": "PRIMARY"} } }

{ "type": "Pipeline", "slug": "auth.register.v1", "data": {
    "title": "Register user", "mode": "COMMAND",
    "input_schema": {"type": "object", "required": ["email"], "properties": {"email": {"type": "string", "format": "email"}, "name": {"type": "string"}}},
    "steps": [
      {"name": "create_user", "kind": "CALL", "impl": "ubos://system/Native/entity.create_from@v1",
       "config": {"type": "User"}, "input": {"data": "vars.args"}, "output": "user_uri"},
      {"name": "grant", "kind": "CALL", "impl": "ubos://acme/Logic/grant.default_role@v1", "input": {"user": "vars.user_uri"}},
      {"name": "welcome", "kind": "EMIT", "emit": {"kind": "MAIL", "name": "welcome", "target": "${vars.user_uri}"}} ] } }
```
- **Origin:** CON-LC-010, CON-US-017, CON-UB-032

### Error codes (FLOW)

| Code | Category | Meaning |
|---|---|---|
| `FLOW.INPUT_INVALID` | INVALID | arguments violate the schema (causes with paths) |
| `FLOW.TRANSITION_NOT_ALLOWED` | CONFLICT | event not allowed from the current state |
| `FLOW.STATE_READ_ONLY` | INVALID | `_state` written outside transitions |
| `FLOW.NOT_EDITABLE` | CONFLICT | update in a non-editable lifecycle state |
| `FLOW.DIALOGUE_INVALID` | INVALID | missing or forged dialogue token |
| `FLOW.SIGNAL_NOT_EXPECTED` | CONFLICT | instance does not accept the signal in its state |
| `FLOW.LOOP_LIMIT` | LIMIT | too many step executions |
| `FLOW.EMIT_DEPTH` | LIMIT | emit causation chain too long |
| `FLOW.ACTION_NOT_APPLICABLE` | INVALID | target type not in the action's target_type chain |
| `FLOW.STEP_FAILED` | (wrapper) | step error with `details.step` |

---

## 5. Non-functional

| ID | Requirement | Target |
|---|---|---|
| NFR-PERF-090 | Pipeline step overhead (excluding the implementation) | ≤ 20 µs |
| NFR-PERF-091 | Available-actions listing for a target | p95 ≤ 5 ms |
| NFR-PERF-092 | Signal delivery to an instance (sync, PG) | p95 ≤ 50 ms |
| NFR-AIR-090 | Actions, pipelines and workflows are complete descriptions | AI can generate a feature as a package of definitions + logic (VRD-22-02, SPEC-27) |
