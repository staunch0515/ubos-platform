---
id: SPEC-22-1
title: "Security and Governance — Model"
status: complete
phase: P4
depends_on: [SPEC-22, SPEC-12, SPEC-13, SPEC-15, SPEC-16, SPEC-17, SPEC-18]
sources: [UP, FU, LC, UB, UC, US, LS]
---

# Security and Governance — Model

Part 1 of SPEC-22 (`22-security/`). Header, concepts and file list: `00-index.md`.

---

## 2. Model

```yaml
ENT-User:
  purpose: Human principal of a tenant (TERM-Principal kind USER).
  origin: [VRD-10-01, CON-FU-014, CON-UC-018 (user_login)]
  fields:
    - {name: username, type: slug, required: true, description: "Equals the entity slug; unique per tenant"}
    - {name: title, type: string, required: true, description: "Display name"}
    - {name: emails, type: "list<Email>", required: false, description: "Emails; first is primary (searchable, unique TENANT)"}
    - {name: status, type: "enum{ACTIVE|DISABLED|LOCKED|INVITED}", required: true, description: "LOCKED after failed logins; DISABLED by admins"}
    - {name: password, type: PasswordHash?, required: false, description: "argon2id hash produced by the transform slot; never readable (sensitivity SECRET, read access false)"}
    - {name: external_ids, type: "list<{provider: uri<IdentityProvider>, subject: string}>?", required: false, description: "OIDC links (searchable)"}
    - {name: preferences, type: "{locale?, timezone?, theme?}?", required: false, description: "REQ-CTX-014"}
    - {name: person, type: "uri<Person>?", required: false, description: "Link to the ontology Person (SPEC-29)"}
  invariants:
    - password is write-only: the access slot denies read to everyone; the value is only compared by the authentication service.
```

```yaml
ENT-ServiceAccount:
  purpose: Non-human principal (integrations, system identity, anonymous).
  origin: [VRD-11-05, CON-LC-027, CON-UP-030]
  fields:
    - {name: title, type: string, required: true, description: "Name"}
    - {name: kind, type: "enum{SYSTEM|INTEGRATION|ANONYMOUS|KERNEL}", required: true, description: "SYSTEM = tenant system identity (REQ-CTX-007); KERNEL = logrums kernel; ANONYMOUS = unauthenticated public access (disabled by default)"}
    - {name: status, type: "enum{ACTIVE|DISABLED}", required: true, description: "Status"}
    - {name: owner, type: "uri<User>?", required: false, description: "Responsible human"}
```

```yaml
ENT-Group:
  purpose: Set of principals; membership via MEMBER_OF relationships (principal -> group, group -> group).
  origin: [VRD-10-01, CON-FU-014]
  fields:
    - {name: title, type: string, required: true, description: "Name"}
    - {name: description, type: text?, required: false, description: "Purpose"}
  invariants:
    - Nested membership depth ≤ 8; cycles rejected (SEC.GROUP_CYCLE).
```

```yaml
ENT-Role:
  purpose: Named set of permission patterns (TERM-Role); assigned via HAS_ROLE relationships with validity.
  origin: [VRD-10-02, CON-FU-022, CON-LC-017]
  fields:
    - {name: title, type: string, required: true, description: "Name"}
    - {name: permissions, type: "list<string>", required: true, description: "Permission patterns (REQ-SEC-010), e.g. entity.read, action.acme.invoice.*"}
    - {name: description, type: text?, required: false, description: "Purpose"}
  invariants:
    - _extends to another Role inherits its permissions (additive).
```

```yaml
ENT-Policy:
  purpose: IAM-style policy (TERM-Policy).
  origin: [VRD-10-02, CON-FU-022, CON-UC-028, CON-UP-052]
  fields:
    - {name: title, type: string, required: true, description: "Name"}
    - {name: enabled, type: bool, required: true, description: "Disabled policies are ignored"}
    - {name: priority, type: int32, required: true, description: "Evaluation order (desc); ties by slug"}
    - {name: statements, type: "list<PolicyStatement>", required: true, description: "§2.1"}
```

### 2.1 Policy statement

```yaml
ENT-PolicyStatement:
  purpose: One rule of a policy.
  origin: [VRD-10-02, VRD-10-04, CON-FU-022]
  fields:
    - {name: sid, type: string(64), required: true, description: "Statement id (for overrides and explanations)"}
    - {name: effect, type: "enum{ALLOW|DENY|REQUIRE_APPROVAL}", required: true, description: "REQUIRE_APPROVAL turns an allowed write into an ApprovalRequest (REQ-SEC-040)"}
    - {name: principals, type: "list<string>", required: true, description: "Principal/Group/Role URIs or '*' (all authenticated of the tenant) or 'anonymous'"}
    - {name: permissions, type: "list<string>", required: true, description: "Permission patterns ('*' allowed as segment wildcard)"}
    - {name: resources, type: "list<string>", required: true, description: "ubos:// URI globs: '*' within a segment, '**' across segments, e.g. ubos://acme/acme.Invoice/**"}
    - name: conditions
      type: PolicyConditions?
      required: false
      description: "{branches?: [glob], types?: [uri] (subtypes included), states?: [string], tags_any?: [string], owner_is_principal?: bool, visibility?: [enum], sensitivity_max?: enum, time_window?: {days, from, to, tz}, context_kinds?: [enum], expression?: rhai (rule subset over principal, resource, env), logic?: uri<Logic> (effect READ, returns bool)}"
    - {name: approval, type: ApprovalSpec?, required: false, description: "REQUIRE_APPROVAL only - approver principals/roles, count (n of m), expires_after, separation_of_duties (default true)"}
```

