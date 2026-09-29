---
id: UBS-SYS-BRG-07
title: Bridge — Operations
status: draft
phase: PH-2
depends_on: [UBS-SYS-BRG-02]
---

# Bridge — Operations

BRG runs in-process with NOD on small Servers and as separate workers on HA Servers and
Cells. Operators monitor import run failures, adapter backlogs and export lag (watermark
age). Failed ranges retry automatically; persistent failures raise alerts with the adapter
and range.
