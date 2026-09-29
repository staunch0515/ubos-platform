---
id: UBS-REQ-05-02
title: Functional Requirements — VER (Versioning, Branches, Merge, Release)
status: review
phase: PH-1
depends_on: [UBS-REQ-05, UBS-REQ-05-01]
---

# Functional Requirements — VER

## CAP-VER-01 — Commit history

### FR-VER-011 — Commit content
- **Statement:** Every commit MUST record:
  - `commit_id` (content hash) and `parents` (zero or more commit IDs);
  - `branch` and `author` (principal) and `on_behalf_of` (optional principal);
  - `process_id` and `transaction_time`;
  - `changes` (object version references) and `root` (Merkle root of the branch state after the commit);
  - `abi_version`, `message` and `signature`.
- **Rationale:** A self-describing commit is the unit of audit, proof and replay.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-InternalAuditor, PER-KernelEngineer
- **Acceptance:**
  1. Given any commit, when it is read, then every listed member is present, and `commit_id` equals the hash of its canonical form without the signature.
- **Verification:** CONF
- **Origin:** EXT-RFC, IMP-03, L40:SPEC-14

### FR-VER-012 — Commits are immutable
- **Statement:** A commit and the object versions it references MUST NOT be modified or deleted by any operation. The only exception is key destruction for crypto-shredding (FR-PROOF-071), which leaves ciphertexts and hashes unchanged.
- **Rationale:** History is the only truth (EXT-TRI Book I).
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM, NOD
- **Personas:** PER-InternalAuditor, PER-RegulatorExaminer
- **Acceptance:**
  1. Given administrative access to the node, when any exposed operation is used, then there is no way to change the bytes of a committed version.
  2. Given direct storage tampering, when the node verifies the chain, then the tampering is detected as a hash mismatch.
- **Verification:** CONF, SEC, FAULT
- **Origin:** EXT-RFC, EXT-TRI

### FR-VER-013 — One commit per process flush
- **Statement:** All changes produced by one process on one branch MUST be committed as exactly one commit, or not at all. A process that writes to several branches MUST produce one commit per branch, linked by the same `process_id`.
- **Rationale:** The atomic unit of business change is the process (L40:SPEC-15).
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-KernelEngineer
- **Acceptance:**
  1. Given a process that updates 340 objects, when it completes, then exactly one new commit exists on the branch, listing 340 changes.
- **Verification:** CONF, FAULT
- **Origin:** L40:SPEC-15

### FR-VER-014 — Reason codes for automated commits
- **Statement:** Commits made by schedules, jobs, agents, integrations or kernel services MUST carry a machine-readable `reason_code` from a registered set, in addition to the free-text message.
- **Rationale:** Auditors filter automated activity by cause.
- **Priority:** Should · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-InternalAuditor
- **Acceptance:**
  1. Given a commit made by the NAV schedule, when read, then `reason_code` is `schedule.run` and the schedule object is referenced.
- **Verification:** CONF
- **Origin:** NEW — improves audit filtering.

### FR-VER-015 — History listing
- **Statement:** The platform MUST list the history of an object, a class, a branch or a process, filtered by author, reason code, time range and change type, with snapshot-consistent paging.
- **Rationale:** History is a primary user-facing feature (history viewer, audit).
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM, SDK
- **Personas:** PER-BusinessUser, PER-InternalAuditor
- **Acceptance:**
  1. Given 10,000 commits on a branch, when paged 100 at a time during concurrent writes, then no entry is duplicated or skipped relative to the snapshot taken at the first page.
- **Verification:** CONF, PROP
- **Origin:** L40:SPEC-14

## CAP-VER-02 — Branches

### FR-VER-021 — Zero-copy branch creation
- **Statement:** Creating a branch MUST write only a branch reference to an existing commit. Its cost MUST be independent of the amount of data in the source branch.
- **Rationale:** Cheap branches make drafts, agents and simulations practical (EXT-RFC 2.3).
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-BusinessArchitect, PER-AiAgent
- **Acceptance:**
  1. Given branches with 1,000 objects and with 10,000,000 objects, when a branch is created from each, then the creation time is within the same NR-PERF bound.
