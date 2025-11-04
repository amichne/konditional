---
title: Integration Guide
description: How to integrate Konditional serialization into your existing codebase
---


## Integration Overview

This guide walks you through integrating the Konditional serialization system into an existing application. We'll cover both **greenfield** (starting fresh) and **brownfield** (existing codebase) scenarios.

::: note
This guide assumes you already have Konditional set up and are using `Conditional` instances for feature flags. If not, see the [Quick Start](/getting-started/quick-start/) guide first.
:::

## Prerequisites

Before integrating serialization, ensure you have:

1. **Konditional library** added to your project
2. **Feature flags defined** as `Conditional` instances (typically enums)
3. **Moshi dependencies** added to your `build.gradle.kts`
4. **Understanding of your deployment strategy** (where JSON will be stored/loaded)

## Integration Patterns

Choose the pattern that best fits your architecture:

### Pattern 1: Build-Time Configuration

**Best for**: Small apps, monoliths, infrequent flag changes

```
Developer → ConfigBuilder → JSON → Git → Build → Embed in APK/JAR
```

- Configuration is committed to source control
- Flags are baked into the application at build time
- Changes require a new build and deployment

### Pattern 2: Runtime Configuration

**Best for**: Large apps, microservices, frequent flag changes

```
Developer → ConfigBuilder → JSON → Upload to S3/CDN → App downloads on launch
```

- Configuration is external to the application
- Flags can be updated without redeploying
- Requires network access and error handling

### Pattern 3: Hybrid Approach

**Best for**: Production apps needing flexibility

```
Developer → ConfigBuilder → Default JSON (embedded) + Remote JSON (downloaded)
                                ↓                              ↓
                          Fallback config              Production overrides
```

- Embedded default configuration as fallback
- Remote configuration overrides defaults
- Graceful degradation if network fails

## Architecture Decisions

### Where to Store JSON?


=== "Tab"

  
=== "Embedded Resources"

    **Pros:**
    - No network dependency
    - Guaranteed availability
    - Fast loading

    **Cons:**
    - Requires rebuild to change
    - Larger APK/JAR size
    - No runtime flexibility

    ```kotlin
    val json = javaClass.getResourceAsStream("/flags.json")
        .bufferedReader()
        .use { it.readText() }
    ```
  

  
=== "Local File System"

    **Pros:**
    - Easy to update locally
    - No network needed
    - Fast loading

    **Cons:**
    - Requires file system access
    - Platform-specific paths
    - Not suitable for mobile

    ```kotlin
    val json = File("/etc/myapp/flags.json").readText()
    ```
  

  
=== "Remote Server/CDN"

    **Pros:**
    - Update without deploying
    - Environment-specific configs
    - A/B testing support

    **Cons:**
    - Network dependency
    - Requires caching
    - Potential latency

    ```kotlin
    val json = httpClient.get("https://cdn.example.com/flags.json")
    ```
  

  
=== "Configuration Service"

    **Pros:**
    - Centralized management
    - Access control
    - Audit logs

    **Cons:**
    - Added complexity
    - Service dependency
    - Requires infrastructure

    ```kotlin
    val json = configService.getFlags(environment, version)
    ```
  


### When to Load Flags?

Consider the trade-offs of different loading strategies:

#### Application Startup (Recommended)

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Load flags before anything else
        loadFeatureFlags()

        // Rest of initialization...
    }

    private fun loadFeatureFlags() {
        val json = loadFlagsJson() // From wherever you store them
        val konfig = SnapshotSerializer.default.deserialize(json)
        Flags.load(konfig)
    }
}
```

**Pros:** Flags available immediately, simple error handling
**Cons:** Increases startup time, blocks initialization

#### Lazy Loading

```kotlin
object FlagManager {
    private var initialized = false

    fun ensureLoaded() {
        if (!initialized) {
            loadFlags()
            initialized = true
        }
    }
}
```

**Pros:** Faster startup, load only when needed
**Cons:** First access is slower, race conditions possible

#### Background Loading

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Load with defaults first
        loadDefaultFlags()

        // Update from remote in background
        CoroutineScope(Dispatchers.IO).launch {
            updateFlagsFromRemote()
        }
    }
}
```

