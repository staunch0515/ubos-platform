---
id: UBS-SYS-CTL-06
title: Control Plane — Configuration
status: draft
phase: PH-4
depends_on: [UBS-SYS-CTL-02]
---

# Control Plane — Configuration

| Key | Default | Range | Design |
|---|---|---|---|
| `heartbeat.missing_after` | 3 | 2–10 heartbeats | DSN-CTL-102 |
| `certs.lifetime_days` | 30 | 1–90 | DSN-CTL-104 |
| `placement.headroom_pct` | 25 | 10–50 | DSN-CTL-201 |
| `migration.catchup_lag_commits` | 100 | 10–10000 | DSN-CTL-203 |
| `migration.pause_timeout_s` | 60 | 5–600 | DSN-CTL-203 |
| `migration.retire_after_days` | 7 | 1–90 | DSN-CTL-203 |
| `waves.canary_soak_hours` | 24 | 1–168 | DSN-CTL-301 |
| `waves.gate.error_rate_increase_pct` | 20 | 1–100 | DSN-CTL-301 |
| `usage.gap_alert_hours` | 6 | 1–72 | DSN-CTL-401 |
| `capacity.alert_threshold_pct` | 75 | 50–95 | DSN-CTL-501 |
| `capacity.forecast_days` | 30 | 7–180 | DSN-CTL-501 |
| `approvals.required_for` | delete, kek_destroy, evacuate, forced_migrate | list | DSN-CTL-003 |
