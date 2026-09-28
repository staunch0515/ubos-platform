---
id: SPEC-12
title: "Identity and ubos:// Addressing"
status: complete
phase: P4
depends_on: [SPEC-02, SPEC-10, SPEC-11, ADR-005]
sources: [UP, FU, UB, UC, US, LS]
---

# Identity and `ubos://` Addressing

## 0. Chapter header

- **Scope:** This chapter defines:
  - entity identity (UUID and locator);
  - rename and copy;
  - the `ubos://` URI grammar;
  - authorities, version selectors, key paths, view hints and operation version qualifiers;
  - normalisation of short forms and the resolution algorithm;
  - the URI conformance table that every URI library must pass.
- **MOD:** `URI`
- **depends_on:** SPEC-02, SPEC-10, SPEC-11.
- **Terms used:** TERM-EntityId, TERM-Locator, TERM-Slug, TERM-Namespace, TERM-Uri, TERM-Authority, TERM-VersionSelector, TERM-KeyPath, TERM-ShortForm, TERM-Context, TERM-TypeCode, TERM-NamedOperation, TERM-RootTenant.
- **Origin summary:**
  - Verdicts: VRD-03-01…05.
  - FU: the query grammar with `commit`, `tag`, `key` (CON-FU-010), rename and copy as commits (CON-FU-016, CON-FU-029), namespace navigation (CON-FU-013).
  - UB: the context authority (CON-UB-017), the presentation suffix (CON-UB-018) and system aliases.
  - LS: `time` and `tag` selectors (CON-LS-016).
  - UC: the URI as universal argument (CON-UC-066) and locator reuse (CON-UC-041).
  - UP: dotted slugs (CON-UP-014).

---

## 1. Concepts

### 1.1 Three layers of identity

| Layer | Form | Stable under | Used for |
|---|---|---|---|
| Entity ID | UUID v7 | everything (rename, copy target is a new ID) | storage keys, lineage, rename-proof references |
| Locator | (tenant, type code, slug) | everything except RENAME | business key, human and AI use |
| URI | `ubos://authority/Type/slug?selectors` | – | addressing a version or view of an entity in a context |

An entity is only a concrete fact inside a context: (entity, tenant, branch or commit) (CON-UB-010). A URI names all four:
- the entity, by locator or ID;
- the tenant and branch, through the authority or the `branch` selector;
- the version, through `commit`, `tag` or `time`.

### 1.2 URI anatomy

```
ubos://acme.prod/acme.crm.Lead/lead-0042?time=2026-09-01T00:00:00Z&key=address.city&view=summary
       └──┬───┘ └─────┬──────┘ └───┬───┘ └──────────────── selectors / hints ─────────────────┘
      authority    type code      slug
      (tenant.context)
```

Named operations add a version qualifier: `ubos://system/Action/entity.edit@v1`.

### 1.3 Stored vs. addressing URIs

- **Stored references** (reference properties, `_extends`, Logic imports, Cron/Hook targets) use the **tenant code** as authority and carry no selector unless the property is pinned. The reading context then supplies the branch, which makes stored data portable across environments (CON-UC-066, "remap a namespace to switch environments").
- **Addressing URIs** (protocol targets, UI links, audit) may use a context authority and selectors.

---

## 2. Model

### 2.1 Parsed URI

```yaml
ENT-UbosUri:
  purpose: Parsed form of a ubos:// URI (value type in ubos_proto; not stored as an entity).
  origin: [VRD-03-02, CON-FU-010, CON-LS-016, CON-UB-018]
  fields:
    - name: authority
      type: Authority
      required: true
      description: Tenant code, tenant.context, or the alias system (normalised to logrums).
    - name: type_code
      type: type_code
      required: true
      description: Type segment.
    - name: target
      type: Target
      required: true
      description: Slug(slug) or Id(uuid) (the ~uuid form, REQ-URI-012).
    - name: op_version
      type: int32?
      required: false
      description: Version qualifier @vN for named operations (REQ-URI-010).
    - name: branch
      type: branch_name?
      required: false
      description: Branch selector.
    - name: commit
      type: commit_id?
      required: false
      description: Commit selector (exact version).
    - name: tag
      type: string?
      required: false
      description: Tag selector.
    - name: time
      type: timestamp?
      required: false
      description: Time selector (latest version at or before time).
    - name: key
      type: KeyPath?
      required: false
      description: Path into the effective snapshot.
    - name: view
      type: enum{summary|detail|json|schema|config}?
      required: false
      description: Presentation hint.
  invariants:
    - At most one of commit, tag, time.
    - time and tag require no commit; branch may combine with time only.
```

