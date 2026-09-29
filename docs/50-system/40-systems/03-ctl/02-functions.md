---
id: UBS-SYS-CTL-02
title: Control Plane — Design Requirements
status: draft
phase: PH-4
depends_on: [UBS-SYS-CTL-01]
---

# Control Plane — Design Requirements (DSN-CTL-*)

| Range | Sub-area |
|---|---|
| DSN-CTL-0xx | blindness, identity and audit of operators |
| DSN-CTL-1xx | node registry and fleet health |
| DSN-CTL-2xx | tenant placement, lifecycle and migration |
| DSN-CTL-3xx | upgrades and configuration rollout |
| DSN-CTL-4xx | usage, plans, entitlements, budgets |
| DSN-CTL-5xx | capacity and tiering |
| DSN-CTL-6xx | consoles |

## Blindness, identity and audit

### DSN-CTL-001 — No business-data path
- **Statement:** CTL MUST have no credential, API or network path that can read tenant business data. Its only node interface is the admin contract (CTR-050), which exposes no business-data operation. Aggregated metrics received from nodes MUST contain only counts, sizes and technical identifiers.
- **Rationale:** AR-006.
- **Priority:** Must · **Phase:** PH-4 · **Systems:** CTL
- **Personas:** PER-SecurityOfficer
- **Acceptance:**
  1. Given an architecture review and a penetration test of CTL, when performed, then no path to business data is found.
- **Verification:** INSP, SEC
- **Origin:** AR-006, FR-OPS-081, CR-CCPA-004

### DSN-CTL-002 — Operator identity
- **Statement:** Operators MUST authenticate through the operator IdP with phishing-resistant MFA (WebAuthn) (NR-SEC-011), with roles: viewer, operator, release manager, billing, security.
- **Rationale:** NR-SEC-011.
- **Priority:** Must · **Phase:** PH-4 · **Systems:** CTL
- **Personas:** PER-PlatformOperator
- **Acceptance:**
  1. Given an operator without WebAuthn, when signing in, then access is refused.
- **Verification:** SEC
- **Origin:** NR-SEC-011

### DSN-CTL-003 — Two-person rule for destructive operations
- **Statement:** Tenant deletion, KEK destruction requests, region evacuation and forced migrations MUST require approval by two different operators, recorded with signatures.
- **Rationale:** Prevention of single-operator damage.
- **Priority:** Must · **Phase:** PH-4 · **Systems:** CTL
- **Personas:** PER-SecurityOfficer
- **Acceptance:**
  1. Given one operator requesting a tenant deletion, when submitted, then it waits for a second approval.
- **Verification:** SEC
- **Origin:** FR-TEN-053, AR-025

### DSN-CTL-004 — Operator audit log
- **Statement:** Every CTL action MUST be written to a hash-chained audit log and exported to the security SIEM. Tenants MUST be able to see operator actions affecting their tenant in the tenant portal.
- **Rationale:** FR-OPS-093, NR-SEC-010.
- **Priority:** Must · **Phase:** PH-4 · **Systems:** CTL
- **Personas:** PER-SecurityOfficer
- **Acceptance:**
  1. Given a suspension of a tenant, when the tenant administrator views the portal, then the operator action is visible.
- **Verification:** SEC
- **Origin:** FR-OPS-093, NR-SEC-010

## Node registry and fleet health

### DSN-CTL-101 — Node registry
- **Statement:** CTL MUST register nodes with a one-time registration token, issue node mTLS certificates from its CA, and track edition, version, Cell, zone and state (FR-OPS-081).
- **Rationale:** FR-OPS-081.
- **Priority:** Must · **Phase:** PH-4 · **Systems:** CTL
- **Personas:** PER-PlatformOperator
- **Acceptance:**
  1. Given a new node with a valid token, when registered, then it appears in the registry with its certificate.
- **Verification:** CONF
- **Origin:** FR-OPS-081, CTR-050

### DSN-CTL-102 — Fleet health
- **Statement:** CTL MUST aggregate heartbeats (DSN-NOD-642) into fleet health views per Cell, version and customer-operated node, flagging missing nodes after 3 missed heartbeats.
- **Rationale:** FR-OPS-081.
- **Priority:** Must · **Phase:** PH-4 · **Systems:** CTL
- **Personas:** PER-PlatformOperator
- **Acceptance:**
  1. Given a node stopped, when 90 s pass, then it is shown as missing, and an alert fires.
