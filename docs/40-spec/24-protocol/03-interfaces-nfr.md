---
id: SPEC-24-3
title: "UBTP Protocol — Interfaces and non-functional"
status: complete
phase: P4
depends_on: [SPEC-24, SPEC-02, SPEC-10, SPEC-15, SPEC-16, SPEC-19, SPEC-21, SPEC-22]
sources: [UP, FU, LC, UB, UC, US, LS]
---

# UBTP Protocol — Interfaces and non-functional

Part 3 of SPEC-24 (`24-protocol/`). Header, concepts and file list: `00-index.md`.

---

## 4. Interfaces

### API-PROTO-001 — Message types (Rust)
- **Kind:** rust enums in `ubos_proto::ubtp`
- **Signature / shape:**
```rust
#[derive(Serialize, Deserialize)]
#[serde(tag = "op")]
pub enum ClientMsg {
    Handshake(Envelope<Handshake>), Mutate(Envelope<Mutate>), Emit(Envelope<Emit>),
    Query(Envelope<Query>), Subscribe(Envelope<Subscribe>), Unsubscribe(Envelope<Unsubscribe>),
    Cancel(Envelope<Cancel>), Ping(Envelope<()>), Reauth(Envelope<Reauth>), Bye(Envelope<()>),
}
#[derive(Serialize, Deserialize)]
#[serde(tag = "msg")]
pub enum ServerMsg {
    Welcome(Welcome), Ack(Ack), Op(OpMsg), Result(ResultMsg), Error(ErrorMsg),
    Event(EventMsg), Pong(Pong), Goodbye(Goodbye),
}
pub struct Envelope<T> { pub v: u16, pub id: String, pub authority: Option<String>,
    pub idempotency_key: Option<String>, pub dry_run: Option<bool>, pub timeout_ms: Option<u32>,
    pub traceparent: Option<String>, pub locale: Option<String>, #[serde(flatten)] pub body: T }
```
- **Semantics:** Server messages use the tag field `msg` so they cannot be confused with client `op`s. `#[serde(deny_unknown_fields)]` is **not** used (must-ignore, REQ-PROTO-025).
- **Origin:** CON-LS-002 (`UbosOp`/`UbosResp`)

### API-PROTO-002 — Transport trait
- **Kind:** rust trait (`ubos_server`)
- **Signature / shape:**
```rust
#[async_trait]
pub trait Binding: Send + Sync {
    fn name(&self) -> &'static str;                  // "tcp" | "ws" | "http" | "local"
    async fn serve(self: Arc<Self>, kernel: Kernel, shutdown: ShutdownSignal) -> Result<(), HostError>;
}
pub trait Codec: Send + Sync {                        // json | msgpack
    fn decode(&self, bytes: &[u8]) -> Result<ClientMsg, ProtoError>;
    fn encode(&self, msg: &ServerMsg) -> Result<Bytes, ProtoError>;
}
```
- **Origin:** CON-LS-002 (`UbosCodec`), VRD-15-02

### API-PROTO-003 — SDK surface (TypeScript)
- **Kind:** TS module `@ubos/sdk`
- **Signature / shape:**
```ts
const client = await Ubos.connect({ url: "wss://host/ubtp", context: "acme.prod",
                                    auth: { token }, client: { kind: "WEB", name: "cockpit", version } });
await client.mutate([{ uri: "Customer/c-001", action: "UPDATE", merge_patch: { phone: "…" }, expected_head: "41" }]);
const res = await client.emit({ target: "Invoice/inv-1", signal: "ubos://acme/Action/invoice.approve@v1",
                                stage: "EXECUTE", args: {}, base_commit: "41" }, { onOp: op => … });
const page = await client.query({ criteria: { types: ["Customer"], where: { path: "address.city", op: "EQ", value: "Hangzhou" } } });
const sub = await client.subscribe([{ kind: "TYPE", types: ["Invoice"] }], ev => …);
```
- **Semantics:** Promise per request, resolving on the terminal message. The optional `onOp` callback receives streamed Ops. The SDK reconnects with `resume`. The Rust SDK (`ubos_sdk`) mirrors this API.
- **Origin:** VRD-14-04, CON-US-050

