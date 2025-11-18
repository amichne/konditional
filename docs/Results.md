# Results: Type-Safe Error Handling

Konditional provides two result types for error handling: `EvaluationResult` for flag evaluation and `ParseResult` for parsing operations. Both follow the "Parse, Don't Validate" principle, making all failure modes explicit and type-safe.

---

## Philosophy: Parse, Don't Validate

Traditional feature flag libraries often use nullable returns or throw exceptions:

```kotlin
// Nullable approach - loses error information
val value: Boolean? = flags.get("my-flag")  // Why is it null?

// Exception approach - forces try-catch everywhere
try {
    val value = flags.get("my-flag")
} catch (e: Exception) {
    // What kind of error? Flag missing? Evaluation failed?
}
```

**Konditional's approach**: Explicit result types that distinguish error cases:

```kotlin
// Type-safe - all outcomes are explicit
when (val result = context.evaluateSafe(MY_FLAG)) {
    is EvaluationResult.Success -> use(result.value)
    is EvaluationResult.FlagNotFound -> handleMissing(result.key)
    is EvaluationResult.EvaluationError -> handleError(result.key, result.error)
}
```

**Benefits**:
- No surprises - all failure modes are documented in the type
- Precise error handling - distinguish between different error cases
- Compiler-enforced - can't forget to handle errors
- Composable - transform results with `map`, `fold`, `flatMap`

---

## EvaluationResult: Flag Evaluation Outcomes

`EvaluationResult<T>` represents the outcome of evaluating a feature flag.

### Structure

```kotlin
sealed interface EvaluationResult<out S> {
    data class Success<S>(val value: S) : EvaluationResult<S>
    data class FlagNotFound(val key: String) : EvaluationResult<Nothing>
    data class EvaluationError(val key: String, val error: Throwable) : EvaluationResult<Nothing>
}
```

### Variants

#### Success

Flag was found and evaluated successfully:

```kotlin
val result: EvaluationResult<Boolean> = context.evaluateSafe(AppFeatures.DARK_MODE)

when (result) {
    is EvaluationResult.Success -> {
        val enabled: Boolean = result.value
        applyDarkMode(enabled)
    }
    // ...
}
```

#### FlagNotFound

Flag is not registered in the namespace:

```kotlin
when (result) {
    is EvaluationResult.FlagNotFound -> {
        val flagKey: String = result.key
        logger.warn("Flag not registered: $flagKey")
        // Use default behavior
    }
    // ...
}
```

This typically indicates:
- Flag hasn't been configured yet
- Wrong namespace was used
- Typo in feature key

#### EvaluationError

Flag exists but evaluation threw an exception:

```kotlin
when (result) {
    is EvaluationResult.EvaluationError -> {
        val flagKey: String = result.key
        val error: Throwable = result.error
        logger.error("Flag evaluation failed: $flagKey", error)
        // Use fallback or fail
    }
    // ...
}
```

This is rare but can occur if:
- Custom `Evaluable` throws an exception
- Serialization/deserialization fails for complex types

---

## EvaluationResult Utility Methods

Transform and extract values from evaluation results:

### fold()

Transform result into any type by providing handlers for each case:

```kotlin
val outcome: Outcome<MyError, Boolean> = context.evaluateSafe(MY_FLAG).fold(
    onSuccess = { Outcome.Success(it) },
    onFlagNotFound = { key -> Outcome.Failure(MyError.FlagNotRegistered(key)) },
    onEvaluationError = { key, error -> Outcome.Failure(MyError.EvaluationFailed(key, error)) }
)
```

**Use when**: Adapting to your application's error handling system (Result, Either, Outcome, etc.)

### map()

Transform the success value while preserving errors:

```kotlin
val result: EvaluationResult<Int> = context.evaluateSafe(MyFeatures.MAX_RETRIES)
    .map { it * 2 }  // Double the retry count

// Chain multiple transformations
val upperCase: EvaluationResult<String> = context.evaluateSafe(MyFeatures.API_ENDPOINT)
    .map { it.trim() }
    .map { it.uppercase() }
```

