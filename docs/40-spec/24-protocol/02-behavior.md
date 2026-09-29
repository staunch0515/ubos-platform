---
id: SPEC-24-2
title: "UBTP Protocol — Behavior"
status: complete
phase: P4
depends_on: [SPEC-24, SPEC-02, SPEC-10, SPEC-15, SPEC-16, SPEC-19, SPEC-21, SPEC-22]
sources: [UP, FU, LC, UB, UC, US, LS]
---

# UBTP Protocol — Behavior

Part 2 of SPEC-24 (`24-protocol/`). Header, concepts and file list: `00-index.md`.

---

## 3. Behavior

### 3.1 Sessions and sequencing

### REQ-PROTO-001 — Handshake first
- **Statement:** On TCP and WebSocket, the first message of a connection MUST be `Handshake`. Any other message fails with `PROTO.HANDSHAKE_REQUIRED` and the connection is closed. The server then:
  1) picks the highest common protocol version (`PROTO.VERSION_UNSUPPORTED` if there is none) and the encoding (JSON if no common binary encoding);
  2) authenticates (REQ-SEC-001);
  3) resolves the context (REQ-CTX-002, REQ-CTX-015);
  4) creates the protocol session;
  5) answers `Welcome`.

  If a `resume` refers to a live session of the same principal, the server restores its subscriptions and delivers the events after `event_cursor` that are still retained (REQ-PROTO-011).
- **Origin:** VRD-15-01, CON-LS-002, CON-LS-003, CON-UP-053
- **Acceptance:**
  1) A Query before Handshake closes the connection with `PROTO.HANDSHAKE_REQUIRED`.
  2) A reconnect with `resume` receives the events missed while offline.
- **Priority:** P0

### REQ-PROTO-002 — Per-request sequence
- **Statement:** For each request *r*, the server MUST send, in order:
  1) for Mutate and Emit, an `Ack{re: r.id, process_id}` as soon as the process starts. For `execution: ASYNC`, the Ack also carries `job`;
  2) zero or more `Op{re: r.id}`;
  3) exactly one terminal `Result{re}` or `Error{re}`.

  After the terminal message, no further messages carry `re = r.id`. Messages of different requests MAY interleave. The server processes requests of one session concurrently, up to `max_inflight`. It gives no ordering guarantee between requests, so clients that need ordering wait for the terminal message.
- **Origin:** CON-LS-003, VRD-15-04
- **Acceptance:** A slow Emit and a fast Query in flight together receive their own messages. Every request ends with exactly one terminal message.
- **Priority:** P0

### REQ-PROTO-003 — Synchronous vs asynchronous emits
- **Statement:**
  - An Emit whose operation resolves to SYNC execution MUST return its result in the terminal `Result`.
  - One that resolves to ASYNC MUST return `Result{job, status: ACCEPTED}` right after the Job is committed. The client follows progress through a `JOB` subscription, or through `Op{PROGRESS}` if it stays connected and the request asked for `follow: true`.
  - AUTO chooses ASYNC for operations marked `long_running`.
- **Rationale:** UC's submit → poll → present, and US/UC's synchronous transitions (VRD-15-04).
- **Origin:** VRD-15-04, CON-UC-059, CON-UC-003
- **Acceptance:** A report pipeline marked `long_running` returns ACCEPTED with a Job URI, and the JOB subscription receives `job.succeeded` with the result reference.
- **Priority:** P0

### REQ-PROTO-004 — Cancellation
- **Statement:** `Cancel{target}` MUST set the cancellation token of the target request's process (REQ-CTX-013). The outcome depends on how far the process has got:
  - if the process has not reached FLUSHING, it ends with `Error{TX.ABORTED}`;
  - if it is flushing or finished, the cancel has no effect and `Result{cancelled: false}` is returned;
  - cancelling an ASYNC job's request does not cancel the job, which has its own `job.cancel` action (SPEC-20).
