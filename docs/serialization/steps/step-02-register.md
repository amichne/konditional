---
title: 'Step 2: Register Your Flags'
description: Register Conditional instances with the ConditionalRegistry
---


## Overview

Before deserializing JSON, you must register all your `Conditional` instances with the `ConditionalRegistry`. This mapping allows the deserializer to convert string keys back into typed `Conditional` references.

::: tip
**Time estimate:** 10 minutes

**Why?** The registry solves the problem of mapping string keys in JSON to strongly-typed Kotlin objects.
:::

## Understanding the Registry

When you serialize a flag, it becomes a string key in JSON:

```kotlin
// In Kotlin
enum class FeatureFlags(override val key: String) : Conditional<Boolean, Context> {
    DARK_MODE("dark_mode")
}

// In JSON
{
  "key": "dark_mode",  // Just a string!
  ...
}
```

When deserializing, we need to map `"dark_mode"` back to `FeatureFlags.DARK_MODE`. The registry handles this mapping.

## Registration Methods

### Method 1: Register an Entire Enum (Recommended)

If your flags are defined as an enum (the typical pattern), register the whole enum at once:

```kotlin
import io.amichne.konditional.serialization.ConditionalRegistry

// Register all flags in the enum
ConditionalRegistry.registerEnum<FeatureFlags>()
```

This registers every enum constant automatically.

### Method 2: Register Individual Flags

For non-enum flags or selective registration:

```kotlin
// Register specific flags
ConditionalRegistry.register(FeatureFlags.DARK_MODE)
ConditionalRegistry.register(FeatureFlags.NEW_ONBOARDING)
```

### Method 3: Register Multiple Enums

For projects with multiple flag enums:

```kotlin
// Register all your flag enums
ConditionalRegistry.registerEnum<FeatureFlags>()
ConditionalRegistry.registerEnum<ExperimentFlags>()
ConditionalRegistry.registerEnum<DebugFlags>()
```

## When to Register

::: caution
**Critical:** Register flags **before** calling `deserialize()`. Registration after deserialization will fail.
:::

### Application Startup (Recommended)

Register flags as early as possible in your application lifecycle:


=== "Tab"

  
=== "Android"

    ```kotlin title="MyApplication.kt"
    import android.app.Application
    import io.amichne.konditional.serialization.ConditionalRegistry

    class MyApplication : Application() {
        override fun onCreate() {
            super.onCreate()

            // Register flags first thing
            registerFeatureFlags()

            // Then load configuration
            loadFlagConfiguration()

            // Rest of initialization...
        }

        private fun registerFeatureFlags() {
            ConditionalRegistry.registerEnum<FeatureFlags>()
            ConditionalRegistry.registerEnum<ExperimentFlags>()
        }
    }
    ```
  

  
=== "Spring Boot"

    ```kotlin title="FlagConfiguration.kt"
    import org.springframework.context.annotation.Configuration
    import javax.annotation.PostConstruct

    @Configuration
    class FlagConfiguration {

        @PostConstruct
        fun registerFlags() {
            ConditionalRegistry.registerEnum<FeatureFlags>()
            ConditionalRegistry.registerEnum<ExperimentFlags>()
        }
    }
    ```
  

  
=== "Ktor"

    ```kotlin title="Application.kt"
    import io.ktor.server.application.*

    fun Application.module() {
        // Register flags before anything else
        ConditionalRegistry.registerEnum<FeatureFlags>()

        // Configure routes, etc.
        configureRouting()
    }
    ```
  

  
=== "Main Function"

    ```kotlin title="Main.kt"
    fun main() {
        // Register before doing anything with flags
        ConditionalRegistry.registerEnum<FeatureFlags>()

        // Load configuration
        val config = loadConfiguration()

        // Start application
        runApplication(config)
    }
    ```
  


## Create a Registration Helper

For larger projects, create a dedicated registration class:

```kotlin title="FlagRegistration.kt"
import io.amichne.konditional.serialization.ConditionalRegistry

/**
 * Centralized registration of all feature flags.
 * Call [registerAll] once during application startup.
 */
object FlagRegistration {

    private var registered = false

    /**
     * Registers all feature flags with the ConditionalRegistry.
     * Safe to call multiple times - will only register once.
     */
    fun registerAll() {
        if (registered) {
            return
        }

        // Register all flag enums
        ConditionalRegistry.registerEnum<FeatureFlags>()
        ConditionalRegistry.registerEnum<ExperimentFlags>()
        ConditionalRegistry.registerEnum<DebugFlags>()

        registered = true
    }

    /**
     * Clears all registrations. Only for testing!
     */
    fun clearForTesting() {
        if (isTestEnvironment()) {
            ConditionalRegistry.clear()
            registered = false
        }
    }

    private fun isTestEnvironment(): Boolean {
        return try {
            Class.forName("org.junit.jupiter.api.Test")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
}
```

Usage:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FlagRegistration.registerAll()
        // ...
    }
}
```

## Testing with the Registry

In tests, you need to register and clean up properly:

```kotlin title="FlagSerializationTest.kt"
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FlagSerializationTest {

    @BeforeEach
    fun setUp() {
        // Register flags before each test
        ConditionalRegistry.registerEnum<FeatureFlags>()
    }

    @AfterEach
    fun tearDown() {
        // Clean up after each test to avoid pollution
        ConditionalRegistry.clear()
    }

    @Test
    fun `test flag serialization`() {
        // Test code here - flags are registered
        val snapshot = createTestSnapshot()
        val json = SnapshotSerializer.default.serialize(snapshot)

        // This will work because flags are registered
        val deserialized = SnapshotSerializer.default.deserialize(json)
    }
}
```

## Verification

Verify your registration is working:

```kotlin
import io.amichne.konditional.serialization.ConditionalRegistry

fun verifyRegistration() {
    // Register
    ConditionalRegistry.registerEnum<FeatureFlags>()

    // Check if specific keys are registered
    val isDarkModeRegistered = ConditionalRegistry.contains("dark_mode")
    val isNewOnboardingRegistered = ConditionalRegistry.contains("new_onboarding")

    println("Dark mode registered: $isDarkModeRegistered")
    println("New onboarding registered: $isNewOnboardingRegistered")

    // You can also retrieve a flag
    val darkMode: Conditional<Boolean, Context> = ConditionalRegistry.get("dark_mode")
    println("Retrieved flag: ${darkMode.key}")
}
```

## Common Mistakes

### ❌ Deserializing Before Registering

```kotlin
// This will crash!
val snapshot = SnapshotSerializer.default.deserialize(json)
ConditionalRegistry.registerEnum<FeatureFlags>()
// IllegalArgumentException: Conditional with key 'dark_mode' not found
```

### ✅ Correct Order

```kotlin
// Register first
ConditionalRegistry.registerEnum<FeatureFlags>()
// Then deserialize
val snapshot = SnapshotSerializer.default.deserialize(json)
```

### ❌ Forgetting Some Flags

```kotlin
// Only registered FeatureFlags
ConditionalRegistry.registerEnum<FeatureFlags>()

// JSON contains ExperimentFlags - will crash!
val json = """{"flags": [{"key": "experiment_new_ui", ...}]}"""
val snapshot = SnapshotSerializer.default.deserialize(json)
```

### ✅ Register All Used Flags

```kotlin
// Register all flag types you use
ConditionalRegistry.registerEnum<FeatureFlags>()
ConditionalRegistry.registerEnum<ExperimentFlags>()

// Now both types can be deserialized
val snapshot = SnapshotSerializer.default.deserialize(json)
```

## What's Next?

With flags registered, you can now create your first serializable configuration.

<div style="display: flex; justify-content: space-between; margin-top: 2rem;">
  <a href="/serialization/steps/step-01-dependencies/" style="text-decoration: none;">
    <strong>← Previous: Step 1 - Dependencies</strong>
  </a>
  <a href="/serialization/steps/step-03-configuration/" style="text-decoration: none;">
    <strong>Next: Step 3 - Create Configuration →</strong>
  </a>
</div>