- **Verification:** CONF, BENCH
- **Origin:** EXT-RFC, IMP-03

### FR-VER-022 — Branch kinds and their policies
- **Statement:** Every branch MUST have one kind from the set below. Each kind MUST enforce its creation rights, allowed merge targets and default lifetime as specified in the BPA standard.

  | Kind | Purpose |
  |---|---|
  | `main` | primary production line of a VAE |
  | `release` | maintenance of a published release |
  | `draft` | business change being prepared |
  | `feature` | builder or developer work |
  | `agent` | AI agent work (IMP-10) |
  | `overlay` | tenant customisation inheriting a vendor line |
  | `loader` | Buk installation staging |
  | `evolution` | governed BPU evolution proposals |
  | `correction` | back-dated restatements |
  | `sandbox` | disposable experiments and simulations |
- **Rationale:** Named kinds carry safe defaults and keep the user interface simple.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-BusinessArchitect, PER-AgentSupervisor
- **Acceptance:**
  1. Given an `agent` branch, when the agent principal requests a merge into `main`, then the request is refused, because agent branches merge only through an approved change set.
  2. Given a `sandbox` branch, when its lifetime expires, then it is archived automatically.
- **Verification:** CONF
- **Origin:** L40:SPEC-14, IMP-10

### FR-VER-023 — Branch naming
- **Statement:** A branch name MUST consist of lowercase slug segments separated by `/`, MUST NOT contain dots, and MUST be unique within the VAE. The first segment MUST be the kind, except for `main`.
- **Rationale:** Dots are reserved in URIs. The kind prefix makes the purpose visible.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-LogicDeveloper
- **Acceptance:**
  1. Given the name `draft/q4.pricing`, when created, then it fails with `VER.INVALID_BRANCH_NAME`.
- **Verification:** CONF
- **Origin:** L40:SPEC-12

### FR-VER-024 — Write policy per class
- **Statement:** Every class MUST declare a write policy for protected branches (`main`, `release/*`):
  - `direct`: operational changes through ports are committed directly under authorization;
  - `change_set`: changes arrive only by merging an approved change set.

  The DVM MUST reject direct commits that violate the policy with `VER.CHANGE_SET_REQUIRED`.
- **Rationale:** Daily transactions flow directly. Governed definitions (classes, rules, master data) always pass review.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-BusinessArchitect, PER-ComplianceOfficer
- **Acceptance:**
  1. Given `FeeSchedule` with policy `change_set`, when a user edits it directly on `main`, then the commit fails with `VER.CHANGE_SET_REQUIRED`, and the user interface offers "start a change".
  2. Given `DealingOrder` with policy `direct`, when a clerk submits an order, then it commits on `main`.
- **Verification:** CONF, SCN
- **Origin:** L40:SPEC-13, IMP-11

### FR-VER-025 — Branch lifecycle
- **Statement:** A branch MUST have the states `active`, `frozen` (read-only) and `archived` (reference hidden, commits retained). The platform MUST detect branches inactive beyond a configurable period and notify their owners.
- **Rationale:** Stale drafts and agent branches accumulate.
- **Priority:** Should · **Phase:** PH-2 · **Systems:** DVM, STU
- **Personas:** PER-TenantAdministrator
- **Acceptance:**
  1. Given a draft inactive for 90 days, when the housekeeping job runs, then the owner is notified. After a further 30 days the branch is archived, and it can be restored.
- **Verification:** CONF
- **Origin:** NEW — branch hygiene at scale.

### FR-VER-026 — Branch-level permissions
- **Statement:** Read, write, merge-into and administer permissions MUST be evaluated per branch and branch kind by the policy engine (CAP-IAM-04).
- **Rationale:** Access to a draft is not access to production, and the reverse.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-SecurityOfficer
- **Acceptance:**
  1. Given a user with write on `draft/*` only, when the user writes on `main`, then the write is denied.
