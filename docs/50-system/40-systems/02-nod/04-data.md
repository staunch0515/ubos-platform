---
id: UBS-SYS-NOD-04
title: UBOS Node — Data and Stores
status: draft
phase: PH-1
depends_on: [UBS-SYS-NOD-02, UBS-SYS-DVM-04]
---

# UBOS Node — Data and Stores

The node owns operational data only. Business data lives in the DVM tables
(UBS-SYS-DVM-04). Node tables live in the `platform` schema (registry, sessions, leases) or
in the tenant partitions (jobs, schedules, deliveries), always with `tenant_id` and RLS where
tenant-scoped.

## 1. Stores

| Store | Content | Authority | Loss impact |
|---|---|---|---|
| PostgreSQL `platform` schema | tenant registry, nodes, leases, sessions, credentials (hashed), config versions, security log | authoritative for operational state | restore from backup |
| PostgreSQL tenant partitions | jobs, schedule slots, deliveries, event log, notification records, sync device state, usage records | authoritative for operational state | restore from backup |
| object store | chunks (via DVM), files, previews, backups, WORM records, diagnostic bundles | authoritative for content | restore from versioned bucket |
| runtime store | caches, rate-limit buckets, queue hints, pub/sub | none (hints) | performance only (FR-EVT-032) |
| search indexes | Tantivy directories, OpenSearch indices, pgvector tables | derived | rebuild (DSN-NOD-344) |
| key service | KEKs, subject keys, signing keys | authoritative for keys | restore key backups with tombstones |
| local disk (Box) | SQLite database, blob directory, indexes | authoritative (device copy) | re-sync from Server if synced |

## 2. Operational tables

```sql
-- platform schema
CREATE TABLE tenant (tenant_id uuid PRIMARY KEY, state smallint, plan text, cell text,
                     residency text, kek_ref text, created_at timestamptz, grace_until timestamptz);
CREATE TABLE vae (tenant_id uuid, vae_id uuid, name text, parent_vae uuid, state smallint,
                  PRIMARY KEY (tenant_id, vae_id));
CREATE TABLE node (node_id text PRIMARY KEY, edition text, version text, zone text,
                   last_heartbeat timestamptz, state smallint);
CREATE TABLE lease (duty text, scope text, holder text, fencing bigint, until timestamptz,
                    PRIMARY KEY (duty, scope));
CREATE TABLE session (tenant_id uuid, session_hash bytea PRIMARY KEY, principal text,
                      device_id text, auth_level smallint, created_at timestamptz,
                      last_seen timestamptz, idle_until timestamptz, absolute_until timestamptz,
                      revoked_at timestamptz);
CREATE TABLE credential (tenant_id uuid, credential_id uuid PRIMARY KEY, kind smallint,
                         principal text, secret_hash bytea, secret_hash_next bytea,
                         scopes text[], ip_ranges cidr[], expires_at timestamptz, state smallint);
CREATE TABLE config_version (version bigint PRIMARY KEY, hash bytea, author text,
                             applied_at timestamptz, document bytea);
CREATE TABLE security_log (seq bigint PRIMARY KEY, at timestamptz, tenant_id uuid,
                           kind text, actor text, detail jsonb, prev_hash bytea, hash bytea);
CREATE TABLE usage_record (node_id text, seq bigint, tenant_id uuid, hour timestamptz,
                           metrics jsonb, prev_hash bytea, hash bytea, pushed_at timestamptz,
                           PRIMARY KEY (node_id, seq));

-- tenant partitions
CREATE TABLE job (tenant_id uuid, job_id uuid, queue text, kind text, state smallint,
                  priority smallint, attempts int, run_after timestamptz,
                  lease_holder text, lease_token bigint, lease_until timestamptz,
                  payload bytea, last_error bytea, poison boolean,
                  PRIMARY KEY (tenant_id, job_id));
CREATE INDEX job_ready ON job (queue, run_after) WHERE state = 0;
CREATE TABLE schedule_state (tenant_id uuid, schedule_id uuid, slot timestamptz, job_id uuid,
                             outcome smallint, PRIMARY KEY (tenant_id, schedule_id, slot));
CREATE TABLE delivery (tenant_id uuid, delivery_id uuid, channel smallint, target text,
                       event_id uuid, attempt int, status smallint, provider_ref text,
                       at timestamptz, PRIMARY KEY (tenant_id, delivery_id, attempt));
CREATE TABLE event_log (tenant_id uuid, event_id uuid, type text, object_id uuid,
                        branch_id bigint, cseq bigint, at timestamptz, payload bytea,
                        PRIMARY KEY (tenant_id, event_id));
CREATE TABLE device (tenant_id uuid, device_id text, principal text, device_key bytea,
                     scope_id uuid, last_sync timestamptz, lag_cseq bigint, wipe_pending boolean,
                     PRIMARY KEY (tenant_id, device_id));
CREATE TABLE upload (tenant_id uuid, upload_id uuid, principal text, size bigint,
                     received bigint, chunk_ids bytea[], state smallint, expires_at timestamptz,
                     PRIMARY KEY (tenant_id, upload_id));
```

