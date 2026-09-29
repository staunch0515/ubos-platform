---
id: UBS-SYS-WSP-04
title: Workspace — Client Data
status: draft
phase: PH-2
depends_on: [UBS-SYS-WSP-02]
---

# Workspace — Client Data

| Data | Where | Rule |
|---|---|---|
| view payload cache | memory (SDK cache) | keyed by object version and view version |
| drafts | IndexedDB (web), local node (desktop, mobile) | encrypted at rest on shared devices; cleared at sign-out |
| co-editing state | CRDT document per draft, persisted as commits at save points | never authoritative until committed |
| preferences | user preference objects in the VAE | synced across devices |
| translations | bundles per locale, versioned with the widget catalogue | model labels come from the VAE |
