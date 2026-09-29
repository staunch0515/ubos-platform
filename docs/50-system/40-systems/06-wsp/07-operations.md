---
id: UBS-SYS-WSP-07
title: Workspace — Operations
status: draft
phase: PH-2
depends_on: [UBS-SYS-WSP-02]
---

# Workspace — Operations

| Topic | Specification |
|---|---|
| delivery | static assets served by NOD (Server) or CDN (Cell) with immutable, hashed file names and a signed manifest |
| versions | the web client version is negotiated with the node's widget catalogue; incompatible clients reload |
| desktop and mobile | signed installers and store releases; auto-update through signed channels |
| telemetry | Web Vitals and error reports without business values; opt-out for customer-operated deployments |
| headless renderer | server-side rendering service for PDFs runs the same bundle in a sandboxed headless Chromium with network disabled |
