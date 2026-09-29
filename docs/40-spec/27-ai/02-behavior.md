---
id: SPEC-27-2
title: "AI — Behavior"
status: complete
phase: P4
depends_on: [SPEC-27, SPEC-13, SPEC-16, SPEC-17, SPEC-19, SPEC-21, SPEC-22, SPEC-24, SPEC-28, SPEC-30]
sources: [UP, FU, LC, UB, UC, US, LS, UW]
---

# AI — Behavior

Part 2 of SPEC-27 (`27-ai/`). Header, concepts and file list: `00-index.md`.

---

## 3. Behavior

### 3.1 Gateway and syscalls

### REQ-AI-001 — AI gateway
- **Statement:** Every model call MUST go through the kernel's AI gateway. No component calls a provider directly. The gateway:
  1) selects the provider and model: first the explicit Agent or Context setting, then the AiRouting rules for the task, then the tenant defaults;
  2) applies the privacy filter (REQ-AI-003);
  3) enforces permissions (`ai.use`, `ai.build`), rate limits and the tenant budget (`max_ai_tokens_per_day`, REQ-TEN-031);
  4) calls the provider with a timeout and one retry on 5xx or timeout;
  5) records a trace node of kind AI with the model, token counts, duration and cost. At DETAILED level it also records the masked prompt and response (REQ-RT-010);
  6) adds the tokens to the process metrics and to `ubos_ai_tokens_total`.
- **Origin:** VRD-16-02, CON-UB-066, CON-LC-062 (token cost monitoring)
- **Acceptance:** Two tenants with different providers route correctly. Exceeding the daily token budget fails with `AI.BUDGET_EXCEEDED`, and usage appears in `ai.usage`.
- **Priority:** P1

### REQ-AI-002 — AI syscalls
- **Statement:** The `ai.*` syscalls (REQ-RT-013) MUST behave as follows:
  - `ai.complete` and `ai.chat` return text or messages.
  - `ai.extract(text, schema)` asks for JSON conforming to the schema. It validates the result and, on failure, re-asks up to 2 times with the validation errors. It then returns the object or fails with `AI.OUTPUT_INVALID`.
  - `ai.embed` returns a vector of the provider's dimensions.

  All are EXTERNAL-effect calls: they are blocked in dry runs unless the Context allows it (REQ-RT-005), their results are recorded for replay, and they are never part of rule evaluation (RULE profile).
- **Origin:** VRD-16-02, CON-UC-060, CON-UB-066 (`ctx.ai()`)
- **Acceptance:** A logic that extracts invoice fields from a PDF text returns a schema-valid object, or a clear error after 3 attempts.
- **Priority:** P1

### REQ-AI-003 — Privacy filter
- **Statement:** Before a prompt is sent, the gateway MUST:
  - remove every SECRET-sensitivity value and every secret (never sent);
  - remove or mask PERSONAL values when the provider is EXTERNAL, unless the tenant setting `ai.allow_personal_external: true` is set and the routing rule's `max_sensitivity` allows PERSONAL;
  - apply the calling principal's masking (REQ-SEC-021), so AI never sees more than the caller may.

  Entity data is placed in delimited data sections of the prompt. Instructions contained in data are never elevated to system instructions (a prompt-injection boundary). Every AI output that affects the platform is validated by the platform's own checks anyway (§1).
- **Origin:** VRD-16-01, NFR-PRIV, CON-US-060 ("hallucination firewall")
- **Acceptance:** An agent summarising customers through an EXTERNAL provider sends masked emails. The recorded prompt shows the masks.
- **Priority:** P1

### 3.2 Builder

### REQ-AI-010 — Builder loop
- **Statement:** `ai.build@v1 {requirement, package_code, context?, base_branch?, attachments?}` (permission `ai.build`) MUST run as an asynchronous Pipeline under the builder agent `ubos://logrums/Agent/builder`, on behalf of the requester, and record an AiBuildSession:
  1) **Reflect.** Assemble the context:
     - the meta-schema and the syscall ABI;
     - SPEC-02 conventions (condensed);
     - the package format;
     - the relevant existing definitions, found by similarity over type, action and logic descriptions (REQ-AI-031) and by the target namespace;
     - the README of dependent packages;
     - the requirement and its attachments.
  2) **Generate.** Ask for a package (manifest, entities, `.rhai` logic, scenarios), as a structured file set validated by `ai.extract` shapes. It must contain at least one scenario per Action and Pipeline (the US "three-piece set" plus tests).
  3) **Validate.** Run `pkg validate` offline (meta-schema, Rhai compile, `rt.check`), then `package.install` with `dry_run` on a new draft branch `draft/builder/<session>`. Collect all errors with paths.
  4) **Repair.** If there are errors, send them back to the model (the "mistake notebook") and go to step 2. At most 3 repair rounds, then status INVALID with the errors.
  5) **Install on the draft and verify.** Install for real on the draft branch (REQ-PKG-010) and run `package.verify` there.
  6) **Present.** Set status READY_FOR_REVIEW. Notify the requester with a TASK that links to the diff (branch status + diffs), the scenario results and the generated files.
  7) **Publish.** Only through `draft.publish` with the approval of a human holding `definition.author` (REQ-SEC-051). Status becomes APPROVED or REJECTED.
