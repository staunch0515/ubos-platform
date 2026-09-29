---
id: UBS-ARC-08
title: Capability Allocation to Systems
status: draft
phase: ALL
depends_on: [UBS-REQ-03, UBS-ARC-01]
---

# Capability Allocation to Systems

Each capability has exactly one **primary system**, which is accountable for it in its
system chapter, and zero or more **supporting systems**. The allocation was derived from
the `Systems` fields of the capability's functional requirements (the first-listed system
of each requirement is weighted highest). Architects reviewed it for consistency with
UBS-ARC-01.

### AR-034 — Single accountable system per capability
- **Rule:** Each capability MUST be specified end to end in the chapter of its primary system. Supporting systems specify only their part and reference the primary chapter.
- **Realises:** UBS-REQ-03
- **Rationale:** No capability falls between systems.

| Name | Capability | Phase | Primary | Supporting |
|---|---|---|---|---|
| Class definition | CAP-MODEL-01 | PH-1 | DVM | FRG |
| Inheritance | CAP-MODEL-02 | PH-1 | DVM | STU, FRG |
| Polymorphism | CAP-MODEL-03 | PH-1 | DVM | FRG, WSP |
| Value types and constraints | CAP-MODEL-04 | PH-1 | DVM | SDK |
| State kinds | CAP-MODEL-05 | PH-1 | DVM | NOD |
| Relationships | CAP-MODEL-06 | PH-1 | DVM | FED |
| Class extension | CAP-MODEL-07 | PH-1 | DVM | — |
| Schema evolution | CAP-MODEL-08 | PH-2 | DVM | FRG, STU |
| Base ontology | CAP-MODEL-09 | PH-2 | DVM | FRG |
| Model introspection and export | CAP-MODEL-10 | PH-1 | DVM | STU, FRG, SDK, BRG, AGT |
| Commit history | CAP-VER-01 | PH-1 | DVM | NOD, SDK |
| Branches | CAP-VER-02 | PH-1 | DVM | STU |
| Change sets | CAP-VER-03 | PH-1 | DVM | STU, WSP |
| Diff | CAP-VER-04 | PH-1 | DVM | WSP |
| Merge | CAP-VER-05 | PH-1 | DVM | — |
| Tags and releases | CAP-VER-06 | PH-1 | DVM | STU |
| Revert and undo | CAP-VER-07 | PH-1 | DVM | — |
| Branch inheritance | CAP-VER-08 | PH-1 | DVM | STU |
| History tiering | CAP-VER-09 | PH-4 | DVM | NOD |
| Transaction-time travel | CAP-VER-10 | PH-1 | DVM | SDK |
| Transaction time | CAP-TIME-01 | PH-1 | DVM | NOD |
| Valid time | CAP-TIME-02 | PH-1 | DVM | SDK |
| Bitemporal queries | CAP-TIME-03 | PH-1 | DVM | SDK |
| Retroactive correction | CAP-TIME-04 | PH-2 | DVM | STU, WSP |
| Future-dated change | CAP-TIME-05 | PH-1 | DVM | NOD, WSP |
| Rule-version binding | CAP-TIME-06 | PH-1 | DVM | — |
| Ledger classes | CAP-LEDG-01 | PH-1 | DVM | — |
| Reversal and adjustment | CAP-LEDG-02 | PH-1 | DVM | — |
| Balances as projections | CAP-LEDG-03 | PH-1 | DVM | — |
| Sequencing | CAP-LEDG-04 | PH-1 | DVM | — |
| Double entry | CAP-LEDG-05 | PH-2 | DVM | — |
| Period close | CAP-LEDG-06 | PH-2 | DVM | — |
| Reconciliation | CAP-LEDG-07 | PH-2 | DVM | BRG, WSP |
| Process and atomic commit | CAP-TXN-01 | PH-1 | DVM | NOD |
| Optimistic concurrency | CAP-TXN-02 | PH-1 | DVM | — |
| Idempotency | CAP-TXN-03 | PH-1 | DVM | NOD, SDK |
| Provenance and causality | CAP-TXN-04 | PH-1 | DVM | — |
| Post-commit effects | CAP-TXN-05 | PH-1 | DVM | NOD, BRG |
| Long-running processes | CAP-TXN-06 | PH-2 | DVM | NOD, WSP, CTL |
| Instruction set | CAP-LOGIC-01 | PH-1 | DVM | BPA, SDK |
| Execution context | CAP-LOGIC-02 | PH-1 | DVM | — |
| L1 expressions | CAP-LOGIC-03 | PH-1 | DVM | BPA, FRG |
| L2 typed scripts | CAP-LOGIC-04 | PH-1 | DVM | FRG |
| L3 WASM components | CAP-LOGIC-05 | PH-3 | DVM | FRG |
| Sandbox and metering | CAP-LOGIC-06 | PH-1 | DVM | NOD |
| Determinism and replay | CAP-LOGIC-07 | PH-1 | DVM | BPA, FRG |
| Logic asset lifecycle | CAP-LOGIC-08 | PH-1 | DVM | FRG |
| Static analysis | CAP-LOGIC-09 | PH-1 | DVM | FRG, STU |
| Connectors | CAP-LOGIC-10 | PH-1 | DVM | BRG, NOD |
| Governance sheets | CAP-RULE-01 | PH-1 | DVM | BPA |
| Cascade and explanation | CAP-RULE-02 | PH-1 | DVM | BPA |
| Validation and invariants | CAP-RULE-03 | PH-1 | DVM | SDK |
| Decision tables | CAP-RULE-04 | PH-2 | DVM | BRG, FRG, STU |
| Policy decisions with veto | CAP-RULE-05 | PH-1 | DVM | — |
| Attach, detach and effective periods | CAP-RULE-06 | PH-2 | DVM | — |
| Simulation and impact analysis | CAP-RULE-07 | PH-2 | DVM | FRG |
| Rule testing | CAP-RULE-08 | PH-2 | DVM | FRG |
| Lifecycles | CAP-FLOW-01 | PH-1 | DVM | — |
| Actions (ports) | CAP-FLOW-02 | PH-1 | DVM | — |
| Guards | CAP-FLOW-03 | PH-1 | DVM | — |
| Approvals and segregation of duties | CAP-FLOW-04 | PH-2 | DVM | — |
| Tasks and work queues | CAP-FLOW-05 | PH-2 | DVM | WSP |
| Durable workflows | CAP-FLOW-06 | PH-2 | DVM | NOD |
| Dialogues | CAP-FLOW-07 | PH-2 | DVM | WSP, SDK, AGT |
| SLAs and escalation | CAP-FLOW-08 | PH-2 | DVM | WSP |
| Delegation | CAP-FLOW-09 | PH-2 | DVM | — |
| Events | CAP-EVT-01 | PH-1 | DVM | BRG, NOD |
| Subscriptions | CAP-EVT-02 | PH-1 | DVM | STU |
| Durable jobs | CAP-EVT-03 | PH-1 | NOD | DVM |
| Schedules | CAP-EVT-04 | PH-1 | NOD | DVM, WSP |
| Retries and dead letters | CAP-EVT-05 | PH-1 | NOD | CTL |
| Worker pool | CAP-EVT-06 | PH-4 | NOD | CTL |
| Replay and backfill | CAP-EVT-07 | PH-2 | DVM | NOD |
| Criteria queries | CAP-QRY-01 | PH-1 | DVM | SDK |
| Full-text search | CAP-QRY-02 | PH-2 | DVM | NOD |
| Semantic search | CAP-QRY-03 | PH-3 | DVM | AGT |
| Aggregation | CAP-QRY-04 | PH-2 | DVM | — |
| History queries | CAP-QRY-05 | PH-1 | DVM | — |
| Saved queries and list views | CAP-QRY-06 | PH-2 | DVM | WSP |
| Consistent pagination | CAP-QRY-07 | PH-1 | DVM | — |
| Local authentication | CAP-IAM-01 | PH-1 | NOD | CTL |
| Enterprise SSO | CAP-IAM-02 | PH-2 | NOD | CTL, WSP |
| Principals and keys | CAP-IAM-03 | PH-1 | DVM | NOD |
| Policy authorization | CAP-IAM-04 | PH-1 | DVM | NOD, SDK |
| Relationship authorization | CAP-IAM-05 | PH-2 | DVM | — |
| Masking and filtering | CAP-IAM-06 | PH-2 | DVM | — |
| Provisioning | CAP-IAM-07 | PH-2 | NOD | DVM, CTL |
| Policy analysis and access review | CAP-IAM-08 | PH-3 | DVM | CTL, WSP |
| Secrets and keys | CAP-IAM-09 | PH-1 | NOD | DVM, CTL |
| Sessions and devices | CAP-IAM-10 | PH-2 | NOD | AGT |
| Tenants and VAEs | CAP-TEN-01 | PH-1 | DVM | NOD, BPA |
| Isolation | CAP-TEN-02 | PH-1 | DVM | NOD |
| Overlays | CAP-TEN-03 | PH-1 | DVM | — |
| VAE hierarchy on one node | CAP-TEN-04 | PH-2 | DVM | — |
| Tenant lifecycle | CAP-TEN-05 | PH-2 | NOD | CTL, DVM |
| Portability | CAP-TEN-06 | PH-2 | NOD | BPA, DVM |
| Placement and migration | CAP-TEN-07 | PH-4 | CTL | NOD, AGT |
| Audit trail | CAP-AUD-01 | PH-1 | DVM | NOD, BRG |
| Lineage | CAP-AUD-02 | PH-2 | DVM | SDK |
| Explanations | CAP-AUD-03 | PH-2 | DVM | WSP |
| Audit views and reports | CAP-AUD-04 | PH-2 | DVM | WSP |
| Access logging | CAP-AUD-05 | PH-2 | DVM | NOD |
| Legal hold | CAP-AUD-06 | PH-3 | DVM | — |
| Retention and WORM | CAP-AUD-07 | PH-3 | DVM | NOD, WSP |
| Server-driven UI | CAP-UX-01 | PH-2 | DVM | WSP, SDK |
| Workspace shell | CAP-UX-02 | PH-2 | WSP | — |
| Forms, lists, details | CAP-UX-03 | PH-2 | WSP | DVM |
| Progressive disclosure | CAP-UX-04 | PH-2 | WSP | STU |
| History and as-of viewer | CAP-UX-05 | PH-2 | WSP | — |
| Change review | CAP-UX-06 | PH-2 | WSP | STU |
| Desktop and mobile clients | CAP-UX-07 | PH-3 | WSP | NOD, SDK |
| Offline experience | CAP-UX-08 | PH-3 | WSP | — |
| External portal | CAP-UX-09 | PH-3 | WSP | NOD, DVM |
| Theming and branding | CAP-UX-10 | PH-3 | WSP | — |
| Live Doc | CAP-OFFICE-01 | PH-2 | WSP | DVM |
| Templates and clauses | CAP-OFFICE-02 | PH-2 | DVM | WSP, STU |
| Redlining and negotiation | CAP-OFFICE-03 | PH-3 | WSP | DVM |
| Smart Grid | CAP-OFFICE-04 | PH-3 | WSP | DVM, BRG |
| War Room | CAP-OFFICE-05 | PH-3 | WSP | DVM |
| Action Messages | CAP-OFFICE-06 | PH-3 | WSP | DVM, NOD |
| Executable Books | CAP-OFFICE-07 | PH-4 | WSP | FRG, DVM, STU, EXC |
| Rendering and export | CAP-OFFICE-08 | PH-2 | WSP | — |
| Model gateway | CAP-AI-01 | PH-3 | AGT | CTL |
| Agent identity and budget | CAP-AI-02 | PH-3 | AGT | DVM, NOD |
| Agent branches | CAP-AI-03 | PH-3 | AGT | DVM |
| Tool exposure | CAP-AI-04 | PH-3 | AGT | DVM |
| BPU knowledge | CAP-AI-05 | PH-3 | AGT | DVM |
| Copilot | CAP-AI-06 | PH-3 | AGT | WSP |
| AI builder | CAP-AI-07 | PH-3 | AGT | FRG, STU, DVM |
| Evaluation and monitoring | CAP-AI-08 | PH-3 | AGT | — |
| AI inside logic | CAP-AI-09 | PH-3 | DVM | AGT |
| Governed evolution | CAP-AI-10 | PH-3 | AGT | DVM |
| Buk format | CAP-PKG-01 | PH-2 | FRG | DVM |
| Dependencies | CAP-PKG-02 | PH-2 | FRG | DVM |
| Installation | CAP-PKG-03 | PH-2 | DVM | STU, NOD |
| Upgrade | CAP-PKG-04 | PH-2 | DVM | CTL, FRG, STU |
| Removal and deprecation | CAP-PKG-05 | PH-2 | DVM | EXC, STU |
| Verification | CAP-PKG-06 | PH-2 | FRG | DVM, EXC |
| Genesis Buks | CAP-PKG-07 | PH-1 | NOD | DVM |
| Licence hooks | CAP-PKG-08 | PH-4 | DVM | NOD, EXC |
| UBTP protocol | CAP-INT-01 | PH-1 | NOD | SDK, BPA |
| REST facade | CAP-INT-02 | PH-2 | NOD | BRG |
| Webhooks | CAP-INT-03 | PH-2 | NOD | CTL, BRG, STU |
| Connector framework | CAP-INT-04 | PH-2 | BRG | DVM, NOD |
| Event streaming out | CAP-INT-05 | PH-3 | BRG | — |
| Email and chat | CAP-INT-06 | PH-2 | BRG | WSP |
| Financial adapters | CAP-INT-07 | PH-3 | BRG | — |
| API credentials | CAP-INT-08 | PH-2 | NOD | — |
| Declared projections | CAP-ANL-01 | PH-1 | DVM | — |
| Projection rebuild | CAP-ANL-02 | PH-1 | DVM | — |
| Reports | CAP-ANL-03 | PH-2 | DVM | WSP, NOD |
| Embedded analytics | CAP-ANL-04 | PH-3 | BRG | NOD |
| CDC export | CAP-ANL-05 | PH-3 | BRG | — |
| Dashboards | CAP-ANL-06 | PH-3 | WSP | — |
| Canonical content addressing | CAP-PROOF-01 | PH-1 | DVM | BPA |
| Signed commits | CAP-PROOF-02 | PH-1 | DVM | NOD |
| Merkle roots and proofs | CAP-PROOF-03 | PH-1 | DVM | BPA |
| Anchoring | CAP-PROOF-04 | PH-3 | NTY | DVM |
| Evidence packs | CAP-PROOF-05 | PH-3 | NTY | — |
| Independent verifier | CAP-PROOF-06 | PH-3 | NTY | BPA |
| Encrypted fields and crypto-shredding | CAP-PROOF-07 | PH-2 | DVM | NOD |
| Object exchange protocol | CAP-SYNC-01 | PH-3 | NOD | DVM, BPA |
| Box–Server sync | CAP-SYNC-02 | PH-3 | NOD | DVM, CTL, WSP |
| Offline conflicts | CAP-SYNC-03 | PH-3 | DVM | WSP |
| Selective replication | CAP-SYNC-04 | PH-3 | NOD | — |
| Federation registry | CAP-SYNC-05 | PH-5 | FED | DVM, NOD |
| Cross-node VAE tree | CAP-SYNC-06 | PH-5 | FED | DVM, NOD |
| Shared BPUs | CAP-SYNC-07 | PH-5 | FED | DVM |
| Browser kernel | CAP-SYNC-08 | PH-5 | DVM | SDK, WSP, BPA |
| Installation and bootstrap | CAP-OPS-01 | PH-1 | NOD | — |
| Backup and restore | CAP-OPS-02 | PH-1 | NOD | CTL |
| Upgrade | CAP-OPS-03 | PH-2 | NOD | DVM |
| Telemetry | CAP-OPS-04 | PH-1 | NOD | DVM, CTL |
| Health and diagnostics | CAP-OPS-05 | PH-1 | NOD | — |
| Disaster recovery | CAP-OPS-06 | PH-2 | NOD | CTL |
| Capacity and tiering | CAP-OPS-07 | PH-4 | CTL | NOD |
| Fleet management | CAP-OPS-08 | PH-4 | CTL | NOD |
| Configuration management | CAP-OPS-09 | PH-1 | NOD | — |
| Metering | CAP-BILL-01 | PH-4 | CTL | NOD |
| Plans and entitlements | CAP-BILL-02 | PH-4 | CTL | NOD |
| Licences | CAP-BILL-03 | PH-4 | EXC | CTL |
| Invoicing and payouts | CAP-BILL-04 | PH-4 | EXC | CTL |
| Cost controls | CAP-BILL-05 | PH-4 | CTL | AGT |
| CLI | CAP-DEV-01 | PH-1 | FRG | — |
| Local environment | CAP-DEV-02 | PH-1 | FRG | NOD |
| Type generation | CAP-DEV-03 | PH-1 | FRG | — |
| Simulator | CAP-DEV-04 | PH-2 | FRG | DVM |
| Test framework | CAP-DEV-05 | PH-1 | FRG | — |
| SDKs | CAP-DEV-06 | PH-1 | SDK | — |
| IDE integration | CAP-DEV-07 | PH-2 | FRG | — |
| Documentation generation | CAP-DEV-08 | PH-2 | FRG | — |
| Debugging and tracing | CAP-DEV-09 | PH-2 | FRG | DVM |
| Spreadsheet import | CAP-MIG-01 | PH-2 | BRG | STU |
| Document import | CAP-MIG-02 | PH-3 | BRG | WSP, AGT |
| Legacy bulk load | CAP-MIG-03 | PH-2 | BRG | DVM |
| Mapping definitions | CAP-MIG-04 | PH-2 | BRG | — |
| Migration reconciliation | CAP-MIG-05 | PH-2 | BRG | — |
| Notification rules | CAP-NTF-01 | PH-2 | NOD | DVM |
| Channels | CAP-NTF-02 | PH-2 | NOD | WSP |
| Preferences and digests | CAP-NTF-03 | PH-2 | WSP | NOD |
| Blob storage | CAP-FILE-01 | PH-1 | NOD | DVM, SDK |
| Content safety | CAP-FILE-02 | PH-2 | NOD | — |
| Previews and extraction | CAP-FILE-03 | PH-2 | NOD | WSP |
| VFS paths | CAP-FILE-04 | PH-2 | DVM | NOD, SDK |
| Electronic signatures | CAP-SIGN-01 | PH-2 | WSP | NOD, DVM |
| Document seals | CAP-SIGN-02 | PH-2 | DVM | WSP |
| Signing ceremonies | CAP-SIGN-03 | PH-2 | DVM | WSP |
| External signature providers | CAP-SIGN-04 | PH-4 | WSP | BRG |
| Languages | CAP-LOC-01 | PH-2 | WSP | DVM |
| Currencies and rates | CAP-LOC-02 | PH-2 | DVM | — |
| Time zones | CAP-LOC-03 | PH-1 | DVM | NOD, WSP |
| Business calendars | CAP-LOC-04 | PH-2 | DVM | — |
| Formats | CAP-LOC-05 | PH-2 | WSP | DVM |
| Standard versioning | CAP-STD-01 | PH-0 | BPA | — |
| Conformance vectors | CAP-STD-02 | PH-0 | BPA | FRG |
| Formal models | CAP-STD-03 | PH-0 | BPA | — |
| Compatibility policy | CAP-STD-04 | PH-0 | BPA | — |
| Certification | CAP-STD-05 | PH-4 | BPA | — |
| Reference licensing | CAP-STD-06 | PH-4 | BPA | DVM |
| Extension registry | CAP-STD-07 | PH-4 | BPA | — |
