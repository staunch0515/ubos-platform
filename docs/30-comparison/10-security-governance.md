---
id: CMP-10
title: "Comparison: Security and Governance"
status: complete
phase: P2
depends_on: [CMP-02, CMP-04, CMP-07, CMP-09]
sources: [UP, FU, LC, UB, UC, US, LS, UW]
---

# Comparison: Security and Governance

## 1. Question

How are identities authenticated and actions authorised? How are integrations credentialed, changes approved, and runtime-authored logic contained — with governance stored as versioned data?

## 2. Candidates

| KEY | Approach | Refs | Summary |
|---|---|---|---|
| UP | Policy entity binds user → branch; script sandbox; default SYSTEM identity | CON-UP-018, 022, 052, HL-UP-006, 008 | Identity decides the world seen |
| FU | Governance as versioned entities: USER, GROUP, POLICY (IAM-style URI globs, priority, explicit DENY), SECURITY_KEY (hashed, scoped, expiring), WEBHOOK (HMAC), APPROVAL_REQUEST (deferred commit); localhost console; API-key scopes | CON-FU-001, 003, 014, 021, 022, 023, 024, HL-FU-001, 003, 004, 011 | The most complete governance |
| LC | Roles as entities; HAS_ROLE / GRANTS_ACCESS_TO relationships with validity; `requiredRoles` per process | CON-LC-017, HL-LC-013 | Access in the business graph |
| UB | Owner vs author; visibility; security types (PKI, users) | CON-UB-044, 014 | Custody model |
| UC | Tenant checks in syscalls; policy-as-code script (fail-open); constitution functions; Ambassador host/sovereign | CON-UC-019, 028, 024, HL-UC-011 | Programmable policy |
| US | Same as UC; `required_permission` on actions | CON-US-017 | – |
| LS | PermissionSlot; policy/prerequisite mechanisms (design); single tenant | CON-LS-013, 027 | Permission as slot |
| UW | Approvals in dock | CON-UW-013 | UX |

## 3. Dimension matrix

| Dimension | UP | FU | LC | UB | UC | LS |
|---|---|---|---|---|---|---|
| Governance stored as versioned entities | ● | ● | ● | ◐ | ◐ | ◐ |
| RBAC | role | ● | ● | – | script | slot |
| Resource-level policies (ABAC) | – | ● URI globs, DENY | – | – | ● script | design |
| Temporal validity of grants | – | expiry (keys) | ● | – | – | – |
| API keys / machine identity | – | ● | – | – | – | – |
| Approval workflow | – | ● | – | publish | – | – |
| Sandbox for runtime logic | ● | ● | n/a | – | op limit | – |
| Default deny | – | DENY wins | ● | – | fail-open | – |
| Field-level protection (masking) | – | – | – | ● types | types | ● FieldMaskSlot |

## 4. Analysis

- **FU is the reference.** Every governance artefact is an entity and is audited through the version chain; policies are IAM-style over `ubos://` resources with explicit DENY (HL-FU-004); approvals are deferred commits (HL-FU-003).
- **LC puts grants in the relationship graph** with validity periods (HL-LC-013). This fits VRD-02-03 and allows time-bounded roles.
- **UC's policy-as-code** is flexible but fails open when no policy exists, and it is not wired to syscalls.
  - *Interpretation:* keep scripted policies only as an extension point under a declarative, deny-by-default base.
- **Field-level protection.** Masking appears as semantic-type attributes (UB/UC) and as FieldMaskSlot (LS). Read paths must apply it according to permission.
- **Logic containment.** The sandbox protects against resource abuse (UP). Syscall-level policy protects against data abuse; UC's tenant prefix checks are the first form of that.

## 5. Verdicts

### VRD-10-01 — Governance entities catalogue (best-of FU, extended)
- **Decision:** best-of FU, extended.
- **Consequences:** Security entities are `User`, `Group`, `Role`, `Policy`, `ApiKey`, `Webhook` and `ApprovalRequest`, plus `Session` if needed. All are versioned and audited. Secrets are stored hashed; plaintext is shown once.

### VRD-10-02 — Authorization = RBAC via relationships + IAM-style policies, deny by default (fusion LC + FU)
- **Decision:** fusion.
- **Consequences:**
  - Roles are assigned via `HAS_ROLE` relationships with validity.
  - Roles grant actions or pipelines via `GRANTS_ACCESS_TO` relationships.
  - Policies are `{effect: ALLOW | DENY, actions, resources: ubos:// globs, conditions (branch, type, tenant, attributes), priority}`.
  - Evaluation: explicit DENY > ALLOW > default DENY.
  - Decisions feed the rule decision model (VRD-09-05).
  - Scripted policies (UC) are allowed only as `condition` logic.

### VRD-10-03 — Enforcement at the syscall and read boundary (fusion UC + NEW)
- **Decision:** fusion of UC with NEW enforcement points.
- **Consequences:**
  - Every syscall (read, stage, invoke, file, AI) and every API read passes the policy decision point with the execution identity.
  - Masking slots are applied on read unless the caller holds an unmask permission.
  - Tenant isolation is enforced here, not in scripts.

### VRD-10-04 — Approvals are deferred commits (best-of FU)
- **Decision:** best-of FU.
- **Consequences:**
  - An `ApprovalRequest` holds the target URI or action, the payload and the required approver policy.
  - Approval executes the change as a process under the approver's identity and links it to the request (VRD-04-05).
  - Transitions may require approval as a guard (VRD-09-06).

### VRD-10-05 — Machine access: scoped, expiring API keys; HMAC-signed outbound webhooks (best-of FU)
- **Decision:** best-of FU.

### VRD-10-06 — Contained runtime logic (fusion UP + UC → VRD-05-03)
- **Decision:** fusion; defined in VRD-05-03.
- **Consequences:** Logic authorship is itself governed: committing Logic requires a permission, and production contexts may require approval of logic commits (AI-authored logic in particular, CMP-16).

### VRD-10-07 — Custody fields (best-of UB)
- **Decision:** best-of UB.
- **Consequences:** Every instance has an `owner` (transferable custody) separate from the commit `author` and a `visibility`. Policies can reference them.
