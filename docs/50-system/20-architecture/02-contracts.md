---
id: UBS-ARC-02
title: Inter-System Contracts
status: draft
phase: ALL
depends_on: [UBS-ARC-01]
---

# Inter-System Contracts

A contract (`CTR-*`) fixes what one system may ask of another and what it can rely on. Its
operations are specified as `IF-*` items in the callee's system chapter (listed under
**Detailed in**).

Block format:
- **Parties:** caller → callee
- **Channel:** transport and protocol
- **Purpose**
- **Operations:** summary list
- **Guarantees:** authentication, authorization, idempotency, ordering, consistency
- **Failure behaviour**
- **Phase** · **Serves:** capabilities
- **Detailed in:** system chapter

## 1. Engine and host

### CTR-001 — Host API (NOD → DVM)
- **Parties:** NOD, FRG, browser host → DVM
- **Channel:** in-process Rust API (C ABI and WASM exports for other hosts)
- **Purpose:** Let a host open VAEs and execute reads, queries and processes with an authenticated execution context.
- **Operations:** `open_vae`, `begin_process(ctx)`, `invoke_port`, `read`, `query`, `commit`, `merge`, `preview`, `replay`, `export`, `import`, `verify`, `explain`, `reflect`
- **Guarantees:** The DVM trusts the context identity supplied by the host (AR-007), and re-checks authorization on every instruction. All calls are deterministic given the context and the store state.
- **Failure behaviour:** Typed errors (`<MOD>.<CODE>`). Aborted processes leave no state.
- **Phase:** PH-1a · **Serves:** CAP-TXN-01, CAP-LOGIC-02, CAP-STD-06
- **Detailed in:** UBS-SYS-DVM

### CTR-002 — Host services (DVM → NOD)
- **Parties:** DVM → NOD (host service interfaces implemented by each host)
- **Channel:** in-process trait interfaces
- **Purpose:** Provide what the kernel must not own: clocks, entropy, blob I/O, secret resolution, connector execution, model calls, outbox release, job enqueue, telemetry.
- **Operations:** `clock.now`, `entropy.fill`, `blob.put/get`, `secret.resolve(handle)` (used only inside connector execution, never returned to logic), `connector.call`, `model.ask`, `outbox.release`, `jobs.enqueue`, `telemetry.emit`
- **Guarantees:** Every non-deterministic result is returned to the DVM for journaling before use. Host services never see more data than the call needs.
- **Failure behaviour:** Host errors map to DVM errors (`LOGIC.CONNECTOR_UNAVAILABLE`, `AI.PROVIDER_UNAVAILABLE`, `NOD.STORAGE_UNAVAILABLE`).
- **Phase:** PH-1a · **Serves:** CAP-LOGIC-07, CAP-LOGIC-10, CAP-AI-09, CAP-TXN-05
- **Detailed in:** UBS-SYS-DVM, UBS-SYS-NOD

### CTR-003 — UBTP (SDK → NOD)
- **Parties:** SDK (every client) → NOD
- **Channel:** UBTP over WebSocket, HTTP, TCP or in-process; QUIC from PH-5
- **Purpose:** The only client protocol for data, actions, queries, subscriptions and sync.
- **Operations:** `Handshake`, `Mutate`, `Query`, `Subscribe`, `Emit` (server push), `Sync` (from PH-3)
- **Guarantees:** Authentication at handshake, with per-message authorization in the DVM. Idempotency keys on `Mutate`. Per-object ordering on `Subscribe`, with resumable cursors. Version negotiation.
- **Failure behaviour:** Errors carry code and explanation. Transient failures are retryable with the same idempotency key.
- **Phase:** PH-1b · **Serves:** CAP-INT-01, CAP-DEV-06
- **Detailed in:** UBS-SYS-NOD, UBS-SYS-SDK, UBS-STD (protocol clauses)

