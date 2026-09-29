---
id: UBS-REQ-05-15
title: Functional Requirements — SYNC (Replication, Offline, Federation, Browser Kernel)
status: complete
phase: PH-3
depends_on: [UBS-REQ-05, UBS-REQ-05-02, UBS-REQ-05-14]
---

# Functional Requirements — SYNC

Sync builds on content addressing (IMP-03). Two nodes exchange only the objects the other
lacks, and they verify everything they receive (IMP-07).

## CAP-SYNC-01 — Object exchange protocol

### FR-SYNC-011 — Have and want exchange
- **Statement:** Two nodes MUST synchronise a replicated scope as follows:
  1. exchange branch heads;
  2. compute missing objects by walking Merkle structures (have and want);
  3. transfer the missing chunks and commits in resumable batches.
- **Rationale:** Efficient, verifiable replication (IMP-07).
- **Priority:** Must · **Phase:** PH-3 · **Systems:** DVM, NOD, BPA
- **Personas:** PER-KernelEngineer
- **Acceptance:**
  1. Given two replicas differing by 100 objects out of 10,000,000, when synchronised, then the transfer volume is proportional to the 100 objects.
- **Verification:** CONF, BENCH
- **Origin:** IMP-07, IMP-03

### FR-SYNC-012 — Integrity on receipt
- **Statement:** Every received chunk, object version and commit MUST be verified (hash and signature) before it becomes visible. Invalid data MUST be rejected, and the peer reported.
- **Rationale:** Zero-trust replication (EXT-RFC 5.3).
- **Priority:** Must · **Phase:** PH-3 · **Systems:** DVM
- **Personas:** PER-SecurityOfficer
- **Acceptance:**
  1. Given a peer sending a corrupted chunk, when received, then it is rejected, and sync fails with `SYNC.INTEGRITY_FAILURE`.
- **Verification:** SEC, FAULT
- **Origin:** EXT-RFC

### FR-SYNC-013 — Authorised scope
- **Statement:** A node MUST send only objects within the replication scope that the receiving principal and device are cleared for.
- **Rationale:** Replication is a read channel.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** NOD
- **Personas:** PER-SecurityOfficer
- **Acceptance:**
  1. Given a device without `confidential` clearance, when synchronised, then no confidential objects are sent.
- **Verification:** SEC
- **Origin:** NEW

### FR-SYNC-014 — Resumable and idempotent
- **Statement:** Interrupted syncs MUST resume from the last acknowledged batch, and repeated batches MUST have no additional effect.
- **Rationale:** SCN-401 A1.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** NOD
- **Personas:** PER-FieldWorker
- **Acceptance:**
  1. Given an interruption at 60%, when resumed, then the transfer continues without duplicates.
- **Verification:** FAULT, SIM
- **Origin:** NEW

### FR-SYNC-015 — Formal model of the protocol
- **Statement:** The sync protocol MUST have a formal model with checked safety properties (no lost commits, no divergence after quiescence, integrity) before release.
- **Rationale:** Distributed protocols need model checking.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** BPA
- **Personas:** PER-KernelEngineer
- **Acceptance:**
  1. Given the model, when checked for the declared bounds, then no invariant violation is found.
- **Verification:** FORM
- **Origin:** NEW

## CAP-SYNC-02 — Box–Server sync

### FR-SYNC-021 — Replication scopes
- **Statement:** Administrators and users MUST be able to define replication scopes for Box and mobile devices by classes, queries, VAEs and time windows.
- **Rationale:** Devices hold only what they need.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** NOD, CTL
- **Personas:** PER-FieldWorker, PER-TenantAdministrator
- **Acceptance:**
  1. Given the scope "inspections, region West, last 90 days", when synchronised, then only matching objects are on the device.
- **Verification:** CONF
- **Origin:** IMP-07

### FR-SYNC-022 — Bidirectional sync with governed integration
- **Statement:** Local commits made on a device MUST be pushed to the server as a device branch. They are then integrated into the target branch automatically for `direct`-policy classes, and through a change set for `change_set`-policy classes.
- **Rationale:** Offline work obeys the same governance.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** NOD, DVM
- **Personas:** PER-FieldWorker
- **Acceptance:**
  1. Given offline edits to inspections (`direct`) and to a checklist template (`change_set`), when synchronised, then the inspections merge, and the template change becomes a change set.
- **Verification:** SCN
- **Origin:** IMP-07

### FR-SYNC-023 — Background and scheduled sync
- **Statement:** Devices MUST sync in the background when connected, on schedule and on demand, with bandwidth and battery policies on mobile.
- **Rationale:** Seamless experience.
- **Priority:** Should · **Phase:** PH-3 · **Systems:** NOD, WSP
- **Personas:** PER-FieldWorker
- **Acceptance:**
  1. Given a metered-network policy, when on cellular, then only metadata syncs until Wi-Fi is available.
