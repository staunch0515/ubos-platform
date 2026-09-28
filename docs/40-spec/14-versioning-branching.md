---
id: SPEC-14
title: "Versioning and Branching"
status: complete
phase: P4
depends_on: [SPEC-02, SPEC-11, SPEC-12, SPEC-13]
sources: [UP, FU, LC, UB, UC]
---

# Versioning and Branching

## 0. Chapter header

- **Scope:** This chapter defines:
  - what a commit means;
  - Branch and Tag entities, and the branch kinds with their read-fallback semantics;
  - writing and deleting on inheriting branches (overlay rules), drafts and publishing;
  - merge strategies and conflicts;
  - revert, process undo and administrative reset;
  - diff, branch status and point-in-time reads.

  It uses the store primitives of SPEC-11. It is used by transactions (SPEC-15), context (SPEC-16), queries (SPEC-21) and tenancy (SPEC-23).
- **MOD:** `VER`
- **depends_on:** SPEC-02, SPEC-11, SPEC-12, SPEC-13.
- **Terms used:** TERM-Commit, TERM-Version, TERM-Head, TERM-Height, TERM-Cas, TERM-Branch, TERM-Overlay, TERM-Draft, TERM-Merge, TERM-Revert, TERM-Reset, TERM-Diff, TERM-Tag, TERM-Pin, TERM-Tombstone, TERM-OwnData.
- **Origin summary:**
  - Verdicts: VRD-08-01…06, VRD-01-03, VRD-01-04.
  - UP: branch hierarchy with recursive fallback (CON-UP-041).
  - FU: branch creation, drafts, environments, merge, three-way conflicts, branch diff (CON-FU-025…031, CON-FU-040).
  - UB: drafts and publish, merge strategies, revert as a commit, JSON Patch diff, micro-commits (CON-UB-022…026).
  - LC: branch as staging (CON-LC-045).
  - UC: branches structural only (CON-UC-045).

---

## 1. Concepts

### 1.1 Branch kinds

| Kind | Parent | Fallback reads | Typical use | Origin |
|---|---|---|---|---|
| `MAIN` | none | – | the tenant's truth | all |
| `OVERLAY` | any branch | **live**: parent's current head | customisation layers (region, industry pack) | CON-UP-041 |
| `DRAFT` | any branch (the base) | **live** | per-principal work in progress, `draft/<principal-slug>/<base>` | CON-FU-030, CON-UB-022 |
| `FEATURE` | any branch | **frozen** at `fork_at`: parent as it was when the branch was created | isolated change sets merged later | CON-FU-028, Git |
| `RELEASE` | any branch | **frozen** at `fork_at` | stabilisation, hot fixes, `release/<name>` | Git |

- A branch holds only the entities committed on it. Everything else is read through the parent chain.
- Branches are therefore cheap (copy-on-write by inheritance, CON-FU-028).

### 1.2 Reading an entity on a branch

```
resolve(e, b, t = now):
  if b has a version of e with committed_at <= t:
      v = the latest such version on b
      if v.action == DELETE -> NOT FOUND (masking tombstone)
      if v.action == UNSET  -> continue to parent (inherit)
      else                  -> v
  if b.parent is none       -> NOT FOUND
  t' = t                     if b.kind in {OVERLAY, DRAFT}
     = min(t, b.fork_at)     if b.kind in {FEATURE, RELEASE}
  return resolve(e, b.parent, t')
```

- For `t = now`, the first test uses `branch_head` (one row per branch) instead of the history.
- The whole chain is fetched in one round trip with `heads_for` (API-STO-006).

### 1.3 Commits are per branch, and IDs are global

- Every commit writes a new `entity_version` row on exactly one branch. Merging copies a result as a new commit on the target (`action = MERGE`, `merge_parent_commit_id` = source head).
- Histories per (entity, branch) are therefore linear. The version graph across branches is a DAG through `parent_commit_id` and `merge_parent_commit_id`.

---

## 2. Model

