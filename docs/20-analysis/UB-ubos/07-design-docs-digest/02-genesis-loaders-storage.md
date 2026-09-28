---
id: ANA-UB-07-02
title: "UB Digest 2: Genesis, Loaders, Boot, Storage Hardening"
status: complete
phase: P1
depends_on: [ANA-UB-07]
sources: [UB]
---

# UB Digest — Part 2: Genesis, Loaders, Boot, Storage

Documents covered: `sys_boot.md` (5,166 lines), `元数据加载器.md` (Meta-Loader, 683),
`boot.md` (552), `database_strong.md` (1,367).

---

## D-UB-09 `sys_boot.md` — The BPU Genesis Protocol (17 phases)

The system bootstraps itself as data, like the JVM loading `Object` and `Class`. Every genesis
change is tied to a genesis **process log** (`proc_genesis_001`, "BPU System Bootstrap").
System tenant: `sys_boot`; store tenant `ubos_store`; first customer `tenant_001`.

| Phase | Name | Content (entity types defined) | Key rationale |
|---|---|---|---|
| 1 | Meta bootstrap | L0 `sys.entity.root` (god object, no parent) · L1 `sys.meta.type` (type of types; fields `java_type`, `validation_rule`) · L2 primitives `sys.type.string/number/boolean/…` · L3 `sys.type.relation` (base of all relations: `source_id`, `target_id`, `relation_strength`) | "The first step is to define *what a definition is*"; "relation is definition" |
| 1.5→2 | Semantic types | Primitive types are **abstract physical storage**, never used directly; abstract business bases `sys.biz.abstract.text` / `.math` (regex, masking, scale, unit) → concrete `sys.biz.text.email`, `.phone`, `sys.biz.math.money`, `.percentage` with `ui_widget`, `validation_regex`, `multiply_display` | **UI auto-generation** from type (`ui_widget: input-email`); change a regex once, all usages update; tenant can extend (`acme.type.company_email`); "Data Virtual Machine — you are programming data" |
| 3 | Time, media, logic | date/timestamp/range; `sys.biz.abstract.file` (`max_size_mb`, `allowed_extensions`) → image/document; enum `sys.biz.abstract.enum` with `dict_code`; memo vs richtext by `ui_widget` | generic file validator by ancestry; dictionaries drive dropdowns |
| 4 | 30+ semantic types | finance (`credit_card` with `masking_rule`), PII (`passport`…), measurement (`weight.kg`…), geo (`country_iso`, `zipcode`, `address_full`), tech (IPv4/IPv6/MAC/cron/semver) | server-side masking on serialization by permission; unit conversion for display; tech formats as first-class citizens |
| 5 | **Party model** (Silverston) | `Party` → `Person`, `Organization` (abstract) → `LegalOrganization` (tax id; can sign contracts) / `InformalOrganization` (team, family); added `sys.biz.text.name` (no line breaks); properties use only `sys.biz.*` types | one party can be customer and employee; business layer never references primitives |
| 6 | **Roles & relationships** ("hat theory") | `sys.entity.party.role` → `Customer` (credit_limit money), `Employee` (job_title), …; `ent_party_rel` between roles with `from_date`/`thru_date` | relationships connect **roles**, not persons; validity periods answer historical questions |
| 7 | **Contact mechanism** | `ContactMech` (email, phone, postal with zipcode type) + `PartyContactMap` (purpose, from/thru) | many-to-many, independent lifecycle, extensible channels; *data type* vs *business object* distinction |
| 8 | **Kernel types** | View (`sys.kernel.view.menu/screen/form`), Logic (`workflow`, `state`, `service action`, `validation rule`), Security (`user_login` separate from `party.person`, `security_group`, `permission`) | UI/workflow/security are entities; one generic page renderer; workflow defines available buttons |
| 9 | **Runtime infrastructure** | `sys.app.manifest` (app packaging, version, dependencies), `sys.i18n.translation`, notification templates + trigger rules, `sys.config.property` (feature toggles), `sys.data.sequence` (document number rules) | modular apps; revertible translations; numbering without code; runtime config with audit |
| 10 | **Geography** | `sys.entity.geo.country/province/city` + `GeoAssoc` hierarchy | reference data vs semantic type (`"CN"` code vs the country entity); virtual regions ("Greater China", "remote areas" for shipping) |
| 11 | **Universal reference data** | `Currency` (decimal places), `UOM` + `UomConversion` (box = 12 each), `Calendar/Period` (fiscal year, 4-4-5 calendar, holidays) | physics/economics/time standards drive formatting and calculations |
| 12 | **Tenant initialization** | in `tenant_001`: Org, Person (admin), UserLogin, SecurityGroup (super admin), User–Group map | minimum viable loop for first login |
| 13 | **Digital identity & PKI** | `sys.sec.pki.authority` (tenant CA), `public_key`, `certificate` (party, key, issuer) | **data sovereignty**: private key stays on device; high-risk commits signed and verified |
| 14 | **Book & Page** | `sys.doc.template.book/page` (blueprints) and `sys.doc.book/page` (instances); `content_data` static text or dynamic query | composite documents (Excel workbook/sheets, Notion); template-based delivery to tenants |
| 15 | **Bootstrap manual** | book template `manual_bootstrap` in `ubos_store` with pages Welcome, Organization, Administrator, Digital Cert | on `OnTenantCreate`, clone into the new tenant; first login opens the unread system-guide book; "configuration as an interactive book" |
| 16 | **IT infrastructure** | digital units (bytes, Hz, bit rate), host/CPU/memory/disk/NIC, OS/file/directory/mount, port/connection/socket | CMDB/ITAM; file hash → intrusion detection; topology; ops automation via workflows |
| 17 | **Communication & events** | notifications/announcements/messages, alerts (with `ack_status`, escalation), audit log, runtime log, data-change log | notification = inform; alert = command requiring acknowledgement; audit has legal weight; change log enables whole-database time machine |