- **Verification:** CONF
- **Origin:** NEW

### FR-SYNC-024 — Personal to organisation onboarding
- **Statement:** A Box user MUST be able to push selected classes and their history into an organisation VAE, keeping signatures and authorship. Class mapping is required when names collide (SCN-402).
- **Rationale:** Grow from personal to enterprise without migration.
- **Priority:** Should · **Phase:** PH-3 · **Systems:** NOD, DVM
- **Personas:** PER-PersonalUser
- **Acceptance:**
  1. Given SCN-402, when executed, then the outcomes match.
- **Verification:** SCN
- **Origin:** UBS-REQ-01

## CAP-SYNC-03 — Offline conflicts

### FR-SYNC-031 — Definition conflicts merge
- **Statement:** Offline Definition changes MUST integrate by three-way merge (CAP-VER-05), with lens alignment first (FR-MODEL-085).
- **Rationale:** One merge semantics everywhere.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** DVM
- **Personas:** PER-FieldWorker
- **Acceptance:**
  1. Given SCN-401, when synchronised, then 38 inspections merge automatically, and 2 report conflicts.
- **Verification:** SCN, PROP
- **Origin:** IMP-01, IMP-07

### FR-SYNC-032 — Ledger entries append with original valid time
- **Statement:** Offline ledger entries MUST be appended on the server in device order, with their original capture times as valid times and new server sequence numbers.
- **Rationale:** Facts captured offline remain facts.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** DVM
- **Personas:** PER-FieldWorker
- **Acceptance:**
  1. Given 120 offline findings, when synchronised, then 120 entries exist with their capture times as valid times.
- **Verification:** SCN
- **Origin:** IMP-01

### FR-SYNC-033 — Post-merge validation and returned rejections
- **Statement:** The server MUST validate integrated offline changes. Rejected changes MUST be returned to the device with explanations and kept for the user to fix, not dropped.
- **Rationale:** No silent loss.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** DVM, WSP
- **Personas:** PER-FieldWorker
- **Acceptance:**
  1. Given an offline edit that violates a rule changed on the server, when synchronised, then the device shows the rejection with its explanation.
- **Verification:** SCN
- **Origin:** NEW

### FR-SYNC-034 — Attributed resolutions
- **Statement:** Conflict resolutions made on devices MUST be committed with the resolving principal and the device identity.
- **Rationale:** Audit.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** DVM
- **Personas:** PER-InternalAuditor
- **Acceptance:**
  1. Given a resolved conflict, when audited, then the principal and device are recorded.
- **Verification:** CONF
- **Origin:** NEW

## CAP-SYNC-04 — Selective replication

### FR-SYNC-041 — Scope by classification
- **Statement:** Replication scopes MUST filter by data classification and MUST never exceed the device's clearance.
- **Rationale:** Data minimisation on devices.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** NOD
- **Personas:** PER-SecurityOfficer
- **Acceptance:**
  1. Given a device cleared for `internal`, when the scope includes personal fields, then those fields are masked or excluded.
- **Verification:** SEC
- **Origin:** NEW

### FR-SYNC-042 — Scope changes remove data
- **Statement:** When a scope shrinks or access is revoked, the device MUST remove the data that is out of scope at its next sync, and MUST report the removal.
- **Rationale:** Access changes reach devices.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** NOD
- **Personas:** PER-SecurityOfficer
- **Acceptance:**
  1. Given a removed region, when synchronised, then that region's objects are gone from the device.
- **Verification:** SEC
- **Origin:** NEW

### FR-SYNC-043 — Encrypted local storage
- **Statement:** Replicated data on devices MUST be encrypted at rest with keys protected by the device key store (FR-IAM-014).
- **Rationale:** Device loss.
- **Priority:** Must · **Phase:** PH-3 · **Systems:** NOD
- **Personas:** PER-SecurityOfficer
- **Acceptance:**
  1. Given the device database file copied off the device, when opened, then its content is unreadable.
- **Verification:** SEC
- **Origin:** NEW

## CAP-SYNC-05 — Federation registry

### FR-SYNC-051 — VAE registry
- **Statement:** The Federation system MUST maintain a registry of participating VAEs, each with a VAE ID, a DID document (keys, service endpoints), an operator identity and a status. Registrations MUST be signed by the VAE's owner key.
- **Rationale:** Discovery and trust (EXT-RFC 4.1).
- **Priority:** Must · **Phase:** PH-5 · **Systems:** FED
- **Personas:** PER-TenantAdministrator
- **Acceptance:**
  1. Given a registration, when resolved by another node, then its endpoint and key are returned and verified.