**Use when**: Transforming success values without changing error handling

### getOrNull()

Get the value if successful, null otherwise:

```kotlin
val value: Boolean? = context.evaluateSafe(AppFeatures.DARK_MODE).getOrNull()

if (value != null) {
    applyDarkMode(value)
}
```

**Use when**: You don't need to distinguish error types and null is acceptable

### getOrDefault()

Get the value if successful, or a default value if failed:

```kotlin
val maxRetries: Int = context.evaluateSafe(MyFeatures.MAX_RETRIES)
    .getOrDefault(default = 3)
```

**Use when**: You have a sensible fallback value

### getOrElse()

Get the value if successful, or compute a default based on the error:

```kotlin
val endpoint: String = context.evaluateSafe(MyFeatures.API_ENDPOINT)
    .getOrElse { error ->
        when (error) {
            is EvaluationResult.FlagNotFound -> "https://api.prod.example.com"
            is EvaluationResult.EvaluationError -> "https://api.backup.example.com"
        }
    }
```

**Use when**: You need error-specific fallback logic

### isSuccess() / isFailure()

Check the result status:

```kotlin
val result = context.evaluateSafe(MY_FLAG)

if (result.isSuccess()) {
    logger.info("Flag evaluated successfully")
}

if (result.isFailure()) {
    logger.warn("Flag evaluation failed")
}
```

**Use when**: You need boolean checks without extracting values

### toResult()

Convert to Kotlin's `Result` type:

```kotlin
val result: Result<Boolean> = context.evaluateSafe(MY_FLAG).toResult()

result
    .onSuccess { value -> logger.info("Value: $value") }
    .onFailure { exception -> logger.error("Failed", exception) }
```

**Note**: Both `FlagNotFound` and `EvaluationError` become `Result.failure`. Use `fold()` if you need to distinguish them.

**Use when**: Integrating with APIs that expect Kotlin's `Result` type

---

## Evaluation APIs

Konditional provides multiple evaluation methods for different error handling needs:

### evaluateSafe() - Recommended

Returns `EvaluationResult` with explicit error cases:

```kotlin
val result: EvaluationResult<Boolean> = context.evaluateSafe(AppFeatures.DARK_MODE)

when (result) {
    is EvaluationResult.Success -> applyDarkMode(result.value)
    is EvaluationResult.FlagNotFound -> logger.warn("Flag not found: ${result.key}")
    is EvaluationResult.EvaluationError -> logger.error("Evaluation failed", result.error)
}
```

**Use when**: You need precise error handling (recommended default)

### evaluateOrNull()

Returns `T?` - value on success, null on any failure:

```kotlin
val darkMode: Boolean? = context.evaluateOrNull(AppFeatures.DARK_MODE)

darkMode?.let { applyDarkMode(it) }
```

**Use when**: You don't need error details and null is acceptable

### evaluateOrDefault()

Returns `T` - value on success, default on any failure:

```kotlin
val maxRetries: Int = context.evaluateOrDefault(
    MyFeatures.MAX_RETRIES,
    default = 3
)
```

**Use when**: You have a sensible fallback and don't need error details

### evaluateOrThrow()

Returns `T` - value on success, throws exception on failure:

```kotlin
try {
    val apiKey: String = context.evaluateOrThrow(MyFeatures.API_KEY)
    initializeApi(apiKey)
} catch (e: FlagNotFoundException) {
    logger.error("Critical flag missing: ${e.key}")
    throw e
} catch (e: FlagEvaluationException) {
    logger.error("Evaluation failed: ${e.key}", e)
    throw e
}
```

**⚠️ Use sparingly**: Only when flag absence is truly exceptional (programmer error)

---

## ParseResult: Parsing Outcomes

