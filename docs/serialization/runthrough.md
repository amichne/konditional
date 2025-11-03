---
title: Complete Integration Runthrough
description: Full end-to-end integration guide in one place
---


## Introduction

This page provides a complete, end-to-end integration guide for Konditional serialization. Follow these instructions from start to finish to integrate serialization into your application.

::: tip
**Estimated time:** 2-3 hours for full integration

**Prerequisites:**
- Existing Kotlin project with Gradle
- Konditional library already integrated
- Feature flags defined as `Conditional` instances
:::

---

## Part 1: Project Setup

### Add Dependencies

First, add Moshi to your `build.gradle.kts`:

```kotlin title="build.gradle.kts" {14-16}
repositories {
    mavenCentral()
}

dependencies {
    // Konditional
    implementation("io.amichne:konditional:1.0.0")

    // Moshi for JSON serialization
    implementation("com.squareup.moshi:moshi:1.15.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
    implementation("com.squareup.moshi:moshi-adapters:1.15.0")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
}
```

Sync your project:

```bash
./gradlew build
```

### Create Flag Registration Helper

Create a centralized registration class:

```kotlin title="FlagRegistration.kt"
package com.yourapp.flags

import io.amichne.konditional.serialization.ConditionalRegistry

object FlagRegistration {
    private var registered = false

    fun registerAll() {
        if (registered) return

        synchronized(this) {
            if (registered) return

            // Register all your flag enums
            ConditionalRegistry.registerEnum<FeatureFlags>()
            ConditionalRegistry.registerEnum<ExperimentFlags>()
            // Add more as needed

            registered = true
        }
    }

    fun clearForTesting() {
        ConditionalRegistry.clear()
        registered = false
    }
}
```

---

## Part 2: Configuration Creation

### Define Your Flags

Example flag enum:

```kotlin title="FeatureFlags.kt"
package com.yourapp.flags

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Conditional
import io.amichne.konditional.builders.FlagBuilder

enum class FeatureFlags(override val key: String) : Conditional<Boolean, Context> {
    DARK_MODE("dark_mode"),
    NEW_ONBOARDING("new_onboarding"),
    COMPACT_CARDS("compact_cards"),
    PREMIUM_FEATURE("premium_feature");

}
```

### Create Configuration Builder

Create environment-specific configurations:

```kotlin title="FlagConfigurations.kt"
package com.yourapp.flags

import io.amichne.konditional.builders.ConfigBuilder
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Rollout
import io.amichne.konditional.core.SingletonFlagRegistry

object FlagConfigurations {

    fun createDevelopmentConfig() = ConfigBuilder.buildSnapshot {
        FeatureFlags.DARK_MODE with {
            default(true)
        }

        FeatureFlags.NEW_ONBOARDING with {
            default(true)
        }

        FeatureFlags.COMPACT_CARDS with {
            default(true)
        }

        FeatureFlags.PREMIUM_FEATURE with {
            default(true)
        }
    }

    fun createStagingConfig() = ConfigBuilder.buildSnapshot {
        FeatureFlags.DARK_MODE with {
            default(true)
        }

        FeatureFlags.NEW_ONBOARDING with {
            default(false)
            rule {
                rollout = Rollout.of(50.0)
                locales(AppLocale.EN_US)
            }.implies(true)
        }

        FeatureFlags.COMPACT_CARDS with {
            default(true)
        }

        FeatureFlags.PREMIUM_FEATURE with {
            default(false)
            rule {
                platforms(Platform.IOS, Platform.ANDROID)
            }.implies(true)
        }
    }

    fun createProductionConfig() = ConfigBuilder.buildSnapshot {
        FeatureFlags.DARK_MODE with {
            default(true)
        }

        FeatureFlags.NEW_ONBOARDING with {
            default(false)
            rule {
                rollout = Rollout.of(10.0)
                locales(AppLocale.EN_US)
                note = "JIRA-456: Gradual rollout for US users"
            }.implies(true)
        }

        FeatureFlags.COMPACT_CARDS with {
            default(false)
            rule {
                platforms(Platform.IOS, Platform.ANDROID)
                versions {
                    min(2, 0, 0)
                }
            }.implies(true)
        }

        FeatureFlags.PREMIUM_FEATURE with {
            default(false)
            rule {
                platforms(Platform.IOS)
                versions {
                    min(2, 1, 0)
                }
            }.implies(true)
        }
    }
}
```

### Generate JSON Files

Create a configuration generator:

```kotlin title="ConfigGenerator.kt"
package com.yourapp.tools

import com.yourapp.flags.FlagConfigurations
import com.yourapp.flags.FlagRegistration
import io.amichne.konditional.serialization.SnapshotSerializer
import java.io.File

fun main() {
    println("Generating feature flag configurations...")

    // Register flags
    FlagRegistration.registerAll()

    val serializer = SnapshotSerializer.default

    // Create config directory
    val configDir = File("config")
    configDir.mkdirs()

    // Generate development config
    val devSnapshot = FlagConfigurations.createDevelopmentConfig()
    val devJson = serializer.serialize(devSnapshot)
    File(configDir, "development-flags.json").writeText(devJson)
    println("✓ Generated development configuration")

    // Generate staging config
    val stagingSnapshot = FlagConfigurations.createStagingConfig()
    val stagingJson = serializer.serialize(stagingSnapshot)
    File(configDir, "staging-flags.json").writeText(stagingJson)
    println("✓ Generated staging configuration")

    // Generate production config
    val productionSnapshot = FlagConfigurations.createProductionConfig()
    val productionJson = serializer.serialize(productionSnapshot)
    File(configDir, "production-flags.json").writeText(productionJson)
    println("✓ Generated production configuration")

    println("\nAll configurations generated successfully!")
}
```

Add a Gradle task:

```kotlin title="build.gradle.kts"
tasks.register<JavaExec>("generateFlagConfigs") {
    group = "flags"
    description = "Generate feature flag configuration files"
    mainClass.set("com.yourapp.tools.ConfigGeneratorKt")
    classpath = sourceSets["main"].runtimeClasspath
}
```

Run it:

```bash
./gradlew generateFlagConfigs
```

---

## Part 3: Application Integration

### Create Flag Loader

Build a robust flag loader with fallback:

```kotlin title="FlagLoader.kt"
package com.yourapp.flags

import android.content.Context
import io.amichne.konditional.core.SingletonFlagRegistry
import io.amichne.konditional.serialization.SnapshotSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

class FlagLoader(
    private val context: Context,
    private val environment: String
) {
    private val serializer = SnapshotSerializer.default

    suspend fun initialize() {
        // Register flags first
        FlagRegistration.registerAll()

        // Load embedded flags immediately (fast)
        loadEmbeddedFlags()

        // Update from remote in background (slow)
        updateFromRemote()
    }

    private fun loadEmbeddedFlags() {
        try {
            val json = context.assets.open("flags.json")
                .bufferedReader()
                .use { it.readText() }

            val snapshot = serializer.deserialize(json)
            Flags.load(snapshot)

            logger.info("Loaded embedded flags")
        } catch (e: Exception) {
            logger.error("Failed to load embedded flags", e)
            loadHardcodedDefaults()
        }
    }

    private suspend fun updateFromRemote() = withContext(Dispatchers.IO) {
        try {
            val url = "https://cdn.yourcompany.com/flags-$environment.json"
            val json = URL(url).readText()

            // Cache for offline use
            cacheJson(json)

            val snapshot = serializer.deserialize(json)
            Flags.load(snapshot)

            logger.info("Updated from remote configuration")
        } catch (e: Exception) {
            logger.warn("Remote update failed, using embedded config", e)
            // App continues with embedded config
        }
    }

    private fun cacheJson(json: String) {
        File(context.cacheDir, "flags.json").writeText(json)
    }

    private fun loadHardcodedDefaults() {
        // Last resort: safe defaults
        val defaultSnapshot = FlagConfigurations.createProductionConfig()
        Flags.load(defaultSnapshot)
        logger.warn("Loaded hardcoded defaults")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(FlagLoader::class.java)
    }
}
```

### Integrate with Application

```kotlin title="MyApplication.kt"
package com.yourapp

import android.app.Application
import com.yourapp.flags.FlagLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MyApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        // Load flags
        applicationScope.launch {
            val loader = FlagLoader(
                context = this@MyApplication,
                environment = BuildConfig.ENVIRONMENT
            )
            loader.initialize()
        }

        // Continue with other initialization...
    }
}
```

### Use Flags in UI

```kotlin title="MainActivity.kt"
package com.yourapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.yourapp.flags.FeatureFlags
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.SingletonFlagRegistry
import io.amichne.konditional.core.StableId

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create user context
        val userContext = createUserContext()

        // Evaluate flags
        with(Flags) {
            val darkMode = userContext.evaluate(FeatureFlags.DARK_MODE)
            val newOnboarding = userContext.evaluate(FeatureFlags.NEW_ONBOARDING)
            val compactCards = userContext.evaluate(FeatureFlags.COMPACT_CARDS)

            // Apply flags
            if (darkMode) {
                setTheme(R.style.DarkTheme)
            }

            if (compactCards) {
                setContentView(R.layout.activity_main_compact)
            } else {
                setContentView(R.layout.activity_main)
            }

            if (newOnboarding && isFirstLaunch()) {
                showOnboarding()
            }
        }
    }

    private fun createUserContext(): Context {
        return Context(
            locale = getCurrentLocale(),
            platform = Platform.ANDROID,
            appVersion = Version.of(
                BuildConfig.VERSION_MAJOR,
                BuildConfig.VERSION_MINOR,
                BuildConfig.VERSION_PATCH
            ),
            stableId = StableId.of(getUserId())
        )
    }

    private fun getCurrentLocale(): AppLocale {
        return when (resources.configuration.locales[0].language) {
            "en" -> AppLocale.EN_US
            "es" -> AppLocale.ES_US
            else -> AppLocale.EN_US
        }
    }

    private fun getUserId(): String {
        // Get stable user ID from your auth system
        return authManager.getUserId()
    }
}
```

