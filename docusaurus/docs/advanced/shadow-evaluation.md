---
slug: /legacy/advanced-shadow-evaluation
title: Shadow evaluation (legacy bridge)
description: Legacy route that bridges to canonical shadow evaluation docs.
unlisted: true
---

This legacy page is a bridge. Canonical shadow-evaluation procedures live in
the observability and production-operation guides.

## Read this page when

- You landed here from an old bookmark or external link.
- You need the current shadow-evaluation procedure and mismatch handling flow.

## Deterministic steps

1. Start with [Shadow evaluation](/observability/shadow-evaluation) for
   baseline and candidate execution semantics.
2. Validate behavior-drift constraints in
   [Migration and shadowing](/theory/migration-and-shadowing).
3. Use [Debugging determinism](/how-to-guides/debugging-determinism) to verify
   stable IDs and bucketing inputs before escalation.
4. Use [Operational debugging](/production-operations/debugging) if mismatches
   persist in production.

## Completion checklist

- [ ] Baseline behavior remains authoritative.
- [ ] Shadow mismatches are observable and non-blocking.
- [ ] Migration decision criteria are documented for your rollout.

## Next steps

- [Debugging determinism](/how-to-guides/debugging-determinism)
- [Operational debugging](/production-operations/debugging)
- [Failure modes](/production-operations/failure-modes)
- [Troubleshooting index](/troubleshooting/)
