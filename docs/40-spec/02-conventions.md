---
id: SPEC-02
title: "Conventions and ID Registry"
status: complete
phase: P3
depends_on: [SPEC-00, SPEC-01]
sources: [UP, FU, LC, UB, UC, US, LS, UW]
---

# Conventions and ID Registry

## 0. Chapter header

- **Scope:** rules shared by every spec chapter and every implementation:
  - normative language and ID registries;
  - naming (tenants, slugs, type codes, property keys, branches, operations);
  - identifiers and time;
  - the notation for ENT fields;
  - the payload rules;
  - the error model;
  - storage rules;
  - code and repository conventions.
- **MOD:** `CONV`.
- **depends_on:** SPEC-01.
- **Terms used:** TERM-Entity, TERM-Locator, TERM-Slug, TERM-Namespace, TERM-TypeCode, TERM-Uri, TERM-RootTenant, TERM-Branch, TERM-SixTables, TERM-RuntimeStore.
- **Origin summary:**
  - VRD-22-01 (conventions are part of the spec) and VRD-22-04 (one vocabulary).
  - VRD-03-01 (slug regex, dotted namespaces), VRD-02-02 (naming regex, reserved keys) and VRD-02-07 with Q-006 (namespaced extension keys).
  - VRD-01-01 (table naming) and VRD-01-06 with ADR-004 (runtime store).
- **Chapter rule:** a later chapter may add rules for its own module. It MUST NOT contradict this chapter. A conflict is resolved by an ADR.

---

## 1. Normative language

- REQ-CONV-001: The key words MUST, MUST NOT, SHOULD, SHOULD NOT and MAY are used as in RFC 2119 / RFC 8174. They are normative only in upper case.
- REQ-CONV-002: Items marked **Priority P0** belong to the minimal viable kernel (SPEC-00 §5). P1 items belong to slice 2. P2 items are later versions.
- REQ-CONV-003: Text marked *Interpretation* or *Note* is informative.

---

## 2. ID registry

### 2.1 Document and item IDs

The document ID scheme is defined in WRITING-RULES §4. This chapter adds the registries it
deferred to P3.

| Kind | Pattern | Numbering |
|---|---|---|
| Requirement | `REQ-<MOD>-NNN` | per MOD, from 001, in the chapter that owns the MOD |
| Interface | `API-<MOD>-NNN` | per MOD, from 001 |
| Non-functional | `NFR-<AREA>-NNN` | per AREA, from 001; defined in SPEC-32 or in a chapter's §5 |
| Entity | `ENT-<PascalName>` | no number; the name equals the platform type code where one exists (e.g. `ENT-Context` ↔ type `Context`) |
| Term | `TERM-<PascalName>` | SPEC-01 only |
| Decision | `ADR-NNN` | global, from 001 |
| Error code | `<MOD>.<UPPER_SNAKE>` | per MOD, §8 |

Rules:
- REQ-CONV-004: IDs are never reused or renumbered. A withdrawn item keeps its ID with the statement `Withdrawn: <reason>`.
- REQ-CONV-005: An item with the same ID is defined once. Other chapters reference it and do not restate it.
- REQ-CONV-006: A REQ that belongs to another chapter's module uses that chapter's MOD and is moved there in P5.

### 2.2 Module codes (MOD)

