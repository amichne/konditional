# Core DSL best practices

Use this page as the practical authoring standard for core feature definitions.
It focuses on maintainable DSL usage and links to theory pages for guarantees.

## Read this page when

- You are creating or refactoring feature declarations.
- You are reviewing pull requests that change targeting behavior.
- You want stable patterns that scale across namespaces.

## Steps in scope

1. Scope features to one clear namespace boundary.
2. Keep identifiers typed (`FeatureId`, axis values, context extensions).
3. Write deterministic rules with explicit criteria and explicit defaults.
4. Move untrusted data handling to parsing boundaries, never inside rules.
5. Reuse rule logic with `ruleSet` and `include(...)` to avoid drift.

## Recommended patterns

```kotlin
object CheckoutFlags : Namespace("checkout") {
    private val environmentAxis = axis<Environment>("environment")

    val checkoutVariant by string<Context>(default = "classic") {
        rule("fast") {
            axis(environmentAxis, Environment.PROD)
            ios()
            rampUp { 20.0 }
        }

        rule {
            android()
            versions { min(3, 2, 0) }
        } yields "fast"
    }
}
```

## Related pages

- [Rule DSL reference](/core/rules)
- [Core types](/core/types)
- [Namespace isolation](/theory/namespace-isolation)
- [Determinism proofs](/theory/determinism-proofs)
- [Parse don’t validate](/theory/parse-dont-validate)

## Next steps

1. Confirm behavior ordering in [Evaluation model](/learn/evaluation-model).
2. Add runtime loading via [Configuration lifecycle](/runtime/lifecycle).
3. Add boundary tests with [Serialization reference](/serialization/reference).