```yaml
ENT-Branch:
  purpose: A line of versions in a tenant (TERM-Branch); an entity of type Branch on the tenant's main branch.
  origin: [VRD-08-01, CON-UP-041, CON-FU-028]
  fields:
    - name: name
      type: branch_name
      required: true
      description: Branch name (REQ-CONV-024); slug = name with "/" replaced by ".".
    - name: kind
      type: enum{MAIN|OVERLAY|DRAFT|FEATURE|RELEASE}
      required: true
      description: Branch kind (§1.1).
    - name: parent
      type: branch_name?
      required: false
      description: Parent branch; required except for MAIN.
    - name: fork_at
      type: timestamp
      required: true
      description: Creation point; FEATURE and RELEASE read their parent as of this time.
    - name: owner_id
      type: uuid?
      required: false
      description: Principal owning a DRAFT branch; null otherwise.
    - name: write_mode
      type: enum{DIRECT|MERGE_ONLY|FROZEN}
      required: true
      description: DIRECT = any authorised write; MERGE_ONLY = only merges and admin processes; FROZEN = no writes. Default DIRECT, MAIN in production tenants usually MERGE_ONLY.
    - name: auto_delete_on_publish
      type: bool?
      required: false
      description: DRAFT - delete the branch after publish. Default true.
    - name: description
      type: text?
      required: false
      description: Purpose.
  invariants:
    - Each tenant has exactly one MAIN branch, named main.
    - The parent chain is acyclic, its length is at most 8, and every parent exists and is not DELETED.
    - kind, parent and fork_at never change after creation.
    - Branch entities themselves are committed only on main.
```

```yaml
ENT-Tag:
  purpose: Named immutable marker of a commit or of a branch point (TERM-Tag).
  origin: [VRD-03-02, CON-FU-010, CON-LS-016]
  fields:
    - name: kind
      type: enum{COMMIT|POINT}
      required: true
      description: COMMIT marks one version of one entity; POINT marks a branch at a time (a release).
    - name: commit_id
      type: commit_id?
      required: false
      description: COMMIT only.
    - name: entity
      type: uri?
      required: false
      description: COMMIT only; the entity (canonical, without selectors).
    - name: branch
      type: branch_name?
      required: false
      description: POINT only.
    - name: at
      type: timestamp?
      required: false
      description: POINT only; the point in time.
    - name: message
      type: text?
      required: false
      description: Release notes.
  invariants:
    - A Tag is created once and never updated (UPDATE fails with VER.TAG_IMMUTABLE); it may be deleted.
    - A POINT tag's at is not later than its creation time.
```

```yaml
ENT-MergeRequest:
  purpose: Input of a merge (value type of the protocol and syscalls; not stored unless used by an ApprovalRequest).
  origin: [VRD-08-04, CON-UB-023, CON-FU-025]
  fields:
    - {name: source, type: branch_name, required: true, description: "Source branch"}
    - {name: target, type: branch_name, required: true, description: "Target branch"}
    - {name: strategy, type: "enum{FAIL_ON_CONFLICT|FORCE_SOURCE|FORCE_TARGET|THREE_WAY}", required: true, description: "Merge strategy (REQ-VER-010)"}
    - {name: entities, type: "list<uri>?", required: false, description: "Restrict to these entities; default all entities with own versions on source"}
    - {name: resolutions, type: "map<string, json>?", required: false, description: "Conflict resolutions keyed '<entity_id>:<json-pointer>'"}
    - {name: dry_run, type: bool?, required: false, description: "Compute the result without committing"}
    - {name: message, type: text?, required: false, description: "Commit message"}
```

```yaml
ENT-MergeResult:
  purpose: Output of a merge or merge preview.
  origin: [CON-UB-023, CON-FU-025, CON-FU-026]
  fields:
    - {name: process_id, type: uuid?, required: false, description: "Merge process (absent for dry run)"}
    - {name: merged, type: "list<{entity: uri, commit_id: commit_id?, mode: FAST_FORWARD|THREE_WAY|FORCED|DELETE|RENAME}>", required: true, description: "Entities merged"}
    - {name: skipped, type: "list<{entity: uri, reason: UP_TO_DATE|TARGET_AHEAD|FORCE_TARGET|UNSET}>", required: true, description: "Entities left as they were"}
    - {name: conflicts, type: "list<Conflict>", required: true, description: "Unresolved conflicts (merge not applied when non-empty)"}
```

```yaml
ENT-Conflict:
  purpose: One path-level merge conflict.
  origin: [CON-FU-026, VRD-08-04]
  fields:
    - {name: entity, type: uri, required: true, description: "Entity concerned"}
    - {name: path, type: string, required: true, description: "JSON Pointer (RFC 6901) into own data; '' for whole-entity conflicts"}
    - {name: kind, type: "enum{BOTH_CHANGED|DELETE_MODIFY|MODIFY_DELETE|RENAME_RENAME|CREATE_CREATE}", required: true, description: "Conflict kind"}
    - {name: base, type: json?, required: false, description: "Value in the common ancestor"}
    - {name: ours, type: json?, required: false, description: "Value on the target"}
    - {name: theirs, type: json?, required: false, description: "Value on the source"}
```