`ParseResult<T>` represents the outcome of parsing operations (versions, IDs, JSON, etc.).

### Structure

```kotlin
sealed interface ParseResult<out T> {
    data class Success<T>(val value: T) : ParseResult<T>
    data class Failure(val error: ParseError) : ParseResult<Nothing>
}
```

### Variants

#### Success

Parsing succeeded:

```kotlin
val result: ParseResult<Version> = Version.parse("2.1.0")

when (result) {
    is ParseResult.Success -> {
        val version: Version = result.value
        println("Parsed: $version")
    }
    // ...
}
```

#### Failure

Parsing failed with structured error:

```kotlin
when (result) {
    is ParseResult.Failure -> {
        val error: ParseError = result.error
        logger.error("Parse failed: ${error.message}")
    }
    // ...
}
```

---

## ParseResult Utility Methods

### fold()

Transform result into any type:

```kotlin
val outcome = Version.parse(input).fold(
    onSuccess = { Outcome.Success(it) },
    onFailure = { error -> Outcome.Failure(MyError.InvalidVersion(error.message)) }
)
```

### map()

Transform the success value:

```kotlin
val majorVersion: ParseResult<Int> = Version.parse("2.1.0")
    .map { it.major }  // Extract major version number
```

### flatMap()

Chain dependent parsing operations:

```kotlin
val result: ParseResult<Configuration> = StableId.parse(idInput)
    .flatMap { id ->
        Version.parse(versionInput).map { version ->
            Configuration(id, version)
        }
    }
```

**Use when**: Second parse depends on first parse's result

### getOrNull()

Get value or null:

```kotlin
val version: Version? = Version.parse("2.1.0").getOrNull()
```

### getOrDefault()

Get value or default:

```kotlin
val version: Version = Version.parse(input).getOrDefault(Version(1, 0, 0))
```

### getOrElse()

Get value or compute default from error:

```kotlin
val version: Version = Version.parse(input).getOrElse { error ->
    logger.warn("Invalid version: ${error.message}")
    Version(1, 0, 0)
}
```

### isSuccess() / isFailure()

Check result status:

```kotlin
if (Version.parse(input).isSuccess()) {
    println("Valid version")
}
```

### onSuccess() / onFailure()

Execute side effects without changing the result:

```kotlin
val version = Version.parse(input)
    .onSuccess { v -> logger.info("Parsed version: $v") }
    .onFailure { e -> logger.warn("Parse failed: ${e.message}") }
    .getOrDefault(Version(1, 0, 0))
```

**Use when**: Logging or metrics without transforming the result

### recover()

Recover from failure by providing a fallback value:

```kotlin
val version: Version = Version.parse(input)
    .onFailure { error -> logger.error("Invalid version: ${error.message}") }
    .recover { Version(1, 0, 0) }
```

**Note**: Unlike `getOrElse`, this always returns a value (not a `ParseResult`)

### toResult()

Convert to Kotlin's `Result` type:

```kotlin
val result: Result<Version> = Version.parse(input).toResult()
```

### getOrThrow()

Get value or throw exception:

```kotlin
val version: Version = Version.parse("2.1.0").getOrThrow()
// Throws IllegalStateException if parsing fails
```

**Use when**: In tests or when you want fail-fast behavior

---

## ParseError: Structured Parse Failures

All parse errors implement `ParseError` with structured error information:

### InvalidHexId

Failed to parse hexadecimal identifier:

```kotlin
data class InvalidHexId(val input: String, val message: String) : ParseError
```

**Example**:
```kotlin
val result = StableId.parse("not-hex")
// Failure(InvalidHexId("not-hex", "Invalid hex format"))
```

### InvalidRollout

Rollout percentage outside valid range (0.0-100.0):

```kotlin
data class InvalidRollout(val value: Double, val message: String) : ParseError
```

