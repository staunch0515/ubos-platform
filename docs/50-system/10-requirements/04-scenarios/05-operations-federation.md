---
id: UBS-REQ-04-05
title: Scenarios — Offline, Ecosystem, Operations and Federation
status: review
phase: PH-2
depends_on: [UBS-REQ-04]
---

# Scenarios — Offline, Ecosystem, Operations and Federation (SCN-401 … SCN-412)

### SCN-401 — Field worker offline for five days
- **Goal:** An inspector works offline for a week and syncs without losing or silently overwriting anything.
- **Personas:** PER-FieldWorker
- **Systems:** NOD (Box on tablet), WSP, DVM
- **Phase:** PH-3
- **Preconditions:** 1. The tablet Box has replicated the scope "site inspections, region West". 2. Server-side, a colleague edits 2 of the same inspection records during the week.
- **Main flow:**
  1. Offline, the worker updates 40 inspections and records 120 findings (ledger kind) with photos.
  2. Back online, sync exchanges missing objects in both directions.
  3. 38 inspections merge automatically. 2 have field conflicts and are presented in the conflict screen with both values. The worker resolves them.
  4. All 120 findings are appended on the Server with their original capture times as valid times.
- **Alternate and failure flows:**
  - A1. Sync is interrupted midway → resuming continues from the last acknowledged object, and no duplicates occur.
  - A2. A class definition changed on the Server during the week → the offline edits are aligned through the lens before merge (SCN-014).
- **Postconditions (observable):** 1. Zero lost edits. 2. Every conflict resolution is attributed to the worker.
- **Business rules exercised:** FR-SYNC-011, FR-SYNC-021, FR-SYNC-031, FR-UX-081, FR-FILE-011
- **Verification:** SCN, SIM, FAULT

### SCN-402 — Personal user joins an organisation
- **Goal:** A freelancer's Box data becomes part of an organisation's Server without conversion.
- **Personas:** PER-PersonalUser, PER-TenantAdministrator
- **Systems:** NOD, DVM
- **Phase:** PH-3
- **Preconditions:** 1. The freelancer has 2 years of client and invoice records on a Box.
- **Main flow:**
  1. The administrator invites the freelancer and grants a VAE scope.
  2. The freelancer chooses which classes to share → the selected history is pushed with signatures intact. Private classes stay local.
  3. The organisation sees the shared records with their original history and authorship.
- **Alternate and failure flows:**
  - A1. Class names collide with organisation classes → a mapping step is required before push.
- **Postconditions (observable):** 1. The signatures of pushed commits verify on the Server.
- **Business rules exercised:** FR-SYNC-021, FR-SYNC-041, FR-PROOF-021
- **Verification:** SCN

### SCN-403 — Publish a Buk and license it to a customer
- **Goal:** An ISV publishes a verified Buk, and a customer installs it under a paid licence.
- **Personas:** PER-IsvDeveloper, PER-ExchangeOperator, PER-TenantAdministrator
- **Systems:** FRG, EXC, CTL, NOD
- **Phase:** PH-4
- **Preconditions:** 1. The ISV has a verified publisher identity.
- **Main flow:**
  1. The ISV runs `forge publish` → the Exchange runs verification (static checks, scenarios, conformance of declared extensions) and assigns the badge "Verified".
  2. The customer administrator buys the plan "per VAE, monthly" → a licence object is issued to the tenant.
  3. The customer installs the Buk (SCN-013 flow) → the licence check logic confirms entitlement at install and at run time.
  4. Monthly usage is metered, invoiced in USD and paid out to the ISV minus the Exchange fee.
- **Alternate and failure flows:**
  - A1. The licence lapses → licensed actions fail with `BILL.LICENCE_INACTIVE`, and read access to data is never blocked.
- **Postconditions (observable):** 1. The invoice lines trace to meter records.
- **Business rules exercised:** FR-PKG-061, FR-PKG-081, FR-BILL-031, FR-BILL-041, FR-BILL-011
- **Verification:** SCN, AUDIT

### SCN-404 — Move a tenant between cells online
- **Goal:** An operator rebalances a large tenant to another cell without user-visible downtime.
- **Personas:** PER-PlatformOperator
- **Systems:** CTL, NOD
- **Phase:** PH-4
- **Preconditions:** 1. Tenant T (400 GB of history) lives in cell `us-east-2a`.
- **Main flow:**
  1. The operator schedules a migration to cell `us-east-2c` → objects are copied by Merkle difference while T keeps working.
  2. The cut-over pauses writes for at most the NR limit, syncs the last commits, verifies the head roots and switches routing.
  3. Clients reconnect transparently.
