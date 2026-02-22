# Core API reference

This page defines the practical evaluation API surface in `konditional-core`.
It stays implementation-focused and defers formal guarantees to theory pages.

## Read this page when

- You need exact behavior of `evaluate(...)`.
- You are integrating core evaluation into application code.
- You are debugging rule selection and default fallback paths.

## API in scope

### `Feature.evaluate(context, registry = namespace): T`

```kotlin
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluate(
    context: C,
    registry: NamespaceRegistry = namespace,
): T
```

- Returns a value of declared type `T`.
- Uses the feature default when no rule matches.
- Reads from the provided registry snapshot.

## Evaluation sequence

1. Resolve the feature definition from the registry snapshot.
2. If namespace kill-switch is enabled, return the default.
3. If the feature is inactive, return the default.
4. Evaluate rules in deterministic order.
5. For the first matching rule, apply ramp-up and allowlist checks.
6. Return the matched rule value, or the default if no rule qualifies.

## Boundary notes

- Core evaluation expects a trusted configuration snapshot.
- Missing feature registration in the registry is an integration error.
- Parsing and typed parse errors are handled in serialization/runtime modules.

## Related pages

- [Rule DSL reference](/core/rules)
- [Core types](/core/types)
- [Evaluation model](/learn/evaluation-model)
- [Determinism proofs](/theory/determinism-proofs)

## Next steps

1. Apply these APIs with [Core DSL best practices](/core/best-practices).
2. Load snapshots safely via [Runtime operations](/runtime/operations).
3. Parse boundary payloads via [Serialization reference](/serialization/reference).
