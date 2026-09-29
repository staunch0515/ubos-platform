---
id: UBS-SYS-NOD-05
title: UBOS Node — State Machines
status: draft
phase: PH-1
depends_on: [UBS-SYS-NOD-02]
---

# UBOS Node — State Machines

## 1. Session

| From | Event | To |
|---|---|---|
| — | authentication success | active |
| active | request within timeouts | active (idle timer reset) |
| active | step-up success | active (higher level) |
| active | idle or absolute timeout | expired |
| active | revocation (user, admin, SCIM, device wipe) | revoked |
| expired, revoked | — | terminal |

## 2. Job

| From | Event | To |
|---|---|---|
| — | enqueue | ready |
| ready | claim | leased |
| leased | heartbeat | leased (lease extended) |
| leased | success commit with valid token | succeeded |
| leased | failure, attempts < max, not poison | ready (run_after = back-off) |
| leased | failure, attempts = max or poison | dead |
| leased | lease expiry | ready |
| ready, leased | cancel | cancelled |
| ready | pause queue | paused |
| paused | resume | ready |
| dead | operator retry | ready |

## 3. Schedule slot

| From | Event | To |
|---|---|---|
| — | slot due, leader inserts `(schedule, slot)` | recorded |
| recorded | job enqueued in the same transaction | fired |
| — | schedule paused at due time | skipped |
| skipped | resume with catch-up policy | fired or skipped (per policy) |

## 4. Leadership lease

| From | Event | To |
|---|---|---|
| — | acquire (no holder or expired) | held (fencing + 1) |
| held | renew before expiry | held |
| held | renewal missed | expired |
| expired | another node acquires | held by new node |
| held | graceful release | free |

## 5. Sync session (Box ↔ Server)

| From | Event | To |
|---|---|---|
| — | connect and authenticate device | heads |
| heads | heads differ | havewant |
| heads | heads equal | done |
| havewant | missing sets computed | transfer |
| transfer | all packs received and verified | integrate |
| transfer | connection lost | suspended (resumable) |
| suspended | reconnect | transfer (from last chunk) |
| integrate | merge succeeded or change set opened | rejections |
| rejections | rejection records delivered | done |
| any | wipe instruction pending | wipe → done |
| any | integrity failure | failed (nothing applied) |

## 6. Tenant

| From | Event | To |
|---|---|---|
| — | create | provisioning |
| provisioning | all steps done | active |
| active | suspend | suspended |
| suspended | resume | active |
| active, suspended | archive | archived (read-only) |
| archived | restore | active |
| archived, suspended | delete request | deleting (grace period) |
| deleting | grace elapsed, no hold | deleted (KEK destroyed, purge scheduled) |
| deleting | cancel within grace | previous state |
| active | migration start | migrating (writes allowed) |
| migrating | cut-over pause | paused (writes blocked ≤ NR-AVAIL-004) |
| paused | cut-over complete | active (new cell) |

## 7. Node upgrade step (per node)

| From | Event | To |
|---|---|---|
| running (old) | drain | draining |
| draining | connections closed or timeout | stopped |
| stopped | start new version | starting |
| starting | health checks pass within watch window | running (new) |
| starting | health checks fail | rolling back |
| rolling back | old version healthy | running (old) |

## 8. Worker

| From | Event | To |
|---|---|---|
| — | registration with attestation | active |
| active | sampled mismatch, repeated failure or expired attestation | quarantined |
| quarantined | operator release with new attestation | active |
| active | deregister | retired |