### CTR-004 — REST facade (external client → NOD)
- **Parties:** third-party systems → NOD
- **Channel:** HTTPS, JSON, OpenAPI 3.1, RFC 9457 errors
- **Purpose:** Integration with REST-oriented tools.
- **Operations:** generated resource and port endpoints, OpenAPI document
- **Guarantees:** Semantics identical to UBTP `Mutate`/`Query` (the facade is a thin mapping). OAuth 2.1 or API keys with scopes.
- **Failure behaviour:** Problem details with the platform code.
- **Phase:** PH-2 · **Serves:** CAP-INT-02, CAP-INT-08
- **Detailed in:** UBS-SYS-NOD

### CTR-005 — Authoritative storage (NOD → PostgreSQL or SQLite)
- **Parties:** NOD storage adapter → database
- **Channel:** sqlx (PostgreSQL wire protocol), embedded SQLite
- **Purpose:** Durable authoritative storage of objects, commits, references, ledgers and kernel indexes.
- **Operations:** transactional flush, snapshot reads, advisory locks for sequencing, logical replication feed
- **Guarantees:** Serializable flush transactions for head and sequence updates. Durable commit (synchronous commit on) before acknowledgement.
- **Failure behaviour:** The node enters read-only mode or unavailable state (NR-AVAIL-006). No partial commits.
- **Phase:** PH-1a · **Serves:** CAP-TXN-01, CAP-VER-01
- **Detailed in:** UBS-SYS-DVM (storage design), UBS-SYS-NOD

### CTR-006 — Object storage (NOD → S3-compatible store)
- **Parties:** NOD → object store
- **Channel:** S3 API
- **Purpose:** Blobs and chunks, cold history tier, backups, WORM records.
- **Operations:** put, get, range get, list, object lock (compliance mode)
- **Guarantees:** Content-addressed keys, so writes are idempotent. Integrity is verified by hash on read.
- **Failure behaviour:** Reads of missing chunks fail with `NOD.BLOB_UNAVAILABLE`. Commits that reference new blobs wait for blob durability first.
- **Phase:** PH-1a · **Serves:** CAP-FILE-01, CAP-VER-09, CAP-AUD-07, CAP-OPS-02
- **Detailed in:** UBS-SYS-NOD

### CTR-007 — Runtime store (NOD → Redis-compatible store)
- **Parties:** NOD → runtime store
- **Channel:** RESP protocol, or the embedded store in small mode
- **Purpose:** Caches, queues as hints, leases, rate limits, subscription fan-out.
- **Operations:** get, set, publish, stream append, lease acquire
- **Guarantees:** None beyond best effort. Losing the store must never lose data (FR-EVT-032).
- **Failure behaviour:** Degraded performance, then sweeper recovery.
- **Phase:** PH-1b · **Serves:** CAP-EVT-03
- **Detailed in:** UBS-SYS-NOD

### CTR-008 — Cluster coordination (NOD ↔ NOD)
- **Parties:** application nodes of one Server or Cell
- **Channel:** database-backed leases and the runtime-store pub/sub
- **Purpose:** Schedule leadership, job leases, cache invalidation, revocation propagation.
- **Operations:** acquire and renew leadership, publish invalidation, broadcast revocation
- **Guarantees:** At most one leader per schedule slot (FR-EVT-043). Invalidations reach every node within the NR bounds or trigger a full cache reset.
- **Failure behaviour:** Leadership moves after lease expiry.
- **Phase:** PH-1b · **Serves:** CAP-EVT-04, CAP-IAM-10
- **Detailed in:** UBS-SYS-NOD

## 2. Experience

