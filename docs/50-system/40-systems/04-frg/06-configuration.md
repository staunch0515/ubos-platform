---
id: UBS-SYS-FRG-06
title: Logic Forge — Configuration
status: draft
phase: PH-1
depends_on: [UBS-SYS-FRG-02]
---

# Logic Forge — Configuration (`forge.toml`)

| Key | Default | Design |
|---|---|---|
| `package.name`, `package.namespace`, `package.version` | — | DSN-FRG-301 |
| `package.abi` | current ABI major | DSN-FRG-301 |
| `dependencies.<name>` | — | DSN-FRG-303 |
| `gates.script_coverage` | 0.80 | DSN-FRG-104 |
| `gates.rule_row_coverage` | 1.00 | DSN-FRG-104 |
| `gates.fail_on_warning` | false | DSN-FRG-101 |
| `types.languages` | ["ts", "rust"] | DSN-FRG-102 |
| `dev.vae_seed` | `seed/` | DSN-FRG-501 |
| `simulate.budget.processes` | 100000 | DSN-FRG-205 |
| `sign.key` | key reference or `sigstore` | DSN-FRG-304 |
| `publish.exchange` | public EXC URL | DSN-FRG-308 |
