---
id: UBS-ARC-05
title: Runtime Flows
status: draft
phase: ALL
depends_on: [UBS-ARC-02, UBS-ARC-04]
---

# Runtime Flows

Each flow lists numbered steps with the responsible system and the contract used. The
flows are normative in their ordering guarantees (marked **Invariant**). The internal
algorithms are specified in the standard and the system chapters.

## F1 — Action invocation and commit

| # | System | Step |
|---|---|---|
| 1 | SDK → NOD (CTR-003) | `Mutate{port, target, params, base_version, idempotency_key}` arrives on an authenticated session |
| 2 | NOD | builds the execution context: principal chain, tenant, VAE, branch, `now` from the host clock, locale, profile |
| 3 | NOD → DVM (CTR-001) | `begin_process(ctx)`; checks the idempotency record, and returns the original result if the key was already seen |
| 4 | DVM | resolves the port polymorphically (FR-MODEL-031); evaluates decisions (allow, deny, require) and guards |
| 5 | DVM | if `require: approval`, stores a deferred commit and creates tasks, then goes to step 11 |
| 6 | DVM | runs port logic in the sandbox; each instruction is authorised and metered; non-deterministic values are journaled |
| 7 | DVM | validates all changed objects: constraints, validations, invariants, sheet declarations, referential integrity at valid time |
| 8 | DVM | CAS check: each written object's current head equals its base; serializable read-set check if declared |
| 9 | DVM → store (CTR-005) | flush in one transaction: chunks, object versions, ledger entries (with sequence allocation), kernel indexes, Merkle update, commit record with rule binding, sync projections, outbox, idempotency record, process record |
| 10 | DVM | signs the commit with the author key through the host key service (and the node co-signature where required) |
| 11 | NOD → SDK | returns the result: commit ID, new versions, warnings |
| 12 | NOD | releases outbox effects after commit (F4) |

**Invariants:**
1. Steps 4–9 either all take effect or none do (FR-TXN-012).
2. Nothing in the outbox is released before step 9 completes (FR-TXN-051).
3. The signature in step 10 covers the commit ID computed in step 9. The commit is acknowledged only after the signature is stored.

## F2 — Bitemporal read

| # | System | Step |
|---|---|---|
| 1 | SDK → NOD | `Query` or read with `branch`, `asof`, `time` (or `commit`/`tag`) |
| 2 | DVM | resolves the snapshot: the head commit at `time` for each branch in the chain |
| 3 | DVM | resolution per UBS-ARC-04 §4, with `temporal_index` |
| 4 | DVM | decrypts permitted fields, applies lenses, masks, and authorises (FR-IAM-041) |
| 5 | NOD → SDK | returns objects with `evaluated_at` commit IDs (FR-TIME-034) |

**Invariant:** Given the same `evaluated_at` commits and principal policies, the result is identical.

## F3 — Change set review and merge

| # | System | Step |
|---|---|---|
| 1 | STU or WSP → NOD | creates a draft branch (constant time) and a change set object |
| 2 | users | commit changes to the draft (F1 on the draft branch) |
| 3 | STU → NOD → FRG (CTR-012) | runs checks, tests and optional simulation, and attaches the results |
| 4 | DVM | on submit: computes the merge preview (base, conflicts, validation of the merged result) |
| 5 | WSP | approvers review the diff, simulation and risk flags; approvals are recorded with the preview hash |
| 6 | DVM | any source change invalidates approvals (FR-VER-032); target drift recomputes the preview |
| 7 | DVM | merge: recompute the preview against the current target; if it is unchanged and approved, write one merge commit (F1 steps 7–10), appending ledger entries with new sequences |
| 8 | NOD | releases effects; emits `ChangeSetMerged` |

**Invariant:** The merged state equals the approved preview result, or the merge is refused.

## F4 — Event delivery and handler execution

| # | System | Step |
|---|---|---|
| 1 | NOD | the outbox releaser reads committed effects and publishes a queue hint for each subscription match |
| 2 | NOD | a worker thread leases a delivery (job object state `leased`) |
| 3 | NOD → DVM | starts a handler process with trigger lineage and delivery ID as its idempotency key |
| 4 | DVM | the handler runs (F1 steps 4–10) |
| 5 | NOD | marks the delivery succeeded, or schedules a retry with backoff; dead-letters after the policy |
| 6 | NOD | the sweeper periodically rebuilds queue hints from job objects (FR-EVT-032) |

**Invariants:**
1. Delivery is at least once.
2. Handler effects are exactly once through idempotency.
3. Deliveries for the same object are processed in commit order.

## F5 — Schedule firing

| # | System | Step |
|---|---|---|
| 1 | NOD | the schedule leader (lease in the database) computes due slots in the schedule's time zone and calendar |
| 2 | NOD | for each slot, inserts a firing record keyed `(schedule, slot)`; a unique constraint guarantees one firing |
| 3 | NOD → DVM | starts the target process with trigger `schedule.run` |
| 4 | NOD | applies the misfire policy after outages |

**Invariant:** There is exactly one process per schedule slot (FR-EVT-043).

## F6 — Agent run