**Genesis landscape** (summary table in the doc): L0 kernel (process, entity, versioning;
phase 1) · L1 semantics (2–4) · L2 physics (10–11) · L3 subject (5, 6, 13) · L4 assets (14,
16) · L5 interface (8) · L6 nervous (8, 17) · L7 runtime (9, 12, 15). Claim: UBOS defines
`Party + Role(Customer)` instead of "customer", `Asset + UOM + Location` instead of
"inventory", `Screen template + widget config` instead of screens — "you can turn it from an
HR system into IoT monitoring by inserting data".

Tail sections: an EAV **inheritance index** (sync `sys_code` and `_extends` of main-branch
heads into `bpu_entity_search_index` via PL/pgSQL; self-join and recursive CTE to find all
descendants of a type), data-integrity check SQL, and a transactional **tenant wipe** script.
Sections marked "[废]" (discarded) / "[假]" (fake) were superseded: earlier phase 2/3/4 drafts
(organization & permissions, state machines & workflow with a Japan-market note, business
instances).

## D-UB-10 `元数据加载器.md` — Meta-Loader: ingest vs runtime registry (683 lines)

### A. Genesis Ingestor (write path)
- Runs as system user `sys_loader`; a client of **Layer 2 (`EntityStateManager`)** — no back door.
- Components: `MetaSourceReader` (classpath/filesystem/remote → `RawEntityDefinition`s) → `DependencyResolver` (DAG over `_extends`, **topological sort into batch levels**) → `LoaderOrchestrator`.
- 5-step algorithm per batch: (1) identity resolution by slugs (existing vs missing); (2) `initializeEntities` for missing → ids; (3) **dirty check** by SHA-256 of JSON vs live data — skip unchanged; (4) `saveDrafts` to branch **`draft/sys_loader`** with `expectedParentCommitId` (optimistic lock); (5) `publishEntities` into main.
- Parents must be *published* before children are validated (batches). Failure stops boot (metadata is foundational); optimistic-lock conflicts may be retried.
- Why JSON instead of SQL seed scripts: going through the kernel triggers validation, caches, indexes and events; readable; order-independent via dependency resolution. Folder layout `genesis/01_kernel/…`.
- **"Remote soul injection"**: the source can be the central **UBOS Store** (`POST https://store…/api/genesis/sync`); a freshly installed client downloads the genesis bundle like `git clone` and ingests it through its own kernel so the system is "alive" immediately.