- **Verification:** CONF, SEC
- **Origin:** EXT-RFC

### FR-SYNC-052 — Federated discovery
- **Statement:** VAE resolution MUST work through a federated registry protocol and through DNS-based discovery, with local caching and a TTL.
- **Rationale:** No single point of control.
- **Priority:** Must · **Phase:** PH-5 · **Systems:** FED
- **Personas:** PER-KernelEngineer
- **Acceptance:**
  1. Given a registry outage, when a cached VAE is resolved, then the cache serves it within the TTL.
- **Verification:** FAULT
- **Origin:** EXT-RFC, EXT-TRI

### FR-SYNC-053 — Trust relationships
- **Statement:** Cross-organisation interaction MUST require an explicit trust relationship object that both sides approve. It specifies the allowed interactions (shared BPUs, published classes, aggregate queries) and the data classifications.
- **Rationale:** Federation is connection without subjugation.
- **Priority:** Must · **Phase:** PH-5 · **Systems:** FED, DVM
- **Personas:** PER-TenantAdministrator, PER-SecurityOfficer
- **Acceptance:**
  1. Given no trust relationship, when a peer requests any data, then it is refused.
- **Verification:** SEC
- **Origin:** EXT-TRI

### FR-SYNC-054 — Secure transport
- **Statement:** Node-to-node traffic MUST use mutually authenticated TLS 1.3 over QUIC (HTTP/3), with identities bound to the registered DIDs.
- **Rationale:** EXT-RFC 4.2 and 4.3.
- **Priority:** Must · **Phase:** PH-5 · **Systems:** FED, NOD
- **Personas:** PER-SecurityOfficer
- **Acceptance:**
  1. Given a peer presenting a certificate not bound to its DID, when connecting, then the handshake fails.
- **Verification:** SEC
- **Origin:** EXT-RFC

## CAP-SYNC-06 — Cross-node VAE tree

### FR-SYNC-061 — Remote parent VAEs
- **Statement:** A child VAE MUST be able to declare a parent VAE on another node. It then inherits definitions and logic as in FR-TEN-041, with transport through Federation.
- **Rationale:** SCN-408.
- **Priority:** Must · **Phase:** PH-5 · **Systems:** FED, DVM
- **Personas:** PER-TenantAdministrator
- **Acceptance:**
  1. Given SCN-408 step 1, when resolved, then the child uses the HQ release.
- **Verification:** SCN
- **Origin:** EXT-TRI, EXT-RFC

### FR-SYNC-062 — Network fallback state machine
- **Statement:** Resolution of a missing inherited item MUST follow this order:
  1. local cache;
  2. local store;
  3. parent lookup;
  4. remote fetch.

  Fetched items MUST be verified and cached as foreign snapshots. Items not found MUST return `IAM.NOT_FOUND`, and an unreachable parent MUST return `SYNC.UPSTREAM_UNAVAILABLE`.
- **Rationale:** EXT-RFC 4.4.
- **Priority:** Must · **Phase:** PH-5 · **Systems:** FED, DVM
- **Personas:** PER-KernelEngineer
- **Acceptance:**
  1. Given each state transition, when exercised in the conformance suite, then the outcomes match the state table.
- **Verification:** CONF, SIM
- **Origin:** EXT-RFC

### FR-SYNC-063 — Operation while the parent is unreachable
- **Statement:** Child VAEs MUST continue with cached inherited items when the parent is unreachable, and MUST report staleness.
- **Rationale:** Local autonomy (SCN-408 A1).
- **Priority:** Must · **Phase:** PH-5 · **Systems:** FED, NOD
- **Personas:** PER-TenantAdministrator
- **Acceptance:**
  1. Given the HQ offline, when Tokyo processes transactions, then they succeed with cached logic.
- **Verification:** FAULT, SCN
- **Origin:** EXT-TRI

### FR-SYNC-064 — Publication policies for parents
- **Statement:** Children MUST declare what they publish upward (aggregates, specific classes), and parents MUST NOT receive anything else.
- **Rationale:** Data stays local.
- **Priority:** Must · **Phase:** PH-5 · **Systems:** FED
- **Personas:** PER-SecurityOfficer
- **Acceptance:**
  1. Given SCN-408 step 3, when HQ asks for raw rows, then it is denied.
- **Verification:** SEC
- **Origin:** EXT-TRI

## CAP-SYNC-07 — Shared BPUs