```yaml
ENT-BranchStatusItem:
  purpose: One row of a branch comparison by heads (CON-FU-040).
  origin: [CON-FU-040]
  fields:
    - {name: entity, type: uri, required: true, description: "Entity"}
    - {name: status, type: "enum{NEW|MODIFIED|DELETED|UNSET|RENAMED}", required: true, description: "Change on the branch relative to its parent"}
    - {name: branch_commit, type: commit_id?, required: false, description: "Own head on the branch"}
    - {name: parent_commit, type: commit_id?, required: false, description: "Resolved version on the parent"}
    - {name: parent_moved, type: bool, required: true, description: "The parent changed the entity since the branch's base version (merge would be three-way)"}
```

---

## 3. Behavior

### 3.1 Commits

### REQ-VER-001 — Commit semantics
- **Statement:** A commit MUST record one new version of one entity on one branch (API-STO-003), with:
  - `parent_commit_id`: the branch head at staging time. For the first commit of the entity on this branch, it is the version the branch **resolved** at staging time (the fork base), or null if none.
  - `action`: CREATE (no version resolvable), UPDATE, DELETE, RENAME, COPY (new entity), MERGE, REVERT or UNSET.
  - `type_commit_id`: the version of the type definition used for validation (SPEC-13 REQ-META-023).
  - `author_id`: the process principal. It is always taken from the execution context and never from the request (CON-UB-026).

  Every commit belongs to exactly one process (REQ-ARCH-003).
- **Rationale:** Setting `parent_commit_id` to the resolved base on first write makes common-ancestor search work for drafts and overlays (REQ-VER-011).
- **Origin:** VRD-01-03, CON-UP-044, CON-UB-021, CON-UB-026
- **Acceptance:** The first draft commit of an entity that exists on main has `parent_commit_id` = main's head at that time.
- **Priority:** P0

### REQ-VER-002 — Branch lifecycle
- **Statement:** The branch lifecycle MUST be as follows:
  - **Creation.** Branches are created by committing a `Branch` entity on main, directly or through `branch.create` (API-VER-001). The kernel sets `fork_at` to the flush time.
  - **MAIN.** The MAIN branch of a tenant is created with the tenant (SPEC-23).
  - **DRAFT.** A DRAFT branch is created automatically on the first write of a principal to `draft/<principal-slug>/<base>`, in the same process.
  - **Deletion.** Deleting a branch is a DELETE commit of its Branch entity. After it, reads on the branch fail with `VER.BRANCH_NOT_FOUND`. Its versions stay in history, and its `branch_head` rows are removed in the same flush.
  - **Constraints.** A branch that is the parent of a non-deleted branch cannot be deleted (`VER.BRANCH_HAS_CHILDREN`). MAIN cannot be deleted.
- **Origin:** VRD-08-01, VRD-08-03, CON-FU-028, CON-FU-030, CON-UB-022
- **Acceptance:**
  1) The first draft write creates the Branch entity and the version in one process.
  2) Deleting a branch with children fails.
  3) After deletion, historical reads with `commit=` still work.
- **Priority:** P0

### REQ-VER-003 — Read fallback
- **Statement:** Reading entity *e* on branch *b* at time *t* (default now) MUST follow the algorithm of §1.2.
  - Tenant layering (SPEC-23) wraps this algorithm: the tenant's chain is tried first, then the root tenant's `main`.
  - The result carries the branch where the version was found (`found_on`).
  - A DELETE version found first means "not found". It never falls through to the parent (masking tombstone, VRD-08-02).
- **Origin:** VRD-08-01, VRD-08-02, CON-UP-041
- **Acceptance:**
  1) An OVERLAY sees parent updates made after its creation.
  2) A FEATURE branch does not.
  3) A DELETE on a DRAFT hides the base version on the draft only.
- **Priority:** P0