### B. Runtime Metadata Registry (read path; the owner's meaning of "meta-loader")
- `BpuRuntimeRegistry`: in-memory registry of live types/logic/layouts, per tenant, only **main-branch heads**, `_extends` resolved into Java-level inheritance.
- Lifecycle: on start (or `MetadataUpdatedEvent` after ingest) → reload tenant metadata → "kernel started".
- The two loaders form a loop: Installer (ingest, to *have* data) + Registry (load, to be *fast*).

## D-UB-11 `boot.md` — Boot hardening (552 lines)

- Owner's idea: a **running-instance entity** (e.g. `LocalhostApp`) should provide DB/tenant defaults instead of hard-coded `defaultTenant`.
- **All references become `ubos://` URIs**, including property `type` and relation `target` — unified addressing, cross-tenant references, no ambiguity; Phase-1 composer gains a **schema normalization** step that expands short references to canonical URIs after merging.
- **Centralized provisioning boot (UNBP — UBOS Network Boot Protocol)**: node computes a **hardware fingerprint** (MAC/board serial hash) → asks the central server for a **boot permit** (`BootPermit`: tenant, role, config, expiry) → initializes context and warms caches per permit. Nodes are stateless terminals; a stolen node is disabled centrally; role changes (worker ↔ api) by reconfiguring centrally. Security: HMAC signature over `nodeId + timestamp` with a pre-shared secret.

## D-UB-12 `database_strong.md` — Storage hardening (1,367 lines)

- **Tombstone pattern** instead of hard delete: delete = insert a tombstone commit + link to process + update instance lifecycle; `terminator_commit_id` gives the full evidence chain (who, when, why); undelete = status back + head to last non-null commit; history references remain valid.
- **Lifecycle status** on instances: `ACTIVE` (rw), `DELETED` (invisible except privileged, ro), `ARCHIVED` (read-only cold data), `LOCKED` (read-only during approval/stock-taking).
- **`owner_id` vs `author_id`**: owner = current custody (like Unix file owner; mutable), author = who committed (immutable history). Uses: fast "my data" queries, implicit owner permission, **ownership transfer** without new commits (sales rep leaves), quotas/billing. Owner may be user, group/department or system.
- **Schema v2.1 review** — physical trace of a "ship" operation: resolve URI → registry id; load head + snapshot; apply **RFC 6902 JSON Patch** in memory; write process log, new version, process map; CAS-move head. Registry separates slug from id; `tags`, `display_name` enable listing without loading JSON; **full snapshots** chosen over deltas for read speed; `embedding vector` column makes the store a vector DB for RAG; `bpu_process_commit_log` carries `app_id` and **`billing_commit_id`**.
- **`seq_num` moved to branch head** ("block height" per branch); CAS update on `(tenant, entity, branch, seq_num)`; merge increments target's sequence only.
- Independent review (headed "Suggestion from Claude"): strengths (everything-is-entity, git for data, audit by default, dual keys, partial indexes, DAG versions, partitions, vector + JSONB); risks and remedies: hot `branch_head` rows (optimistic retry, advisory locks per shard, or queue-based async commits), JSONB schema drift (validation), ivfflat maintenance (prefer HNSW, async embeddings, separate table), partition automation, soft-delete overhead, missing FKs; query strategies (materialized views, search index, redundant columns on instance); point-in-time queries, diff in application layer, rollback; **PostgreSQL row-level security** per tenant, tenant sharding for large tenants; growth estimate (version chain ~365 M rows/year for 1 M users) → monthly partitions and cold archive (S3 + Parquet).