```yaml
ENT-ApiKey:
  purpose: Scoped, expiring machine credential (TERM-ApiKey).
  origin: [VRD-10-05, CON-FU-023, CON-FU-003]
  fields:
    - {name: prefix, type: string(12), required: true, description: "First 8 chars after 'ubos_' for identification (searchable)"}
    - {name: hash, type: bytes, required: true, description: "SHA-256 of the full key; never readable"}
    - {name: principal, type: "uri", required: true, description: "User or ServiceAccount the key acts as"}
    - {name: scopes, type: "list<string>", required: true, description: "Permission patterns; effective permissions = principal's ∩ scopes"}
    - {name: resources, type: "list<string>?", required: false, description: "Optional URI globs restricting the key further"}
    - {name: expires_at, type: timestamp, required: true, description: "Max 1 year (tenant-configurable)"}
    - {name: active, type: bool, required: true, description: "Revocation = commit active=false"}
    - {name: allowed_ips, type: "list<string>?", required: false, description: "CIDRs"}
```

```yaml
ENT-Secret:
  purpose: Encrypted secret value referenced by Context env vars and logic (secret.get).
  origin: [VRD-10-01, REQ-CTX-003, CON-FU-061]
  fields:
    - {name: ciphertext, type: bytes, required: true, description: "AES-256-GCM under the tenant data key (REQ-SEC-060)"}
    - {name: key_version, type: int32, required: true, description: "Data key version"}
    - {name: description, type: text?, required: false, description: "What it is for"}
    - {name: rotated_at, type: timestamp?, required: false, description: "Last rotation"}
  invariants:
    - ciphertext is never returned by reads (access slot read=false); plaintext only via secret.get with permission secret.read on the Secret.
```

```yaml
ENT-IdentityProvider:
  purpose: External OIDC identity provider configuration.
  origin: [NEW: enterprise SSO]
  fields:
    - {name: issuer, type: string, required: true, description: "OIDC issuer URL"}
    - {name: client_id, type: string, required: true, description: "Client id"}
    - {name: client_secret, type: "uri<Secret>", required: true, description: "Secret reference"}
    - {name: claim_mapping, type: json, required: true, description: "username/email/groups claims; group claim -> Group URI map"}
    - {name: auto_provision, type: bool, required: true, description: "Create Users on first login"}
```

```yaml
ENT-ApprovalRequest:
  purpose: A deferred commit or deferred action awaiting approval (TERM-ApprovalRequest).
  origin: [VRD-10-04, CON-FU-021]
  fields:
    - {name: kind, type: "enum{MUTATE|EMIT|MERGE|PUBLISH}", required: true, description: "What will run"}
    - {name: request, type: json, required: true, description: "The original UBTP message body (changes / action call / merge request), with base commits"}
    - {name: requested_by, type: uri, required: true, description: "Requesting principal"}
    - {name: context, type: uri, required: true, description: "Context of the request"}
    - {name: approval, type: ApprovalSpec, required: true, description: "Copied from the policy statement (with statement sid)"}
    - {name: status, type: "enum{PENDING|APPROVED|REJECTED|CANCELLED|EXPIRED|FAILED}", required: true, description: "State"}
    - {name: decisions, type: "list<{principal, decision: APPROVE|REJECT, comment?, at}>", required: true, description: "Recorded votes"}
    - {name: diff, type: "list<json>?", required: false, description: "Dry-run diff shown to approvers (REQ-TX-025)"}
    - {name: expires_at, type: timestamp, required: true, description: "Default now + 7 days"}
    - {name: result_process, type: uuid?, required: false, description: "Process that executed the request"}
    - {name: failure, type: error?, required: false, description: "Why execution failed (e.g. VER.HEAD_CONFLICT)"}
```

### 2.2 Platform relationship types

| Code | Source → Target | Validity | Metadata | Meaning |
|---|---|---|---|---|
| `HAS_ROLE` | User/ServiceAccount/Group/Agent → Role | optional | `{scope?: [URI glob], tenant?: code}` | role assignment, optionally scoped to resources |
| `MEMBER_OF` | User/ServiceAccount/Agent/Group → Group | optional | – | group membership |
| `GRANTS_ACCESS_TO` | Role → Action/Pipeline/Query | – | – | explicit operation grant (LC), shorthand for `action.<slug>` etc. |
| `DELEGATES_TO` | User → User/Agent | required | `{permissions: [pattern]}` | time-bounded delegation (on_behalf_of) |
| `TENANT_GRANT` | Tenant → Tenant | optional | `{permissions, resources}` | cross-tenant access (SPEC-23) |
