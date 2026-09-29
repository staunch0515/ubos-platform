---
id: UBS-SYS-EXC-05
title: Buk Exchange — State Machines
status: draft
phase: PH-4
depends_on: [UBS-SYS-EXC-02]
---

# Buk Exchange — State Machines

## Licence

| From | Event | To |
|---|---|---|
| — | purchase or grant | active |
| active | term end without renewal | grace (reads and exports unaffected; mutating ports disabled after grace) |
| grace | renewal | active |
| grace | grace end | expired |
| active | cancellation | cancelled at term end |
| any | fraud or legal revocation | revoked |

## Payout

| From | Event | To |
|---|---|---|
| — | period close | calculated |
| calculated | review passed | approved |
| approved | processor transfer | paid |
| paid | processor failure notice | failed → approved (retry) |