- **Verification:** CONF, SEC
- **Origin:** L40:SPEC-22

### FR-VER-027 — Branch from any point
- **Statement:** A branch MUST be creatable from a branch head, a commit, a tag or a branch at a transaction time.
- **Rationale:** Corrections and investigations start from past states.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-FundAccountant, PER-InternalAuditor
- **Acceptance:**
  1. Given `main@2026-10-02T18:00Z`, when a correction branch is created from it, then reading on the new branch returns the state as of that time.
- **Verification:** CONF
- **Origin:** L40:SPEC-14

## CAP-VER-03 — Change sets

### FR-VER-031 — Change set object and lifecycle
- **Statement:** A change set MUST be a Definition object that references a source branch and a target branch. It has the lifecycle `open → submitted → approved → merged`, with the exits `rejected` and `abandoned`. It MUST record title, description, author, reviewers, approvals, linked simulation runs and comments.
- **Rationale:** The change set is the business-facing unit of governed change: the "draft → submit → published" of IMP-11.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM, STU, WSP
- **Personas:** PER-BusinessArchitect, PER-Approver
- **Acceptance:**
  1. Given a submitted change set, when approvers approve per policy, then it moves to `approved`. When merged, then it moves to `merged` and references the merge commit.
- **Verification:** CONF, SCN
- **Origin:** L40:SPEC-14, IMP-11

### FR-VER-032 — Approval invalidation on change
- **Statement:** Any change to the source branch after an approval MUST invalidate that approval and return the change set to `submitted`. The reviewers MUST see the diff since their approval.
- **Rationale:** Reviewers approve what they saw, not what came later.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-Approver, PER-ComplianceOfficer
- **Acceptance:**
  1. Given an approved change set, when the author edits one object, then the approval is voided, and the incremental diff is shown.
- **Verification:** CONF, SCN
- **Origin:** NEW — standard four-eyes integrity.

### FR-VER-033 — Review summary
- **Statement:** A change set MUST provide a summary of counts per class and operation, the classes under `change_set` policy, the ledger entries included, the affected projections and a risk flag list defined by policy.
- **Rationale:** Reviewers need scale and risk at a glance.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM, STU, WSP
- **Personas:** PER-Approver, PER-AgentSupervisor
- **Acceptance:**
  1. Given a change set touching 15 products and 1 fee schedule, when summarised, then it shows "Product: 12 updated, 3 created; FeeSchedule: 1 updated (governed)".
- **Verification:** SCN, USE
- **Origin:** IMP-10, IMP-11

### FR-VER-034 — Target drift handling
- **Statement:** When the target branch moves after submission, the platform MUST recompute the merge preview. A change set whose preview has conflicts MUST NOT be mergeable until they are resolved.
- **Rationale:** A merge must be evaluated against the target as it is now.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-BusinessArchitect
- **Acceptance:**
  1. Given `main` changed after approval in a way that conflicts, when a merge is attempted, then it fails with `VER.MERGE_CONFLICT`, and the change set shows the new conflicts.
- **Verification:** CONF, SCN
- **Origin:** L40:SPEC-14

### FR-VER-035 — Partial acceptance
- **Statement:** A reviewer MUST be able to accept a subset of a change set's objects. The platform then merges only the accepted objects and leaves the rest on the source branch in a new change set. Selection MUST respect dependencies: an object whose references would dangle MUST NOT be accepted alone.
- **Rationale:** Agent change sets are often mostly right (SCN-301).
- **Priority:** Should · **Phase:** PH-3 · **Systems:** DVM, WSP
- **Personas:** PER-AgentSupervisor, PER-Approver
- **Acceptance:**
  1. Given 35 items, of which 33 are accepted, when merged, then 33 items land on `main`, and a new change set holds 2 items.
  2. Given item B that references new item A, when B is accepted without A, then the selection is refused with an explanation.
