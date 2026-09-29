---
id: SPEC-22-2
title: "Security and Governance — Behavior"
status: complete
phase: P4
depends_on: [SPEC-22, SPEC-12, SPEC-13, SPEC-15, SPEC-16, SPEC-17, SPEC-18]
sources: [UP, FU, LC, UB, UC, US, LS]
---

# Security and Governance — Behavior

Part 2 of SPEC-22 (`22-security/`). Header, concepts and file list: `00-index.md`.

---

## 3. Behavior

### 3.1 Authentication

### REQ-SEC-001 — Credentials and principals
- **Statement:** The kernel MUST authenticate every request to exactly one Principal `{id, uri, kind USER|SERVICE|API_KEY|AGENT|SYSTEM|ANONYMOUS, tenant, scopes?}` by one of these methods:

| Method | Binding | Result |
|---|---|---|
| Password (`username` + password; argon2id verify) | Handshake / `POST /auth/login` | access + refresh tokens |
| OIDC authorization code (P1) | `GET /auth/oidc/{provider}` flow | tokens; user linked or provisioned by `external_ids` |
| Access token (JWT, EdDSA, 15 min) | `Authorization: Bearer` / Handshake `auth.token` | principal from claims |
| Refresh token (opaque, 30 days, rotating, runtime store) | `POST /auth/refresh` | new token pair |
| API key (`ubos_<43 base64url chars>`) | `X-API-Key` header / Handshake `auth.api_key` | the key's principal with scopes |
| Local owner (personal edition only) | in-process binding | the `local` tenant's owner user |
| Anonymous | no credentials, tenant allows it | `ServiceAccount/anonymous` |

  Tokens carry `{sub: principal URI, tenant, kind, sid (session), exp, scopes?}`. Principals with status other than ACTIVE are rejected with `SEC.PRINCIPAL_INACTIVE`, and revoked or expired keys with `SEC.UNAUTHENTICATED`.
- **Origin:** VRD-10-01, VRD-10-05, CON-FU-003, CON-FU-023, CON-UP-018
- **Acceptance:**
  1) Login returns tokens.
  2) A disabled user's token is rejected on the next request within 60 s (token check against a principal-status cache).
  3) An API key acts with scope intersection.
- **Priority:** P0 (password, token, API key), P1 (OIDC, refresh rotation)

### REQ-SEC-002 — Credential storage
- **Statement:**
  - Passwords MUST be stored only as argon2id hashes, produced by the `transform.hash_argon2@v1` slot at staging.
  - API keys MUST be shown in plaintext exactly once, in the create response, and stored as a SHA-256 hash with a searchable prefix.
  - Secrets MUST be encrypted (REQ-SEC-060).
  - None of these values is ever returned by reads, queries, traces or audit records. Their properties have `sensitivity: SECRET` and an `access` slot that denies read.
- **Origin:** VRD-10-01, CON-FU-023, CON-UC-018 (password bcrypt)
- **Acceptance:** Querying Users with `fields: ["password"]` fails with `QRY.PATH_FORBIDDEN`. The trace of a login contains no hash.
- **Priority:** P0

### REQ-SEC-003 — Login protection
- **Statement:** The authentication service MUST do the following:
  - count failed logins per (tenant, username) and per source address in the runtime store;
  - set User `status := LOCKED` after 10 consecutive failures, through a SYSTEM process;
  - answer at a constant rate for unknown and known usernames;
  - rate-limit login attempts per source address (default 20 per minute).

  Successful logins are recorded as processes of kind ADMIN with operation `ubos://system/Native/auth.login@v1` (no commits, `audit_always`). Failed attempts are recorded in telemetry only (SPEC-30).
- **Origin:** NEW: baseline account protection
- **Acceptance:** The 11th wrong password returns `SEC.PRINCIPAL_INACTIVE`, and the User shows LOCKED with an audit row.
- **Priority:** P1

