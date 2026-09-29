---
id: UBS-SYS-BPA-02
title: BPA Standard Governance — Design Requirements
status: draft
phase: PH-0
depends_on: [UBS-SYS-BPA-01]
---

# BPA Standard Governance — Design Requirements (DSN-BPA-*)

### DSN-BPA-001 — Standard repository and release format
- **Statement:** The standard MUST be maintained in the `bpa-standard` repository (DEC-018) as Markdown clauses with stable IDs, JSON Schemas for all DAT structures, ABNF grammars, vectors and a release manifest. Each release MUST be tagged, signed, and published with a content hash (FR-STD-011).
- **Rationale:** FR-STD-011, FR-STD-013.
- **Priority:** Must · **Phase:** PH-0 · **Systems:** BPA
- **Personas:** PER-StandardsSteward
- **Acceptance:**
  1. Given a release, when its manifest is verified, then every file hash matches, and the signature verifies.
- **Verification:** INSP, SEC
- **Origin:** FR-STD-011, FR-STD-013, DEC-018

### DSN-BPA-002 — Profiles
- **Statement:** Releases MUST define profiles (Core, Bitemporal, Ledger, Governance, Proof, Sync) with the clauses and vectors in each, and dependencies between profiles (FR-STD-014).
- **Rationale:** FR-STD-014, STD-CONF-004.
- **Priority:** Must · **Phase:** PH-0 · **Systems:** BPA
- **Personas:** PER-ThirdPartyImplementer
- **Acceptance:**
  1. Given the release manifest, when a profile is selected, then its clause and vector lists are complete and closed under dependencies.
- **Verification:** INSP
- **Origin:** FR-STD-014, STD-CONF-004

### DSN-BPA-003 — Vector coverage of MUST clauses
- **Statement:** Every MUST clause of a released profile MUST be covered by at least one positive and, where a failure mode exists, one negative vector (FR-STD-022). The release pipeline MUST block releases with uncovered clauses.
- **Rationale:** FR-STD-022.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** BPA, FRG
- **Personas:** PER-StandardsSteward
- **Acceptance:**
  1. Given a new MUST clause without vectors, when a release is attempted, then the pipeline fails naming the clause.
- **Verification:** INSP
- **Origin:** FR-STD-022, STD-CONF-001

### DSN-BPA-004 — Formal models and alignment
- **Statement:** Core semantics (commit and CAS, merge, timelines, sync, shared BPUs, sagas, approvals) MUST have formal models (TLA+ or equivalent) in `ubos-verify` (FR-STD-031); traces generated from the models MUST be converted into vectors, and vectors MUST be replayable against the models (FR-STD-032).
- **Rationale:** FR-STD-031, FR-STD-032.
- **Priority:** Must · **Phase:** PH-0 · **Systems:** BPA
- **Personas:** PER-KernelEngineer
- **Acceptance:**
  1. Given the merge model, when 1,000 model traces are converted to vectors, then the reference DVM passes all of them.
- **Verification:** FORM, CONF
- **Origin:** FR-STD-031, FR-STD-032

### DSN-BPA-005 — Change process and errata
- **Statement:** Changes MUST follow STD-CONF-030: proposals with rationale, clause diffs and vectors; classification as patch, minor or major; public comment period (14 days for minor, 60 days for major); errata referencing clauses and vectors (FR-STD-012).
- **Rationale:** FR-STD-011, FR-STD-012, STD-CONF-030.
- **Priority:** Must · **Phase:** PH-1 · **Systems:** BPA
- **Personas:** PER-StandardsSteward
- **Acceptance:**
  1. Given a minor proposal, when accepted, then it is released only with its vectors after the comment period.
- **Verification:** INSP
- **Origin:** FR-STD-011, FR-STD-012, STD-CONF-030

### DSN-BPA-006 — Compatibility policy
- **Statement:** Within a major version, ABI, canonical formats, content IDs and storage formats MUST remain compatible (FR-STD-041, FR-STD-042); minor releases MAY only add. Deprecations MUST be announced one minor release before removal in the next major.
- **Rationale:** FR-STD-041, FR-STD-042, NR-COMPAT-001, NR-COMPAT-003.
- **Priority:** Must · **Phase:** PH-0 · **Systems:** BPA
- **Personas:** PER-ThirdPartyImplementer
- **Acceptance:**
  1. Given a minor release, when its vectors from the previous minor are run, then all still pass.
- **Verification:** CONF
- **Origin:** FR-STD-041, FR-STD-042, NR-COMPAT-001, NR-COMPAT-003, NR-COMPAT-004

### DSN-BPA-007 — Extension and algorithm registries
- **Statement:** The steward MUST operate the extension registry (STD-CONF-010, FR-STD-071) and the algorithm registry (STD-CONF-011), each entry with schema, vectors and owner; extensions MAY be promoted into the standard through the change process (FR-STD-072).
- **Rationale:** FR-STD-043, FR-STD-071, FR-STD-072.
- **Priority:** Must · **Phase:** PH-2 · **Systems:** BPA
- **Personas:** PER-StandardsSteward
- **Acceptance:**
  1. Given an extension without vectors, when registration is requested, then it is refused.
- **Verification:** INSP
- **Origin:** FR-STD-043, FR-STD-071, FR-STD-072, STD-CONF-010, STD-CONF-011

### DSN-BPA-008 — Certification programme
- **Statement:** Certification MUST follow STD-CONF-020: signed report submission, reproduction by the steward on a certification environment from the same source, certificate issue for passing profiles, public registry, validity of at most 24 months, recertification on major releases (FR-STD-051, FR-STD-053), and certificate verification by anyone (FR-STD-052).
- **Rationale:** FR-STD-051, FR-STD-052, FR-STD-053.
- **Priority:** Must · **Phase:** PH-4 · **Systems:** BPA
- **Personas:** PER-ThirdPartyImplementer
- **Acceptance:**
  1. Given a submitted report whose reproduction fails one vector, when reviewed, then no certificate is issued for that profile.
- **Verification:** INSP, CONF
- **Origin:** FR-STD-051, FR-STD-052, FR-STD-053, STD-CONF-020

### DSN-BPA-009 — Reference implementation licensing
- **Statement:** The reference DVM MUST be embeddable by third parties (FR-STD-061) under the licence set of DEC-019 (open edition AGPL-3.0 plus commercial licence; SDKs and verifier Apache-2.0; specifications CC BY 4.0), and licence terms metadata MUST be machine-readable in releases (FR-STD-062).
- **Rationale:** FR-STD-061, FR-STD-062, DEC-019.
- **Priority:** Must · **Phase:** PH-4 · **Systems:** BPA, DVM
- **Personas:** PER-ThirdPartyImplementer
- **Acceptance:**
  1. Given a release, when its licence metadata is read, then each artefact names its licence.
- **Verification:** INSP
- **Origin:** FR-STD-061, FR-STD-062, DEC-019

### DSN-BPA-010 — Standard before code
- **Statement:** Kernel changes that alter observable semantics MUST merge only after the corresponding standard change is accepted and its vectors exist; the kernel CI MUST run the latest accepted vectors (AR-033).
- **Rationale:** AR-033.
- **Priority:** Must · **Phase:** PH-0 · **Systems:** BPA, DVM
- **Personas:** PER-KernelEngineer
- **Acceptance:**
  1. Given a kernel pull request changing merge semantics without an accepted standard change, when checked, then the policy check fails.
- **Verification:** INSP
- **Origin:** AR-033
