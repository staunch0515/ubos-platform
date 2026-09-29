---
id: UBS-SYS-NOD-03
title: UBOS Node — Interfaces
status: draft
phase: PH-1
depends_on: [UBS-SYS-NOD-02, UBS-ARC-02]
---

# UBOS Node — Interfaces (IF-NOD-*)

| Group | Range | Contract |
|---|---|---|
| UBTP operations | IF-NOD-001…019 | CTR-003 |
| REST facade and webhooks | IF-NOD-020…029 | CTR-004 |
| Identity endpoints | IF-NOD-030…039 | CTR-052 |
| Files and sync | IF-NOD-040…049 | CTR-006, CTR-060 |
| Worker protocol | IF-NOD-050…054 | CTR-070 |
| Admin and fleet | IF-NOD-060…069 | CTR-050, CTR-051 |
| Operator CLI | IF-NOD-070…079 | — |

UBTP message schemas are normative in STD-PROTO (UBS-STD-15); this document binds them to
node behaviour.

## UBTP operations

### IF-NOD-001 — `Handshake`
- **Kind:** UBTP message
- **Caller → Callee:** SDK → NOD
- **Request:** `{ protocol_versions[], abi_versions[], auth: session_token | oauth_token | api_key | device_proof, tenant_hint?, vae, client: { name, version, device_id? } }`
- **Response:** `{ protocol_version, abi_version, session_id, principal, widget_catalogue_version, limits: { frame_max, rate } }`
- **Errors:** `INT.VERSION_UNSUPPORTED`, `IAM.DENIED`, `TEN.SUSPENDED`
- **Idempotency:** A repeated handshake creates a new connection context on the same session.
- **Authorization:** authentication only.
- **Satisfies:** FR-INT-011, FR-INT-013, STD-PROTO-001

### IF-NOD-002 — `Mutate`
- **Kind:** UBTP message
- **Caller → Callee:** SDK → NOD
- **Request:** `{ id, idempotency_key, branch, operation: port | instructions | changeset_op | merge | revert, payload }`
- **Response:** `{ id, outcome: committed | deferred | empty, commit_id?, cseq?, pending_change_id?, result?, warnings[] }`
- **Errors:** any DVM error, `OPS.QUOTA_EXCEEDED`, `BILL.NOT_ENTITLED`
- **Idempotency:** by `idempotency_key` (DSN-NOD-012).
- **Authorization:** DVM per instruction.
- **Satisfies:** FR-INT-011, FR-TXN-031

### IF-NOD-003 — `Query`
- **Kind:** UBTP message
- **Caller → Callee:** SDK → NOD
- **Request:** `{ id, kind: read | query | aggregate | traverse | history | diff | view | explain | reflect, selector, payload, page }`
- **Response:** streamed frames `{ id, rows | object | payload, cursor?, done }`
- **Errors:** `QRY.*`, `IAM.NOT_FOUND`, `INT.CURSOR_EXPIRED`
- **Idempotency:** pure.
- **Authorization:** DVM.
- **Satisfies:** FR-INT-011, FR-QRY-071

### IF-NOD-004 — `Subscribe` and `Unsubscribe`
- **Kind:** UBTP message
- **Caller → Callee:** SDK → NOD
- **Request:** `{ id, target: object | saved_query | inbox | notifications, branch, cursor? }`
- **Response:** acknowledgement, then `Emit` frames
- **Errors:** `IAM.NOT_FOUND`, `INT.CURSOR_EXPIRED` (reset required)
- **Idempotency:** re-subscribing with the same ID replaces the subscription.
- **Authorization:** read on the target, re-checked per update.
- **Satisfies:** FR-INT-014, NR-PERF-015

### IF-NOD-005 — `Emit`
- **Kind:** UBTP message
- **Caller → Callee:** NOD → SDK
- **Request:** `{ subscription_id, cursor, kind: change | reset | task | notification | session_event | banner, payload }`
- **Response:** none (client acknowledges cursor periodically)
- **Errors:** —
- **Idempotency:** cursors make redelivery detectable.
- **Authorization:** per subscriber.
- **Satisfies:** FR-INT-014, FR-IAM-102

### IF-NOD-006 — `Sync`
- **Kind:** UBTP message
- **Caller → Callee:** NOD(Box) ↔ NOD(Server)
- **Request:** `{ phase: heads | havewant | pack | push | integrate | rejections | wipe, payload }`
- **Response:** phase-specific
- **Errors:** `SYNC.INTEGRITY_FAILURE`, `SYNC.UPSTREAM_UNAVAILABLE`, `IAM.DENIED`
- **Idempotency:** content-addressed packs; resumable.
- **Authorization:** device principal plus scope.
- **Satisfies:** FR-SYNC-011, STD-SYNC-001

