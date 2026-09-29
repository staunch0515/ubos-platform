---
id: UBS-SYS-BPA-05
title: BPA Standard Governance — State Machines
status: draft
phase: PH-0
depends_on: [UBS-SYS-BPA-02]
---

# BPA Standard Governance — State Machines

## Change proposal

| From | Event | To |
|---|---|---|
| — | submit | draft |
| draft | vectors and clause diff complete | in comment |
| in comment | period elapsed, steward accepts | accepted |
| in comment | steward rejects | rejected |
| accepted | included in a release | released |
| released | defect found | errata issued (new patch release) |

## Certificate

| From | Event | To |
|---|---|---|
| — | reproduction passes | valid |
| valid | 24 months or next major | expired |
| valid | defect or misrepresentation | revoked |
| expired | recertification passes | valid (new certificate) |
