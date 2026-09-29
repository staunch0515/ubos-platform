---
id: SPEC-28
title: "Packages, Loader, Genesis and Boot"
status: complete
phase: P4
depends_on: [SPEC-10, SPEC-13, SPEC-14, SPEC-15, SPEC-22, SPEC-23, SPEC-24]
sources: [UP, LC, UB, UC, US, LS]
---

# Packages, Loader, Genesis and Boot

## 0. Chapter header

- **Scope:** This chapter covers:
  - the package format (files and archive), the manifest, and the installed `Package` entity;
  - the loader algorithm (install, upgrade with three-way merge, uninstall);
  - export of packages and data;
  - genesis (the `system` and `ontology` root packages);
  - boot strategies and the boot sequence;
  - development mode (files with hot reload);
  - package scenarios as acceptance tests.
- **MOD:** `PKG`
- **depends_on:** SPEC-10, SPEC-13, SPEC-14, SPEC-15, SPEC-22, SPEC-23, SPEC-24.
- **Terms used:** TERM-Package, TERM-Genesis, TERM-Loader, TERM-Scenario, TERM-Namespace, TERM-Merge, TERM-Branch.
- **Origin summary:**
  - Verdicts: VRD-17-01…05, VRD-22-02, VRD-22-03.
  - UB: genesis through the kernel, meta-loader design (topological batches, SHA-256 dirty check, loader branch), apps as bundles, context export/import (CON-UB-031, 046, 062).
  - UC: data-driven genesis (CON-UC-033).
  - LS: an ordered, idempotent bootloader with one process per file (CON-LS-024).
  - UP: genesis strategies (CON-UP-027).
  - LC: file definitions with hot reload, data-driven test cases (CON-LC-004, 061).
  - US: genesis deltas, scenarios, the three-piece delivery (CON-US-041, 043, 063).

---

## 1. Concepts

- **Everything installable is a package.** The kernel's own meta-model (`system`), the base ontology (`ontology`), apps, verticals, tenant configurations and data sets are all packages (VRD-17-01).
- **Files for development, the store for truth.** Package sources are files in Git. Installing them always commits them through the loader. The kernel never reads definitions from files at run time (VRD-17-04).
- **Upgrades are merges.**
  - Each package has a loader branch. Every package version is committed there and merged into the target branch.
  - Local tenant changes to package entities are therefore preserved by three-way merge, and conflicts are reported instead of overwritten (UB meta-loader design, SPEC-14).

---

## 2. Model

### 2.1 Source layout

```
packages/<code>/                       # code = namespace, e.g. acme.crm (root packages: system, ontology)
  package.yaml                         # manifest (ENT-PackageManifest)
  entities/<TypeCode>/<slug>.json      # one entity per file: {"data": {...}} (type and slug from the path)
  entities/<TypeCode>/*.bundle.json    # optional bundles: [{"slug": "...", "data": {...}}, ...]
  logic/<slug>.rhai                    # Logic sources; referenced by entities/Logic/<slug>.json "source_file"
  i18n/<locale>.json                   # translations (SPEC-27)
  scenarios/<name>.yaml                # UBTP scenarios (SPEC-24 REQ-PROTO-040)
  migrations/<from>-<to>.yaml          # optional data migration pipelines
  README.md                            # human documentation (also used by AI, SPEC-27)
```

- The archive form is `<code>-<version>.ubpkg`: a ZIP of the same tree plus `CHECKSUMS` (SHA-256 per file).
- Signing and remote registries are deferred (VRD-17-05).

