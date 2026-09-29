---
id: UBS-SYS-NTY-04
title: Notary — Data
status: draft
phase: PH-3
depends_on: [UBS-SYS-NTY-02]
---

# Notary — Data

Receipts live in the tenant's VAE as ledger entries (DAT-AnchorReceipt). NTY keeps only
operational state: anchoring schedule per tenant, last root and receipt reference, TSA
health, and pack jobs (request, status, archive location, expiry). Pack archives are stored
in the tenant's object-store prefix with expiring download links.