### CTR-010 — View protocol (WSP → NOD)
- **Parties:** WSP renderers (web, desktop, mobile, portal) → NOD
- **Channel:** UBTP messages specialised for views (`Query` of view payloads, `Mutate` for actions, `Subscribe` for live updates)
- **Purpose:** Server-driven UI.
- **Operations:** `view.resolve(object|class, mode, context)`, `view.validate(draft)`, `actions.available`, `action.invoke`, `timeline.get`, `diff.get`
- **Guarantees:** Payloads contain only permitted fields and actions, with state-dependent flags. The widget catalogue version is negotiated.
- **Failure behaviour:** Unknown widgets fall back to generic rendering and are reported.
- **Phase:** PH-2 · **Serves:** CAP-UX-01, CAP-UX-03, CAP-UX-06
- **Detailed in:** UBS-SYS-WSP, UBS-SYS-NOD

### CTR-011 — Builder operations (STU → NOD)
- **Parties:** STU → NOD
- **Channel:** UBTP
- **Purpose:** Modelling, change sets, simulations and releases.
- **Operations:** reflection, change-set lifecycle, merge preview, simulation start and results, impact analysis, release tagging, Forge checks (executed by the Forge service on the node)
- **Guarantees:** All builder changes are ordinary commits on branches. There are no builder-only write paths.
- **Failure behaviour:** Standard errors. Long operations return job handles.
- **Phase:** PH-2 · **Serves:** CAP-VER-03, CAP-RULE-07, CAP-MODEL-08
- **Detailed in:** UBS-SYS-STU, UBS-SYS-NOD

## 3. Toolchain and standard

### CTR-012 — Forge service on a node (NOD → FRG library)
- **Parties:** NOD → FRG (linked library)
- **Channel:** in-process
- **Purpose:** Run checks, type generation, lens verification and simulations server-side for Studio and agents.
- **Operations:** `check(branch)`, `types(branch)`, `verify_lens`, `simulate(spec)`, `verify_buk(archive)`
- **Guarantees:** The same results as the CLI for the same inputs.
- **Failure behaviour:** Findings are data, not errors. Budget exhaustion returns partial results.
- **Phase:** PH-2 · **Serves:** CAP-DEV-04, CAP-PKG-06
- **Detailed in:** UBS-SYS-FRG

### CTR-013 — Forge to node (FRG CLI → NOD)
- **Parties:** FRG CLI → NOD
- **Channel:** UBTP through the Rust SDK
- **Purpose:** Install, test against, and pull models and history from a VAE.
- **Operations:** install Buk into a VAE, run scenarios over UBTP, pull history for simulation, push types
- **Guarantees:** The same authorization as any client.
- **Failure behaviour:** Standard errors.
- **Phase:** PH-1b · **Serves:** CAP-DEV-01, CAP-DEV-05
- **Detailed in:** UBS-SYS-FRG

### CTR-014 — Conformance package (BPA → FRG and implementers)
- **Parties:** BPA releases → FRG conformance runner and third-party implementers
- **Channel:** versioned data archive (vectors, schemas, profiles, manifest)
- **Purpose:** Language-neutral conformance testing.
- **Operations:** load profile, run vectors through an adapter, emit a signed report
- **Guarantees:** Vectors are immutable per standard release. The adapter protocol is stable within a major version.
- **Failure behaviour:** Unsupported vectors are reported as `not_applicable` only when outside the claimed profiles.
- **Phase:** PH-0 · **Serves:** CAP-STD-02, CAP-STD-05
- **Detailed in:** UBS-SYS-BPA, UBS-SYS-FRG

### CTR-015 — Publish to Exchange (FRG → EXC)
- **Parties:** FRG → EXC
- **Channel:** HTTPS API
- **Purpose:** Submit signed Buk archives for verification and listing.
- **Operations:** upload, request verification, publish version, deprecate
- **Guarantees:** Archive integrity is checked by root hash and publisher signature.
- **Failure behaviour:** A verification failure blocks listing, with findings returned.
- **Phase:** PH-4 · **Serves:** CAP-PKG-06, CAP-BILL-03
- **Detailed in:** UBS-SYS-EXC

## 4. Intelligence