- **Rationale:** UB's reflect → generate → validate → commit loop and UP's AI Architect, with every AI output passing the same gates as human work.
- **Origin:** VRD-16-01, CON-UB-063, CON-UP-061, CON-US-063, CON-US-060 (LLM as compiler)
- **Acceptance:** The requirement "track equipment loans with due dates and reminders" produces a package with Type, Lifecycle, actions, a Cron reminder and scenarios on a draft branch. It is reviewable, and after approval it is published.
- **Priority:** P1

### REQ-AI-011 — Flywheel data
- **Statement:** Human edits made on a builder draft before approval, and rejections with notes, MUST be stored as `feedback` on the AiBuildSession (with the diff between generated and approved content). Sessions are exportable (`ai.build_export@v1`, `tenant.admin`) as training or evaluation samples. Nothing is sent anywhere automatically.
- **Origin:** CON-UB-063 (data flywheel)
- **Acceptance:** An approved session with manual fixes exports as a JSONL pair (requirement, final package).
- **Priority:** P2

### 3.3 Agents

### REQ-AI-020 — Agents as principals
- **Statement:** Creating an Agent MUST also create:
  - its home Context (kind AGENT, memory CACHE or DURABLE);
  - its principal record (kind AGENT) with the HAS_ROLE relationships requested. Granting roles to an agent needs `security.admin`.

  Agents authenticate internally (no password). They can also get API keys for external runtimes (an agent running outside the kernel through the SDK). Every action of an agent is audited with principal = the agent and on_behalf_of = its owner (VRD-16-03).
- **Origin:** VRD-16-03, CON-UC-062, CON-UC-023
- **Acceptance:** An agent's commits show the agent as author and the owner as on-behalf-of, and its memory persists across runs.
- **Priority:** P2

### REQ-AI-021 — Tools
- **Statement:** An agent's `tools` MUST be exposed to the model as function definitions generated from the named operations' schemas (title, description, params/input schema), in the same way as the OpenAPI generation (REQ-PROTO-030). A tool call is executed as the corresponding UBTP operation, under the agent principal, in the agent's home context or a context passed in the call. It is a separate process with `parent_process_id` = the agent run process. Tool results are returned to the model after masking. Calls to operations not listed in `tools` fail with `AI.TOOL_NOT_ALLOWED`, even if the agent's roles would permit them.
- **Origin:** VRD-16-03, CON-UC-064 (`sys_discovery`), CON-UC-058 (tools)
- **Acceptance:** An agent with the tools `query.open_invoices` and `invoice.remind` can list and remind, but its attempt to call `invoice.approve` fails.
- **Priority:** P2

### REQ-AI-022 — Agent runs
- **Statement:** `agent.run@v1 {agent, goal, context?}` (or a Cron or Hook targeting it) MUST start a Job that loops:
  1) call the model with instructions, goal, memory summary and tools;
  2) execute the tool calls (REQ-AI-021);
  3) append the results;

  until the model returns a final answer, or a budget limit is reached (`max_steps`, tokens, run time: `AI.BUDGET_EXCEEDED`).

  Autonomy rules:
  - **SUGGEST:** write tools are not executed. They are returned as proposed intents to the owner.
  - **ACT_WITH_APPROVAL:** write tools become ApprovalRequests, with the agent as requester and the owner as approver by default.
  - **ACT:** writes run subject to policy.

  The run's final answer and a summary are stored in memory (memory policy) and returned as the job result.
- **Origin:** VRD-16-03, CON-UC-062, CON-LC-062 (mandatory human review)
- **Acceptance:** A nightly agent in ACT_WITH_APPROVAL mode creates 3 approval requests for dunning letters and makes no direct writes.
- **Priority:** P2

### 3.4 Copilot

