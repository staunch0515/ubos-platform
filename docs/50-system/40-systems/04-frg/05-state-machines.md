---
id: UBS-SYS-FRG-05
title: Logic Forge — State Machines
status: draft
phase: PH-2
depends_on: [UBS-SYS-FRG-02]
---

# Logic Forge — State Machines

## Buk version lifecycle (as seen by Forge and EXC)

| From | Event | To |
|---|---|---|
| — | `forge pack` | packed |
| packed | `forge sign` | signed |
| signed | `forge verify` passes | verified (local record) |
| signed | `forge verify` fails | rejected (findings) |
| verified | `forge publish` | submitted |
| submitted | EXC verification passes | listed |
| submitted | EXC verification fails | rejected |
| listed | deprecate | deprecated |
| deprecated | yank (security) | yanked (installs blocked, existing installs warned) |
