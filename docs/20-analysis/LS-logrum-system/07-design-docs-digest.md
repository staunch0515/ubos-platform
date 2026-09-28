---
id: ANA-LS-07
title: "LS Design Docs Digest"
status: complete
phase: P1
depends_on: [INV-LS]
sources: [LS]
---

# LS — Design Documents Digest

| D-id | Document | Lines | Topic | Concepts |
|---|---|---|---|---|
| D-LS-01 | claude.md | 122 | Relational architecture refactoring brief | CON-LS-010, 012, 014, 021, 022, 063 |
| D-LS-02 | genesis_data/01_meta_type.json | 53 | MetaType, MetaRelationType, BehaviorType | CON-LS-011 |
| D-LS-03 | genesis_data/02_relationType.json | 116 | Core relation types (+ label SchemaProvide script) | CON-LS-011, 052 |
| D-LS-04 | genesis_data/03_base_behavior.json | 391 | 20 BehaviorTypes + 10 Behaviors | CON-LS-013, 014, 053 |
| D-LS-05 | genesis_data/04_entity_type.json | 239 | Property relation types + LogicType, UserType, BookType, PageType | CON-LS-012, 015 |
| D-LS-06 | genesis_data/05_process.json | 148 | Process relations, MetaProcessType, ProcessInstanceType, OrderFlow, order_001_run | CON-LS-017, 020 |
| D-LS-07 | genesis_data/06_action.json, 07_action.json, docs/data/06_action.json | 475 | Action variants (single state machine; three-stage processes) | CON-LS-018, 022, 054 |
| D-LS-08 | README.md, console.md, docs/data/51_data.json | 58 | Tagline, dev commands, admin user instance | CON-LS-001, 015 |

### D-LS-01 — claude.md
- Request (verbatim rules): "Entity is just a UUID; everything else is a Relation." · "The value of tenantId only can be logrums." · "Data can only be saved by calling commit_changes." · "Do not include any comments in the JSON file."
- Core logic: BehaviorType = interface ("what kind of logic can be placed here", e.g. ValidateType, RenderType); Behavior = implementation; Refine = inheritance chain between behaviours executed layer by layer (onion / chain of responsibility); RelationType = configurator filling slots with behaviours; execution flow: Process emits an instruction carrying a BehaviorType → system finds entity → EntityType → iterates RelationTypes → finds configured slot → finds Behavior → runs its refine chain.
- Principle "scripts only give orders, the engine runs its legs off": scripts prepare parameters, emit (`ctx.emit`) directly to BehaviorType URIs, aggregate returned lists into front-end formats.
- Three stages with detailed slot sequences (CON-LS-022), ending with scripts assembling `Mutate` instructions.

### D-LS-02…06 — Genesis data
- Every entity is a map keyed by relation-type URIs; identity relation holds tenant/type/slug.
- Meta layer (01–02), behaviour layer (03), property relations and entity types (04), process layer with a sample order approval flow (`INIT --submit--> REVIEW --approve--> APPROVED` with a Mutate setting `prop_status = PAID`) and a running instance at cursor `REVIEW` (05).

### D-LS-07 — Actions
- `06_action.json` (loaded by the bootloader): `rel_action_process`, `ActionType`, `UniversalEditorFlow` (five-state editor machine using `native::load_entity`, `native::load_type`, `native::exec_behavior` over the type's relations), `UniversalEditAction`.
- `07_action.json` (latest design; invalid JSON due to a stray token; not loaded): stage relations, `param_target_ref`, `ActionType`, `UniversalEditAction` (a MetaType extending ActionType), `Proc_Uni_Init/Reflect/Commit` scripts using `ctx.emit` (CON-LS-022).
- `docs/data/06_action.json`: intermediate variant (`rel_process_stage_init/inter/exec`, `Proc_Universal_Init/Query/Commit`).

### D-LS-08 — Other
- README tagline; `console.md` shares US commands (Docker Postgres/Redis, source bundling for AI, `cargo run -p ubos_server`); `51_data.json` = `UserType/admin` with label and email.
