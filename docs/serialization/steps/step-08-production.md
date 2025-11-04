---
title: 'Step 8: Production Setup'
description: Deploy and operate serialization in production environments
---


## Overview

The final step is setting up serialization for production use. This includes deployment strategies, monitoring, rollback procedures, and operational best practices.

::: tip
**Time estimate:** 45 minutes

**Goal:** Production-ready serialization infrastructure
:::

## Deployment Strategies

### Strategy 1: Embedded Configuration (Simple)

Bundle JSON with your application:

```kotlin
// Place JSON in resources
src/main/resources/
  └── flags.json

// Load at startup
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        loadEmbeddedFlags()
    }

    private fun loadEmbeddedFlags() {
        ConditionalRegistry.registerEnum<FeatureFlags>()

        val json = javaClass.getResourceAsStream("/flags.json")
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: throw IllegalStateException("flags.json not found")

        val konfig = SnapshotSerializer.default.deserialize(json)
        Flags.load(konfig)
    }
}
```

**Pros:** Simple, no network dependency, guaranteed availability

**Cons:** Requires rebuild to change, larger APK/JAR

### Strategy 2: Remote Configuration (Dynamic)

Load from a CDN or configuration service:

```kotlin
class FlagLoader(
    private val cdnUrl: String,
    private val cacheDir: File
) {
    suspend fun loadFlags(): Result<Flags.Snapshot> = withContext(Dispatchers.IO) {
        try {
            // Try remote first
            val remoteSnapshot = downloadRemoteFlags()
            cacheSnapshot(remoteSnapshot)
            Result.success(remoteSnapshot)
        } catch (e: IOException) {
            // Fallback to cache
            logger.warn("Remote load failed, using cache", e)
            try {
                val cachedSnapshot = loadCachedFlags()
                Result.success(cachedSnapshot)
            } catch (e: Exception) {
                // Last resort: embedded
                logger.error("Cache failed, using embedded", e)
                Result.success(loadEmbeddedFlags())
            }
        }
    }

    private fun downloadRemoteFlags(): Flags.Snapshot {
        val url = "$cdnUrl/flags-${BuildConfig.ENVIRONMENT}.json"
        val json = URL(url).readText()

        ConditionalRegistry.registerEnum<FeatureFlags>()
        return SnapshotSerializer.default.deserialize(json)
    }

    private fun cacheSnapshot(konfig: Flags.Snapshot) {
        val json = SnapshotSerializer.default.serialize(konfig)
        File(cacheDir, "flags.json").writeText(json)
    }

    private fun loadCachedFlags(): Flags.Snapshot {
        val json = File(cacheDir, "flags.json").readText()

        ConditionalRegistry.registerEnum<FeatureFlags>()
        return SnapshotSerializer.default.deserialize(json)
    }

    private fun loadEmbeddedFlags(): Flags.Snapshot {
        val json = javaClass.getResourceAsStream("/default-flags.json")
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: throw IllegalStateException("Embedded flags not found")

        ConditionalRegistry.registerEnum<FeatureFlags>()
        return SnapshotSerializer.default.deserialize(json)
    }
}
```

**Pros:** Update without rebuild, environment-specific configs, A/B testing

**Cons:** Network dependency, requires caching, complex error handling

### Strategy 3: Hybrid (Recommended)

Combine both approaches for resilience:

```kotlin
class ProductionFlagLoader(
    private val context: Context,
    private val remoteUrl: String
) {
    suspend fun initialize() {
        // Load embedded flags immediately
        loadEmbeddedFlags()

        // Update from remote in background
        updateFromRemote()
    }

    private fun loadEmbeddedFlags() {
        val json = context.assets.open("flags.json")
            .bufferedReader()
            .use { it.readText() }

        ConditionalRegistry.registerEnum<FeatureFlags>()

        val konfig = SnapshotSerializer.default.deserialize(json)
        Flags.load(konfig)

        logger.info("Embedded flags loaded")
    }

    private suspend fun updateFromRemote() = withContext(Dispatchers.IO) {
        try {
            val json = downloadWithRetry(remoteUrl, maxRetries = 3)

            val konfig = SnapshotSerializer.default.deserialize(json)

            // Validate before loading
            validateSnapshot(konfig)

            Flags.load(konfig)

            // Cache for next launch
            cacheJson(json)

            logger.info("Remote flags loaded successfully")
        } catch (e: Exception) {
            logger.warn("Remote update failed, keeping embedded config", e)
            // App continues with embedded config
        }
    }

    private fun validateSnapshot(konfig: Flags.Snapshot) {
        require(konfig.flags.isNotEmpty()) {
            "Snapshot must contain flags"
        }

        // Add custom validation
        val requiredFlags = setOf("dark_mode", "critical_feature")
        val actualFlags = konfig.flags.keys.map { it.key }.toSet()

        require(requiredFlags.all { it in actualFlags }) {
            "Missing required flags"
        }
    }
}
```