```yaml
ENT-Authority:
  purpose: First URI segment, before resolution.
  origin: [VRD-03-03, CON-UB-017, CON-UC-015]
  fields:
    - name: tenant
      type: tenant_code
      required: true
      description: Tenant code (system -> logrums).
    - name: context_slug
      type: slug?
      required: false
      description: Slug of a Context entity of that tenant (text after the first dot).
```

### 2.2 Resolved address

```yaml
ENT-ResolvedAddress:
  purpose: Result of resolving a URI in an execution context (REQ-URI-009).
  origin: [VRD-03-03, CON-UB-010]
  fields:
    - name: tenant
      type: tenant_code
      required: true
      description: View tenant.
    - name: branch_chain
      type: list<branch_name>
      required: true
      description: Branch and its fallback parents (SPEC-14), most specific first.
    - name: pinned_commit
      type: commit_id?
      required: false
      description: Commit fixed by a selector or by a pinned Context.
    - name: entity_id
      type: uuid?
      required: false
      description: Entity ID when the target exists (absent for create targets).
    - name: owner_tenant
      type: tenant_code?
      required: false
      description: Tenant owning the entity (logrums for root entities read through a tenant).
    - name: type_code
      type: type_code
      required: true
      description: Registered type code in its stored case.
    - name: slug
      type: slug?
      required: false
      description: Current slug in its stored case.
    - name: commit_id
      type: commit_id?
      required: false
      description: Resolved version (absent if not found or deleted on this chain).
    - name: renamed_from
      type: slug?
      required: false
      description: Set when resolution followed a rename redirect (REQ-URI-013).
    - name: canonical
      type: uri
      required: true
      description: Canonical URI string (REQ-URI-006).
```

### 2.3 Reserved authorities and schemes

| Name | Meaning | Chapter |
|---|---|---|
| `system` | alias of the root tenant `logrums` | SPEC-23 |
| `logrums` | root tenant | SPEC-23 |
| `local` | default tenant of the personal edition | SPEC-31 |
| scheme `client://` | a connected client as a target of pushed UI (`client://current`, `client://session/{id}`) | SPEC-25 |
| scheme `blob://sha256/{hex}` | blob content (read only, needs a File grant) | SPEC-11 |

---

## 3. Behavior

### 3.1 Identity

### REQ-URI-001 — Entity ID assignment
- **Statement:** The session (SPEC-15) MUST assign a new UUID v7 to an entity when the entity is first staged for creation and its locator has no non-DELETED registry row. Clients MAY propose an ID in a Mutate. The kernel accepts it only if it is a valid UUID and unused; otherwise it fails with `URI.ID_TAKEN`. An ID is never reused, including after deletion.
- **Origin:** VRD-03-01, CON-UC-010, REQ-CONV-030
- **Acceptance:**
  1) A client-proposed unused ID is kept.
  2) A reused ID fails with `URI.ID_TAKEN`.
  3) Deleting and re-creating a locator yields a new ID.
- **Priority:** P0

### REQ-URI-002 — Locator uniqueness
- **Statement:** A locator (tenant, type code, slug) MUST identify at most one non-DELETED entity. Comparison is case-insensitive (REQ-CONV-011). Creating an entity with a locator held by another non-DELETED entity MUST fail with `URI.LOCATOR_TAKEN`, unless the session resolves the locator to that existing entity (upsert semantics, SPEC-15).
- **Origin:** VRD-03-01, CON-UB-011, REQ-STO-002
- **Acceptance:** Creating `Invoice/INV-1` when `Invoice/inv-1` exists fails with `URI.LOCATOR_TAKEN`.
- **Priority:** P0

### REQ-URI-003 — Slug and type code syntax
- **Statement:** Slugs MUST satisfy REQ-CONV-014, type codes REQ-CONV-017 and tenant codes REQ-CONV-012. Violations fail with `URI.INVALID_SLUG`, `URI.INVALID_TYPE` or `URI.INVALID_AUTHORITY`. Slugs MUST NOT start with `~` (reserved for the ID form) and MUST NOT contain `@` (reserved for the version qualifier).
- **Origin:** VRD-03-01, VRD-02-02
- **Acceptance:** Conformance table rows C-20…C-27 (§3.7).
- **Priority:** P0

### REQ-URI-004 — Rename
- **Statement:** Renaming an entity MUST be a RENAME commit on the target branch.
  - The snapshot is unchanged.
  - `details = {old_slug, new_slug}`.
  - On `main`, the commit also updates `entity_instance.slug` (REQ-STO-002).
  - The entity ID never changes.
  - A rename on a non-main branch records the intended slug in `details` and takes effect in the registry only when merged into `main`.
  - Renaming to a slug held by another non-DELETED entity fails with `URI.LOCATOR_TAKEN`.
  - Root-package entities can be renamed only by root administrators.
- **Origin:** CON-FU-016, HL-FU-012, VRD-03-01
- **Acceptance:**
  1) After a rename on main, the new URI resolves and the ID is unchanged.
  2) The old URI resolves through the redirect (REQ-URI-013).
