# How-To: Handle Configuration Failures

## Problem

You need to:

- Gracefully handle configuration load failures without breaking the service
- Decide between failing fast vs. continuing with degraded functionality
- Alert operations when configuration issues occur
- Implement fallback strategies for different failure scenarios

## Solution

### Step 1: Understand the Failure Boundary

Configuration failures happen at the **load boundary**, not evaluation:

```kotlin
// Loading: This is where failures occur
val result = NamespaceSnapshotLoader(AppFeatures).load(json)
when {
  result.isSuccess -> { /* Config loaded */
  }
  result.isFailure -> { /* Handle failure HERE */
  }
}

// Evaluation: This never fails (total evaluation)
val value = AppFeatures.someFeature.evaluate(ctx)  // Always returns a value
```

**Key insight:** Features always evaluate to a value (defaults or overrides). Failures only occur when trying to load
new configuration.

### Step 2: Implement Failure Handlers

```kotlin
class ConfigurationFailureHandler {
  fun handleLoadFailure(
      error: ParseError,
      json: String
  ) {
    when (error) {
      is ParseError.InvalidJson -> handleInvalidJSON(error, json)
      is ParseError.FeatureNotFound -> handleUnknownFeature(error)
      is ParseError.InvalidSnapshot -> handleTypeMismatch(error)
    }
  }

  private fun handleInvalidJSON(
      error: ParseError.InvalidJson,
      json: String
  ) {
    logger.error("Invalid JSON syntax: ${error.message}")
    logger.error("JSON: $json")

    // Alert operations: config service is broken
    alertOps(
        severity = Severity.HIGH,
        message = "Config service returning malformed JSON",
        details = mapOf("error" to error.message)
    )

    // Metric
    metrics.increment("config.failure.invalid_json")

    // Continue with last-known-good (no action needed)
  }

  private fun handleUnknownFeature(error: ParseError.FeatureNotFound) {
    logger.warn("Config references unknown feature: ${error.key}")

    // Could be:
    // 1. Config is ahead of code (feature not deployed yet)
    // 2. Typo in config
    // 3. Feature was removed from code but still in config

    // Alert with lower severity (might be expected during rollout)
    alertOps(
        severity = Severity.MEDIUM,
        message = "Unknown feature in config: ${error.key}",
        details = mapOf("feature_key" to error.key)
    )

    metrics.increment("config.failure.unknown_feature", tags = mapOf(
        "key" to error.key
    ))
  }

  private fun handleTypeMismatch(error: ParseError.InvalidSnapshot) {
    logger.error("Type mismatch for ${error.key}: expected ${error.expectedType}, got ${error.actualType}")

    // This is always a bug in config
    alertOps(
        severity = Severity.HIGH,
        message = "Config has wrong type for feature",
        details = mapOf(
            "feature" to error.key,
            "expected" to error.expectedType,
            "actual" to error.actualType
        )
    )

    metrics.increment("config.failure.type_mismatch", tags = mapOf(
        "feature" to error.key
    ))
  }
}
```

### Step 3: Implement Fallback Strategies

**Strategy 1: Continue with Last-Known-Good**

```kotlin
class ConfigLoader(private val namespace: Namespace) {
  fun loadWithFallback(json: String) {
    val result = NamespaceSnapshotLoader(namespace).load(json)
when {
      result.isSuccess -> {
        logger.info("Config loaded successfully")
        lastSuccessfulLoad = Instant.now()
      }
      result.isFailure -> {
        logger.warn("Config load failed, continuing with last-known-good")
        failureHandler.handleLoadFailure(result.parseErrorOrNull(), json)
        // Last-known-good automatically preserved
      }
    }
  }
}
```

**Strategy 2: Retry with Exponential Backoff**

```kotlin
class RetryingConfigLoader(private val namespace: Namespace) {
  suspend fun loadWithRetry(
      fetchConfig: suspend () -> String,
      maxAttempts: Int = 3,
      initialDelay: Duration = 1.seconds
  ) {
    var attempt = 0
    var delay = initialDelay

    while (attempt < maxAttempts) {
      try {
        val json = fetchConfig()
        val result = NamespaceSnapshotLoader(namespace).load(json)
when {
          result.isSuccess -> {
            logger.info("Config loaded on attempt ${attempt + 1}")
            return
          }
          result.isFailure -> {
            logger.warn("Config load failed (attempt ${attempt + 1}): ${result.parseErrorOrNull()}")
            attempt++
            if (attempt < maxAttempts) {
              delay(delay)
              delay *= 2  // Exponential backoff
            }
          }
        }
      } catch (e: Exception) {
        logger.error("Config fetch failed (attempt ${attempt + 1})", e)
        attempt++
        if (attempt < maxAttempts) {
          delay(delay)
          delay *= 2
        }
      }
    }

    logger.error("All config load attempts failed")
    alertOps(Severity.HIGH, "Config load failed after $maxAttempts attempts")
  }
}
```