### IF-NOD-007 — `Upload`
- **Kind:** UBTP message
- **Caller → Callee:** SDK → NOD
- **Request:** `{ upload_id?, size, media_type, field_ref, chunk_index, bytes }`
- **Response:** `{ upload_id, received_bytes, content_id? }`
- **Errors:** `MODEL.OBJECT_TOO_LARGE`, `OPS.QUOTA_EXCEEDED`
- **Idempotency:** chunk index plus hash.
- **Authorization:** write on the target field.
- **Satisfies:** FR-FILE-011, FR-FILE-012

## REST facade and webhooks

### IF-NOD-020 — Resource endpoints
- **Kind:** HTTP endpoint
- **Caller → Callee:** external client → NOD
- **Request:** `GET|POST|PATCH /v1/{vae}/{class}[/{id}]`, `GET …/{id}/history`, `GET …/{id}/timeline`, query parameters `branch`, `asof`, `time`, `filter`, `sort`, `cursor`
- **Response:** JSON representation, `ETag` = version ID
- **Errors:** RFC 9457 problem details with `code`
- **Idempotency:** `Idempotency-Key` header; `If-Match` for updates.
- **Authorization:** OAuth scopes ∩ principal permissions.
- **Satisfies:** FR-INT-021, FR-INT-023

### IF-NOD-021 — Port endpoints
- **Kind:** HTTP endpoint
- **Caller → Callee:** external client → NOD
- **Request:** `POST /v1/{vae}/{class}/{id}/ports/{port}` with JSON arguments
- **Response:** `{ result, commit_id, cseq }` or `202` with pending change
- **Errors:** problem details
- **Idempotency:** `Idempotency-Key`.
- **Authorization:** as IF-NOD-020.
- **Satisfies:** FR-INT-021, FR-FLOW-022

### IF-NOD-022 — OpenAPI document
- **Kind:** HTTP endpoint
- **Caller → Callee:** external client → NOD
- **Request:** `GET /v1/{vae}/openapi.json`
- **Response:** OpenAPI 3.1 document
- **Errors:** —
- **Idempotency:** pure.
- **Authorization:** `model.read`.
- **Satisfies:** FR-INT-022

### IF-NOD-023 — Outbound webhook delivery
- **Kind:** HTTP endpoint
- **Caller → Callee:** NOD → subscriber
- **Request:** `POST` CloudEvents JSON with headers `webhook-id`, `webhook-timestamp`, `webhook-signature`
- **Response:** 2xx to acknowledge
- **Errors:** non-2xx → retry
- **Idempotency:** `webhook-id` = event ID.
- **Authorization:** HMAC signature.
- **Satisfies:** FR-INT-031, FR-EVT-013

### IF-NOD-024 — Inbound webhook
- **Kind:** HTTP endpoint
- **Caller → Callee:** external system → NOD
- **Request:** `POST /hooks/{tenant}/{connector}` with the sender's signature
- **Response:** `202`
- **Errors:** `401` on signature failure, `409` on replay
- **Idempotency:** sender event ID.
- **Authorization:** connector signature scheme.
- **Satisfies:** FR-INT-032

## Identity endpoints

### IF-NOD-030 — OIDC and SAML
- **Kind:** HTTP endpoint
- **Caller → Callee:** browser ↔ NOD ↔ IdP
- **Request:** `/auth/{tenant}/oidc/login`, `/callback`; `/auth/{tenant}/saml/login`, `/acs`, `/metadata`
- **Response:** session cookie or token
- **Errors:** `IAM.DENIED`
- **Idempotency:** state and nonce single-use.
- **Authorization:** IdP assertion.
- **Satisfies:** FR-IAM-021, FR-IAM-022

### IF-NOD-031 — SCIM 2.0
- **Kind:** HTTP endpoint
- **Caller → Callee:** IdP → NOD
- **Request:** `/scim/v2/{tenant}/Users`, `/Groups` (RFC 7644)
- **Response:** SCIM resources
- **Errors:** SCIM error schema
- **Idempotency:** `externalId` matching.
- **Authorization:** SCIM bearer token per tenant.
- **Satisfies:** FR-IAM-071

### IF-NOD-032 — OAuth authorization server
- **Kind:** HTTP endpoint
- **Caller → Callee:** client → NOD
- **Request:** `/oauth/{tenant}/authorize`, `/token`, `/revoke`, `/.well-known/oauth-authorization-server`
- **Response:** tokens per OAuth 2.1
- **Errors:** OAuth error codes
- **Idempotency:** codes single-use.
- **Authorization:** client credentials or user consent.
- **Satisfies:** FR-INT-081, FR-IAM-104

## Files and sync

### IF-NOD-040 — Resumable upload (HTTP)
- **Kind:** HTTP endpoint
- **Caller → Callee:** client → NOD
- **Request:** tus 1.0 protocol at `/files/{vae}/uploads`
- **Response:** upload URL, offsets, final content ID
- **Errors:** `413`, quota errors
- **Idempotency:** offsets.
- **Authorization:** write on the target field.
- **Satisfies:** FR-FILE-011, FR-FILE-012