### Example exchange (WebSocket, JSON)

```text
→ {"v":1,"op":"Handshake","id":"h1","client":{"kind":"WEB","name":"cockpit","version":"1.0.0"},
   "protocols":[1],"encodings":["json"],"context":"acme.prod","auth":{"token":"eyJ…"},"capabilities":["PUSH_UI","NOTIFICATIONS"]}
← {"msg":"Welcome","re":"h1","session_id":"0192…","protocol":1,"encoding":"json",
   "principal":{"uri":"ubos://acme/User/ann","title":"Ann","kind":"USER","tenant":"acme"},
   "context":{"authority":"acme.prod","tenant":"acme","branch":"main","read_only":false,"locale":"en","timezone":"Asia/Shanghai"},
   "server":{"version":"1.0.0","edition":"enterprise","abi":["v1"]},
   "limits":{"max_frame_bytes":16777216,"max_inflight":32,"heartbeat_s":30,"max_subscriptions":100}}
→ {"v":1,"op":"Emit","id":"e7","idempotency_key":"k-19","target":"Invoice/inv-1",
   "signal":"ubos://acme/Action/invoice.approve@v1","stage":"EXECUTE","args":{},"base_commit":"41"}
← {"msg":"Ack","re":"e7","process_id":"0192…f3"}
← {"msg":"Op","re":"e7","kind":"UI","body":{"instruction":"toast","level":"success","text":"Invoice approved"}}
← {"msg":"Result","re":"e7","data":{"action":"ubos://acme/Action/invoice.approve@v1?commit=880","target":"ubos://acme/acme.Invoice/inv-1?commit=902",
   "state":"APPROVED","ui_policy":"READ_ONLY","available_events":[{"event":"post","title":"Post","style":"PRIMARY"}],
   "result":{"process_id":"0192…f3","status":"COMPLETED","commits":[{"uri":"ubos://acme/acme.Invoice/inv-1?commit=902","commit_id":"902","action":"UPDATE","branch":"main"}]}}}
```

### Error codes (PROTO)

| Code | Category | Meaning |
|---|---|---|
| `PROTO.HANDSHAKE_REQUIRED` | INVALID | message before Handshake |
| `PROTO.VERSION_UNSUPPORTED` | INVALID | no common protocol version |
| `PROTO.MALFORMED` | INVALID | undecodable frame or missing required field |
| `PROTO.FRAME_TOO_LARGE` | LIMIT | frame above the maximum |
| `PROTO.DUPLICATE_ID` | INVALID | request id already in flight |
| `PROTO.INFLIGHT_LIMIT` | LIMIT | too many concurrent requests |
| `PROTO.TARGET_INVALID` | INVALID | Emit target is not an operation, action target or instance |
| `PROTO.SUBSCRIPTION_LIMIT` | LIMIT | too many subscriptions |
| `PROTO.PUSH_UNSUPPORTED` | (warning) | client lacks the PUSH_UI capability |

---

## 5. Non-functional

| ID | Requirement | Target |
|---|---|---|
| NFR-PERF-130 | Protocol overhead per request (decode, dispatch, encode) | ≤ 100 µs (JSON), ≤ 40 µs (msgpack) for 2 KiB messages |
| NFR-SCAL-130 | Concurrent protocol sessions per gateway node | ≥ 20 000 (WebSocket/TCP) |
| NFR-AVAIL-130 | Session resume after a gateway restart | subscriptions restored from the runtime store; events retained per REQ-PROTO-011 |
| NFR-PORT-130 | Same semantics across bindings | the scenario suite is identical on TCP, WS, HTTP and in-process |