- **Priority:** P1

### REQ-URI-005 — Copy
- **Statement:** Copying MUST create a **new** entity (new ID, given target slug, target branch) with a COPY commit.
  - The new entity's own data equals the effective own data of the source URI's resolved version.
  - `details = {source_uri}`, where `source_uri` is the canonical source URI with `commit`.
  - Copy across tenants follows SPEC-23 rules.
- **Origin:** CON-FU-029, VRD-03-01
- **Acceptance:** Copying `…/Invoice/inv-1?commit=41` to `inv-2` creates a new ID whose first commit has action COPY and a `details.source_uri` with `commit=41`.
- **Priority:** P1

### 3.2 Grammar and normalisation

### REQ-URI-006 — Canonical grammar
- **Statement:** URI libraries MUST accept and produce the following grammar (ABNF, RFC 5234; core rules ALPHA, DIGIT from RFC 5234):
```abnf
ubos-uri      = "ubos://" authority "/" type-code "/" target [ op-version ] [ "?" query ]
authority     = tenant-code [ "." context-slug ]
tenant-code   = LALPHA 1*62( LALPHA / DIGIT / "-" )
context-slug  = slug
type-code     = *4( ns-seg "." ) type-name
ns-seg        = LALPHA *30( LALPHA / DIGIT )
type-name     = UALPHA *62( ALPHA / DIGIT )
target        = slug / id-form
id-form       = "~" uuid
slug          = segment *7( "." segment )                  ; max 200 chars
segment       = ALNUM [ *62( ALNUM / "_" / "-" ) ALNUM ]
op-version    = "@v" 1*4DIGIT
query         = param *( "&" param )
param         = "branch=" branch-name / "commit=" 1*19DIGIT / "tag=" slug
              / "time=" date-time / "key=" key-path / "view=" view
branch-name   = bseg *3( "/" bseg )                        ; REQ-CONV-024
bseg          = ( LALPHA / DIGIT ) *62( LALPHA / DIGIT / "_" / "-" )
key-path      = key-seg *( "." key-seg )
key-seg       = key-name *( "[" 1*9DIGIT "]" ) / 1*9DIGIT
key-name      = ( ALPHA / "_" ) *63( ALPHA / DIGIT / "_" )
view          = "summary" / "detail" / "json" / "schema" / "config"
date-time     = <RFC 3339 date-time, UTC "Z" or offset>
uuid          = 8HEXDIG "-" 4HEXDIG "-" 4HEXDIG "-" 4HEXDIG "-" 12HEXDIG
LALPHA        = %x61-7A
UALPHA        = %x41-5A
ALNUM         = ALPHA / DIGIT
```
- **Semantics:**
  - Each parameter may appear at most once (`URI.DUPLICATE_PARAM`).
  - Unknown parameters fail with `URI.UNKNOWN_PARAM`.
  - Percent-encoding is decoded before matching. Characters outside the grammar must be percent-encoded, and after decoding must satisfy the grammar.
- **Origin:** VRD-03-02, CON-FU-010, CON-LS-016
- **Acceptance:** Conformance table (§3.7) rows C-01…C-19 parse to the given structures.
- **Priority:** P0

### REQ-URI-007 — Normalisation
- **Statement:** Every URI that is stored, audited or returned by the kernel MUST be in canonical form:
  1) The scheme, tenant code and context slug are in lower case. `system` becomes `logrums`.
  2) The type code and slug take their **registered** case when the entity or type exists. Otherwise they keep the case given.
  3) A named operation whose slug ends in segment `v<N>` is written `base@v<N>` (REQ-URI-010).
  4) Query parameters appear in the fixed order `branch, commit, tag, time, key, view`, each at most once.
  5) `time` is rewritten to UTC with a `Z` suffix and millisecond precision.
  6) A `branch` equal to the branch implied by the authority is removed.
  7) Percent-encoding is minimal: only characters outside the grammar are encoded, and hex digits are upper case.
  8) The ID form `~uuid` is rewritten to the locator form in audit and responses, unless the caller asked for ID form. Stored references keep the form they were written in.
- **Rationale:** One canonical string per address makes equality, caching and audit reliable (VRD-03-04, VRD-03-05).
- **Origin:** VRD-03-04, VRD-03-05
- **Acceptance:** Conformance rows C-30…C-39 normalise to the given outputs.
- **Priority:** P0

### REQ-URI-008 — Short forms
- **Statement:** In UI input, scripts and protocol messages (never in stored data), the kernel and SDKs MUST accept the short forms below and normalise them against the current execution context before use. Stored data MUST contain only canonical URIs. Writing a short form into a reference property MUST normalise it at staging time.