```yaml
ENT-PackageManifest:
  purpose: package.yaml content.
  origin: [VRD-17-01, CON-UB-062, CON-US-063]
  fields:
    - {name: code, type: string, required: true, description: "Package code = its namespace (REQ-CONV-015); reserved: system, ontology"}
    - {name: version, type: string, required: true, description: "Semantic version MAJOR.MINOR.PATCH"}
    - {name: title, type: string, required: true, description: "Display name"}
    - {name: description, type: text, required: true, description: "What the package provides (people and AI)"}
    - {name: kind, type: "enum{ROOT|ONTOLOGY|APP|VERTICAL|CONFIG|DATA}", required: true, description: "ROOT/ONTOLOGY install only into logrums"}
    - {name: dependencies, type: "list<{code, version: semver range}>", required: false, description: "Required packages"}
    - {name: kernel, type: "{min_version, abi: [string]}", required: true, description: "Kernel compatibility (REQ-PKG-003)"}
    - {name: target, type: "enum{ROOT|TENANT|ANY}", required: true, description: "Where it may be installed"}
    - {name: entities, type: "list<glob>?", required: false, description: "Entity files (default entities/**)"}
    - {name: post_install, type: uri?, required: false, description: "Pipeline/Logic run after install or upgrade (own process)"}
    - {name: migrations, type: "list<{from: semver range, pipeline: uri}>?", required: false, description: "Data migrations run on upgrade from matching versions"}
    - {name: uninstall, type: "enum{REMOVE_DEFINITIONS|KEEP}", required: false, description: "Default REMOVE_DEFINITIONS (REQ-PKG-020)"}
    - {name: overwrite, type: "enum{MERGE|KEEP_LOCAL|OVERWRITE}", required: false, description: "Default conflict strategy on upgrade (REQ-PKG-012); default MERGE"}
    - {name: scenarios, type: "list<glob>?", required: false, description: "Acceptance scenarios (default scenarios/**)"}
    - {name: license, type: string?, required: false, description: "SPDX id"}
```

```yaml
ENT-Package:
  purpose: Installed package record (TERM-Package) - entity of type Package in the target tenant, slug = code.
  origin: [VRD-17-01, VRD-17-02, CON-UB-062]
  fields:
    - {name: version, type: string, required: true, description: "Installed version"}
    - {name: manifest, type: json, required: true, description: "Manifest as installed"}
    - {name: digest, type: bytes, required: true, description: "SHA-256 over the canonical package content"}
    - {name: loader_branch, type: branch_name, required: true, description: "pkg/<code with '.' replaced by '-'>"}
    - {name: items, type: "map<uri, {hash: bytes, commit_id: commit_id}>", required: true, description: "Installed items (canonical URI without selector) with content hash and loader-branch commit"}
    - {name: status, type: "enum{INSTALLING|INSTALLED|UPGRADING|FAILED|UNINSTALLED}", required: true, description: "Lifecycle"}
    - {name: installed_at, type: timestamp, required: true, description: "First install"}
    - {name: last_report, type: json?, required: false, description: "Last install/upgrade report (REQ-PKG-013)"}
  invariants:
    - At most 10 000 items per package (larger content is split into several packages).
    - Every installed entity has entity_instance.package_id = this Package's entity id (REQ-STO-002 column).
```

### 2.2 Entity file

```json
// packages/acme.crm/entities/Type/acme.crm.Lead.json
{ "data": {
    "code": "acme.crm.Lead", "title": "Lead", "kind": "ENTITY",
    "_extends": ["Entity"],
    "properties": [
      { "name": "company", "type": "Text", "required": true, "searchable": true },
      { "name": "email", "type": "Email", "unique": "TENANT", "searchable": true },
      { "name": "owner", "type": "Reference", "target": "User" } ],
    "lifecycle": "Lifecycle/acme.crm.lead" } }

// packages/acme.crm/entities/Logic/acme.crm.lead.qualify.v1.json
{ "data": { "language": "RHAI", "abi": "v1", "kind": "FUNCTION", "entry": "main",
            "effect": "STAGE", "capabilities": ["entity", "emit", "log"],
            "source_file": "logic/acme.crm.lead.qualify.v1.rhai" } }
```

- Short forms such as `Entity`, `Text` and `Lifecycle/acme.crm.lead` are resolved against the target tenant and normalised by the loader (REQ-PKG-004).
- `source_file` is inlined into `source` at load time.

---

## 3. Behavior

### 3.1 Loading

### REQ-PKG-001 — Loader entry points
- **Statement:** The loader MUST accept a package from:
  - a directory (development);
  - a `.ubpkg` archive (upload through `PUT /blob` and then `package.install`);
  - the kernel's embedded root packages (genesis).

  Installation, upgrade and uninstall run in processes of kind SYSTEM, as the calling principal, who needs `definition.author` and `tenant.admin` in the target tenant (`ubos.root_admin` for ROOT and ONTOLOGY packages). Every loader action is audited.