### REQ-SEC-004 — First administrator
- **Statement:** At genesis (SPEC-28), the kernel MUST create `ubos://logrums/User/admin` with the role `ubos.root_admin`. Its password is taken from `UBOS_BOOTSTRAP_ADMIN_PASSWORD` if set. Otherwise it is set through a one-time setup token printed to the host log. The token is valid for 24 h, is single-use, and is stored hashed in the runtime store. There is no "allow everything when no users exist" mode.
- **Rationale:** FU allowed everything while no users existed. This replaces that with an explicit bootstrap.
- **Origin:** CON-FU-022 (bootstrap mode, replaced), CON-UC-019
- **Acceptance:** A fresh server rejects anonymous writes. Using the setup token sets the admin password, and a second use fails.
- **Priority:** P0

### 3.2 Permissions and authorisation

### REQ-SEC-010 — Permission names
- **Statement:** Permissions MUST be dotted names drawn from this catalogue. Patterns may use `*` for one segment and `**` for the rest.

| Permission | Checked when |
|---|---|
| `entity.read` / `entity.create` / `entity.update` / `entity.delete` / `entity.rename` / `entity.copy` / `entity.unset` | resolving reads; staging by action |
| `entity.history` | history, blame, diff, as-of reads |
| `entity.edit_raw` | raw own-data editing (`entity.edit_raw@v1`) |
| `entity.unmask.<SENSITIVITY>` | reading unmasked PERSONAL or SECRET-class values where masking applies |
| `entity.transfer_owner` / `entity.archive` / `entity.lock` | registry administration |
| `action.<slug>` | executing an action (e.g. `action.acme.invoice.approve.v1`, or `action.acme.invoice.*`) |
| `query.<slug>` / `pipeline.<slug>` / `operation.execute` | running named operations |
| `logic.execute` / `logic.author` | invoking or committing Logic |
| `definition.author` | committing Type, TypeExtension, Lifecycle, Action, Pipeline, Workflow, View, Widget, Constraint, Policy, Role, Package |
| `branch.create` / `branch.delete` / `branch.merge` / `branch.publish` / `version.revert` / `ubos.admin.reset` | versioning |
| `secret.read` / `secret.write` | secrets |
| `security.admin` | users, groups, roles, policies, keys of the tenant |
| `approval.decide` | voting on approvals (together with the approval spec) |
| `ai.use` / `ai.build` | AI syscalls and the builder (SPEC-27) |
| `http.egress` | `http.request` (together with the Context allowlist) |
| `ubos.rt.eval`, `ubos.tx.draft_level`, `ubos.trace.detailed`, `ubos.qry.deleted` | special kernel features used in other chapters |
| `tenant.admin` / `ubos.root_admin` | tenant administration / root tenant administration |
- **Origin:** VRD-10-02, CON-FU-022 (actions COMMIT/REVERT/MERGE/READ/APPROVE/DELETE/CREATE_BRANCH)
- **Acceptance:** Every permission used by another chapter appears in this catalogue or in an ADR.
- **Priority:** P0

### REQ-SEC-011 — Authorisation algorithm (PDP)
- **Statement:** `authorize(principal, permission, resource, env)` MUST return a Decision (SPEC-18 model, priority 900) as follows:
  1) **Principal.** If the principal is not ACTIVE: DENY (veto).
  2) **Tenant boundary.** The resource's tenant must be one of the execution context's read tenants (reads) or its data tenant (writes), as allowed by SPEC-16 REQ-CTX-011 and SPEC-23 grants. Otherwise the result is DENY (veto), reported as not found.
  3) **Root tenant.** Resources owned by `logrums` are readable by every authenticated principal and writable only with `ubos.root_admin`. This does not apply to tenant overlay versions (REQ-STO-006), which are the tenant's own.
  4) **Principal set.** P = {principal} ∪ its groups (MEMBER_OF, transitive, valid at `now`) ∪ its roles (HAS_ROLE of any member of P, valid at `now`, whose metadata `scope`, if any, matches the resource).
  5) **RBAC grant.** Some role in P grants a pattern matching `permission` (or a GRANTS_ACCESS_TO relationship names the operation).
  6) **Statements.** Collect the enabled statements of all Policies whose `principals` intersect P or equal `*`, plus the Context's policies (REQ-CTX-004). Keep those whose `permissions` and `resources` match and whose `conditions` hold. Order them by policy priority (desc).
  7) **Combine:**
     - any matching DENY → DENY (veto);
     - else, if the RBAC grant holds or there is a matching ALLOW, and a matching REQUIRE_APPROVAL applies to a write → ALLOW_WITH_APPROVAL (with the statement's ApprovalSpec);
     - else, if the RBAC grant holds or there is a matching ALLOW → ALLOW;
     - else → DENY (default).
  8) **API keys.** If the principal came from an API key, the permission must also match the key's `scopes` and `resources`. Otherwise: DENY.
  9) **Delegation.** If `on_behalf_of` is set through DELEGATES_TO, the permission must also be allowed for the delegator and be inside the delegation's permissions.

  Every Policy, Role and grant Relationship version that decided the outcome MUST be recorded as DEPENDS_ON_POLICY (REQ-TX-020). The decision carries `source = policy URI#sid` or `role URI`.
