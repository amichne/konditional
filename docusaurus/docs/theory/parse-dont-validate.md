# Parse Don't Validate

Why `ParseResult` prevents invalid states from existing in the system.

---

## The Problem with Validation

Traditional validation checks data and returns a boolean or throws an exception:

```kotlin
// ✗ Validation approach
fun validateConfig(json: String): Boolean {
    return json.contains("flags") && json.contains("key")
}

val json = fetchConfig()
if (validateConfig(json)) {
    // Still working with untyped String
    // No guarantee it's actually valid
    // This will error
    applyConfig(json)
}
```

**Issues:**
1. Validated data remains in its original (untyped) form
2. No compile-time guarantee that validated data is used correctly
3. Validation checks can be bypassed or forgotten
4. Invalid states can still be constructed

---

## Parse Don't Validate Principle

**Parse** means: transform untrusted input into a typed representation, failing early if impossible.

```kotlin
// ✓ Parse approach
sealed interface ParseResult<out T> {
    data class Success<T>(val value: T) : ParseResult<T>
    data class Failure(val error: ParseError) : ParseResult<Nothing>
}

fun parseConfig(json: String): ParseResult<Configuration> {
    // Either return Success(validConfig) or Failure(error)
    // No middle ground
}
```

**Benefits:**
1. **Type-states**: Success contains valid `Configuration`, Failure contains `ParseError`
2. **Exhaustive handling**: When-expression forces you to handle both cases
3. **No invalid states**: If you have a `Configuration`, it's guaranteed valid
4. **No silent failures**: Parse failures are explicit (not exceptions)

---

## How Konditional Applies This

### The Trust Boundary

JSON enters the system as an untrusted `String`. Konditional parses it into a trusted `Configuration`:

```kotlin
val json: String = fetchRemoteConfig()  // Untrusted

val _ = AppFeatures // ensure features are registered before parsing
when (val result = ConfigurationSnapshotCodec.decode(json)) {
    is ParseResult.Success -> {
        val config: Configuration = result.value  // Trusted
        AppFeatures.load(config)
    }
    is ParseResult.Failure -> {
        // Invalid JSON rejected
        logError(result.error.message)
    }
}
```

**Key insight:** If you have a `Configuration` instance, it has already been validated. You can't construct an invalid `Configuration` because the parser is the only way to create one from JSON.

---

## Mechanism: Sealed Interface Guarantees

`ParseResult` is a sealed interface with exactly two subtypes:

```kotlin
sealed interface ParseResult<out T> {
    data class Success<T>(val value: T) : ParseResult<T>
    data class Failure(val error: ParseError) : ParseResult<Nothing>
}
```

**Compiler enforcement:**

```kotlin
when (val result = ConfigurationSnapshotCodec.decode(json)) {
    is ParseResult.Success -> { /* handle success */ }
    is ParseResult.Failure -> { /* handle failure */ }
    // No other cases possible
    // When-expression is exhaustive (compiler verified)
}
```

If you forget to handle a case, the code won't compile.

---

## No Exceptions Cross the Boundary

Traditional parsing throws exceptions:

```kotlin
// ✗ Exception-based parsing
try {
    val config = JSON.parse(json)  // Might throw
    applyConfig(config)
} catch (e: Exception) {
    // Easy to forget this
    logError(e)
}
```

**Issues:**
- Exceptions are invisible in type signatures
- Easy to forget exception handling
- Exceptions can propagate and crash the application

Konditional uses `ParseResult` instead:

```kotlin
// ✓ Explicit boundary
when (val result = ConfigurationSnapshotCodec.decode(json)) {
    is ParseResult.Success -> applyConfig(result.value)
    is ParseResult.Failure -> logError(result.error.message)
}
```

**Benefits:**
- Parse failures are explicit in the return type
- Compiler forces you to handle both cases
- No hidden control flow (no exceptions)

---

## Invalid States Are Unrepresentable

Because `Configuration` can only be created via parsing (not public constructors), invalid configurations cannot exist:

```kotlin
// ✗ Can't construct invalid Configuration
// val config = Configuration(flags = emptyMap())  // Private constructor

// ✓ Must go through parser
when (val result = ConfigurationSnapshotCodec.decode(json)) {
    is ParseResult.Success -> {
        // config is guaranteed valid
        val config = result.value
    }
}
```

**Property:** If a `Configuration` exists in the system, it has been validated.

---

## Comparison: Validation vs Parsing

### Validation (Traditional)

```kotlin
fun validateJson(json: String): Boolean {
    // Check if valid
    return json.contains("flags")
}

val json = fetchConfig()
if (validateJson(json)) {
    // json is still a String
    // No compile-time guarantee it's used correctly
    processConfig(json)
}
```

**Problems:**
- `json` remains untyped after validation
- Caller can bypass validation
- Invalid states can be constructed

### Parsing (Konditional)

```kotlin
fun parseJson(json: String): ParseResult<Configuration> {
    // Transform to typed representation or fail
}

when (val result = parseJson(json)) {
    is ParseResult.Success -> {
        // result.value is a typed, valid Configuration
        processConfig(result.value)
    }
    is ParseResult.Failure -> {
        // Explicit failure handling
    }
}
```

**Benefits:**
- `Configuration` is typed and guaranteed valid
- Caller must handle both success and failure (exhaustive when)
- Invalid configurations cannot be constructed

---

## Why This Matters for Production Safety

### Traditional Approach: Silent Failures

```kotlin
val json = """{ "invalid": true }"""

if (validateJson(json)) {
    applyConfig(json)  // Might crash later
}
// Validation passed but config is invalid
// System continues with corrupt state
```

### Konditional Approach: Fail-Safe

```kotlin
val json = """{ "invalid": true }"""

when (val result = ConfigurationSnapshotCodec.decode(json)) {
    is ParseResult.Success -> {
        // Unreachable: JSON is invalid
    }
    is ParseResult.Failure -> {
        // Invalid JSON rejected
        // Last-known-good config remains active
        logError(result.error.message)
    }
}
```

**Operational guarantee:** Invalid JSON cannot become active configuration.

---

## The Guarantee

**If you have a `Configuration` instance, it is valid.**

No need to:
- Re-validate before using it
- Check for null/undefined fields
- Guard against type mismatches

The parser did all that work upfront.

---

## Next Steps

- [Fundamentals: Trust Boundaries](/fundamentals/trust-boundaries) — Compile-time vs runtime guarantees
- [Fundamentals: Configuration Lifecycle](/fundamentals/configuration-lifecycle) — JSON → ParseResult → load
- [API Reference: Serialization](/api-reference/serialization) — ParseResult API details
