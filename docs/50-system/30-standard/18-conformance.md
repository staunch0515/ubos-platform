---
id: UBS-STD-18
title: BPA Standard — Conformance, Profiles, Extensions and Certification (CONF)
status: draft
phase: PH-0
depends_on: [UBS-STD-00]
---

# BPA Standard — Conformance (CONF)

### STD-CONF-001 — Conformance vector format
- **Clause:** A conformance vector MUST be a canonical JSON document (`DAT-ConformanceVector`) that is independent of implementation language. Vectors are grouped by clause and profile, and packaged with a signed manifest per standard release.
- **Grammar / Schema:** `DAT-ConformanceVector`
- **Conformance vectors:** —
- **Satisfies:** FR-STD-021, FR-STD-022
- **Notes:** —

```yaml
DAT-ConformanceVector:
  id: { type: text, required: true, description: "VER-CONF-nnnn" }
  clauses: { type: list, required: true, description: "STD clause ids exercised" }
  profile: { type: text, required: true, description: "profile name" }
  polarity: { type: enum, required: true, description: "positive | negative" }
  setup: { type: list, required: true, description: "ordered adapter operations establishing the initial state (genesis, classes, objects)" }
  steps: { type: list, required: true, description: "ordered operations with expected results {op, input, expect: {result | error_code, match: exact|subset|hash}}" }
  determinism: { type: composite, required: false, description: "fixed now, entropy seed, tzdb version" }
  description: { type: text, required: true, description: "what is checked" }
```

### STD-CONF-002 — Adapter protocol
- **Clause:** Implementations are driven by the runner through an adapter speaking UBTP (STD-PROTO) or an in-process adapter with the same message structures. The adapter MUST additionally support the test-only operations `reset`, `set_now`, `set_entropy_seed`, `load_genesis` and `export_state`. It MUST be disabled in production builds.
- **Grammar / Schema:** —
- **Conformance vectors:** —
- **Satisfies:** FR-STD-021, CTR-014
- **Notes:** —

### STD-CONF-003 — Runner report
- **Clause:** The runner MUST produce `DAT-ConformanceReport`, signed by the runner key, listing every vector with its outcome (`pass`, `fail`, `not_applicable` outside the claimed profiles) and failure details.
- **Grammar / Schema:** `DAT-ConformanceReport`
- **Conformance vectors:** —
- **Satisfies:** FR-STD-023
- **Notes:** —

```yaml
DAT-ConformanceReport:
  standard_version: { type: text, required: true, description: "BPA version" }
  implementation: { type: composite, required: true, description: "{name, vendor, version, build_hash, backends}" }
  profiles_claimed: { type: list, required: true, description: "profile names" }
  results: { type: list, required: true, description: "{vector, outcome, detail}" }
  summary: { type: map, required: true, description: "counts per profile and outcome" }
  environment: { type: map, required: true, description: "OS, CPU, database versions" }
  signature: { type: composite, required: true, description: "DAT-Signature (runner)" }
```

### STD-CONF-004 — Profile conformance
- **Clause:** An implementation conforms to a profile when every vector of the profile passes and every MUST clause of the profile is implemented. Claims MUST name the standard version and profiles, and SHOULD reference a public report.
- **Grammar / Schema:** —
- **Conformance vectors:** —
- **Satisfies:** FR-STD-014, FR-STD-051
- **Notes:** —

### STD-CONF-010 — Extensions
- **Clause:** Vendor extensions (instructions, value types, properties, widgets, error codes) MUST use names prefixed `x-<vendor>.`, MUST be registered in the extension registry with schema and vectors, and MUST NOT change the semantics of standard items. Standard names without the prefix are reserved.
- **Grammar / Schema:** —
- **Conformance vectors:** —
- **Satisfies:** FR-STD-043, FR-STD-071
- **Notes:** —

### STD-CONF-011 — Algorithm registry
- **Clause:** Algorithm identifiers for hashes, signatures and AEAD are registered by the standard. Version 1.0 registers `sha256`, `ed25519`, `aes-256-gcm` and `xchacha20-poly1305`. Adding an algorithm is a minor release. Implementations MUST verify content produced with every registered algorithm of their major version.
- **Grammar / Schema:** —
- **Conformance vectors:** VER-CONF-6200…6209
- **Satisfies:** FR-PROOF-012, NR-SEC-004
- **Notes:** —

### STD-CONF-020 — Certification procedure
- **Clause:** Certification MUST follow these steps:
  1. the vendor submits the signed report, the claimed profiles, the extension list and the build identity;
  2. the steward reproduces the run on a certification environment with a build from the same source;
  3. a certificate (`DAT-Certificate`) is issued for the passing profiles, signed by the steward, listed publicly and valid for at most 24 months or until the next major version.
- **Grammar / Schema:** `DAT-Certificate`
- **Conformance vectors:** —
- **Satisfies:** FR-STD-051, FR-STD-052, FR-STD-053, SCN-407
- **Notes:** —

```yaml
DAT-Certificate:
  certificate_id: { type: text, required: true, description: "UUID" }
  implementation: { type: composite, required: true, description: "{name, vendor, version, build_hash}" }
  standard_version: { type: text, required: true, description: "BPA version" }
  profiles: { type: list, required: true, description: "certified profiles" }
  issued_at: { type: instant, required: true, description: "time" }
  expires_at: { type: instant, required: true, description: "time" }
  report: { type: text, required: true, description: "content-id of the reproduced report" }
  status: { type: enum, required: true, description: "valid | revoked | expired" }
  signature: { type: composite, required: true, description: "DAT-Signature (standards steward)" }
```

### STD-CONF-030 — Standard change process
- **Clause:** Every change to the standard MUST be proposed with rationale, clause diffs and new or changed vectors. Changes are classified as `patch` (errata, clarifications without behaviour change), `minor` (additive) or `major` (breaking). A change is published only with its vectors, and an erratum MUST reference the clause and vectors affected.
- **Grammar / Schema:** —
- **Conformance vectors:** —
- **Satisfies:** FR-STD-011, FR-STD-012, AR-033
- **Notes:** —
