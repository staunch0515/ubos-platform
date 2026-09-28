---
id: SPEC-11-3
title: "Storage Model and Store Traits — Interfaces"
status: complete
phase: P4
depends_on: [SPEC-11, SPEC-02, SPEC-10, ADR-001, ADR-004]
sources: [UP, FU, LC, UB, UC, US, LS]
---

# Storage Model and Store Traits — Interfaces

Part 3 of SPEC-11 (`11-storage/`). Header, concepts and file list: `00-index.md`.

---

## 4. Interfaces

Kernel-internal Rust traits in `ubos_store`. Write methods take a transaction handle and are
visible only to the `tx` module (REQ-ARCH-003). All methods are `async`. `StoreResult<T>` is
`Result<T, StoreError>`.

### API-STO-001 — Store and transactions
- **Kind:** rust trait
- **Signature / shape:**
```rust
#[async_trait]
pub trait Store: Send + Sync {
    async fn begin(&self, tenant: &TenantCode) -> StoreResult<Box<dyn StoreTx>>; // sets ubos.tenant (RLS)
    fn reader(&self) -> &dyn ReaderOps;
    fn edition(&self) -> Edition;              // Personal | Enterprise
    async fn schema_version(&self) -> StoreResult<u32>;
}
#[async_trait]
pub trait StoreTx: Send {
    fn instances(&mut self) -> &mut dyn InstanceOps;
    fn versions(&mut self) -> &mut dyn VersionOps;
    fn heads(&mut self) -> &mut dyn HeadOps;
    fn index(&mut self) -> &mut dyn IndexOps;
    fn process_log(&mut self) -> &mut dyn ProcessLogOps;
    fn lineage(&mut self) -> &mut dyn LineageOps;
    fn outbox(&mut self) -> &mut dyn OutboxOps;
    async fn commit(self: Box<Self>) -> StoreResult<()>;
    async fn rollback(self: Box<Self>) -> StoreResult<()>;
}
```
- **Semantics:** A transaction is bound to one tenant for RLS. Cross-tenant writes are impossible except for patched root entities (REQ-STO-006). Dropping a `StoreTx` without committing rolls it back.
- **Origin:** CON-LS-006, VRD-01-05

### API-STO-002 — InstanceOps
- **Kind:** rust trait
- **Signature / shape:**
```rust
#[async_trait]
pub trait InstanceOps: Send {
    /// Insert or reuse by locator; returns the effective entity id (ID-split guard).
    async fn upsert(&mut self, row: &NewInstance) -> StoreResult<Uuid>;
    async fn lock_for_update(&mut self, id: Uuid) -> StoreResult<InstanceRow>;
    async fn set_status(&mut self, id: Uuid, status: EntityStatus,
                        terminator: Option<CommitId>) -> StoreResult<()>;
    async fn rename(&mut self, id: Uuid, new_slug: &Slug) -> StoreResult<()>; // LOCATOR_TAKEN on clash
    async fn touch(&mut self, id: Uuid, at: Timestamp,
                   display_name: Option<&str>, tags: Option<&[String]>) -> StoreResult<()>;
    async fn set_owner(&mut self, id: Uuid, owner: Option<Uuid>) -> StoreResult<()>;
}
```
- **Semantics:** `upsert` raises `URI.LOCATOR_TAKEN` if the given `id` differs from an existing non-DELETED row with the same locator and the session did not intend reuse.
- **Origin:** CON-UC-041, CON-UP-044

### API-STO-003 — VersionOps
- **Kind:** rust trait
- **Signature / shape:**
```rust
#[async_trait]
pub trait VersionOps: Send {
    async fn insert(&mut self, v: &NewVersion) -> StoreResult<CommitId>;
    /// Only allowed transition: embedding NULL -> value.
    async fn set_embedding(&mut self, commit: CommitId, emb: &[f32]) -> StoreResult<()>;
}
pub struct NewVersion {
    pub tenant_id: TenantCode, pub entity_id: Uuid, pub branch: BranchName,
    pub parent: Option<CommitId>, pub merge_parent: Option<CommitId>,
    pub action: CommitAction, pub snapshot: CanonicalJson, pub snapshot_hash: [u8; 32],
    pub type_commit_id: Option<CommitId>, pub details: Option<serde_json::Value>,
    pub author_id: Uuid, pub message: Option<String>, pub committed_at: Timestamp,
}
```
- **Origin:** CON-UB-040, REQ-STO-003