- **Verification:** CONF
- **Origin:** FR-OPS-081, CTR-051

### DSN-CTL-103 — Customer-operated nodes
- **Statement:** Customer-operated nodes MUST connect outbound only; CTL MUST queue admin operations for them and deliver them over the node's long-lived outbound channel (FR-OPS-083).
- **Rationale:** FR-OPS-083.
- **Priority:** Should · **Phase:** PH-4 · **Systems:** CTL
- **Personas:** PER-PlatformOperator
- **Acceptance:**
  1. Given a node behind NAT, when a config is applied, then it arrives through the outbound channel.
- **Verification:** SCN
- **Origin:** FR-OPS-083

### DSN-CTL-104 — Certificate rotation
- **Statement:** Node certificates MUST have 30-day lifetimes and rotate automatically at 2/3 of their lifetime; the CA key MUST live in an HSM.
- **Rationale:** NR-SEC-004, NR-SEC-006.
- **Priority:** Must · **Phase:** PH-4 · **Systems:** CTL
- **Personas:** PER-SecurityOfficer
- **Acceptance:**
  1. Given a node running for 60 days, when inspected, then its certificate has rotated twice without downtime.
- **Verification:** SEC
- **Origin:** NR-SEC-004, NR-SEC-006

## Tenant placement, lifecycle and migration

### DSN-CTL-201 — Placement policies
- **Statement:** CTL MUST place new tenants by policy (FR-TEN-071): residency region (FR-TEN-073), plan (shared or dedicated Cell), Cell capacity headroom and anti-affinity for related tenants. Placement decisions MUST be recorded with inputs.
- **Rationale:** FR-TEN-071, FR-TEN-073.
- **Priority:** Must · **Phase:** PH-4 · **Systems:** CTL
- **Personas:** PER-PlatformOperator
- **Acceptance:**
  1. Given an EU-resident tenant, when placed, then only EU Cells are candidates.
- **Verification:** CONF
- **Origin:** FR-TEN-071, FR-TEN-073, NR-PRIV-004

### DSN-CTL-202 — Tenant lifecycle orchestration
- **Statement:** CTL MUST orchestrate tenant creation, suspension, resumption, archive and deletion by calling node operations (IF-NOD-063) as idempotent, resumable steps, and reflect state in the registry (FR-TEN-051…053).
- **Rationale:** FR-TEN-051, FR-TEN-052, FR-TEN-053.
- **Priority:** Must · **Phase:** PH-4 · **Systems:** CTL
- **Personas:** PER-PlatformOperator
- **Acceptance:**
  1. Given a CTL crash during tenant creation, when restarted, then the operation completes exactly once.
- **Verification:** FAULT
- **Origin:** FR-TEN-051, FR-TEN-052, FR-TEN-053

### DSN-CTL-203 — Online migration orchestration
- **Statement:** CTL MUST orchestrate migration between Cells (FR-TEN-072) through the node steps of DSN-NOD-411: export, import, catch-up loops until lag is under a threshold, write pause, final catch-up, root comparison, routing cut-over, source retirement after a safety window. Any failed check before cut-over MUST abort and resume writes on the source.
- **Rationale:** FR-TEN-072, NR-AVAIL-004.
- **Priority:** Must · **Phase:** PH-4 · **Systems:** CTL
- **Personas:** PER-PlatformOperator
- **Acceptance:**
  1. Given a root mismatch at cut-over, when detected, then the migration aborts, and the source continues serving.
- **Verification:** FAULT, SIM
- **Origin:** FR-TEN-072, NR-AVAIL-004

### DSN-CTL-204 — Routing directory
- **Statement:** CTL MUST publish the tenant → Cell routing directory to the edge routers of every Cell and to DNS, with versioned updates and a cut-over barrier during migration.
- **Rationale:** Tenants are reachable at a stable address across migrations.
- **Priority:** Must · **Phase:** PH-4 · **Systems:** CTL
- **Personas:** PER-PlatformOperator
- **Acceptance:**
  1. Given a completed migration, when clients reconnect, then they reach the new Cell within 60 s.
- **Verification:** SCN
- **Origin:** FR-TEN-072, FR-TEN-013

