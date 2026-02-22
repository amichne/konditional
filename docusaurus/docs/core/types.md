# Core types

This page summarizes the types you use directly in `konditional-core`.
It is scoped to practical modeling choices and links to theory for guarantees.

## Read this page when

- You are modeling new feature domains or custom values.
- You are deciding where to extend `Context`.
- You are reviewing type boundaries between core and serialization.

## Concepts in scope

- **`Feature<T, C, M>`**: typed feature handle bound to value type, context type,
  and namespace.
- **`Context`**: runtime evaluation input contract.
- **`Namespace`**: isolation boundary and registry owner.
- **`FeatureId`**: stable serialized identifier.
- **`StableId`**: deterministic bucketing key.
- **`Version`**: semantic version comparator for targeting.
- **`Konstrained<S>`**: typed custom value contract for parse-boundary schema
  enforcement.

## Modeling guidance

- Prefer `@JvmInline value class` for identifiers and constrained primitives.
- Keep `Context` extensions additive and deterministic.
- Treat `Konstrained` values as trusted only after parse-boundary decoding.

## Related pages

- [Core API reference](/core/reference)
- [Rule DSL reference](/core/rules)
- [Serialization persistence format](/serialization/persistence-format)
- [Type safety boundaries](/theory/type-safety-boundaries)

## Next steps

1. Apply these types in [Core DSL best practices](/core/best-practices).
2. Verify evaluation behavior in [Evaluation model](/learn/evaluation-model).
3. Validate JSON boundaries in [Serialization reference](/serialization/reference).