- **Rationale:** FU's role-then-policy evaluation combined with LC's relationship grants. Explicit DENY beats ALLOW, which beats the default DENY. Nothing fails open (the UC fail-open mode is not carried over).
- **Origin:** VRD-10-02, CON-FU-022, CON-LC-017, CON-UC-028
- **Acceptance:**
  1) An editor role allows update, but a DENY statement for branch `main` in context `prod` wins.
  2) A HAS_ROLE whose validity ended yesterday grants nothing.
  3) An API key with scope `entity.read` cannot update.
- **Priority:** P0

### REQ-SEC-012 — Conditions
- **Statement:** Statement conditions MUST be evaluated against these resource attributes:
  - tenant, type chain, branch, `_state`, tags, owner, visibility;
  - the highest sensitivity among touched properties (writes);
  - context kind, time and time zone.

  `expression` runs in the RULE profile (ADR-002), with `principal`, `resource` (effective data, loaded lazily) and `env`. `logic` runs a READ Logic returning bool, which is the only scripted form (UC policy-as-code as an extension point). An error in a condition makes the statement **not match** for ALLOW and REQUIRE_APPROVAL, and **match** for DENY. Errors never widen access.
- **Origin:** VRD-10-02, CON-UC-028, CON-FU-022 (branches, entityTypes, timeWindow)
- **Acceptance:** An ALLOW limited to `owner_is_principal` lets users edit only their own records. A DENY whose expression errors still denies.
- **Priority:** P0 (declarative conditions), P1 (expression, logic)

### REQ-SEC-013 — Compilation for queries
- **Statement:** The PDP MUST offer `read_filter(principal, types, env)`. It returns a FilterExpr over indexed paths and registry fields that is equivalent to the read decision for as many conditions as possible. Conditions that cannot be expressed as a filter become a post-filter predicate (REQ-QRY-007). Results MUST equal per-entity `authorize` calls.
- **Origin:** REQ-QRY-007, NEW
- **Acceptance:** A property-based test with random policies and data shows that query results equal the entities that pass individual `authorize` calls.
- **Priority:** P0

### 3.3 Enforcement points

### REQ-SEC-020 — Where the PDP is called
- **Statement:** The kernel MUST call the PDP at each of these points. None of the checks is left to scripts or clients.

| Point | Permission |
|---|---|
| Protocol: before running a named operation (Emit/Query named) | `action.*`, `pipeline.*`, `query.*`, `operation.execute` |
| URI resolution for reads (REQ-URI-009 step 7), including syscalls, expansion and lineage nodes | `entity.read` |
| Staging (REQ-TX-006/010 step 6), per change set | `entity.<action>`; `definition.author` or `logic.author` for definition types; `security.admin` for governance types |
| Query compilation and post-filter | `entity.read` + REQ-SEC-013 |
| Versioning operations | `branch.*`, `version.revert`, `ubos.admin.reset` |
| Syscalls with external effect | `ai.use`, `http.egress`, `secret.read`, `logic.execute` |
| File content reads | `entity.read` on the File entity |
| Render payloads (SPEC-25) | `entity.read` + field access; actions omitted if not allowed |
| Subscriptions (SPEC-20/24) | `entity.read` at subscription time and per delivered event |
- **Origin:** VRD-10-03, CON-UC-004, CON-UC-028
- **Acceptance:** A static check in CI lists all service entry points and fails if one lacks a PDP call or an explicit `// no-auth: reason` annotation.
- **Priority:** P0

