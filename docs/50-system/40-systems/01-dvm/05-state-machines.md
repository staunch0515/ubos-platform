---
id: UBS-SYS-DVM-05
title: DVM — State Machines
status: draft
phase: PH-1
depends_on: [UBS-SYS-DVM-02]
---

# DVM — State Machines

Each machine is given as a transition table. A transition not listed is illegal and MUST
be rejected. "Actor" names the component or principal that may trigger it.

## 1. Process (DSN-DVM-501)

| From | Event | Guard | To | Actor | Effect |
|---|---|---|---|---|---|
| — | `begin_process` | context valid, branch writable or read-only mode | created | host | snapshot map empty |
| created | first instruction | — | running | runtime | snapshot fixed per branch on first read |
| running | `commit` | no staged writes | committed (empty) | host | metering stored, no commit |
| running | `commit` | staged writes | staged | host | pre-lock validation |
| staged | pre-lock validation fails | — | aborted | txn | violations returned |
| staged | decision returns requires | — | deferred | txn | `PendingChange` written |
| staged | enqueued | — | flushing | sequencer | waits for lock |
| flushing | CAS and validation pass, storage commit | — | committed | sequencer | commit visible |
| flushing | `TXN.HEAD_MOVED` or `TXN.READ_SET_CHANGED`, re-executable, attempts < 5 | — | running | txn | new snapshot, re-run |
| flushing | conflict, not re-executable or attempts exhausted | — | aborted | txn | error returned |
| running, staged | limit, cancel, sandbox fault | — | aborted | runtime | staging discarded |
| flushing | storage failure | — | aborted | sequencer | `NOD.STORAGE_UNAVAILABLE` |

Terminal states: committed, aborted, deferred.

## 2. Pending change (DSN-DVM-550)

| From | Event | Guard | To | Actor |
|---|---|---|---|---|
| — | process deferred | — | awaiting | txn |
| awaiting | approval | approvals complete, preview hash current | applying | approver |
| awaiting | approval | approvals incomplete | awaiting | approver |
| applying | bases unchanged, flush succeeds | — | applied | txn |
| applying | bases changed, re-run gives same preview | — | applied | txn |
| applying | bases changed, preview differs | — | awaiting (approvals stale) | txn |
| awaiting | reject | — | rejected | approver |
| awaiting | withdraw | requester | withdrawn | requester |
| awaiting | expiry | age > expiry | expired | sweep |

## 3. Change set (STD-VER-020)

| From | Event | Guard | To |
|---|---|---|---|
| — | open | source ≠ target | draft |
| draft | submit | preview has no unresolved conflict of type validation | in review |
| in review | source or target change | preview hash changes | in review (approvals stale, drifted flag if conflicts) |
| in review | approve | approvals complete for current hash | approved |
| approved | source or target change | preview hash changes | in review |
| approved | merge | approvals valid, merge succeeds | merged |
| approved | merge | conflict | approved (merge error returned) |
| in review, approved | reject | — | rejected |
| draft, in review | withdraw | author | abandoned |
| merged | revert | — | merged (revert commit linked) |

## 4. Branch (DSN-DVM-207)

| From | Event | Guard | To |
|---|---|---|---|
| — | create | name valid, depth ≤ limit | active |
| active | freeze | `branch.admin` | frozen |
| frozen | unfreeze | `branch.admin` | active |
| active, frozen | merged into target | work or agent kind, policy "archive on merge" | merged |
| active, frozen, merged | archive | — | archived |
| archived | restore | — | active |
| active, frozen, merged, archived | delete | not `main`, no dependent branches | deleted-soft |
| deleted-soft | purge | retention expired, no hold | — (refs removed, commits kept while reachable) |

## 5. Logic asset (DSN-DVM-640)

| From | Event | Guard | To |
|---|---|---|---|
| — | create | — | draft |
| draft | verification record attached | record valid for asset hash | verified |
| verified | activate | on protected branch: verified | active |
| draft | activate | only on work, simulation or agent branches | active (unprotected scope) |
| active | deprecate | — | deprecated |
| deprecated | retire | no active callers or forced | retired |
| active, deprecated | failure rate over threshold | — | quarantined |
| quarantined | release | governed change set | active |
| any non-retired | new version | — | (new object version starts in draft) |

## 6. Bulk process (DSN-DVM-560)

| From | Event | To |
|---|---|---|
| — | start | running |
| running | batch committed | running (checkpoint advanced) |
| running | node failure | suspended |
| suspended | resume | running (from checkpoint) |
| running | cancel | cancelled (committed batches stay) |
| running | last batch | completed |
| running | batch error, policy stop | failed |
| running | batch error, policy skip | running (error recorded) |

## 7. Saga (DSN-DVM-564)

| From | Event | To |
|---|---|---|
| — | start | running (step 1) |
| running (step k) | step k committed | running (step k+1) or completed |
| running (step k) | step k failed or timed out | compensating (step k−1) |
| compensating (step j) | compensation j committed | compensating (step j−1) or compensated |
| compensating | compensation failed after retries | stuck (escalated) |
| stuck | manual resolution | compensated or completed |

## 8. Period (DSN-DVM-442)

| From | Event | Guard | To |
|---|---|---|---|
| — | create | — | open |
| open | soft close | — | soft-closed |
| soft-closed | close | close checklist complete | closed (close artefact sealed) |
| soft-closed | reopen | — | open |
| closed | reopen | approved change set | open (artefact superseded) |

## 9. Outbox row (DSN-DVM-534)

| From | Event | To |
|---|---|---|
| — | commit | ready |
| ready | claim | claimed (lease) |
| claimed | ack | released |
| claimed | lease expiry | ready (attempts + 1) |
| ready | attempts > max | dead-lettered (operator alert) |
