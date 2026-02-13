# How-To: Load Configuration Safely from Remote

## Problem

You need to:

- Load feature flag configuration from a remote source (API, S3, CDN)
- Validate configuration before applying it to production traffic
- Handle invalid configuration without breaking the service
- Update configuration without redeploying code

## Solution

### Step 1: Define Features Statically

```kotlin
object AppFeatures : Namespace("app") {
    val darkMode by boolean<Context>(default = false)
    val maxRetries by integer<Context>(default = 3)
    val checkoutFlow by enum<CheckoutVariant, Context>(default = CheckoutVariant.CLASSIC)
}
```

**Static definitions establish the contract:** Types, keys, and defaults are known at compile-time.

### Step 2: Load Configuration with Explicit Validation

```kotlin
fun loadRemoteConfiguration() {
    val json = try {
        httpClient.get("https://config.example.com/app-features.json").body<String>()
    } catch (e: Exception) {
        logger.error("Failed to fetch remote config", e)
        // Last-known-good remains active
        return
    }

    when (val result = NamespaceSnapshotLoader(AppFeatures).load(json)) {
        is ParseResult.Success -> {
            logger.info("Remote config loaded successfully")
            metrics.increment("config.load.success")
        }
        is ParseResult.Failure -> {
            logger.error("Remote config validation failed: ${result.error}")
            metrics.increment("config.load.failure")
            alertOps("Configuration validation failed", result.error)
            // Last-known-good remains active
        }
    }
}
```

**Key insight:** `ParseResult` makes validation explicit. Invalid config is rejected before affecting traffic.

### Step 3: Handle Parse Failures Gracefully

```kotlin
when (val result = NamespaceSnapshotLoader(AppFeatures).load(json)) {
    is ParseResult.Failure -> {
        when (result.error) {
            is ParseError.InvalidJSON -> {
                logger.error("JSON syntax error: ${result.error.message}")
                // Alert: config server is returning malformed JSON
            }
            is ParseError.UnknownFeature -> {
                logger.error("Unknown feature key: ${result.error.key}")
                // Alert: config references a feature that doesn't exist in code
            }
            is ParseError.TypeMismatch -> {
                logger.error("Type mismatch for ${result.error.key}: expected ${result.error.expectedType}, got ${result.error.actualType}")
                // Alert: config has wrong type for a feature
            }
        }
    }
}
```

### Step 4: Use Initial Defaults if Remote Unavailable

```kotlin
class ConfigurationManager(private val namespace: Namespace) {
    private var initialized = false

    fun initialize() {
        if (initialized) return

        // Try to load remote config
        val loaded = try {
            val json = fetchRemoteConfig()
            when (val result = NamespaceSnapshotLoader(namespace).load(json)) {
                is ParseResult.Success -> {
                    logger.info("Initialized with remote config")
                    true
                }
                is ParseResult.Failure -> {
                    logger.warn("Remote config invalid: ${result.error}")
                    false
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to fetch remote config on init", e)
            false
        }

        if (!loaded) {
            logger.info("Using default configuration")
        }

        initialized = true
        // Service starts either way—with remote config or defaults
    }
}
```

## Guarantees

- **Validation at boundary**: Invalid config rejected before affecting traffic
    - **Mechanism**: `ParseResult.Failure` returned if JSON doesn't match definitions
    - **Boundary**: Validation catches schema errors, not business logic errors

- **Atomic replacement**: All evaluations see old config OR new config, never partial
    - **Mechanism**: Configuration atomically swapped on successful load
    - **Boundary**: No guarantee about *when* a particular request sees the update

- **Last-known-good preserved**: Failed loads don't affect evaluation
    - **Mechanism**: Failed load doesn't modify namespace state
    - **Boundary**: "Last-known-good" might be initial defaults if no successful load

## Configuration Format

### Valid JSON Example

```json
{
  "darkMode": {
    "rules": [
      {
        "value": true,
        "predicates": {
          "platforms": ["IOS", "ANDROID"]
        }
      }
    ]
  },
  "maxRetries": {
    "rules": [
      {
        "value": 5,
        "predicates": {
          "android": true
        }
      }
    ]
  },
  "checkoutFlow": {
    "rules": [
      {
        "value": "SIMPLIFIED",
        "predicates": {
          "rampUp": { "percentage": 50.0 }
        }
      }
    ]
  }
}
```

### What Gets Validated

1. **JSON syntax**: Must be valid JSON
2. **Feature keys**: Must match defined properties in namespace
3. **Type safety**: Values must match feature types
4. **Rule structure**: Rules must have valid predicates

See [Persistence Format](/serialization/persistence-format) for complete schema.

## What Can Go Wrong?

### Network Failures

```kotlin
// Config fetch times out or fails
try {
    val json = httpClient.get(configUrl).body<String>()
} catch (e: Exception) {
    // DON'T: Crash the service
    // DO: Log error, keep last-known-good, alert ops
    logger.error("Config fetch failed", e)
    metrics.increment("config.fetch.failure")
}
```

**Result:** Service continues with last-known-good configuration.

### Typo in Feature Key

```json
{
  "darkMood": {  // Typo: should be "darkMode"
    "rules": [{ "value": true }]
  }
}
```

**Result:** `ParseResult.Failure(UnknownFeature("darkMood"))`. Config rejected, last-known-good preserved.

### Type Mismatch

```json
{
  "maxRetries": {
    "rules": [{ "value": "five" }]  // Wrong type: String instead of Int
  }
}
```