| MOD | Chapter | File | Module |
|---|---|---|---|
| CONV | SPEC-02 | `02-conventions.md` | Conventions |
| ARCH | SPEC-10 | `10-architecture.md` | Architecture |
| STO | SPEC-11 | `11-storage/` | Storage |
| URI | SPEC-12 | `12-identity-uri.md` | Identity and addressing |
| META | SPEC-13 | `13-meta-model/` | Meta-model |
| VER | SPEC-14 | `14-versioning-branching.md` | Versioning and branching |
| TX | SPEC-15 | `15-transactions.md` | Transactions and processes |
| CTX | SPEC-16 | `16-context.md` | Context |
| RT | SPEC-17 | `17-logic-runtime.md` | Logic runtime |
| RULE | SPEC-18 | `18-rules-validation.md` | Rules and validation |
| FLOW | SPEC-19 | `19-orchestration/` | Orchestration |
| EVT | SPEC-20 | `20-events-jobs/` | Events and jobs |
| QRY | SPEC-21 | `21-query-search.md` | Query and search |
| SEC | SPEC-22 | `22-security/` | Security |
| TEN | SPEC-23 | `23-tenancy.md` | Tenancy |
| PROTO | SPEC-24 | `24-protocol/` | Protocol (UBTP) |
| UI | SPEC-25 | `25-ui-protocol/` | UI protocol |
| SHELL | SPEC-26 | `26-client-shells.md` | Client shells |
| AI | SPEC-27 | `27-ai/` | AI |
| PKG | SPEC-28 | `28-packages-boot.md` | Packages and boot |
| ONT | SPEC-29 | `29-base-ontology.md` | Base ontology |
| OBS | SPEC-30 | `30-audit-observability.md` | Audit and observability |
| DEP | SPEC-31 | `31-deployment-editions.md` | Deployment and editions |
| NFR | SPEC-32 | `32-nfr-catalogue.md` | NFR catalogue |

### 2.3 NFR areas

| AREA | Meaning |
|---|---|
| PERF | latency, throughput |
| SCAL | data and load growth, partitioning |
| AVAIL | availability, failover |
| DUR | durability, backup, restore |
| SEC | security properties (not implementation defects) |
| PRIV | privacy, masking, retention |
| PORT | portability across editions and platforms |
| AIR | AI-readiness (machine-readable definitions, prompt size) |
| OPS | operability, observability, upgrades |
| UX | client responsiveness and accessibility |

---

## 3. Naming

### 3.1 Character classes

- REQ-CONV-010: All identifiers defined below are ASCII. Display names and descriptions are Unicode text.
- REQ-CONV-011: Comparisons of tenant codes, slugs, type codes and branch names are **case-insensitive** for uniqueness. The stored form keeps the case given at creation.

### 3.2 Tenant codes

- REQ-CONV-012: A tenant code MUST match `^[a-z][a-z0-9-]{1,62}$`.
- REQ-CONV-013: Reserved tenant codes:
  - `logrums` is the root tenant (TERM-RootTenant; VRD-18-01). It holds genesis and platform packages.
  - `system` is an authority alias that resolves to `logrums` (VRD-03-04). No tenant may be created with this code.
  - `local` is reserved for the personal edition's default tenant.

### 3.3 Slugs and namespaces

- REQ-CONV-014: A slug MUST match:
  ```
  segment = [A-Za-z0-9] ( [A-Za-z0-9_-]{0,62} [A-Za-z0-9] )?
  slug    = segment ( "." segment ){0,7}
  ```
  The maximum length is 200 characters (VRD-03-01).
- REQ-CONV-015: The dotted prefix of a slug is its namespace (TERM-Namespace). Entities that a package installs MUST carry the package namespace as their slug prefix, e.g. `acme.crm.lead-stages`. Entities in the root package carry no namespace, or `sys.` for kernel internals.
- REQ-CONV-016: User-created business data slugs MAY be generated. The default generator produces `<type-lower>-<base32 of 10 random bytes>`, e.g. `invoice-k3v9q2mf7t1c0b8h`.

### 3.4 Type codes

- REQ-CONV-017: A type code is the slug of a Type entity. It MUST match:
  ```
  type_code = ( ns_segment "." ){0,4} Name
  ns_segment = [a-z][a-z0-9]{0,30}
  Name       = [A-Z][A-Za-z0-9]{0,62}
  ```
  Examples: `Entity`, `Type`, `Email`, `Money`, `acme.Invoice`, `acme.crm.Lead`.