## CI/CD Integration

### GitHub Actions Example

```yaml title=".github/workflows/deploy-flags.yml"
name: Deploy Feature Flags

on:
  push:
    paths:
      - 'config/production-flags.json'
    branches:
      - main

jobs:
  deploy:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK
        uses: actions/setup-java@v3
        with:
          java-version: '17'

      - name: Validate configuration
        run: ./gradlew test --tests FlagValidationTest

      - name: Deploy to CDN
        run: |
          aws s3 cp config/production-flags.json \
            s3://${{ secrets.CDN_BUCKET }}/flags-production.json \
            --cache-control "max-age=300"

      - name: Invalidate CDN cache
        run: |
          aws cloudfront create-invalidation \
            --distribution-id ${{ secrets.CDN_DISTRIBUTION_ID }} \
            --paths "/flags-production.json"
```

### Validation in CI

```kotlin title="FlagValidationTest.kt"
class FlagValidationTest {

    @Test
    fun `production configuration is valid`() {
        val json = File("config/production-flags.json").readText()

        ConditionalRegistry.registerEnum<FeatureFlags>()

        // Should deserialize without errors
        val konfig = SnapshotSerializer.default.deserialize(json)

        // Validate structure
        assertTrue(konfig.flags.isNotEmpty())

        // Test evaluation doesn't crash
        testEvaluation(konfig)
    }

    @Test
    fun `all environments have valid configs`() {
        val environments = listOf("development", "staging", "production")

        environments.forEach { env ->
            val json = File("config/$env-flags.json").readText()

            ConditionalRegistry.registerEnum<FeatureFlags>()

            val konfig = SnapshotSerializer.default.deserialize(json)

            assertNotNull(konfig, "$env configuration should be valid")

            // Environment-specific assertions
            when (env) {
                "production" -> assertProductionSafe(konfig)
                "development" -> assertDevelopmentComplete(konfig)
            }

            ConditionalRegistry.clear()
        }
    }

    private fun assertProductionSafe(konfig: Flags.Snapshot) {
        // No debug flags enabled
        Flags.load(konfig)
        val context = createTestContext()

        with(Flags) {
            assertFalse(
                context.evaluate(FeatureFlags.DEBUG_MENU),
                "Debug menu must be disabled in production"
            )
        }
    }
}
```

## Monitoring

### Application Metrics

```kotlin
class FlagMetrics(private val metricsClient: MetricsClient) {

    fun recordFlagLoad(success: Boolean, source: String, durationMs: Long) {
        metricsClient.count("flag.load", 1, mapOf(
            "success" to success.toString(),
            "source" to source
        ))

        if (success) {
            metricsClient.histogram("flag.load.duration_ms", durationMs)
        }
    }

    fun recordFlagEvaluation(flagKey: String, value: Any) {
        metricsClient.count("flag.evaluation", 1, mapOf(
            "flag" to flagKey,
            "value" to value.toString()
        ))
    }

    fun recordError(operation: String, error: Exception) {
        metricsClient.count("flag.error", 1, mapOf(
            "operation" to operation,
            "error_type" to error::class.simpleName.orEmpty()
        ))
    }
}

// Usage
class InstrumentedFlagLoader(
    private val flagLoader: FlagLoader,
    private val metrics: FlagMetrics
) {
    suspend fun loadFlags(): Result<Flags.Snapshot> {
        val startTime = System.currentTimeMillis()

        return try {
            val result = flagLoader.loadFlags()
            val duration = System.currentTimeMillis() - startTime

            metrics.recordFlagLoad(
                success = result.isSuccess,
                source = "remote",
                durationMs = duration
            )

            result
        } catch (e: Exception) {
            metrics.recordError("load", e)
            throw e
        }
    }
}
```

### Logging

```kotlin
class FlagLogger {
    private val logger = LoggerFactory.getLogger(FlagLogger::class.java)

    fun logLoad(konfig: Flags.Snapshot, source: String) {
        logger.info(
            "Loaded {} flags from {}",
            konfig.flags.size,
            source
        )

        if (logger.isDebugEnabled) {
            konfig.flags.keys.forEach { flag ->
                logger.debug("Flag loaded: {}", (flag as Conditional<*, *>).key)
            }
        }
    }

    fun logEvaluation(context: Context, flag: Conditional<*, *>, value: Any) {
        logger.debug(
            "Evaluated flag {} for user {} (platform: {}, version: {}): {}",
            flag.key,
            context.stableId.id.take(8),
            context.platform,
            context.appVersion,
            value
        )
    }

    fun logError(operation: String, error: Exception) {
        logger.error("Flag operation failed: {}", operation, error)
    }

    fun logReload(oldSize: Int, newSize: Int) {
        logger.info(
            "Reloaded flags: {} -> {} flags ({} change)",
            oldSize,
            newSize,
            if (newSize > oldSize) "+${newSize - oldSize}" else "${newSize - oldSize}"
        )
    }
}
```

