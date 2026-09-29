---
id: SPEC-22
title: "Security and Governance"
status: complete
phase: P4
depends_on: [SPEC-12, SPEC-13, SPEC-15, SPEC-16, SPEC-17, SPEC-18]
sources: [UP, FU, LC, UB, UC, US, LS]
---

# Security and Governance

## 0. Chapter header

- **Scope:** This chapter covers the following:
  - The governance entities: User, ServiceAccount, Group, Role, Policy, ApiKey, Secret and ApprovalRequest.
  - Authentication: passwords, OIDC, API keys, tokens and protocol sessions.
  - Permission naming and the authorisation algorithm (the policy decision point).
  - The enforcement points and field-level protection (access, masking and sensitivity).
  - Custody and visibility.
  - Approvals as deferred commits.
  - Governance of definitions and logic authorship.
  - Secrets, bootstrap of the first administrator, and rate limits.

  Per the charter, the chapter specifies security *features*. It does not review implementations for vulnerabilities.
- **MOD:** `SEC`
- **depends_on:** SPEC-12, SPEC-13, SPEC-15, SPEC-16, SPEC-17, SPEC-18.
- **Terms used:** TERM-Principal, TERM-Role, TERM-Policy, TERM-ApiKey, TERM-ApprovalRequest, TERM-Custody, TERM-Masking, TERM-Relationship, TERM-SystemIdentity, TERM-Decision.
- **Origin summary:**
  - Verdicts: VRD-10-01…07, VRD-02-03, VRD-05-03, VRD-16-01.
  - FU: access filters, the governance catalogue, approvals, role + policy authorisation, API keys, webhooks (CON-FU-003, 014, 021…024, 060).
  - LC: access as relationships (CON-LC-017).
  - UC: policy as code and tenant checks in syscalls (CON-UC-028, CON-UC-019).
  - UB: custody and masking by type (CON-UB-044, CON-UB-057).
  - UP: auth context and the identity policy (CON-UP-018, CON-UP-052).
  - LS: the PermissionSlot and FieldMaskSlot (CON-LS-013).

## Parts of this chapter

This chapter is split into files (WRITING-RULES R2.2). Read them in order.

| File | Sections |
|---|---|
| `00-index.md` (this file) | §0, §1, §6, §7, §8 |
| `01-model.md` | Model — §2 |
| `02-behavior.md` | Behavior — §3 |
| `03-interfaces-nfr.md` | Interfaces and non-functional — §4, §5 |

## 1. Concepts

### 1.1 Layers of protection

| Layer | Question | Mechanism | Chapter |
|---|---|---|---|
| Authentication | Who is calling? | credentials → Principal | here |
| Tenant boundary | Which tenant's data may be touched? | read/data tenants, grants | SPEC-16, SPEC-23 |
| Authorisation | May this principal do this permission on this resource? | RBAC through relationships + IAM policies, deny by default | here |
| Field protection | Which properties may be seen or changed? | `access` slots, sensitivity, `mask` slots | here, SPEC-13 |
| Containment | Can logic escape? | sandbox, profiles, capabilities | SPEC-17 |
| Governance | Who may change definitions and logic, and with whose approval? | definition permissions, approvals | here |
| Evidence | What happened? | process log, lineage, policy provenance | SPEC-30 |

### 1.2 Everything is an entity

Users, groups, roles, policies, keys, secrets and approvals are versioned entities. Every change to them is a process with an audit record (FU). Role assignment and group membership are Relationship entities with validity (LC).

## 6. Acceptance

| REQ | Criterion |
|---|---|
| REQ-SEC-001…004 | Login, token, key, OIDC; storage never readable; lockout; bootstrap without fail-open |
| REQ-SEC-010…013 | Catalogue coverage; PDP cases (deny wins, validity, key scopes, delegation); conditions fail-safe; filter equivalence property test |
| REQ-SEC-020…022 | Enforcement-point CI check; masking by sensitivity everywhere; custody and PRIVATE visibility |
| REQ-SEC-040, 041 | Approval creation with diff; n-of-m execution with re-validation; separation of duties |
| REQ-SEC-050, 051 | Definition authorship; AI drafts need human approval |
| REQ-SEC-060…062 | Secret encryption and rotation; key revocation latency; rate limits |
| REQ-SEC-070 | Admin surfaces restricted |

## 7. Implementation notes

- **Reference code:**
  - FU policy service and API keys [FU:backend/src/main/java/org/logrum/ubos/kernel/service/ApiKeyService.java], filters [FU:backend/src/main/java/org/logrum/ubos/web/security/ApiKeySecurityFilter.java], webhooks [FU:backend/src/main/java/org/logrum/ubos/kernel/service/WebhookService.java].
  - LC authorisation handler [LC:src/main/java/com/logicorum/handler/AuthorizationHandler.java] and role relationship types [LC:data/relationships/role.rtp.json].
  - UC policy [UC:src/kernel/security.rs#L5-L33].
- **Divergences resolved:**
  - FU's "no matching policy → allow (role decides)" becomes "role grant or explicit ALLOW" under default deny.
  - UC's fail-open development mode is dropped.
  - UP's "tenant = branch" identity policy is replaced by tenants plus Context selection.
- **Crates:** `argon2`, `jsonwebtoken` (EdDSA), `openidconnect`, `aes-gcm`, `ring` for HMAC, `globset` for URI globs (compile once per policy version).
- **Caching:** principal sets are cached by (principal, relationship heads fingerprint), and policies by (tenant, policy heads fingerprint). Both are invalidated by `entity.committed` events of governance types (SPEC-20).

## 8. Open questions

None. Deferred to P2: MFA (TOTP/WebAuthn) and attribute-based delegation chains longer than one hop.