- **Verification:** CONF, SCN
- **Origin:** IMP-10

### FR-VER-036 — Comments on change-set items
- **Statement:** Users MUST be able to comment on a change set and on individual items and fields, with threads and resolution status.
- **Rationale:** Review is a conversation.
- **Priority:** Should · **Phase:** PH-2 · **Systems:** STU, WSP
- **Personas:** PER-Approver, PER-BusinessArchitect
- **Acceptance:**
  1. Given a comment on a field, when the field changes, then the comment is marked "outdated" and stays visible.
- **Verification:** SCN
- **Origin:** NEW

## CAP-VER-04 — Diff

### FR-VER-041 — Diff between any two states
- **Statement:** The platform MUST compute the difference between any two states given as commit, branch, tag or branch-at-time, per object and per field path. For each object it MUST report whether it was added, removed, changed or moved (slug change).
- **Rationale:** Diff is the core review tool.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-Approver, PER-BusinessArchitect
- **Acceptance:**
  1. Given two tags 30 days apart, when diffed, then every changed object appears with field paths and old and new values.
- **Verification:** CONF, PROP
- **Origin:** L40:SPEC-14

### FR-VER-042 — Semantic diff
- **Statement:** Diffs MUST be semantic for keyed lists (elements matched by key), rich documents (text-level changes) and value types (for example a `money` change shows amount and currency).
- **Rationale:** Positional diffs of lists and documents are unreadable.
- **Priority:** Should · **Phase:** PH-2 · **Systems:** DVM, WSP
- **Personas:** PER-ContractManager, PER-Approver
- **Acceptance:**
  1. Given a reordered and edited keyed list, when diffed, then one element is shown as changed and the order change as a move, not all elements as changed.
- **Verification:** CONF
- **Origin:** NEW

### FR-VER-043 — Large diffs
- **Statement:** Diffs MUST be pageable and summarisable, with counts computed without materialising all entries.
- **Rationale:** Agent and migration change sets can touch millions of objects.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-AgentSupervisor
- **Acceptance:**
  1. Given a diff of 1,000,000 changes, when the summary is requested, then counts are returned within NR-PERF bounds.
- **Verification:** BENCH
- **Origin:** IMP-03

### FR-VER-044 — Permission-aware diff
- **Statement:** A diff MUST NOT reveal values the reader may not read. Masked fields MUST be reported as "changed" without values, and invisible objects MUST be omitted without any count leak beyond policy.
- **Rationale:** Review must not bypass authorization.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-SecurityOfficer
- **Acceptance:**
  1. Given a reviewer without `personal` clearance, when a change to `email` is diffed, then the entry shows "email: changed" with no values.
- **Verification:** CONF, SEC
- **Origin:** L40:SPEC-22

### FR-VER-045 — Diff cost proportional to change
- **Statement:** The cost of diffing two states MUST be proportional to the number of changed objects, not to the total number of objects, by comparing content-addressed structure.
- **Rationale:** Fast diff is a direct benefit of IMP-03.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-KernelEngineer
- **Acceptance:**
  1. Given 10 changes in a store of 10,000,000 objects, when diffed, then the time is within the NR-PERF bound for small diffs.
- **Verification:** BENCH
- **Origin:** IMP-03

## CAP-VER-05 — Merge

### FR-VER-051 — Three-way merge of Definition state
- **Statement:** Merging MUST compute the merge base as the best common ancestor (a recursive virtual base for criss-cross histories). It MUST merge each Definition object field by field: a field changed on one side only takes that change, and a field changed identically on both sides takes it once.
- **Rationale:** Git-grade merge for business definitions.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-BusinessArchitect
- **Acceptance:**
  1. Given the property suite of merge laws (identity, commutativity of disjoint changes, idempotence), when run over generated histories, then all properties hold.
- **Verification:** PROP, CONF, FORM
- **Origin:** L40:SPEC-14

