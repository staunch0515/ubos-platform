---
id: CMP-03
title: "Comparison: Identity and Addressing"
status: complete
phase: P2
depends_on: [CMP-00, CMP-01, CMP-02]
sources: [UP, FU, LC, UB, UC, US, LS, UW]
---

# Comparison: Identity and Addressing

## 1. Question

How is an entity identified, physically and for humans, and how is any version, branch, field or presentation of it addressed uniformly by the kernel, scripts, UI, APIs and AI?

## 2. Candidates

| KEY | Approach | Refs | Summary |
|---|---|---|---|
| UP | UUID + unique (type, slug); dotted slugs | CON-UP-010, 014 | No tenant; `ubos://ctx/logic/…` appears in data only |
| FU | UUID stable, slug mutable; URI `ubos://scope/type/slug?branch&commit&tag&key`; frontend modes | CON-FU-010, 011, 013, 016, HL-FU-008, 012 | Most complete query grammar (commit, tag, key path); rename/copy as commits |
| LC | UUID + (app, type, slug) | CON-LC-016 | App scoping; no URI scheme |
| UB | UUID + (tenant, type, slug); `ubos://tenant[/branch]/type/slug[.intent][;params]`; context slug as authority | CON-UB-011, 017, 018, HL-UB-001, 002 | Context-addressed; presentation intent; aliases (design) |
| UC/US | UUID + locator; `ubos://tenant/type/slug?commit=` | CON-UC-010, 016, 066; CON-US-051 | URI as universal argument; widgets addressed by URI |
| LS | Identity relation; `ubos://tenant/Type/slug?commit|tag|branch|time` | CON-LS-016, HL-LS-009 | Version modes incl. time |
| UW | Context with tenant + branch (UI) | CON-UW-011 | UX only |

## 3. Dimension matrix

| Dimension | UP | FU | LC | UB | UC/US | LS |
|---|---|---|---|---|---|---|
| Physical UUID separate from business key | ● | ● | ● | ● | ● | ● |
| Tenant in key | – | ● | app | ● | ● | ● (fixed) |
| Rename without identity change | – | ● | – | – | – | – |
| URI scheme implemented | – | ● | – | ● | ● | ● |
| Version by commit | – | ● | – | design `@commit` | ● | ● |
| Tag / time / branch in URI | – | tag, branch | – | branch segment | – | ● all |
| Field path (`key`) | – | ● | – | – | – | – |
| Presentation intent | – | – | – | ● `.summary/.json` | – | – |
| Context indirection (slug → tenant+branch) | – | environments | – | ● | Workspace | – |
| Hierarchical namespaces | ● dotted | ● tree | – | ● prefixes | – | – |

## 4. Analysis

- **Agreement (fact).** Every backend separates a physical UUID from a business locator. Six use `(tenant, type, slug)` with uniqueness (VRD-01-01), and four implement the `ubos://` scheme.
- **Where grammars differ.**
  - Branch placement: UB puts the branch in the path; FU and LS put it in the query.
  - Version selectors: FU adds `tag` and `key`, LS adds `time`, UB adds a presentation intent and matrix parameters.
  - The authority segment: FU calls it a tenant or app "scope"; UB calls it a context slug that resolves to tenant + branch.
  - *Interpretation:* query parameters compose better than path segments (no ambiguity between branch and type), and a context authority gives indirection for environments.
- **Rename.** Only FU separates stable identity from a mutable slug and records rename and copy as commits (HL-FU-012). Without this, a slug is effectively immutable.
- **URI as universal argument.** UC/US show URIs used by imports, `_extends`, Cron/Hook targets, widgets, the resource browser and cross-tenant references (CON-UC-066). This needs one grammar with a single parser in every runtime (server, scripts, client).

## 5. Verdicts

### VRD-03-01 — Identity = immutable UUID + business locator (tenant, type, slug) (fusion UB + FU)
- **Decision:** fusion.
- **Consequences:**
  - UUID is never reused.
  - The locator is unique among ACTIVE entities.
  - Rename and copy are commits with action `RENAME` or `COPY` that update the registry (FU).
  - Slugs use dotted namespaces (UP/FU) and match a fixed regex.

### VRD-03-02 — Canonical URI grammar (fusion FU + LS + UB)
- **Decision:** fusion.
- **Grammar:** `ubos://{authority}/{Type}/{slug}[?branch=&commit=&tag=&time=&key=&view=]`
  - `authority` is a tenant code or a context slug (VRD-03-03).
  - `Type` is the entity type code.
  - `branch`, `commit`, `tag` and `time` are mutually constrained version selectors; `commit` wins.
  - `key` is a dot/array path into the snapshot (FU).
  - `view` is a presentation intent (`summary | detail | json | schema | config`), from UB's suffix.
- **Dropped:** branch as a path segment (UB); `cid=` alias (FU frontend); triple-slash form (LS) except as a tolerated input.

### VRD-03-03 — Context authority: a context slug resolves to tenant + branch (best-of UB)
- **Decision:** best-of UB (CON-UB-017), aligned with UC's Workspace (CMP-07).
- **Consequences:**
  - The resolver first looks up a Context entity with that slug in the system tenant.
  - If none is found, the authority is treated as a tenant code on the default branch.

### VRD-03-04 — Short forms and aliases, normalised at the edge (fusion UB design + FU frontend modes)
- **Decision:** fusion.
- **Consequences:**
  - Relative forms (`Type/slug` within the current context, `self`, `@draft`) and system aliases are allowed in UI and scripts.
  - They are always normalised to the canonical form before storage or audit.

### VRD-03-05 — One URI library per runtime, contract-tested (NEW)
- **Decision:** NEW.
- **Rationale:** Four divergent parsers exist in the corpus.
- **Consequences:**
  - The spec includes a URI conformance table: input → normalised form or error.
  - Server, script host and client libraries must pass it.