### CTR-020 — Model calls (NOD → AGT gateway)
- **Parties:** NOD (for `ask.model`, embeddings, copilot) → AGT model gateway
- **Channel:** internal gRPC or HTTP (mTLS)
- **Purpose:** A single governed path to models.
- **Operations:** `complete(route, prompt, schema)`, `embed(route, content)`, usage report
- **Guarantees:** Classification enforcement and redaction before egress. The response is returned for journaling. Metering is attributed to tenant and principal.
- **Failure behaviour:** `AI.PROVIDER_UNAVAILABLE`, `AI.CLASSIFICATION_BLOCKED`, `AI.BUDGET_EXHAUSTED`.
- **Phase:** PH-3 · **Serves:** CAP-AI-01, CAP-AI-09, CAP-QRY-03
- **Detailed in:** UBS-SYS-AGT

### CTR-021 — Agent operations (AGT → NOD)
- **Parties:** AGT agent runtime → NOD
- **Channel:** UBTP with agent principal credentials
- **Purpose:** Agents act as principals: open agent branches, call tools, run previews, submit change sets.
- **Operations:** `branch.open(agent)`, tool invocations (ports and queries), `validate`, `preview`, `changeset.submit`, feedback retrieval
- **Guarantees:** Agents can write only to their branch (FR-AI-031). No merge rights (FR-AI-025).
- **Failure behaviour:** Scope violations are denied and logged. The kill switch cancels in-flight calls.
- **Phase:** PH-3 · **Serves:** CAP-AI-02, CAP-AI-03, CAP-AI-04
- **Detailed in:** UBS-SYS-AGT, UBS-SYS-NOD

### CTR-022 — MCP (external AI client → AGT)
- **Parties:** external MCP clients → AGT MCP server
- **Channel:** MCP over HTTP (streamable) with OAuth 2.1 delegation
- **Purpose:** Let users' AI assistants use UBOS tools with delegated rights.
- **Operations:** list tools and resources, call tool, read resource
- **Guarantees:** The tool set is filtered by delegation scope. Calls are attributed "client on behalf of user".
- **Failure behaviour:** `IAM.DELEGATION_EXPIRED`, `IAM.DENIED`.
- **Phase:** PH-3 · **Serves:** CAP-AI-04, CAP-IAM-10
- **Detailed in:** UBS-SYS-AGT

### CTR-023 — Model providers (AGT → provider)
- **Parties:** AGT → model providers or self-hosted inference
- **Channel:** provider APIs over TLS
- **Purpose:** Inference and embeddings.
- **Operations:** provider-specific, adapted behind the gateway
- **Guarantees:** Contractual no-training terms (CR-CCPA-004) and region pinning.
- **Failure behaviour:** Policy-compliant fallback or error.
- **Phase:** PH-3 · **Serves:** CAP-AI-01
- **Detailed in:** UBS-SYS-AGT

### CTR-024 — Copilot sessions (WSP → AGT)
- **Parties:** WSP → AGT copilot, through NOD
- **Channel:** UBTP streaming messages proxied by NOD
- **Purpose:** In-context assistance with page context.
- **Operations:** open session, ask, receive streamed answer and proposals, accept a proposal into a draft
- **Guarantees:** The copilot acts with the user's permissions. Proposals never commit without user action.
- **Failure behaviour:** The copilot degrades without affecting Workspace functions (NR-AVAIL-005).
- **Phase:** PH-3 · **Serves:** CAP-AI-06
- **Detailed in:** UBS-SYS-AGT, UBS-SYS-WSP

## 5. Proof

### CTR-030 — Notary to node (NTY → NOD)
- **Parties:** NTY → NOD
- **Channel:** UBTP with the Notary service principal
- **Purpose:** Obtain tenant roots and proofs; commit anchor receipts; build evidence packs.
- **Operations:** `root.current(tenant)`, `proof.inclusion`, `proof.consistency`, `evidence.collect(scope)`, append `AnchorReceipt` entries
- **Guarantees:** Read-only access to business data scoped to evidence requests authorised by tenant users. Receipts are written into the tenant's own ledger (AR-005).
- **Failure behaviour:** Anchoring retries with alerts (FR-PROOF-043).
- **Phase:** PH-3 · **Serves:** CAP-PROOF-04, CAP-PROOF-05
- **Detailed in:** UBS-SYS-NTY