### FR-VER-052 — Complete conflict reporting
- **Statement:** When fields conflict, the merge MUST fail as a whole and report every conflict at once with base, source and target values, the authors and the commits. A conflict MUST NOT be resolved silently.
- **Rationale:** Silent overwrite is the main failure of LWW systems (UBS-META-06 §6).
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-BusinessArchitect
- **Acceptance:**
  1. Given 7 conflicting fields across 3 objects, when merged, then one error lists all 7.
- **Verification:** CONF, PROP
- **Origin:** L40:SPEC-14

### FR-VER-053 — Per-field merge strategies
- **Statement:** A field MAY declare a merge strategy:
  - `three_way` (default);
  - `prefer_source` or `prefer_target`;
  - `last_writer` by transaction time;
  - `set_union` for sets;
  - `counter_delta` for integer counters;
  - `manual` (always a conflict when both sides changed).

  The strategy MUST apply only to that field.
- **Rationale:** Some fields have natural merge semantics. LWW is allowed only where declared.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-BusinessArchitect
- **Acceptance:**
  1. Given a `set_union` field `tags` changed on both sides, when merged, then the result is the union without conflict.
- **Verification:** CONF, PROP
- **Origin:** EXT-RFC, UBS-META-06

### FR-VER-054 — Ledger entries are appended, never merged
- **Statement:** When a source branch holding new ledger entries is merged, those entries MUST be appended to the target in source order, with new target sequence numbers and a reference to their source sequence numbers. Ledger entries MUST NOT participate in field merge.
- **Rationale:** Facts from a branch become facts on the target (IMP-01).
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-FundAccountant
- **Acceptance:**
  1. Given 12 entries on a correction branch, when merged, then 12 new entries appear on `main` with contiguous new sequence numbers, each carrying `source_seq`.
- **Verification:** CONF, PROP
- **Origin:** IMP-01

### FR-VER-055 — Validation of the merged result
- **Statement:** Before a merge commit is written, the merged state MUST pass all constraints, invariants and governance-sheet declarations evaluated on the merged result. A failure MUST abort the merge with explained errors.
- **Rationale:** Two valid sides can produce an invalid combination.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-BusinessArchitect
- **Acceptance:**
  1. Given side A raising a limit and side B raising usage, where each is valid alone but the combination breaks an invariant, when merged, then the merge fails with the invariant explanation.
- **Verification:** CONF, PROP
- **Origin:** L40:SPEC-18

### FR-VER-056 — Atomic merge
- **Statement:** A merge MUST produce exactly one merge commit with two parents, or no change at all.
- **Rationale:** Partial merges would publish inconsistent states.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-KernelEngineer
- **Acceptance:**
  1. Given a crash injected during merge, when the node restarts, then either the merge commit exists completely or the target is unchanged.
- **Verification:** FAULT, SIM
- **Origin:** L40:SPEC-15

### FR-VER-057 — Merge preview
- **Statement:** The platform MUST compute a merge preview (the result, conflicts and validation outcome) without writing anything.
- **Rationale:** Review and simulation depend on it.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-BusinessArchitect, PER-Approver
- **Acceptance:**
  1. Given a preview, when the same merge is executed with no intervening changes, then the resulting state equals the preview.
- **Verification:** CONF, PROP
- **Origin:** L40:SPEC-14

### FR-VER-058 — Recorded conflict resolutions
- **Statement:** Every conflict resolution MUST be recorded with the chosen value, the resolver principal and an optional reason, and linked to the merge commit.
- **Rationale:** Resolutions are business decisions that auditors review.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-InternalAuditor
- **Acceptance:**
  1. Given a resolved conflict, when the merge commit is inspected, then each resolution is listed.
- **Verification:** CONF
- **Origin:** NEW

## CAP-VER-06 — Tags and releases

