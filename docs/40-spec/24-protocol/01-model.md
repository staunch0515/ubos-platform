---
id: SPEC-24-1
title: "UBTP Protocol — Model"
status: complete
phase: P4
depends_on: [SPEC-24, SPEC-02, SPEC-10, SPEC-15, SPEC-16, SPEC-19, SPEC-21, SPEC-22]
sources: [UP, FU, LC, UB, UC, US, LS]
---

# UBTP Protocol — Model

Part 1 of SPEC-24 (`24-protocol/`). Header, concepts and file list: `00-index.md`.

---

## 2. Model

### 2.1 Request envelope

```yaml
ENT-RequestEnvelope:
  purpose: Common fields of every client-to-server message (TERM-Envelope).
  origin: [VRD-15-01, CON-LC-003, CON-LS-002]
  fields:
    - {name: v, type: int32, required: true, description: "Protocol version (1)"}
    - {name: op, type: "enum{Handshake|Mutate|Emit|Query|Subscribe|Unsubscribe|Cancel|Ping|Reauth|Bye}", required: true, description: "Message kind"}
    - {name: id, type: string(64), required: true, description: "Client-chosen request id, unique among in-flight requests of the session"}
    - {name: authority, type: string?, required: false, description: "Per-request context override (tenant[.context]); default = session context (REQ-CTX-015)"}
    - {name: idempotency_key, type: string(128)?, required: false, description: "Mutate/Emit only (REQ-TX-015)"}
    - {name: dry_run, type: bool?, required: false, description: "Mutate/Emit only (REQ-TX-025)"}
    - {name: timeout_ms, type: int32?, required: false, description: "Requested deadline (≤ server max)"}
    - {name: traceparent, type: string?, required: false, description: "W3C trace context (SPEC-30)"}
    - {name: locale, type: string?, required: false, description: "Override of the effective locale for messages and formatting"}
```

### 2.2 Client operations

| op | Fields (in addition to the envelope) | Terminal result |
|---|---|---|
| `Handshake` | `client {kind: WEB\|DESKTOP\|MOBILE\|CLI\|SDK\|AGENT, name, version}`, `protocols: [int]`, `encodings: [json\|msgpack]`, `context: authority`, `auth {token \| api_key \| password {username, password} \| oidc_code {provider, code, redirect_uri}}`, `capabilities: [PUSH_UI, DELTAS, NOTIFICATIONS, BLOBS]`, `resume?: {session_id, event_cursor}` | `Welcome` (§2.3) |
| `Mutate` | `changes: [MutateChange]` (SPEC-15 §2), `level?`, `message?` | `Result{process result}` |
| `Emit` | `target: uri`, `signal?: uri\|string`, `stage?: DESCRIBE\|PREPARE\|EXECUTE`, `event?`, `args?`, `base_commit?`, `dialogue_state?`, `dialogue_token?`, `durable?`, `correlation?` | `Result{TransitionResponse \| pipeline output \| instance URI}` or `Result{job, status: ACCEPTED}` |
| `Query` | exactly one of `criteria` (CriteriaQuery), `named {uri, args, page?}`, `get {uri, expand?, fields?, view?, own?}`, `similar {…}` | `Result{QueryResult \| value \| entity}` |
| `Subscribe` | `filters: [SubscriptionFilter]`, `from?: event_cursor` | `Result{subscription_id, event_cursor}` |
| `Unsubscribe` | `subscription_id` | `Result{}` |
| `Cancel` | `target: request id` | `Result{cancelled: bool}` |
| `Ping` | – | `Pong` |
| `Reauth` | `auth {token}` | `Result{principal, expires_at}` |
| `Bye` | – | the server closes the session |

```yaml
ENT-SubscriptionFilter:
  purpose: One subscription filter (VRD-11-04).
  origin: [VRD-11-04, CON-LS-028, CON-UC-059]
  fields:
    - {name: kind, type: "enum{ENTITY|TYPE|QUERY|JOB|INSTANCE|EVENT|NOTIFICATIONS}", required: true, description: "What to watch"}
    - {name: uri, type: uri?, required: false, description: "ENTITY/JOB/INSTANCE target"}
    - {name: types, type: "list<uri>?", required: false, description: "TYPE - entity types (subtypes included)"}
    - {name: where, type: FilterExpr?, required: false, description: "TYPE - filter on indexed paths of the committed version"}
    - {name: criteria, type: CriteriaQuery?, required: false, description: "QUERY - live query (REQ-QRY-041)"}
    - {name: events, type: "list<string>?", required: false, description: "EVENT - event type patterns (e.g. acme.invoice.*)"}
```

### 2.3 Server messages

| Message | Fields | When |
|---|---|---|
| `Welcome` | `re`, `session_id`, `protocol`, `encoding`, `principal {uri, title, kind, tenant}`, `context {authority, tenant, branch, point?, read_only, locale, timezone}`, `server {version, edition, abi: ["v1"]}`, `limits {max_frame_bytes, max_inflight, heartbeat_s, max_subscriptions}`, `resumed?: bool` | answer to Handshake |
| `Ack` | `re`, `process_id?`, `job?` | a write request was accepted and started (always sent for Mutate/Emit before long work or streaming) |
| `Op` | `re`, `kind: PROGRESS\|LOG\|UI\|DELTA\|WARNING`, `body` | streamed during a request: progress (percent, message), script logs (when requested), pushed UI (SPEC-25), render deltas, warnings |
| `Result` | `re`, `data`, `warnings?`, `replayed?` | successful terminal message |
| `Error` | `re`, `error` (SPEC-02 §8 object) | failed terminal message (also for protocol errors with `re` = the offending id or null) |
| `Event` | `sub`, `seq`, `cursor`, `event {type, subject: uri, commit_id?, process_id?, principal?, at, data?}` | subscription delivery (SPEC-20) |
| `Pong` | `re` | heartbeat |
| `Goodbye` | `reason: SHUTDOWN\|AUTH_EXPIRED\|IDLE\|PROTOCOL_ERROR\|SUSPENDED`, `retry_after_ms?` | the server is closing |

### 2.4 Protocol session

```yaml
ENT-ProtocolSession:
  purpose: Server-side state of one connected client after Handshake (TERM-ProtocolSession); cached under sess:{id} (REQ-STO-017).
  origin: [CON-LS-003, VRD-15-01]
  fields:
    - {name: session_id, type: uuid, required: true, description: "Session id"}
    - {name: principal, type: Principal, required: true, description: "Authenticated principal"}
    - {name: client, type: "{kind, name, version}", required: true, description: "Client description"}
    - {name: authority, type: string, required: true, description: "Default context"}
    - {name: encoding, type: "enum{json|msgpack}", required: true, description: "Negotiated encoding"}
    - {name: capabilities, type: "list<string>", required: true, description: "Negotiated capabilities"}
    - {name: subscriptions, type: "map<string, SubscriptionFilter>", required: true, description: "Active subscriptions"}
    - {name: event_cursor, type: string, required: true, description: "Last delivered event position (for resume)"}
    - {name: expires_at, type: timestamp, required: true, description: "Token expiry; Reauth extends"}
```
