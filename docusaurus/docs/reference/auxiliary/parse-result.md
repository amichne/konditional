---
title: ParseResult API
---

# ParseResult API

## What It Does

`ParseResult<T>` provides an explicit boundary for JSON parsing and validation. It returns either `Success(value)` or `Failure(error)`, forcing callers to handle parse failures before configuration becomes active.

## ParseResult Type

### Signature

```kotlin
sealed interface ParseResult<out T> {
    data class Success<T>(val value: T) : ParseResult<T>
    data class Failure(val error: ParseError) : ParseResult<Nothing>
}
```

**Evidence**: `konditional-core/src/main/kotlin/io/amichne/konditional/core/result/ParseResult.kt:15`

### Semantics

**Guarantee**: Invalid input never produces a `Success` value.

**Mechanism**: Explicit result type with sealed hierarchy forces pattern matching.

**Boundary**: Semantic correctness (e.g., "is 10% the right percentage?") is not validated.

---

## fold()

Transform `ParseResult` by providing success and failure handlers.

### Signature

```kotlin
inline fun <T, R> ParseResult<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (ParseError) -> R
): R
```

**Evidence**: `konditional-core/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:19`

### Parameters

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `onSuccess` | `(T) -> R` | Yes | Function to apply if result is `Success` |
| `onFailure` | `(ParseError) -> R` | Yes | Function to apply if result is `Failure` |

### Return Value

Returns `R` — result of applying the appropriate function.

### Examples

```kotlin
val message: String = result.fold(
    onSuccess = { config -> "Loaded ${config.flags.size} features" },
    onFailure = { error -> "Parse failed: ${error.message}" }
)
```

---

## map()

Transform the success value, preserving failure.

### Signature

```kotlin
inline fun <T, R> ParseResult<T>.map(transform: (T) -> R): ParseResult<R>
```

**Evidence**: `konditional-core/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:33`

### Parameters

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `transform` | `(T) -> R` | Yes | Function to transform success value |

### Return Value

Returns `ParseResult<R>`:
- `Success(transform(value))` if original was success
- `Failure(error)` if original was failure (unchanged)

### Examples

```kotlin
val versionResult: ParseResult<String> = configResult.map { config ->
    config.metadata.version ?: "unknown"
}
```

---

## flatMap()

Chain operations that return `ParseResult`.

### Signature

```kotlin
inline fun <T, R> ParseResult<T>.flatMap(
    transform: (T) -> ParseResult<R>
): ParseResult<R>
```

**Evidence**: `konditional-core/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:46`

### Parameters

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `transform` | `(T) -> ParseResult<R>` | Yes | Function that returns a new `ParseResult` |

### Return Value

Returns `ParseResult<R>`:
- Result of `transform(value)` if original was success
- `Failure(error)` if original was failure (short-circuits)

### Examples

```kotlin
val loadResult: ParseResult<Unit> = ConfigurationSnapshotCodec.decode(json)
    .flatMap { config ->
        try {
            namespace.load(config)
            ParseResult.Success(Unit)
        } catch (e: Exception) {
            ParseResult.Failure(ParseError.Custom(e.message ?: "Load failed"))
        }
    }
```

---

## getOrNull()

Extract value or return null.

### Signature

```kotlin
fun <T> ParseResult<T>.getOrNull(): T?
```

**Evidence**: `konditional-core/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:55`

### Return Value

Returns `T?`:
- `value` if success
- `null` if failure

### Examples

```kotlin
val config: Configuration? = result.getOrNull()
if (config != null) {
    namespace.load(config)
}
```

---

## getOrDefault()

Extract value or return default.

### Signature

```kotlin
fun <T> ParseResult<T>.getOrDefault(default: T): T
```

**Evidence**: `konditional-core/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:63`

### Parameters

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `default` | `T` | Yes | Value to return if result is failure |

### Return Value

Returns `T` — success value or provided default.

### Examples

```kotlin
val config: Configuration = result.getOrDefault(fallbackConfig)
namespace.load(config)
```

---

## getOrElse()

Extract value or compute alternative.

### Signature

```kotlin
inline fun <T> ParseResult<T>.getOrElse(onFailure: (ParseError) -> T): T
```

**Evidence**: `konditional-core/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:71`

### Parameters

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `onFailure` | `(ParseError) -> T` | Yes | Function to compute fallback value from error |

### Return Value

Returns `T` — success value or computed fallback.

### Examples

```kotlin
val config: Configuration = result.getOrElse { error ->
    logger.error("Parse failed: ${error.message}")
    loadLastKnownGoodConfig()
}
```

---

## isSuccess() / isFailure()

Check result status.

### Signatures

