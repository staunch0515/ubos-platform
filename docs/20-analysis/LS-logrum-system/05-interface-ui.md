---
id: ANA-LS-05
title: "LS Interfaces & UI"
status: complete
phase: P1
depends_on: [ANA-LS-01, ANA-LS-03]
sources: [LS]
---

# LS — Interfaces & UI

LS has no graphical client; interaction is specified through protocol test clients and
server-pushed UI instructions.

### CON-LS-050 — Protocol test clients as executable scenarios
- **What:** Binaries with a client-side codec (encode `UbosOp`, decode `UbosResp`): `test_emit` (Emit `SchemaProvideSlot` on `MetaType/UserType`), `test_mutate` (create `UserType/alice` with label and email relations), `test_query` (query alice), `test_process` / `test_approve` (create `PROCESS/AliceApproval` with cursor/memory/logic, emit submit/approve, query final cursor), `test_notify` (process that emits `Notify` to `client://current`), `test_universal` (process calling `ctx.emit(SchemaProvideSlot)` and emitting `RenderForm`) [LS:apps/ubos_server/src/bin/].
- **Tags:** protocol, workflow, observability

### CON-LS-051 — Server-pushed UI instructions
- **What:** `Emit{target_uri: client://current, signal: RenderForm, args: {title, schema: [fields]}}`; `Notify`; `QueryResult`.
- **Why (intent):** The server decides what the client renders next; clients are thin renderers of signals.
- **Tags:** ui-rendering, protocol

### CON-LS-052 — Form schema from SchemaProvide slots per relation
- **What:** Each relation contributes field definitions `{id|key, label, widget (text, email, ref_picker…), required, placeholder}`; e.g. genesis `label` relation's script builds fields for every relation of the entity type.
- **Sources:** [LS:genesis_data/02_relationType.json], [LS:crates/ubos_kernel/src/semantic/behavior.rs].
- **Tags:** ui-rendering, metadata

### CON-LS-053 — UI widgets as behaviors of the UI slot
- **What:** `UISlot` implementations: UIInput → TextInput, UIUpload → FileUploader, UITextarea{mode: code}, UIRefPicker{targetType}, readonly config — widget choice is part of the relation type configuration.
- **Sources:** [LS:genesis_data/03_base_behavior.json], [LS:genesis_data/05_process.json].
- **Tags:** ui-rendering, plugin

### CON-LS-054 — Universal editor as pure data
- **What:** `UniversalEditAction` + three stage processes (or the single `UniversalEditorFlow` state machine) edit any entity: pick target → reflect its type's relations into a form (with permission, compute, mask) → validate per relation → emit `Mutate` per relation.
- **Sources:** [LS:genesis_data/07_action.json], [LS:genesis_data/06_action.json].
- **Tags:** workflow, ui-rendering, metadata
