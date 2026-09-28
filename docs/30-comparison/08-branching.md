---
id: CMP-08
title: "Comparison: Branching Semantics"
status: complete
phase: P2
depends_on: [CMP-01, CMP-04, CMP-07]
sources: [UP, FU, LC, UB, UC, US, LS, UW]
---

# Comparison: Branching Semantics

## 1. Question

What do branches mean? This covers inheritance and overlay, drafts, merges and conflicts, revert, diff and environments. How should reads and writes behave across branches?

## 2. Candidates

| KEY | Approach | Refs | Summary |
|---|---|---|---|
| UP | Branch tree with recursive read fallback (overlay) | CON-UP-041, 051, HL-UP-002 | Regional/tenant overrides |
| FU | Merge (source-wins deep merge), three-way conflict detection by path, revert = head move, branch from commit, drafts `draft/{user}/{base}` inheriting base, environments, diff/status, blame | CON-FU-025…031, 040, 041, HL-FU-005, 006, 007, 009 | The broadest feature set |
| LC | Branch as per-request staging (`draft`, `main`) | CON-LC-045 | No merge |
| UB | Draft per user → publish (merge FORCE_SOURCE + delete); strategies FAIL_ON_CONFLICT / FORCE_SOURCE / FORCE_TARGET / THREE_WAY_MERGE with ancestor search; revert = new commit; diff = JSON Patch; micro-commits | CON-UB-021…026, HL-UB-006 | The most Git-faithful semantics |
| UC/US/LS | Main-only reads; branch in payload/URI/workspace | CON-UC-045, CON-LS-016 | Structural only |
| UW | Branch shown in context | CON-UW-011 | UX |

## 3. Dimension matrix

| Dimension | UP | FU | UB | UC/US/LS |
|---|---|---|---|---|
| Overlay inheritance (read fallback) | ● | ● (drafts) | – | – |
| Per-user drafts | – | ● | ● | – |
| Merge strategies | – | source-wins | ● 4 strategies | – |
| Conflict granularity | – | ● path-level | top-level field | – |
| Common-ancestor search | – | given base | ● | – |
| Revert semantics | – | head move (reset) | ● new commit | – |
| Diff format | – | status + blame | ● JSON Patch | – |
| Environments / pinning | – | ● | via Context | Workspace branch |

## 4. Analysis

- UP's **overlay** semantics (a child branch stores only overrides) and FU/UB's **Git** semantics (drafts, merge, revert) are complementary.
  - An overlay is right for *customisation*: region, tenant, industry pack.
  - Git branches are right for *change management*: draft, review, publish.
- **Draft inheritance.** FU already combines the two: drafts inherit the base branch.
- **Revert.**
  - UB's revert-as-new-commit preserves history.
  - FU's head move behaves like `reset` and loses reachability.
  - *Interpretation:* a platform with audit by default should use revert-as-commit and allow `reset` only for administrators.
- **Merge.**
  - UB's three-way merge compares top-level fields only.
  - FU's detector works per path and accepts URIs.
  - Together they give strategy selection, ancestor search and path-level conflicts.
- **Undefined in the corpus.** Write and delete semantics under overlay inheritance are not defined: does deleting on a child hide the parent's value? UP notes this gap.
- **The other repos.** The Rust repositories have branch fields but only read `main`; the spec must supply the branch semantics they lack.

## 5. Verdicts

### VRD-08-01 — Branches are entities with a parent and a kind (fusion UP + FU + NEW)
- **Decision:** fusion of UP and FU, plus a NEW `kind` field.
- **Consequences:**
  - A `Branch` entity has `name`, `parent`, `kind` ∈ {overlay, draft, feature, release} and `created_from_commit`.
  - Reads on overlay and draft branches fall back to the parent recursively.
  - Feature branches are Git-like copies from a commit.
- **Dropped:** UP's separate `sys_branch_config` table.

### VRD-08-02 — Overlay write/delete semantics (NEW)
- **Decision:** NEW; the corpus lacks an answer.
- **Consequences:**
  - A write on a child stores the child's own snapshot. It is full, not a delta, per VRD-01-02.
  - A delete on a child writes a *masking tombstone*, which hides the parent's value on that branch only.
  - `reset to inherit` removes the child's head.
  - UI shows inherited vs overridden values (UB Studio, CON-UB-053).

### VRD-08-03 — Drafts per user inheriting the base; publish = merge + cleanup (fusion FU + UB)
- **Decision:** fusion.
- **Consequences:**
  - Draft branch `draft/{user}/{base}`.
  - Drafts may skip FINAL-level validation (CMP-09).
  - Publish = merge (THREE_WAY by default) + delete draft heads, as one process.

### VRD-08-04 — Merge with strategies and path-level conflicts (fusion UB + FU)
- **Decision:** fusion.
- **Consequences:**
  - Strategies: FAIL_ON_CONFLICT, FORCE_SOURCE, FORCE_TARGET, THREE_WAY.
  - Common ancestor by parent-chain walk.
  - Conflicts reported per JSON path as `{base, ours, theirs}`.
  - Any two URIs can be diffed or merged.
  - Diff output is JSON Patch (RFC 6902).

### VRD-08-05 — Revert writes a new commit; reset is an administrative operation (best-of UB)
- **Decision:** best-of UB.
- **Dropped:** FU's head-move revert as the default behaviour.

### VRD-08-06 — Environments are Contexts pointing to a branch or a pinned commit (best-of FU → VRD-07-01)
- **Decision:** best-of FU; implemented through VRD-07-01.
- **Consequences:** Promotion = moving a pointer or merging; freeze = pin.
