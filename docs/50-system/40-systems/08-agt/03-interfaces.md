---
id: UBS-SYS-AGT-03
title: Agent Hub — Interfaces
status: draft
phase: PH-3
depends_on: [UBS-SYS-AGT-02]
---

# Agent Hub — Interfaces (IF-AGT-*)

### IF-AGT-001 — `complete`
- **Kind:** HTTP endpoint
- **Caller → Callee:** NOD → AGT
- **Request:** `{ tenant, principal, purpose, model_class, parts: [{ role, text, classification }], schema?, budget, trace }`
- **Response:** `{ output, provider, model, tokens_in, tokens_out, cost, redactions }`
- **Errors:** `AI.CLASSIFICATION_BLOCKED`, `AI.BUDGET_EXHAUSTED`, `AI.PROVIDER_UNAVAILABLE`, `AI.OUTPUT_INVALID`
- **Idempotency:** not idempotent; request ID recorded.
- **Authorization:** node service identity plus tenant policy.
- **Satisfies:** CTR-020, FR-AI-011, FR-AI-091

### IF-AGT-002 — `embed`
- **Kind:** HTTP endpoint
- **Caller → Callee:** NOD → AGT
- **Request:** `{ tenant, model_class, items: [{ id, text, classification }] }`
- **Response:** `{ vectors: [{ id, vector }], model }`
- **Errors:** `QRY.CLASSIFICATION_NOT_EMBEDDABLE`
- **Idempotency:** pure per model version.
- **Authorization:** node service identity.
- **Satisfies:** CTR-020, FR-QRY-033

### IF-AGT-003 — Agent administration
- **Kind:** HTTP endpoint
- **Caller → Callee:** STU, WSP → AGT (via NOD)
- **Request:** `runs.start(agent, input)`, `runs.list`, `runs.get`, `runs.stop`, `kill(scope)`, `evals.run(agent, version)`
- **Response:** run handles and records
- **Errors:** `IAM.DENIED`
- **Idempotency:** start keyed by trigger ID.
- **Authorization:** supervisor roles.
- **Satisfies:** FR-AI-022, FR-AI-024, FR-AI-081

### IF-AGT-004 — MCP server
- **Kind:** HTTP endpoint
- **Caller → Callee:** external MCP client → AGT
- **Request:** MCP `initialize`, `tools/list`, `tools/call`, `resources/list`, `resources/read`
- **Response:** MCP results
- **Errors:** MCP errors carrying platform codes
- **Idempotency:** mutating tools carry idempotency keys derived from the MCP request ID.
- **Authorization:** OAuth 2.1 delegated token from NOD.
- **Satisfies:** CTR-022, FR-AI-042

### IF-AGT-005 — Copilot session
- **Kind:** UBTP message
- **Caller → Callee:** WSP → AGT (proxied by NOD)
- **Request:** `open(context)`, `ask(text)`, `accept(proposal_id)`
- **Response:** streamed tokens, citations, proposals
- **Errors:** degraded mode message
- **Idempotency:** —
- **Authorization:** user session.
- **Satisfies:** CTR-024, FR-AI-061
