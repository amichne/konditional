---
title: Feature Evaluation (Legacy)
description: Legacy path for evaluation APIs.
unlisted: true
---

This page has moved.

See [Namespace operations](/reference/api/namespace-operations/) and [Runtime operations](/runtime/operations/).

## Explain

Use `explain(...)` from the runtime operations API to inspect evaluation decisions.

## Non-Throwing Variants

Use `evaluateSafely(...)` / `explainSafely(...)` when you want typed absence handling instead of
`IllegalStateException` for missing feature definitions.

Both APIs return `ParseResult`:
- `ParseResult.Success(...)` for normal evaluations
- `ParseResult.Failure(ParseError.FeatureNotFound(...))` when the feature is not present in the target registry