- **Origin:** VRD-17-01, VRD-17-02, CON-UB-031, CON-LS-024
- **Acceptance:** Installing the same archive twice in a tenant produces one install, and the second run is a reported no-op.
- **Priority:** P0

### REQ-PKG-002 — Manifest and dependency checks
- **Statement:** Before staging anything, the loader MUST check the following:
  1) The manifest is valid against its schema.
  2) The target is allowed (ROOT/ONTOLOGY only in `logrums`).
  3) All `dependencies` are installed in the target tenant or in `logrums`, with satisfying versions. Missing ones fail with `PKG.DEPENDENCY_MISSING` and list what is needed. The loader never installs dependencies implicitly in version 1.
  4) The dependency graph is acyclic.
  5) The package code is a namespace the tenant owns, or a vendor namespace (REQ-TEN-010).
  6) Every entity slug and type code lies inside the package namespace (except in root packages).
- **Origin:** VRD-17-02, VRD-18-01, CON-UB-062
- **Acceptance:** Installing `acme.crm` without the required `ontology >= 1.2` fails, naming the dependency.
- **Priority:** P0

### REQ-PKG-003 — Kernel compatibility
- **Statement:** A package whose `kernel.min_version` is higher than the running kernel, or whose `abi` list contains no ABI the kernel serves (REQ-RT-011), MUST be rejected with `PKG.KERNEL_INCOMPATIBLE`.
- **Origin:** VRD-05-02, CON-UP-029
- **Acceptance:** A package requiring ABI `v2` is refused by a `v1`-only kernel.
- **Priority:** P0

### REQ-PKG-004 — Normalisation and ordering
- **Statement:** The loader MUST:
  1) read all entity files, taking the type and slug from the path;
  2) inline `source_file` contents;
  3) normalise all URIs to canonical form against the target tenant (REQ-URI-008). Unresolvable references to entities outside the package fail with `PKG.REFERENCE_UNRESOLVED`;
  4) compute the canonical content hash of each item's own data (REQ-STO-004);
  5) order the items topologically:
     - meta-level definitions first (Type, then TypeExtension, Dictionary, RelationshipType, Behavior, Lifecycle);
     - then Logic;
     - then Action, Pipeline, Workflow and Query;
     - then View and Widget;
     - then Constraint and Policy;
     - then Roles and data.

     Within each group, items are ordered by their `_extends` and reference edges. A cycle within a group is allowed only for references (not for `_extends`). An `_extends` cycle fails with `META.INHERITANCE_CYCLE`.
- **Rationale:** UB's topological batches and UC/LS's ordered files. One ordering algorithm replaces file-name ordering.
- **Origin:** VRD-17-02, CON-UB-031, CON-UC-033, CON-LS-024
- **Acceptance:** A package whose files are listed in reverse dependency order installs correctly.
- **Priority:** P0

### REQ-PKG-005 — Definitions visible within the loading process
- **Statement:** Within one process, definition entities staged in the session (types, extensions, lifecycles, dictionaries) MUST be used by the type registry and the rule engine for that process. This means data items of a newly defined type in the same package validate against the staged definition. Other processes do not see staged definitions.
- **Origin:** NEW: required for single-process package installs (VRD-17-02 step 4)
- **Acceptance:** A package with a new type and 10 seed instances of it installs in one process.
- **Priority:** P0

### REQ-PKG-010 — Install and upgrade algorithm
- **Statement:** Installing or upgrading package *P* in tenant *t* on target branch *T* (default `main`) MUST run as follows:
  1) Run the checks (REQ-PKG-002/003) and normalisation (REQ-PKG-004).
  2) **Loader branch.** Ensure the branch `pkg/<code>` (kind FEATURE, parent *T*) exists. On first install, it is created with `fork_at` = now.
  3) **Stage on the loader branch** (process 1, kind SYSTEM). For each item:
     - if its hash equals the hash recorded in `Package.items`, skip it;
     - otherwise stage it as CREATE or UPDATE on the loader branch;
     - items of the previous version that are missing from *P* are staged as DELETE.

     Pre-flush validation runs at PUBLISH level. Flush.
  4) **Merge** the loader branch into *T* (process 2, kind MERGE), with the strategy from the request or manifest (REQ-PKG-012).
     - The merge base of each item is the item's previous loader-branch version (REQ-VER-011), so local edits in *T* since the last install are detected.
     - In the same process, write or update the `Package` entity (version, digest, items with loader commits, status INSTALLED, report).
     - Set `entity_instance.package_id` for new entities.
  5) **Migrations.** If upgrading, run each `migrations` pipeline whose `from` range matches the previous version, in order, each in its own process.
  6) **Post-install.** Run `post_install` in its own process.
  7) **Report.** Return the report (REQ-PKG-013).

  If step 3 or 4 fails, the target is unchanged and the Package status becomes FAILED, with the report. A later run resumes idempotently.
