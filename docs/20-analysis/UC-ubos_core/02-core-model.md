---
id: ANA-UC-02
title: "UC Core Model"
status: complete
phase: P1
depends_on: [ANA-UC-01, ANA-UC-07]
sources: [UC]
---

# UC — Core Model

### CON-UC-010 — Entity: UUID identity + business locator + payload + inheritance + head
- **What:** `Entity { id (UUID, 32 hex), tenant_id, entity_type, slug, payload: JSON, extends: Vec<URI>, head_commit_id, created_at, _resolver }`.
- **How:** `Entity::new` derives `extends` from `payload._extends` (string or array) and generates a UUID if none is given; `reconstruct` rebuilds from storage [UC:src/domain/entity.rs#L9-L84]. The locator `(tenant_id, entity_type, slug)` is unique in `bpu_entity_instance` (`uq_instance_locator`); `head_commit_id` is the optimistic-lock version.
- **Why (intent):** Early stages used a composite string id `tenant:type:slug` (D-UC-02); the final model separates physical identity (UUID, stable across renames) from the business locator (human, unique per tenant+type). "Tenant = database, type = table, slug = primary key" (same idea as CON-UB-011).
- **Evolution:** stage 1 entity had `version_hash` / `parent_hash` (in-memory Git) (D-UC-01).
- **Sources:** [UC:src/domain/entity.rs], [UC:docs/db/V1_init.sql], [UC:docs/master/06_connect db.md].
- **Tags:** identity, entity, versioning

### CON-UC-011 — Entity type is open data, not a compiled enum
- **What:** `EntityType(pub String)` — a newtype over a string ("physical partition tag"), with a few system constants: `Workspace`, `Logic`, `Job`, `File`, `Cron`, `Hook`.
- **How:** [UC:src/domain/entity_type.rs#L10-L32]; any string creates a new "logical table"; the unique locator keeps slugs unique per type. Plan_16 proposes defining a new type by saving a type entity (`SysEntityType:Book`) that holds schema and index configuration.
- **Why (intent):** D-UC-45 — "remove the enum limit": new business types without recompiling ("dynamic modelling").
- **Evolution:** stage 1 `EntityType` enum (`SysFunction` code vs `SysLogrum` runtime vs `SysData`) (D-UC-01).
- **Sources:** [UC:docs/plan/Plan_16.md].
- **Tags:** meta-model, type-system, schema

### CON-UC-012 — Purely explicit inheritance via `_extends` URIs
- **What:** Every entity declares its parent(s) with `_extends: "ubos://logrums/Type/<code>"`; no implicit type-based inheritance.
- **How:** Deploy, job creation, genesis workspace and loader all inject `_extends` [UC:src/kernel/manager.rs#L14-L19], [UC:src/kernel/bootstrap/loader/processor.rs#L49-L55]. Property lookup: `recursive_find_prop` checks `properties[].default` of the current entity, then recurses into each `_extends` parent by locator, depth ≤ 20, skipping self-reference [UC:src/kernel/resolution.rs#L11-L59]. Syscall `sys_resolve_prop(uri_or_id, key)` exposes it to scripts [UC:src/kernel/syscalls/db/reader/resolve.rs]. `Kernel::resolve_property(type, key)` is deliberately disabled ("in pure explicit inheritance mode … must have an entity instance").
- **Why (intent):** `prompt.md` rule "entities must use purely explicit inheritance"; the chain is data and visible, which also drives UI selection (CON-UC-055).
- **Sources:** [UC:docs/prompt.md], [UC:src/kernel/mod.rs#L58-L70].
- **Tags:** meta-model, inheritance, rehydration

### CON-UC-013 — Logic (mould) and Job (cast)
- **What:** `Logic { name, script_content, input_schema }` is the static definition; a `Job` is one execution instance created from it.
- **How:** `Job { id, function_id, tenant_id, user_id, context_data (args), status PENDING|RUNNING|DONE|ERROR, result, parent_job_id, logs }`; `to_entity()` stores it as a `Job` entity with slug `job-<id>` [UC:src/domain/job.rs#L25-L75], [UC:src/domain/logic.rs#L8-L31].
- **Why (intent):** D-UC-01 — "SysFunction is the mould, Logrum is the casting". A Logrum/Job is a **persisted execution capsule**: it freezes time (inputs and code are fixed), supports long-running async work, and keeps compute stateless ("why must Logrum be persisted as an entity?" — Plan_01 stage 2).
- **Sources:** [UC:docs/master/01_first step.md]…[UC:docs/master/05_fifth.md].
- **Tags:** scripting, process, versioning

### CON-UC-014 — Strict wiring of inputs
- **What:** The kernel refuses to create an execution whose declared inputs are not all supplied.
- **How:** Stage 3: `required_inputs` checked at `invoke`; missing parameters → no Logrum is created. Stage 19 (design): `input_schema` is real JSON Schema validated before execution, with smart coercion string→number/bool. Final code stores `input_schema` but performs no validation; the UI uses it to render forms (CON-UC-054).
- **Why (intent):** "Garbage in, garbage out" prevention; logic only reads its capsule ("code may only obtain parameters from the Logrum").
- **Sources:** [UC:docs/master/03_third step.md], [UC:docs/master/19_schema.md].
- **Tags:** validation, scripting
- **Status:** design (validation); implemented (schema storage).

### CON-UC-015 — Workspace (formerly Logrum): the environment entity
- **What:** A stable environment in which executions run: `Workspace { id, name (= slug), tenant_id (sovereign tenant), default_branch, memory: Map<String,JSON>, env_vars: Map<String,String> }`.
- **How:** Persisted as a `Workspace` entity (`owner_tenant`, `branch`, `memory`, `env_vars`, `_extends`), projected in memory by `Workspace::from_entity`, cached in Redis `ubos:workspace:<id>` [UC:src/domain/workspace.rs#L10-L76]. The genesis workspace `logrums/Workspace/genesis` is created at boot [UC:src/kernel/bootstrap/env.rs#L8-L36].
- **Why (intent):** D-UC-27/D-UC-40 — "Logrum is the environment, Process is the execution, the user lives in the Process, everything is an entity". The front end selects a Workspace first; it fixes tenant and branch for everything after (D-UC-51).
- **Evolution:** "Logrum" first meant the execution record (stages 1–18), then the Redis session container `LogrumContext{tenant, user, process, session_data, env_vars}` (D-UC-23), then a pure environment (D-UC-27), finally renamed `Workspace` in code; execution records became `Job`.
- **Sources:** [UC:docs/master/28_redis.md], [UC:docs/master/35_logrum.md], [UC:docs/plan/Plan_11.md].
- **Tags:** context, multi-tenancy, versioning

### CON-UC-016 — `ubos://tenant/type/slug[?commit=N]`
- **What:** Canonical URI; `commit` pins a historical version.
- **How:** `UbosUri::parse` requires exactly three non-empty path segments and supports only the `commit` query parameter; `Display` round-trips [UC:src/domain/uri.rs#L23-L87]. Used by `Kernel::resolve`, `sys_db_get`, `sys_resolve_prop`, `_extends`, genesis `type`/`target` references, and the resource browser.
- **Why (intent):** D-UC-47 — from "location addressing" to "intent addressing": a global, semantic address for any resource; the namespace segment may map to a Workspace/tenant; later uses listed: field-level references (instead of embedded copies), cross-tenant sovereignty declaration, `target_func` of Cron/Hook as a full address (cross-tenant Open API), `?resource=` page parameters, `import "ubos://…"` modules, lineage records by URI, shadow environments by remapping the namespace.
- **Sources:** [UC:docs/plan/plan_18_ubos.md].
- **Tags:** identity, protocol

### CON-UC-017 — UODS: a strict meta-schema for metadata definitions
- **What:** "UBOS Object Definition Standard v4.0 Strict": validation rules every genesis/type definition must satisfy; the loader must reject violations.
- **How:** Object level: `code` (`^[a-z0-9_]+$`), `name`, `_extends` (null only for `root`), `entity_type` ∈ {Type, Function, Context, User} mandatory; `is_abstract`, `description`, `ui_widget` optional; `properties` required when non-abstract and adding fields. Property level: `name`, `type` (a Type URI) mandatory; `required`, `default` (must match type), `is_searchable`. Type-specific: `relation` ⇒ `target` mandatory; `enum_single/multi` ⇒ `dict_code` mandatory; number/measure ⇒ `scale` (recommended), `unit_symbol`; meta primitives ⇒ `primitive_type` ∈ {TEXT, NUMBER, BOOL, TIME, MAP, LIST} and `store_type` ∈ {VARCHAR, NUMERIC, JSONB, BOOLEAN, TIMESTAMPTZ}. Includes a "garbage data self-check" section.
- **Why (intent):** D-UC-54 — "without a meta-schema, piling up JSON is producing garbage data"; only defined requiredness makes data executable.
- **Status:** design; the loader in code is permissive (CON-UC-033).
- **Sources:** [UC:docs/init_data.md].
- **Tags:** meta-model, validation, schema

### CON-UC-018 — Genesis semantic type ontology (15 files, 150 definitions)
- **What:** A single-rooted semantic type tree plus a business ontology, all as `Type` entities in tenant `logrums`.
- **How:** 01 meta kernel (`root` → `meta` → string/text/number/integer/boolean/date/map/list/relation with primitive/store types) · 02 business types (abstract_text → name/code/email/url/phone; abstract_math → money/percent/file_size; abstract_enum → enum_single/multi; switch; file_ref/image_ref) · 03 technical & PII formats (ipv4/ipv6/mac/semver/color/hash/pem/password(bcrypt)/uuid/passport/ssn/credit_card/iban/sku with `validation_regex`, `masking_rule`) · 04 contact & geo (zipcode, lat/long with min/max/scale, geo_boundary → country/province/city/district, contact_mech → email/telecom/postal/web) · 05 party & roles (party → person/organization → org_legal/org_group; party_role → employee/customer/supplier; party_relationship; party_contact_map) · 06 documents & messages (msg_root, templates, book/page templates and instances, document, avatar) · 07 security & audit & UI (user_login, sec_group, maps, permission, log_audit, log_data_change, view_screen, view_menu) · 08 IT infrastructure (it_asset → host/nic/disk; process, port, volume, directory, file, heartbeat) · 09 kernel logic (sys_function, sys_action, sequence, config, i18n key, app, workflow, workflow_state) · 10 reference data (currency, fx rate scale 6, UOM, conversion scale 10, period type/period, holiday) · 11 measures (length…power, all `measure` → `abstract_math`) · 12 PKI (key, certificate, CA) · 13 (duplicate of 01) · 14 kernel scripts (`fn_save_raw`, `fn_view_entity`, `fn_sys_policy` in Rhai) · 15 UI config (views `scr_home`, `scr_schema_list`, `scr_data_editor` with `config_json`).
- **Why (intent):** Same universal-data-model intent as CON-UB-014 (Party/Role/Relationship, contact mechanisms, reference data); here normalized to UODS and flat `code`s. Types carry validation, masking, widget and precision so UI and checks derive from them.
- **Sources:** [UC:docs/db/genesis_data/], [UC:docs/init_data.md].
- **Tags:** meta-model, type-system, domain-pack, org-model

### CON-UC-019 — `logrums`: the sovereign root tenant and a constitution in scripts
- **What:** The system's root tenant is `logrums` ("collective intelligence" metaphor); all system types, logic and the genesis workspace live there; only `logrums` may create users and communities (tenants).
- **How:** Plan_13: "Genesis reset" (root tenant `logrums`), "constitution" functions `create_user` / `create_community` whose scripts begin with `if ctx.identity.tenant_id != "logrums" { throw "CONSTITUTIONAL VIOLATION" }`. Code: deploy/invoke look Logic up in `logrums`; `sys_db_get` allows reading own tenant or `logrums` (public zone) [UC:src/kernel/syscalls/db/reader/get.rs#L28-L55].
- **Why (intent):** A single sovereign root with a written constitution; other tenants ("communities") are chartered by it.
- **Sources:** [UC:docs/plan/Plan_13.md], [UC:docs/prompt.md].
- **Tags:** multi-tenancy, governance, permission
