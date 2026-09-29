---
id: UBS-SYS-SDK-06
title: Client SDKs — Configuration
status: draft
phase: PH-1
depends_on: [UBS-SYS-SDK-02]
---

# Client SDKs — Configuration

| Option | Default | Design |
|---|---|---|
| `deadlineMs` | 30000 | DSN-SDK-003 |
| `retry.maxAttempts` | 5 | DSN-SDK-003 |
| `retry.baseDelayMs` | 100 | DSN-SDK-003 |
| `reconnect.maxDelayMs` | 30000 | DSN-SDK-004 |
| `pageSize` | 200 | DSN-SDK-005 |
| `cache.maxEntries` | 5000 | DSN-SDK-008 |
| `upload.chunkBytes` | 8 MiB | DSN-SDK-011 |
| `telemetry.propagate` | true | DSN-SDK-016 |
