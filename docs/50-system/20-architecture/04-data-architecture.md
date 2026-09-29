---
id: UBS-ARC-04
title: Data Architecture
status: draft
phase: ALL
depends_on: [UBS-ARC-01, UBS-REQ-05-01, UBS-REQ-05-02, UBS-REQ-05-03]
---

# Data Architecture

This document fixes the **logical** data architecture: what is authoritative, which
indexes and projections exist, where each kind of data lives, how keys protect it, and how
it ages. Physical table definitions (`DAT-*`) are in `UBS-SYS-DVM` (storage design).
Storage is designed per need (DEC-011): there is no fixed table count.

## 1. Categories of data

| Category | Authoritative? | Where | Rebuildable from | Examples |
|---|---|---|---|---|
| A. Content objects | yes | authoritative store (small) + object store (chunks, blobs) | — | object versions, class versions, logic assets, files |
| B. Commit graph and references | yes | authoritative store | — | commits, parents, branch heads, tags, change-set objects |
| C. Ledger entries | yes | authoritative store | — | entries, sequence counters |
| D. Kernel indexes | no (derived, maintained in the flush) | authoritative store | A + B + C | head index, temporal index, relationship index, key-uniqueness index, Merkle tree nodes |
| E. Declared projections | no | authoritative store (separate schema) | A + B + C | balances, open-order lists, reporting tables |
| F. Search and vector indexes | no | search engine or database extensions | A (+ gateway for embeddings) | full-text, vectors |
| G. System objects | yes (System kind) | authoritative store | — | jobs, schedules, subscriptions, sessions, sync cursors |
| H. Journals and audit | yes | authoritative store + object store | — | execution journals, security log, decision logs |
| I. Runtime caches and queues | no | runtime store | A–H | effective-class cache, queues, leases |
| J. Analytical exports | no (copies with lineage) | lakehouse, analytics engine | A–C | Parquet/Iceberg tables |
| K. Keys and secrets | yes (outside the data store) | KMS/HSM, secret store, OS key stores | — | KEKs, tenant data keys, subject keys, signing keys, connector secrets |

### AR-015 — Derived data is marked and rebuildable
- **Rule:** Every store or table in categories D, E, F, I and J MUST be marked derived, MUST have a documented rebuild procedure, and MUST pass a rebuild-equality check in the verification suites.
- **Realises:** DEC-011, FR-ANL-021, FR-ANL-022
- **Rationale:** No hidden second truth.

### AR-016 — Kernel indexes are maintained in the flush
- **Rule:** Category D indexes MUST be updated in the same storage transaction as the commit (so reads at the new head are immediately consistent). Their content MUST be a pure function of categories A–C.
- **Realises:** FR-TXN-012, FR-VER-103, NR-PERF-001
- **Rationale:** Fast, consistent reads without making indexes authoritative.

## 2. Logical model of authoritative state

```
 Commit ──parents──▶ Commit            Branch ref ──head──▶ Commit
   │ changes                            Tag ──────────────▶ Commit
   ▼
 ObjectVersion(hash) ──content──▶ Chunk(hash)*        (Definition, System, RawDocument kinds)
 LedgerEntry(ledger_key, seq) ──content──▶ Chunk(hash)*  (Ledger kind)
 Commit ──root──▶ MerkleNode(hash) ──children──▶ MerkleNode … ──leaf──▶ ObjectVersion
```

In table form:

| Entity | Identity | Key attributes | Immutable |
|---|---|---|---|
| Chunk | content hash | bytes (ciphertext for encrypted domains), size | yes |
| ObjectVersion | content hash | object UUID, class and schema version, kind, valid-time period, envelope, chunk list, encryption domain | yes |
| LedgerEntry | (ledger class, ledger key, sequence) + content hash | entry ID, valid time, amounts, references, source sequence (after merge) | yes |
| Commit | content hash | parents, branch, author, on-behalf-of, process, transaction time, change list, root, rule binding reference, ABI, signatures | yes |
| BranchRef | (VAE, branch name) | head commit, kind, parent line, inheritance mode, state | no (moved by CAS) |
| Tag | (VAE, tag name) | commit, signature, tombstone flag | yes (except tombstoning) |
| MerkleNode | content hash | children hashes (prolly tree over (object UUID → version hash)) | yes |

