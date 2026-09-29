---
id: UBS-SYS-EXC-01
title: Buk Exchange — Context
status: draft
phase: PH-4
depends_on: [UBS-SYS-EXC-00, UBS-ARC-02]
---

# Buk Exchange — Context

| Neighbour | Direction | Contract | What flows |
|---|---|---|---|
| FRG | FRG → EXC | CTR-015 | signed archives, verification requests, deprecations |
| NOD | NOD → EXC | CTR-054 | search, archive, verification record, licence retrieval |
| CTL | CTL ↔ EXC | CTR-053 | entitlements, licence status, invoice lines |
| payment processor | EXC → processor | provider API | card and ACH payments, payouts (fiat only) |
