---
id: SPEC-23
title: "Tenancy"
status: complete
phase: P4
depends_on: [SPEC-11, SPEC-12, SPEC-13, SPEC-14, SPEC-16, SPEC-22]
sources: [UP, FU, LC, UB, UC, LS, UW]
---

# Tenancy

## 0. Chapter header

- **Scope:** This chapter covers:
  - the `Tenant` entity and tenant lifecycle (provisioning, suspension, archiving, purge);
  - the root tenant `logrums` and its "constitution";
  - namespaces and their global ownership;
  - the resolution order across tenants and branches;
  - tenant customisation of platform entities (extension and overlay patches, with patch policies);
  - cross-tenant grants and foreign-logic execution;
  - public sharing;
  - isolation in depth (kernel, RLS, dedicated storage);
  - quotas and metering.
- **MOD:** `TEN`
- **depends_on:** SPEC-11, SPEC-12, SPEC-13, SPEC-14, SPEC-16, SPEC-22.
- **Terms used:** TERM-Tenant, TERM-RootTenant, TERM-Namespace, TERM-Overlay, TERM-TypeExtension, TERM-SovereignTenant, TERM-HostTenant, TERM-Context.
- **Origin summary:**
  - Verdicts: VRD-18-01…05, VRD-02-07 (Q-006), VRD-07-05.
  - UB: tenant as a key dimension and context entities, with RLS and partitions noted (CON-UB-011, CON-UB-017, CON-UB-040).
  - UC: the `logrums` root with its constitution, host vs. sovereign tenants, and tenant checks in syscalls (CON-UC-019, CON-UC-024).
  - UP: overlay inheritance of the platform (CON-UP-041, CON-UP-052).
  - FU: tenant-scoped identity (CON-FU-012).
  - LC: app scoping (CON-LC-016).
  - UW: the tenant shown in the working context (CON-UW-011).

---

## 1. Concepts

Tenancy and variation are two separate dimensions (CMP-18):

| Dimension | Answers | Mechanism |
|---|---|---|
| **Tenancy** | Who owns the data? Who may see it? | `tenant_id` on every row; Tenant entities; grants; RLS |
| **Variation** | Which version of the world applies here? | branches (overlay, draft, feature, release), Contexts, pins |

```
            logrums (root: kernel types, ontology, platform packages)   ← readable by all, writable by root admins
               ▲ read-only fallback
   ┌───────────┴───────────┐
 tenant acme            tenant beta             ← own data, own branches, own Contexts
   │ own types (acme.*)     │
   │ TypeExtensions on root types (acme.* properties)
   │ overlay patches of root entities (tenant-scoped heads, patch policy)
   └── grants ──► beta (TENANT_GRANT: acme may read beta's ubos://beta/Catalog/**)
```

---

## 2. Model

```yaml
ENT-Tenant:
  purpose: A tenant (TERM-Tenant); entity of type Tenant in logrums, slug = tenant code.
  origin: [VRD-18-01, CON-UB-011, CON-UC-019, CON-FU-012]
  fields:
    - {name: title, type: string, required: true, description: "Display name"}
    - {name: status, type: "enum{PROVISIONING|ACTIVE|SUSPENDED|ARCHIVED|PURGING}", required: true, description: "Lifecycle (REQ-TEN-003)"}
    - {name: kind, type: "enum{ORGANIZATION|PERSONAL|SYSTEM}", required: true, description: "SYSTEM only for logrums; PERSONAL for the personal edition 'local'"}
    - {name: namespaces, type: "list<string>", required: true, description: "Namespaces claimed (REQ-TEN-010); read-only here, maintained via Namespace entities"}
    - {name: isolation, type: "enum{SHARED|DEDICATED_PARTITION|DEDICATED_SCHEMA}", required: true, description: "Storage isolation mode (REQ-TEN-032); default SHARED"}
    - {name: public_access, type: bool, required: true, description: "Allow anonymous / other-tenant reads of PUBLIC entities (REQ-TEN-025); default false"}
    - {name: settings, type: TenantSettings, required: true, description: "§2.1"}
    - {name: quotas, type: TenantQuotas?, required: false, description: "§2.1"}
    - {name: admin, type: "uri<User>", required: true, description: "Initial tenant admin (in the tenant)"}
    - {name: parent, type: "uri<Tenant>?", required: false, description: "Organisational parent (communities, UC); informational in version 1"}
  invariants:
    - Code (slug) satisfies REQ-CONV-012 and is not reserved (REQ-URI-018).
    - Tenant entities are written only by processes of logrums root admins or the kernel.
```