## 3. Logical table inventory (by purpose)

Each table is listed with its category, owner and write path. Physical design follows in
`UBS-SYS-DVM`.

| Logical table | Category | Purpose | Write path | Retention |
|---|---|---|---|---|
| `chunk` | A | content-addressed chunks stored inline (small) or pointers to the object store (large) | flush | per disposal rules |
| `object_version` | A | object version envelopes | flush | per retention |
| `ledger_entry` | C | ledger entries, partitioned by ledger class and key hash | flush | per retention |
| `ledger_sequence` | C | next sequence per ledger key (gap-free allocation) | flush (row-locked) | permanent |
| `commit` | B | commit records | flush | permanent |
| `commit_parent` | B | DAG edges | flush | permanent |
| `branch_ref` | B | branch heads and metadata | flush (CAS) | permanent (archived state) |
| `tag` | B | immutable tags | flush | permanent |
| `merkle_node` | D | prolly-tree nodes for roots, diffs, proofs and sync | flush | while reachable plus proof retention |
| `head_index` | D | (VAE, branch, object UUID) → current version, for local overrides only | flush | current |
| `temporal_index` | D | (VAE, branch, object UUID, valid_from, valid_to, tx_from, tx_to) → version | flush | per history tiering |
| `relationship_index` | D | reference and link edges for integrity, traversal and authorization relations | flush | current plus temporal |
| `unique_key_index` | D | natural-key uniqueness per branch | flush | current |
| `idempotency_record` | G | idempotency keys and result references | flush | ≥ 7 days |
| `process` | H | process records and status | flush | per audit retention |
| `execution_journal` | H | journal index (large payloads in chunks) | flush | per audit retention |
| `outbox` | G | effects awaiting release | flush; release marks | until released + audit window |
| `job`, `schedule_state`, `subscription_cursor`, `sync_cursor`, `session` | G | kernel bookkeeping objects | kernel services | per type |
| `security_log` | H | hash-chained security events | NOD | per policy (≥ 1 year) |
| `decision_log` | H | authorization decisions (sensitive and sampled) | DVM | per policy |
| `projection_*` | E | declared projections (separate schema per VAE) | flush (`sync`) or projector (`async`) | current or declared |
| `watermark` | E, F, J | watermarks of async consumers | consumers | current |

### AR-017 — Tenant scoping in every authoritative table
- **Rule:** Every authoritative and kernel-index table MUST carry the tenant (and VAE where applicable) in its key, and MUST be protected by row-level security (or physical separation) keyed on the session's tenant.
- **Realises:** FR-TEN-021, NR-SEC-003
- **Rationale:** Isolation enforced below the application.

### AR-018 — Business classes never become DDL implicitly
- **Rule:** Adding or changing a business class MUST NOT create or alter database tables, except through declared projections.
- **Realises:** CST-004, DEC-011
- **Rationale:** Metadata-driven evolution without migrations.

## 4. Read resolution path

Resolving `(VAE, branch, object, asof, time)`:

| Step | Action | Index used |
|---|---|---|
| 1 | Resolve the branch chain (inheritance) at `time` | `branch_ref`, cached |
| 2 | For each branch in the chain, from child to parent: look up the object at `time` and `asof` | `temporal_index` (or `head_index` when `time = head` and `asof = now`) |
| 3 | The first hit wins. A tombstone hit returns not found. | — |
| 4 | Load the version envelope and chunks; decrypt fields the caller may read; apply lenses to the requested schema version | `object_version`, `chunk`, key service |
| 5 | Apply masking and authorization | policy engine |