### REQ-VER-004 — Writes on inheriting branches
- **Statement:** A write of *e* on a non-MAIN branch MUST store the entity's **full own data** (not a delta) as a version on that branch (VRD-08-02, VRD-01-02). Three special operations exist:
  - **Delete on a child branch:** a DELETE version on that branch (a masking tombstone). The parent is unchanged.
  - **Reset to inherit:** an UNSET version on that branch, and the head row is removed. After it, reads fall back to the parent again. Resetting when the branch has no own version is a no-op.
  - **Create on a child branch** of an entity that does not exist on the parent: a CREATE version on the child. The registry row is created (REQ-STO-002), and the entity is visible only through branches that reach the child.
- **Origin:** VRD-08-02, CON-UB-010 (ghost entities)
- **Acceptance:**
  1) Reset to inherit after an overlay edit shows the parent's value again.
  2) Historical reads before the reset still show the overlay's value.
- **Priority:** P0

### REQ-VER-005 — Write modes
- **Statement:** The kernel MUST enforce `Branch.write_mode`:
  - MERGE_ONLY branches accept only commits from merge, revert and ADMIN processes.
  - FROZEN branches accept none.
  - Violations fail with `VER.BRANCH_WRITE_DENIED`.

  SPEC-22 policies may add further restrictions per branch.
- **Origin:** NEW: protected branches for governed publishing (FU approvals, VRD-10-04)
- **Acceptance:** A direct Mutate on a MERGE_ONLY main fails. A publish (merge) succeeds.
- **Priority:** P1

### REQ-VER-006 — Points and pins
- **Statement:** A **point** is (branch, timestamp). Resolving a URI or Context pinned to a point MUST use §1.2 with `t` = the point's timestamp.
  - A POINT Tag names a point.
  - A Context MAY be pinned to a point or a Tag (SPEC-16). This is how environments freeze a release (VRD-08-06).
  - A pin to a single commit (`?commit=`) addresses exactly that version of one entity.
- **Rationale:**
  - FU pinned environments to a commit ID. A commit ID pins one entity only, while an environment needs a consistent view of all entities.
  - Commit IDs are not strictly ordered by commit time (REQ-STO-013), so the point is a timestamp.
- **Origin:** VRD-08-06, CON-FU-031, CON-LS-016
- **Acceptance:** A Context pinned to tag `release-2026.09` keeps returning the same versions after later commits on the release branch.
- **Priority:** P0

### REQ-VER-007 — Tags
- **Statement:**
  - COMMIT tags are created with an existing commit.
  - POINT tags are created with a branch and a time that is not in the future (default: now).
  - Tags are immutable; deleting them is allowed.
  - A URI `tag=` selector resolves per REQ-URI-009 step 6.
- **Origin:** CON-FU-010, CON-LS-016
- **Acceptance:** Updating a Tag fails with `VER.TAG_IMMUTABLE`.
- **Priority:** P1

### 3.2 Merge

### REQ-VER-010 — Merge algorithm
- **Statement:** Merging source branch *S* into target *T* MUST run as one process of kind MERGE, as follows:
  1) **Select the entities**: those with own versions on *S*, meaning a head on *S*, or an UNSET since the merge base. The request may restrict the set with `entities`.
  2) **Classify each entity *e*:**
     - *s* = own head of *e* on *S* (or UNSET);
     - *t* = the version of *e* resolved on *T* now;
     - *b* = the common ancestor of *s* and *t* (REQ-VER-011).
  3) **Decide per case:**

| Case | Result |
|---|---|
| *s* is UNSET | skip (`UNSET`) |
| *s* = *t*, or snapshot hashes equal | skip (`UP_TO_DATE`) |
| *t* = *b* (target unchanged since base), or *t* absent and *b* absent | **fast-forward**: new commit on *T* with *s*'s own data, `action = MERGE` (or DELETE if *s* is DELETE, RENAME details carried), `merge_parent = s` |
| *s* = *b* (source unchanged) | skip (`TARGET_AHEAD`) |
| both changed, strategy FAIL_ON_CONFLICT | conflict `BOTH_CHANGED` at path `''` |
| both changed, FORCE_SOURCE | commit *s*'s own data on *T* (`FORCED`) |
| both changed, FORCE_TARGET | skip (`FORCE_TARGET`) |
| both changed, THREE_WAY | path-level merge of own data (REQ-VER-012); conflicts recorded per path |
| delete on one side, modify on the other (THREE_WAY) | conflict `DELETE_MODIFY` / `MODIFY_DELETE` |
| both created independently (no *b*) with the same locator | conflict `CREATE_CREATE` (FORCE_* strategies choose a side) |

  4) **Apply resolutions.** Values from `resolutions` replace conflicting paths. A resolution for a whole-entity conflict takes `"ours"`, `"theirs"` or a full own-data object.
  5) **Stop on conflicts.** If any conflict remains, nothing is committed. The result lists the conflicts, with error `VER.MERGE_CONFLICT` (category CONFLICT).
  6) **Validate** all merged entities together at the target's validation level (SPEC-18: PUBLISH for MAIN and RELEASE), including cross-entity constraints.
  7) **Flush** all merge commits atomically, with CAS on the target heads read in step 2.
