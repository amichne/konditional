# Why typed flags

Use this page to understand the structural reason Konditional prevents common
feature-flag failures.

## What you will achieve

You will:

- understand why typed property-based flags are safer than string keys;
- see where compile-time guarantees end and runtime boundaries begin;
- map these guarantees to practical production outcomes.

## Prerequisites

You need basic Kotlin generics and property-delegation familiarity.

## Main content

Konditional models a feature as a typed Kotlin property on a `Namespace`.
This gives the compiler control over key identity and value type propagation.

```kotlin
object AppFeatures : Namespace("app") {
    val darkMode by boolean<Context>(default = false)
    val maxRetries by integer<Context>(default = 3)
}
```

At call sites, type flow is fixed:

```kotlin
val enabled: Boolean = AppFeatures.darkMode.evaluate(ctx)
val retries: Int = AppFeatures.maxRetries.evaluate(ctx)
```

## What this prevents

- string-key typos that silently miss remote configuration;
- unsafe runtime coercion from mismatched payload types;
- ad hoc evaluation behavior split across services;
- non-deterministic rollout behavior caused by custom hashing logic.

## Boundary discipline

Type safety in Kotlin source does not automatically validate remote JSON.
Konditional enforces a parse boundary:

- decode untrusted input at the boundary;
- return `Result.failure(KonditionalBoundaryFailure(ParseError))` on failure;
- keep last-known-good runtime state when parsing fails.

For details, see
[Parse don't validate](/theory/parse-dont-validate).

## Limits and non-goals

Typed flags do not replace:

- business-level correctness testing;
- organizational rollout governance;
- runtime SLO and incident response practices.

Typed flags are a foundation, not a full operations policy.

## Next steps

- [First success map](/overview/first-success-map)
- [Quickstart](/quickstart/)
- [Type safety boundaries](/theory/type-safety-boundaries)
