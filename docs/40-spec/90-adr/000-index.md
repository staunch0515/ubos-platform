---
id: SPEC-90
title: "Architecture Decision Records — Index"
status: complete
phase: P3
depends_on: [SPEC-00, SPEC-02]
sources: []
---

# Architecture Decision Records — Index

An ADR records a decision that fixes or changes a verdict (SPEC-00 §6). Format: TEMPLATES §D.
ADRs are never deleted. A replaced ADR gets status `superseded by ADR-NNN`.

| ADR | Title | Status | Decides | Origin |
|---|---|---|---|---|
| ADR-001 | Technology stack | accepted | Rust kernel, Rhai, PostgreSQL/SQLite, Redis, React/TS, Tauri | Q-001, VRD-20-01…05 |
| ADR-002 | Rule expression language | accepted | Restricted Rhai subset for invariants, guards and conditions | Q-007, VRD-09-04, VRD-09-06 |
| ADR-003 | Client set | accepted | Five clients: web, desktop, mobile, CLI, SDK/agent; IDE integration is tooling | Q-008, VRD-19-04, VRD-14-03 |
| ADR-004 | Technical state | accepted | Jobs as entities; idempotency keys in the runtime store with TTL | Q-005, VRD-01-06, VRD-04-04, VRD-11-03 |
| ADR-005 | Tenant-qualified context authority | accepted | URI authority `tenant[.context]` instead of a global context slug | VRD-03-03, SPEC-12 |
