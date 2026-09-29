---
id: SPEC-31
title: "Deployment and Editions"
status: complete
phase: P4
depends_on: [SPEC-10, SPEC-11, SPEC-20, SPEC-24, SPEC-26, SPEC-28, SPEC-30, ADR-001, ADR-003, ADR-004]
sources: [UP, FU, LC, UB, UC, US, LS]
---

# Deployment and Editions

## 0. Chapter header

- **Scope:** This chapter covers how the platform is built, packaged and run:
  - build artifacts (crates to binaries and images);
  - the two editions and their exact differences;
  - topologies, from a single process to scaled roles;
  - the reference container deployment;
  - database and runtime-store provisioning, backups and upgrades;
  - configuration and secrets handling at host level;
  - the five clients' distribution;
  - the deferred edge sync.
- **MOD:** `DEP`
- **depends_on:** SPEC-10, SPEC-11, SPEC-20, SPEC-24, SPEC-26, SPEC-28, SPEC-30, ADR-001, ADR-003, ADR-004.
- **Terms used:** TERM-Edition, TERM-Host, TERM-Kernel, TERM-TopologyRole, TERM-RuntimeStore, TERM-Client.
- **Origin summary:**
  - Verdicts: VRD-19-01…05, VRD-20-01…05.
  - LS: editions by Cargo feature and the protocol-first server (CON-LS-006, 062, 001).
  - US: Cargo workspace, the "one core, many clients" goal, dev and production databases (CON-US-001, 042, 064).
  - UC: the producer/consumer split and Redis roles (CON-UC-003, 043).
  - UP: the deployment triad of database, kernel and copilot (CON-UP-004).
  - UB: partitions, scale notes and network boot designs (CON-UB-040, 033).
  - FU: Docker/pgvector and the local LLM configuration (CON-FU-061).

---

## 1. Concepts

```
                     ┌──────────── clients: web · desktop · mobile · CLI · SDK/agent ────────────┐
                     │                     (UBTP over WS / TCP / HTTP)                            │
                     ▼                                                                            ▼
   ┌─────────────────────────────── enterprise ───────────────────────────────┐   ┌──── personal ────┐
   │  gateway ×N   worker ×M   scheduler (leader)   dispatcher (leaders/part.)  │   │ one process:     │
   │        │           │            │                     │                    │   │ kernel + roles + │
   │        └───────────┴──── PostgreSQL 16 + pgvector ────┴── Redis 7 ─────────│   │ SQLite + in-proc │
   │                          blob store (S3 or FS)   OTel collector   AI providers │ runtime store    │
   └────────────────────────────────────────────────────────────────────────────┘   └──────────────────┘
```

---

## 2. Model

```yaml
ENT-BuildArtifact:
  purpose: A released binary or image.
  origin: [VRD-19-01, CON-US-001, CON-LS-062]
  fields:
    - {name: name, type: string, required: true, description: "Artifact name (table below)"}
    - {name: edition, type: "enum{PERSONAL|ENTERPRISE|BOTH}", required: true, description: "Cargo features"}
    - {name: targets, type: "list<string>", required: true, description: "Platform targets"}
    - {name: embeds, type: "list<string>", required: true, description: "Embedded content (root packages, web shell)"}
```

| Artifact | Built from | Edition | Targets | Embeds |
|---|---|---|---|---|
| `ubos-server` | `ubos_server` | ENTERPRISE (also runs PERSONAL for small servers) | linux x86_64 / aarch64 (musl static) | root packages; optionally the web shell (static assets) |
| `ubos` (CLI) | `ubos_cli` | BOTH (local mode = PERSONAL) | linux, macOS, Windows (x86_64, aarch64) | root packages (for `--local`) |
| UBOS Desktop | `ubos_desktop` (Tauri 2) | PERSONAL (+ remote mode) | Windows (MSI), macOS (DMG, notarised), Linux (AppImage, deb) | kernel, root packages, web shell |
| UBOS Mobile | `@ubos/shell` in Tauri 2 mobile (ADR-006) | client only | iOS 16+, Android 10+ | web shell |
| Web shell | `@ubos/shell` | client only | static assets (served by `ubos-server` or a CDN) | – |
| SDKs | `ubos_sdk` (crates.io), `@ubos/sdk` (npm) | client only | – | – |
| Container image | `ubos-server` | ENTERPRISE | OCI, linux/amd64 + linux/arm64 | as `ubos-server` |