| Short form | Meaning | Example → canonical (context `acme.prod`, branch `main`) |
|---|---|---|
| `Type/slug` | entity in the current context | `Invoice/inv-1` → `ubos://acme.prod/Invoice/inv-1` |
| `/Type/slug` | same, anchored | as above |
| `self` | the entity being processed | → canonical URI of the current target |
| `@draft` suffix | the current principal's draft of the base branch | `Invoice/inv-1@draft` → `…/Invoice/inv-1?branch=draft/u-17/main` |
| `@<commit>` suffix | exact commit | `Invoice/inv-1@41` → `…/Invoice/inv-1?commit=41` |
| `ubos:///tenant/Type/slug` | LS path form (tolerated) | → `ubos://tenant/Type/slug` |
| `ubos://Type/slug` | FU short form (tolerated only if `Type` is not a tenant code) | → current authority |
| system alias `sys:<name>` | a root entity listed in the alias table of SPEC-28 | `sys:Entity` → `ubos://logrums/Type/Entity` |
| `.summary` / `.json` … suffix (UB) | view hint | `Invoice/inv-1.json` → `…?view=json` only when `inv-1.json` is not an existing slug |

- **Origin:** VRD-03-04, CON-UB-018, CON-FU-011, CON-LS-016
- **Acceptance:** Conformance rows C-40…C-49. A Mutate that stores `Invoice/inv-1` in a reference property persists the canonical form.
- **Priority:** P0 (Type/slug, self, commit suffix), P1 (others)

### 3.3 Resolution

### REQ-URI-009 — Resolution algorithm
- **Statement:** Resolving a URI *u* in execution context *x* MUST produce an `ENT-ResolvedAddress` by these steps:
  1) **Parse and normalise** *u* (REQ-URI-006/007/008).
  2) **Authority.** Take `tenant` and optional `context_slug`.
     - With a context slug, load Context `ubos://<tenant>/Context/<context_slug>` (SPEC-16). It yields a branch or a pinned commit and further settings.
     - Without a context slug, use the tenant's default Context (slug `default`) if it exists; otherwise use branch `main`, unpinned.
     - If the Context does not exist, the result is `URI.CONTEXT_NOT_FOUND`.
     - A tenant that does not exist, or that the principal may not access, is `URI.NOT_FOUND` (REQ-CONV-052).
  3) **Branch.** A `branch` selector overrides the Context's branch. `branch_chain` is the branch plus its fallback parents (SPEC-14).
  4) **Type.** Look up the type code case-insensitively in the tenant resolution order (SPEC-23: tenant, then root). If it is unknown, the result is `URI.TYPE_NOT_FOUND`.
  5) **Target.**
     - A slug target (after applying `op_version`, REQ-URI-010) is looked up in the registry: first the tenant's own locator, then, for root-visible types, the root's locator (SPEC-23).
     - An ID form looks up the registry by ID and checks that the entity's type is the type segment or a subtype of it (`URI.TYPE_MISMATCH`).
     - If there is no match, follow the rename redirect (REQ-URI-013).
  6) **Version.**
     - `commit`: the version must belong to this entity, else `URI.COMMIT_MISMATCH`.
     - `tag`: resolve the Tag (SPEC-14). A COMMIT tag must name a commit of this entity. A POINT tag resolves as `time` on the tag's branch.
     - `time`: the latest version at or before `time` on the first branch of the chain that has a head at that time.
     - Otherwise: the head on the first branch of the chain that has a head (`deleted = true` means not found on this chain).
     - A pinned Context without selectors resolves as of the pin's point (branch + timestamp, SPEC-14 REQ-VER-006, SPEC-16).
  7) **Authorise** the read (SPEC-22). A denial is reported as `URI.NOT_FOUND`.
- **Rationale:** Context indirection makes environments data (UB), while selectors give exact time travel (FU, LS).
- **Origin:** VRD-03-02, VRD-03-03, CON-UB-017, CON-UC-016, CON-LS-016
- **Acceptance:** Resolution tests cover each step and each error code. A branch selector overrides a context branch. A deleted head on a draft masks the main version.
- **Priority:** P0

### REQ-URI-010 — Operation version qualifier
- **Statement:** For entities of named-operation types (Action, Pipeline, Query, Logic, Native and their subtypes), the major API version MUST be the last slug segment `v<N>`. The URI form `<base>@v<N>` is equivalent to slug `<base>.v<N>`.
  - Without a qualifier, resolving `<base>` finds the entity with slug `<base>` if it exists. Otherwise it finds the entity with the highest `N` among `<base>.v<N>`, and the result carries `details.op_version_implicit = true`.
  - Protocol clients and stored references SHOULD always give the qualifier.
