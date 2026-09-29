---
id: UBS-SYS-AGT-06
title: Agent Hub — Configuration
status: draft
phase: PH-3
depends_on: [UBS-SYS-AGT-02]
---

# Agent Hub — Configuration

| Setting | Level | Default | Design |
|---|---|---|---|
| model class → provider routes | tenant, region | platform defaults | DSN-AGT-001 |
| allowed classification per provider | platform, tenant | internal | DSN-AGT-002 |
| redaction policy | tenant | block personal, tokenise identifiers | DSN-AGT-002 |
| AI budget and caps | tenant (CTL) | plan | DSN-AGT-003 |
| call recording retention | tenant | 365 days | DSN-AGT-004 |
| agent step limit | agent definition | 50 | DSN-AGT-104 |
| kill switch latency target | platform | NR-PERF-019 | DSN-AGT-106 |
| evaluation thresholds | agent definition | per suite | DSN-AGT-501 |
