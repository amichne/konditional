---
title: Parse Boundary
sidebar_position: 6
---

# Parse Boundary

JSON and external configuration are untrusted inputs. Konditional parses them into trusted runtime state only through explicit `Result` boundaries.

## Boundary API Shape

```kotlin
val result = NamespaceSnapshotLoader(AppFeatures).load(json)
```

- `Result.success(MaterializedConfiguration)`: trusted payload loaded.
- `Result.failure(KonditionalBoundaryFailure(parseError))`: update rejected, active state unchanged.

## Parse Error Taxonomy

`ParseError` variants include invalid JSON/snapshot shapes and feature-key resolution failures.

## Why This Model

- No exceptions as normal control flow.
- Typed errors can be logged, metered, and tested.
- Last-known-good runtime state survives malformed updates.

## Next Steps

- [Quickstart: Load First Snapshot Safely](/quickstart/load-first-snapshot-safely)
- [Parse Don\'t Validate Theory](/theory/parse-dont-validate)
