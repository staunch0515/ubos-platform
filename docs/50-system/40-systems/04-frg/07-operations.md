---
id: UBS-SYS-FRG-07
title: Logic Forge — Operations
status: draft
phase: PH-2
depends_on: [UBS-SYS-FRG-02]
---

# Logic Forge — Operations

| Topic | Specification |
|---|---|
| distribution | single static binary per platform (Linux, macOS, Windows; x86-64 and ARM64); npm wrapper; VS Code extension bundles the binary |
| CI templates | GitHub Actions and GitLab CI templates: check, test with JUnit output, verify, and sign on tag |
| CI signing keys | CI verification records are signed with a project CI key held in the CI secret store; tenants decide which verifier keys they trust |
| Forge service in NOD | runs in the NOD execution pool with the batch profile; budgets per request |
| EXC verification service | isolated workers without network access except to EXC storage; reproducibility checked by re-running 5% of verifications on a second worker |
| cache | `.forge/` content-addressed cache; safe to delete |