### DSN-CTL-205 — Evacuation
- **Statement:** CTL MUST support evacuating a Cell (planned maintenance or risk) by migrating tenants in priority order within capacity limits.
- **Rationale:** AR-014, NR-AVAIL-001.
- **Priority:** Should · **Phase:** PH-4 · **Systems:** CTL
- **Personas:** PER-PlatformOperator
- **Acceptance:**
  1. Given an evacuation of a 50-tenant Cell, when run, then all tenants move with each pause within NR-AVAIL-004.
- **Verification:** SCN
- **Origin:** AR-014, NR-AVAIL-004

## Upgrades and configuration rollout

### DSN-CTL-301 — Upgrade waves
- **Statement:** CTL MUST roll out node versions in waves (canary Cell, early-adopter Cells, general, customer-operated opt-in) with automatic progression gates on health and SLO metrics and automatic halt on regression (FR-OPS-082).
- **Rationale:** FR-OPS-082.
- **Priority:** Must · **Phase:** PH-4 · **Systems:** CTL
- **Personas:** PER-PlatformOperator
- **Acceptance:**
  1. Given a release that raises error rates in the canary Cell, when observed, then the wave halts, and the canary rolls back.
- **Verification:** FAULT, SCN
- **Origin:** FR-OPS-082, FR-OPS-033

### DSN-CTL-302 — Configuration rollout
- **Statement:** Node configuration documents MUST be versioned, signed by CTL and rolled out in waves like binaries (IF-NOD-061), with diff views and rollback to any previous version.
- **Rationale:** FR-OPS-091, FR-OPS-093.
- **Priority:** Must · **Phase:** PH-4 · **Systems:** CTL
- **Personas:** PER-PlatformOperator
- **Acceptance:**
  1. Given a faulty configuration, when rolled back, then all nodes return to the previous version.
- **Verification:** CONF
- **Origin:** FR-OPS-091, FR-OPS-093

### DSN-CTL-303 — Maintenance windows
- **Statement:** Tenants on dedicated Cells MAY declare maintenance windows; waves MUST respect them unless a security release is flagged urgent.
- **Rationale:** Customer control for dedicated deployments.
- **Priority:** Should · **Phase:** PH-4 · **Systems:** CTL
- **Personas:** PER-TenantAdministrator
- **Acceptance:**
  1. Given a window on Sundays, when a normal wave arrives on Tuesday, then the dedicated Cell is upgraded on Sunday.
- **Verification:** CONF
- **Origin:** FR-OPS-082

## Usage, plans, entitlements and budgets

### DSN-CTL-401 — Usage ingestion and verification
- **Statement:** CTL MUST ingest usage records (DAT-UsageRecord), verify each node's hash chain and sequence continuity (FR-BILL-012), deduplicate by record ID, and flag gaps for investigation.
- **Rationale:** FR-BILL-011, FR-BILL-012.
- **Priority:** Must · **Phase:** PH-4 · **Systems:** CTL
- **Personas:** PER-PlatformOperator
- **Acceptance:**
  1. Given a duplicated batch, when ingested, then no record is counted twice.
- **Verification:** CONF, SEC
- **Origin:** FR-BILL-011, FR-BILL-012

### DSN-CTL-402 — Air-gapped usage files
- **Statement:** CTL MUST accept signed usage files exported by air-gapped nodes and verify them like pushed records.
- **Rationale:** CTR-051 failure behaviour.
- **Priority:** Must · **Phase:** PH-4 · **Systems:** CTL
- **Personas:** PER-PlatformOperator
- **Acceptance:**
  1. Given a usage file with a modified record, when imported, then it is rejected.
- **Verification:** SEC
- **Origin:** CTR-051, FR-BILL-012

### DSN-CTL-403 — Plans and entitlements
- **Statement:** Plans (FR-BILL-021) MUST be versioned catalogue objects defining meters, included quantities, limits, features and prices; entitlements per tenant MUST be derived from the plan, add-ons and EXC licences (CTR-053), and pushed to nodes (DSN-NOD-431).
- **Rationale:** FR-BILL-021, FR-BILL-022.
- **Priority:** Must · **Phase:** PH-4 · **Systems:** CTL
- **Personas:** PER-TenantAdministrator
- **Acceptance:**
  1. Given a plan upgrade, when applied, then the node enforces the new limits within 5 minutes.
- **Verification:** CONF
- **Origin:** FR-BILL-021, FR-BILL-022, CTR-053