- **Rationale:** Named operations are addressed by URI and versioned (VRD-15-03). Keeping the major version in the slug keeps it inside the locator model, with no second key.
- **Origin:** VRD-15-03, CON-LC-001, CON-LC-013
- **Acceptance:**
  1) `Action/entity.edit@v2` resolves slug `entity.edit.v2`.
  2) Unqualified `Action/entity.edit` resolves to `v2` when `v1` and `v2` exist.
- **Priority:** P0

### REQ-URI-011 — Key path extraction
- **Statement:** A `key` selector MUST return the value at the path in the **effective** data (SPEC-13) of the resolved version.
  - Segments name object properties.
  - `[n]` or a bare number indexes arrays (FU `items.0.price`).
  - A missing path is `URI.KEY_NOT_FOUND`.
  - In writes (Mutate with a keyed target URI), the key path designates the property to set in own data. Writing into an inherited list element copies the list into own data first (SPEC-13 REQ on lists).
- **Origin:** CON-FU-010
- **Acceptance:** `…?key=items.0.price` and `…?key=items[0].price` return the same value.
- **Priority:** P1

### REQ-URI-012 — ID form
- **Statement:** `ubos://tenant/Type/~<uuid>` MUST resolve by entity ID (step 5 of REQ-URI-009). Reference properties MAY declare `ref_form: id` (SPEC-13) to store rename-proof references in this form. The kernel MUST render ID-form references in the locator form in effective data returned to clients, unless the client asks for `ref_form=id`.
- **Rationale:** Locator references are readable for people and AI. ID references survive renames (FU stable identity).
- **Origin:** CON-FU-016, CON-UC-051, NEW: rename-proof references
- **Acceptance:** After a rename, an ID-form reference still resolves without a redirect warning.
- **Priority:** P1

### REQ-URI-013 — Rename redirect
- **Statement:** When a slug target is not found, the resolver MUST look for a RENAME commit with `details.old_slug` equal to the slug, of the same type in the tenant resolution order. If exactly one entity matches, it resolves to that entity with `renamed_from` set and returns warning `URI.RENAMED`. If several match, the result is `URI.AMBIGUOUS_REDIRECT`. Redirects are followed at most 5 hops.
- **Origin:** CON-FU-016, NEW: keep locator references working after RENAME
- **Acceptance:** A reference to `Customer/acme-old` resolves to the renamed entity with warning `URI.RENAMED`.
- **Priority:** P1

### REQ-URI-014 — Create targets
- **Statement:** A Mutate or Emit may target a URI whose entity does not exist yet (create). Resolution then returns `entity_id = null` and `commit_id = null` with no error. Only CREATE and COPY accept such a target; any other operation fails with `URI.NOT_FOUND`.
- **Origin:** CON-UP-010 (find or create), CON-LS-004
- **Acceptance:** Mutate CREATE on a new locator succeeds. Mutate UPDATE on it fails with `URI.NOT_FOUND`.
- **Priority:** P0

### REQ-URI-015 — Namespace navigation
- **Statement:** The query component (SPEC-21) MUST support listing entities by slug prefix within a type and tenant, returning a namespace tree (folders are derived from dotted segments and never stored).
- **Origin:** CON-FU-013, VRD-03-01
- **Acceptance:** Listing prefix `finance.taxes` returns `finance.taxes.calc_rate` and the folder `finance.taxes.eu` if deeper slugs exist.
- **Priority:** P1

### 3.4 Libraries and conformance

### REQ-URI-016 — One library per runtime
- **Statement:** URI parsing, normalisation and formatting MUST be implemented once per runtime:
  - Rust `ubos_proto::uri`, used by the kernel, the Rhai syscalls and the CLI;
  - TypeScript `@ubos/sdk/uri`, used by the web shell and the TS SDK.

  No other component may parse `ubos://` strings by hand. Resolution (REQ-URI-009) exists only in the kernel.
- **Origin:** VRD-03-05
- **Acceptance:** Both libraries pass the conformance table (API-URI-004). A lint forbids other `ubos://` string parsing in the code base.
- **Priority:** P0

### 3.5 URIs in scripts and messages

### REQ-URI-017 — URIs as universal arguments
- **Statement:** Every syscall, protocol message field and definition field that refers to an entity MUST accept a URI (canonical or short form) and MUST reject bare IDs or bare slugs without a type. The kernel normalises the URI at the boundary and records the canonical form.
- **Origin:** CON-UC-066, VRD-03-04
- **Acceptance:** `entity.get("Invoice/inv-1")` works in Rhai. `entity.get("inv-1")` fails with `URI.INVALID`.
- **Priority:** P0

### 3.6 Reserved names

### REQ-URI-018 — Reserved authorities
- **Statement:** The authorities `system`, `logrums` and `local` and the schemes `client://` and `blob://` are reserved (§2.3). Tenant creation MUST reject reserved codes (`TEN.RESERVED_CODE`).
- **Origin:** REQ-CONV-013, CON-LS-005
- **Acceptance:** Creating tenant `system` fails.
- **Priority:** P0