- **Origin:** NEW: interactive clients and agents need cancellation
- **Acceptance:** Cancelling a long logic run returns `TX.ABORTED`, and nothing is committed.
- **Priority:** P1

### REQ-PROTO-005 — Heartbeat, idle and expiry
- **Statement:** Clients MUST send a `Ping` at least every `heartbeat_s` (default 30 s) when idle. The server closes a connection after three missed heartbeats. When the session's token expires, the server sends `Goodbye{AUTH_EXPIRED}`, unless a `Reauth` arrived before expiry. Suspending the tenant closes its sessions with `Goodbye{SUSPENDED}`.
- **Origin:** CON-LS-003, REQ-SEC-001, REQ-TEN-003
- **Acceptance:** A silent client is disconnected after about 90 s. `Reauth` extends a session without a reconnect.
- **Priority:** P1

### 3.2 Operations

### REQ-PROTO-010 — Operation semantics
- **Statement:** The kernel MUST implement the operations with these semantics, reusing the chapters named:

| Operation | Behaviour |
|---|---|
| `Mutate` | REQ-TX-030 (process kind MUTATE); idempotency, dry run and level from the envelope |
| `Emit` with `target` = entity or type and `signal` = Action URI | the action stage (REQ-FLOW-011…014), default stage EXECUTE |
| `Emit` with `target` = Pipeline URI | REQ-FLOW-001 |
| `Emit` with `target` = Workflow URI and `signal` = `start` | REQ-FLOW-030 |
| `Emit` with `target` = ProcessInstance URI or Workflow + `correlation` | REQ-FLOW-031/032 |
| `Emit` with `target` = Logic URI | `logic.run@v1` (REQ-RT-002), permission `logic.execute` |
| `Query.get` | resolve and return effective data (REQ-META-020), with `view` (SPEC-25) and `fields`/`expand` |
| `Query.criteria` | REQ-QRY-001 |
| `Query.named` | REQ-QRY-010 (includes every `ubos://system/Query/*` kernel query) |
| `Query.similar` | REQ-QRY-031 |
| `Subscribe` | REQ-PROTO-011 |

  An Emit target that is not an operation, action target or instance fails with `PROTO.TARGET_INVALID`.
- **Origin:** VRD-15-01, VRD-15-03, CON-LS-004, CON-UB-003
- **Acceptance:** A scenario suite exercises every row over each binding.
- **Priority:** P0

### REQ-PROTO-011 — Subscriptions
- **Statement:** `Subscribe` MUST do the following:
  - authorise each filter (read permission on the target and types, REQ-SEC-020);
  - register the filters in the session, at most `max_subscriptions` (default 100);
  - return a subscription ID and the current event cursor.

  Events come from the dispatcher (SPEC-20):
  - each event is checked against the filters and against the principal's read permission **at delivery time**;
  - the event data is masked;
  - deliveries are numbered (`seq`) per subscription and carry a resumable `cursor`.

  Retention for resume is at least 10 minutes or 10 000 events per tenant (runtime store). Beyond that, the server reports a gap with `Event{type: "subscription.gap"}`, after which the client re-queries.
- **Origin:** VRD-11-04, CON-LS-028, CON-UC-059
- **Acceptance:**
  1) A TYPE subscription on invoices receives `entity.committed` events for invoices the user can read only.
  2) A resume after 2 minutes delivers the missed events in order.
- **Priority:** P0 (ENTITY, TYPE, JOB, NOTIFICATIONS), P1 (EVENT, INSTANCE), P2 (QUERY live)

### REQ-PROTO-012 — Pushed UI and notifications
- **Statement:**
  - UI emits addressed to `client://current` MUST be delivered as `Op{kind: UI}` in the stream of the request whose process emitted them, after its flush.
  - UI emits to `client://session/{id}` go to that session, if it belongs to the same principal or the emitter has `ubos.ui.push_any`.
  - NOTIFY emits go to all sessions of the target principal that subscribed to NOTIFICATIONS.
  - A client without the `PUSH_UI` capability receives UI emits as `Op{kind: WARNING, body: {code: PROTO.PUSH_UNSUPPORTED}}`.