---

## Part 4: Testing

### Unit Tests

```kotlin title="FlagSerializationTest.kt"
package com.yourapp.flags

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.SingletonFlagRegistry
import io.amichne.konditional.core.StableId
import io.amichne.konditional.serialization.ConditionalRegistry
import io.amichne.konditional.serialization.SnapshotSerializer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FlagSerializationTest {
    private val serializer = SnapshotSerializer.default

    @BeforeEach
    fun setUp() {
        FlagRegistration.registerAll()
    }

    @AfterEach
    fun tearDown() {
        FlagRegistration.clearForTesting()
    }

    @Test
    fun `round-trip serialization preserves behavior`() {
        val original = FlagConfigurations.createProductionConfig()
        val json = serializer.serialize(original)
        val restored = serializer.deserialize(json)

        assertNotNull(json)

        val context = createTestContext()

        with(Flags) {
            Flags.load(original)
            val originalValues = context.evaluate()

            Flags.load(restored)
            val restoredValues = context.evaluate()

            assertEquals(originalValues.size, restoredValues.size)
            originalValues.forEach { (key, value) ->
                assertEquals(value, restoredValues[key])
            }
        }
    }

    @Test
    fun `all environments are valid`() {
        val configs = mapOf(
            "development" to FlagConfigurations.createDevelopmentConfig(),
            "staging" to FlagConfigurations.createStagingConfig(),
            "production" to FlagConfigurations.createProductionConfig()
        )

        configs.forEach { (env, snapshot) ->
            val json = serializer.serialize(snapshot)
            val restored = serializer.deserialize(json)

            assertNotNull(restored, "$env config should be valid")
        }
    }

    private fun createTestContext() = Context(
        AppLocale.EN_US,
        Platform.ANDROID,
        Version.of(2, 0, 0),
        StableId.of("test-user-12345678")
    )
}
```

### Integration Tests

```kotlin title="FlagLoadingIntegrationTest.kt"
package com.yourapp.flags

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yourapp.flags.FeatureFlags
import io.amichne.konditional.core.SingletonFlagRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class FlagLoadingIntegrationTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun flagsLoadSuccessfully() = runBlocking {
        val loader = FlagLoader(context, "production")
        loader.initialize()

        // Verify flags are accessible
        val testContext = createTestContext()
        with(Flags) {
            val darkMode = testContext.evaluate(FeatureFlags.DARK_MODE)
            assertNotNull(darkMode)
        }
    }
}
```

---

## Part 5: CI/CD Setup

### GitHub Actions Workflow

```yaml title=".github/workflows/deploy-flags.yml"
name: Deploy Feature Flags

on:
  push:
    paths:
      - 'config/*.json'
    branches:
      - main

jobs:
  validate-and-deploy:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Validate configurations
        run: ./gradlew test --tests '*FlagSerializationTest*'

      - name: Deploy to S3
        run: |
          aws s3 cp config/production-flags.json \
            s3://${{ secrets.S3_BUCKET }}/flags-production.json \
            --cache-control "max-age=300"

      - name: Invalidate CloudFront
        run: |
          aws cloudfront create-invalidation \
            --distribution-id ${{ secrets.CLOUDFRONT_DIST_ID }} \
            --paths "/flags-production.json"
```

---

## Part 6: Monitoring

### Add Logging

```kotlin title="FlagLogger.kt"
package com.yourapp.flags

import org.slf4j.LoggerFactory

object FlagLogger {
    private val logger = LoggerFactory.getLogger("FeatureFlags")

    fun logLoad(numFlags: Int, source: String) {
        logger.info("Loaded $numFlags flags from $source")
    }

    fun logEvaluation(flag: String, value: Any, userId: String) {
        logger.debug("Flag $flag evaluated to $value for user ${userId.take(8)}")
    }

    fun logError(operation: String, error: Exception) {
        logger.error("Flag operation failed: $operation", error)
    }
}
```

### Add Metrics

```kotlin title="FlagMetrics.kt"
package com.yourapp.flags

class FlagMetrics(private val metricsClient: MetricsClient) {

    fun recordLoad(success: Boolean, source: String) {
        metricsClient.increment("flags.load", mapOf(
            "success" to success.toString(),
            "source" to source
        ))
    }

    fun recordEvaluation(flag: String, value: String) {
        metricsClient.increment("flags.evaluation", mapOf(
            "flag" to flag,
            "value" to value
        ))
    }
}
```

---

## Conclusion

You now have a complete, production-ready serialization integration! Your application can:

✅ Load feature flags from JSON
✅ Support multiple environments
✅ Handle errors gracefully with fallbacks
✅ Dynamically update flags from remote sources
✅ Monitor flag usage
✅ Test configurations thoroughly

## Next Steps

- **Explore advanced features:** [Patch Updates](/advanced/patch-updates/)
- **API reference:** [Complete API Documentation](/serialization/api/)
- **Migration guide:** [Migrating Existing Systems](/advanced/migration/)

::: tip
For questions or issues, check the [GitHub repository](https://github.com/amichne/konditional) or file an issue.
:::