### 3.7 Conformance table

Inputs are shown after percent-decoding. The context for short forms is authority
`acme.prod` (tenant `acme`, Context `prod` → branch `main`), principal slug `u-17`, current
target `ubos://acme/Invoice/inv-1`. The registered cases are type `Invoice` and slug `inv-1`.

| # | Input | Result |
|---|---|---|
| C-01 | `ubos://acme/Invoice/inv-1` | ok: tenant `acme`, type `Invoice`, slug `inv-1` |
| C-02 | `ubos://acme.prod/Invoice/inv-1` | ok: tenant `acme`, context `prod` |
| C-03 | `ubos://acme.team.dev/Invoice/inv-1` | ok: tenant `acme`, context `team.dev` |
| C-04 | `ubos://acme/acme.crm.Lead/lead-0042` | ok: type `acme.crm.Lead` |
| C-05 | `ubos://acme/Invoice/inv-1?commit=41` | ok: commit 41 |
| C-06 | `ubos://acme/Invoice/inv-1?branch=feature/q4&time=2026-09-01T00:00:00Z` | ok: branch + time |
| C-07 | `ubos://acme/Invoice/inv-1?tag=release-2026.09` | ok: tag |
| C-08 | `ubos://acme/Invoice/inv-1?key=lines[0].amount&view=json` | ok: key + view |
| C-09 | `ubos://system/Action/entity.edit@v1` | ok: tenant `logrums`, slug `entity.edit.v1` |
| C-10 | `ubos://acme/Invoice/~0192f1c4-7b7e-7c4a-9d31-5b2f0e6a1c20` | ok: ID form |
| C-11 | `ubos://acme/Invoice/inv-1?commit=41&tag=x` | error `URI.SELECTOR_CONFLICT` |
| C-12 | `ubos://acme/Invoice/inv-1?commit=41&time=2026-01-01T00:00:00Z` | error `URI.SELECTOR_CONFLICT` |
| C-13 | `ubos://acme/Invoice/inv-1?branch=a&branch=b` | error `URI.DUPLICATE_PARAM` |
| C-14 | `ubos://acme/Invoice/inv-1?foo=1` | error `URI.UNKNOWN_PARAM` |
| C-15 | `ubos://acme/Invoice` | error `URI.INVALID` (missing target) |
| C-16 | `ubos://acme/Invoice/inv-1/extra` | error `URI.INVALID` |
| C-17 | `ubos://acme/Invoice/inv-1?view=table` | error `URI.INVALID_PARAM` |
| C-18 | `ubos://acme/Invoice/inv-1?time=yesterday` | error `URI.INVALID_PARAM` |
| C-19 | `http://acme/Invoice/inv-1` | error `URI.INVALID_SCHEME` |
| C-20 | `ubos://Acme/Invoice/inv-1` | ok after normalisation → `ubos://acme/Invoice/inv-1` |
| C-21 | `ubos://ac/Invoice/inv-1` | ok (tenant code min length 2) |
| C-22 | `ubos://a/Invoice/inv-1` | error `URI.INVALID_AUTHORITY` |
| C-23 | `ubos://acme/invoice/inv-1` | parse error `URI.INVALID_TYPE` (type name must start upper case) — accepted only as short-form input, where it is normalised by registry lookup (C-44) |
| C-24 | `ubos://acme/Invoice/-inv` | error `URI.INVALID_SLUG` |
| C-25 | `ubos://acme/Invoice/a.b.c.d.e.f.g.h.i` | error `URI.INVALID_SLUG` (more than 8 segments) |
| C-26 | `ubos://acme/Invoice/inv@1` | error `URI.INVALID_SLUG` (`@` must be `@v<N>`) |
| C-27 | `ubos://acme/Invoice/~not-a-uuid` | error `URI.INVALID_SLUG` |
| C-30 | `UBOS://ACME/Invoice/inv-1` | → `ubos://acme/Invoice/inv-1` |
| C-31 | `ubos://system/Type/Entity` | → `ubos://logrums/Type/Entity` |
| C-32 | `ubos://acme/Invoice/INV-1` | → `ubos://acme/Invoice/inv-1` (registered case) |
| C-33 | `ubos://acme/Invoice/inv-1?view=json&branch=dev` | → `ubos://acme/Invoice/inv-1?branch=dev&view=json` |
| C-34 | `ubos://acme/Invoice/inv-1?time=2026-09-01T08:00:00%2B08:00` | → `…?time=2026-09-01T00:00:00.000Z` |
| C-35 | `ubos://acme.prod/Invoice/inv-1?branch=main` (Context prod → main) | → `ubos://acme.prod/Invoice/inv-1` |
| C-36 | `ubos://logrums/Action/entity.edit.v1` | → `ubos://logrums/Action/entity.edit@v1` |
| C-37 | `ubos://acme/Invoice/inv%2D1` | → `ubos://acme/Invoice/inv-1` |
| C-38 | `ubos://acme/Invoice/~0192f1c4-…` (in a response) | → locator form `ubos://acme/Invoice/inv-1` |
| C-39 | `ubos://acme/Invoice/inv-1?key=lines.0.amount` | → `…?key=lines[0].amount` |
| C-40 | `Invoice/inv-1` | → `ubos://acme.prod/Invoice/inv-1` |
| C-41 | `/Invoice/inv-1` | → `ubos://acme.prod/Invoice/inv-1` |
| C-42 | `self` | → `ubos://acme/Invoice/inv-1` |
| C-43 | `Invoice/inv-1@draft` | → `ubos://acme.prod/Invoice/inv-1?branch=draft/u-17/main` |
| C-44 | `invoice/inv-1` | → `ubos://acme.prod/Invoice/inv-1` (type case from registry) |
| C-45 | `Invoice/inv-1@41` | → `ubos://acme.prod/Invoice/inv-1?commit=41` |
| C-46 | `ubos:///acme/Invoice/inv-1` | → `ubos://acme/Invoice/inv-1` |
| C-47 | `ubos://Invoice/inv-1` (no tenant `invoice` exists) | → `ubos://acme.prod/Invoice/inv-1` |
| C-48 | `sys:Entity` | → `ubos://logrums/Type/Entity` |
| C-49 | `Invoice/inv-1.json` (no slug `inv-1.json`) | → `ubos://acme.prod/Invoice/inv-1?view=json` |