### API-STO-004 — HeadOps
- **Kind:** rust trait
- **Signature / shape:**
```rust
#[async_trait]
pub trait HeadOps: Send {
    /// First commit on the branch; HEAD_CONFLICT if a head exists.
    async fn insert(&mut self, h: &NewHead) -> StoreResult<()>;
    /// CAS move; HEAD_CONFLICT if expected does not match (REQ-STO-012).
    async fn advance(&mut self, key: &HeadKey, expected: Expected, new_head: CommitId,
                     deleted: bool, at: Timestamp) -> StoreResult<i64 /* new seq */>;
    /// Remove a head (reset to inherit, draft cleanup); CAS on expected.
    async fn remove(&mut self, key: &HeadKey, expected: Expected) -> StoreResult<()>;
    /// Administrative reset (SPEC-14); audited by the caller's ADMIN process.
    async fn force_set(&mut self, key: &HeadKey, head: CommitId, deleted: bool) -> StoreResult<()>;
}
pub struct HeadKey { pub tenant_id: TenantCode, pub entity_id: Uuid, pub branch: BranchName }
pub enum Expected { Commit(CommitId), Seq(i64), Absent }
```
- **Origin:** CON-UB-041, VRD-08-02, VRD-08-05

### API-STO-005 — IndexOps, ProcessLogOps, LineageOps, OutboxOps
- **Kind:** rust trait
- **Signature / shape:**
```rust
#[async_trait] pub trait IndexOps: Send {
    async fn insert_rows(&mut self, rows: &[IndexRow]) -> StoreResult<()>;
    async fn replace_for_commits(&mut self, commits: &[CommitId], rows: &[IndexRow]) -> StoreResult<()>;
}
#[async_trait] pub trait ProcessLogOps: Send {
    async fn insert(&mut self, row: &ProcessLogRow) -> StoreResult<()>;
    async fn set_billing(&mut self, process: Uuid, billing: CommitId) -> StoreResult<()>;
}
#[async_trait] pub trait LineageOps: Send {
    async fn insert(&mut self, rows: &[LineageRow]) -> StoreResult<()>; // de-duplicated
}
#[async_trait] pub trait OutboxOps: Send {
    async fn push(&mut self, rows: &[NewOutboxRow]) -> StoreResult<()>;
}
```
- **Origin:** REQ-STO-007…011

### API-STO-006 — ReaderOps
- **Kind:** rust trait
- **Signature / shape:**
```rust
#[async_trait]
pub trait ReaderOps: Send + Sync {
    async fn instance(&self, id: Uuid) -> StoreResult<Option<InstanceRow>>;
    async fn instance_by_locator(&self, loc: &Locator) -> StoreResult<Option<InstanceRow>>; // non-DELETED
    async fn head(&self, key: &HeadKey) -> StoreResult<Option<HeadRow>>;
    async fn heads_for(&self, tenants: &[TenantCode], entity: Uuid, branches: &[BranchName])
        -> StoreResult<Vec<HeadRow>>;                     // one round trip for fallback chains
    async fn version(&self, commit: CommitId) -> StoreResult<Option<VersionRow>>;
    async fn versions(&self, commits: &[CommitId]) -> StoreResult<Vec<VersionRow>>;
    async fn history(&self, entity: Uuid, branch: &BranchName, range: CommitRange, page: Page)
        -> StoreResult<Vec<VersionMeta>>;                 // without snapshots
    async fn version_at(&self, entity: Uuid, branch: &BranchName, time: Timestamp)
        -> StoreResult<Option<CommitId>>;                 // latest commit <= time (TERM-AsOf)
    async fn parent_chain(&self, commit: CommitId, limit: u32) -> StoreResult<Vec<VersionMeta>>;
    async fn list_heads(&self, filter: &HeadFilter, page: Page) -> StoreResult<Vec<HeadListItem>>;
    async fn search(&self, plan: &IndexQueryPlan) -> StoreResult<Vec<HeadListItem>>; // SPEC-21 compiles plans
    async fn process(&self, id: Uuid) -> StoreResult<Option<ProcessLogRow>>;
    async fn lineage(&self, q: &LineageQuery) -> StoreResult<Vec<LineageRow>>;
    async fn similar(&self, q: &VectorQuery) -> StoreResult<Vec<(CommitId, f32)>>;
}
```
- **Semantics:** Readers never see uncommitted data. `list_heads` and `search` return registry and head data without snapshots (REQ-STO-020).
- **Origin:** CON-LS-043, CON-UB-042, CON-FU-041