- **Rationale:** UB's strategies and ancestor walk, combined with FU's path-level detection. Atomicity makes publishing all-or-nothing.
- **Origin:** VRD-08-04, CON-UB-023, CON-FU-025, CON-FU-026
- **Acceptance:**
  1) A fast-forward of 3 entities produces 3 MERGE commits in one process.
  2) A both-changed entity with disjoint paths merges cleanly under THREE_WAY and conflicts under FAIL_ON_CONFLICT.
  3) A concurrent target change between steps 2 and 7 fails with `VER.HEAD_CONFLICT`.
- **Priority:** P1 (P0 for fast-forward publish of drafts)

### REQ-VER-011 — Common ancestor
- **Statement:** The common ancestor of versions *s* and *t* MUST be found by a bidirectional breadth-first walk over `parent_commit_id` and `merge_parent_commit_id`, restricted to the entity, up to 10 000 steps. The ancestor is the first commit reached from both sides.
  - If none is found, *b* is absent.
  - If the limit is exceeded, the merge fails with `VER.ANCESTOR_LIMIT`.
- **Origin:** CON-UB-023 (max depth 10 000), VRD-08-04
- **Acceptance:** After a draft edit and a main edit of the same entity, the ancestor is the main version the draft started from (REQ-VER-001).
- **Priority:** P1

### REQ-VER-012 — Path-level three-way merge
- **Statement:** THREE_WAY merge of own data (*b*, *t*, *s*) MUST recurse over the union of keys of objects:
  - For each path, compare *t* with *b* (ours changed?) and *s* with *b* (theirs changed?).
  - If only one side changed, take it. If both changed to equal values, take it.
  - If both changed differently, record a `BOTH_CHANGED` conflict at the JSON Pointer.
  - Arrays are atomic values, unless the property has `merge_key` (SPEC-13 REQ-META-021). Then elements are matched by key and merged recursively. Insertions from both sides are kept in target order, then source order.
  - `_extends` is compared as an atomic list.
- **Origin:** CON-FU-026, VRD-08-04
- **Acceptance:** base `{a:1,b:1}`, ours `{a:2,b:1}`, theirs `{a:1,b:3}` merge to `{a:2,b:3}`. With theirs `{a:3}`, the result is a conflict at `/a`.
- **Priority:** P1

### REQ-VER-013 — Publishing a draft
- **Statement:** `draft.publish(base)` MUST, in one process:
  1) merge `draft/<principal>/<base>` into *base* with THREE_WAY (or the requested strategy);
  2) on success, remove the draft's heads;
  3) if `auto_delete_on_publish`, delete the draft Branch entity.

  `draft.discard(entities?)` MUST write UNSET for the given entities (default: all) on the draft branch.
  - Drafts are validated at DRAFT level when written (SPEC-18) and at the target's level when published.
  - If *base* is MERGE_ONLY with an approval policy, publish creates an ApprovalRequest instead (SPEC-22, VRD-10-04).
- **Origin:** VRD-08-03, CON-UB-022, CON-FU-030
- **Acceptance:**
  1) Publish of two drafted entities commits both to main and leaves no draft heads.
  2) Discard makes main's values visible to the draft owner again.
- **Priority:** P0

### 3.3 Revert, undo, reset

### REQ-VER-020 — Revert an entity
- **Statement:** `version.revert(uri, to_commit)` MUST write a new commit on the branch of *uri*:
  - its own data equals the own data of `to_commit`, which must be a version of the same entity on any branch;
  - `action = REVERT`, `details = {reverted_to}`;
  - its parent is the current head (with CAS).

  Reverting to a DELETE version deletes; reverting a deleted entity to a live version restores it (REQ-STO-002).
- **Origin:** VRD-08-05, CON-UB-024
- **Acceptance:** After revert, the history shows all earlier versions plus the REVERT commit.
- **Priority:** P0