**Example**:
```kotlin
val result = Rollout.of(150.0)
// Failure(InvalidRollout(150.0, "Rollout must be between 0 and 100"))
```

### InvalidVersion

Failed to parse semantic version:

```kotlin
data class InvalidVersion(val input: String, val message: String) : ParseError
```

**Example**:
```kotlin
val result = Version.parse("1.x.0")
// Failure(InvalidVersion("1.x.0", "Invalid version format"))
```

### FeatureNotFound

Feature key not found in registry:

```kotlin
data class FeatureNotFound(val key: String) : ParseError {
    val message: String get() = "Feature not found: $key"
}
```

**Example**:
```kotlin
val result = FeatureRegistry.get("unknown-feature")
// Failure(FeatureNotFound("unknown-feature"))
```

### FlagNotFound

Flag not found in registry:

```kotlin
data class FlagNotFound(val key: String) : ParseError {
    val message: String get() = "Flag not found: $key"
}
```

### InvalidSnapshot

Failed to deserialize JSON snapshot:

```kotlin
data class InvalidSnapshot(val reason: String) : ParseError {
    val message: String get() = "Invalid snapshot: $reason"
}
```

**Example**:
```kotlin
val result = SnapshotSerializer.fromJson("{invalid json}")
// Failure(InvalidSnapshot("Unexpected character..."))
```

### InvalidJson

Invalid JSON data:

```kotlin
data class InvalidJson(val reason: String) : ParseError {
    val message: String get() = "Invalid JSON: $reason"
}
```

---

## Exception Types

For APIs that throw exceptions instead of returning results:

### FlagNotFoundException

Thrown when a flag is not registered:

```kotlin
class FlagNotFoundException(val key: String) : NoSuchElementException("Flag not found: $key")
```

**Thrown by**: `context.evaluateOrThrow()` when flag doesn't exist

**Usage**:
```kotlin
try {
    val value = context.evaluateOrThrow(MY_FLAG)
} catch (e: FlagNotFoundException) {
    logger.error("Missing flag: ${e.key}")
}
```

### FlagEvaluationException

Thrown when flag evaluation fails:

```kotlin
class FlagEvaluationException(val key: String, cause: Throwable) :
    RuntimeException("Flag evaluation failed: $key", cause)
```

**Thrown by**: `context.evaluateOrThrow()` when evaluation throws

**Usage**:
```kotlin
try {
    val value = context.evaluateOrThrow(MY_FLAG)
} catch (e: FlagEvaluationException) {
    logger.error("Evaluation failed: ${e.key}", e.cause)
}
```

### ParseException

Wrapper for `ParseError` when using throwing APIs:

```kotlin
class ParseException(val error: ParseError) : Exception(error.message)
```

**Usage**:
```kotlin
val result = Version.parse(input).toResult()

result.onFailure { exception ->
    if (exception is ParseException) {
        when (val error = exception.error) {
            is ParseError.InvalidVersion -> handleInvalidVersion(error)
            else -> handleOtherError(error)
        }
    }
}
```

---

## Error Handling Patterns

### Pattern 1: Explicit Error Handling

**When**: You need precise control over each error case

```kotlin
when (val result = context.evaluateSafe(MY_FLAG)) {
    is EvaluationResult.Success -> {
        // Use the value
        processValue(result.value)
    }
    is EvaluationResult.FlagNotFound -> {
        // Flag not configured - use default
        logger.warn("Flag not found: ${result.key}, using default")
        processValue(defaultValue)
    }
    is EvaluationResult.EvaluationError -> {
        // Evaluation failed - log and use fallback
        logger.error("Evaluation failed: ${result.key}", result.error)
        processFallback()
    }
}
```

### Pattern 2: Simple Default Fallback

**When**: You have a sensible default and don't need error details

```kotlin
val enabled = context.evaluateOrDefault(AppFeatures.NEW_UI, default = false)

if (enabled) {
    showNewUI()
} else {
    showLegacyUI()
}
```