### FR-VER-061 — Immutable tags
- **Statement:** A tag MUST name exactly one commit, MUST be a slug, and MUST NOT be moved or reused once created. Deleting a tag reference MUST keep a tombstone that prevents reuse.
- **Rationale:** Period closes, releases and evidence refer to tags.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-FundAccountant, PER-TenantAdministrator
- **Acceptance:**
  1. Given tag `close-2026-10`, when re-pointing is attempted, then it fails with `VER.TAG_IMMUTABLE`.
- **Verification:** CONF
- **Origin:** L40:SPEC-14

### FR-VER-062 — Releases
- **Statement:** A release MUST be a signed tag on a branch plus a release object with version (semantic), notes, the change sets included and approvals. A `release` branch MAY be created from it for fixes.
- **Rationale:** Vendors and tenants publish and consume versions of rule sets and Buks.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM, STU
- **Personas:** PER-IsvDeveloper, PER-BusinessArchitect
- **Acceptance:**
  1. Given release `2026.11.0`, when inspected, then it lists its change sets and approvals, and the tag signature verifies.
- **Verification:** CONF
- **Origin:** L40:SPEC-14

### FR-VER-063 — Pinning to a release
- **Statement:** An overlay or child VAE MUST be able to follow its parent line either `live` (always the parent head) or `pinned` (a specific release). Pin changes MUST be governed changes.
- **Rationale:** Regulated tenants adopt vendor changes on their own schedule.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-TenantAdministrator
- **Acceptance:**
  1. Given an overlay pinned to `1.2.0`, when the vendor releases `1.3.0`, then the overlay's effective state is unchanged until the pin is updated.
- **Verification:** CONF, SCN
- **Origin:** EXT-TRI, L40:SPEC-23

### FR-VER-064 — Signed tags
- **Statement:** Tags on `main` and `release` branches MUST be signed by the tagging principal. Tag signature verification MUST be available offline.
- **Rationale:** Releases and closes are evidence.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-RegulatorExaminer
- **Acceptance:**
  1. Given an exported tag, when verified with the verifier tool, then the signature and target commit check out.
- **Verification:** CONF
- **Origin:** EXT-RFC

## CAP-VER-07 — Revert and undo

### FR-VER-071 — Revert a commit
- **Statement:** Reverting a commit MUST create a new commit that restores, for each Definition object changed by that commit, the previous values of the fields that commit changed. Fields changed later by other commits MUST be reported as conflicts.
- **Rationale:** Undo without rewriting history.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-BusinessArchitect
- **Acceptance:**
  1. Given a commit that changed 3 fields, when reverted, then a new commit restores them, and the original commit is still in history.
- **Verification:** CONF, PROP
- **Origin:** L40:SPEC-14

### FR-VER-072 — Revert a process
- **Statement:** Reverting a process MUST revert all its commits on all branches as one revert process, with conflicts reported for objects changed afterwards (SCN-018).
- **Rationale:** A bulk mistake is undone as the unit in which it was made.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-BusinessArchitect, PER-AgentSupervisor
- **Acceptance:**
  1. Given SCN-018, when executed, then the revert covers 335 objects automatically and lists 5 conflicts.
- **Verification:** SCN, PROP
- **Origin:** L40:SPEC-15

### FR-VER-073 — Revert a merged change set
- **Statement:** Reverting a merged change set MUST revert the merge commit's effect on the target branch relative to its first parent.
- **Rationale:** Rollback of a published change.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-BusinessArchitect
- **Acceptance:**
  1. Given a merged pricing change, when reverted, then the prices return to their pre-merge values.
- **Verification:** CONF
- **Origin:** L40:SPEC-14

### FR-VER-074 — Revert of ledger effects
- **Statement:** When a reverted commit or process appended ledger entries, the revert MUST append reversal entries (FR-LEDG-021) instead of removing entries.
- **Rationale:** Ledgers are corrected only by reversal.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-FundAccountant
- **Acceptance:**
  1. Given a process that appended 12 entries, when reverted, then 12 reversal entries are appended.
- **Verification:** CONF
- **Origin:** IMP-01