### REQ-VER-021 — Undo a process
- **Statement:** `version.undo_process(process_id, force?)` MUST revert every OUTPUT commit *c* of the process, in one new process:
  - For each entity, the target is `c.parent_commit_id`, or a DELETE if *c* was a CREATE.
  - If the entity's head on that branch is no longer *c*, the undo fails with `VER.UNDO_CONFLICT` for that entity, unless `force` is set. With `force`, it applies a THREE_WAY inverse: the undo is merged over the later changes, and conflicts are returned.

  The undo process records `DEPENDS_ON_DATA` on the undone commits and has `parent_process_id` = the undone process.
- **Rationale:** The process is the unit of business change (VRD-04-01), so it is also the unit of undo.
- **Origin:** NEW: consequence of VRD-04-01 and VRD-08-05
- **Acceptance:** Undoing an invoice-posting process restores all touched entities in one step.
- **Priority:** P1

### REQ-VER-022 — Administrative reset
- **Statement:** `version.reset(uri, to_commit)` MUST move the head to an earlier commit **of the same branch** without writing a version (API-STO-004 `force_set`). It is allowed only for principals with the `ubos.admin.reset` permission (SPEC-22). It runs as an ADMIN process whose audit record lists the old and new heads. Commits after `to_commit` stay in history but become unreachable from the head.
- **Origin:** VRD-08-05, CON-FU-027
- **Acceptance:** A non-admin reset fails with FORBIDDEN. An admin reset is audited with both heads.
- **Priority:** P2

### 3.4 Diff and status

### REQ-VER-030 — Diff
- **Statement:** `version.diff(a, b, mode)` MUST accept any two URIs (REQ-URI-017) and return RFC 6902 JSON Patch operations that transform *a* into *b*.
  - `mode = own` (default) diffs own data.
  - `mode = effective` diffs effective data.
  - Paths use JSON Pointer. Arrays are diffed element-wise by `merge_key` if declared, else by index.
  - Diffs of different entities are allowed (compare two templates).
- **Origin:** VRD-08-04, CON-UB-025, CON-FU-026
- **Acceptance:** `diff(uri?commit=40, uri?commit=41)` returns the operations of that commit. Applying them to 40 yields 41.
- **Priority:** P1

### REQ-VER-031 — Branch status
- **Statement:** `branch.status(branch)` MUST return, from heads only (no snapshot reads), one `BranchStatusItem` for every entity with own versions on the branch. The statuses are:
  - NEW: not resolvable on the parent at the branch's base;
  - MODIFIED;
  - DELETED;
  - UNSET;
  - RENAMED.

  `parent_moved` is true when the parent's resolved version differs from the item's base version.
- **Origin:** CON-FU-040, CON-FU-053
- **Acceptance:** For a draft with one new, one modified and one deleted entity, the result has three items with the correct statuses, computed with no `entity_version` snapshot reads.
- **Priority:** P1

### REQ-VER-032 — History
- **Statement:** `version.history(uri, range, page)` MUST return the versions of the entity on the branch, newest first. Each version carries `{commit_id, action, parent, merge_parent, author, committed_at, message, process_id}` (from the OUTPUT lineage row). Optionally, it follows fallback to parent branches (`include_inherited`) and marks each version's `branch`.
- **Origin:** CON-UC-042, CON-FU-051, VRD-12-03
- **Acceptance:** The history of an overlay-edited entity with `include_inherited` shows the parent's versions before the overlay's first commit.
- **Priority:** P0

### 3.5 Micro-commits

### REQ-VER-040 — Autosave on drafts
- **Statement:** Clients MAY autosave edits as ordinary commits on the principal's draft branch. The kernel MUST accept autosave commits at DRAFT validation level, with CAS. Squashing autosave commits is deferred (§8).
- **Origin:** CON-UB-026, CON-UB-047 (design)
- **Acceptance:** Ten autosaves create ten versions on the draft. Publishing creates one MERGE commit on main.
- **Priority:** P1

---

## 4. Interfaces

The operations are exposed as syscalls (SPEC-17, namespace `branch.*` / `version.*`) and as
named UBTP operations (SPEC-24). The Rust service trait:

