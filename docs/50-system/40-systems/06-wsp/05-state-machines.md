---
id: UBS-SYS-WSP-05
title: Workspace — State Machines
status: draft
phase: PH-2
depends_on: [UBS-SYS-WSP-02]
---

# Workspace — State Machines

## 1. Form

| From | Event | To |
|---|---|---|
| — | open | viewing |
| viewing | edit (permitted) | editing (draft autosave on) |
| editing | change | validating → editing (violations shown) |
| editing | submit | submitting |
| submitting | committed | viewing (new version) |
| submitting | deferred | pending approval |
| submitting | conflict (`TXN.HEAD_MOVED`) | merging (show both versions, reapply edits) |
| submitting | rejected | editing (errors shown) |

## 2. Signing ceremony

| From | Event | To |
|---|---|---|
| — | prepare (document rendered, sealed draft hash) | prepared |
| prepared | send | in progress |
| in progress | signer signs | in progress (next signers notified per order) |
| in progress | all signed | completing |
| completing | seal committed | executed |
| in progress | document or data changed | voided |
| in progress | decline | declined |
| in progress | expiry | expired |

## 3. Negotiation round

| From | Event | To |
|---|---|---|
| — | open round | internal drafting |
| internal drafting | send to counterparty | counterparty review |
| counterparty review | counterparty returns changes | internal review |
| internal review | accept all | agreed |
| internal review | counter-propose | counterparty review (next round) |
| agreed | start signing | signing ceremony |

## 4. Offline action (desktop, mobile)

| From | Event | To |
|---|---|---|
| — | action while offline | queued (committed on device branch) |
| queued | sync integrates | done |
| queued | sync rejects | needs attention (conflict screen) |
| needs attention | user resolves | queued or discarded |