### 2.1 Settings and quotas

| Setting | Default | Used by |
|---|---|---|
| `locale`, `timezone` | `en`, `UTC` | REQ-CTX-014 |
| `idempotency_ttl` | PT24H (max P7D) | REQ-TX-015 |
| `read_audit` | false | REQ-ARCH-005 |
| `post_filter_scan_limit` | 10 000 | REQ-QRY-006 |
| `apikey_max_lifetime` | P365D | REQ-SEC-001 |
| `retention.process_log` | P7Y | REQ-STO-015 |
| `retention.versions_online` | P3Y (older partitions may be archived; heads always online) | REQ-STO-015 |
| `ai.provider`, `ai.embedding_provider` | none | SPEC-27 |
| `trace_default` | SUMMARY | REQ-CTX-009 |

| Quota | Meaning | Enforcement |
|---|---|---|
| `max_entities` | ACTIVE registry rows | `quota` check at CREATE (TEN.QUOTA_EXCEEDED) |
| `max_storage_bytes` | snapshots + blobs | at flush / blob put |
| `max_requests_per_minute`, `max_concurrent_processes` | load | gateway (REQ-SEC-062) |
| `max_jobs_queued` | queue depth | job submit |
| `max_ai_tokens_per_day` | AI usage | AI syscalls (SPEC-27) |

```yaml
ENT-Namespace:
  purpose: Global claim of a dotted namespace by a tenant (entity in logrums, slug = namespace).
  origin: [VRD-02-07, CON-UP-014, CON-FU-013, CON-LC-016]
  fields:
    - {name: tenant, type: "uri<Tenant>", required: true, description: "Owning tenant"}
    - {name: kind, type: "enum{TENANT|PACKAGE_VENDOR}", required: true, description: "PACKAGE_VENDOR namespaces may be installed in other tenants (SPEC-28)"}
  invariants:
    - A namespace is claimed by exactly one tenant; nested namespaces (acme.crm under acme) belong to the owner of the parent unless explicitly delegated.
```

```yaml
ENT-TenantGrant:
  purpose: TENANT_GRANT relationship metadata (SPEC-22 §2.2) - cross-tenant access.
  origin: [VRD-18-04, CON-UC-024]
  fields:
    - {name: permissions, type: "list<string>", required: true, description: "Permission patterns granted to the grantee tenant's principals (further filtered by their own roles)"}
    - {name: resources, type: "list<string>", required: true, description: "URI globs in the granting tenant"}
    - {name: principals, type: "list<string>?", required: false, description: "Restrict to principals/roles of the grantee"}
    - {name: embassy, type: bool?, required: false, description: "Allow ambassador sessions of the grantee inside the granting tenant (REQ-TEN-022)"}
```

```yaml
ENT-OverlayPolicy:
  purpose: Type.overlay_policy - how tenants may patch root-owned instances of this type.
  origin: [VRD-18-02, VRD-08-02, NEW]
  fields:
    - {name: mode, type: "enum{NONE|PRESENTATION|FULL}", required: true, description: "NONE = no patches; PRESENTATION = only title, description, icon, ui_modes, display, i18n, default_views, and narrowing of constraints; FULL = any own-data change allowed by narrowing rules"}
```

---

## 3. Behavior

### 3.1 Tenants