### API-VER-001 — VersionService
- **Kind:** rust trait (`ubos_kernel::version`)
- **Signature / shape:**
```rust
#[async_trait]
pub trait VersionService: Send + Sync {
    // branches (run inside the caller's session; staged, flushed by the process)
    async fn create_branch(&self, s: &mut Session, name: &BranchName, kind: BranchKind,
                           parent: Option<&BranchName>, write_mode: WriteMode) -> Result<(), UbosError>;
    async fn delete_branch(&self, s: &mut Session, name: &BranchName) -> Result<(), UbosError>;
    async fn branch(&self, x: &ExecutionContext, name: &BranchName) -> Result<BranchInfo, UbosError>;
    async fn chain(&self, x: &ExecutionContext, name: &BranchName) -> Result<Vec<BranchInfo>, UbosError>;
    // reads
    async fn resolve(&self, x: &ExecutionContext, entity: Uuid, branch: &BranchName,
                     at: Option<Timestamp>) -> Result<Option<Resolved>, UbosError>; // §1.2
    async fn history(&self, x: &ExecutionContext, uri: &str, q: HistoryQuery) -> Result<Page<VersionMeta>, UbosError>;
    async fn diff(&self, x: &ExecutionContext, a: &str, b: &str, mode: DiffMode) -> Result<Vec<PatchOp>, UbosError>;
    async fn status(&self, x: &ExecutionContext, branch: &BranchName) -> Result<Vec<BranchStatusItem>, UbosError>;
    // changes (staged into the session)
    async fn merge(&self, s: &mut Session, req: &MergeRequest) -> Result<MergeResult, UbosError>;
    async fn publish_draft(&self, s: &mut Session, base: &BranchName, strategy: MergeStrategy) -> Result<MergeResult, UbosError>;
    async fn discard_draft(&self, s: &mut Session, base: &BranchName, entities: Option<&[String]>) -> Result<(), UbosError>;
    async fn revert(&self, s: &mut Session, uri: &str, to: CommitId) -> Result<(), UbosError>;
    async fn unset(&self, s: &mut Session, uri: &str) -> Result<(), UbosError>;      // reset to inherit
    async fn undo_process(&self, s: &mut Session, process: Uuid, force: bool) -> Result<MergeResult, UbosError>;
    async fn reset(&self, s: &mut Session, uri: &str, to: CommitId) -> Result<(), UbosError>; // ADMIN
    async fn tag(&self, s: &mut Session, name: &Slug, target: TagTarget, message: Option<&str>) -> Result<(), UbosError>;
}
pub struct Resolved { pub commit_id: CommitId, pub found_on: BranchName, pub action: CommitAction, pub seq_num: Option<i64> }
```
- **Semantics:** Mutating methods stage change sets in the session. The single flush (SPEC-15) writes them. Merge-type methods mark the process kind MERGE.
- **Origin:** CON-UB-023, CON-FU-025, VRD-08-*

### API-VER-002 — Named operations (UBTP)
- **Kind:** UBTP Emit targets (SPEC-24)
- **Signature / shape:**

| URI | Args | Result |
|---|---|---|
| `ubos://system/Action/branch.create@v1` | `{name, kind, parent?, write_mode?}` | Branch URI |
| `ubos://system/Action/branch.delete@v1` | `{name}` | – |
| `ubos://system/Action/branch.merge@v1` | `MergeRequest` | `MergeResult` |
| `ubos://system/Action/draft.publish@v1` | `{base, strategy?}` | `MergeResult` or ApprovalRequest URI |
| `ubos://system/Action/draft.discard@v1` | `{base, entities?}` | – |
| `ubos://system/Action/version.revert@v1` | `{uri, to_commit}` | commit |
| `ubos://system/Action/version.unset@v1` | `{uri}` | – |
| `ubos://system/Action/version.undo_process@v1` | `{process_id, force?}` | `MergeResult` |
| `ubos://system/Action/version.reset@v1` | `{uri, to_commit}` | – |
| `ubos://system/Action/tag.create@v1` | `{name, kind, commit?/entity? or branch?/at?, message?}` | Tag URI |
| `ubos://system/Query/version.history@v1` | `{uri, from?, to?, include_inherited?, page}` | versions |
| `ubos://system/Query/version.diff@v1` | `{a, b, mode?}` | JSON Patch |
| `ubos://system/Query/branch.status@v1` | `{branch}` | status items |
- **Origin:** VRD-15-03

### Error codes (VER)