### CTR-031 — Time-stamp authorities (NTY → TSA)
- **Parties:** NTY → RFC 3161 TSA (and optional public-ledger timestamping)
- **Channel:** RFC 3161 over HTTPS
- **Purpose:** External time evidence for roots.
- **Operations:** timestamp request and response
- **Guarantees:** The receipt is verifiable with the TSA's certificate chain.
- **Failure behaviour:** Retry, or a second TSA.
- **Phase:** PH-3 · **Serves:** CAP-PROOF-04
- **Detailed in:** UBS-SYS-NTY

### CTR-032 — Evidence and export formats (NTY or NOD → verifier)
- **Parties:** evidence packs, signed PDFs and exports → open verifier (any party)
- **Channel:** files (specified formats)
- **Purpose:** Independent verification.
- **Operations:** verify pack, verify seal, verify export
- **Guarantees:** The formats are specified in the BPA (FR-PROOF-063).
- **Failure behaviour:** A report pinpoints the failing items.
- **Phase:** PH-3 · **Serves:** CAP-PROOF-06
- **Detailed in:** UBS-SYS-NTY, UBS-STD

## 6. Bridge

### CTR-040 — Bridge to node (BRG → NOD)
- **Parties:** BRG → NOD
- **Channel:** UBTP with the Bridge service principal per tenant
- **Purpose:** Ingest external data through ports and bulk processes; consume change feeds for export.
- **Operations:** bulk import processes, port invocations with idempotency keys, `Subscribe` change feed with cursors
- **Guarantees:** Every ingestion is a normal governed process. The change feed is ordered per object and resumable.
- **Failure behaviour:** Resume from cursor. Rejected rows are reported with reasons.
- **Phase:** PH-2 · **Serves:** CAP-MIG-01, CAP-ANL-05, CAP-INT-07
- **Detailed in:** UBS-SYS-BRG

### CTR-041 — External systems (NOD connectors and BRG adapters → external)
- **Parties:** NOD connector runtime (for `call.connector`) and BRG adapters (files, bulk) → external systems
- **Channel:** HTTPS, SFTP, SMTP and IMAP, Kafka, cloud queues, read-only SQL
- **Purpose:** Integration.
- **Operations:** declared connector operations
- **Guarantees:** Egress allow-lists, classification checks, recorded exchanges, rate limits.
- **Failure behaviour:** Circuit breakers and retries (FR-LOGIC-105).
- **Phase:** PH-2 · **Serves:** CAP-LOGIC-10, CAP-INT-04
- **Detailed in:** UBS-SYS-NOD, UBS-SYS-BRG

### CTR-042 — Analytics sinks (BRG → lakehouse)
- **Parties:** BRG → Parquet and Iceberg sinks, embedded analytics engine
- **Channel:** object store and catalog APIs
- **Purpose:** The analytics exit (IMP-09).
- **Operations:** incremental snapshot commits per commit range
- **Guarantees:** Exactly-once per commit range. Masking by sink clearance.
- **Failure behaviour:** Retry of the range. Watermark not advanced.
- **Phase:** PH-3 · **Serves:** CAP-ANL-04, CAP-ANL-05
- **Detailed in:** UBS-SYS-BRG

## 7. Control and identity