- **Origin:** VRD-13-06, CON-LS-005, CON-LS-051, CON-UC-056
- **Acceptance:** An action that pushes a toast shows it in the caller's cockpit, and a notification reaches the principal's mobile session.
- **Priority:** P1

### 3.3 Bindings and encodings

### REQ-PROTO-020 — TCP binding
- **Statement:**
  - **Framing:** each frame is a 4-byte big-endian unsigned length followed by the encoded message (LS codec). The maximum frame is 16 MiB (`PROTO.FRAME_TOO_LARGE`, and the connection is closed).
  - **Port and TLS:** the default port is 9876. TLS 1.3 is required, except on loopback or when the host configuration sets `tcp_insecure_loopback_only`.
  - **Encoding:** chosen at Handshake. The Handshake frame itself is always JSON.
- **Origin:** VRD-15-02, CON-LS-002, CON-LS-001
- **Acceptance:** The Rust SDK and the CLI run the scenario suite over TCP with JSON and with msgpack.
- **Priority:** P0

### REQ-PROTO-021 — WebSocket binding
- **Statement:** The server MUST accept WebSocket upgrades at `GET /ubtp`, with subprotocols `ubtp.v1.json` or `ubtp.v1.msgpack`. Each UBTP message is carried in one WebSocket message: text frames for JSON, binary frames for msgpack. Authentication is in the Handshake message, never in the URL. The same limits apply as for TCP.
- **Origin:** VRD-15-02
- **Acceptance:** The web shell connects over WebSocket, and its sessions resume after a network blip.
- **Priority:** P0

### REQ-PROTO-022 — HTTP binding
- **Statement:** The server MUST provide the following over HTTP:

| Route | Maps to | Notes |
|---|---|---|
| `POST /ubtp/{op}` (op ∈ mutate, emit, query) | the message with that op; body = the message fields without `v`/`op`/`id` | response: terminal message body; `Accept: application/x-ndjson` streams all messages as NDJSON |
| `GET /entity?uri=…&view=&fields=&expand=` | `Query.get` | convenience |
| `POST /invoke` | `Emit` | convenience (UC `/invoke`) |
| `POST /query` | `Query` | convenience |
| `PUT /blob` (streamed body, `Content-Type`) | blob upload (REQ-STO-018) | returns `{sha256, size}`; a File entity is created by a following Mutate or by `?create_file=true` |
| `GET /blob/{sha256}` | blob download | requires read of a File referencing it |
| `GET /ubtp` (upgrade) | WebSocket | REQ-PROTO-021 |
| `/auth/*` | SPEC-22 API-SEC-002 | |
| `/health/*` | SPEC-10 | |
| `GET /openapi.json` | REQ-PROTO-030 | |

  The HTTP binding carries the envelope fields as headers:
  - authentication: `Authorization: Bearer` or `X-API-Key`;
  - context: `X-UBOS-Context: tenant[.context]`;
  - `Idempotency-Key`, `X-Request-Id`, `traceparent` and `Accept-Language`.

  HTTP status codes follow the error category (SPEC-02 §8.2). A successful non-streamed request returns 200, or 202 for `ACCEPTED`. HTTP requests are stateless: each is its own short protocol session without subscriptions.
- **Origin:** VRD-15-02, VRD-15-05, CON-LC-050, CON-UC-050, CON-UP-050
- **Acceptance:**
  1) `curl` with an API key can create an entity through `POST /ubtp/mutate` and read it through `GET /entity`.
  2) A validation failure returns 422 with causes.
- **Priority:** P0

### REQ-PROTO-023 — In-process binding
- **Statement:** Embedded hosts (desktop, CLI local mode, tests) MUST use an in-process binding that passes the same message structures through channels, without serialisation. It has the same sequencing and authentication semantics. The personal-edition owner principal authenticates implicitly (REQ-SEC-001 "local owner").
- **Origin:** VRD-19-01, ADR-003, CON-US-001
- **Acceptance:** The desktop shell runs the same scenario suite through the in-process binding.
- **Priority:** P1