### REQ-TEN-001 — Tenant key everywhere
- **Statement:** Every row of the six tables and of the database-resident technical tables MUST carry a `tenant_id` (SPEC-11). Every kernel operation runs with the data tenant and read tenants of its execution context (REQ-CTX-011). No kernel API accepts data without a tenant.
- **Origin:** VRD-18-01, CON-UB-011
- **Acceptance:** A schema check shows `tenant_id` on every table. A store call without a tenant does not compile (the type system requires `TenantCode`).
- **Priority:** P0

### REQ-TEN-002 — Root tenant and constitution
- **Statement:** The tenant `logrums` MUST exist from genesis. It owns:
  - the meta-model and kernel types;
  - Native descriptors;
  - the ontology and platform packages;
  - Tenant and Namespace entities;
  - the kernel service accounts.

  Its entities are readable by every authenticated principal of every tenant (REQ-SEC-011 step 3). Only principals with `ubos.root_admin` and the kernel identity can write them. The **constitution operations**, `tenant.create`, `tenant.suspend`, `tenant.archive`, `tenant.purge` and `namespace.claim`, are Actions in `logrums` whose permissions are held only by root admins, unless a self-service policy grants `tenant.create`.
- **Rationale:** A single sovereign root with written rules (UC "constitution") that is enforced by policies rather than by `if` statements in scripts.
- **Origin:** VRD-18-01, CON-UC-019
- **Acceptance:** A tenant admin cannot commit to `logrums`. A root admin can create a tenant.
- **Priority:** P0

### REQ-TEN-003 — Tenant lifecycle
- **Statement:** The tenant lifecycle MUST work as follows:
  - **Creation** (`tenant.create@v1 {code, title, admin: {username, email}, packages?}`) runs as follows:
    1) A root process commits the Tenant (status PROVISIONING) and its Namespace claims.
    2) A provisioning Job runs in the new tenant as `logrums/ServiceAccount/kernel`. It creates, in one flush:
       - the `main` Branch entity;
       - `Context/default`;
       - `ServiceAccount/system` and `ServiceAccount/anonymous` (DISABLED);
       - the baseline Roles `tenant.admin`, `editor` and `viewer`, and the baseline Policies;
       - the admin User with a setup token (REQ-SEC-004 pattern);
       - the requested packages (SPEC-28).
    3) A root process sets the status to ACTIVE.

    Provisioning is idempotent. A failed job is retried and leaves the status at PROVISIONING.
  - **SUSPENDED:** logins and writes are rejected (`TEN.SUSPENDED`). Reads are allowed for tenant admins only.
  - **ARCHIVED:** read-only for tenant admins. Jobs and schedules are stopped.
  - **PURGING:** see REQ-TEN-004.
- **Origin:** VRD-18-01, CON-UC-019 (create_community), CON-UC-033 (genesis workspace)
- **Acceptance:**
  1) After `tenant.create`, the admin can log in with the setup token and see an empty cockpit with the default Context.
  2) Suspending the tenant blocks their next write.
- **Priority:** P0 (create, ACTIVE), P1 (suspend, archive)

### REQ-TEN-004 — Tenant purge
- **Statement:** `tenant.purge@v1 {code, confirm: code, export_first: bool}` MUST run as follows:
  1) require `ubos.root_admin` and a REQUIRE_APPROVAL statement with 2 approvers (non-disableable);
  2) when `export_first` is true, first export all heads and history (SPEC-28 export);
  3) set PURGING;
  4) physically delete all rows with `tenant_id = code` from the six tables, the technical tables, the runtime store and the blobs referenced only by that tenant;
  5) keep the Tenant entity in `logrums` with status PURGING → deleted (tombstone) and a purge record in `details`.

  This is the **only** operation allowed to delete version and process rows (an exception to REQ-STO-003). It is recorded as a root ADMIN process.
- **Rationale:** Contractual and legal deletion of a whole tenant needs physical removal. Immutability applies within a tenant's lifetime.
- **Origin:** NEW: required for real deployments (privacy/NFR-PRIV)
- **Acceptance:** After a purge, no row with that tenant code remains, and the root audit shows the purge process with its approvals.
- **Priority:** P2

