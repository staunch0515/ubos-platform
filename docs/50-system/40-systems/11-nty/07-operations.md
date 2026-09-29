---
id: UBS-SYS-NTY-07
title: Notary — Operations
status: draft
phase: PH-3
depends_on: [UBS-SYS-NTY-02]
---

# Notary — Operations

NTY runs as a small stateless service per Server or Cell. TSA contracts and certificate
chains are tracked with renewal reminders. The verifier is released independently with
reproducible builds and published hashes so that third parties can confirm the binary.