### AR-019 — Bounded read amplification
- **Rule:** Branch-inheritance resolution MUST be bounded by chain depth (at most 8, FR-VER-084) and MUST use per-branch negative caches so that a head read of an inherited object costs at most one index probe per level in the worst case, and one probe in the common case.
- **Realises:** NR-PERF-001, FR-VER-081
- **Rationale:** Overlays must not slow reads.

## 5. Encryption and key hierarchy

```
 Platform root key (HSM/KMS)
   └─ Tenant KEK (per tenant; customer-managed optional, FR-IAM-093)
        ├─ Tenant data key(s) (rotating; encrypt chunks of non-personal classifications)
        ├─ Subject keys (per data subject; encrypt personal and sensitive_personal fields, FR-PROOF-071)
        └─ Device keys (wrap replicated data on devices, FR-SYNC-043)
 Principal signing keys (Ed25519): devices, OS key stores, or KMS for service and node keys
```

| Key | Protects | Rotation | Destruction effect |
|---|---|---|---|
| Tenant KEK | tenant data keys | yearly or on demand | tenant unreadable (deletion certificate, FR-TEN-053) |
| Tenant data key | chunks of the tenant | quarterly (new writes); rewrap on KEK rotation | — (never destroyed while data is retained) |
| Subject key | personal fields of one subject | on demand | crypto-shredding (FR-PROOF-073) |
| Device key | local replicated data | on device registration | remote wipe (FR-IAM-103) |
| Signing keys | commits, tags, seals | per policy; history kept (FR-IAM-033) | future signing impossible; past signatures remain verifiable |

### AR-020 — Hashes cover ciphertext
- **Rule:** Content hashes and Merkle trees MUST be computed over the stored form (ciphertext plus per-field commitments for encrypted fields), never over plaintext.
- **Realises:** FR-PROOF-072
- **Rationale:** Erasure and key rotation never break proofs.

### AR-021 — Deterministic encryption only for keys
- **Rule:** Deterministic encryption MAY be used only for fields that participate in uniqueness or equality lookups and are declared so. All other fields MUST use randomised authenticated encryption.
- **Realises:** NR-SEC-004, FR-MODEL-013
- **Rationale:** Balance lookup needs with confidentiality.

## 6. Data lifecycle

| Stage | Location | Entry condition | Exit condition |
|---|---|---|---|
| Hot | authoritative store and hot object tier | new versions and entries; reachable from heads | tiering policy (FR-VER-091) |
| Warm | authoritative index plus cold object tier for chunks | older than the policy threshold | disposal eligibility |
| Cold | cold object tier (WORM for record classes) | policy | retention end without hold |
| Disposed | content (or keys) destroyed; hashes kept | retention end, no hold (FR-AUD-072) | — |
| Erased | subject keys destroyed; ciphertext kept | erasure request (FR-PROOF-073) | — |

## 7. Data classification flow

| Channel | Classification check |
|---|---|
| UI payloads and API responses | masking by caller policy (FR-IAM-061) |
| Events to subscribers | masking by subscriber owner (FR-IAM-063) |
| Connectors (egress) | allowed classifications per connector (FR-LOGIC-101) |
| Model gateway | route classification allow-list and redaction (FR-AI-013) |
| Sync to devices | device clearance (FR-SYNC-041) |
| Workers | worker clearance (FR-EVT-062) |
| Exports and CDC | sink clearance (FR-ANL-054) |
| Telemetry | no business values (FR-OPS-042) |
| Embeddings | classification rules (FR-QRY-033) |

### AR-022 — One classification model everywhere
- **Rule:** Every egress channel listed in section 7 MUST evaluate the same field classification and the same policy engine, and MUST NOT implement its own ad hoc filtering.
- **Realises:** FR-MODEL-047, CR-GDPR-001
- **Rationale:** Consistent protection and a single audit story.
