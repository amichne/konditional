---
title: Testing Strategies
sidebar_position: 6
---

# Testing Strategies

Test deterministic behavior and boundary failure modes as first-class invariants.

**Prerequisites:** You have completed [Quickstart Verify End-to-End](/quickstart/verify-end-to-end).

## Core Unit Tests

- Evaluate the same feature and context multiple times; assert identical results.
- Assert default fallback when no rule matches.
- Assert namespace isolation between unrelated namespaces.

## Boundary Tests

- Pass malformed JSON and assert `Result.failure` with `ParseError` present.
- Assert last-known-good evaluation remains unchanged after failed loads.

## Golden Fixture Tests

- Keep representative snapshot and patch JSON fixtures in version control.
- Verify decode/load behavior across version upgrades.

## Migration Tests

- Use shadow evaluation in CI to compare baseline and candidate behavior before cutover.

## Expected Outcome

After this guide, your test suite proves deterministic evaluation, safe boundary behavior, and migration readiness.

## Next Steps

- [Theory: Determinism Proofs](/theory/determinism-proofs)
- [Guide: Migration from Legacy](/guides/migration-from-legacy)
