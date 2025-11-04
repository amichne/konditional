---
title: 'Step 5: Deserialize JSON'
description: Load JSON configurations back into Flags.Snapshot
---


## Overview

Deserialization converts JSON back into a `Flags.Snapshot` that can be loaded into the runtime. This is typically done during application startup or when updating configurations.

::: tip
**Time estimate:** 15 minutes

**Goal:** Load your JSON configuration into a usable `Flags.Snapshot`
:::

## Basic Deserialization

```kotlin
import io.amichne.konditional.serialization.SnapshotSerializer
import java.io.File

// 1. Register your flags (critical!)
ConditionalRegistry.registerEnum<FeatureFlags>()

// 2. Read JSON from file
val json = File("config/production-flags.json").readText()

// 3. Deserialize
val konfig = SnapshotSerializer.default.deserialize(json)

println("Loaded ${konfig.flags.size} flags")
```

::: caution
**Always** register flags before deserializing, or you'll get an `IllegalArgumentException`.
:::

## Loading from Different Sources

### From Resources (Embedded in APK/JAR)

```kotlin
fun loadEmbeddedConfiguration(): Flags.Snapshot {
    ConditionalRegistry.registerEnum<FeatureFlags>()

    val json = javaClass.getResourceAsStream("/flags.json")
        ?.bufferedReader()
        ?.use { it.readText() }
        ?: throw IllegalStateException("flags.json not found in resources")

    return SnapshotSerializer.default.deserialize(json)
}
```

### From Assets (Android)

```kotlin
import android.content.Context as AndroidContext

fun loadFromAssets(context: AndroidContext): Flags.Snapshot {
    ConditionalRegistry.registerEnum<FeatureFlags>()

    val json = context.assets.open("flags.json")
        .bufferedReader()
        .use { it.readText() }

    return SnapshotSerializer.default.deserialize(json)
}
```

### From Remote URL

```kotlin
import java.net.URL

suspend fun loadRemoteConfiguration(url: String): Flags.Snapshot {
    ConditionalRegistry.registerEnum<FeatureFlags>()

    val json = withContext(Dispatchers.IO) {
        URL(url).readText()
    }

    return SnapshotSerializer.default.deserialize(json)
}

// Usage
lifecycleScope.launch {
    val konfig = loadRemoteConfiguration("https://cdn.example.com/flags.json")
    Flags.load(konfig)
}
```

### From Network with OkHttp

```kotlin
import okhttp3.OkHttpClient
import okhttp3.Request

suspend fun loadFromCDN(client: OkHttpClient, url: String): Flags.Snapshot {
    ConditionalRegistry.registerEnum<FeatureFlags>()

    val json = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).execute().use { response ->
            response.body?.string()
                ?: throw IOException("Empty response body")
        }
    }

    return SnapshotSerializer.default.deserialize(json)
}
```

## Error Handling

Production code must handle deserialization errors gracefully:

### Pattern 1: Fallback to Embedded Config

```kotlin
fun loadConfigWithFallback(context: AndroidContext): Flags.Snapshot {
    ConditionalRegistry.registerEnum<FeatureFlags>()

    return try {
        // Try to load from cache/remote
        loadCachedConfiguration()
    } catch (e: Exception) {
        logger.warn("Failed to load cached config, using embedded", e)
        try {
            loadEmbeddedConfiguration(context)
        } catch (e: Exception) {
            logger.error("Failed to load embedded config, using hardcoded", e)
            createDefaultSnapshot()
        }
    }
}

fun createDefaultSnapshot(): Flags.Snapshot {
    // Last resort: hardcoded defaults
    return ConfigBuilder.buildSnapshot {
        FeatureFlags.DARK_MODE with { default(false) }
        FeatureFlags.NEW_ONBOARDING with { default(false) }
    }
}
```

### Pattern 2: Result Type

```kotlin
sealed class ConfigLoadResult {
    data class Success(val konfig: Flags.Snapshot) : ConfigLoadResult()
    data class Error(val error: Exception, val fallback: Flags.Snapshot) : ConfigLoadResult()
}

fun loadConfiguration(json: String): ConfigLoadResult {
    ConditionalRegistry.registerEnum<FeatureFlags>()

    return try {
        val konfig = SnapshotSerializer.default.deserialize(json)
        ConfigLoadResult.Success(konfig)
    } catch (e: Exception) {
        logger.error("Deserialization failed", e)
        val fallback = createDefaultSnapshot()
        ConfigLoadResult.Error(e, fallback)
    }
}

// Usage
when (val result = loadConfiguration(json)) {
    is ConfigLoadResult.Success -> {
        Flags.load(result.konfig)
        showToast("Configuration loaded")
    }
    is ConfigLoadResult.Error -> {
        Flags.load(result.fallback)
        showToast("Using default configuration")
        logError(result.error)
    }
}
```

## Validation After Deserialization

Validate the deserialized konfig before using it:

```kotlin
fun loadAndValidate(json: String): Flags.Snapshot {
    ConditionalRegistry.registerEnum<FeatureFlags>()

    val konfig = SnapshotSerializer.default.deserialize(json)

    // Validate
    require(konfig.flags.isNotEmpty()) {
        "Snapshot must contain at least one flag"
    }

    // Check required flags exist
    val requiredFlags = setOf("dark_mode", "new_onboarding")
    val actualFlags = konfig.flags.keys.map { it.key }.toSet()

    require(requiredFlags.all { it in actualFlags }) {
        "Missing required flags: ${requiredFlags - actualFlags}"
    }

    // Test evaluation works
    val testContext = createTestContext()
    Flags.load(konfig)

    with(Flags) {
        testContext.evaluate(FeatureFlags.DARK_MODE)
        testContext.evaluate(FeatureFlags.NEW_ONBOARDING)
    }

    return konfig
}
```

## Caching Deserialized Snapshots

Don't deserialize repeatedly - cache the result:

```kotlin
object FlagCache {
    private var cached: Flags.Snapshot? = null
    private var lastLoadTime: Long = 0

    fun getSnapshot(forceRefresh: Boolean = false): Flags.Snapshot {
        val cacheAge = System.currentTimeMillis() - lastLoadTime

        if (!forceRefresh && cached != null && cacheAge < CACHE_TTL) {
            return cached!!
        }

        // Load fresh
        val konfig = loadConfiguration()
        cached = konfig
        lastLoadTime = System.currentTimeMillis()

        return konfig
    }

    fun clear() {
        cached = null
        lastLoadTime = 0
    }

    private companion object {
        const val CACHE_TTL = 5 * 60 * 1000L // 5 minutes
    }
}
```

## Background Loading

Load configurations in the background to avoid blocking the UI:

```kotlin
class FlagLoader(private val context: AndroidContext) {

    suspend fun loadInBackground(): Flags.Snapshot = withContext(Dispatchers.IO) {
        ConditionalRegistry.registerEnum<FeatureFlags>()

        try {
            // Try remote first
            loadFromRemote()
        } catch (e: IOException) {
            logger.warn("Remote load failed, trying cache", e)
            try {
                loadFromCache()
            } catch (e: Exception) {
                logger.warn("Cache load failed, using embedded", e)
                loadEmbedded()
            }
        }
    }

    private fun loadFromRemote(): Flags.Snapshot {
        val url = getConfigUrl()
        val json = URL(url).readText()

        // Cache for next time
        cacheJson(json)

        return SnapshotSerializer.default.deserialize(json)
    }

    private fun loadFromCache(): Flags.Snapshot {
        val cacheFile = File(context.cacheDir, "flags.json")
        val json = cacheFile.readText()
        return SnapshotSerializer.default.deserialize(json)
    }

    private fun loadEmbedded(): Flags.Snapshot {
        val json = context.assets.open("default-flags.json")
            .bufferedReader()
            .use { it.readText() }

        return SnapshotSerializer.default.deserialize(json)
    }

    private fun cacheJson(json: String) {
        File(context.cacheDir, "flags.json").writeText(json)
    }

    private fun getConfigUrl(): String {
        val environment = BuildConfig.ENVIRONMENT
        return "https://cdn.example.com/flags-$environment.json"
    }
}
```

## Testing Deserialization

Test your deserialization logic:

```kotlin
class DeserializationTest {

    @BeforeEach
    fun setUp() {
        ConditionalRegistry.registerEnum<FeatureFlags>()
    }

    @AfterEach
    fun tearDown() {
        ConditionalRegistry.clear()
    }

    @Test
    fun `deserialize valid JSON successfully`() {
        val json = """
            {
              "flags": [
                {
                  "key": "dark_mode",
                  "type": "BOOLEAN",
                  "defaultValue": true,
                  "default": {"value": true, "type": "BOOLEAN"},
                  "rules": []
                }
              ]
            }
        """.trimIndent()

        val konfig = SnapshotSerializer.default.deserialize(json)

        assertEquals(1, konfig.flags.size)
    }

    @Test
    fun `deserialization fails gracefully with invalid JSON`() {
        val invalidJson = "not valid json"

        assertThrows<JsonDataException> {
            SnapshotSerializer.default.deserialize(invalidJson)
        }
    }

    @Test
    fun `deserialization fails if flag not registered`() {
        ConditionalRegistry.clear() // Clear registration

        val json = """
            {
              "flags": [
                {
                  "key": "unknown_flag",
                  "type": "BOOLEAN",
                  "defaultValue": true,
                  "default": {"value": true, "type": "BOOLEAN"},
                  "rules": []
                }
              ]
            }
        """.trimIndent()

        assertThrows<IllegalArgumentException> {
            SnapshotSerializer.default.deserialize(json)
        }
    }
}
```

## What's Next?

With deserialization working, you'll learn how to load the konfig into the runtime.

<div style="display: flex; justify-content: space-between; margin-top: 2rem;">
  <a href="/serialization/steps/step-04-serialize/" style="text-decoration: none;">
    <strong>← Previous: Step 4 - Serialize</strong>
  </a>
  <a href="/serialization/steps/step-06-load/" style="text-decoration: none;">
    <strong>Next: Step 6 - Load into Runtime →</strong>
  </a>
</div>