Jobs, schedules and timers are also represented as System-kind objects in the DVM where
business users need to see them (FR-EVT-031); the `job` table is the execution queue, and
the object is updated by processes at state changes.

## 3. Data structures

### DAT-UsageRecord
- **Purpose:** Hourly metered usage of one tenant on one node.
- **Kind:** Message
- **Stored in:** `usage_record`
- **Schema:**
```yaml
DAT-UsageRecord:
  node_id:   { type: string, required: true, description: reporting node }
  seq:       { type: integer, required: true, description: per-node sequence }
  tenant_id: { type: uuid, required: true, description: tenant }
  hour:      { type: instant, required: true, description: start of the hour }
  metrics:   { type: map<string, decimal>, required: true, description: units by meter (commits, instruction units, storage GB-hours, active users, AI cost) }
  prev_hash: { type: content_id, required: true, description: hash of the previous record }
  hash:      { type: content_id, required: true, description: hash of this record }
```
- **Invariants:**
  1. `seq` is gap-free per node, and the hash chain verifies.
- **Satisfies:** FR-BILL-011, FR-BILL-012

### DAT-JobRecord
- **Purpose:** One unit of asynchronous work.
- **Kind:** System
- **Stored in:** `job`
- **Schema:**
```yaml
DAT-JobRecord:
  job_id:       { type: uuid, required: true, description: identity }
  queue:        { type: string, required: true, description: queue name }
  kind:         { type: string, required: true, description: handler, schedule, bulk, saga, retention, index, custom }
  state:        { type: enum, required: true, description: ready | leased | succeeded | failed | dead | cancelled | paused }
  attempts:     { type: integer, required: true, description: attempts so far }
  run_after:    { type: instant, required: true, description: earliest start }
  lease_token:  { type: integer, required: false, description: fencing token of the current lease }
  payload:      { type: bytes, required: true, description: canonical job arguments }
```
- **Invariants:**
  1. A result commit is accepted only with the current lease token.
- **Satisfies:** FR-EVT-031, FR-EVT-033

### DAT-SessionRecord
- **Purpose:** One authenticated session.
- **Kind:** System
- **Stored in:** `session`
- **Schema:**
```yaml
DAT-SessionRecord:
  session_hash: { type: bytes, required: true, description: SHA-256 of the opaque token }
  principal:    { type: did, required: true, description: authenticated principal }
  device_id:    { type: string, required: false, description: bound device }
  auth_level:   { type: integer, required: true, description: authentication assurance level }
  idle_until:   { type: instant, required: true, description: idle expiry }
  absolute_until: { type: instant, required: true, description: absolute expiry }
  revoked_at:   { type: instant, required: false, description: revocation time }
```
- **Invariants:**
  1. Tokens are never stored in clear.
- **Satisfies:** FR-IAM-101, FR-IAM-102