### FR-SYNC-071 — Shared objects with shared history
- **Statement:** Organisations with a trust relationship MUST be able to share BPUs. Each party holds a replica with one shared commit history.
- **Rationale:** A single agreed truth across companies (SCN-409).
- **Priority:** Must · **Phase:** PH-5 · **Systems:** FED, DVM
- **Personas:** PER-ContractManager, PER-ExternalParty
- **Acceptance:**
  1. Given SCN-409, when synchronised, then the head roots of both replicas are equal.
- **Verification:** SCN, FORM
- **Origin:** EXT-TRI

### FR-SYNC-072 — Multi-party approval of Definition changes
- **Statement:** Changes to Definition state of a shared BPU MUST become effective only when signed by the authorised principals of every party required by the sharing agreement.
- **Rationale:** No unilateral rewriting.
- **Priority:** Must · **Phase:** PH-5 · **Systems:** FED, DVM
- **Personas:** PER-ContractManager
- **Acceptance:**
  1. Given one party's signature only, when evaluated, then the change is pending on both replicas.
- **Verification:** SCN, SEC
- **Origin:** NEW

### FR-SYNC-073 — Concurrent proposals
- **Statement:** Concurrent conflicting proposals on a shared BPU MUST all remain non-effective until the parties agree on one. Both parties MUST see all proposals.
- **Rationale:** SCN-409 A1.
- **Priority:** Must · **Phase:** PH-5 · **Systems:** FED
- **Personas:** PER-ContractManager
- **Acceptance:**
  1. Given two conflicting amendments, when both are synchronised, then neither is effective.
- **Verification:** SIM, FORM
- **Origin:** NEW

### FR-SYNC-074 — Party-appended ledger entries
- **Statement:** Parties MUST be able to append ledger entries to a shared BPU within their declared rights, without the other party's approval, and the entries MUST replicate.
- **Rationale:** Delivery confirmations and payments are unilateral facts.
- **Priority:** Must · **Phase:** PH-5 · **Systems:** FED, DVM
- **Personas:** PER-ExternalParty
- **Acceptance:**
  1. Given B's delivery confirmation, when appended, then it appears on A's replica.
- **Verification:** SCN
- **Origin:** IMP-01

### FR-SYNC-075 — Ending a share
- **Statement:** Ending a sharing agreement MUST stop further replication, and each party MUST keep the history up to the end.
- **Rationale:** Clean exit.
- **Priority:** Should · **Phase:** PH-5 · **Systems:** FED
- **Personas:** PER-ContractManager
- **Acceptance:**
  1. Given an ended share, when a party appends, then the entry stays local and is not replicated.
- **Verification:** CONF
- **Origin:** NEW

## CAP-SYNC-08 — Browser kernel

### FR-SYNC-081 — DVM in the browser
- **Statement:** The DVM MUST be compilable to WebAssembly and runnable in modern browsers with SQLite on origin-private file storage, serving reads and local commits for a replicated scope.
- **Rationale:** Local-first web (IMP-07).
- **Priority:** Should · **Phase:** PH-5 · **Systems:** DVM, SDK, WSP
- **Personas:** PER-PersonalUser, PER-BusinessUser
- **Acceptance:**
  1. Given SCN-411, when executed, then the outcomes match.
- **Verification:** SCN
- **Origin:** IMP-07

### FR-SYNC-082 — Conformance of the browser kernel
- **Statement:** The browser kernel MUST pass the conformance profile declared for it (a subset that excludes server-only instructions).
- **Rationale:** Same semantics everywhere.
- **Priority:** Must · **Phase:** PH-5 · **Systems:** DVM, BPA
- **Personas:** PER-KernelEngineer
- **Acceptance:**
  1. Given the browser profile vectors, when run in the browser, then 100% pass.
- **Verification:** CONF
- **Origin:** EXT-BPU

### FR-SYNC-083 — Eviction handling
- **Statement:** The browser kernel MUST detect storage eviction, re-replicate the scope, and warn users about unsynced local commits before modes that are prone to eviction.
- **Rationale:** Browser storage is not durable.
- **Priority:** Must · **Phase:** PH-5 · **Systems:** DVM, WSP
- **Personas:** PER-PersonalUser
- **Acceptance:**
  1. Given simulated eviction, when the app reloads, then it re-replicates and shows the status.
- **Verification:** FAULT
- **Origin:** NEW

### FR-SYNC-084 — Non-extractable keys in the browser
- **Statement:** Signing and encryption keys in the browser MUST be non-extractable WebCrypto keys or keys held by an external authenticator.
- **Rationale:** Key custody in hostile environments.
- **Priority:** Must · **Phase:** PH-5 · **Systems:** SDK
- **Personas:** PER-SecurityOfficer
- **Acceptance:**
  1. Given the browser key store, when script attempts export, then it is refused.
- **Verification:** SEC
- **Origin:** NEW