### DSN-CTL-404 — Rating and invoice lines
- **Statement:** CTL MUST rate usage against plans with decimal arithmetic and produce invoice lines per tenant and period for EXC invoicing (fiat only, DEC-012), reproducible from records and plan versions.
- **Rationale:** FR-BILL-041, FR-BILL-052.
- **Priority:** Must · **Phase:** PH-4 · **Systems:** CTL, EXC
- **Personas:** PER-TenantAdministrator
- **Acceptance:**
  1. Given the same usage and plan, when rated twice, then invoice lines are identical.
- **Verification:** CONF
- **Origin:** FR-BILL-041, FR-BILL-052

### DSN-CTL-405 — Budgets, caps and cost transparency
- **Statement:** Tenant administrators MUST be able to set budgets and caps per meter (FR-BILL-051) and see cost breakdowns by meter, VAE and agent (FR-BILL-052); CTL MUST push caps to nodes and AGT.
- **Rationale:** FR-BILL-051, FR-BILL-052, CAP-BILL-05.
- **Priority:** Must · **Phase:** PH-4 · **Systems:** CTL, AGT
- **Personas:** PER-TenantAdministrator
- **Acceptance:**
  1. Given an AI budget of 500 USD, when 80% is reached, then the administrator is notified; at 100%, model calls are blocked.
- **Verification:** CONF
- **Origin:** FR-BILL-051, FR-BILL-052

## Capacity and tiering

### DSN-CTL-501 — Capacity model
- **Statement:** CTL MUST forecast Cell capacity (storage, commit rate, CPU) from node capacity reports (DSN-NOD-644) with trend projection and alert when a Cell is predicted to exceed 75% within 30 days (FR-OPS-071).
- **Rationale:** FR-OPS-071.
- **Priority:** Must · **Phase:** PH-4 · **Systems:** CTL
- **Personas:** PER-PlatformOperator
- **Acceptance:**
  1. Given a growth trend reaching 80% in 20 days, when evaluated, then an alert with the projection is raised.
- **Verification:** CONF
- **Origin:** FR-OPS-071

### DSN-CTL-502 — Tiering policies
- **Statement:** CTL MUST manage tiering policies per plan (age thresholds, classes excluded, hold awareness) (FR-OPS-072) and send them to nodes for execution; policies MUST never tier content under hold to a tier without WORM when WORM is required.
- **Rationale:** FR-OPS-072, FR-VER-093.
- **Priority:** Must · **Phase:** PH-4 · **Systems:** CTL
- **Personas:** PER-PlatformOperator
- **Acceptance:**
  1. Given a policy change, when applied, then nodes report the new policy version.
- **Verification:** CONF
- **Origin:** FR-OPS-072, FR-VER-091, FR-VER-093

### DSN-CTL-503 — Quotas per plan
- **Statement:** CTL MUST define quota defaults per plan and overrides per tenant, pushed to nodes (DSN-NOD-645) (FR-OPS-073).
- **Rationale:** FR-OPS-073.
- **Priority:** Must · **Phase:** PH-4 · **Systems:** CTL
- **Personas:** PER-PlatformOperator
- **Acceptance:**
  1. Given a quota override, when applied, then the node enforces it.
- **Verification:** CONF
- **Origin:** FR-OPS-073

## Consoles

### DSN-CTL-601 — Operator console
- **Statement:** CTL MUST provide an operator console (web) for fleet, tenants (technical view only), waves, migrations, usage and alerts, built with the same UI toolkit as WSP.
- **Rationale:** NR-OPER-004.
- **Priority:** Must · **Phase:** PH-4 · **Systems:** CTL
- **Personas:** PER-PlatformOperator
- **Acceptance:**
  1. Given the console, when an operator performs a migration, then every step and its status is visible.
- **Verification:** USE
- **Origin:** NR-OPER-004, FR-OPS-081

### DSN-CTL-602 — Tenant portal
- **Statement:** CTL MUST provide a tenant portal for plan, usage, invoices (from EXC), budgets, residency, maintenance windows and operator-action history.
- **Rationale:** FR-BILL-052.
- **Priority:** Must · **Phase:** PH-4 · **Systems:** CTL
- **Personas:** PER-TenantAdministrator
- **Acceptance:**
  1. Given a tenant administrator, when viewing usage, then daily and monthly totals per meter are shown.
- **Verification:** USE
- **Origin:** FR-BILL-052