- **Rationale:** UB's loader design (hash dirty check, loader branch, publish), combined with SPEC-14's three-way merge. Tenant customisations survive upgrades.
- **Origin:** VRD-17-02, CON-UB-031, VRD-08-04
- **Acceptance:**
  1) Upgrading `acme.crm` 1.0 → 1.1, where a tenant had changed a View's title, keeps the tenant's title and applies the package's new column.
  2) The Package entity shows version 1.1.
- **Priority:** P0 (install), P1 (upgrade merge, migrations)

### REQ-PKG-011 — Idempotency
- **Statement:** Re-running an install of an already installed version with an identical digest MUST NOT write any version, and it reports `UP_TO_DATE`. Re-running after a partial failure MUST complete the remaining steps without duplicating commits.
- **Origin:** VRD-17-02, CON-UC-033 (save only if absent), CON-LS-024
- **Acceptance:** Running `ubos pkg install` three times produces commits only on the first run.
- **Priority:** P0

### REQ-PKG-012 — Conflict strategies
- **Statement:** On upgrade, for items changed both in the package and in the target since the last install:
  - **MERGE** (default): three-way path merge (REQ-VER-012). Remaining conflicts fail the install with `PKG.CONFLICTS` and the conflict list. The operator then chooses resolutions or another strategy.
  - **KEEP_LOCAL**: conflicting items keep the target version, and the package's change is reported as skipped.
  - **OVERWRITE**: conflicting items take the package version (FORCE_SOURCE).

  Items deleted in the target but changed in the package are reported (DELETE_MODIFY) and follow the same strategies.
- **Origin:** VRD-08-04, CON-UB-023
- **Acceptance:** With KEEP_LOCAL, a locally edited Logic stays, and the report lists it.
- **Priority:** P1

### REQ-PKG-013 — Install report
- **Statement:** Every loader run MUST return, and store in `Package.last_report`, a report with:
  - package code, versions from/to, strategy;
  - counts of created, updated, deleted and unchanged items;
  - the merged, skipped and conflicting items with paths;
  - validation errors;
  - migrations and post-install results with process IDs;
  - duration.
- **Origin:** CON-UB-031, CON-FU-025 (merged/skipped/failed lists)
- **Acceptance:** The report of a failed install names every invalid item with its error path.
- **Priority:** P0

### REQ-PKG-020 — Uninstall
- **Statement:** `package.uninstall {code, mode}` MUST, in one MERGE-kind process:
  - stage DELETE for every entity with this `package_id` on the target branch, when mode is REMOVE_DEFINITIONS;
  - refuse with `PKG.IN_USE` if other installed packages depend on it;
  - refuse if ACTIVE instances of the package's entity types exist, unless `force_orphan_data: true`. In that case the instances are kept and reported as orphaned;
  - set the Package status to UNINSTALLED.

  With mode KEEP, only the Package record changes: the definitions stay and become tenant-owned (`package_id := null`).
- **Origin:** CON-UB-062, NEW: complete lifecycle
- **Acceptance:** Uninstalling an app with existing records is refused. With `force_orphan_data`, its records remain readable as their (now deleted) type's last definition version through history.
- **Priority:** P2

### 3.2 Export

### REQ-PKG-030 — Export
- **Statement:** `package.export` MUST write a package tree (directory or `.ubpkg`) from the store. It accepts one of three selections:
  - (a) an installed package: the entities with its `package_id`, in their **current** target-branch versions, including local changes;
  - (b) a namespace: all definitions under it;
  - (c) a data selection: a CriteriaQuery or a Context's heads (UB context export).

  Logic sources are written as `.rhai` files. The manifest is generated or updated with the version bumped as requested. Export is read-only (a process row only if read auditing is on).
