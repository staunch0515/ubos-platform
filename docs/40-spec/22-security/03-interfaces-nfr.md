---
id: SPEC-22-3
title: "Security and Governance — Interfaces and non-functional"
status: complete
phase: P4
depends_on: [SPEC-22, SPEC-12, SPEC-13, SPEC-15, SPEC-16, SPEC-17, SPEC-18]
sources: [UP, FU, LC, UB, UC, US, LS]
---

# Security and Governance — Interfaces and non-functional

Part 3 of SPEC-22 (`22-security/`). Header, concepts and file list: `00-index.md`.

---

## 4. Interfaces

### API-SEC-001 — PolicyDecisionPoint
- **Kind:** rust trait (`ubos_kernel::security`)
- **Signature / shape:**
```rust
#[async_trait]
pub trait PolicyDecisionPoint: Send + Sync {
    async fn authorize(&self, x: &ExecutionContext, permission: &str, resource: &ResourceRef) -> Result<AuthzDecision, UbosError>;
    async fn authorize_many(&self, x: &ExecutionContext, permission: &str, resources: &[ResourceRef]) -> Result<Vec<AuthzDecision>, UbosError>;
    async fn read_filter(&self, x: &ExecutionContext, types: &[TypeRef]) -> Result<(FilterExpr, Option<PostFilter>), UbosError>;
    async fn field_access(&self, x: &ExecutionContext, entity: &EffectiveEntity) -> Result<FieldAccess, UbosError>;
    async fn explain(&self, x: &ExecutionContext, permission: &str, resource: &ResourceRef) -> Result<Explanation, UbosError>;
}
pub enum AuthzOutcome { Allow, AllowWithApproval(ApprovalSpec), Deny }
pub struct AuthzDecision { pub outcome: AuthzOutcome, pub decision: Decision, pub provenance: Vec<ProvenanceRecord> }
```
- **Origin:** CON-FU-022 (isPermitted), CON-LS-027 (PDP design)

### API-SEC-002 — Authentication endpoints (HTTP binding)
- **Kind:** http
- **Signature / shape:**

| Route | Body | Result |
|---|---|---|
| `POST /auth/login` | `{tenant, username, password}` | `{access_token, refresh_token, expires_in, principal}` |
| `POST /auth/refresh` | `{refresh_token}` | new pair |
| `POST /auth/logout` | – | revokes the refresh token and session |
| `GET /auth/oidc/{provider}` → callback | OIDC | tokens |
| `POST /auth/setup` | `{token, password}` | first admin password (REQ-SEC-004) |

  On TCP and WebSocket, the same credentials go in the UBTP Handshake `auth` field (SPEC-24).
- **Origin:** REQ-SEC-001

### API-SEC-003 — Named operations
- **Kind:** UBTP targets
- **Signature / shape:**

| URI | Args | Result |
|---|---|---|
| `ubos://system/Action/security.apikey_create@v1` | `{principal, scopes, resources?, expires_at}` | `{key (plaintext once), uri}` |
| `ubos://system/Action/security.apikey_rotate@v1` | `{key_uri, grace?}` | new key |
| `ubos://system/Action/security.grant_role@v1` | `{principal, role, valid_from?, valid_to?, scope?}` | Relationship URI |
| `ubos://system/Action/security.revoke_role@v1` | `{relationship, at?}` | – |
| `ubos://system/Action/entity.transfer_owner@v1` | `{uri, owner}` | – |
| `ubos://system/Action/approval.approve@v1` / `approval.reject@v1` / `approval.cancel@v1` | `{request, comment?}` | status |
| `ubos://system/Query/security.explain@v1` | `{principal?, permission, resource}` | decision trace: statements, roles, conditions, outcome |
| `ubos://system/Query/security.effective_permissions@v1` | `{principal?, resource?}` | permissions with sources |
- **Origin:** CON-FU-054 (governance managers), VRD-16-06 (explanations)

### Error codes (SEC)

| Code | Category | Meaning |
|---|---|---|
| `SEC.UNAUTHENTICATED` | UNAUTHENTICATED | missing, invalid or expired credentials |
| `SEC.PRINCIPAL_INACTIVE` | UNAUTHENTICATED | principal disabled or locked |
| `SEC.FORBIDDEN` | FORBIDDEN | denied operation on a visible resource |
| `SEC.FIELD_FORBIDDEN` | FORBIDDEN | write to a protected property (path) |
| `SEC.APPROVAL_REQUIRED` | (result) | request converted to an approval (status PENDING) |
| `SEC.APPROVAL_NOT_ELIGIBLE` | FORBIDDEN | principal may not decide this approval |
| `SEC.AI_REQUIRES_REVIEW` | FORBIDDEN | agent definition commit outside drafts |
| `SEC.RATE_LIMITED` | LIMIT | rate limit exceeded |
| `SEC.GROUP_CYCLE` | INVALID | cyclic group membership |
| `SEC.SETUP_TOKEN_INVALID` | UNAUTHENTICATED | bootstrap token wrong or used |

---

## 5. Non-functional

| ID | Requirement | Target |
|---|---|---|
| NFR-PERF-110 | `authorize` with warm caches (principal set, policies) | p95 ≤ 100 µs |
| NFR-PERF-111 | Principal-set cache invalidation after a role change | ≤ 5 s cluster-wide (via events) |
| NFR-SEC-110 | Default deny | 100 % of unknown permissions and resources denied |
| NFR-SEC-111 | Secret material in logs, traces, audit | none (automated scan in CI scenarios) |
| NFR-PRIV-110 | Masking consistent across bindings, queries, traces and AI prompts | the same mask output everywhere |