- REQ-CONV-018: Root package types have no namespace. Every other type MUST be namespaced with its package namespace.
- REQ-CONV-019: Corpus type codes such as `sys.biz.text.email` or `sys_function` are normalised to this form in SPEC-29 (e.g. `Email`, `Logic`). The normalisation table in SPEC-29 is the only place where old codes appear.

### 3.5 Property keys (Q-006)

- REQ-CONV-020: A property key in a payload MUST match `^([a-z][a-z0-9]{0,30}\.){0,3}[a-z][a-z0-9_]{0,62}$`.
- REQ-CONV-021: Properties defined by root package types use **unprefixed** keys (`email`, `display_name`).
- REQ-CONV-022: A property that a tenant or a non-root package adds to a type it does not own MUST use a **dotted namespaced key**, e.g. `acme.email`, `acme.crm.score` (Q-006, VRD-02-07). A package's own types MAY use unprefixed keys for their own properties.
- REQ-CONV-023: Keys that start with `_` are reserved for the kernel. Defined reserved keys:

| Key | Meaning |
|---|---|
| `_extends` | parent(s) for inheritance (TERM-Extends) |
| `_id` | entity ID in hydrated references |
| `_uri` | canonical URI in hydrated references or query results |
| `_type` | type code in query results and hydrated references |
| `_commit` | commit ID in query results |
| `_meta` | kernel-computed metadata (read-only, never stored) |
| `_state` | lifecycle state for action state machines (SPEC-19) |

  A client or script that writes a reserved key other than `_extends` and `_state` MUST get error `META.RESERVED_KEY`. `_state` is written only by transitions.

### 3.6 Branch and tag names

- REQ-CONV-024: A branch name MUST match `^[a-z0-9][a-z0-9_-]{0,62}(/[a-z0-9][a-z0-9_-]{0,62}){0,3}$`. Segments contain no dots, so the Branch entity's slug is the name with `/` replaced by `.` (a one-to-one mapping, SPEC-14).
- REQ-CONV-025: The default branch is `main`. Reserved prefixes:
  - `draft/<principal-slug>/<base>`: draft branches (VRD-08-03).
  - `overlay/<name>`: overlay branches inside one tenant (VRD-08-02). A tenant's overlay of root-package entities needs no branch name: it is stored as tenant-scoped heads (SPEC-11 REQ-STO-006, SPEC-23).
  - `release/<version>`: release branches.
- REQ-CONV-026: A tag name is the slug of a Tag entity (REQ-CONV-014), e.g. `release-2026.09` (SPEC-14).

### 3.7 Named operations and versions

- REQ-CONV-027: Actions, pipelines, queries, logic and native handlers are addressed by URI. A version qualifier `@v<N>` may follow the slug in the URI path, e.g. `ubos://system/Action/entity.edit@v1`. The qualifier is not part of the slug. SPEC-12 defines its grammar and resolution.
- REQ-CONV-028: Packages use semantic versions (`MAJOR.MINOR.PATCH`). The syscall ABI (`v1`, `v2`, …) and UBTP (`1`, `2`, …) use integer major versions.

### 3.8 Other names

| Item | Form | Example |
|---|---|---|
| Enum values (status, action, kinds) | `UPPER_SNAKE` | `ACTIVE`, `DEPENDS_ON_FUNC` |
| Event types | dotted lower snake | `entity.committed`, `job.failed` |
| Syscall names | `namespace.function` lower snake | `entity.get`, `query.find`, `ai.complete` |
| Slot names | lower snake | `validate`, `mask`, `ui` |
| Environment variables of a Context | `UPPER_SNAKE` | `DEFAULT_CURRENCY` |
| Error codes | `<MOD>.<UPPER_SNAKE>` | `VER.HEAD_CONFLICT` |
| JSON fields in protocol and definitions | `snake_case` | `expected_head` |

---

## 4. Identifiers