### FR-VER-075 — Dependent process report
- **Statement:** Before a revert, the platform MUST list the processes triggered by the reverted process (through events, jobs or triggers) that are still in effect, and offer to include them.
- **Rationale:** Reverting a cause without its effects leaves inconsistency.
- **Priority:** Should · **Phase:** PH-2 · **Systems:** DVM
- **Personas:** PER-BusinessArchitect
- **Acceptance:**
  1. Given P that triggered Q, when P's revert is previewed, then Q is listed with its changes.
- **Verification:** CONF
- **Origin:** L40:SPEC-15

## CAP-VER-08 — Branch inheritance

### FR-VER-081 — Read-through to the parent line
- **Statement:** A branch with an inheritance parent MUST resolve any object that it does not hold locally from the parent line: the parent head for `live` inheritance, or the pinned commit for `pinned` inheritance. Resolution MUST continue recursively up the chain.
- **Rationale:** Tenants store only their delta (EXT-TRI Book II, "delta overrides").
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-ImplementationConsultant
- **Acceptance:**
  1. Given a vendor line with 500 objects and an overlay with 3 overrides, when the overlay is read, then 500 objects are visible, and 3 of them come from the overlay.
- **Verification:** CONF, PROP
- **Origin:** EXT-TRI, EXT-WP, L40:SPEC-14

### FR-VER-082 — Local overrides
- **Statement:** Writing an inherited object on a child branch MUST create a local version that shadows the parent version for that branch only.
- **Rationale:** Customise one asset without forking all.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-ImplementationConsultant
- **Acceptance:**
  1. Given an override of `tax_logic` on tenant A, when tenant B reads it, then B sees the vendor version.
- **Verification:** CONF
- **Origin:** EXT-TRI

### FR-VER-083 — Hiding inherited objects
- **Statement:** A child branch MUST be able to hide an inherited object with a tombstone. The tombstone MUST NOT delete anything on the parent line.
- **Rationale:** Tenants may not want some vendor views, rules or seed records.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-ImplementationConsultant
- **Acceptance:**
  1. Given a tombstone for view `X`, when the child is read, then `X` is not found. The parent is unchanged.
- **Verification:** CONF
- **Origin:** L40:SPEC-14

### FR-VER-084 — Inheritance chain limits
- **Statement:** An inheritance chain MUST NOT contain cycles and MUST support a depth of at least 8.
- **Rationale:** Platform → industry → vendor → region → tenant → department chains exist.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-KernelEngineer
- **Acceptance:**
  1. Given a chain of 8 branches, when the deepest is read, then resolution succeeds. When a cycle is configured, then it fails with `VER.INHERITANCE_CYCLE`.
- **Verification:** CONF
- **Origin:** NEW

### FR-VER-085 — Provenance of effective objects
- **Statement:** For any object read on a branch, the platform MUST be able to report the branch and commit that supplied the effective version.
- **Rationale:** "Why do I see this version?" is a common support question.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-ImplementationConsultant, PER-TenantAdministrator
- **Acceptance:**
  1. Given an inherited object, when explained, then the answer names the vendor branch and commit.
- **Verification:** CONF
- **Origin:** NEW

### FR-VER-086 — Upstream change notification for overrides
- **Statement:** When the parent line changes an object that a child overrides, the platform MUST flag the override as "upstream changed" and MUST offer a three-way merge of the parent change into the override.
- **Rationale:** Overrides silently falling behind upstream fixes are a classic customisation trap.
- **Priority:** Should · **Phase:** PH-2 · **Systems:** DVM, STU
- **Personas:** PER-ImplementationConsultant
- **Acceptance:**
  1. Given a vendor fix to an overridden view, when released, then the tenant's override is flagged, and a merge preview is available.
- **Verification:** CONF, SCN
- **Origin:** NEW

## CAP-VER-09 — History tiering

