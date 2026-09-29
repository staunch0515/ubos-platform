---
id: SPEC-27-1
title: "AI — Model"
status: complete
phase: P4
depends_on: [SPEC-27, SPEC-13, SPEC-16, SPEC-17, SPEC-19, SPEC-21, SPEC-22, SPEC-24, SPEC-28, SPEC-30]
sources: [UP, FU, LC, UB, UC, US, LS, UW]
---

# AI — Model

Part 1 of SPEC-27 (`27-ai/`). Header, concepts and file list: `00-index.md`.

---

## 2. Model

```yaml
ENT-AiProvider:
  purpose: A model endpoint configuration (TERM-AiProvider).
  origin: [VRD-16-02, CON-UB-066, CON-UC-060, CON-FU-061]
  fields:
    - {name: title, type: string, required: true, description: "Name"}
    - {name: kind, type: "enum{OPENAI_COMPATIBLE|ANTHROPIC|OLLAMA|AZURE_OPENAI|CUSTOM_HTTP}", required: true, description: "API dialect"}
    - {name: endpoint, type: string, required: true, description: "Base URL"}
    - {name: api_key, type: "uri<Secret>?", required: false, description: "Credential"}
    - {name: locality, type: "enum{INTERNAL|EXTERNAL}", required: true, description: "INTERNAL = on-premise/private (Ollama, vLLM); EXTERNAL = public service; governs privacy (REQ-AI-003)"}
    - {name: models, type: "map<enum{CHAT|COMPLETE|EXTRACT|EMBED}, {name, max_tokens?, dims?}>", required: true, description: "Models per capability (EMBED needs dims)"}
    - {name: limits, type: "{rpm?, tpm?, timeout_ms (default 60000), max_output_tokens?}", required: false, description: "Rate and time limits"}
    - {name: cost, type: "{input_per_1k: decimal, output_per_1k: decimal, currency}?", required: false, description: "For usage reporting"}
    - {name: active, type: bool, required: true, description: "Enabled"}
```

```yaml
ENT-AiRouting:
  purpose: Tenant/context routing of AI tasks to providers (UB hybrid deployment by task difficulty).
  origin: [CON-UB-066, VRD-16-02]
  fields:
    - {name: rules, type: "list<{task: enum{RUNTIME|BUILD|AGENT|COPILOT|ANSWER|EXPLAIN|TRANSLATE|EMBED}, provider: uri<AiProvider>, model?: string, max_sensitivity?: enum{NONE|PERSONAL}}>", required: true, description: "First matching rule wins; default from tenant settings ai.provider / ai.embedding_provider"}
```

```yaml
ENT-AiBuildSession:
  purpose: Record of one builder run (auditable, feeds the data flywheel).
  origin: [VRD-16-01, CON-UB-063, CON-US-063]
  fields:
    - {name: requirement, type: text, required: true, description: "User's natural-language requirement"}
    - {name: requested_by, type: uri, required: true, description: "Human principal"}
    - {name: target, type: "{package_code, context, base_branch}", required: true, description: "Where the result goes"}
    - {name: draft_branch, type: branch_name, required: true, description: "draft/<builder-agent>/<n>"}
    - {name: iterations, type: "list<{n, prompt_digest, output_digest, errors: [error], tokens, duration_ms}>", required: true, description: "Generate/validate/repair rounds"}
    - {name: package, type: "uri<File>?", required: false, description: "Generated .ubpkg (blob-backed File)"}
    - {name: verify, type: json?, required: false, description: "Scenario results (REQ-PKG-060)"}
    - {name: status, type: "enum{GENERATING|INVALID|READY_FOR_REVIEW|APPROVED|REJECTED|FAILED}", required: true, description: "Outcome"}
    - {name: approval, type: "uri<ApprovalRequest>?", required: false, description: "Publish approval"}
    - {name: feedback, type: "list<{by, kind: ACCEPT|EDIT|REJECT, note?, diff?}>?", required: false, description: "Human corrections (flywheel samples)"}
    - {name: model, type: string, required: true, description: "Provider/model used"}
    - {name: cost, type: "{input_tokens, output_tokens, amount?}", required: true, description: "Usage"}
```

```yaml
ENT-Agent:
  purpose: An AI principal (TERM-Agent, principal kind AGENT).
  origin: [VRD-16-03, CON-UC-062, CON-UC-023]
  fields:
    - {name: title, type: string, required: true, description: "Name"}
    - {name: owner, type: "uri<User>", required: true, description: "Responsible human (on_behalf_of for actions)"}
    - {name: description, type: text, required: true, description: "Purpose; shown to people and used in prompts"}
    - {name: home, type: "uri<Context>", required: true, description: "Context of kind AGENT (memory, env, policies)"}
    - {name: model, type: "{task_routing?: uri<AiRouting>, provider?: uri, model?: string}", required: false, description: "Model choice"}
    - {name: instructions, type: "text | uri<File>", required: true, description: "System instructions"}
    - {name: tools, type: "list<uri>", required: true, description: "Allowed named operations (Actions, Pipelines, Queries) exposed as tools (REQ-AI-021)"}
    - {name: autonomy, type: "enum{SUGGEST|ACT_WITH_APPROVAL|ACT}", required: true, description: "SUGGEST = propose only; ACT_WITH_APPROVAL = writes become approvals; ACT = writes allowed by policy"}
    - {name: budget, type: "{tokens_per_day, operations_per_run, max_steps (default 20), max_run_time (PT15M)}", required: true, description: "Hard limits"}
    - {name: status, type: "enum{ACTIVE|PAUSED|DISABLED}", required: true, description: "Principal status"}
  invariants:
    - The agent's roles come from HAS_ROLE relationships like any principal (REQ-SEC-011); tools outside its permissions fail at call time.
```

```yaml
ENT-I18nKey:
  purpose: A translatable text with AI-assisted translations (VRD-16-07).
  origin: [VRD-16-07, CON-UB-064]
  fields:
    - {name: source_locale, type: string, required: true, description: "Locale of the authored text"}
    - {name: source, type: text, required: true, description: "Source text (ICU MessageFormat placeholders allowed)"}
    - {name: source_hash, type: bytes, required: true, description: "SHA-256 of source (computed)"}
    - {name: context, type: text?, required: false, description: "Usage hint (page, field) for translators and AI"}
    - {name: translations, type: "map<locale, {text, source_hash, machine: bool, reviewed_by?: uri, at}>", required: true, description: "Translations; stale when source_hash differs"}
```