- REQ-CONV-030: Entity IDs, process IDs, request IDs and job attempt IDs are **UUID version 7** (time-ordered). Entity IDs are never reused (VRD-03-01).
- REQ-CONV-031: Commit IDs are **int64**, taken from one monotonically increasing sequence per database. A commit ID identifies exactly one version.
- REQ-CONV-032: Branch height (`seq_num`) is int64. It starts at 1 for the first commit of an entity on a branch and grows by 1 per commit.
- REQ-CONV-033: Clients MUST treat all IDs as opaque. JSON carries UUIDs as lowercase hyphenated strings. Commit IDs and heights are JSON **strings** of decimal digits, so that JavaScript clients do not lose precision.

---

## 5. Time

- REQ-CONV-034: Timestamps are UTC with millisecond precision or better.
  - In JSON they are RFC 3339 strings with a `Z` suffix, e.g. `2026-09-28T08:15:30.123Z`.
  - In PostgreSQL they are `timestamptz`; in SQLite they are the same RFC 3339 text.
- REQ-CONV-035: Business dates without time are `YYYY-MM-DD`. Time zones for business meaning (e.g. office hours) are stored as IANA names in separate properties.
- REQ-CONV-036: Durations in definitions are ISO 8601 durations (`PT30S`). Durations in configuration may use milliseconds with an `_ms` suffix.

---

## 6. Spec type notation (for ENT fields)

ENT definitions (TEMPLATES §S) use these types. They are specification types; SPEC-11 and
SPEC-13 map them to storage columns and semantic types.

| Notation | Meaning | JSON form |
|---|---|---|
| `uuid` | UUID v7 | string |
| `commit_id` | int64 commit ID | string of digits |
| `int32`, `int64` | integers | number (`int64` as string in protocol payloads) |
| `decimal(p,s)` | exact decimal | string, e.g. `"1250.00"` |
| `float64` | IEEE double (measurements only) | number |
| `bool` | boolean | `true` / `false` |
| `string` / `string(n)` | text, optional max length | string |
| `text` | long text | string |
| `slug`, `type_code`, `tenant_code`, `branch_name` | names per §3 | string |
| `uri` / `uri<Type>` | canonical `ubos://` URI, optionally of a given type | string |
| `timestamp`, `date`, `duration` | per §5 | string |
| `json` | any JSON value | any |
| `json_schema` | a JSON Schema document | object |
| `rhai` | Rhai source text | string |
| `enum{A\|B\|C}` | closed set of `UPPER_SNAKE` values | string |
| `list<T>` | ordered list | array |
| `map<K,V>` | object with keys of kind K | object |
| `vector(n)` | float vector of n dimensions | array of numbers |
| `bytes` | binary data | base64 string |
| `T?` | optional (the key may be absent or null) | – |

- REQ-CONV-037: Money and other exact quantities MUST use `decimal(p,s)` with the scale from the semantic type (SPEC-29). They MUST NOT use `float64`.

---

## 7. Payload conventions

- REQ-CONV-040: A snapshot is a JSON object. Its top-level keys are property keys (§3.5) or reserved keys (§3.5).
- REQ-CONV-041: An **absent** key means "not set / inherit". An explicit `null` means "set to empty", and it overrides an inherited value. Composition (SPEC-13) MUST respect this difference.
- REQ-CONV-042: A reference to another entity is stored as a canonical URI string without version selectors, unless the property is declared as pinned (SPEC-13).
- REQ-CONV-043: Lists are replaced as a whole on composition, unless the property declares a merge key (SPEC-13).
- REQ-CONV-044: Snapshots SHOULD stay below 1 MiB. Large binary content is stored outside the snapshot and referenced by URI (SPEC-11).

---

## 8. Error model

### 8.1 Error object

Every error returned by the kernel, the protocol or a syscall has this shape:

```yaml
error:
  code: string           # <MOD>.<UPPER_SNAKE>, e.g. VER.HEAD_CONFLICT
  category: enum{INVALID|NOT_FOUND|CONFLICT|UNAUTHENTICATED|FORBIDDEN|LIMIT|UNAVAILABLE|INTERNAL}
  message: string        # English, one sentence, no secrets or personal data
  path: string?          # JSON path of the offending field, e.g. $.lines[2].amount
  target: uri?           # entity or operation concerned
  details: json?         # machine-readable context, e.g. expected and actual head
  retryable: bool
  process_id: uuid?      # set when a process was started
  trace_id: string?      # telemetry trace
  causes: list<error>?   # nested errors (e.g. all validation failures)
```

- REQ-CONV-050: Validation failures MUST be collected and returned together as `causes` of one `RULE.VALIDATION_FAILED` error, each with a `path`.
- REQ-CONV-051: Error codes are part of the public contract. A chapter lists its codes in §4 (Interfaces). Renaming a code is a breaking change.

### 8.2 Category mapping

| Category | Meaning | HTTP binding | Retry |
|---|---|---|---|
| INVALID | request or data violates a rule | 400 / 422 (validation) | no |
| NOT_FOUND | target does not exist or is not visible | 404 | no |
| CONFLICT | CAS failure, duplicate locator, merge conflict | 409 | after refresh |
| UNAUTHENTICATED | no or invalid credentials | 401 | no |
| FORBIDDEN | denied by policy | 403 | no |
| LIMIT | sandbox, rate or size limit exceeded | 429 / 413 | after delay (rate) |
| UNAVAILABLE | dependency down, timeout | 503 | yes |
| INTERNAL | unexpected failure | 500 | no |

- REQ-CONV-052: An entity the principal may not read MUST be reported as NOT_FOUND, not FORBIDDEN (SPEC-22).

---

## 9. Storage rules

- REQ-CONV-060: **Six-table rule.** All business and metadata state MUST live in the six tables (TERM-SixTables; VRD-01-01). Implementations MUST NOT add tables for business or metadata state. A new concept becomes a type, not a table.
- REQ-CONV-061: Physical table names use the prefix `ubos_` plus the logical name:

| Logical name | Physical name |
|---|---|
| `entity_instance` | `ubos_entity_instance` |
| `entity_version` | `ubos_entity_version` |
| `branch_head` | `ubos_branch_head` |
| `search_index` | `ubos_search_index` |
| `process_log` | `ubos_process_log` |
| `process_commit_map` | `ubos_process_commit_map` |

- REQ-CONV-062: **Runtime store.** Transient technical state (queue signals, event delivery, caches, idempotency keys, rate limits, session memory cache) lives in the declared runtime store (TERM-RuntimeStore; ADR-004). The runtime store is never authoritative: losing it may lose in-flight work or cached data, but no committed business fact. If an implementation keeps runtime state in SQL, the tables go in a separate schema `ubos_rt` (PostgreSQL) or a separate database file (SQLite), with the prefix `rt_`.
- REQ-CONV-063: Job records, schedules, subscriptions, webhooks, approvals and process instances are **entities** (ADR-004). They are not runtime-store records.
- REQ-CONV-064: Schema migrations of the six tables are versioned files that each change one concept (CON-UP-043). They are part of the kernel release, never of a package.

---

## 10. Language rules

- REQ-CONV-070: Everything in code is in English: identifiers, comments, log messages, error messages, commit messages and definition codes. User-facing text is translated through i18n keys (SPEC-27, VRD-16-07).
- REQ-CONV-071: Identifiers use US spelling (`behavior`, `normalize`). Spec prose may use either spelling.
- REQ-CONV-072: Use only glossary terms (SPEC-01). A synonym in code, API or definitions is a defect of conformance (VRD-22-04).

---

## 11. Code and repository conventions

These rules apply to the implementation repositories built from this spec (ADR-001).

### 11.1 Rust

