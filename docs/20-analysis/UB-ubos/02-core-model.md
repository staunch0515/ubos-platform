---
id: ANA-UB-02
title: "UB Core Model"
status: complete
phase: P1
depends_on: [ANA-UB-01]
sources: [UB]
---

# UB — Core Model

### CON-UB-010 — Entity = identity + context + reference
- **What:** An entity is a logical reference that becomes a concrete fact only within a context: **(UUID, Tenant, Branch, Timestamp)**.
- **How:** Storage holds commits and JSON; the entity is a pointer (branch head). Change = move pointer. "Ghost entities" (exists on draft, absent on main) are valid. Each layer sees a different form (URI → rich object → head → id).
- **Why (intent):** Multi-version, multi-tenant systems cannot identify data by id alone.
- **Sources:** [UB:docs/The essence of entities.md], [UB:docs/Rehydration.md] (D-UB-02, D-UB-06).
- **Tags:** identity, context, versioning

### CON-UB-011 — Tenant = database, Type = table, Slug = primary key
- **What:** Instance identity is `(tenant_id, entity_type, slug)` plus a physical UUID `id`; `entity_type` is a coarse **PascalCase resource class** ("folder"/"table": `User`, `Order`, `Book`, `Page`, `Context`, `Action`, `Logic`); the row's actual structure comes from `_extends`.
- **How:** `UNIQUE (tenant_id, entity_type, slug)`; SQL comments "your 'Type as Table'", "your 'PK'". "A table whose rows may each have different columns."
- **Why (intent):** D-UB-08 — familiar RDBMS mapping, small-cardinality type column for partitioning/indexing, id collisions isolated per type, REST-like URIs.
- **Sources:** [UB:backend/src/main/resources/db/migration/V1__init_schema.sql], [UB:docs/UBOS协议的综合处理.md].
- **Tags:** identity, schema, multi-tenancy

### CON-UB-012 — Registry carries lifecycle and custody metadata
- **What:** The instance row holds non-versioned operational metadata.
- **How:** `status` (ACTIVE / DELETED / ARCHIVED; LOCKED designed), `terminator_commit_id` (the commit that deleted it), `display_name` (list without loading JSON), `tags TEXT[]` (GIN), `owner_id` (custody, distinct from commit author), `visibility` (default INTERNAL), `created_at`, `last_modified_at`; partial index on active rows.
- **Why (intent):** D-UB-12 — tombstones with evidence chain; fast "my data"; ownership transfer without new commits; quotas/billing.
- **Sources:** [UB:backend/src/main/resources/db/migration/V1__init_schema.sql], [UB:docs/database_strong.md].
- **Tags:** entity, permission, persistence

### CON-UB-013 — Self-describing, layered semantic type system
- **What:** Types are entities in an explicit inheritance tree from a single root; primitives are abstract storage formats; business code uses only semantic types.
- **How:** L0 `sys.entity.root` → L1 `sys.meta.type` → L2 primitives `sys.type.*` (abstract) → L3 abstract business bases (`sys.biz.abstract.text` with regex/masking; `.math` with scale/unit; `.file` with size/extensions; `.enum` with `dict_code`) → L4 concrete semantic types (`sys.biz.text.email`, `.name`, `.phone`, `.memo`, `.richtext`; `sys.biz.math.money`, `.percentage`; `sys.biz.fin.credit_card` with `masking_rule`; `sys.biz.meas.weight.kg`; `sys.biz.geo.zipcode`; tech IPv4/MAC/cron/semver…) → tenant extensions (`acme.type.company_email`). Type definitions carry `ui_widget`, `validation_regex`, `masking_rule`, `multiply_display`, `is_abstract`, `properties[]`. Boot data `data/boot/init.json`: 112 type definitions with `code`, `name`, `_extends`, `properties`, `is_abstract`, plus units with `scale`.
- **Why (intent):** D-UB-09 — "programming data": UI, validation and masking derive from type; one regex change updates every usage.
- **Sources:** [UB:docs/sys_boot.md], [UB:data/boot/init.json], [UB:backend/src/main/resources/db/migration/V2__data_001.sql].
- **Tags:** meta-model, type-system, ui-rendering, rules

