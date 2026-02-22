# Parse don’t validate

This page defines the canonical boundary discipline: parse untrusted input into
trusted typed models, or reject with typed errors.

## Read this page when

- You are implementing config ingestion paths.
- You are deciding how errors cross module boundaries.
- You are auditing for exception-driven control flow at the boundary.

## Concepts in scope

- **Untrusted input**: JSON payloads, file content, network responses.
- **Trusted output**: `MaterializedConfiguration` produced by decode success.
- **Typed failure**: parse failure captured by `Result.failure(...)` with parse
  error details.

## Boundary contract

```kotlin
decode(untrustedJson, schema, options) -> Result<MaterializedConfiguration>
```

- Success means the payload has crossed into trusted domain types.
- Failure means no runtime snapshot update occurs.
- Control flow is explicit through result branching, not exceptions.

## Invariants

- Invalid payloads never become active snapshots.
- Boundary logic must be deterministic for the same `(payload, schema, options)`.
- Last-known-good snapshot remains active on boundary failure.

## Related pages

- [Type safety boundaries](/theory/type-safety-boundaries)
- [Serialization reference](/serialization/reference)
- [Runtime lifecycle](/runtime/lifecycle)
- [Learn: configuration lifecycle](/learn/configuration-lifecycle)

## Next steps

1. Use strict load options as default.
2. Add boundary tests for malformed JSON and unknown feature keys.
3. Wire rejection handling into [Runtime operations](/runtime/operations).
