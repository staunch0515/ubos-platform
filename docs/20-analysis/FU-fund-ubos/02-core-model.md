---
id: ANA-FU-02
title: "FU Core Model"
status: complete
phase: P1
depends_on: [ANA-FU-01, ANA-UP-02]
sources: [FU]
---

# FU — Core Model

Base model = UP (ANA-UP-02: instance / version chain / branch head / branch config / process
log / relations). FU adds tenant scoping, a canonical URI, namespaces and a catalogue of
governance entity types.

### CON-FU-010 — Canonical `ubos://` URI (backend grammar)
- **What:** Every resource version is addressable by one string.
- **How:**
  - Grammar: `ubos://{scope}/{type}/{slug}?branch={b}&commit={id}&tag={t}&key={path}`; short form `ubos://{type}/{slug}?…` (scope = `default`).
  - `scope` = tenant id or app id; `type` lower-cased on parse; default branch constant `main` (other code paths default to `master` — the two defaults coexist).
  - `commit` → exact version (time travel); `tag` → release tag (parsed, not resolved); `key` → dot/array-index path extraction inside the JSON (`feature_flags.ai_enabled`, `items.0.price`).
  - Custom parser (explicitly not `java.net.URI`), URL-decoding of segments; `build()` is the inverse.
  - `UbosResourceResolverService.resolve(uri)`: commit → snapshot by id; else tenant-aware lookup with `scope` as tenant; then optional key extraction.
- **Why (intent):** Javadoc examples `ubos://default/logic/tax-calc?branch=master`, `ubos://tenant_01/user/user-001?branch=development&commit=12345`, `ubos://app_core/config/global_settings?branch=dev&key=feature_flags.ai_enabled` — one address format for API, policies, approvals, merge inputs.
- **Sources:** [FU:backend/src/main/java/org/logrum/ubos/kernel/util/UbosUriUtil.java], [FU:backend/src/main/java/org/logrum/ubos/kernel/service/UbosResourceResolverService.java].
- **Tags:** identity, protocol

### CON-FU-011 — Extended URI modes (frontend grammar)
- **What:** The Studio's URI utilities define seven URI modes beyond the standard form.
- **How:**

| Mode | Form | Meaning (from names) |
|---|---|---|
| standard | `ubos://{type}/{slug}?branch=&cid=` | resource on branch, optional commit |
| ctx | `ubos://ctx/{slug}` | resource relative to current context |
| commit | `ubos://commit:{hash}/{slug}` | resource at a commit |
| tag | `ubos://tag:{name}/{slug}` | resource at a release tag |
| self | `ubos://self` | the current entity |
| alias | `ubos://alias:{name}` | named alias |
| remote | `ubos://@{host}/{slug}` | resource on another UBOS node |

  Note: the frontend uses `cid=` where the backend uses `commit=`.
- **Why (intent):** Anticipates relative, versioned, aliased and federated references.
- **Sources:** [FU:frontend/src/utils/useUbosUri.ts], [FU:frontend/src/utils/ubosUri.ts].
- **Tags:** identity, protocol, integration

### CON-FU-012 — Tenant-scoped identity
- **What:** Entity instances carry `tenant_id`; a tenant-aware code path scopes lookups, creation and branch heads by tenant.
- **How:** `V1` adds `tenant_id VARCHAR(64) NOT NULL` to `lcm_entity_instance`; `commit(…, processId, tenantId)`; `findEntityId(type, slug, tenantId)`; head upsert `ON CONFLICT (tenant_id, entity_id, branch_name)`; `getResourceSnapshot(uri, tenantId)`. The legacy (UP) path remains tenant-less. Client keeps `tenantId` in Redux (`tenantSlice`, default `Tenant_A`).
- **Why (intent):** Move from "tenant = branch" (UP) to explicit tenant partitioning while keeping branches for variants.
- **Sources:** [FU:backend/src/main/resources/db/migration/V1__init_ubos_schema.sql], [FU:backend/src/main/java/org/logrum/ubos/kernel/service/LcmKernelService.java#L1069-L1165], [FU:frontend/src/store/tenantSlice.ts].
- **Tags:** multi-tenancy, identity

### CON-FU-013 — Namespaces from dotted slugs
- **What:** A slug such as `finance.taxes.calc_rate` is treated as a virtual path; namespaces are derived, not stored.
- **How:** Backend `findBySlugPrefix(prefix, type)` (`LIKE 'prefix.%'`), endpoint `/api/console/entities/navigate`; frontend `parseSlugsToTree` builds a folder tree (folders first, alphabetical), breadcrumbs, parent path.
- **Why (intent):** Javadoc: "Slug is treated as a dot-separated virtual path … list everything under a directory."
- **Sources:** [FU:backend/src/main/java/org/logrum/ubos/kernel/service/LcmKernelService.java#L182-L238], [FU:frontend/src/utils/namespaceTree.ts].
- **Tags:** identity, frontend

### CON-FU-014 — Governance entity catalogue
- **What:** Seven platform-level entity types with typed payloads.
- **How:**

| entity_type | slug | Payload fields |
|---|---|---|
| `SCHEMA` | target entity type (upper-case) | JSON Schema draft-07 document |
| `USER` | username | username, email, displayName, groups[], enabled, createdAt, lastLoginAt, metadata |
| `GROUP` | groupName | groupName, displayName, description, roles[], policies[], parentGroup, createdAt, metadata |
| `POLICY` | policyName | policyName, description, rules[{effect, actions[], resources[], conditions{branches[], entityTypes[], timeWindow}}], priority, enabled, createdAt, metadata |
| `SECURITY_KEY` | keyId (UUID) | keyId, keyName, hashedKey, description, scope, active, createdAt, expiresAt, createdBy |
| `WEBHOOK` | hookName | hookName, triggerEvent, targetUrl, entityType, secretToken, active, retryCount, timeoutMs, description, createdAt, createdBy |
| `APPROVAL_REQUEST` | `REQ-XXXXXXXX` | requestId, targetUri, originalPayload, status, requestedBy, approver, commitMessage, targetCommitId, requestedAt, resolvedAt, rejectionReason |

- **Why (intent):** Governance configuration is ordinary versioned data (CON-FU-001).
- **Sources:** [FU:backend/src/main/java/org/logrum/ubos/web/console/dto/] (`*Payload.java`).
- **Tags:** metadata, auth, permission, integration

### CON-FU-015 — Soft deletion by tombstone commit
- **What:** Deleting a user commits a tombstone payload instead of removing rows.
- **How:** `deleteUser` commits `{username, groups:[], enabled:false, metadata:{deleted:true, deletedAt}}`; API keys and webhooks are "deactivated" by committing `active:false`.
- **Why (intent):** Deletion keeps history and is itself auditable/revertable.
- **Sources:** [FU:backend/src/main/java/org/logrum/ubos/kernel/service/LcmUserService.java], [FU:backend/src/main/java/org/logrum/ubos/kernel/service/ApiKeyService.java].
- **Tags:** versioning, entity

### CON-FU-016 — Stable identity, mutable slug (rename as commit)
- **What:** Renaming changes the slug on the identity row and records a commit describing the move.
- **How:** `renameEntity(uri, newSlug)`: uniqueness check per type → `UPDATE lcm_entity_instance SET slug` → commit `{action:"RENAME", oldSlug, newSlug}` on the branch → move head. `entity_id` never changes.
- **Why (intent):** Separates immutable identity (UUID) from human-facing name (slug), like a file move in Git.
- **Sources:** [FU:backend/src/main/java/org/logrum/ubos/kernel/service/LcmKernelService.java#L891-L979].
- **Tags:** identity, versioning
