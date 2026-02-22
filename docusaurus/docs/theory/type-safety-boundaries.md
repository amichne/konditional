# Type safety boundaries

This page is the canonical boundary model for where compile-time guarantees end
and where parse-time guarantees begin.

## Read this page when

- You need precise language for "type-safe" claims.
- You are designing APIs that cross from JSON into core/runtime.
- You are writing tests that distinguish compiler guarantees from runtime checks.

## Concepts in scope

- **Compile-time domain**: statically declared features, context typing, and
  typed rule values.
- **Parse boundary domain**: untrusted payloads become trusted only through
  typed decode results.
- **Trusted runtime domain**: evaluation over a materialized snapshot remains
  typed and total.

## Guarantee model

- For statically declared features, Kotlin typing enforces value and context
  compatibility.
- `evaluate(...)` remains total because each feature has a required default.
- External payloads are not trusted until decoding succeeds.

## Boundary implications

- Compiler guarantees do not parse JSON.
- Runtime guarantees do not replace parse-boundary checks.
- Manual construction of trusted snapshot types bypasses boundary validation and
  must be treated as trusted-only code.

## Related pages

- [Parse don’t validate](/theory/parse-dont-validate)
- [Determinism proofs](/theory/determinism-proofs)
- [Serialization reference](/serialization/reference)
- [Learn: type safety](/learn/type-safety)

## Next steps

1. Apply boundary handling patterns from [Parse don’t validate](/theory/parse-dont-validate).
2. Validate API usage in [Core API reference](/core/reference).
3. Add parse failure tests in [Serialization persistence format](/serialization/persistence-format).
