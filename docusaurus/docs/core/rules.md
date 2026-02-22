# Rule DSL reference

This page is the practical reference for building deterministic targeting rules
in `konditional-core`.

## Read this page when

- You are writing rule criteria in a namespace.
- You need exact matching semantics for targeting blocks.
- You are composing reusable rule groups across features.

## Concepts in scope

- **Rule forms**: `rule(value) { ... }` and `rule { ... } yields value`.
- **Conjunction semantics**: criteria in one rule are combined with AND.
- **Axis semantics**: repeated values for one axis widen that axis set.
- **Context narrowing**: `whenContext<R> { ... }` gates predicates by type.
- **Reuse**: `ruleSet { ... }` plus `include(...)` composes reusable blocks.

## Targeting primitives

- `locales(...)`
- `platforms(...)`
- `versions { min(...); max(...) }`
- `axis(axisHandle, ...)` and inferred `axis(...)`
- `extension { ... }`
- `whenContext<R> { ... }`
- `rampUp { ... }`
- `allowlist(...)`
- `always()` / `matchAll()`
- `note("...")`

## Example

```kotlin
val checkout by string<Context>(default = "v1") {
    rule {
        ios()
        versions { min(3, 0, 0) }
        rampUp { 25.0 }
        note("iOS staged rollout")
    } yields "v2"

    rule("v1") { always() }
}
```

## Related pages

- [Core DSL best practices](/core/best-practices)
- [Evaluation model](/learn/evaluation-model)
- [Determinism proofs](/theory/determinism-proofs)
- [Type safety boundaries](/theory/type-safety-boundaries)

## Next steps

1. Validate rule ordering with [Evaluation model](/learn/evaluation-model).
2. Review rollout behavior in [Rollout strategies (legacy path)](/rules-and-targeting/rollout-strategies).
3. Confirm parse contracts in [Parse don’t validate](/theory/parse-dont-validate).
