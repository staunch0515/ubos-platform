---
id: UBS-SYS-STU-05
title: Studio — State Machines
status: draft
phase: PH-2
depends_on: [UBS-SYS-STU-02]
---

# Studio — State Machines

## Builder change flow

| From | Event | To |
|---|---|---|
| — | start change | editing (work branch) |
| editing | save | editing (commit) |
| editing | run checks | checked (findings) |
| checked | fix and save | editing |
| checked | all green, open change set | in review |
| in review | reviewer requests changes | editing |
| in review | approved | approved |
| approved | merge | released to target |
| released to target | tag release | released |
