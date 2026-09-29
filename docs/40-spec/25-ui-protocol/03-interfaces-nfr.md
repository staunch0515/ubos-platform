---
id: SPEC-25-3
title: "UI Protocol — Interfaces and non-functional"
status: complete
phase: P4
depends_on: [SPEC-25, SPEC-13, SPEC-18, SPEC-19, SPEC-21, SPEC-22, SPEC-24]
sources: [UP, FU, UB, UC, US, LS, UW]
---

# UI Protocol — Interfaces and non-functional

Part 3 of SPEC-25 (`25-ui-protocol/`). Header, concepts and file list: `00-index.md`.

---

## 4. Interfaces

### API-UI-001 — RenderService
- **Kind:** rust trait (`ubos_kernel::ui`)
- **Signature / shape:**
```rust
#[async_trait]
pub trait RenderService: Send + Sync {
    async fn render(&self, x: &ExecutionContext, req: RenderRequest) -> Result<RenderPayload, UbosError>;
    async fn project(&self, x: &ExecutionContext, req: ProjectRequest) -> Result<RenderPayload, UbosError>;
    fn resolve_widget(&self, t: &EffectiveType, prop: Option<&EffectiveProperty>, mode: Mode,
                      platform: Platform, matrices: &[UiMatrix]) -> ResolvedWidget;                   // REQ-UI-002 (pure)
    fn derive_view(&self, t: &EffectiveType, layout: LayoutType, mode: Mode) -> View;             // REQ-UI-011 (pure)
}
pub struct RenderRequest { pub target: String, pub mode: Mode, pub view: Option<String>, pub platform: Platform,
                           pub params: Option<Value>, pub page: Option<Page>, pub hints: bool }
```
- **Origin:** CON-UB-050, CON-US-052

### API-UI-002 — Named operations
- **Kind:** UBTP Query targets
- **Signature / shape:**

| URI | Args | Result |
|---|---|---|
| `ubos://system/Query/ui.render@v1` | RenderRequest | RenderPayload |
| `ubos://system/Query/ui.project@v1` | `{uri, as_type, mode, include?, exclude?, overrides?}` | RenderPayload |
| `ubos://system/Query/ui.widgets@v1` | `{platform?}` | Widget catalogue |
| `ubos://system/Query/ui.resolve@v1` | `{type, property?, mode, platform}` | widget and the rule that chose it (debugging) |
- **Origin:** CON-US-054 (`/view/{slug}/init`)

### Example payload (abridged, invoice detail, clerk, web)

```json
{ "ui": { "id": "root", "widget": "ubos://logrums/Widget/Detail", "children": [
      { "id": "number", "widget": "ubos://logrums/Widget/TextLabel", "bind": "number", "props": { "label": "Number" } },
      { "id": "customer", "widget": "ubos://logrums/Widget/RefLink", "bind": "customer", "props": { "label": "Customer" } },
      { "id": "total", "widget": "ubos://logrums/Widget/MoneyLabel", "bind": "total", "props": { "label": "Total" } },
      { "id": "iban", "widget": "ubos://logrums/Widget/MaskedLabel", "bind": "iban", "props": { "label": "IBAN" } },
      { "id": "state", "widget": "ubos://logrums/Widget/StateChip", "bind": "_state" } ] },
  "data": { "number": "INV-2026-0042", "customer": { "_uri": "ubos://acme/Customer/c-001", "title": "Hangzhou Tea Co." },
            "total": { "amount": "1250.00", "currency": "CNY", "_formatted": "¥1,250.00" },
            "iban": "CN** **** **** 4242", "_state": "SUBMITTED" },
  "meta": { "uri": "ubos://acme/acme.Invoice/inv-42?commit=901", "commit_id": "901", "base_commit": "901", "branch": "main",
            "type": "ubos://acme/Type/acme.Invoice", "view": "derived:DETAIL", "mode": "view", "platform": "WEB",
            "locale": "zh-CN", "hidden_paths": ["/margin"], "read_only_paths": [] },
  "actions": [ { "id": "edit", "title": "Edit", "placement": "TOOLBAR", "on": "CLICK",
                 "chain": [ { "kind": "EMIT", "action": "ubos://logrums/Action/entity.edit@v1", "stage": "PREPARE" } ] } ],
  "ui_policy": "READ_ONLY" }
```

### Error codes (UI)

| Code | Category | Meaning |
|---|---|---|
| `UI.VIEW_NOT_FOUND` | NOT_FOUND | view URI unknown |
| `UI.NOT_ANCESTOR` | INVALID | projection type not in the entity's chain |
| `UI.PROPS_INVALID` | INVALID | component props violate the widget's props_schema (at View commit) |
| `UI.BINDING_INVALID` | INVALID | action binding references a non-operation |
| `UI.MODE_UNSUPPORTED` | INVALID | widget does not support the requested mode (at View commit) |

---

## 5. Non-functional

| ID | Requirement | Target |
|---|---|---|
| NFR-PERF-170 | `ui.render` of an entity detail with warm caches | p95 ≤ 30 ms (server) |
| NFR-PERF-171 | Table render, 50 rows, 8 columns | p95 ≤ 80 ms (server) |
| NFR-UX-170 | Time to first interactive form in the web shell after a click | ≤ 300 ms on the reference network |
| NFR-UX-171 | Accessibility | WCAG 2.1 AA for standard widgets |
