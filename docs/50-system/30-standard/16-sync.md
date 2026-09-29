---
id: UBS-STD-16
title: BPA Standard — Sync Protocol (SYNC)
status: draft
phase: PH-0
depends_on: [UBS-STD-08, UBS-STD-15]
---

# BPA Standard — Sync Protocol (SYNC) `[Profile: Sync]`

## 1. Replication

### STD-SYNC-001 — Session and scope
- **Clause:** A sync session MUST start with `sync.open{scope, device_id?, cursor?}`. The responder MUST authorise the scope against the initiator principal and device clearance (FR-SYNC-013). The effective scope is the intersection of the requested scope and the authorised scope, and MUST be returned.
- **Grammar / Schema:** `DAT-SyncScope`
- **Conformance vectors:** VER-CONF-5700…5714
- **Satisfies:** FR-SYNC-013, FR-SYNC-021, FR-SYNC-041
- **Notes:** —

```yaml
DAT-SyncScope:
  vae: { type: text, required: true, description: "authority" }
  branches: { type: list, required: true, description: "branch names" }
  classes: { type: list, required: false, description: "class URIs (subclasses included)" }
  query: { type: composite, required: false, description: "DAT-Query restricting objects" }
  max_classification: { type: enum, required: true, description: "clearance ceiling" }
  history_window: { type: duration, required: false, description: "versions older than now − window are not transferred (heads always)" }
```

### STD-SYNC-002 — Head exchange and have/want
- **Clause:** Peers MUST exchange `sync.heads{branch → commit id, root}` for the scope. For each branch whose root differs, the receiver walks the prolly tree top-down, requesting nodes (`sync.want{node_hashes}`) and answering `sync.have{node_hashes}` until the leaf differences are known. Scoped sync MUST use a scope-filtered tree built with the same boundary rule (STD-PROOF-001) over the in-scope keys. Its root is signed by the responder for the session.
- **Grammar / Schema:** —
- **Conformance vectors:** VER-CONF-5715…5744
- **Satisfies:** FR-SYNC-011, NR-PERF-018
- **Notes:** Scope-filtered trees are needed because full-branch roots cover out-of-scope data.

### STD-SYNC-003 — Transfer and verification
- **Clause:** Missing chunks, versions, entries and commits MUST be transferred in batches (`sync.batch{items, batch_seq}`). Each item MUST be verified by content hash, and each commit by signature, before it is acknowledged (`sync.ack{batch_seq}`). An invalid item MUST abort the session with `SYNC.INTEGRITY_FAILURE`. Sessions MUST be resumable from the last acknowledged batch.
- **Grammar / Schema:** —
- **Conformance vectors:** VER-CONF-5745…5774
- **Satisfies:** FR-SYNC-012, FR-SYNC-014
- **Notes:** —

### STD-SYNC-004 — Device branches and integration
- **Clause:** Local commits of a device MUST be pushed as a branch `device/<device>/<n>` of kind `device`. The server MUST integrate them into the target branch as follows:
  1. objects of `direct`-policy classes are merged automatically (STD-VER-050);
  2. ledger entries are appended (STD-VER-054);
  3. changes to `change_set`-policy classes form a change set.

  The result (`sync.integration{merged, conflicts, rejected: [{item, explanation}]}`) MUST be returned. Rejected items MUST remain on the device branch.
- **Grammar / Schema:** —
- **Conformance vectors:** VER-CONF-5775…5804
- **Satisfies:** FR-SYNC-022, FR-SYNC-031, FR-SYNC-032, FR-SYNC-033
- **Notes:** —

### STD-SYNC-005 — Scope reduction and wipe
- **Clause:** When the effective scope shrinks, the responder MUST send `sync.evict{keys}` for objects leaving the scope, and the device MUST delete them locally. When a device is revoked, the next contact MUST receive `sync.revoked`, and the device MUST destroy its local keys.
- **Grammar / Schema:** —
- **Conformance vectors:** VER-CONF-5805…5814
- **Satisfies:** FR-SYNC-042, FR-IAM-103
- **Notes:** —

### STD-SYNC-006 — Formal model
- **Clause:** The sync protocol MUST be accompanied by a TLA+ model in the standard repository, with the invariants NoLostCommit, IntegrityOnReceipt, ConvergenceAfterQuiescence and ScopeSafety model-checked for 3 peers, 4 commits each and 2 interruptions.
- **Grammar / Schema:** —
- **Conformance vectors:** —
- **Satisfies:** FR-SYNC-015, FR-STD-031
- **Notes:** —

## 2. Federation `[from PH-5]`

### STD-SYNC-010 — Network fallback state machine
- **Clause:** Cross-node inherited resolution MUST follow the states `LOCAL_CACHE → LOCAL_STORE → PARENT_LOOKUP → REMOTE_FETCH` (UBS-ARC-05 F10). Remote results MUST include an inclusion proof against a signed parent commit, and MUST be cached as foreign snapshots keyed by content ID.
- **Grammar / Schema:** —
- **Conformance vectors:** VER-CONF-5820…5844
- **Satisfies:** FR-SYNC-062, FR-SYNC-063
- **Notes:** —

### STD-SYNC-011 — Shared BPU replication
- **Clause:** A shared BPU MUST be replicated on a branch `shared/<agreement-id>` in each party's VAE, with identical commit history.
  - Definition changes MUST be proposed as signed change sets. They become effective on both replicas only when every party required by the sharing agreement has signed the same merge commit ID. Until then, the proposal is pending on both sides.
  - Ledger appends within a party's rights MUST be signed by that party and accepted by the other replica without further approval.
  - Conflicting pending proposals MUST all remain pending.
- **Grammar / Schema:** —
- **Conformance vectors:** VER-CONF-5845…5884
- **Satisfies:** FR-SYNC-071, FR-SYNC-072, FR-SYNC-073, FR-SYNC-074
- **Notes:** The multi-signature merge commit is the single point at which shared state advances.

### STD-SYNC-012 — Shared BPU formal model
- **Clause:** The shared-BPU protocol MUST have a TLA+ model with the invariants NoUnilateralDefinitionChange, ReplicaAgreement (equal roots after delivery of all messages) and ProgressUnderAgreement, model-checked for 2 and 3 parties.
- **Grammar / Schema:** —
- **Conformance vectors:** —
- **Satisfies:** FR-SYNC-071, FR-STD-031
- **Notes:** —