**Strategy 3: Circuit Breaker**

```kotlin
class CircuitBreakerConfigLoader(
    private val namespace: Namespace,
    private val failureThreshold: Int = 5,
    private val resetTimeout: Duration = 1.minutes
) {
  private var failureCount = 0
  private var lastFailure: Instant? = null
  private var circuitOpen = false

  fun load(json: String): Result<MaterializedConfiguration> {
    // Check if circuit should be reset
    lastFailure?.let {
      if (Duration.between(it, Instant.now()) > resetTimeout) {
        logger.info("Circuit breaker reset after timeout")
        failureCount = 0
        circuitOpen = false
      }
    }

    if (circuitOpen) {
      logger.warn("Circuit breaker open, skipping config load")
      metrics.increment("config.circuit_breaker.open")
      return Result.failure(KonditionalBoundaryFailure(ParseError.InvalidJson("Circuit breaker open")))
    }

    val result = NamespaceSnapshotLoader(namespace).load(json)
    return when {
      result.isSuccess -> {
        failureCount = 0
        result
      }
      result.isFailure -> {
        failureCount++
        lastFailure = Instant.now()

        if (failureCount >= failureThreshold) {
          circuitOpen = true
          logger.error("Circuit breaker opened after $failureCount failures")
          alertOps(Severity.CRITICAL, "Config circuit breaker opened")
        }

        result
      }
    }
  }
}
```

## Guarantees

- **Evaluation never fails**: Features always return a value
    - **Mechanism**: Total evaluation with required defaults
    - **Boundary**: Configuration load can fail, evaluation cannot

- **Failed loads don't affect traffic**: Invalid config rejected atomically
    - **Mechanism**: `load()` either succeeds completely or leaves state unchanged
    - **Boundary**: No partial application of configuration

- **Last-known-good preserved**: Previous configuration remains active on failure
    - **Mechanism**: Failed load doesn't modify namespace state
    - **Boundary**: "Last-known-good" might be initial defaults if no successful load yet

## What Can Go Wrong?

### Ignoring Failures

```kotlin
// DON'T: Silently ignore failures
NamespaceSnapshotLoader(AppFeatures).load(json)

// DO: Explicitly handle failures
val result = NamespaceSnapshotLoader(AppFeatures).load(json)
when {
  result.isFailure -> handleFailure(result.parseErrorOrNull())
  result.isSuccess -> Unit
}
```

### Failing Fast When You Should Continue

```kotlin
// DON'T: Crash the service on config failure
val startupResult = NamespaceSnapshotLoader(AppFeatures).load(json)
when {
  startupResult.isFailure -> throw ConfigLoadException()
}

// DO: Continue with last-known-good
val result = NamespaceSnapshotLoader(AppFeatures).load(json)
when {
  result.isFailure -> {
    logger.warn("Config load failed, continuing: ${result.parseErrorOrNull()}")
    // Service continues normally
  }
}
```

**Exception:** It's okay to fail fast at application startup if no defaults are acceptable.

### Not Alerting Operations

```kotlin
// DON'T: Log and move on
val result = NamespaceSnapshotLoader(AppFeatures).load(json)
when {
  result.isFailure -> logger.error("Config failed: ${result.parseErrorOrNull()}")
}

// DO: Alert operations
val result = NamespaceSnapshotLoader(AppFeatures).load(json)
when {
  result.isFailure -> {
    logger.error("Config failed: ${result.parseErrorOrNull()}")
    alertOps("Configuration validation failed", result.parseErrorOrNull())
    metrics.increment("config.failure")
  }
}
```

## Decision Tree: Fail Fast vs. Continue

### Fail Fast When:

1. **Startup and no acceptable defaults exist**
   ```kotlin
   // App cannot function without remote config
   val json = fetchConfigOrThrow()
   val result = NamespaceSnapshotLoader(AppFeatures).load(json)
when {
       result.isFailure -> {
           logger.fatal("Cannot start without valid config: ${result.parseErrorOrNull()}")
           exitProcess(1)
       }
   }
   ```

2. **Deployment validation (pre-production)**
   ```kotlin
   // Validate config before deploying to production
   val result = NamespaceSnapshotLoader(AppFeatures).load(candidateConfig)
   require(result.isSuccess) {
       "Config validation failed: ${result.parseErrorOrNull()}"
   }
   ```

### Continue (Last-Known-Good) When:

1. **Service is already running**
   ```kotlin
   // Refresh configuration, but don't break existing traffic
   val result = NamespaceSnapshotLoader(AppFeatures).load(json)
   when {
       result.isFailure -> {
           logger.warn("Config refresh failed, continuing with last-known-good")
           // Traffic continues unaffected
       }
   }
   ```