### CON-UB-014 — Universal business ontology in genesis data
- **What:** A pre-built library of business entity types spanning people, organizations, money, measurement, time, geography, documents, IT assets, security and events.
- **How:** Genesis phases 5–17 (D-UB-09): Party → Person / Organization (Legal, Informal); Roles (Customer, Employee…) and **relationships between roles** with `from_date/thru_date`; ContactMech + PartyContactMap (purpose, validity); kernel view/logic/security types (menu, screen, form, workflow, state, action, validation rule, UserLogin ≠ Person, security group, permission); app manifest, i18n translation, notification template, config property, sequence; geography with associations; currency, UOM + conversion, calendar/period; tenant bootstrap data; PKI (CA, public key, certificate); Book/Page templates and instances; IT infrastructure; notifications, alerts (ack), audit/runtime/change logs.
- **Why (intent):** "UBOS does not define *customer*, it defines Party + Role(Customer)" — any vertical is assembled by data.
- **Sources:** [UB:docs/sys_boot.md], [UB:backend/src/main/resources/db/migration/V2..V19].
- **Tags:** meta-model, domain-pack, org-model, finance, document

### CON-UB-015 — `UbosEntity`: effective vs self data
- **What:** The in-memory entity carries identity, source commit/branch, `inheritanceChain`, `effectiveData` (after composition/resolution) and `selfData` (own delta).
- **How:** `set/remove` modify only `selfData`; `toChangeRequest()` produces `DraftSaveRequest{identity, data=selfData, expectedParentCommitId=commitId}` (optimistic lock built in); `getProperties()` wraps effective data.
- **Why (intent):** Editing a child must store only its overrides, never flatten inherited values.
- **Sources:** [UB:backend/src/main/java/com/ubos/core/model/UbosEntity.java].
- **Tags:** entity, versioning, meta-model

### CON-UB-016 — State DTO (live + draft) vs effective entity
- **What:** `EntityStateDTO` shows storage truth for editors (live commit/data/publishedAt and, per user, draft commit/data/updatedAt); `UbosEntity` shows one effective version for execution/rendering.
- **How:** `GetEntityStateHandler` reads `main` head and `draft/{user}` head. Formula: `DTO + intent (branch/commit) + assembler = UbosEntity`.
- **Sources:** [UB:backend/src/main/java/com/ubos/ese/handler/GetEntityStateHandler.java], [UB:docs/design.md].
- **Tags:** versioning, entity

### CON-UB-017 — The context is itself an entity
- **What:** `UbosContext extends UbosEntity` with `tenantId` and `branch`; named contexts are `Context` entities in `sys_boot` mapping a **context slug → (tenantId, branch)**.
- **How:** `ContextEnvironmentProvider.resolveEnvironment(slug)` evaluates `Context/<slug>` in `sys_boot@main` (cached 10 min); unknown slug falls back to tenant = slug, branch = main. Owner's model (D-UB-07): the Java instance, the tenant and the context are entities; execution = context + user + process.
- **Why (intent):** Environments (dev/test/prod, per tenant) are data; URIs and APIs address a context, not raw tenant/branch pairs.
- **Sources:** [UB:backend/src/main/java/com/ubos/core/context/UbosContext.java], [UB:backend/src/main/java/com/ubos/ese/lifecycle/ContextEnvironmentProvider.java].
- **Tags:** context, multi-tenancy, versioning

### CON-UB-018 — `ubos://` URI grammar with branch, intent and matrix parameters
- **What:** URIs address a resource in a context, optionally with branch, a presentation intent and parameters.
- **How:** `UbosUri.parse`: `ubos://{tenant}/{slug}` (type inferred from slug prefix `sys.action.`, `sys.logic.`, `sys.type.`, `sys.view.`, `sys.menu.`), `ubos://{tenant}/{type}/{slug}`, `ubos://{tenant}/{branch}/{type}/{slug}`; suffix intent `.summary|.detail|.json|.schema|.config`; matrix parameters `;key=value`; default branch `main`. The tenant segment is treated as a **context slug** by resolvers. Designs add `@tenant/slug`, `tenant:slug`, bare slugs and **system aliases** (D-UB-08), `@draft` / `@commitId` qualifiers (D-UB-14) and `?render=image` presentation parameters (D-UB-20).
- **Sources:** [UB:backend/src/main/java/com/ubos/core/api/UbosUri.java].
- **Tags:** identity, protocol

### CON-UB-019 — Templates and instances (Book/Page)
- **What:** Composite documents with blueprints: `sys.doc.template.book/page` and instances `sys.doc.book/page`; page `content_data` is static content or a dynamic query.
- **How:** Tenant creation clones the `manual_bootstrap` book template from `ubos_store` into an interactive onboarding book (D-UB-09 phases 14–15).
- **Sources:** [UB:docs/sys_boot.md].
- **Tags:** document, domain-pack, ui-rendering