### API-STO-007 — Outbox claiming
- **Kind:** rust trait
- **Signature / shape:**
```rust
#[async_trait]
pub trait OutboxClaim: Send + Sync {
    async fn claim(&self, dispatcher: &str, max: u32, lease: Duration) -> StoreResult<Vec<OutboxRow>>;
    async fn complete(&self, seqs: &[i64]) -> StoreResult<()>;     // delete rows
    async fn release(&self, seqs: &[i64]) -> StoreResult<()>;      // give back early
}
```
- **Origin:** REQ-STO-011

### API-STO-008 — Store conformance suite
- **Kind:** test contract
- **Signature / shape:** A crate `ubos_store::conformance` exposes `run_all(store: Arc<dyn Store>)`. It covers REQ-STO-001…021 and runs against each backend in CI.
- **Origin:** VRD-01-05, VRD-22-03

### API-STO-009 — RuntimeStore
- **Kind:** rust trait
- **Signature / shape:**
```rust
#[async_trait]
pub trait RuntimeStore: Send + Sync {
    async fn get(&self, key: &str) -> RtResult<Option<Bytes>>;
    async fn set(&self, key: &str, val: Bytes, ttl: Option<Duration>) -> RtResult<()>;
    async fn set_nx(&self, key: &str, val: Bytes, ttl: Option<Duration>) -> RtResult<bool>;
    async fn cas(&self, key: &str, expected: Option<Bytes>, val: Bytes, ttl: Option<Duration>) -> RtResult<bool>;
    async fn del(&self, key: &str) -> RtResult<()>;
    async fn incr(&self, key: &str, by: i64, ttl: Option<Duration>) -> RtResult<i64>;
    async fn push(&self, queue: &str, item: Bytes) -> RtResult<()>;
    async fn pop(&self, queues: &[&str], wait: Duration) -> RtResult<Option<(String, Bytes)>>;
    async fn publish(&self, channel: &str, msg: Bytes) -> RtResult<()>;
    async fn subscribe(&self, channels: &[&str]) -> RtResult<BoxStream<'static, (String, Bytes)>>;
    async fn acquire_lease(&self, name: &str, holder: &str, ttl: Duration) -> RtResult<bool>;
    async fn release_lease(&self, name: &str, holder: &str) -> RtResult<()>;
}
```
- **Semantics:** Implementations: Redis 7 (enterprise), and an in-process implementation with optional `rt_*` persistence (personal). Every value is non-authoritative (ADR-004).
- **Origin:** ADR-004, CON-UC-043

### API-STO-010 — BlobStore
- **Kind:** rust trait
- **Signature / shape:**
```rust
#[async_trait]
pub trait BlobStore: Send + Sync {
    async fn put(&self, data: BoxStream<'static, Bytes>, expected_sha256: Option<[u8; 32]>)
        -> BlobResult<BlobRef /* sha256, size */>;
    async fn get(&self, sha256: &[u8; 32]) -> BlobResult<BoxStream<'static, Bytes>>;
    async fn exists(&self, sha256: &[u8; 32]) -> BlobResult<bool>;
    async fn delete(&self, sha256: &[u8; 32]) -> BlobResult<()>;   // GC only
}
```
- **Origin:** REQ-STO-018

### Error codes (STO)

| Code | Category | Meaning |
|---|---|---|
| `STO.UNAVAILABLE` | UNAVAILABLE | database not reachable; retryable |
| `STO.ARCHIVED` | NOT_FOUND | version is in an archived partition; `details.archive` locates it |
| `STO.ENTITY_LOCKED` | CONFLICT | entity status LOCKED |
| `STO.ENTITY_ARCHIVED` | CONFLICT | entity status ARCHIVED (read-only) |
| `STO.SNAPSHOT_TOO_LARGE` | LIMIT | snapshot above the configured maximum (default 1 MiB) |
| `STO.IMMUTABLE_VIOLATION` | INTERNAL | attempt to change an immutable row |
| `STO.RUNTIME_UNAVAILABLE` | UNAVAILABLE | runtime store not reachable |
| `STO.BLOB_NOT_FOUND` | NOT_FOUND | blob hash unknown |
| `STO.BLOB_HASH_MISMATCH` | INVALID | uploaded content does not match expected hash |

(`VER.HEAD_CONFLICT` is defined in SPEC-14; `URI.LOCATOR_TAKEN` in SPEC-12.)