### IF-NOD-041 — Signed download
- **Kind:** HTTP endpoint
- **Caller → Callee:** client → NOD
- **Request:** `GET /files/{token}` with range support
- **Response:** decrypted bytes
- **Errors:** `403` expired or invalid
- **Idempotency:** pure.
- **Authorization:** signed token bound to principal and version.
- **Satisfies:** FR-FILE-011

## Worker protocol

### IF-NOD-050 — `worker.lease`
- **Kind:** UBTP message
- **Caller → Callee:** Worker → NOD
- **Request:** `{ capabilities, max_jobs }`
- **Response:** `{ jobs: [{ job_id, lease_token, asset_hash, inputs_ref, budget }] }`
- **Errors:** `IAM.DENIED` (quarantined)
- **Idempotency:** leases expire.
- **Authorization:** worker principal.
- **Satisfies:** FR-EVT-061, FR-EVT-062

### IF-NOD-051 — `worker.result`
- **Kind:** UBTP message
- **Caller → Callee:** Worker → NOD
- **Request:** `{ job_id, lease_token, outputs, input_hash, output_hash, journal_ref, metering }`
- **Response:** `{ accepted, commit_id? }`
- **Errors:** `TXN.HEAD_MOVED` (fencing), `LOGIC.UNVERIFIED_ASSET`
- **Idempotency:** by `(job_id, lease_token)`.
- **Authorization:** worker principal and lease.
- **Satisfies:** FR-EVT-063, CTR-070

## Admin and fleet

### IF-NOD-060 — `node.register` and `node.health`
- **Kind:** HTTP endpoint
- **Caller → Callee:** CTL → NOD admin endpoint
- **Request:** registration token / —
- **Response:** node identity and version / health report
- **Errors:** `401`
- **Idempotency:** registration keyed by node ID.
- **Authorization:** mTLS CTL identity.
- **Satisfies:** CTR-050, FR-OPS-081

### IF-NOD-061 — `config.apply`
- **Kind:** HTTP endpoint
- **Caller → Callee:** CTL → NOD
- **Request:** `{ config_version, document, signature }`
- **Response:** `{ applied, restart_required_keys[] }`
- **Errors:** validation errors
- **Idempotency:** by config version.
- **Authorization:** mTLS CTL identity plus signed document.
- **Satisfies:** FR-OPS-091, FR-OPS-093

### IF-NOD-062 — `upgrade.step`
- **Kind:** HTTP endpoint
- **Caller → Callee:** CTL → NOD
- **Request:** `{ target_version, step: drain | stop | start | verify | rollback }`
- **Response:** step status
- **Errors:** step failure details
- **Idempotency:** steps are idempotent.
- **Authorization:** mTLS CTL identity.
- **Satisfies:** FR-OPS-082, FR-OPS-031

### IF-NOD-063 — `tenant.*` lifecycle and migration
- **Kind:** HTTP endpoint
- **Caller → Callee:** CTL → NOD
- **Request:** `tenant.create`, `tenant.suspend`, `tenant.resume`, `tenant.archive`, `tenant.delete`, `tenant.migrate.{export|import|catchup|pause|cutover|retire}`
- **Response:** operation status
- **Errors:** `TXN.ALREADY_EXISTS`, `AUD.LEGAL_HOLD_ACTIVE`
- **Idempotency:** operation IDs.
- **Authorization:** mTLS CTL identity; deletion requires a signed approval from two CTL operators.
- **Satisfies:** FR-TEN-051, FR-TEN-052, FR-TEN-053, FR-TEN-072

### IF-NOD-064 — `usage.push` and heartbeat
- **Kind:** HTTP endpoint
- **Caller → Callee:** NOD → CTL
- **Request:** hash-chained usage records; heartbeat with version and health
- **Response:** acknowledgement with last accepted record
- **Errors:** chain mismatch
- **Idempotency:** record IDs.
- **Authorization:** mTLS node identity.
- **Satisfies:** FR-BILL-011, FR-BILL-012, CTR-051

## Operator CLI (`ubos-node`)

### IF-NOD-070 — Operator commands
- **Kind:** CLI command
- **Caller → Callee:** operator → NOD
- **Request:** `ubos-node init | start | check-config | migrate | backup | restore --pitr <time> | verify <what> | rebuild <what> | diag bundle | jobs <list|retry|cancel> | tenant <create|export|import> | keys rotate`
- **Response:** human-readable and `--json` output
- **Errors:** non-zero exit with error code
- **Idempotency:** per command, documented.
- **Authorization:** local operator access; tenant data commands require a tenant administrator token.
- **Satisfies:** NR-OPER-001, NR-OPER-004, FR-OPS-053
