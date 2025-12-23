# Loading from JSON

Konditional treats JSON as a **trust boundary**: compile-time guarantees apply to statically-defined flags, while runtime JSON is validated before affecting evaluation.

---

## The Two-Phase Lifecycle

### Phase 1: Define Flags (Compile-Time)

```kotlin
object AppFeatures : Namespace("app") {
    val darkMode by boolean<Context>(default = false)
    val apiEndpoint by string<Context>(default = "https://api.example.com")
}
```

At this point, features are registered in the namespace registry.

### Phase 2: Load Configuration (Runtime)

```kotlin
val json = File("flags.json").readText()

when (val result = AppFeatures.fromJson(json)) {
    is ParseResult.Success -> {
        // Configuration loaded successfully
        println("Config loaded")
    }
    is ParseResult.Failure -> {
        // Invalid JSON rejected, last-known-good config remains active
        logError("Parse failed: ${result.error.message}")
    }
}
```

---

## The ParseResult Boundary

`ParseResult` is an explicit success/failure type that forces error handling:

```kotlin
sealed interface ParseResult<out T> {
    data class Success<T>(val value: T) : ParseResult<T>
    data class Failure(val error: ParseError) : ParseResult<Nothing>
}
```

**Guarantees:**

- `ParseResult.Success` means the JSON is valid and type-correct
- `ParseResult.Failure` means the payload is rejected **before** it can affect evaluation
- No exceptions are thrown — you must explicitly handle both cases

---

## Prerequisite: Features Must Be Registered

Deserialization requires that your `Namespace` objects have been initialized (so features are registered) **before** calling `fromJson(...)`.

```kotlin
// Ensure namespace is initialized at startup (t0)
val _ = AppFeatures  // Reference forces class initialization

// Later, at runtime...
when (val result = AppFeatures.fromJson(json)) {
    is ParseResult.Success -> Unit
    is ParseResult.Failure -> handleError(result.error)
}
```

If JSON references a feature that hasn't been registered, deserialization fails with `ParseError.FeatureNotFound`.

---

## Atomic Configuration Updates

`Namespace.load(configuration)` replaces the active snapshot atomically:

```kotlin
// Thread 1: Update configuration
when (val result = SnapshotSerializer.fromJson(json)) {
    is ParseResult.Success -> AppFeatures.load(result.value)
    is ParseResult.Failure -> logError(result.error.message)
}

// Thread 2: Concurrent evaluation
val enabled = AppFeatures.darkMode.evaluate(context)  // Sees old OR new, never mixed
```

**How it works:**

- Registry stores configuration in an `AtomicReference`
- `load(...)` performs a single atomic swap
- Readers see either the old snapshot or the new snapshot — never a partial state

---

## Exporting Configuration

```kotlin
val json = AppFeatures.toJson()
File("flags.json").writeText(json)
```

Use this to externalize your current configuration state for storage or transport.

---

## Incremental Updates via Patches

```kotlin
val currentConfig = AppFeatures.configuration
when (val result = SnapshotSerializer.applyPatchJson(currentConfig, patchJson)) {
    is ParseResult.Success -> AppFeatures.load(result.value)
    is ParseResult.Failure -> logError(result.error.message)
}
```

Patches allow you to send incremental updates (add/modify/remove flags) instead of shipping the full snapshot every time.

---

## Error Handling Best Practices

```kotlin
fun loadRemoteConfig(json: String) {
    when (val result = AppFeatures.fromJson(json)) {
        is ParseResult.Success -> {
            // Success: new config is active
            logger.info("Config updated successfully")
        }
        is ParseResult.Failure -> {
            // Failure: last-known-good config remains active
            logger.error("Config parse failed: ${result.error.message}")
            metrics.increment("config.parse.failure")
            // Optionally: alert on-call, retry later
        }
    }
}
```

**Operational approach:**

- Keep last-known-good configuration active on parse failures
- Log/alert when parse fails (don't fail silently)
- Retry on the next update cycle
- Never crash or corrupt evaluation due to bad JSON

---

## What's Validated (and What's Not)

**Validated at the parse boundary:**

- JSON syntax validity
- Schema/structure validity
- Value type checking against declared feature types
- Feature key existence (must be registered)

**Not validated by the type system:**

- Semantic correctness (e.g., whether 50% is the intended ramp-up)
- Business correctness (e.g., whether the targeted segment is correct)

---

## Next Steps

- [Fundamentals: Configuration Lifecycle](/fundamentals/configuration-lifecycle) — Deep dive into JSON → ParseResult → load
- [Fundamentals: Refresh Safety](/fundamentals/refresh-safety) — Why hot-reload is safe
- [API Reference: Serialization](/api-reference/serialization) — Full API for snapshot/patch operations
- [Persistence Format](/persistence-format) — JSON schema reference