### REQ-SEC-021 — Field-level protection
- **Statement:** After an entity is authorised for read, the kernel MUST apply property protection:
  1) **`access` slots** (CHAIN): properties whose read is denied are removed and listed in `_meta.hidden`.
  2) **Sensitivity** (`NONE|PERSONAL|SECRET`, from the property or its value type): values are masked by the `mask` slot unless the reader holds `entity.unmask.<SENSITIVITY>` on the resource. SECRET properties are always removed, unless their type declares an explicit readable mask.
  3) **Writes:** staging a change to a property whose `access` write is denied, or which is `read_only`, fails with `SEC.FIELD_FORBIDDEN` and its path.

  Masking and removal also apply to traces, audit inputs and AI prompts (SPEC-27).
- **Origin:** VRD-10-03, CON-UB-057, CON-LS-013 (PermissionSlot, FieldMaskSlot)
- **Acceptance:**
  1) A support agent sees `credit_card: "**** **** **** 4242"`.
  2) A finance role with `entity.unmask.PERSONAL` sees the full value.
  3) The trace contains the masked form.
- **Priority:** P0

### REQ-SEC-022 — Custody and visibility
- **Statement:** On CREATE, `owner_id` MUST be set to the creating principal, or to the `on_behalf_of` principal when present. Custody is changed only by the admin operation `entity.transfer_owner` (a registry change with an ADMIN process, REQ-STO-002). `visibility` affects the default read rules:
  - `PRIVATE`: only the owner and principals with an explicit ALLOW on the resource;
  - `INTERNAL`: tenant principals, subject to policies;
  - `PUBLIC`: also `anonymous` and other tenants, if the tenant enables public access (SPEC-23).

  Policies may use `owner_is_principal` and `visibility` conditions.
- **Origin:** VRD-10-07, CON-UB-044, CON-UB-012
- **Acceptance:** A PRIVATE note is not returned to a colleague who holds `entity.read` through a role, unless a statement names the note.
- **Priority:** P1

### 3.4 Approvals

### REQ-SEC-040 — Creating approval requests
- **Statement:** When the PDP returns ALLOW_WITH_APPROVAL for a write request (Mutate, action EXECUTE, merge, draft publish), the kernel MUST NOT run the request. Instead it MUST:
  1) run the request as a dry run (REQ-TX-025) to compute the `diff` and validate it, and return any validation failure immediately;
  2) create an `ApprovalRequest` with the original message, base commits, spec and diff, in a process of kind EMIT;
  3) notify the approvers (emit NOTIFY);
  4) respond with `Result{approval: <URI>, status: PENDING}`.

  Idempotency keys apply to the creation.
- **Origin:** VRD-10-04, CON-FU-021, VRD-08-03
- **Acceptance:** A Mutate on a prod price list returns a PENDING approval with a diff, and the price list is unchanged.
- **Priority:** P1

### REQ-SEC-041 — Deciding and executing
- **Statement:** The approval decisions MUST work as follows:
  - **Approve:** `approval.approve@v1 {request, comment}` is allowed for principals matching the spec, holding `approval.decide`, and not the requester (when `separation_of_duties`). The vote is recorded. When the required count is reached, the kernel executes the stored request in a **new process**:
    - principal = the last approver, on_behalf_of = the requester, `parent_process_id` = the approval process;
    - the stored base commits are used as expected heads;
    - validation runs again at the time of execution.
    - On success: status APPROVED, `result_process` set. On failure (e.g. `VER.HEAD_CONFLICT`): status FAILED with the error, and the requester may re-request.
  - **Reject:** any eligible approver may reject (status REJECTED, with a reason).
  - **Cancel:** only the requester may cancel.
  - **Expiry:** the scheduler expires requests after `expires_at`.