---

## 4. Interfaces

### API-URI-001 — URI value type (Rust)
- **Kind:** rust struct + functions in `ubos_proto::uri`
- **Signature / shape:**
```rust
pub struct UbosUri { /* fields of ENT-UbosUri */ }
impl UbosUri {
    pub fn parse(s: &str) -> Result<UbosUri, UriError>;            // canonical grammar only
    pub fn parse_short(s: &str, ctx: &ShortFormContext) -> Result<UbosUri, UriError>;
    pub fn to_canonical(&self, reg: Option<&dyn CaseRegistry>) -> String;
    pub fn locator(&self) -> Option<Locator>;
    pub fn with_commit(&self, c: CommitId) -> UbosUri;
    pub fn without_selectors(&self) -> UbosUri;
}
pub struct ShortFormContext<'a> {
    pub authority: &'a Authority, pub principal_slug: &'a str,
    pub current_target: Option<&'a UbosUri>, pub base_branch: &'a BranchName,
    pub is_tenant: &'a dyn Fn(&str) -> bool,
    pub alias: &'a dyn Fn(&str) -> Option<UbosUri>,
}
pub trait CaseRegistry { fn type_case(&self, t: &str) -> Option<String>; fn slug_case(&self, loc: &Locator) -> Option<String>; }
```
- **Semantics:** Parsing is pure and does no I/O. Case normalisation needs a registry lookup, which the kernel supplies.
- **Origin:** REQ-URI-006…008, REQ-URI-016

### API-URI-002 — Resolver (kernel)
- **Kind:** rust trait in `ubos_kernel::uri`
- **Signature / shape:**
```rust
#[async_trait]
pub trait UriResolver: Send + Sync {
    async fn resolve(&self, x: &ExecutionContext, uri: &str, intent: ResolveIntent)
        -> Result<ResolvedAddress, UbosError>;       // intent: Read | Write | Create
    async fn resolve_many(&self, x: &ExecutionContext, uris: &[&str], intent: ResolveIntent)
        -> Vec<Result<ResolvedAddress, UbosError>>;
    async fn canonical(&self, x: &ExecutionContext, uri: &str) -> Result<String, UbosError>;
}
```
- **Semantics:** Implements REQ-URI-009. Reads through a session record DEPENDS_ON_DATA provenance for the resolved commit (SPEC-15).
- **Origin:** REQ-URI-009

### API-URI-003 — TypeScript library
- **Kind:** TS module `@ubos/sdk/uri`
- **Signature / shape:**
```ts
export interface UbosUri { authority: { tenant: string; context?: string }; typeCode: string;
  target: { slug: string } | { id: string }; opVersion?: number;
  branch?: string; commit?: string; tag?: string; time?: string; key?: string;
  view?: 'summary'|'detail'|'json'|'schema'|'config'; }
export function parse(s: string): UbosUri;                       // throws UriError{code}
export function parseShort(s: string, ctx: ShortFormContext): UbosUri;
export function format(u: UbosUri): string;                      // canonical without registry case
export function equals(a: string, b: string): boolean;           // after syntactic normalisation
```
- **Semantics:** `commit` is a string (REQ-CONV-033).
- **Origin:** REQ-URI-016