```kotlin
fun <T> ParseResult<T>.isSuccess(): Boolean
fun <T> ParseResult<T>.isFailure(): Boolean
```

**Evidence**: `konditional-core/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:79,84`

### Return Value

Returns `Boolean`:
- `isSuccess()`: `true` if `Success`, `false` if `Failure`
- `isFailure()`: `true` if `Failure`, `false` if `Success`

### Examples

```kotlin
if (result.isSuccess()) {
    metrics.increment("config.parse.success")
} else {
    metrics.increment("config.parse.failure")
}
```

---

## onSuccess() / onFailure()

Side effects based on result status.

### Signatures

```kotlin
inline fun <T> ParseResult<T>.onSuccess(action: (T) -> Unit): ParseResult<T>
inline fun <T> ParseResult<T>.onFailure(action: (ParseError) -> Unit): ParseResult<T>
```

**Evidence**: `konditional-core/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:96,113`

### Parameters

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `action` | `(T) -> Unit` or `(ParseError) -> Unit` | Yes | Side effect to perform |

### Return Value

Returns original `ParseResult<T>` (unchanged, enables chaining).

### Examples

```kotlin
ConfigurationSnapshotCodec.decode(json)
    .onSuccess { config -> logger.info("Parsed ${config.flags.size} features") }
    .onFailure { error -> logger.error("Parse failed: ${error.message}") }
    .fold(
        onSuccess = { namespace.load(it) },
        onFailure = { /* keep last-known-good */ }
    )
```

---

## recover()

Convert failure to success by providing fallback.

### Signature

```kotlin
inline fun <T> ParseResult<T>.recover(transform: (ParseError) -> T): T
```

**Evidence**: `konditional-core/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:130`

### Parameters

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `transform` | `(ParseError) -> T` | Yes | Function to produce fallback value from error |

### Return Value

Returns `T` — success value or transformed failure.

### Examples

```kotlin
val config: Configuration = result.recover { error ->
    logger.warn("Using fallback config due to: ${error.message}")
    buildFallbackConfig()
}
```

---

## toResult()

Convert to Kotlin `Result<T>`.

### Signature

```kotlin
fun <T> ParseResult<T>.toResult(): Result<T>
```

**Evidence**: `konditional-core/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:139`

### Return Value

Returns `Result<T>`:
- `Result.success(value)` if original was success
- `Result.failure(ParseException(error))` if original was failure

### Examples

```kotlin
val kotlinResult: Result<Configuration> = parseResult.toResult()
kotlinResult
    .onSuccess { config -> namespace.load(config) }
    .onFailure { exception -> logger.error("Failed", exception) }
```

---

## Pattern: Complete Error Handling

Recommended pattern for production use:

```kotlin
fun loadConfig(json: String) {
    when (val result = ConfigurationSnapshotCodec.decode(json)) {
        is ParseResult.Success -> {
            AppFeatures.load(result.value)
            logger.info("Config loaded: v${result.value.metadata.version}")
            metrics.increment("config.load.success")
        }
        is ParseResult.Failure -> {
            val errorMsg = when (val error = result.error) {
                is ParseError.InvalidJson -> "Invalid JSON syntax: ${error.message}"
                is ParseError.FeatureNotFound -> "Unknown feature: ${error.featureId}"
                is ParseError.TypeMismatch -> "Type mismatch: ${error.message}"
                is ParseError.SchemaViolation -> "Schema violation: ${error.message}"
                else -> "Parse error: ${error.message}"
            }

            logger.error(errorMsg)
            metrics.increment("config.load.failure", tags = mapOf(
                "error_type" to error::class.simpleName
            ))
            alertOps("Configuration rejected", error)

            // Last-known-good remains active
        }
    }
}
```

---

## Semantics / Notes

- **No exceptions**: All validation errors return `Failure`, never throw (except for programmer errors)
- **Immutability**: All operations return new results, original unchanged
- **Chaining**: Most operations return `ParseResult<T>` to enable chaining
- **Short-circuiting**: Operations like `flatMap` and `onSuccess` short-circuit on failure

---

## Compatibility

- **Introduced**: v0.1.0
- **Deprecated**: None
- **Alternatives**: Kotlin `Result<T>` (can convert via `toResult()`)

---

## Related

- [Guide: Load Remote Config](/reference/auxiliary/snapshot-loader) — Using ParseResult in practice
- [Reference: Namespace Operations](/reference/auxiliary/namespace-operations) — load() requires validated config
- [Design Theory: Parse Don't Validate](/theory/parse-dont-validate) — Why ParseResult exists
- [Production Operations: Failure Modes](/troubleshooting) — Parse error scenarios
- [Learn: Type Safety](/theory/type-safety-boundaries) — Runtime validation boundary