### REQ-AI-030 — Copilot contract
- **Statement:** `ai.copilot@v1 {text, ui_context: {stage_intent?, view?, selection?: [uri]}}` MUST return proposals and never execute anything:
```yaml
proposals:
  - intent: Intent          # SPEC-19 §2.6; resolvable by REQ-FLOW-050
    title: string           # human-readable description
    explanation: string     # why this matches the request
    confidence: 0..1
answer: text?               # optional direct answer (from READ tools only)
citations: [uri with commit]
```
  - The copilot may call READ-effect queries as the **user** (never elevated) to ground proposals. It sees only what the user sees.
  - The client shows proposals in the omnibar or the dock. Selecting one follows the normal intent path, with all its checks.
- **Rationale:** Natural language becomes a first-class input (UC), without an AI shortcut around governance (VRD-16-04).
- **Origin:** VRD-16-04, CON-UC-058, CON-UP-056, CON-UW-020
- **Acceptance:** "approve all invoices under 500 from Hangzhou Tea" returns one RUN_ACTION proposal per matching invoice. Nothing changes until the user confirms each one, or confirms the batch through a bulk action.
- **Priority:** P1

### REQ-AI-031 — Answering over data and definitions
- **Statement:** `ai.ask@v1 {question, scope?: {types?, since?}}` MUST:
  1) retrieve relevant entities through similarity (REQ-QRY-031) and criteria queries, as the asking principal;
  2) retrieve relevant definitions: the `system` package gives meta types (Type, Action, Logic, View) an embedding spec over their descriptions, which enables RAG over the meta-model (VRD-16-05);
  3) answer with citations (canonical URIs with commits).

  If retrieval returns nothing relevant, the answer says so. It does not invent content.
- **Origin:** VRD-16-05, CON-UB-065, CON-UC-064
- **Acceptance:** "Which suppliers delivered late last month?" answers with a list citing Shipment versions. A question about data the user cannot read yields no leaked content.
- **Priority:** P2

### 3.5 Explanations

### REQ-AI-040 — Explanations
- **Statement:** `ai.explain@v1 {process_id | uri (with commit) + path?}` MUST:
  - build its input from `audit.explain_input` (REQ-OBS-016), or, for a value, from blame and lineage (REQ-QRY-020/021);
  - produce a short explanation in the principal's locale, naming the rules, decisions and changes involved, with links.

  When AI is unavailable or not configured, it MUST return a deterministic template explanation built from the same input ("Rejected by invariant `no_overdraft` on /balance: …").
- **Origin:** VRD-16-06, CON-US-061, CON-US-060 (holographic context)
- **Acceptance:** "Why was my approval rejected?" explains the unmet guard, both with AI and in fallback mode.
- **Priority:** P1

### 3.6 AI-native i18n

### REQ-AI-050 — i18n resolution
- **Statement:** Labels, titles and messages MAY reference `i18n:<key>`, which names an `I18nKey` entity. Rendering in locale *L* works as follows:
  1) use the translation for *L* if its `source_hash` equals the current source hash;
  2) else use the fallback chain (e.g. `zh-Hant` → `zh` → source);
  3) if *L* is missing or stale and the tenant enables `ai.translate`, enqueue a TRANSLATE job (deduplicated by key and locale). The job asks the model with the key's `context` and the page context, and commits the translation with `machine: true`.

  Humans may edit translations, which sets `reviewed_by`. Machine translations never overwrite reviewed ones unless the source changed.
- **Origin:** VRD-16-07, CON-UB-064
- **Acceptance:** A new label authored in Chinese appears in English on the second view after the translation job, and editing the source marks the translations as stale.
- **Priority:** P2

### 3.7 AI-readiness

### REQ-AI-060 — Machine-readable platform description
- **Statement:** `ai.context@v1 {topics?: [conventions, meta_schema, abi, package_format, protocol, types(namespace)]}` MUST return a compact, versioned description of the platform for external AI coding agents and tools, assembled from:
  - the spec fixtures (meta-schema, syscalls, URI conformance, CLI commands);
  - SPEC-02 conventions;
  - the package format;
  - the requested type definitions.

  Implementation repositories ship the same material in `AGENTS.md`/`CLAUDE.md` (REQ-CONV-088, VRD-22-01). The UC/LS practice of prompt-driven development is thereby served by first-class, versioned artifacts.
- **Origin:** VRD-22-01, CON-UC-065, CON-LS-063, CON-UB-067, CON-LC-063
- **Acceptance:** An external coding agent given only `ai.context` output and a requirement produces a package that passes `pkg validate`.
- **Priority:** P1
