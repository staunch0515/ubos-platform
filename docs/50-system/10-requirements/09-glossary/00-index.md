---
id: UBS-REQ-09
title: Glossary — Index
status: complete
phase: ALL
depends_on: [UBS-META-01]
---

# Glossary — Index

Terms are defined as `GL-*` items in alphabetical parts. Writers MUST use these terms
exactly (W1.7). **Visible to** says which product layer may show the term in user
interfaces:
- `business`: every layer;
- `builder`: builder and platform layers;
- `platform only`: the platform layer only.

## Files

| File | Range |
|---|---|
| `01-a-l.md` | terms A–L |
| `02-m-z.md` | terms M–Z |

## Term families (overview)

| Family | Terms |
|---|---|
| Architecture | GL-Bpa, GL-Dvm, GL-Bpu, GL-BpuElement, GL-HardenedMechanism, GL-ConformanceProfile, GL-ConformanceVector, GL-Certification |
| Model | GL-Object, GL-ObjectVersion, GL-Class, GL-Trait, GL-Linearization, GL-StateKind, GL-DefinitionKind, GL-LedgerKind, GL-SystemKind, GL-RawDocumentKind, GL-Field, GL-ValueType, GL-Classification, GL-Relationship, GL-Extension, GL-Lens, GL-SchemaVersion, GL-Ontology, GL-Port |
| Versioning | GL-Commit, GL-Branch, GL-BranchKind, GL-ChangeSet, GL-WritePolicy, GL-Diff, GL-Merge, GL-MergeBase, GL-Conflict, GL-Tag, GL-Release, GL-Overlay, GL-BranchInheritance, GL-Tombstone, GL-Revert |
| Time | GL-TransactionTime, GL-ValidTime, GL-Bitemporal, GL-Asof, GL-RuleBinding, GL-Restatement, GL-Retroactive |
| Ledger | GL-Entry, GL-Reversal, GL-Adjustment, GL-PostingGroup, GL-Period |
| Transactions | GL-Process, GL-UnitOfWork, GL-Flush, GL-Idempotency, GL-Outbox, GL-Saga, GL-Provenance |
| Logic | GL-Instruction, GL-InstructionFamily, GL-Abi, GL-Context, GL-Gateway, GL-Profile, GL-Metering, GL-Journal, GL-Replay, GL-LogicAsset, GL-LogicTier, GL-Connector, GL-ImmuneSystem |
| Rules and flow | GL-GovernanceSheet, GL-Selector, GL-Declaration, GL-Cascade, GL-Specificity, GL-Decision, GL-Guard, GL-Lifecycle, GL-Approval, GL-DeferredCommit, GL-Task, GL-Workflow, GL-Simulation |
| Events | GL-Event, GL-Subscription, GL-Job, GL-Schedule, GL-Worker |
| Tenancy and security | GL-Tenant, GL-Vae, GL-VaeTree, GL-Principal, GL-Did, GL-Policy, GL-AuthorizationRelation, GL-Masking, GL-Delegation |
| Packages and interfaces | GL-Buk, GL-Genesis, GL-LoaderBranch, GL-Ubtp, GL-ViewObject, GL-Widget, GL-Vfs, GL-Uri |
| Office | GL-LiveDoc, GL-SmartGrid, GL-WarRoom, GL-ActionMessage, GL-ExecutableBook, GL-Seal |
| AI | GL-ModelGateway, GL-AgentBranch, GL-Tool, GL-Mcp, GL-Copilot, GL-AiBuilder, GL-EvolutionProposal, GL-Knowledge |
| Proof | GL-ContentAddress, GL-MerkleRoot, GL-InclusionProof, GL-ConsistencyProof, GL-Anchor, GL-EvidencePack, GL-Verifier, GL-CryptoShredding |
| Records management | GL-LegalHold, GL-Retention, GL-Worm |
| Deployment | GL-Box, GL-Server, GL-Cell, GL-BrowserKernel, GL-ControlPlane, GL-Projection, GL-Watermark |
| Federation | GL-Federation, GL-NetworkFallback, GL-SharedBpu, GL-TrustRelationship, GL-ReplicationScope |