- **Rationale:** Round trips between store and files keep Git review possible (VRD-17-04), and UB's "data is application" migration between environments.
- **Origin:** VRD-17-04, CON-UB-046, CON-UP-045
- **Acceptance:** Exporting an installed package and re-installing it into another tenant reproduces equal content hashes.
- **Priority:** P1

### 3.3 Genesis and boot

### REQ-PKG-040 — Root packages
- **Statement:** The kernel binary MUST embed two root packages:
  - `system`: the meta-model (SPEC-13); platform types of all chapters; generic actions (REQ-FLOW-017); kernel queries; Native descriptors; platform relationship types; baseline roles and policies; system Lifecycles; default widgets and views (SPEC-25).
  - `ontology`: the base domain library (SPEC-29).

  Their versions equal the kernel version (for `system`) and the ontology release (for `ontology`). They install into `logrums`, on `main`, with no namespace prefix.
- **Origin:** VRD-17-01, VRD-02-06, CON-UC-018, CON-UB-013
- **Acceptance:** `package.list` in `logrums` shows `system` and `ontology` with versions.
- **Priority:** P0

### REQ-PKG-041 — Genesis
- **Statement:** When `ubos://logrums/Type/Type` does not exist, boot MUST perform genesis:
  1) create tenant `logrums` rows (the Tenant entity for itself, Namespace claims for reserved codes);
  2) install `system` in **bootstrap mode**: meta-schema validation uses the built-in copy (REQ-META-001), and the order puts `Type/Entity` and `Type/Type` first;
  3) create the kernel ServiceAccounts, `Context/default` of `logrums` and the root admin (REQ-SEC-004);
  4) install `ontology`.

  Each package install is its own processes (REQ-PKG-010), with principal `logrums/ServiceAccount/kernel` and app = the package. Genesis is idempotent: if it is interrupted, the next boot resumes it.
- **Origin:** VRD-17-03, CON-UC-033, CON-LS-024, CON-UP-027, CON-UB-031
- **Acceptance:**
  1) On an empty database, boot creates both packages and the admin setup token.
  2) Killing the process mid-genesis and booting again completes it without duplicates.
- **Priority:** P0

### REQ-PKG-042 — Boot strategies
- **Statement:** The host MUST support `UBOS_BOOT_STRATEGY`:

| Strategy | Behaviour | Allowed |
|---|---|---|
| `SAFE` (default) | genesis if missing; otherwise no package changes; warns if the embedded root packages are newer than the installed ones | always |
| `UPGRADE` | as SAFE, then upgrade `system` and `ontology` to the embedded versions (REQ-PKG-010, strategy MERGE) and run pending migrations | always (production upgrades) |
| `RESET` | drop all data of all tenants, then genesis | only with `UBOS_ALLOW_RESET=true` and not on an enterprise database whose tenants other than `logrums` hold data, unless `--force-reset-production` is also given; audited in host logs |
- **Origin:** VRD-17-03, CON-UP-027
- **Acceptance:** RESET without the allow flag refuses to start, with a clear message.
- **Priority:** P0 (SAFE), P1 (UPGRADE), P2 (RESET)

### REQ-PKG-043 — Boot sequence
- **Statement:** On every start (REQ-ARCH-010 step 5), boot MUST run these steps and report the outcome of each in a `BootReport`:
  1) Verify the store schema version (REQ-STO-019).
  2) Synchronise Native descriptors with the registered natives: create new ones and update changed descriptions. Descriptors of removed natives are marked deprecated, never deleted.
  3) Genesis if needed (REQ-PKG-041).
  4) Apply the strategy (REQ-PKG-042).
  5) In the personal edition, ensure tenant `local` exists (REQ-TEN-003, created with the owner user from the first-run wizard of the desktop shell).
  6) Warm the semantic caches for `logrums` types and the most-used tenant types (configurable).
  7) Recover runtime state: re-enqueue QUEUED jobs (ADR-004 sweeper), restore leases, and register schedules (SPEC-20) for leader roles.
  8) Report readiness.

  Boot writes processes of kind BOOT only when it changes data (genesis, upgrades).
- **Origin:** VRD-17-03, CON-UC-033, CON-LS-001
- **Acceptance:** The BootReport lists each step with its duration and outcome, and `/health/ready` turns green only after step 8.
- **Priority:** P0

### 3.4 Development mode