### 3.2 Namespaces

### REQ-TEN-010 — Namespace claims
- **Statement:** A tenant MUST own every namespace it uses for type codes, extension property keys and package slugs (REQ-CONV-015/018/022), with one exception: namespaces of installed vendor packages keep their vendor's namespace. Namespaces are claimed through `namespace.claim@v1`, which creates a Namespace entity in `logrums`. The tenant's code is claimed automatically at creation. Committing a definition in an unclaimed or foreign namespace fails with `TEN.NAMESPACE_NOT_OWNED`.
- **Rationale:** Global ownership keeps packages portable between tenants and avoids collisions in the resolution order.
- **Origin:** VRD-02-07 (Q-006), CON-UP-014, CON-FU-013
- **Acceptance:** Tenant `beta` cannot create type `acme.Invoice`. An installed vendor package `crmkit.*` may.
- **Priority:** P0

### REQ-TEN-011 — No shadowing of root codes
- **Statement:** A tenant MUST NOT create a type, action or other definition whose locator (type code and slug) equals a root entity's locator. Tenant definitions are namespaced (REQ-CONV-018), and the only way to change a root entity is an overlay patch (REQ-TEN-016) or a TypeExtension. An attempt fails with `TEN.SHADOWS_ROOT`.
- **Origin:** VRD-18-02, REQ-CONV-018
- **Acceptance:** Creating `Type/Customer` in tenant acme fails, while `Type/acme.Customer` succeeds.
- **Priority:** P0

### 3.3 Resolution order

### REQ-TEN-015 — Read order
- **Statement:** For an execution context with data tenant *t*, branch chain *b₁…bₙ* (most specific first) and read tenants *[t, g₁…gₖ, logrums]*, the kernel MUST resolve entity *e* owned by tenant *o* as follows:

| Owner of *e* | Pairs tried, in order |
|---|---|
| *t* | (t, b₁) … (t, bₙ) |
| `logrums` | (t, b₁) … (t, bₙ) (tenant patches), then (logrums, main) |
| a granted tenant *g* (REQ-TEN-020) | (g, main), or the branch the grant names |
| any other tenant | not visible (`URI.NOT_FOUND`) |

  This order is the VRD-18-02 order: tenant draft → tenant branch → tenant overlay of root → root. SPEC-14 §1.2 applies inside each tenant's branch chain, and SPEC-21 REQ-QRY-002 applies the same order to queries.
- **Origin:** VRD-18-02, REQ-STO-006, REQ-VER-003
- **Acceptance:** A root View patched by acme on `main` and again on a draft shows the draft patch on the draft, acme's patch on main, and the root version in tenant beta.
- **Priority:** P0

### REQ-TEN-016 — Overlay patches of root entities
- **Statement:** A tenant MAY commit versions of root-owned entities in its own head space (REQ-STO-006), subject to the `overlay_policy` of the entity's type:
  - NONE rejects the patch (`TEN.PATCH_FORBIDDEN`).
  - PRESENTATION allows only the presentation fields and narrowing changes (REQ-META-014).
  - FULL allows any change that passes validation and narrowing.

  Default policies:
  - FULL: `View`, `Widget`, `Dictionary` (a tenant may add entries; removing entries requires FULL).
  - PRESENTATION: `Type`, `Lifecycle`.
  - NONE: `Logic`, `Action`, `Pipeline`, `Workflow`, `Native`, `Policy`, `Role`, `Package`, `Tenant`, `Namespace`, `RelationshipType`.

  Patches require `definition.author` in the tenant. `version.unset` removes a patch (reset to inherit).
- **Rationale:** Customisation without copying (UP overlay), while platform behaviour stays controlled by the root (VRD-18-02).
- **Origin:** VRD-18-02, VRD-08-02, CON-UP-041
- **Acceptance:**
  1) A tenant re-labels the root `Person.birth_date` property and changes its widget.
  2) Removing the property or patching a root Logic is refused.