| Code | Category | Meaning |
|---|---|---|
| `VER.HEAD_CONFLICT` | CONFLICT | CAS failure; `details {expected, actual, seq}` |
| `VER.BRANCH_NOT_FOUND` | NOT_FOUND | branch missing or deleted |
| `VER.BRANCH_EXISTS` | CONFLICT | branch name taken |
| `VER.BRANCH_HAS_CHILDREN` | CONFLICT | branch is a parent |
| `VER.BRANCH_CHAIN_TOO_DEEP` | LIMIT | more than 8 ancestors |
| `VER.BRANCH_WRITE_DENIED` | FORBIDDEN | write mode forbids the commit |
| `VER.MERGE_CONFLICT` | CONFLICT | conflicts listed in `details.conflicts` |
| `VER.ANCESTOR_LIMIT` | LIMIT | ancestor walk exceeded 10 000 |
| `VER.UNDO_CONFLICT` | CONFLICT | entity changed after the undone process |
| `VER.TAG_IMMUTABLE` | CONFLICT | tag update attempted |
| `VER.COMMIT_NOT_OF_ENTITY` | INVALID | revert/reset target belongs to another entity or branch |

---

## 5. Non-functional

| ID | Requirement | Target |
|---|---|---|
| NFR-PERF-040 | Resolve on a 3-level branch chain, warm head cache | p95 ≤ 0.3 ms |
| NFR-PERF-041 | Branch status for 10 000 own heads | ≤ 1 s, no snapshot reads |
| NFR-PERF-042 | Publish of a draft with 100 entities (fast-forward) | p95 ≤ 1 s (PG) |
| NFR-SCAL-040 | Branches per tenant | ≥ 10 000 (drafts included) |

---

## 6. Acceptance

| REQ | Criterion |
|---|---|
| REQ-VER-001 | First draft commit has the fork base as parent; author comes from the context |
| REQ-VER-002 | Auto-draft creation; delete rules; history survives deletion |
| REQ-VER-003 | OVERLAY live vs FEATURE frozen; masking tombstone |
| REQ-VER-004 | Reset to inherit with correct history |
| REQ-VER-005 | MERGE_ONLY and FROZEN enforcement |
| REQ-VER-006, 007 | Pinned Context stable; tag immutability |
| REQ-VER-010…013 | Merge case table tests; ancestor; path merge; publish/discard |
| REQ-VER-020…022 | Revert; undo process; admin reset audited |
| REQ-VER-030…032 | JSON Patch round trip; head-only status; history with inheritance |
| REQ-VER-040 | Autosave on drafts |

---

## 7. Implementation notes

- **Reference code:**
  - UB merge [UB:backend/src/main/java/com/ubos/bpu/handler/MergeBranchHandler.java], revert [UB:backend/src/main/java/com/ubos/bpu/handler/RevertHandler.java], diff [UB:backend/src/main/java/com/ubos/bpu/handler/DiffHandler.java], drafts [UB:backend/src/main/java/com/ubos/ese/handler/SaveDraftsHandler.java], [UB:backend/src/main/java/com/ubos/ese/handler/PublishEntitiesHandler.java].
  - FU merge [FU:backend/src/main/java/org/logrum/ubos/kernel/service/LcmKernelService.java#L710-L878], conflicts [FU:backend/src/main/java/org/logrum/ubos/kernel/service/LcmMergeConflictService.java], branch diff [FU:…/LcmKernelService.java#L1019-L1062], drafts [FU:backend/src/main/java/org/logrum/ubos/kernel/service/LcmStashService.java].
  - UP fallback [UP:ubos-server/src/main/java/org/logrum/ubos/kernel/service/LcmKernelService.java#L54-L89].
- **Divergences resolved:**
  - FU's head-move revert becomes the admin-only reset.
  - UB's merge compared top-level fields only; here the merge works per path.
  - UB's publish used FORCE_SOURCE; here it defaults to THREE_WAY.
  - FU's `stash/{user}/{entityId}` frontend naming is dropped in favour of `draft/<principal>/<base>`.
  - UP's `sys_branch_config` table is replaced by Branch entities.
- **Rust crates:** use `json-patch` for RFC 6902 and a JSON Pointer utility. Keep the three-way merge in `ubos_kernel::version::merge` as a pure function over `serde_json::Value`, with property metadata passed in.

## 8. Open questions

None.
- Deferred to P2: squashing micro-commits (CON-UB-047); rebase of FEATURE branches; branch-level locking beyond write modes.
- Deferred to a later version: sync of personal editions by branch push/pull (VRD-19-05).