2. **Acceptable defaults exist**
   ```kotlin
   // Features have sensible defaults
   object AppFeatures : Namespace("app") {
       val maxRetries by integer<Context>(default = 3)  // Reasonable default
   }

   // If config load fails, default=3 is fine
   ```

3. **Partial functionality is better than no functionality**
   ```kotlin
   // E-commerce site: better to run with defaults than crash
   val result = NamespaceSnapshotLoader(AppFeatures).load(json)
   when {
       result.isFailure -> {
           logger.warn("Using default configuration")
           // Checkout still works, just with default behavior
       }
   }
   ```

## Testing Failure Handling

### Test Last-Known-Good Preservation

```kotlin
@Test
fun `failed load preserves last-known-good`() {
  // Load valid config
  val validJson = """{ "darkMode": { "rules": [{ "value": true }] } }"""
  val result1 = NamespaceSnapshotLoader(AppFeatures).load(validJson)
  require(result1.isSuccess)

  // Verify darkMode is true
  val ctx = Context(stableId = StableId.of("user"))
  assertTrue(AppFeatures.darkMode.evaluate(ctx))

  // Try to load invalid config
  val invalidJson = """{ "darkMode": { "rules": [{ "value": "invalid" }] } }"""
  val result2 = NamespaceSnapshotLoader(AppFeatures).load(invalidJson)
  require(result2.isFailure)

  // Verify darkMode is STILL true (last-known-good preserved)
  assertTrue(AppFeatures.darkMode.evaluate(ctx))
}
```

### Test Failure Alerting

```kotlin
@Test
fun `failure triggers alert`() {
  val alertSpy = mockk<AlertService>(relaxed = true)
  val handler = ConfigurationFailureHandler(alertSpy)

  val invalidJson = """{ "invalid": "json" """
  val result = NamespaceSnapshotLoader(AppFeatures).load(invalidJson)

  require(result.isFailure)
  handler.handleLoadFailure(result.parseErrorOrNull(), invalidJson)

  verify { alertSpy.send(match { it.severity == Severity.HIGH }) }
}
```

## Monitoring Failure Patterns

### Key Metrics

```kotlin
// Failure rate
metrics.increment("config.load.failure")
metrics.increment("config.load.success")

// Failure reasons
metrics.increment("config.failure.invalid_json")
metrics.increment("config.failure.type_mismatch")
metrics.increment("config.failure.unknown_feature")

// Time since last successful load
metrics.gauge("config.time_since_last_success", duration)
```

### Alerts

1. **No successful load in X minutes**: Config pipeline may be broken
2. **Failure rate > threshold**: Config service degraded
3. **Repeated same error**: Persistent issue needs investigation
4. **Circuit breaker opened**: Multiple consecutive failures

## Common Failure Scenarios

### Scenario 1: Config Service Returns 500

```kotlin
try {
  val json = httpClient.get(configUrl).body<String>()
  NamespaceSnapshotLoader(AppFeatures).load(json)
} catch (e: HttpException) {
  logger.error("Config service error: ${e.statusCode}", e)
  metrics.increment("config.fetch.http_error", tags = mapOf(
      "status_code" to e.statusCode.toString()
  ))
  // Continue with last-known-good
}
```

### Scenario 2: Config Contains Typo

```json
{
  "darkMood": {
    "rules": [
      {
        "value": true
      }
    ]
  }
  // Typo: darkMood vs darkMode
}
```

Result: `Result.failure(KonditionalBoundaryFailure(ParseError.featureNotFound(...)))`. Last-known-good preserved, ops alerted.

### Scenario 3: Config Type Changed

```json
{
  "maxRetries": {
    "rules": [
      {
        "value": "5"
      }
    ]
  }
  // String instead of Int
}
```

Result: `Result.failure(KonditionalBoundaryFailure(ParseError.invalidSnapshot(...)))`. Last-known-good preserved, ops alerted.

### Scenario 4: Network Timeout

```kotlin
try {
  val json = withTimeout(5.seconds) {
    httpClient.get(configUrl).body<String>()
  }
} catch (e: TimeoutCancellationException) {
  logger.warn("Config fetch timeout, continuing with last-known-good")
  metrics.increment("config.fetch.timeout")
  // Continue with last-known-good
}
```

## Next Steps

- [Failure Modes](/production-operations/failure-modes) — Comprehensive failure catalog
- [Safe Remote Configuration](/how-to-guides/safe-remote-config) — Load patterns
- [Refresh Patterns](/production-operations/refresh-patterns) — Polling, webhooks
- [Configuration Lifecycle](/learn/configuration-lifecycle) — How config flows