### CTR-050 — Fleet administration (CTL → NOD)
- **Parties:** CTL → NOD admin endpoint
- **Channel:** HTTPS with mTLS, CTL service identity
- **Purpose:** Register nodes, observe health, apply configuration, orchestrate upgrades and migrations, manage tenant lifecycle.
- **Operations:** `node.register`, `node.health`, `config.apply`, `upgrade.step`, `tenant.create/suspend/archive/delete`, `tenant.migrate.*`, `usage.pull`
- **Guarantees:** No business-data operations exist on this endpoint (AR-006). All operations are audited.
- **Failure behaviour:** Idempotent steps allow resumable orchestration.
- **Phase:** PH-4 · **Serves:** CAP-OPS-08, CAP-TEN-05, CAP-TEN-07
- **Detailed in:** UBS-SYS-CTL, UBS-SYS-NOD

### CTR-051 — Usage and heartbeat (NOD → CTL)
- **Parties:** NOD → CTL
- **Channel:** HTTPS with mTLS
- **Purpose:** Metering and liveness.
- **Operations:** push usage records (hash-chained), heartbeat, version report
- **Guarantees:** At-least-once, with record IDs for deduplication.
- **Failure behaviour:** Local buffering. Air-gapped nodes export usage files instead.
- **Phase:** PH-4 · **Serves:** CAP-BILL-01
- **Detailed in:** UBS-SYS-CTL

### CTR-052 — Identity providers (IdP → NOD)
- **Parties:** customer identity providers → NOD
- **Channel:** OIDC, SAML 2.0, SCIM 2.0
- **Purpose:** Enterprise sign-in and provisioning.
- **Operations:** authentication flows, SCIM user and group operations
- **Guarantees:** Signed assertions and tokens. Deprovisioning propagates within the NR bound.
- **Failure behaviour:** Local break-glass accounts per policy.
- **Phase:** PH-2 · **Serves:** CAP-IAM-02, CAP-IAM-07
- **Detailed in:** UBS-SYS-NOD

### CTR-053 — Licensing and billing (CTL ↔ EXC)
- **Parties:** CTL ↔ EXC
- **Channel:** HTTPS
- **Purpose:** Licences, entitlements, invoices and payouts.
- **Operations:** licence issue and verify, entitlement sync, usage-based invoice lines, payout statements
- **Guarantees:** Signed licence objects. Fiat only (DEC-012).
- **Failure behaviour:** Grace periods, and data is never held hostage (FR-PKG-082).
- **Phase:** PH-4 · **Serves:** CAP-BILL-02, CAP-BILL-03, CAP-BILL-04
- **Detailed in:** UBS-SYS-EXC, UBS-SYS-CTL

### CTR-054 — Buk retrieval (NOD → EXC)
- **Parties:** NOD → EXC
- **Channel:** HTTPS
- **Purpose:** Download verified Buk archives and licence files.
- **Operations:** search, fetch archive, fetch verification record, fetch licence
- **Guarantees:** Archives are verified locally (hash and signature) before loading.
- **Failure behaviour:** Offline installation from files is always possible.
- **Phase:** PH-4 · **Serves:** CAP-PKG-03
- **Detailed in:** UBS-SYS-EXC

## 8. Replication and federation

### CTR-060 — Device sync (Box or mobile ↔ Server)
- **Parties:** NOD(Box) ↔ NOD(Server)
- **Channel:** UBTP `Sync` over WebSocket or HTTP
- **Purpose:** Offline-first replication within an organisation.
- **Operations:** head exchange, have/want, chunk transfer, device-branch push, rejection return
- **Guarantees:** Integrity on receipt, authorised scope, resumability (CAP-SYNC-01 … CAP-SYNC-04).
- **Failure behaviour:** Resume. Rejected items are returned with explanations.
- **Phase:** PH-3 · **Serves:** CAP-SYNC-01, CAP-SYNC-02
- **Detailed in:** UBS-SYS-NOD, UBS-STD (sync protocol)

### CTR-061 — Federation registry (NOD ↔ FED)
- **Parties:** NOD ↔ FED registry
- **Channel:** HTTPS and DNS
- **Purpose:** Register VAEs, resolve DIDs and endpoints, manage trust relationships.
- **Operations:** `register`, `resolve`, `trust.propose/accept/end`
- **Guarantees:** Signed registrations. Cached resolution with TTL.
- **Failure behaviour:** Cached operation during registry outages (FR-SYNC-052).
- **Phase:** PH-5 · **Serves:** CAP-SYNC-05
- **Detailed in:** UBS-SYS-FED

