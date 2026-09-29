---
id: SPEC-27-3
title: "AI — Interfaces and non-functional"
status: complete
phase: P4
depends_on: [SPEC-27, SPEC-13, SPEC-16, SPEC-17, SPEC-19, SPEC-21, SPEC-22, SPEC-24, SPEC-28, SPEC-30]
sources: [UP, FU, LC, UB, UC, US, LS, UW]
---

# AI — Interfaces and non-functional

Part 3 of SPEC-27 (`27-ai/`). Header, concepts and file list: `00-index.md`.

---

## 4. Interfaces

### API-AI-001 — AiGateway
- **Kind:** rust trait (`ubos_kernel::ai`)
- **Signature / shape:**
```rust
#[async_trait]
pub trait AiGateway: Send + Sync {
    async fn chat(&self, x: &ExecutionContext, task: AiTask, req: ChatRequest) -> Result<ChatResponse, UbosError>;
    async fn extract(&self, x: &ExecutionContext, task: AiTask, text: &str, schema: &Value, opts: ExtractOpts) -> Result<Value, UbosError>;
    async fn embed(&self, x: &ExecutionContext, task: AiTask, texts: &[String]) -> Result<Vec<Vec<f32>>, UbosError>;
    async fn usage(&self, tenant: &TenantCode, period: Period) -> Result<AiUsage, UbosError>;
}
#[async_trait]
pub trait AiClient: Send + Sync {                      // one per provider kind; injected via KernelBuilder
    async fn call(&self, provider: &AiProviderCfg, req: ProviderRequest) -> Result<ProviderResponse, AiError>;
}
```
- **Origin:** CON-UB-066, CON-UC-060

### API-AI-002 — Named operations
- **Kind:** UBTP targets
- **Signature / shape:**

| URI | Args | Result | Permission |
|---|---|---|---|
| `ubos://system/Pipeline/ai.build@v1` | `{requirement, package_code, context?, base_branch?, attachments?}` | Job → AiBuildSession URI | `ai.build` |
| `ubos://system/Query/ai.copilot@v1` | `{text, ui_context}` | proposals | `ai.use` |
| `ubos://system/Query/ai.ask@v1` | `{question, scope?}` | answer + citations | `ai.use` |
| `ubos://system/Query/ai.explain@v1` | `{process_id \| uri, path?}` | explanation | read of the subject |
| `ubos://system/Action/agent.run@v1` | `{agent, goal, context?}` | Job | `action.agent.run` + agent ACTIVE |
| `ubos://system/Query/ai.usage@v1` | `{period}` | tokens and cost by task, provider, principal | `tenant.admin` |
| `ubos://system/Query/ai.context@v1` | `{topics?}` | platform description | authenticated |
| `ubos://system/Action/ai.build_export@v1` | `{from, to}` | JSONL blob | `tenant.admin` |
- **Origin:** VRD-16-01…07

### Example copilot exchange

```json
→ { "op": "Query", "id": "c1", "named": { "uri": "ubos://system/Query/ai.copilot@v1",
     "args": { "text": "create a bug for the login timeout and assign it to li", "ui_context": { "view": "View/acme.dev.board" } } } }
← { "msg": "Result", "re": "c1", "data": { "proposals": [
     { "intent": { "kind": "CREATE", "target": "Type/acme.dev.Issue",
                   "args": { "title": "Login timeout", "kind": "BUG", "assignee": "ubos://acme/User/li" }, "placement": "DOCK" },
       "title": "Create bug \"Login timeout\" assigned to Li", "explanation": "Matched 'bug' and user 'li'", "confidence": 0.92 } ],
     "citations": [ "ubos://acme/User/li?commit=311" ] } }
```

### Error codes (AI)

| Code | Category | Meaning |
|---|---|---|
| `AI.NOT_CONFIGURED` | UNAVAILABLE | no provider for the task |
| `AI.PROVIDER_FAILED` | UNAVAILABLE | provider error or timeout (retryable) |
| `AI.BUDGET_EXCEEDED` | LIMIT | token or step budget exhausted |
| `AI.OUTPUT_INVALID` | INVALID | output failed schema validation after retries |
| `AI.TOOL_NOT_ALLOWED` | FORBIDDEN | agent called a tool outside its list |
| `AI.PRIVACY_BLOCKED` | FORBIDDEN | request would send disallowed sensitive data |
| `AI.BUILD_INVALID` | INVALID | builder could not produce a valid package (errors attached) |

---

## 5. Non-functional

| ID | Requirement | Target |
|---|---|---|
| NFR-PRIV-190 | Secret and SECRET-sensitivity data sent to providers | never |
| NFR-PERF-190 | Gateway overhead excluding the provider | ≤ 10 ms |
| NFR-AIR-190 | Builder success on the reference requirement set (P1 exit) | ≥ 70 % valid packages within 3 repair rounds |
| NFR-OPS-190 | AI cost visibility | tokens and cost per tenant, task and principal available daily |