- REQ-CONV-080: The workspace uses these crates, with the `ubos_` prefix (SPEC-10 details them):
  - `ubos_proto`: protocol and data types;
  - `ubos_store`: the store traits and the PostgreSQL and SQLite backends;
  - `ubos_kernel`: the kernel;
  - `ubos_runtime`: the Rhai runtime and syscalls;
  - the hosts: `ubos_server`, `ubos_cli` and `ubos_desktop`;
  - `ubos_sdk`: the Rust client.
- REQ-CONV-081: Rust stable, edition 2021 or later. `cargo fmt` and `cargo clippy -D warnings` clean. `unsafe` code only behind a reviewed module with a safety comment.
- REQ-CONV-082: Errors:
  - Library crates define error enums with `thiserror`, mapped to §8 codes at the kernel boundary.
  - `anyhow` is allowed only in hosts and tools.
  - Panics are not a form of error reporting.
- REQ-CONV-083: Logging and telemetry use `tracing`, with structured fields (`tenant`, `process_id`, `commit_id`, `principal`). Logs MUST NOT contain secrets or snapshot contents by default (SPEC-30).
- REQ-CONV-084: Async code uses Tokio. Rhai runs on blocking threads or a dedicated pool, never on the async executor directly (CON-UC-005).

### 11.2 TypeScript

- REQ-CONV-085: Strict TypeScript mode, ES modules, React function components. The server state comes only through the TypeScript SDK (UBTP client). zustand holds UI state only.
- REQ-CONV-086: The web shell contains no business rules that the kernel also enforces. It may pre-validate with the derived schema for user feedback (SPEC-25).

### 11.3 Files and repository

- REQ-CONV-087: A source file SHOULD stay under 400 lines, and a document under 800 lines (WRITING-RULES R2.2).
- REQ-CONV-088: Each implementation repository contains an instruction file (`AGENTS.md`, with `CLAUDE.md` as a copy or link) generated from `docs/00-meta/AI-GUIDE.md` (VRD-22-01, TERM-InstructionFile).
- REQ-CONV-089: Package sources in a repository use the layout of SPEC-28 (`packages/<namespace>/…`). The store is the truth; files are for development and exchange (VRD-17-04).
- REQ-CONV-090: Commit messages follow Conventional Commits with the MOD as scope, e.g. `feat(VER): three-way merge`.

---

## 12. Spec-writing conventions for P4

- REQ-CONV-091: Each chapter follows TEMPLATES §S. Section 3 lists REQs in the order an implementer builds them.
- REQ-CONV-092: Every REQ has an Origin (VRD, HL, CON or `NEW: <reason>`) and at least one verifiable acceptance criterion. Where possible, the criterion is a scenario (TERM-Scenario) the CLI can run (VRD-22-02, VRD-22-03).
- REQ-CONV-093: ENT definitions use §6 notation. Field names are `snake_case` and match the JSON payload keys.
- REQ-CONV-094: API items give Rust trait signatures for kernel interfaces and JSON message shapes for protocol interfaces.
- REQ-CONV-095: A verdict that a chapter does not implement is listed in its §8 as deferred, with the reason (SPEC-00 §6).

---

## 13. Acceptance

| REQ | Criterion |
|---|---|
| REQ-CONV-012…026 | A shared conformance table of valid and invalid names (tenant, slug, type code, property key, branch) passes in the kernel and in the TypeScript SDK |
| REQ-CONV-030…033 | IDs are UUID v7 and are serialised as specified; commit IDs round-trip as strings through the web client |
| REQ-CONV-040…043 | Composition tests show that absent keys inherit, `null` overrides, and lists are replaced |
| REQ-CONV-050…052 | Protocol tests check the error shape, the category mapping and NOT_FOUND masking |
| REQ-CONV-060…063 | A schema check finds exactly the six business tables; runtime-store loss tests lose no committed data |
| REQ-CONV-080…090 | CI runs fmt, clippy and a check of the instruction file |

## 14. Open questions

None. Q-006 is answered by §3.5.