### CTR-062 — Cross-organisation sync (NOD ↔ NOD)
- **Parties:** nodes of different organisations
- **Channel:** UBTP `Sync` over QUIC with mutual TLS bound to DIDs
- **Purpose:** VAE trees across nodes and shared BPUs.
- **Operations:** fallback fetch, shared-object replication, multi-party signature exchange
- **Guarantees:** Only the interactions allowed by the trust relationship. Multi-party approval for shared Definition state.
- **Failure behaviour:** Local autonomy continues. Staleness is reported.
- **Phase:** PH-5 · **Serves:** CAP-SYNC-06, CAP-SYNC-07
- **Detailed in:** UBS-SYS-FED

## 9. Workers and outbound channels

### CTR-070 — Worker jobs (Worker ↔ NOD)
- **Parties:** NOD(Worker role) ↔ NOD(Server or Cell)
- **Channel:** UBTP with worker principal
- **Purpose:** Enterprise-internal batch execution (DEC-002).
- **Operations:** lease job, fetch inputs (classification-checked), return result with hashes
- **Guarantees:** Only verified components. Sampled re-execution.
- **Failure behaviour:** Lease expiry and quarantine.
- **Phase:** PH-4 · **Serves:** CAP-EVT-06
- **Detailed in:** UBS-SYS-NOD

### CTR-071 — Notification providers (NOD → providers)
- **Parties:** NOD → e-mail, push and chat providers
- **Channel:** provider APIs
- **Purpose:** Deliver notifications and Action Messages.
- **Operations:** send, delivery status callback
- **Guarantees:** Per-recipient rendering with masking. Delivery records.
- **Failure behaviour:** Retries and fallbacks.
- **Phase:** PH-2 · **Serves:** CAP-NTF-02, CAP-OFFICE-06
- **Detailed in:** UBS-SYS-NOD

### CTR-072 — Telemetry and SIEM export (NOD → collectors)
- **Parties:** NOD → OpenTelemetry collectors and SIEM sinks
- **Channel:** OTLP, HTTPS
- **Purpose:** Observability and security monitoring.
- **Operations:** export metrics, traces, logs and security events
- **Guarantees:** No business values (FR-OPS-042). At-least-once for security events.
- **Failure behaviour:** Local buffering with bounded size.
- **Phase:** PH-1b · **Serves:** CAP-OPS-04, CAP-AUD-01
- **Detailed in:** UBS-SYS-NOD

## 10. Contract rules

### AR-007 — Identity is established at the edge
- **Rule:** Authentication MUST happen in NOD (or AGT for MCP) at the channel edge. The DVM receives an execution context whose principal chain is attested by the host and MUST NOT accept identity from payloads.
- **Realises:** FR-LOGIC-022, FR-IAM-041
- **Rationale:** One trust boundary for identity.

### AR-008 — Every contract is versioned
- **Rule:** Every `CTR-*` MUST carry a version negotiated or declared at connection time. Breaking changes require a new major version, supported in parallel for at least one minor release of the callee.
- **Realises:** NR-COMPAT-002, FR-INT-013
- **Rationale:** Rolling upgrades.

### AR-009 — Service principals per tenant
- **Rule:** Platform services acting inside a tenant (BRG, NTY, AGT) MUST use per-tenant service principals with explicit, reviewable grants, never a global superuser.
- **Realises:** FR-IAM-031, CR-SOC2-002
- **Rationale:** Least privilege for platform components.

### AR-010 — Idempotent cross-system writes
- **Rule:** Every contract operation that changes state MUST accept an idempotency key or be naturally idempotent (content-addressed writes).
- **Realises:** FR-TXN-031
- **Rationale:** Retries are normal across system boundaries.