```yaml
ENT-Topology:
  purpose: Deployment shape (TERM-TopologyRole assignment).
  origin: [VRD-19-03, CON-UC-003, CON-UP-004]
  fields:
    - {name: name, type: "enum{PERSONAL_EMBEDDED|SINGLE_NODE|SCALED|MULTI_REGION_READ}", required: true, description: "Shape (§3.3)"}
    - {name: roles, type: "map<RuntimeRole, {instances: int, resources: {cpu, memory}}>", required: true, description: "Role placement"}
```

---

## 3. Behavior

### 3.1 Editions

### REQ-DEP-001 — Edition matrix
- **Statement:** The editions MUST differ exactly as follows, and in no other way visible to packages or clients (REQ-ARCH-008):

| Aspect | Personal | Enterprise |
|---|---|---|
| Cargo feature | `personal` | `enterprise` |
| Database | SQLite 3 (WAL) | PostgreSQL 16 + pgvector |
| Runtime store | in-process + `rt_*` SQLite tables | Redis 7 |
| Blob store | file system | file system or S3-compatible |
| Tenants | many allowed; default `local` | many |
| Concurrency | single writer | many writers, row CAS |
| Roles | all in one process | any placement (§3.3) |
| Vector search | brute force ≤ 10 000 candidates (REQ-STO-014) | HNSW |
| Partitions, archiving, RLS, dedicated tenant storage | no | yes (REQ-STO-015/016, REQ-TEN-032) |
| Transport bindings | in-process, HTTP/WS on localhost (optional LAN) | TCP, WS, HTTP |
| Authentication | local owner implicit; others as enterprise | all methods (REQ-SEC-001) |
| AI | same gateway; usually a local provider (Ollama) | same |
| Packages, protocol, ABI, scenarios | identical | identical |
- **Origin:** VRD-19-02, VRD-01-05, CON-LS-062, CON-US-042
- **Acceptance:** The shared scenario suite passes on both editions. A package exported from a personal install installs unchanged on an enterprise install.
- **Priority:** P0

### REQ-DEP-002 — One code base
- **Statement:** Edition differences MUST be confined to:
  - the `ubos_store` backends;
  - the runtime-store implementations;
  - the blob-store implementations;
  - host defaults.

  Kernel logic MUST NOT branch on the edition, except through capability queries (`store.edition()`, used by vector search and archiving). A CI build matrix compiles and tests both features.
- **Origin:** VRD-19-01, CON-LS-006
- **Acceptance:** A grep for edition checks outside the allowed modules finds none (a CI lint).
- **Priority:** P0

### 3.2 Build and release

### REQ-DEP-010 — Reproducible builds
- **Statement:** Release builds MUST:
  - pin toolchains (the Rust toolchain file, the Node version);
  - build the Rust artifacts with `--locked` and the web shell from the lockfile;
  - embed the root packages and the spec fixtures (meta-schema, ABI, URI conformance) at the kernel version;
  - publish SHA-256 checksums and an SBOM (CycloneDX) per artifact.

  Version numbers follow SemVer. The kernel version equals the `system` package version (REQ-PKG-040).
- **Origin:** NEW: operability; VRD-20-01
- **Acceptance:** Two builds of the same tag produce identical checksums for the server binary.
- **Priority:** P1

### REQ-DEP-011 — Compatibility promises
- **Statement:** Within a kernel MAJOR version:
  - UBTP v1 (REQ-PROTO-025), ABI v1 (REQ-RT-011) and the package format stay compatible;
  - store migrations are forward-only and run by `--migrate` (REQ-STO-019);
  - clients of the same MAJOR version work with any server of that MAJOR version.

  A new MAJOR version ships a migration guide and keeps the old protocol version for at least one MAJOR version.
- **Origin:** VRD-15-03, CON-UP-029, CON-LC-013
- **Acceptance:** A 1.0 CLI works against a 1.5 server, and a 1.5 web shell works against a 1.0 server (degrading unknown features).
- **Priority:** P1

### 3.3 Topologies

### REQ-DEP-020 — Supported topologies
- **Statement:** The platform MUST support these topologies with the same binaries:

| Topology | Shape | Use |
|---|---|---|
| PERSONAL_EMBEDDED | desktop or CLI process with an embedded kernel, SQLite, in-process runtime store | single user, offline, development |
| SINGLE_NODE | one `ubos-server --role all` + PostgreSQL + Redis (or SQLite + in-process for small teams) | small organisations, test environments |
| SCALED | N gateways behind a load balancer (sticky by connection, not required for HTTP) + M workers + scheduler (2 instances, leader-elected) + dispatcher (2+ instances, partition leases) + PostgreSQL (primary + replicas) + Redis (with replicas or cluster) + blob store + OTel collector | production |
| MULTI_REGION_READ | SCALED in one write region + read-only gateway pools in other regions against PostgreSQL replicas (queries, renders; writes forwarded to the write region) | later version (P2) |
- **Origin:** VRD-19-03, CON-UC-003, CON-UP-004
- **Acceptance:** The same scenario suite passes against SINGLE_NODE and SCALED deployments (REQ-ARCH-007).
- **Priority:** P0 (PERSONAL_EMBEDDED, SINGLE_NODE), P1 (SCALED), P2 (MULTI_REGION_READ)

