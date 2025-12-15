# Context: Konditional Public API

You are documenting the public-facing API of Konditional, a type-safe feature flag library for Kotlin. Your audience is developers who know Kotlin but are new to the library.

## Scope

Focus exclusively on:

- **Feature definition DSL**: `by boolean()`, `by string()`, `by int()`, `by double()`, `by jsonObject<T>()`
- **FeatureContainer**: Inheritance pattern, namespace binding, property delegation
- **Rule builder syntax**: `rule { }`, `platforms()`, `locales()`, `versions { min(), max() }`, `rollout { }`
- **Context interface**: Required fields, custom context extension via data classes
- **Evaluation entry points**: `feature { }` function and its usage patterns
- **Namespace organization**: `Namespace.Global`, custom namespaces, isolation benefits

## Out of Scope (defer to other domains)

- Internal rule matching algorithms → See `02-internal-semantics.md`
- Bucketing implementation details → See `02-internal-semantics.md`
- Thread-safety mechanisms → See `04-reliability-guarantees.md`
- JSON serialization internals → See `05-configuration-integrity.md`
- Type system guarantees (reference only by name) → See `03-type-safety-theory.md`

## Audience

Developers who:
- Are evaluating Konditional for their project
- Are onboarding to a codebase that uses Konditional
- Need to define new features or modify existing ones
- Want to understand the API without reading source code

## Key Concepts

| Concept | API Surface |
|---------|-------------|
| Define a boolean feature | `val flag by boolean(default = false) { rules }` |
| Define a typed feature | `val config by jsonObject<MyType>(default = ...) { rules }` |
| Add platform targeting | `rule { platforms(Platform.IOS, Platform.ANDROID) } returns value` |
| Add version constraints | `rule { versions { min(2, 0, 0); max(4, 0, 0) } } returns value` |
| Add percentage rollout | `rule { rollout { 25.0 } } returns value` |
| Evaluate a feature | `val result = feature { MyFeatures.flagName }` |
| Create custom context | `data class MyContext(...) : Context` |

## Constraints

- Examples must be copy-paste runnable (assume imports are present)
- Show the simplest example first, then add complexity
- Use realistic feature names (not `FOO`, `BAR`)
- Document happy path first, edge cases second
- Prefer showing patterns over exhaustive API enumeration

## Example Quality Standards

**Good example** (minimal, realistic, demonstrates one concept):
```kotlin
object AppFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val darkMode by boolean(default = false)
}

val isDark: Boolean = feature { AppFeatures.darkMode }
```

**Bad example** (too abstract, unrealistic names):
```kotlin
object Foo : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val BAR by boolean(default = true)
}
```

## Context Injection Point

When documenting specific APIs, inject relevant type signatures here:

```
[INSERT: Core type signatures from context/core-types.kt if needed]
```

## Output Format

For API documentation, produce:
1. Brief conceptual introduction (2-3 sentences)
2. Code example showing basic usage
3. Explanation of what the code does
4. Variations or options (if applicable)
5. Common patterns or idioms

Avoid excessive headers within a single concept. Use prose over bullet points.
