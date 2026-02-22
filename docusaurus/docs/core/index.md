# konditional-core

`konditional-core` is the typed definition and evaluation layer. It gives you
compile-time-safe feature declarations and deterministic evaluation against a
trusted snapshot.

## Read this page when

- You are starting a new namespace or feature set.
- You need the minimum module surface without runtime loading concerns.
- You want the practical entrypoint before reading deeper theory guarantees.

## Concepts in scope

- **Typed definitions**: `Feature<T, C, M>` and namespace delegates keep
  feature values type-safe at compile time.
- **Total evaluation**: every feature has a required default, so
  `evaluate(...)` always returns a value.
- **Determinism contract**: for the same context and same snapshot,
  evaluation produces the same result.
- **Boundary limit**: this module does not parse untrusted JSON. Parsing and
  materialization live in `konditional-serialization`.

## Minimal example

```kotlin
object AppFeatures : Namespace("app") {
    val darkMode by boolean<Context>(default = false) {
        rule(true) { ios() }
        rule(true) { rampUp { 10.0 } }
    }
}

val enabled: Boolean = AppFeatures.darkMode.evaluate(context)
```

## Related pages

- [Core types](/core/types)
- [Rule DSL reference](/core/rules)
- [Type safety boundaries](/theory/type-safety-boundaries)
- [Determinism proofs](/theory/determinism-proofs)

## Next steps

1. Read [Core DSL best practices](/core/best-practices).
2. Read [Evaluation model](/learn/evaluation-model).
3. Add runtime loading with [konditional-runtime](/runtime).