### REQ-DEP-021 — Reference container deployment
- **Statement:** The release MUST include:
  - a `docker-compose.yml` for SINGLE_NODE: server, `pgvector/pgvector:pg16`, `redis:7`, an optional Ollama, and an optional OTel collector with Prometheus;
  - Kubernetes manifests (a Helm chart) for SCALED:
    - Deployments per role with readiness and liveness probes on `/health/*` (REQ-ARCH-011);
    - a PodDisruptionBudget for gateways;
    - a migration Job run before a rollout (`ubos-server migrate`);
    - configuration through environment variables and mounted secrets (REQ-ARCH-009).
- **Origin:** CON-US-042 (Docker pgvector + Redis), CON-FU-061, VRD-19-03
- **Acceptance:** `docker compose up` on a clean machine reaches `/health/ready`, and the admin setup token is printed in the log (REQ-SEC-004).
- **Priority:** P1

### REQ-DEP-022 — Resource baselines
- **Statement:** The documentation MUST state the minimum resources per role:

| Role | Minimum |
|---|---|
| gateway | 1 vCPU, 512 MiB |
| worker | 2 vCPU, 1 GiB |
| scheduler, dispatcher | 0.5 vCPU, 256 MiB |
| all-in-one | 2 vCPU, 2 GiB |
| PostgreSQL (small) | 2 vCPU, 4 GiB, SSD |
| Redis | 0.5 vCPU, 512 MiB |
| Personal edition | 2 GiB RAM, 500 MiB disk plus data |

  NFR targets (SPEC-32) are measured on the reference hardware defined there.
- **Origin:** NEW: operability
- **Acceptance:** A SINGLE_NODE deployment at the stated minimum passes the smoke load test in SPEC-32.
- **Priority:** P2

### 3.4 Data operations

### REQ-DEP-030 — Backup and restore
- **Statement:**
  - **Enterprise:** backups MUST use PostgreSQL continuous archiving (base backups + WAL) for point-in-time recovery, plus blob-store versioning or replication. Redis is not backed up (ADR-004).
  - **Personal:** backups use the SQLite online backup API to a file, plus a copy of the blob directory.
  - **Restore:** after a restore, the host MUST start normally. The sweeper rebuilds the queues, and dispatchers resume from the outbox (REQ-EVT-026, REQ-STO-011).

  `ubos-server backup verify` restores into a scratch database and runs a consistency check:
  - every head points to an existing version;
  - every version has an OUTPUT lineage row;
  - no search index row points to a missing commit.
- **Origin:** NFR-DUR, ADR-004
- **Acceptance:** A PITR restore to a time T followed by boot shows all processes committed before T, and none after.
- **Priority:** P1

### REQ-DEP-031 — Rolling upgrades
- **Statement:** Upgrading a SCALED deployment MUST follow these steps:
  1) run `ubos-server migrate`. Migrations are backward-compatible for one minor version: expand first, contract in the next release;
  2) roll out workers, dispatchers and the scheduler;
  3) roll out gateways, whose draining closes protocol sessions with `Goodbye{SHUTDOWN, retry_after_ms}` so that clients resume elsewhere;
  4) run `UPGRADE` boot on one instance (root package upgrades, REQ-PKG-042).

  Mixed kernel versions MUST coexist during the rollout, within one minor version.
- **Origin:** REQ-STO-019, REQ-PKG-042, REQ-ARCH-012
- **Acceptance:** A rolling upgrade under load loses no committed data, and clients reconnect automatically.
- **Priority:** P1

### REQ-DEP-032 — Host-level secrets
- **Statement:** Host secrets MUST come from environment variables or mounted files (`*_FILE` variants), and never from the database or packages. They are:
  - the database and Redis credentials;
  - the KEK (REQ-SEC-060);
  - the JWT signing keys;
  - the dialogue-token key (SPEC-19);
  - `UBOS_BOOTSTRAP_ADMIN_PASSWORD`.

  Rotation of the JWT and dialogue keys supports two active keys at once.
- **Origin:** REQ-SEC-060, REQ-ARCH-009
- **Acceptance:** Setting `UBOS_KEK_FILE` loads the key from the file, and the configuration dump shows only `***`.
- **Priority:** P1

### 3.5 Clients distribution