### Pattern 3: Nullable with Fallback Logic

**When**: You want optional behavior based on flag presence

```kotlin
val customEndpoint: String? = context.evaluateOrNull(MyFeatures.CUSTOM_ENDPOINT)

val endpoint = customEndpoint ?: defaultEndpoint

httpClient.configure { baseUrl = endpoint }
```

### Pattern 4: Transformation Chains

**When**: Processing flag values through multiple steps

```kotlin
val config = context.evaluateSafe(MyFeatures.CONFIG_JSON)
    .map { parseConfig(it) }
    .map { validateConfig(it) }
    .map { enrichConfig(it) }
    .getOrElse { error ->
        logger.warn("Config loading failed: $error")
        defaultConfig
    }
```

### Pattern 5: Fail-Fast in Critical Paths

**When**: Flag absence indicates programmer error

```kotlin
// During application initialization
fun initializeApp() {
    val apiKey = context.evaluateOrThrow(CriticalConfig.API_KEY)
    val dbUrl = context.evaluateOrThrow(CriticalConfig.DATABASE_URL)

    // If we get here, all critical config exists
    initializeServices(apiKey, dbUrl)
}
```

### Pattern 6: Parsing with Recovery

**When**: Parsing user input with graceful degradation

```kotlin
val version = Version.parse(userInput)
    .onFailure { e -> logger.warn("Invalid version: ${e.message}") }
    .getOrDefault(Version(1, 0, 0))
```

---

## When to Use Which Result Type

### Use EvaluationResult when:

- Evaluating feature flags
- You need to distinguish between "flag not found" and "evaluation failed"
- Building monitoring/metrics around flag evaluation
- You want explicit, type-safe error handling

### Use ParseResult when:

- Parsing version strings, IDs, rollout percentages
- Deserializing JSON configurations
- Loading snapshots from external sources
- You need structured error information for user feedback

### Use evaluateOrDefault when:

- You have a sensible default value
- Error details aren't important for your use case
- You want the simplest possible API

### Use evaluateOrNull when:

- Null is semantically meaningful in your context
- You're working with nullable types already
- Error details aren't important

### Use evaluateOrThrow when:

- Flag absence is a programmer error (should never happen in production)
- During application initialization with critical configuration
- In test code where fail-fast is desired

---

## Summary

**EvaluationResult**:
- Three variants: `Success`, `FlagNotFound`, `EvaluationError`
- Utilities: `fold`, `map`, `getOrNull`, `getOrDefault`, `getOrElse`, `isSuccess`, `isFailure`, `toResult`
- Use for flag evaluation with precise error handling

**ParseResult**:
- Two variants: `Success`, `Failure`
- Utilities: `fold`, `map`, `flatMap`, `getOrNull`, `getOrDefault`, `getOrElse`, `isSuccess`, `isFailure`, `onSuccess`, `onFailure`, `recover`, `toResult`, `getOrThrow`
- Use for parsing operations with structured errors

**ParseError**:
- Seven variants: `InvalidHexId`, `InvalidRollout`, `InvalidVersion`, `FeatureNotFound`, `FlagNotFound`, `InvalidSnapshot`, `InvalidJson`
- All provide structured error information with descriptive messages

**Exception Types**:
- `FlagNotFoundException`: Flag not registered
- `FlagEvaluationException`: Evaluation threw an exception
- `ParseException`: Wrapper for `ParseError` in throwing APIs

**Core Principle**: Prefer result types (`evaluateSafe`, `ParseResult`) over exceptions for expected failure modes. Only use throwing APIs (`evaluateOrThrow`) when failure is truly exceptional.

---

## Next Steps

- **[Evaluation](Evaluation.md)**: Understand rule matching and specificity
- **[Features](Features.md)**: Define type-safe feature flags
- **[Serialization](Serialization.md)**: Export and import configurations
- **[Overview](index.md)**: Back to API overview
