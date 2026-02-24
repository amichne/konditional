---
title: Parse Don\'t Validate
sidebar_position: 4
---

# Parse Don\'t Validate

## Invariant

External payloads are untrusted. They must be parsed into trusted typed models before entering runtime state.

## Model

- Input boundary returns `Result<T>`.
- Failures carry typed `ParseError` via `KonditionalBoundaryFailure`.
- No partial updates on failure.

## Why This Matters

Validation-only pipelines often allow partially-invalid states to leak inward. Parsing into trusted types prevents invalid intermediate state from entering core evaluation.

## Test Evidence

| Test | Evidence |
| --- | --- |
| `BoundaryFailureResultTest` | Parse failures remain typed and inspectable through result channel. |
| `ConfigurationSnapshotCodecTest` | Snapshot decode enforces schema-aware trusted materialization. |

## Next Steps

- [Concept: Parse Boundary](/concepts/parse-boundary)
- [Reference: Snapshot Load Options](/reference/snapshot-load-options)
