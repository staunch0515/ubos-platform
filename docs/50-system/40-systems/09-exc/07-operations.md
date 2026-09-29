---
id: UBS-SYS-EXC-07
title: Buk Exchange — Operations
status: draft
phase: PH-4
depends_on: [UBS-SYS-EXC-02]
---

# Buk Exchange — Operations

| Topic | Specification |
|---|---|
| deployment | stateless API, isolated verification workers without internet, WORM bucket with CDN for downloads |
| signing keys | EXC listing and licence keys in HSM; published key history |
| security response | advisory publication within 24 h of a confirmed critical issue; yank with notices |
| compliance | payment processing through a PCI DSS Level 1 processor; tax calculation through a tax service |