**Pros:** Fast startup, eventually consistent
**Cons:** Complex state management, temporary inconsistency

## Registering Conditionals

The registry maps string keys to `Conditional` instances. Register all flags **before** deserialization:

### For Enum-Based Flags

```kotlin
// Register an entire enum at once
ConditionalRegistry.registerEnum<FeatureFlags>()
```

### For Mixed Flag Types

```kotlin
// Register multiple enums
ConditionalRegistry.registerEnum<FeatureFlags>()
ConditionalRegistry.registerEnum<ExperimentFlags>()
ConditionalRegistry.registerEnum<DebugFlags>()

// Or register individually
ConditionalRegistry.register(FeatureFlags.DARK_MODE)
ConditionalRegistry.register(ExperimentFlags.NEW_ONBOARDING)
```

### Registration Timing

::: caution
**Critical:** Register all flags before calling `deserialize()`. Missing registrations will cause runtime errors.
:::

```kotlin
// ✅ Good: Register first
fun initializeFlags() {
    ConditionalRegistry.registerEnum<FeatureFlags>()
    val konfig = SnapshotSerializer.default.deserialize(json)
    Flags.load(konfig)
}

// ❌ Bad: Deserialize before registering
fun initializeFlags() {
    val konfig = SnapshotSerializer.default.deserialize(json) // Error!
    ConditionalRegistry.registerEnum<FeatureFlags>()
}
```

## Error Handling

Production applications must handle serialization errors gracefully:

### Strategy 1: Fallback to Defaults

```kotlin
fun loadFlags(): Flags.Snapshot {
    return try {
        val json = downloadRemoteConfig()
        SnapshotSerializer.default.deserialize(json)
    } catch (e: IOException) {
        logger.warn("Failed to load remote config, using defaults", e)
        loadDefaultSnapshot()
    } catch (e: JsonDataException) {
        logger.error("Invalid JSON format, using defaults", e)
        loadDefaultSnapshot()
    }
}

private fun loadDefaultSnapshot(): Flags.Snapshot {
    val defaultJson = resources.openRawResource(R.raw.default_flags)
        .bufferedReader()
        .use { it.readText() }
    return SnapshotSerializer.default.deserialize(defaultJson)
}
```

### Strategy 2: Partial Updates

```kotlin
fun updateFlags() {
    try {
        val patchJson = downloadPatch()
        val currentKonfig = getCurrentSnapshot()
        val updated = SnapshotSerializer.default.applyPatchJson(
            currentKonfig,
            patchJson
        )
        Flags.load(updated)
        logger.info("Successfully applied patch update")
    } catch (e: Exception) {
        logger.error("Patch update failed, keeping current config", e)
        // Don't change anything - keep working configuration
    }
}
```

### Strategy 3: Validation Before Loading

```kotlin
fun loadFlags(json: String): Result<Flags.Snapshot> {
    return try {
        // Deserialize
        val konfig = SnapshotSerializer.default.deserialize(json)

        // Validate before loading
        validateSnapshot(konfig)

        // Load if valid
        Flags.load(konfig)
        Result.success(konfig)
    } catch (e: Exception) {
        logger.error("Flag loading failed", e)
        Result.failure(e)
    }
}

private fun validateSnapshot(konfig: Flags.Snapshot) {
    require(konfig.flags.isNotEmpty()) {
        "Snapshot cannot be empty"
    }

    // Validate against schema
    val requiredFlags = setOf("feature_x", "feature_y")
    val actualFlags = konfig.flags.keys.map { it.key }.toSet()

    require(requiredFlags.all { it in actualFlags }) {
        "Missing required flags: ${requiredFlags - actualFlags}"
    }
}
```

## Testing Integration

### Unit Tests

Test serialization/deserialization in isolation:

```kotlin
class FlagSerializationTest {
    @BeforeEach
    fun setup() {
        ConditionalRegistry.registerEnum<FeatureFlags>()
    }

    @AfterEach
    fun tearDown() {
        ConditionalRegistry.clear()
    }

    @Test
    fun `serialization round-trip preserves behavior`() {
        val original = createTestSnapshot()
        val json = SnapshotSerializer.default.serialize(original)
        val restored = SnapshotSerializer.default.deserialize(json)

        val context = createTestContext()

        with(Flags) {
            Flags.load(original)
            val originalValue = context.evaluate(FeatureFlags.MY_FLAG)

            Flags.load(restored)
            val restoredValue = context.evaluate(FeatureFlags.MY_FLAG)

            assertEquals(originalValue, restoredValue)
        }
    }
}
```

### Integration Tests

Test the full loading workflow:

```kotlin
@Test
fun `application loads flags on startup`() {
    // Prepare test JSON
    val testJson = """
        {
          "flags": [
            {
              "key": "my_feature",
              "type": "BOOLEAN",
              "defaultValue": true,
              "default": {"value": true, "type": "BOOLEAN"},
              "rules": []
            }
          ]
        }
    """.trimIndent()

    // Save to test location
    File(testFlagsPath).writeText(testJson)

    // Initialize app
    val app = TestApplication()
    app.onCreate()

    // Verify flags are loaded
    val context = createTestContext()
    with(Flags) {
        val value = context.evaluate(FeatureFlags.MY_FEATURE)
        assertTrue(value)
    }
}
```

## Performance Considerations

### Deserialization Performance

For large configurations (100+ flags):

```kotlin
// ✅ Good: Deserialize once, reuse
val konfig = SnapshotSerializer.default.deserialize(json)
Flags.load(konfig)

// ❌ Bad: Deserialize repeatedly
repeat(100) {
    val konfig = SnapshotSerializer.default.deserialize(json)
    // This is very slow!
}
```

### Memory Usage

Snapshots are immutable and can be shared:

```kotlin
// ✅ Good: Share konfig across components
val konfig = loadSnapshot()
componentA.useSnapshot(konfig)
componentB.useSnapshot(konfig)

// ⚠️ Consider: Snapshot size in memory
// For apps with 1000+ flags, monitor memory usage
```

### Caching

Implement caching for remote configurations:

```kotlin
class FlagCache(private val cacheDir: File) {
    fun getCachedOrFetch(url: String): String {
        val cacheFile = File(cacheDir, "flags.json")

        // Use cached if recent
        if (cacheFile.exists() && cacheFile.isRecent()) {
            return cacheFile.readText()
        }

        // Fetch new version
        val json = httpClient.get(url)
        cacheFile.writeText(json)
        return json
    }

    private fun File.isRecent(): Boolean {
        val age = System.currentTimeMillis() - lastModified()
        return age < TimeUnit.HOURS.toMillis(1)
    }
}
```

## Common Pitfalls

### Pitfall 1: Forgetting to Register

```kotlin
// ❌ This will crash at runtime
val konfig = SnapshotSerializer.default.deserialize(json)
// IllegalArgumentException: Conditional with key 'my_flag' not found

// ✅ Always register first
ConditionalRegistry.registerEnum<FeatureFlags>()
val konfig = SnapshotSerializer.default.deserialize(json)
```

### Pitfall 2: Type Mismatches

```kotlin
// JSON says INT but code expects BOOLEAN
{
  "key": "my_flag",
  "type": "INT",  // Wrong!
  "defaultValue": 42
}

// ✅ Ensure JSON matches your Conditional definition
enum class FeatureFlags(override val key: String) : Conditional<Boolean, Context> {
    MY_FLAG("my_flag")  // Expects Boolean, not Int!
}
```

### Pitfall 3: Missing Default Field

```kotlin
// ❌ Old format without 'default' field
{
  "key": "my_flag",
  "type": "BOOLEAN",
  "defaultValue": true
  // Missing: "default": {"value": true, "type": "BOOLEAN"}
}

// ✅ Always include the default wrapper
{
  "key": "my_flag",
  "type": "BOOLEAN",
  "defaultValue": true,
  "default": {"value": true, "type": "BOOLEAN"}
}
```

## Next Steps

Now that you understand the integration concepts, proceed to the step-by-step guide:

[Step 1: Dependencies →](/serialization/steps/step-01-dependencies/)

Or jump straight to a complete example:

[Full Runthrough →](/serialization/runthrough/)
