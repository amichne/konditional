# Type safety

Konditional combines compile-time type guarantees with explicit parse-boundary
handling for untrusted configuration inputs.

## Read this page when

- You need a practical boundary between Kotlin typing and JSON parsing.
- You are migrating from string-keyed flag systems.
- You are writing tests for invalid payload handling.

## Concepts in scope

- **Compile-time safety**: statically declared features are typed end-to-end.
- **Total evaluation**: required defaults keep `evaluate(...)` non-null.
- **Context typing**: feature context constraints are compiler-enforced.
- **Parse boundary**: untrusted payloads become trusted snapshots only through
  `Result`-based decoding.

## Practical workflow

1. Declare feature types and defaults in Kotlin.
2. Evaluate with the required context type.
3. Decode external payloads using serialization APIs.
4. On parse failure, keep the last-known-good snapshot active.

## Related pages

- [Core types](/core/types)
- [Serialization reference](/serialization/reference)
- [Type safety boundaries](/theory/type-safety-boundaries)
- [Parse don’t validate](/theory/parse-dont-validate)

## Next steps

1. Use this model while authoring rules in [Core DSL best practices](/core/best-practices).
2. Validate end-to-end flow in [Configuration lifecycle](/learn/configuration-lifecycle).
3. Add boundary tests from [Serialization persistence format](/serialization/persistence-format).