- **Alternate and failure flows:**
  - A1. The root verification fails → the cut-over aborts, and T stays on the source cell.
- **Postconditions (observable):** 1. The head roots are identical before and after. 2. The source copy is retained for the rollback window and then destroyed with a certificate.
- **Business rules exercised:** FR-TEN-071, FR-OPS-081, FR-PROOF-031
- **Verification:** SCN, FAULT, BENCH

### SCN-405 — Rolling kernel upgrade
- **Goal:** A Server cluster is upgraded to a new kernel minor version without downtime or data conversion stops.
- **Personas:** PER-PlatformOperator
- **Systems:** NOD, DVM
- **Phase:** PH-2
- **Preconditions:** 1. A 3-node Server cluster runs kernel 1.4.
- **Main flow:**
  1. The operator runs the upgrade plan → the compatibility check confirms the storage format and ABI compatibility.
  2. The nodes upgrade one at a time → requests drain and move to other nodes.
  3. After all nodes run 1.5, new instructions become available to logic declaring ABI 1.5.
- **Alternate and failure flows:**
  - A1. A health check fails on the upgraded node → it is rolled back automatically, and the plan halts.
- **Postconditions (observable):** 1. No failed client requests beyond retried ones. 2. The audit shows the upgrade steps.
- **Business rules exercised:** FR-OPS-031, FR-STD-041, FR-OPS-051
- **Verification:** SCN, FAULT

### SCN-406 — Disaster recovery failover
- **Goal:** A regional failure is survived within the RPO and RTO targets.
- **Personas:** PER-PlatformOperator
- **Systems:** NOD
- **Phase:** PH-2
- **Preconditions:** 1. A primary site and a standby site with streaming replication.
- **Main flow:**
  1. The primary site is lost (simulated) → the operator initiates failover, or the automatic policy triggers it.
  2. The standby is promoted, and the last replicated commit is determined and reported.
  3. The clients reconnect. The loss window is measured against the RPO.
- **Alternate and failure flows:**
  - A1. The former primary returns → it is fenced, and its divergent commits (if any) are exported for manual review, never merged silently.
- **Postconditions (observable):** 1. The measured RPO and RTO are within targets.
- **Business rules exercised:** FR-OPS-061, FR-OPS-021
- **Verification:** SCN, FAULT

### SCN-407 — Certify a third-party BPA implementation
- **Goal:** An independent implementation or embedding earns "BPU Compliant" certification.
- **Personas:** PER-ThirdPartyImplementer, PER-StandardsSteward
- **Systems:** BPA, FRG
- **Phase:** PH-4
- **Preconditions:** 1. The vendor implements BPA 1.x profile "Core" in another language.
- **Main flow:**
  1. The vendor runs the conformance runner → 100% of the Core vectors pass, and the report is signed by the runner.
  2. The vendor submits the report and a declaration of supported profiles and extensions.
  3. The steward reproduces the run on the certification environment → the certificate is issued with version, profiles and expiry.
- **Alternate and failure flows:**
  - A1. The vendor claims the "Bitemporal" profile but fails 3 vectors → certification is limited to the passing profiles.
- **Postconditions (observable):** 1. The certificate is listed publicly and verifiable.
- **Business rules exercised:** FR-STD-021, FR-STD-051, FR-STD-071
- **Verification:** SCN, CONF, AUDIT

### SCN-408 — Headquarters and subsidiary VAEs on different nodes
- **Goal:** A subsidiary runs its own node, inherits logic from headquarters and keeps its data local.
- **Personas:** PER-TenantAdministrator
- **Systems:** FED, NOD, DVM
- **Phase:** PH-5
- **Preconditions:** 1. The HQ VAE runs on the HQ Server. 2. The Tokyo subsidiary VAE runs on the Tokyo Server as a child of the HQ VAE.
- **Main flow:**
  1. HQ publishes release 2026.11 of the tax logic → Tokyo, which has no override, receives it at next resolution through network fallback and caches it as a foreign snapshot.
  2. The Tokyo compliance sheet (Japan pack) overrides one declaration locally.
  3. HQ requests the monthly totals → Tokyo answers a query with aggregate results only. No raw records leave Tokyo.
- **Alternate and failure flows:**
  - A1. The HQ node is unreachable → Tokyo continues with cached logic and reports "upstream unavailable" for uncached items only.