## Rollback Procedures

### Version Your Configurations

```kotlin
data class VersionedConfiguration(
    val version: String,
    val timestamp: Long,
    val konfig: Flags.Snapshot
)

class ConfigurationVersionManager(private val cacheDir: File) {

    fun save(konfig: Flags.Snapshot) {
        val version = generateVersion()
        val versioned = VersionedConfiguration(
            version = version,
            timestamp = System.currentTimeMillis(),
            konfig = konfig
        )

        // Save current version
        saveVersion(versioned, "current")

        // Archive for rollback
        saveVersion(versioned, version)

        // Clean old versions (keep last 10)
        cleanOldVersions()
    }

    fun rollback(toVersion: String? = null): Flags.Snapshot {
        return if (toVersion != null) {
            loadVersion(toVersion).konfig
        } else {
            loadPreviousVersion().konfig
        }
    }

    private fun generateVersion(): String {
        val timestamp = System.currentTimeMillis()
        return "v$timestamp"
    }

    private fun saveVersion(config: VersionedConfiguration, name: String) {
        val json = SnapshotSerializer.default.serialize(config.konfig)
        val metadata = """
            {
              "version": "${config.version}",
              "timestamp": ${config.timestamp}
            }
        """.trimIndent()

        File(cacheDir, "$name.json").writeText(json)
        File(cacheDir, "$name.meta.json").writeText(metadata)
    }

    private fun loadPreviousVersion(): VersionedConfiguration {
        // Implementation to find previous version
        val versions = listVersions().sortedByDescending { it.timestamp }
        return versions[1] // Get second most recent (current is first)
    }
}
```

### Emergency Rollback

```kotlin
object EmergencyFlagControl {

    fun rollbackToDefault() {
        logger.warn("Emergency rollback initiated")

        val defaultSnapshot = ConfigBuilder.buildSnapshot {
            // Safe defaults for all flags
            FeatureFlags.values().forEach { flag ->
                flag with { default(false) }
            }
        }

        Flags.load(defaultSnapshot)

        logger.info("Rolled back to safe defaults")
    }

    fun disableFlag(flag: Conditional<*, *>) {
        logger.warn("Emergency disable: ${flag.key}")

        // Create patch to disable
        val patch = SerializablePatch(
            flags = listOf(
                SerializableFlag(
                    key = flag.key,
                    type = ValueType.BOOLEAN,
                    defaultValue = false
                )
            )
        )

        val current = getCurrentSnapshot()
        val updated = SnapshotSerializer.default.applyPatch(current, patch)
        Flags.load(updated)
    }
}
```

## Security Considerations

### Validate JSON Source

```kotlin
fun loadFlagsSecurely(url: String) {
    // Only load from trusted domains
    require(url.startsWith("https://cdn.yourcompany.com/")) {
        "Untrusted source: $url"
    }

    // Verify SSL certificate
    val json = downloadWithCertificatePin(url)

    // Validate signature
    verifySignature(json)

    // Load
    val konfig = SnapshotSerializer.default.deserialize(json)
    Flags.load(konfig)
}
```

### Prevent Injection

```kotlin
fun sanitizeConfiguration(konfig: Flags.Snapshot): Flags.Snapshot {
    // Ensure no malicious values
    konfig.flags.forEach { (key, entry) ->
        validateFlagEntry(key, entry)
    }

    return konfig
}

private fun validateFlagEntry(key: Conditional<*, *>, entry: Flags.FlagEntry<*, *>) {
    // Implement validation logic
    // - Check for suspicious patterns
    // - Validate value ranges
    // - Ensure rules are reasonable
}
```

## Congratulations!

You've completed the integration! Your serialization system is now production-ready.

<div style="display: flex; justify-content: space-between; margin-top: 2rem;">
  <a href="/serialization/steps/step-07-testing/" style="text-decoration: none;">
    <strong>← Previous: Step 7 - Testing</strong>
  </a>
  <a href="/serialization/runthrough/" style="text-decoration: none;">
    <strong>View Full Runthrough →</strong>
  </a>
</div>

## Quick Links

- [API Reference](../api.md) - Complete API documentation
- [Patch Updates](../../advanced/patch-updates.md) - Advanced patch techniques
- [Migration Guide](../../advanced/migration.md) - Migrating existing systems
