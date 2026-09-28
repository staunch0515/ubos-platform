---
id: ANA-LS-02
title: "LS Core Model"
status: complete
phase: P1
depends_on: [ANA-LS-01, ANA-LS-07]
sources: [LS]
---

# LS — Core Model

### CON-LS-010 — Entity is a UUID; everything else is a relation
- **What:** An entity document is a map from **relation-type URI** to value: `{ ubos://logrums/MetaRelationType/identity: {tenantId, entityType, slug}, …/label: {title, description}, …/prop_email: "alice@logrums.com", … }`; the physical row has only id + locator.
- **How:** Genesis files and mutations use full URIs as keys [LS:genesis_data/], [LS:apps/ubos_server/src/bin/test_mutate.rs]; `GenesisEntity {identity, #[flatten] relations: HashMap<Uri, Value>}` [LS:crates/ubos_kernel/src/model/genesis.rs].
- **Why (intent):** D-LS-01 — no fields, only typed relations; every attribute carries the full semantics (storage, UI, validation…) of its relation type; globally unambiguous keys.
- **Tags:** meta-model, identity, entity

### CON-LS-011 — Self-describing meta bootstrap
- **What:** `MetaType/MetaType` (root: "definition of all types") declares relation types identity, label, extends, relationTypes, behaviorConfig; `MetaType/MetaRelationType` extends it (adds implements, script, process logic, stage relations); `MetaType/BehaviorType`; the core relations themselves are `MetaRelationType` entities (identity, label, extends, relationTypes, behaviorConfig, implements, script, …).
- **Sources:** [LS:genesis_data/01_meta_type.json], [LS:genesis_data/02_relationType.json], [LS:crates/ubos_kernel/src/constants.rs].
- **Tags:** meta-model, boot

### CON-LS-012 — RelationType as slot configurator (`behaviorConfig`)
- **What:** A relation type fills behavior slots: `behaviorConfig: { <BehaviorType URI>: {impl: <Behavior URI>, config: {…}} }` — e.g. `prop_email`: StorageSlot→StoreText, UISlot→UIInput{icon: mail}, ValidationSlot→ValidEmail; `rel_author`: StorageSlot→StoreRef; `prop_process_logic`: UISlot→UITextarea{mode: code}; `rel_definition`: UISlot→UIRefPicker{targetType: MetaProcessType}. A slot may also embed a script directly (`label` → SchemaProvideSlot script).
- **How:** Assembled relation node = merged `behaviors: {slot URI → BehaviorImpl{engine (default rhai), script, priority}}` [LS:crates/ubos_kernel/src/semantic/relation/types.rs#L10-L65].
- **Why (intent):** Property semantics are composed from reusable behaviours instead of coded per field.
- **Tags:** meta-model, plugin, type-system

### CON-LS-013 — BehaviorType catalogue (20 slots with I/O contracts)
- **What:** Each slot declares `schema {input, output}`: StorageSlot (value→void), UISlot (config→component), ValidationSlot (value→boolean), SchemaProvideSlot (void→json_schema), TransformInputSlot (value→value), DefaultValueSlot (context→value), Before/AfterCreateSlot, Before/AfterUpdateSlot, BeforeDeleteSlot (context→void), PermissionSlot (context→boolean), FieldMaskSlot (value→value), QuotaSlot (context→boolean), SummaryRenderSlot (value→string), NavigationSlot (context→uri), ComputeFieldSlot (context→value), AggregationSlot (context→value), CascadeUpdateSlot (context→void), DependencyCheckSlot (context→boolean).
- **Sources:** [LS:genesis_data/03_base_behavior.json].
- **Why (intent):** A closed vocabulary of extension points covering persistence, UI, validation, security, privacy, lifecycle hooks, computation and navigation.
- **Tags:** plugin, rules, ui-rendering

### CON-LS-014 — Behaviors as implementations with refine chains
- **What:** `Behavior` entities `implements` a BehaviorType and carry a `script` (Rhai or `native::…`) or a `component`: StoreText/StoreJson/StoreRef (`native::store_*`), UIInput (TextInput), UIUpload (FileUploader), ValidRequired, TransformHash (`native::hash_sha256`), MaskLast4, ComputeAge, WelcomeEmail (`native::emit_signal('send_email', …)`). Design: behaviours refine others (EmailValidate refines BaseValidate) and execute layer by layer.
- **Sources:** [LS:genesis_data/03_base_behavior.json], [LS:claude.md].
- **Status:** data implemented; refine execution design.
- **Tags:** plugin, scripting, rules

### CON-LS-015 — Entity types as sets of relation types
- **What:** `MetaType` instances list relation types: `UserType {label, prop_email}`, `BookType {prop_isbn, rel_author…}`, `PageType {prop_page_num, rel_book_ref…}`, `LogicType {prop_engine, prop_script_code…}`, `MetaProcessType`, `ProcessInstanceType`, `ActionType`; instances e.g. `UserType/admin`.
- **Sources:** [LS:genesis_data/04_entity_type.json], [LS:docs/data/51_data.json].
- **Tags:** meta-model, domain-pack

### CON-LS-016 — Identity relation, single tenant, versioned URI
- **What:** Identity is a relation `{tenantId, entityType, slug}` (tenant always `logrums`); URI `ubos://tenant/Type/slug` (host form) or `ubos:///tenant/Type/slug` (path form); version modes `?commit=`, `?tag=`, `?branch=`, `?time=` (head default) [LS:crates/ubos_kernel/src/semantic/uri.rs#L8-L92].
- **Why (intent):** Addressing includes time (commit, time), labels (tag) and lines of work (branch).
- **Tags:** identity, protocol, versioning

### CON-LS-017 — Processes as entities: blueprint vs running instance
- **What:** `MetaProcessType` entities hold transition logic (`prop_process_logic`); `ProcessInstanceType` entities hold `prop_process_cursor` (state), `prop_process_memory` (variables) and `rel_definition` (blueprint); code `ProcessState` also carries `prop_process_logic` (script), `prop_status` and `prop_workspace_snapshot` (environment snapshot per process) [LS:crates/ubos_kernel/src/model/process.rs#L10-L58], [LS:genesis_data/05_process.json].
- **Why (intent):** A process run is an ordinary versioned entity: every step is a commit, history is its version chain.
- **Tags:** workflow, state-machine, versioning

### CON-LS-018 — Actions as entities bound to three stage processes
- **What:** `ActionType` declares relations `rel_stage_init`, `rel_stage_interact`, `rel_stage_exec` (each a StoreRef to a `MetaProcessType`); action parameters are relations on the action type (e.g. `param_target_ref` with UIRefPicker, ValidRequired, SchemaProvide→ProvideEntityPicker); `UniversalEditAction` extends ActionType and binds `Proc_Uni_Init`, `Proc_Uni_Reflect`, `Proc_Uni_Commit`. Earlier variant: single `rel_action_process` → `UniversalEditorFlow` state machine (STATE_INIT → REFLECT → WAIT_INPUT → VALIDATE → COMMIT → DONE).
- **Sources:** [LS:genesis_data/07_action.json], [LS:genesis_data/06_action.json].
- **Tags:** workflow, metadata, ui-rendering

### CON-LS-019 — ChangeSet: the only write unit
- **What:** `ChangeSet {tenant_id, entity_type, slug, payload, action: Create|Update|Delete(soft)|Upsert (default), branch?}` — "the standard input unit of persistence; only core data, no runtime state" [LS:crates/ubos_proto/src/change_set.rs#L10-L52].
- **Why (intent):** Rule "data can only be saved by calling commit_changes".
- **Tags:** persistence, versioning