### API-URI-004 — Conformance fixture
- **Kind:** data file
- **Signature / shape:** `spec-fixtures/uri-conformance.json`: an array of `{id, input, context?, registry?, expect: {ok: UbosUri|canonical string} | {error: code}}` generated from §3.7. Both libraries run it in CI.
- **Origin:** VRD-03-05

### Error codes (URI)

| Code | Category | Meaning |
|---|---|---|
| `URI.INVALID` | INVALID | not a URI of the grammar |
| `URI.INVALID_SCHEME` | INVALID | scheme is not `ubos` |
| `URI.INVALID_AUTHORITY` | INVALID | bad tenant code or context slug |
| `URI.INVALID_TYPE` | INVALID | bad type code |
| `URI.INVALID_SLUG` | INVALID | bad slug or ID form |
| `URI.INVALID_PARAM` | INVALID | bad parameter value |
| `URI.UNKNOWN_PARAM` | INVALID | unknown parameter |
| `URI.DUPLICATE_PARAM` | INVALID | parameter repeated |
| `URI.SELECTOR_CONFLICT` | INVALID | incompatible selectors |
| `URI.NOT_FOUND` | NOT_FOUND | tenant, entity or version not found or not visible |
| `URI.TYPE_NOT_FOUND` | NOT_FOUND | type code not registered in resolution order |
| `URI.CONTEXT_NOT_FOUND` | NOT_FOUND | context slug unknown |
| `URI.KEY_NOT_FOUND` | NOT_FOUND | key path absent |
| `URI.TYPE_MISMATCH` | INVALID | ID form entity is not of the type segment |
| `URI.COMMIT_MISMATCH` | INVALID | commit or tag does not belong to the entity |
| `URI.LOCATOR_TAKEN` | CONFLICT | locator held by another entity |
| `URI.ID_TAKEN` | CONFLICT | proposed ID already used |
| `URI.AMBIGUOUS_REDIRECT` | CONFLICT | several renamed entities match |
| `URI.RENAMED` | (warning) | resolved through a rename redirect |

---

## 5. Non-functional

| ID | Requirement | Target |
|---|---|---|
| NFR-PERF-020 | Parse + normalise (no registry) | ≤ 5 µs per URI (Rust), ≤ 50 µs (TS) |
| NFR-PERF-021 | Resolve with warm caches | p95 ≤ 0.5 ms |
| NFR-AIR-001 | URIs are human-readable in prompts and audit | Locator form by default (REQ-URI-007, REQ-URI-012) |

---

## 6. Acceptance

| REQ | Criterion |
|---|---|
| REQ-URI-001…003 | ID and locator rules tested; conformance rows C-20…C-27 |
| REQ-URI-004…005 | Rename and copy commits with `details`; ID stable |
| REQ-URI-006…008 | Conformance rows C-01…C-49 pass in Rust and TS |
| REQ-URI-009 | Resolution step tests and error codes |
| REQ-URI-010 | Version qualifier tests |
| REQ-URI-011…013 | Key path, ID form and redirect tests |
| REQ-URI-014 | Create target test |
| REQ-URI-015 | Namespace tree test |
| REQ-URI-016 | Lint plus shared fixture in CI |
| REQ-URI-017 | Syscalls reject bare IDs and slugs |
| REQ-URI-018 | Reserved code rejection |

---

## 7. Implementation notes

- **Reference parsers:**
  - FU custom parser, which explicitly avoids `java.net.URI` [FU:backend/src/main/java/org/logrum/ubos/kernel/util/UbosUriUtil.java], and the frontend modes [FU:frontend/src/utils/ubosUri.ts].
  - LS `uri.rs` [LS:crates/ubos_kernel/src/semantic/uri.rs#L8-L92], which has the version modes.
  - UC `uri.rs` [UC:src/domain/uri.rs#L23-L87], which has the round trip.
  - UB `UbosUri.parse` [UB:backend/src/main/java/com/ubos/core/api/UbosUri.java].
- **Divergences resolved here:**
  - UB's branch path segment is dropped.
  - FU's `cid=` alias is dropped.
  - FU's `default` scope becomes the current authority.
  - LS's triple slash is tolerated as input only.
  - UB's type inference from slug prefixes is replaced by the explicit type segment. Its intent suffix becomes `view=`.
- **Authority syntax** differs from UB, which looked up context slugs in a global `sys_boot` space. Here the context slug is qualified by its tenant (`acme.prod`), so context slugs never collide across tenants and resolution needs no global lookup (ADR-005, adapting VRD-03-03).
- **Caching:** cache (authority → Context commit) and (locator → entity ID) per tenant. Invalidate them on commits of Context entities and on RENAME/DELETE (the outbox events of SPEC-20).

## 8. Open questions

None.