### REQ-PROTO-024 — Encodings
- **Statement:**
  - **JSON** (UTF-8) is mandatory.
  - **MessagePack** is optional and negotiated at Handshake. In msgpack, int64 values (commit IDs, heights) MAY be native integers. In JSON they are strings (REQ-CONV-033).
  - **Decimals** are strings in both encodings (REQ-CONV-037).
  - **Binary content** above 1 MiB MUST use the blob routes rather than inline base64.
- **Origin:** VRD-20-05, VRD-15-02
- **Acceptance:** The same scenario yields semantically equal results in both encodings.
- **Priority:** P0 (JSON), P1 (msgpack)

### 3.4 Versioning and compatibility

### REQ-PROTO-025 — Protocol evolution
- **Statement:** Within protocol version 1:
  - new optional fields and new `Op.kind` or `Event.type` values MAY be added;
  - receivers MUST ignore unknown fields ("must-ignore");
  - new `op` values are never added without a new protocol version;
  - removing or changing the meaning of a field requires version 2.

  Servers MUST support version 1 for at least the lifetime of the platform's first major release. Named operations evolve through their own `@vN` qualifiers (REQ-URI-010), not through the protocol version.
- **Origin:** VRD-15-03, CON-UP-029, CON-LC-013
- **Acceptance:** A version-1 client ignores an added field in `Welcome` without error.
- **Priority:** P0

### 3.5 Integration surface

### REQ-PROTO-030 — OpenAPI description
- **Statement:** The server MUST generate `GET /openapi.json` (OpenAPI 3.1) that describes:
  - the HTTP binding routes;
  - for each named operation the caller may execute (Actions, Pipelines, Queries in its read tenants), a path `/invoke/{operation}` whose request schema is the operation's `params_schema` or `input_schema` and whose response schema is its output schema, when declared.

  The document is generated per principal and cached per policy-set fingerprint.
- **Rationale:** External integration through standard tooling. Named operations are the API (LC), so their schemas are the contract.
- **Origin:** VRD-15-05, CON-LC-001, CON-FU-002
- **Acceptance:** An OpenAPI generator produces a client that invokes `acme.invoice.approve@v1` successfully.
- **Priority:** P2

### REQ-PROTO-031 — Machine clients
- **Statement:** Machine clients (integrations) SHOULD use the HTTP binding with scoped API keys (REQ-SEC-001). Outbound integration is by webhooks (SPEC-20). The server MUST apply the rate limits of REQ-SEC-062 to API keys per key.
- **Origin:** VRD-15-05, CON-FU-003, CON-FU-060
- **Acceptance:** An integration with a READ scope key is refused on `POST /ubtp/mutate` with 403.
- **Priority:** P1

### 3.6 Scenarios

### REQ-PROTO-040 — Scenario files
- **Statement:** A scenario is a YAML or JSON document with:
  - `name`, `description`, `context`, `principal` (`as: username or service account`), and `setup?` (other scenario files);
  - `steps: [{send: <UBTP message without v/id>, expect: [Matcher], save?: {var: jsonpath}}]`.

  Matchers:
  - `{terminal: Result|Error}`;
  - `{path: jsonpath, equals|matches|exists|contains: value}`;
  - `{error_code: code}`;
  - `{ops: {kind, count}}`;
  - `{events: {sub, type, within_ms}}`.

  Values may reference saved variables as `${var}`. The CLI runs scenarios over any binding (`ubos scenario run file --binding tcp|ws|http|local`) and reports pass/fail per step. Packages ship scenarios as acceptance tests (SPEC-28, VRD-22-02).
- **Origin:** VRD-22-02, CON-US-004, CON-US-043, CON-LS-050, CON-LC-061
- **Acceptance:** The "invoice approve" scenario passes identically over the four bindings.
- **Priority:** P0