- **Priority:** P1

### REQ-TEN-017 — Extensions of root types
- **Statement:** Adding properties, rules or type-level slot bindings to root types MUST use TypeExtension entities in the tenant (SPEC-13 §2.8), with keys in the tenant's namespaces (REQ-CONV-022). Extensions apply in the owning tenant only. Extensions owned by `logrums` apply to all tenants.
- **Origin:** VRD-02-07, VRD-18-02 (Q-006)
- **Acceptance:** acme's extension adds `acme.vat_id` to `Organization`. beta's Organizations do not have it, and acme's queries can filter on it when it is searchable.
- **Priority:** P1

### 3.4 Cross-tenant access

### REQ-TEN-020 — Tenant grants
- **Statement:** A tenant admin of *G* MAY grant tenant *T* access to resources of *G* by creating a TENANT_GRANT relationship (*G* → *T*) in `logrums`. This goes through `tenant.grant@v1`, which requires `tenant.admin` in *G*. Principals of *T* then:
  - have *G* in their read tenants for resources matching the grant;
  - still need matching permissions from their own roles or policies;
  - see *G*'s entities read-only, unless the grant includes write permissions. Writes to *G* then run with data tenant *G* in a separate process (REQ-CTX-012).

  Every cross-tenant read is recorded as DEPENDS_ON_DATA with the foreign commit. Revoking a grant takes effect at once (the cache is invalidated by event).
- **Origin:** VRD-18-04, CON-UC-024, VRD-10-02
- **Acceptance:** acme reads `ubos://beta/Catalog/**` after beta's grant and gets `URI.NOT_FOUND` after revocation.
- **Priority:** P1

### REQ-TEN-021 — Foreign logic
- **Statement:** When a process of host tenant *H* invokes Logic owned by another tenant *S*, the following applies:
  - The Logic must be visible to *H*: it is PUBLIC, or *S* granted *H* `logic.execute` on it.
  - If `pure`, it runs with *H* as the data tenant and cannot read *S*'s data beyond the Logic and its imports.
  - If effectful, it may be invoked only inside an ambassador Context of *S* in *H* (REQ-TEN-022). Its writes go to *S*.

  These cases are the table of REQ-CTX-011.
- **Origin:** VRD-18-04, VRD-07-05, CON-UC-024
- **Acceptance:** Calling a vendor's effectful logic from a normal acme context fails with `TEN.FOREIGN_EFFECTFUL`.
- **Priority:** P2

### REQ-TEN-022 — Ambassador contexts
- **Statement:** A SESSION or AGENT Context of sovereign tenant *S* with `host_tenant = H` MUST be resolvable only if a TENANT_GRANT *H* → *S* exists with `embassy: true`. Processes in such a context follow REQ-CTX-011. *S*'s writes stay in *S* and are invisible to *H* unless shared (grant *S* → *H* or visibility PUBLIC).
- **Origin:** VRD-18-04, CON-UC-023, CON-UC-024
- **Acceptance:** A vendor agent session inside acme stores its working notes under the vendor tenant, and acme's users cannot see them.
- **Priority:** P2

### REQ-TEN-025 — Public sharing
- **Statement:** Entities with `visibility = PUBLIC` in a tenant with `public_access = true` MUST be readable by principals of every tenant and by `anonymous` (when enabled), at the tenant's `main`. They are subject to DENY statements and masking. Public reads are never writable across tenants.
- **Origin:** VRD-10-07, NEW: catalogues, published documents, marketplace
- **Acceptance:** An unauthenticated request for a PUBLIC product page of a public tenant succeeds. A PRIVATE one returns `URI.NOT_FOUND`.
- **Priority:** P2

### 3.5 Isolation and quotas