### FR-VER-091 — Tiering policy
- **Statement:** The platform MUST move object versions older than a policy threshold, and not referenced by branch heads, to a colder storage tier by policy per class and VAE.
- **Rationale:** Full history grows without bound (IMP-08).
- **Priority:** Must · **Phase:** PH-4 · **Systems:** DVM, NOD
- **Personas:** PER-PlatformOperator
- **Acceptance:**
  1. Given policy "older than 2 years", when the tiering job runs, then qualifying versions move to the cold tier, and hot storage shrinks accordingly.
- **Verification:** SCN, BENCH
- **Origin:** IMP-08

### FR-VER-092 — Tiered history stays readable and provable
- **Statement:** Tiered versions MUST remain readable through the same operations (with the latency allowed by NR-PERF), and their proofs MUST remain verifiable.
- **Rationale:** Tiering is a cost optimisation, not an archive cut-off.
- **Priority:** Must · **Phase:** PH-4 · **Systems:** DVM
- **Personas:** PER-InternalAuditor
- **Acceptance:**
  1. Given a tiered version, when read with `time=` in the past, then it is returned, and its inclusion proof verifies.
- **Verification:** CONF, BENCH
- **Origin:** IMP-08

### FR-VER-093 — Tiering respects retention and holds
- **Statement:** Tiering MUST NOT dispose of data. Disposal MUST happen only through retention policies (CAP-AUD-07) and never under legal hold.
- **Rationale:** Moving data must not shortcut retention law.
- **Priority:** Must · **Phase:** PH-4 · **Systems:** DVM
- **Personas:** PER-ComplianceOfficer
- **Acceptance:**
  1. Given a held object, when tiering and disposal run, then it is tiered but not disposed of.
- **Verification:** CONF, AUDIT
- **Origin:** NEW

## CAP-VER-10 — Transaction-time travel

### FR-VER-101 — Read at any past point
- **Statement:** Every read operation MUST accept an optional point: `commit`, `tag`, or `time` (a transaction-time instant on a branch). It MUST return the state exactly as it was at that point.
- **Rationale:** Reproducibility for audit and investigation.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM, SDK
- **Personas:** PER-InternalAuditor, PER-RegulatorExaminer
- **Acceptance:**
  1. Given `time=T`, when read repeatedly while new commits arrive, then the result never changes.
- **Verification:** CONF, PROP
- **Origin:** L40:SPEC-14, EXT-TRI

### FR-VER-102 — Queries at any past point
- **Statement:** Criteria queries MUST accept the same point parameters as reads and MUST evaluate against the state at that point, including class definitions and authorization data as of the current time unless a historical-authorization mode is requested by an auditor role.
- **Rationale:** Past lists are as important as past records.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-InternalAuditor
- **Acceptance:**
  1. Given "all open orders at T", when queried, then the result equals the set that was open at T.
- **Verification:** CONF, PROP
- **Origin:** L40:SPEC-21

### FR-VER-103 — Snapshot-consistent multi-object reads
- **Statement:** All reads within one request or one process MUST observe a single consistent snapshot per branch, unless the caller explicitly requests a refreshed snapshot.
- **Rationale:** Logic that reads several objects must not see a torn state.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM
- **Personas:** PER-LogicDeveloper
- **Acceptance:**
  1. Given a process reading A then B while another commit changes both, when inspected, then the process saw either both old or both new values.
- **Verification:** SIM, PROP
- **Origin:** L40:SPEC-15

### FR-VER-104 — Point selectors in URIs
- **Statement:** Canonical URIs MUST support the selectors `branch`, `commit`, `tag`, `time` and `asof`, with the resolution precedence defined in the BPA standard.
- **Rationale:** Links to past states must be shareable.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** DVM, SDK
- **Personas:** PER-InternalAuditor, PER-LogicDeveloper
- **Acceptance:**
  1. Given `ubos://t1/Fund/geq?branch=main&time=2026-10-02T18:00:00Z&asof=2026-10-01`, when resolved, then the bitemporal state is returned.
- **Verification:** CONF
- **Origin:** L40:SPEC-12, IMP-02
