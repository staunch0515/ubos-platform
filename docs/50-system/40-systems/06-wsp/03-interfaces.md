---
id: UBS-SYS-WSP-03
title: Workspace — Interfaces
status: draft
phase: PH-2
depends_on: [UBS-SYS-WSP-02]
---

# Workspace — Interfaces (IF-WSP-*)

### IF-WSP-001 — Renderer contract
- **Kind:** library API
- **Caller → Callee:** view payload → WSP renderer
- **Request:** `ViewPayload { view_id, catalogue_version, layout: tree of nodes { widget, binding, state: visible | hidden | read_only | required, label_key, children } , data, actions[], violations[] }`
- **Response:** rendered UI
- **Errors:** unknown widget → generic fallback
- **Idempotency:** deterministic rendering for identical payload and locale.
- **Authorization:** already applied in the payload.
- **Satisfies:** FR-UX-011, FR-UX-014, FR-UX-073

### IF-WSP-002 — Widget plug-in API
- **Kind:** library API
- **Caller → Callee:** Buk-provided widget → WSP
- **Request:** a Web Component registered with a catalogue extension name, a props schema and accessibility metadata
- **Response:** rendered element
- **Errors:** schema mismatch → fallback
- **Idempotency:** —
- **Authorization:** Buk widgets run in an isolated iframe or shadow realm without SDK credentials; data is passed as props only.
- **Satisfies:** FR-UX-014, NR-SEC-002

### IF-WSP-003 — Deep links
- **Kind:** HTTP endpoint
- **Caller → Callee:** external link → WSP
- **Request:** `/w/{vae}/{class}/{id}?branch=&asof=&time=&view=`
- **Response:** the resolved screen
- **Errors:** not found screen (no existence leak)
- **Idempotency:** pure.
- **Authorization:** user session.
- **Satisfies:** FR-UX-021, STD-ADDR-001