### REQ-TEN-030 — Isolation in depth
- **Statement:** Tenant isolation MUST be enforced at three levels:
  1) The kernel: the PDP tenant boundary (REQ-SEC-011 step 2) and URI resolution (REQ-TEN-015).
  2) The store API: every transaction is bound to a tenant (API-STO-001), and writes to other tenants are impossible except patches of root entities.
  3) In the enterprise edition, PostgreSQL RLS (REQ-STO-016).

  Code paths that must cross tenants are root administration, grants and provisioning. They use the kernel identity and log `cross_tenant: true` in the process metrics.
- **Origin:** VRD-18-05, CON-UC-019, CON-UB-040 notes
- **Acceptance:** A fault-injection test that removes the kernel check (in a test build) still cannot read another tenant's rows through the enterprise store.
- **Priority:** P0 (1, 2), P1 (3)

### REQ-TEN-031 — Quotas
- **Statement:** The kernel MUST enforce tenant quotas (§2.1) at the listed points and return `TEN.QUOTA_EXCEEDED` with the quota name, limit and current value. Usage counters are kept in the runtime store and reconciled daily from the database by a SYSTEM job.
- **Origin:** NEW: operability; CON-UB-043 (billing readiness)
- **Acceptance:** Creating an entity beyond `max_entities` fails, and deleting one frees a slot.
- **Priority:** P2

### REQ-TEN-032 — Dedicated storage for large tenants (enterprise)
- **Statement:** A tenant with `isolation = DEDICATED_PARTITION` MUST have its rows in dedicated list partitions (by `tenant_id`) of the six tables, below the yearly range partitions. `DEDICATED_SCHEMA` MUST place its rows in a separate schema with the same DDL, selected by the store per transaction. Changing isolation is an ADMIN migration job that moves rows while the tenant is SUSPENDED. The data model and APIs do not change.
- **Origin:** VRD-18-05, D-UB-12 scale notes
- **Acceptance:** A tenant moved to a dedicated partition keeps identical query results.
- **Priority:** P2

### REQ-TEN-033 — Metering
- **Statement:** Usage MUST be derivable from `process_log`:
  - processes by kind and app;
  - commits;
  - `metrics` (operations, AI tokens, duration);
  - blob bytes.

  A SYSTEM billing job MAY periodically aggregate usage into `BillingBatch` entities (tenant, period, counters) in `logrums` and set `process_log.billing_commit_id` for the processes it accounted (UB billing linkage, REQ-STO-009).
- **Origin:** CON-UB-043, CON-FU-044
- **Acceptance:** A monthly BillingBatch lists per-app process counts that match an audit query over the same period.
- **Priority:** P2

---

## 4. Interfaces

### API-TEN-001 — TenantService
- **Kind:** rust trait (`ubos_kernel::tenancy`)
- **Signature / shape:**
```rust
#[async_trait]
pub trait TenantService: Send + Sync {
    async fn tenant(&self, code: &TenantCode) -> Result<Arc<TenantInfo>, UbosError>;              // cached
    fn read_order(&self, x: &ExecutionContext, owner: &TenantCode) -> Vec<(TenantCode, BranchName)>; // REQ-TEN-015
    async fn check_namespace(&self, tenant: &TenantCode, code: &str) -> Result<(), UbosError>;     // REQ-TEN-010
    async fn grants_for(&self, grantee: &TenantCode) -> Result<Arc<Vec<TenantGrant>>, UbosError>;
    async fn quota_check(&self, tenant: &TenantCode, q: QuotaKind, delta: i64) -> Result<(), UbosError>;
}
```
- **Origin:** CON-UB-017, CON-UC-024

### API-TEN-002 — Named operations
- **Kind:** UBTP targets (Actions in `logrums`)
- **Signature / shape:**