| # | System | Step |
|---|---|---|
| 1 | AGT | a trigger (schedule, event or request) starts a run for an agent definition version |
| 2 | AGT → NOD (CTR-021) | opens branch `agent/<agent>/<run>` as the agent principal |
| 3 | AGT | loop: plan, call tools (ports and queries as the agent), call models through the gateway (CTR-020 and CTR-023), within budgets |
| 4 | AGT → NOD | runs validation and merge preview on the agent branch |
| 5 | AGT → NOD | submits a change set with summary, rationale, confidence and citations |
| 6 | WSP | a supervisor reviews and accepts all, some or none (FR-VER-035) |
| 7 | DVM | merges the accepted items (F3 step 7) |
| 8 | AGT | stores feedback and metrics for evaluation |

**Invariants:**
1. No agent write reaches a protected branch without a non-agent approval, except explicitly granted ledger appends (FR-AI-031).
2. The kill switch aborts at any step (FR-AI-022).

## F7 — Buk installation or upgrade

| # | System | Step |
|---|---|---|
| 1 | NOD | obtains the archive (from EXC, CTR-054, or a file) and verifies the root hash and publisher signature |
| 2 | NOD → FRG | resolves dependencies deterministically, writes the lock record, and checks ABI compatibility |
| 3 | DVM | creates a `loader` branch from the target line and loads the content as commits |
| 4 | FRG | runs verification and scenarios in a sandbox VAE copy (FR-PKG-031) |
| 5 | STU | the administrator reviews the install plan and diff; conflicts are reported per overlay for upgrades |
| 6 | DVM | merges into the target line as a governed change set; lenses and migrations run in the same merge |
| 7 | NOD | activates schedules, subscriptions and connectors, subject to required configuration |

**Invariant:** A failed step leaves the target unchanged.

## F8 — Device sync (Box ↔ Server)

| # | System | Step |
|---|---|---|
| 1 | Box → Server (CTR-060) | authenticates the device and principal; negotiates the scope |
| 2 | both | exchange branch heads for the scope; walk Merkle trees to compute have/want |
| 3 | Server → Box | streams missing chunks and commits; the Box verifies the hashes and signatures of each batch |
| 4 | Box → Server | pushes local commits as the device branch `device/<device>/<n>` |
| 5 | Server DVM | integrates: automatic merge for `direct` classes, change set for `change_set` classes; ledger entries appended with original valid times |
| 6 | Server → Box | returns merge results, conflicts to resolve and rejections with explanations |

**Invariant:** No acknowledged local commit is lost. Rejected items stay on the device until they are resolved.

## F9 — Anchoring and evidence

| # | System | Step |
|---|---|---|
| 1 | NTY | at the schedule, requests the tenant root (CTR-030) |
| 2 | NTY → TSA (CTR-031) | timestamps the root hash |
| 3 | NTY → NOD | appends `AnchorReceipt` to the tenant's anchor ledger |
| 4 | NTY | on an evidence request: resolves the scope at bitemporal coordinates, collects versions, rule bindings, commits, inclusion and consistency proofs to anchored roots, and keys; applies provable redaction; signs the manifest |
| 5 | verifier | any party verifies offline (CTR-032) |

**Invariant:** Every evidence-pack item is provably included in an anchored root, or explicitly marked unanchored with a reason.

## F10 — Network fallback resolution (cross-node VAE tree)

| # | State | Action | Next |
|---|---|---|---|
| 1 | `LOCAL_CACHE` | look up the foreign-snapshot cache | hit → return; miss → 2 |
| 2 | `LOCAL_STORE` | resolve locally, including branch inheritance | hit → return; miss → 3 |
| 3 | `PARENT_LOOKUP` | find the parent VAE in the registry cache | no parent → `IAM.NOT_FOUND`; parent → 4 |
| 4 | `REMOTE_FETCH` | fetch from the parent over CTR-062; verify the signature and inclusion proof; cache as a foreign snapshot | success → return; unreachable → `SYNC.UPSTREAM_UNAVAILABLE` |

**Invariant:** Foreign snapshots are used only after verification (FR-SYNC-062).

## F11 — Erasure by crypto-shredding

| # | System | Step |
|---|---|---|
| 1 | WSP | a compliance officer creates an erasure request for subject S |
| 2 | DVM | checks legal holds and retention obligations; refuses with a reason if any apply |
| 3 | DVM | commits an erasure record; requests key destruction |
| 4 | NOD → KMS | destroys the subject key in the live key store; schedules expiry in key backups within the window |
| 5 | DVM | invalidates caches and projections that hold decrypted values for S; rebuilds them |
| 6 | BRG | propagates erasure markers to exports and analytical sinks |

**Invariant:** After step 4, no path in the platform can produce S's personal plaintext. After the backup window, restored backups cannot either (FR-PROOF-074).

### AR-023 — Every flow is replayable and observable
- **Rule:** Every flow in this document MUST propagate a trace ID and process lineage (NR-OBS-001), and every state-changing step MUST be attributable to a process.
- **Realises:** FR-TXN-041, FR-OPS-041
- **Rationale:** Explainability across systems.
