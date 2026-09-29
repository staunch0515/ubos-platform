---
id: SPEC-24
title: "UBTP Protocol"
status: complete
phase: P4
depends_on: [SPEC-02, SPEC-10, SPEC-15, SPEC-16, SPEC-19, SPEC-21, SPEC-22]
sources: [UP, FU, LC, UB, UC, US, LS]
---

# UBTP Protocol

## 0. Chapter header

- **Scope:** This chapter defines the UBOS Transfer Protocol (UBTP):
  - the message model: the envelope, the client operations Handshake, Mutate, Emit, Query, Subscribe and Unsubscribe, the control messages, and the server messages;
  - the request/response sequencing and protocol sessions;
  - the transport bindings (TCP, WebSocket, HTTP, in-process) and encodings;
  - protocol versioning, limits and errors;
  - the external integration surface (HTTP convenience routes, OpenAPI);
  - the scenario file format.

  Every client (ADR-003) uses only this protocol (VRD-14-04).
- **MOD:** `PROTO`
- **depends_on:** SPEC-02, SPEC-10, SPEC-15, SPEC-16, SPEC-19, SPEC-21, SPEC-22.
- **Terms used:** TERM-Ubtp, TERM-UbtpOperation, TERM-Envelope, TERM-ProtocolSession, TERM-TransportBinding, TERM-NamedOperation, TERM-Subscription, TERM-Client, TERM-Scenario.
- **Origin summary:**
  - Verdicts: VRD-15-01…05, VRD-14-04, VRD-11-04, VRD-20-05, VRD-22-02.
  - LS: frames, the five operations, the per-connection session, the Ack + streamed Op + Error responses, protocol test clients (CON-LS-001…005, 029, 050).
  - US: the UBTP whitepaper (CON-US-006).
  - LC: the uniform envelope, idempotency and named processes (CON-LC-001, 003, 050).
  - UB: opcodes behind the protocol (CON-UB-003).
  - UC and UP: the minimal verb APIs and the boot handshake (CON-UC-050, CON-UC-059, CON-UP-050, CON-UP-053).
  - FU: the three API families and API keys (CON-FU-002, CON-FU-003).

## Parts of this chapter

This chapter is split into files (WRITING-RULES R2.2). Read them in order.

| File | Sections |
|---|---|
| `00-index.md` (this file) | §0, §1, §6, §7, §8 |
| `01-model.md` | Model — §2 |
| `02-behavior.md` | Behavior — §3 |
| `03-interfaces-nfr.md` | Interfaces and non-functional — §4, §5 |

## 1. Concepts

- **One message model, several transports.** UBTP is defined as JSON-compatible messages. Transports carry them unchanged, except for framing (VRD-15-02).
- **Five operations cover the platform:**

| Operation | Meaning | Kernel path |
|---|---|---|
| `Handshake` | authenticate, select the context, negotiate encoding and capabilities | SPEC-22, SPEC-16 |
| `Mutate` | direct entity changes (change sets) | SPEC-15 REQ-TX-030 |
| `Emit` | invoke a named operation or signal: action stage, pipeline, workflow start, instance signal | SPEC-19 |
| `Query` | read: criteria query, named query, entity read | SPEC-21 |
| `Subscribe` / `Unsubscribe` | receive events: entity changes, live queries, jobs, notifications | SPEC-20 |

- **Streaming responses.** A request yields an optional `Ack`, zero or more `Op` messages (progress, logs, pushed UI, deltas), and exactly one terminal `Result` or `Error` (LS: "each op yields Ack and then every side-effect op as a separate Op message").
- **The server may address the client.** UI emits to `client://current` arrive as `Op{kind: UI}` in the same stream. Notifications and subscriptions arrive as `Event` messages (LS "client as addressable target").

## 6. Acceptance

| REQ | Criterion |
|---|---|
| REQ-PROTO-001…005 | Handshake enforcement and resume; sequencing; async emits; cancel; heartbeat and reauth |
| REQ-PROTO-010…012 | Operation mapping table; subscriptions with authorisation at delivery and resume; pushed UI and notifications |
| REQ-PROTO-020…025 | TCP framing and TLS; WS subprotocols; HTTP routes and status mapping; in-process binding; encodings; must-ignore |
| REQ-PROTO-030, 031 | OpenAPI per principal; machine client scopes |
| REQ-PROTO-040 | Scenario runner across bindings |

## 7. Implementation notes

- **Reference code:**
  - LS codec [LS:apps/ubos_server/src/codec.rs#L20-L63], operations [LS:crates/ubos_proto/src/op.rs#L8-L57], server loop [LS:apps/ubos_server/src/server.rs#L43-L121], test clients [LS:apps/ubos_server/src/bin/].
  - US whitepaper [US:docs/front/UBOS.md] (CON-US-006).
  - LC controller [LC:src/main/java/com/logicorum/api/ProcessController.java].
- **Unifications:**
  - LS's two variants of `Emit`/`Mutate` become one shape. `Emit.target` is the entity or operation, and `signal` is the action URI, the signal name or `start`.
  - LS answered `Query` with an `Emit{QueryResult}` to the client. Here `Query` has a terminal `Result`, and pushed UI stays in `Op{UI}`.
- **Implementation:** axum (HTTP/WS) and `tokio_util::codec::LengthDelimitedCodec` (TCP). One task per connection, with a bounded mpsc channel per session for outgoing messages, and backpressure. Slow consumers are closed with `Goodbye{PROTOCOL_ERROR}` after their buffer (1 000 messages) overflows.

## 8. Open questions

None. Deferred: zero-copy binary framing beyond msgpack (e.g. FlatBuffers) by ADR if measurements require it (VRD-20-05); HTTP/3.
