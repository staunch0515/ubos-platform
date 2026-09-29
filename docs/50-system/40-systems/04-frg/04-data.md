---
id: UBS-SYS-FRG-04
title: Logic Forge — Data
status: draft
phase: PH-2
depends_on: [UBS-SYS-FRG-02]
---

# Logic Forge — Data

## 1. Project layout

```
my-buk/
  forge.toml          # name, namespace, version, ABI range, dependencies, gates
  forge.lock          # resolved dependencies with root hashes
  model/              # classes, value types, relationships, lenses (YAML)
  sheets/             # governance sheets (textual grammar)
  logic/              # Rhai scripts and modules
  components/         # L3 component sources and WIT
  views/              # view definitions
  seed/               # seed data with valid time
  tests/              # unit, rule, scenario, property tests
  docs/               # hand-written docs merged into generated reference
  .forge/             # build cache (ignored by Git)
```

## 2. Buk archive

### DAT-BukManifest
- **Purpose:** The declaration of one Buk version.
- **Kind:** Definition
- **Stored in:** Buk archive (`manifest.json`), installed as a System-kind object
- **Schema:**
```yaml
DAT-BukManifest:
  name:          { type: string, required: true, description: package name }
  namespace:     { type: string, required: true, description: owned namespace }
  version:       { type: semver, required: true, description: version }
  abi:           { type: string, required: true, description: supported ABI range }
  dependencies:  { type: list<object>, required: false, description: name and semver range }
  capabilities:  { type: list<string>, required: true, description: requested capabilities }
  licence:       { type: object, required: true, description: licence terms metadata }
  content_root:  { type: content_id, required: true, description: root of the content tree }
  migrations:    { type: list<object>, required: false, description: from-version ranges and migration assets }
```
- **Invariants:**
  1. The archive root hash covers the manifest and the content tree.
- **Satisfies:** FR-PKG-011, FR-PKG-012, FR-STD-062

### DAT-BukVerificationRecord
- **Purpose:** The signed outcome of verifying one Buk archive.
- **Kind:** Message
- **Stored in:** Buk archive trailer and EXC listing
- **Schema:**
```yaml
DAT-BukVerificationRecord:
  archive_root: { type: content_id, required: true, description: verified archive }
  forge_version: { type: string, required: true, description: verifier version }
  checks:       { type: list<object>, required: true, description: check name, outcome, findings count }
  coverage:     { type: object, required: true, description: script and rule coverage }
  capabilities: { type: object, required: true, description: requested versus inferred }
  signature:    { type: DAT-Signature, required: true, description: verifier signature }
```
- **Invariants:**
  1. Re-running verification on the same archive with the same Forge version yields the same record body.
- **Satisfies:** FR-PKG-061, FR-PKG-063