- **Rationale:** FU's deferred commit, extended with n-of-m, separation of duties and a safe re-check.
- **Origin:** VRD-10-04, CON-FU-021, VRD-04-05
- **Acceptance:**
  1) A 2-of-3 approval executes after the second approval, and the commit's author is the second approver with on_behalf_of = the requester.
  2) A self-approval is refused.
- **Priority:** P1

### 3.5 Governance of definitions and logic

### REQ-SEC-050 — Definition authorship
- **Statement:** Commits of definition entities MUST require `definition.author` (or `logic.author` for Logic, `security.admin` for governance entities) on the target resource. Definition entities are Type, TypeExtension, Lifecycle, Action, Pipeline, Workflow, Query, Constraint, View, Widget, Package, Behavior, Dictionary, RelationshipType, Logic, Policy and Role. Tenants MAY attach REQUIRE_APPROVAL statements to definition permissions on `main` (VRD-10-06).
- **Origin:** VRD-10-06, VRD-05-03
- **Acceptance:** A business user without `definition.author` cannot change a Type, even through a generic Mutate.
- **Priority:** P0

### REQ-SEC-051 — AI-authored definitions
- **Statement:** Definition and Logic commits whose principal is of kind AGENT, or whose process was started by the AI builder (SPEC-27), MUST go to a DRAFT branch. Publishing them to any non-DRAFT branch MUST require an approval by a human principal with `definition.author`. This rule cannot be disabled by tenant policy.
- **Origin:** VRD-16-01, VRD-10-06, CON-UP-061
- **Acceptance:** An agent committing a Logic on main is refused with `SEC.AI_REQUIRES_REVIEW`. Its draft can be published only through an approval by a human.
- **Priority:** P1

### 3.6 Secrets and keys

### REQ-SEC-060 — Encryption of secrets
- **Statement:** Each tenant MUST have data encryption keys (DEK, AES-256), stored encrypted under a key-encryption key (KEK). The KEK comes from host configuration (`UBOS_KEK` or a KMS URI) and never from the database. `Secret.ciphertext` is AES-256-GCM, with the tenant, the entity ID and `key_version` as associated data. DEK rotation re-encrypts secrets in an ADMIN process, and old versions stay readable only through their key version.
- **Origin:** NEW: required by VRD-10-01 (secrets) and REQ-CTX-003
- **Acceptance:** A database dump alone does not reveal secrets. After rotation, `secret.get` returns the same plaintext.
- **Priority:** P1

### REQ-SEC-061 — Key rotation and revocation
- **Statement:**
  - API keys MUST be revocable immediately (commit `active=false`). Validation caches expire within 60 s, and the revocation event also evicts them directly.
  - Keys expire at `expires_at`.
  - `security.apikey_rotate@v1` creates a new key and schedules the old key's expiry (grace default 24 h).
  - Last-use times are kept in the runtime store only (no version per use).
- **Origin:** VRD-10-05, CON-FU-023
- **Acceptance:** A revoked key is rejected within 60 s, or at once on the same node.
- **Priority:** P1

### REQ-SEC-062 — Rate limits
- **Statement:** The gateway MUST enforce per-principal and per-tenant rate limits (requests per window, concurrent processes), configured per tenant and by service account kind. Exceeding a limit returns `SEC.RATE_LIMITED` (category LIMIT) with `retry_after_ms`.
- **Origin:** CON-LC-046 (rate limiting), NEW
- **Acceptance:** A burst above the limit gets 429 on HTTP, or an equivalent error frame on TCP/WS.
- **Priority:** P1

### 3.7 Console and operator access

### REQ-SEC-070 — Operator surfaces
- **Statement:** Administrative bindings MUST be restricted to principals with `tenant.admin` or `ubos.root_admin`. These include raw eval, reset, index rebuild, partition archive and key rotation. Hosts MAY additionally bind the admin HTTP routes to a separate listen address (default `127.0.0.1`), as FU restricted its console to localhost.
- **Origin:** CON-FU-003 (LocalhostSecurityFilter), VRD-15-05
- **Acceptance:** With the default configuration, `/admin/*` routes are not reachable on the public listener.
- **Priority:** P1
