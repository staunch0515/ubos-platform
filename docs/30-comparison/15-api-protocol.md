---
id: CMP-15
title: "Comparison: API and Protocol"
status: complete
phase: P2
depends_on: [CMP-03, CMP-04, CMP-06, CMP-11]
sources: [UP, FU, LC, UB, UC, US, LS]
---

# Comparison: API and Protocol

## 1. Question

What is the external interface of the kernel? This covers the verbs, envelope, transport, session, streaming and versioning that all clients and integrations use.

## 2. Candidates

| KEY | Approach | Refs | Summary |
|---|---|---|---|
| UP | Small generic REST: boot / run / commit / view / audit | CON-UP-050 | No per-business endpoints |
| FU | Three families: generic API, console REST for Studio, text terminal; API-key boundary | CON-FU-002, 003 | Operator-oriented |
| LC | `/{api}/{version}/{domain}/{operation}` → named process; uniform envelope; GET = query engine | CON-LC-001, 050, HL-LC-001 | API = process catalogue |
| UB | Context-addressed HTTP API; kernel operations as opcode enums | CON-UB-003, 005, HL-UB-005 | Closed instruction set |
| UC | deploy / invoke (202) / eval / search / get / logic / ambassador connect | CON-UC-050, 023 | Minimal verbs + session handshake |
| US | UC + action init/prepare/submit/transition, check_syntax | CON-US-050, 022, 005 | Interaction endpoints |
| LS | UBTP over TCP: 4-byte length-prefixed JSON; Handshake / Mutate / Emit / Query / Subscribe; Ack + streamed ops; `client://current` | CON-LS-001…005, 029, HL-LS-004 | Implemented five-instruction protocol |

## 3. Dimension matrix

| Dimension | UP | LC | UB | UC/US | LS |
|---|---|---|---|---|---|
| Generic verbs (no per-type endpoints) | ● | ● (process names) | ● | ● | ● |
| Closed instruction set | ◐ | – | ● opcodes | ◐ | ● 5 ops |
| Session / handshake | boot | – | context path | ambassador | ● |
| Server push / streaming | – | – | – | polling | ● |
| Uniform envelope | ◐ | ● | ◐ | ◐ | ● Ack/Op/Error |
| Versioning of operations | – | ● names | – | – | – |
| Browser-friendly | ● | ● | ● | ● | ✕ (TCP) |
| Idempotency / request id | – | ● | – | – | request_id field |

## 4. Analysis

- **All repositories reject per-entity REST.** The API is a small set of generic verbs. Three styles appear:
  - LC's style: a catalogue of named, versioned processes.
  - LS's style: five transport-level operations.
  - UB's style: a kernel opcode enum used internally.
- **The styles map onto each other.**
  - `Mutate` corresponds to the commit/change-set write.
  - `Emit` covers invoking an action, pipeline or FSM transition, and signalling a process instance.
  - `Query` corresponds to criteria queries and named queries.
  - `Subscribe` corresponds to events and job status.
  - `Handshake` corresponds to context selection and authentication.
- **Transport.**
  - LS's TCP framing suits native, desktop and edge clients but not browsers.
  - The same messages can be carried over WebSocket, and over HTTP for simple request/response.
  - *Interpretation:* define the protocol as messages and bind them to several transports.
- **Envelope.** LC's uniform response envelope and idempotency, together with LS's `Ack` + streamed `Op` + `Error`, together cover the envelope.

## 5. Verdicts

### VRD-15-01 — UBTP: five operations as the canonical protocol (best-of LS, enriched)
- **Decision:** best-of LS, enriched.
- **Operations:**
  - `Handshake{client, context, auth, capabilities}`
  - `Mutate{changes[], expected_heads?, idempotency_key?}`
  - `Emit{target (URI), signal (action/event/transition), args, idempotency_key?}`
  - `Query{criteria | named_query, as_of?, page}`
  - `Subscribe{filters, jobs?}` with `Unsubscribe`
- **Envelope:** requests carry `request_id`. Responses are `Ack{request_id, process_id}`, `Result{request_id, data}`, streamed `Op{…}` and `Error{request_id, code, message, details[path]}`.

### VRD-15-02 — One message model, three transport bindings (NEW)
- **Decision:** NEW.
- **Consequences:**
  - **TCP** with 4-byte big-endian length framing (LS) for native, desktop and edge.
  - **WebSocket**, with one JSON message per frame, for browsers.
  - **HTTP** binding for request/response and integrations: `POST /ubtp/{op}`, plus convenience routes, e.g. `/invoke`, `/query`, `/entity/{uri}`.
  - Encoding: JSON first; a binary encoding (e.g. MessagePack or CBOR) is negotiated in the Handshake for the "own binary protocol, zero-copy" goal.

### VRD-15-03 — Named operations are addressed by URI and versioned (fusion LC + UB)
- **Decision:** fusion.
- **Consequences:**
  - Actions, pipelines and queries have URIs with a version, e.g. `ubos://system/Action/entity.edit@v1`.
  - Emit targets them. The server-side instruction set (UB opcodes) is the implementation behind the protocol.

### VRD-15-04 — Synchronous vs asynchronous emits (fusion UC + US)
- **Decision:** fusion.
- **Consequences:** Short emits (FSM transitions, sync logic) return `Result`. Long emits return `Ack` with a job URI, and progress is streamed to subscribers (VRD-11-03, VRD-11-04).

### VRD-15-05 — External integration surface (best-of FU)
- **Decision:** best-of FU.
- **Consequences:**
  - Machine clients use the HTTP binding with scoped API keys.
  - Operator consoles are restricted, e.g. localhost or admin policy.
  - Outbound integration uses webhooks (VRD-11-02).