- **Postconditions (observable):** 1. The cached foreign snapshots are verifiable against HQ signatures.
- **Business rules exercised:** FR-SYNC-061, FR-SYNC-051, FR-TEN-041
- **Verification:** SCN, FAULT, SIM

### SCN-409 — Shared contract BPU between two organisations
- **Goal:** Two companies operate one contract with a single, mutually agreed truth.
- **Personas:** PER-ContractManager, PER-ExternalParty
- **Systems:** FED, DVM, WSP
- **Phase:** PH-5
- **Preconditions:** 1. Company A and company B each run a UBOS node and are registered in the federation.
- **Main flow:**
  1. A proposes a shared agreement → B accepts → both nodes hold a replica of the same BPU with a shared commit history.
  2. A proposes an amendment → the change requires signatures of both parties' authorised principals before it is effective on either replica.
  3. Delivery confirmations are appended by B (ledger kind) and replicate to A.
- **Alternate and failure flows:**
  - A1. Both parties submit conflicting amendments concurrently → neither is effective. Both parties see the conflict and must agree on one.
- **Postconditions (observable):** 1. The head roots of both replicas are equal after sync. 2. Neither party can rewrite history unilaterally.
- **Business rules exercised:** FR-SYNC-071, FR-SIGN-031, FR-PROOF-031
- **Verification:** SCN, SIM, FORM

### SCN-410 — Enterprise worker pool runs a batch
- **Goal:** A heavy monthly batch runs on the organisation's own idle machines, safely.
- **Personas:** PER-PlatformOperator, PER-SecurityOfficer
- **Systems:** NOD (Worker role), DVM
- **Phase:** PH-4
- **Preconditions:** 1. 20 office workstations are registered as workers with the classification limit "internal".
- **Main flow:**
  1. The month-end risk simulation (verified WASM) is split into 2,000 jobs.
  2. Workers pull jobs whose data classification is at most "internal", run them sandboxed and return results with a hash.
  3. 5% of jobs are re-executed on a second worker, and the results must match.
- **Alternate and failure flows:**
  - A1. A result mismatch → the job is re-run on the Server, and the worker is quarantined.
- **Postconditions (observable):** 1. No job with "confidential" data ever runs on a workstation worker.
- **Business rules exercised:** FR-EVT-061, FR-LOGIC-051, FR-LOGIC-061
- **Verification:** SCN, SEC

### SCN-411 — Local-first web user
- **Goal:** A user works in the browser with instant responses and offline capability, backed by the full kernel.
- **Personas:** PER-PersonalUser, PER-BusinessUser
- **Systems:** SDK, DVM (WASM), WSP
- **Phase:** PH-5
- **Preconditions:** 1. The browser supports WASM and origin-private storage.
- **Main flow:**
  1. The user opens Workspace → the browser kernel loads, replicates the user's scope and serves reads locally.
  2. Edits commit locally and sync in the background.
  3. The network drops → work continues, and the UI shows offline status.
- **Alternate and failure flows:**
  - A1. The browser storage is evicted → the kernel re-replicates from the server, and unsynced local commits are reported as lost only if they were never acknowledged. The UI warns users before eviction-prone modes.
- **Postconditions (observable):** 1. The browser kernel passes the same conformance profile as the Box for the supported feature set.
- **Business rules exercised:** FR-SYNC-081, FR-SYNC-021, FR-UX-081
- **Verification:** SCN, CONF

### SCN-412 — Automated deprovisioning and access review
- **Goal:** A leaver loses all access immediately, and quarterly reviews prove least privilege.
- **Personas:** PER-TenantAdministrator, PER-SecurityOfficer, PER-InternalAuditor
- **Systems:** CTL, DVM
- **Phase:** PH-3
- **Preconditions:** 1. SCIM provisioning from the corporate identity provider.
- **Main flow:**
  1. HR terminates employee E → SCIM deactivates E. Sessions are revoked, pending tasks are reassigned and E's delegations end.
  2. The quarterly access review is generated by policy analysis → each manager confirms or revokes the permissions of their reports.
  3. The review record is signed and retained.
- **Alternate and failure flows:**
  - A1. E is an approver in pending approvals → the approvals are re-routed according to delegation rules.
- **Postconditions (observable):** 1. There is no successful request by E after deactivation plus the propagation limit.
- **Business rules exercised:** FR-IAM-071, FR-IAM-081, FR-IAM-101, FR-FLOW-091
- **Verification:** SCN, AUDIT