### REQ-PKG-050 — Hot reload from files
- **Statement:** `ubos dev --watch <package dir> --context <authority>` (CLI, SPEC-26) MUST:
  - watch the package files;
  - on each change, re-run the loader for that package against the given context's branch. That branch MUST be a DRAFT or FEATURE branch; `main` of production-kind contexts is refused;
  - use strategy OVERWRITE, because the files are the source during development;
  - report validation errors with file paths and line numbers where possible (`RT.COMPILE_ERROR` from `.rhai` files).

  Changes become visible to clients in that context through the normal subscriptions.
- **Rationale:** LC's hot-reload experience, without letting files become a runtime source of truth.
- **Origin:** VRD-17-04, CON-LC-004
- **Acceptance:** Saving a `.rhai` file updates the Logic on the developer's draft branch within 2 s, and a compile error points at the file and line.
- **Priority:** P1

### 3.5 Scenarios

### REQ-PKG-060 — Package scenarios
- **Statement:** `package.verify {code, context?}` and `ubos pkg verify` MUST run all scenarios of the package (REQ-PROTO-040) against a fresh FEATURE branch of the given context (default: a temporary branch of the tenant's `main`). The branch is deleted afterwards unless `keep: true`. The result lists each scenario and step. A package intended for publication SHOULD ship at least one scenario per Action and Pipeline it defines (VRD-22-02).
- **Rationale:** A feature is delivered as a package of entities *with* scenarios, and it is verified per step (US scenarios, LC data-driven tests, UC staged verification).
- **Origin:** VRD-22-02, VRD-22-03, CON-US-043, CON-LC-061, CON-LS-050
- **Acceptance:** `ubos pkg verify acme.crm` runs its 12 scenarios on a temporary branch and leaves `main` untouched.
- **Priority:** P0

---

## 4. Interfaces

### API-PKG-001 — Loader
- **Kind:** rust trait (`ubos_kernel::packages`)
- **Signature / shape:**
```rust
#[async_trait]
pub trait PackageLoader: Send + Sync {
    async fn read(&self, source: PackageSource) -> Result<LoadedPackage, UbosError>;        // dir | archive | embedded
    async fn plan(&self, x: &ExecutionContext, p: &LoadedPackage, opts: &InstallOptions) -> Result<InstallPlan, UbosError>; // dry run
    async fn install(&self, x: &ExecutionContext, p: &LoadedPackage, opts: &InstallOptions) -> Result<InstallReport, UbosError>;
    async fn uninstall(&self, x: &ExecutionContext, code: &str, opts: &UninstallOptions) -> Result<InstallReport, UbosError>;
    async fn export(&self, x: &ExecutionContext, sel: ExportSelection, out: ExportTarget) -> Result<ExportReport, UbosError>;
}
pub struct InstallOptions { pub target_branch: Option<BranchName>, pub strategy: Option<ConflictStrategy>,
                            pub resolutions: Option<Value>, pub bootstrap: bool, pub run_post_install: bool }
```
- **Origin:** CON-UB-031, CON-LS-024

### API-PKG-002 — Named operations
- **Kind:** UBTP targets
- **Signature / shape:**

| URI | Args | Result |
|---|---|---|
| `ubos://system/Action/package.install@v1` | `{blob: sha256 \| path (local only), target_branch?, strategy?, resolutions?, dry_run?}` | InstallReport or InstallPlan |
| `ubos://system/Action/package.uninstall@v1` | `{code, mode, force_orphan_data?}` | report |
| `ubos://system/Action/package.export@v1` | `{selection, version_bump?}` | blob sha256 of the `.ubpkg` |
| `ubos://system/Action/package.verify@v1` | `{code, context?, keep?}` | scenario results |
| `ubos://system/Query/package.list@v1` | – | installed packages |
| `ubos://system/Query/boot.report@v1` | – | last BootReport (admins) |
- **Origin:** CON-UB-062 (`upm install`)

### API-PKG-003 — CLI verbs
- **Kind:** cli (SPEC-26)
- **Signature / shape:**
```
ubos pkg new <code>                      # scaffold packages/<code>/ with manifest, one type, one action, one scenario
ubos pkg validate <dir>                  # offline: manifest, meta-schema, rhai compile (no server needed)
ubos pkg install <dir|file.ubpkg> [--context A] [--branch B] [--strategy merge|keep-local|overwrite] [--dry-run]
ubos pkg export <code|--namespace ns|--query file> [--out dir|file.ubpkg]
ubos pkg verify <code> [--context A] [--keep]
ubos dev --watch <dir> --context <tenant.context>
ubos boot [--strategy safe|upgrade|reset]   # local/embedded kernel
```
- **Origin:** CON-US-004 (CLI boot/eval), CON-UB-062

### Error codes (PKG)

| Code | Category | Meaning |
|---|---|---|
| `PKG.MANIFEST_INVALID` | INVALID | manifest violates its schema |
| `PKG.DEPENDENCY_MISSING` | INVALID | a dependency is not installed or version unsatisfied |
| `PKG.DEPENDENCY_CYCLE` | INVALID | cyclic dependencies |
| `PKG.KERNEL_INCOMPATIBLE` | INVALID | kernel version or ABI mismatch |
| `PKG.NAMESPACE_VIOLATION` | FORBIDDEN | item outside the package namespace |
| `PKG.REFERENCE_UNRESOLVED` | INVALID | reference to a missing external entity |
| `PKG.CONFLICTS` | CONFLICT | merge conflicts on upgrade (details) |
| `PKG.IN_USE` | CONFLICT | uninstall blocked by dependents or data |
| `PKG.TARGET_FORBIDDEN` | FORBIDDEN | ROOT/ONTOLOGY outside logrums, or dev reload on a protected branch |
| `PKG.RESET_FORBIDDEN` | FORBIDDEN | RESET strategy without the allow flags |

---

## 5. Non-functional

| ID | Requirement | Target |
|---|---|---|
| NFR-PERF-140 | Genesis (system + ontology, ~400 definitions) on PostgreSQL | ≤ 15 s; on SQLite ≤ 5 s |
| NFR-PERF-141 | No-op reinstall of a 1 000-item package | ≤ 2 s (hash comparison only) |
| NFR-OPS-140 | Hot reload latency (file save → visible on draft) | ≤ 2 s |
| NFR-AIR-140 | Packages are complete, text-based feature descriptions | manifests, entity JSON, `.rhai`, scenarios, README; the AI builder emits this format (SPEC-27) |

---

## 6. Acceptance

| REQ | Criterion |
|---|---|
| REQ-PKG-001…005 | Entry points; dependency and compatibility checks; ordering; same-process definitions |
| REQ-PKG-010…013 | Install; upgrade with local edits preserved; idempotent reruns; strategies; reports |
| REQ-PKG-020 | Uninstall guards |
| REQ-PKG-030 | Export/reinstall hash equality |
| REQ-PKG-040…043 | Root packages; resumable genesis; strategies; boot report and readiness |
| REQ-PKG-050 | Hot reload on drafts only |
| REQ-PKG-060 | Scenario verification on a temporary branch |

---

## 7. Implementation notes

- **Reference code:**
  - LS bootloader [LS:crates/ubos_kernel/src/engine/bootloader.rs#L15-L131].
  - UC bootstrap and loader [UC:src/kernel/bootstrap/mod.rs#L8-L25], [UC:src/kernel/bootstrap/loader/].
  - UB import/export [UB:backend/src/main/java/com/ubos/boot/KernelManagerService.java], meta-loader design [UB:docs/元数据加载器.md].
  - LC file loaders [LC:src/main/java/com/logicorum/core/AbstractConfigLoader.java].
  - UP genesis strategies [UP:ubos-server/src/main/java/org/logrum/ubos/server/bootstrap/SystemBootstrapper.java].
- **Genesis data migration:** the corpus genesis files (UC/US `docs/db/genesis_data/01…17`, UB `data/boot/init.json`, LS `genesis_data/01…07`) are the input for the `system` and `ontology` packages. They are normalised to SPEC-02 codes and to the ENT shapes of SPEC-13 and SPEC-19. SPEC-29 gives the mapping table for types.
- **Embedding:** use `include_dir!` for the root packages, so a kernel binary always carries matching definitions.
- **Loader branch naming:** `pkg/acme-crm` for code `acme.crm`, following the branch segment rule (no dots, REQ-CONV-024).

## 8. Open questions

None. Deferred to a later version (VRD-17-05): the remote package registry, package signing, and signed node boot permits.