| URI | Args | Permission |
|---|---|---|
| `ubos://system/Action/tenant.create@v1` | `{code, title, admin: {username, email}, packages?, isolation?}` | `ubos.root_admin` or a self-service policy |
| `ubos://system/Action/tenant.suspend@v1` / `tenant.resume@v1` / `tenant.archive@v1` | `{code, reason}` | `ubos.root_admin` |
| `ubos://system/Action/tenant.purge@v1` | `{code, confirm, export_first}` | `ubos.root_admin` + 2 approvals |
| `ubos://system/Action/tenant.settings@v1` | `{code, settings}` | `tenant.admin` of the tenant (settings) / root (quotas, isolation) |
| `ubos://system/Action/namespace.claim@v1` | `{namespace, kind}` | `tenant.admin` |
| `ubos://system/Action/tenant.grant@v1` / `tenant.revoke_grant@v1` | `{grantee, permissions, resources, principals?, embassy?}` | `tenant.admin` of the granting tenant |
| `ubos://system/Query/tenant.usage@v1` | `{code, period}` | `tenant.admin` |
- **Origin:** CON-UC-019 (constitution functions)

### Error codes (TEN)

| Code | Category | Meaning |
|---|---|---|
| `TEN.SUSPENDED` | FORBIDDEN | tenant suspended or archived |
| `TEN.NAMESPACE_NOT_OWNED` | FORBIDDEN | definition in a namespace the tenant does not own |
| `TEN.NAMESPACE_TAKEN` | CONFLICT | namespace already claimed |
| `TEN.SHADOWS_ROOT` | CONFLICT | locator equals a root entity's |
| `TEN.PATCH_FORBIDDEN` | FORBIDDEN | overlay patch not allowed by overlay policy |
| `TEN.CROSS_TENANT_WRITE` | FORBIDDEN | write to another tenant's entity |
| `TEN.FOREIGN_EFFECTFUL` | FORBIDDEN | effectful foreign logic outside an embassy |
| `TEN.QUOTA_EXCEEDED` | LIMIT | quota exceeded (details) |
| `TEN.RESERVED_CODE` | INVALID | reserved tenant code |

---

## 5. Non-functional

| ID | Requirement | Target |
|---|---|---|
| NFR-SCAL-120 | Tenants per enterprise installation (shared isolation) | ≥ 10 000 |
| NFR-PERF-120 | Tenant provisioning (without optional packages) | ≤ 5 s |
| NFR-SEC-120 | Cross-tenant leakage | 0 in the isolation test suite on both editions |
| NFR-PRIV-120 | Tenant purge completeness | no remaining rows, runtime keys or orphan blobs |

---

## 6. Acceptance

| REQ | Criterion |
|---|---|
| REQ-TEN-001…004 | Tenant key; root constitution; provisioning idempotency; suspend; purge |
| REQ-TEN-010, 011 | Namespace ownership; no shadowing |
| REQ-TEN-015…017 | Read order across drafts, patches and root; overlay policies; extensions per tenant |
| REQ-TEN-020…025 | Grants and revocation; foreign logic rules; ambassador contexts; public sharing |
| REQ-TEN-030…033 | Isolation in depth; quotas; dedicated storage; metering |

---

## 7. Implementation notes

- **Reference material:**
  - UC tenant rule in syscalls (own tenant or `logrums`) [UC:src/kernel/syscalls/db/reader/get.rs#L28-L55].
  - UC constitution scripts [UC:docs/plan/Plan_13.md].
  - UB context and tenant handling [UB:backend/src/main/java/com/ubos/ese/lifecycle/ContextEnvironmentProvider.java].
  - UP overlay fallback [UP:ubos-server/src/main/java/org/logrum/ubos/kernel/service/LcmKernelService.java#L54-L89].
- **Divergences resolved:**
  - UP's "tenant = branch" and LC's `app_id` scoping become tenants plus namespaces. Apps are packages.
  - UC's hard-coded tenant checks move into the PDP.
- **Type addition:** SPEC-13's Type gains `overlay_policy` (ENT-OverlayPolicy). Its default for new types is PRESENTATION.

## 8. Open questions

None. Deferred to a later version: tenant hierarchies with inherited policies (`parent`, informational only in version 1); data residency by region.
