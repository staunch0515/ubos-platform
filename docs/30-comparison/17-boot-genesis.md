---
id: CMP-17
title: "Comparison: Boot and Genesis"
status: complete
phase: P2
depends_on: [CMP-02, CMP-04]
sources: [UP, FU, LC, UB, UC, US, LS, UW]
---

# Comparison: Boot and Genesis

## 1. Question

How does an empty system become a working platform? This covers the meta-model bootstrap, the base ontology, kernel functions, default contexts and users, and later updates of packaged metadata. How are the same mechanisms used for installing apps?

## 2. Candidates

| KEY | Approach | Refs | Summary |
|---|---|---|---|
| UP | Genesis through the kernel commit API; strategy SAFE / RESET / UPDATE (UPDATE designed) | CON-UP-027, HL-UP-011 | Seeds as ordinary commits |
| LC | Definitions as files, hot-reloaded (processes, schemas, SQL, relationship types) | CON-LC-004 | Metadata in the repository, not in the store |
| UB | 17-phase genesis protocol; import sorted by priority in one transaction; Meta-Loader design: topological batches over `_extends`, SHA-256 dirty check, loader branch + publish, remote UBOS Store; network boot permit (design) | CON-UB-031, 033, 062, HL-UB-004, 009 | Most complete loader design |
| UC | Idempotent loader: first kernel function, then JSON files (UODS), then genesis workspace; auto `_extends` | CON-UC-033, 018, HL-UC-012 | Minimum viable self-bootstrapping |
| US | UC + 16/17 action files + `ui_modes`; CLI `boot` | CON-US-041, 004 | – |
| LS | Ordered files, idempotent by root existence, one process per file | CON-LS-024, 011 | Meta bootstrap through commit path |

## 3. Dimension matrix

| Dimension | UP | LC | UB | UC/US | LS |
|---|---|---|---|---|---|
| Seeds go through the commit path (audited) | ● | – | ● | ● | ● |
| Dependency ordering | code order | – | ● topological (design) | file order | file order |
| Idempotency / change detection | SAFE check | reload | ● hash (design) | exists check | root check |
| Update of installed metadata | designed | hot reload | ● dirty check | – | – |
| Validation of seed data | – | JSON Schema | – | UODS (design) | – |
| Packages / remote store | – | – | ● design | – | – |
| Reset strategy | ● | – | – | clear.sql | clear.sql |

## 4. Analysis

- **Consensus:** genesis must go through the same commit path as business writes, so the initial world is audited and versioned (UP, UB, UC, LS).
- **Updates.** UB's loader design is the only one that handles *updates* of already-installed metadata: dirty check by hash, a loader branch, publish.
  - Everyone else only seeds an empty system.
  - The same mechanism is what installing and upgrading apps and domain packs needs (CON-UB-062 "apps as entity bundles").
- **LC's file-based definitions** remain valuable for development: definitions are reviewable text in Git with hot reload. *Interpretation:* the store is the truth, files are the packaging format.

## 5. Verdicts

### VRD-17-01 — Packages (bundles) are the unit of installation, genesis is the root package (fusion UB + UC)
- **Decision:** fusion.
- **Consequences:**
  - A `Package` manifest has `code`, `version`, `dependencies`, and entity files (UODS-valid JSON, scripts as line arrays).
  - Genesis = the `system` root package followed by the ontology package.
  - Apps and verticals are packages.

### VRD-17-02 — Loader algorithm (best-of UB design + UC/LS implementation)
- **Decision:** best-of UB design with the UC and LS implementations.
- **Consequences:**
  1. Validate every item against the meta-schema (VRD-02-02).
  2. Order by `_extends` and references (topological).
  3. Compute a content hash per item and skip unchanged items.
  4. Commit to a loader branch as one process per package.
  5. Publish (merge) to the target branch.
  6. Record the installed package version as an entity.
- Idempotent; re-running is safe.

### VRD-17-03 — Boot sequence (fusion UC + LS + UP)
- **Decision:** fusion.
- **Consequences:**
  1. Ensure the kernel native functions and meta root exist.
  2. Install the `system` package if missing.
  3. Ensure the system Context and admin identity.
  4. Load runtime caches: semantic caches keyed by commit, scheduler, subscriptions.
  5. Report readiness.
- Strategy is configuration: `SAFE` (default), `UPGRADE` (apply package updates), `RESET` (development only).

### VRD-17-04 — Files for development, store for truth (adapt LC)
- **Decision:** adapt LC.
- **Consequences:** Package sources live as files in Git, with hot reload in development mode. Installation always commits them; production never reads definitions from files.

### VRD-17-05 — Remote package registry and node boot permits (adapt UB design, later version)
- **Decision:** adapt UB design; deferred.
- **Consequences:** Deferred to spec v2: a central package store, plus signed boot permits for edge nodes (CON-UB-033).
