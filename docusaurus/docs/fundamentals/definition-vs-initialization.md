# Definition vs Initialization

Konditional's lifecycle has two distinct phases: **definition** (compile-time property delegation) and **initialization** (runtime namespace registration). Understanding this distinction is critical for working with JSON-loaded configuration.

---

## Phase 1: Definition (Compile-Time)

When you write delegated properties, the Kotlin compiler processes them at compile-time:

```kotlin
object AppFeatures : Namespace("app") {
    val darkMode by boolean<Context>(default = false)  // Definition happens here
    val apiEndpoint by string<Context>(default = "https://api.example.com")
}
```

**What happens:**

- Compiler generates property accessors
- Type information is bound (`boolean` → `Feature<Boolean, Context, Namespace>`)
- Default values are type-checked
- Rule DSL is validated for type correctness

**Scope:** This is compile-time type checking. No runtime code has executed yet.

---

## Phase 2: Initialization (Runtime, t0)

When the `Namespace` class is first accessed, Kotlin's class initialization runs:

```kotlin
// First access triggers class initialization (t0)
val _ = AppFeatures  // or: val flag = AppFeatures.darkMode

// During initialization:
// 1. Namespace("app") constructor runs
// 2. Each delegated property initializes and registers its Feature
// 3. NamespaceRegistry records the feature definitions
```

**What happens at initialization:**

1. `Namespace` constructor creates/retrieves the `NamespaceRegistry` for the given ID
2. Each property delegate creates a `Feature` instance
3. `Feature` is registered in the namespace's registry
4. Registry records the `FlagDefinition` (default + rules + salt + active state)

**Critical invariant:** Features must be registered **before** JSON deserialization attempts to reference them.

---

## Why This Matters: The Precondition

JSON deserialization looks up features by key in the registry. If a feature hasn't been registered (because the namespace wasn't initialized), deserialization fails:

```kotlin
// ✗ Incorrect order
val json = fetchRemoteConfig()
when (val result = SnapshotSerializer.fromJson(json)) {
    // Fails with ParseError.FeatureNotFound if AppFeatures not initialized
    // This will error
    is ParseResult.Failure -> println(result.error)
}
```

```kotlin
// ✓ Correct order
val _ = AppFeatures  // Ensure initialization at startup (t0)

val json = fetchRemoteConfig()
when (val result = AppFeatures.fromJson(json)) {
    is ParseResult.Success -> Unit
    is ParseResult.Failure -> logError(result.error.message)
}
```

---

## Initialization Patterns

### Pattern 1: Explicit Reference at Startup

```kotlin
fun main() {
    // Force initialization of all namespaces
    val _ = AppFeatures
    val _ = PaymentFeatures
    val _ = AnalyticsFeatures

    // Now safe to deserialize JSON
    loadRemoteConfig()
}
```

### Pattern 2: Lazy Initialization (Use with Caution)

```kotlin
val appFeatures by lazy { AppFeatures }

fun loadConfig() {
    appFeatures  // Initializes on first access
    when (val result = AppFeatures.fromJson(json)) {
        is ParseResult.Success -> Unit
        is ParseResult.Failure -> logError(result.error.message)
    }
}
```

**Warning:** Ensure the lazy initialization completes **before** JSON deserialization.

### Pattern 3: Dependency Injection

```kotlin
@Singleton
class FeatureRegistry {
    init {
        // Initialize all namespaces at DI container startup
        AppFeatures
        PaymentFeatures
        AnalyticsFeatures
    }
}
```

---

## Definition vs Initialization: Summary

| Phase                | When           | What Happens                                      | Guarantees                                    |
|----------------------|----------------|---------------------------------------------------|-----------------------------------------------|
| **Definition**       | Compile-time   | Property delegation, type checking, code generation | Types are correct, rule DSL is valid          |
| **Initialization**   | Runtime (t0)   | Class initialization, feature registration        | Features exist in registry, ready for lookup  |

---

## The Two-Phase Contract

1. **Define** flags as properties (compile-time type safety)
2. **Initialize** namespaces at startup (runtime registration)
3. **Load** JSON configuration (runtime validation + atomic swap)

Violating this order (attempting to load JSON before initialization) results in `ParseError.FeatureNotFound`.

---

## Next Steps

- [Configuration Lifecycle](/fundamentals/configuration-lifecycle) — JSON → ParseResult → load → evaluation
- [Trust Boundaries](/fundamentals/trust-boundaries) — Compile-time vs runtime guarantees
- [Failure Modes](/fundamentals/failure-modes) — What happens when things go wrong