**Result:** `ParseResult.Failure(TypeMismatch("maxRetries", expectedType = "Int", actualType = "String"))`. Config
rejected.

### Partial Configuration

```json
{
  "darkMode": {
    "rules": [{ "value": true }]
  }
  // maxRetries and checkoutFlow not included
}
```

**Result:** `ParseResult.Success`. Only `darkMode` is overridden. `maxRetries` and `checkoutFlow` use their static
definitions.

**Best practice:** Partial configuration is fine for gradual rollouts. Features not in JSON use static rules + defaults.

## Advanced Patterns

### Pattern: Versioned Configuration

```kotlin
data class VersionedConfig(
    val version: String,
    val config: String,
    val timestamp: Instant
)

class ConfigLoader(private val namespace: Namespace) {
    private var currentVersion: String? = null

    fun loadVersioned(versioned: VersionedConfig) {
        when (val result = NamespaceSnapshotLoader(namespace).load(versioned.config)) {
            is ParseResult.Success -> {
                logger.info("Loaded config version ${versioned.version}")
                currentVersion = versioned.version
                metrics.gauge("config.version", versioned.version)
            }
            is ParseResult.Failure -> {
                logger.error("Config version ${versioned.version} invalid: ${result.error}")
                metrics.increment("config.invalid_version", tags = mapOf(
                    "version" to versioned.version
                ))
            }
        }
    }

    fun getCurrentVersion(): String? = currentVersion
}
```

### Pattern: Staged Rollout

```kotlin
class StagedConfigLoader(private val namespace: Namespace) {
    fun loadWithCanary(json: String, canaryPercentage: Double = 1.0) {
        // First: validate without loading
        when (val result = ConfigurationSnapshotCodec.decode(json)) {
            is ParseResult.Failure -> {
                logger.error("Validation failed: ${result.error}")
                return
            }
            is ParseResult.Success -> Unit
        }

        // Second: apply to canary traffic only
        if (Random.nextDouble() < canaryPercentage / 100.0) {
            NamespaceSnapshotLoader(namespace).load(json)
            logger.info("Config applied to canary traffic")
        }

        // Third: after monitoring, apply to all traffic
        // (This is a simplified example; real implementation would be more sophisticated)
    }
}
```

### Pattern: Configuration Diff

```kotlin
fun logConfigDiff(oldJson: String, newJson: String) {
    val oldConfig = Json.parseToJsonElement(oldJson).jsonObject
    val newConfig = Json.parseToJsonElement(newJson).jsonObject

    val added = newConfig.keys - oldConfig.keys
    val removed = oldConfig.keys - newConfig.keys
    val modified = newConfig.keys.intersect(oldConfig.keys).filter {
        oldConfig[it] != newConfig[it]
    }

    logger.info("""
        Config diff:
        Added: $added
        Removed: $removed
        Modified: $modified
    """.trimIndent())
}
```

## Monitoring Remote Configuration

### Metrics to Track

```kotlin
// Load success/failure rate
metrics.increment("config.load.success")
metrics.increment("config.load.failure")

// Validation failure reasons
metrics.increment("config.validation.type_mismatch")
metrics.increment("config.validation.unknown_feature")
metrics.increment("config.validation.invalid_json")

// Load latency
metrics.recordLatency("config.load.duration", duration)

// Configuration version
metrics.gauge("config.version", version)
```

### Alerts to Configure

1. **No successful load in X minutes**: Remote config source may be down
2. **Validation failure rate > threshold**: Config server is sending bad data
3. **Fetch failures spike**: Network issues or config server issues
4. **Version hasn't changed in X hours**: Config pipeline may be stuck

## Testing Remote Configuration

### Test Invalid JSON Rejection

```kotlin
@Test
fun `invalid JSON is rejected`() {
    val invalidJson = """{ "darkMode": { "rules": [ INVALID ] } }"""

    val result = NamespaceSnapshotLoader(AppFeatures).load(invalidJson)

    assertTrue(result is ParseResult.Failure)
    assertTrue((result as ParseResult.Failure).error is ParseError.InvalidJSON)
}
```

### Test Type Safety

```kotlin
@Test
fun `type mismatch is rejected`() {
    val json = """{ "maxRetries": { "rules": [{ "value": "five" }] } }"""

    val result = NamespaceSnapshotLoader(AppFeatures).load(json)

    assertTrue(result is ParseResult.Failure)
    val error = (result as ParseResult.Failure).error
    assertTrue(error is ParseError.TypeMismatch)
}
```

### Test Partial Configuration

```kotlin
@Test
fun `partial configuration loads successfully`() {
    val json = """{ "darkMode": { "rules": [{ "value": true }] } }"""

    val result = NamespaceSnapshotLoader(AppFeatures).load(json)

    assertTrue(result is ParseResult.Success)

    // darkMode overridden
    val ctx = Context(stableId = StableId.of("user"))
    assertTrue(AppFeatures.darkMode.evaluate(ctx))

    // maxRetries still uses default
    assertEquals(3, AppFeatures.maxRetries.evaluate(ctx))
}
```

## Next Steps

- [Refresh Patterns](/production-operations/refresh-patterns) — Polling, webhooks, file watch
- [Failure Modes](/production-operations/failure-modes) — Comprehensive error scenarios
- [Handling Failures](/how-to-guides/handling-failures) — What to do when config fails
- [Persistence Format](/serialization/persistence-format) — Complete JSON schema
