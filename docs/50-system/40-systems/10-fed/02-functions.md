---
id: UBS-SYS-FED-02
title: Federation — Design Requirements
status: draft
phase: PH-5
depends_on: [UBS-SYS-FED-01]
---

# Federation — Design Requirements (DSN-FED-*)

### DSN-FED-001 — Signed VAE registrations
- **Statement:** A VAE registration MUST bind a `did:ubos` identifier to endpoints, public keys and the operating organisation, signed by the VAE's controller key (FR-SYNC-051). Updates MUST be signed by a key valid in the current key history.
- **Rationale:** FR-SYNC-051, STD-ADDR-010.
- **Priority:** Must · **Phase:** PH-5 · **Systems:** FED
- **Personas:** PER-TenantAdministrator
- **Acceptance:**
  1. Given a registration update signed by a revoked key, when submitted, then it is rejected.
- **Verification:** SEC
- **Origin:** FR-SYNC-051, STD-ADDR-010

### DSN-FED-002 — Resolution and discovery
- **Statement:** Resolution MUST work through the registry's HTTPS API and a DNS TXT record fallback (`_ubos.<domain>`), with caching by TTL; nodes MUST keep operating with cached resolutions during registry outages (FR-SYNC-052).
- **Rationale:** FR-SYNC-052.
- **Priority:** Must · **Phase:** PH-5 · **Systems:** FED
- **Personas:** PER-PlatformOperator
- **Acceptance:**
  1. Given the registry offline for 6 hours, when partners sync, then cached resolutions are used.
- **Verification:** SIM, SEC
- **Origin:** FR-SYNC-052

### DSN-FED-003 — Trust relationships
- **Statement:** Trust relationships (FR-SYNC-053) MUST be bilateral objects proposed by one party and accepted by the other, stating allowed interactions (read scopes, shared classes, remote parent use), validity and termination rules. Both parties MUST hold a signed copy in their VAE.
- **Rationale:** FR-SYNC-053.
- **Priority:** Must · **Phase:** PH-5 · **Systems:** FED
- **Personas:** PER-ComplianceOfficer
- **Acceptance:**
  1. Given an interaction outside the relationship, when attempted, then the receiving node refuses it and logs a security event.
- **Verification:** SEC
- **Origin:** FR-SYNC-053, FR-SYNC-054

### DSN-FED-004 — Transport security
- **Statement:** Cross-organisation connections MUST use mutual TLS with certificates bound to the parties' DIDs and TLS 1.3 only (FR-SYNC-054).
- **Rationale:** FR-SYNC-054, NR-SEC-004.
- **Priority:** Must · **Phase:** PH-5 · **Systems:** FED, NOD
- **Personas:** PER-SecurityOfficer
- **Acceptance:**
  1. Given a peer presenting a certificate for another DID, when connecting, then the connection is refused.
- **Verification:** SEC
- **Origin:** FR-SYNC-054, NR-SEC-004

### DSN-FED-005 — Remote parent VAEs
- **Statement:** A VAE MAY have a parent on another organisation's node (FR-SYNC-061) under a publication policy (FR-SYNC-064); the FED module MUST run the fallback state machine (STD-SYNC-010, DSN-NOD-508) so the child keeps working when the parent is unreachable (FR-SYNC-063).
- **Rationale:** FR-SYNC-061, FR-SYNC-062, FR-SYNC-063, FR-SYNC-064.
- **Priority:** Must · **Phase:** PH-5 · **Systems:** FED, NOD
- **Personas:** PER-TenantAdministrator
- **Acceptance:**
  1. Given a franchise child VAE and the franchisor parent offline, when the child operates, then local processes succeed with staleness markers.
- **Verification:** SIM
- **Origin:** FR-SYNC-061, FR-SYNC-062, FR-SYNC-063, FR-SYNC-064, STD-SYNC-010

### DSN-FED-006 — Shared BPUs
- **Statement:** Shared objects (FR-SYNC-071) MUST have a shared history replicated to each party; Definition changes MUST be proposals that take effect only with the signatures required by the share's policy (FR-SYNC-072); concurrent proposals MUST be ordered deterministically and conflicting ones rejected (FR-SYNC-073); parties MAY append ledger entries to their own sub-ledgers (FR-SYNC-074); ending a share MUST leave each party a verifiable final copy (FR-SYNC-075).
- **Rationale:** FR-SYNC-071…075, STD-SYNC-011, STD-SYNC-012.
- **Priority:** Must · **Phase:** PH-5 · **Systems:** FED, DVM
- **Personas:** PER-ExternalParty
- **Acceptance:**
  1. Given a shared contract between three parties requiring all signatures, when two sign, then the change is pending; after the third, it takes effect on all nodes with equal roots.
- **Verification:** SIM, FORM
- **Origin:** FR-SYNC-071, FR-SYNC-072, FR-SYNC-073, FR-SYNC-074, FR-SYNC-075, STD-SYNC-011, STD-SYNC-012

### DSN-FED-007 — Aggregated queries across children
- **Statement:** Parents MAY run aggregated queries over children across nodes when trust allows (FR-TEN-043); children MUST compute aggregates locally and return only aggregates.
- **Rationale:** FR-TEN-043, privacy by design.
- **Priority:** Should · **Phase:** PH-5 · **Systems:** FED
- **Personas:** PER-Executive
- **Acceptance:**
  1. Given an aggregate request, when answered, then no row-level data leaves the child node.
- **Verification:** SEC
- **Origin:** FR-TEN-043, CR-GDPR-001

### DSN-FED-008 — Registry neutrality
- **Statement:** The registry MUST store only identifiers, endpoints, public keys and trust metadata, never business data, and MUST be operable by multiple independent operators replicating signed records.
- **Rationale:** AR-005, AR-006.
- **Priority:** Must · **Phase:** PH-5 · **Systems:** FED
- **Personas:** PER-StandardsSteward
- **Acceptance:**
  1. Given two registry operators, when records replicate, then clients verify them by signature regardless of source.
- **Verification:** INSP, CONF
- **Origin:** AR-005, AR-006