### REQ-DEP-040 — Client delivery
- **Statement:**
  - **Web shell:** served by `ubos-server` at `/` by default, or from a CDN with the server URL configured. It reads `/ubos-client.json` (server URL, OIDC providers, branding default) at start.
  - **Desktop and mobile:** auto-update through the Tauri updater with signed updates, from a channel URL configured per organisation.
  - **CLI:** distributed through package managers (Homebrew, winget, apt) and a signed tarball.

  All clients check server compatibility at Handshake (`PROTO.VERSION_UNSUPPORTED`, REQ-DEP-011).
- **Origin:** ADR-003, ADR-006, VRD-19-04
- **Acceptance:** A desktop app two minor versions behind shows an update prompt and still works.
- **Priority:** P1

### 3.6 Edge and sync (later version)

### REQ-DEP-050 — Branch-based sync
- **Statement:** A later version MAY let personal-edition nodes sync with a server by branches:
  - **push:** the local changes of a branch become a remote FEATURE or DRAFT branch;
  - **pull:** remote commits are merged into the local branch with SPEC-14 merge semantics;
  - identities stay the same (UUID), and commit IDs are mapped per node.

  Node boot permits and a remote package registry are part of the same later version (VRD-17-05). Version 1 does not implement sync. Desktop remote mode covers online collaboration.
- **Origin:** VRD-19-05, VRD-17-05, CON-UB-033
- **Acceptance:** – (deferred)
- **Priority:** P2

---

## 4. Interfaces

### API-DEP-001 — Server commands
- **Kind:** cli (`ubos-server`)
- **Signature / shape:**
```
ubos-server run [--role all|gateway|worker|scheduler|dispatcher]... [--config file.toml]
ubos-server migrate [--dry-run]
ubos-server boot [--strategy safe|upgrade|reset]
ubos-server backup verify --from <pitr-target|file>
ubos-server check                      # configuration, connectivity, schema version, fixtures
ubos-server version                    # kernel, protocol, ABI, system/ontology package versions, edition
```
- **Origin:** REQ-ARCH-009, REQ-STO-019, REQ-PKG-042

### API-DEP-002 — Client bootstrap file
- **Kind:** http static file
- **Signature / shape:**
```json
{ "server": "wss://ubos.acme.example/ubtp", "http": "https://ubos.acme.example",
  "oidc": [ { "provider": "ubos://acme/IdentityProvider/corp", "title": "ACME SSO" } ],
  "branding": { "title": "ACME Platform", "logo": "/assets/logo.svg" }, "min_client": "1.0.0" }
```
- **Origin:** REQ-DEP-040

### Error codes (DEP)

| Code | Category | Meaning |
|---|---|---|
| `DEP.CONFIG_INVALID` | INTERNAL | host configuration rejected (details name the key) |
| `DEP.EDITION_UNSUPPORTED` | INVALID | feature not available in this edition |

---

## 5. Non-functional

| ID | Requirement | Target |
|---|---|---|
| NFR-OPS-210 | Time to a running SINGLE_NODE from the compose file | ≤ 5 min on a clean machine |
| NFR-AVAIL-210 | Planned downtime for minor upgrades (SCALED) | 0 (rolling) |
| NFR-DUR-210 | Recovery point objective (enterprise with WAL archiving) | ≤ 5 min |
| NFR-DUR-211 | Recovery time objective (restore of 100 GB) | ≤ 2 h |
| NFR-PORT-210 | Server binary | static, no system libraries beyond libc (musl) |

---

## 6. Acceptance

| REQ | Criterion |
|---|---|
| REQ-DEP-001, 002 | Edition matrix via the scenario suite; edition-check lint |
| REQ-DEP-010, 011 | Reproducible checksums and SBOM; cross-version client/server tests |
| REQ-DEP-020…022 | Topology suites; compose and Helm smoke tests; minimum-resource smoke |
| REQ-DEP-030…032 | PITR restore consistency; rolling upgrade under load; host secret loading |
| REQ-DEP-040 | Client update and compatibility |
| REQ-DEP-050 | Deferred |

---

## 7. Implementation notes

- **Reference material:**
  - US workspace [US:Cargo.toml], development environment [US:console.md] (pgvector + Redis Docker).
  - LS feature switch [LS:crates/ubos_store/Cargo.toml].
  - FU compose and Ollama configuration [FU:backend/src/main/resources/application.yml].
- **Leader election:** Redis-based leases (SET NX PX with renewal) in the enterprise edition; trivial in-process in the personal edition.
- **Load balancing:** WebSocket and TCP sessions are long-lived. Use least-connections balancing. HTTP needs no affinity.

## 8. Open questions

None. Deferred: MULTI_REGION_READ (P2), edge sync, the package registry and boot permits (later version).
