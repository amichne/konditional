---
title: 'Step 4: Serialize to JSON'
description: Convert your Flags.Snapshot to JSON format
---


## Overview

With a `Flags.Snapshot` created, you can now serialize it to JSON. This JSON can be committed to version control, deployed to a CDN, or stored in a configuration service.

::: tip
**Time estimate:** 10 minutes

**Output:** A JSON file containing your complete flag configuration
:::

## Basic Serialization

The `SnapshotSerializer` provides a simple API for serialization:

```kotlin
import io.amichne.konditional.serialization.SnapshotSerializer

// Create your configuration
val snapshot = ConfigBuilder.buildSnapshot {
    FeatureFlags.DARK_MODE with {
        default(true)
    }
}

// Serialize to JSON
val serializer = SnapshotSerializer.default
val json = serializer.serialize(snapshot)

println(json)
```

Output:

```json
{
  "flags": [
    {
      "key": "dark_mode",
      "type": "BOOLEAN",
      "defaultValue": true,
      "default": {
        "value": true,
        "type": "BOOLEAN"
      },
      "salt": "v1",
      "isActive": true,
      "rules": []
    }
  ]
}
```

## Saving to a File

Write the JSON to a file:

```kotlin
import java.io.File

fun saveConfiguration(snapshot: Flags.Snapshot, outputPath: String) {
    val json = SnapshotSerializer.default.serialize(snapshot)
    File(outputPath).writeText(json)
    println("Configuration saved to $outputPath")
}

// Usage
val snapshot = createProductionConfig()
saveConfiguration(snapshot, "config/production-flags.json")
```

## Pretty Printing

The serializer automatically formats JSON with indentation for readability:

```kotlin
val json = SnapshotSerializer.default.serialize(snapshot)
// Already pretty-printed with 2-space indentation!
```

If you need custom formatting:

```kotlin
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

// Create a custom serializer with 4-space indentation
val customMoshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

val customSerializer = SnapshotSerializer(customMoshi)
val adapter = customMoshi.adapter(SerializableSnapshot::class.java).indent("    ")

val snapshot = createConfig()
val serializableSnapshot = snapshot.toSerializable()
val json = adapter.toJson(serializableSnapshot)
```

## Serializing Complex Configurations

For the production config from Step 3:

```kotlin
fun serializeProductionConfig() {
    val snapshot = createProductionConfig()
    val json = SnapshotSerializer.default.serialize(snapshot)

    File("config/production-flags.json").writeText(json)
}
```

The resulting JSON will look like:

```json
{
  "flags": [
    {
      "key": "dark_mode",
      "type": "BOOLEAN",
      "defaultValue": true,
      "default": {
        "value": true,
        "type": "BOOLEAN"
      },
      "salt": "v1",
      "isActive": true,
      "rules": []
    },
    {
      "key": "new_onboarding",
      "type": "BOOLEAN",
      "defaultValue": false,
      "default": {
        "value": false,
        "type": "BOOLEAN"
      },
      "salt": "v1",
      "isActive": true,
      "rules": [
        {
          "value": {
            "value": true,
            "type": "BOOLEAN"
          },
          "rollout": 10.0,
          "note": null,
          "locales": ["EN_US"],
          "platforms": [],
          "versionRange": {
            "type": "UNBOUNDED"
          }
        }
      ]
    }
  ]
}
```

## Environment-Specific Serialization

Create separate JSON files for each environment:

```kotlin
fun serializeAllEnvironments() {
    // Development
    val devSnapshot = createDevelopmentConfig()
    File("config/dev-flags.json").writeText(
        SnapshotSerializer.default.serialize(devSnapshot)
    )

    // Staging
    val stagingSnapshot = createStagingConfig()
    File("config/staging-flags.json").writeText(
        SnapshotSerializer.default.serialize(stagingSnapshot)
    )

    // Production
    val prodSnapshot = createProductionConfig()
    File("config/production-flags.json").writeText(
        SnapshotSerializer.default.serialize(prodSnapshot)
    )

    println("All configurations serialized successfully")
}
```

## Automating Serialization

### Gradle Task

Create a Gradle task to generate configuration files:

```kotlin title="build.gradle.kts"
tasks.register("generateFlagConfigs") {
    doLast {
        // Use reflection or source generation to run your config builder
        exec {
            mainClass.set("com.yourapp.ConfigGeneratorKt")
            classpath = sourceSets["main"].runtimeClasspath
        }
    }
}
```

Then create the generator:

```kotlin title="ConfigGenerator.kt"
package com.yourapp

fun main() {
    println("Generating flag configurations...")

    val environments = mapOf(
        "development" to createDevelopmentConfig(),
        "staging" to createStagingConfig(),
        "production" to createProductionConfig()
    )

    environments.forEach { (env, snapshot) ->
        val outputFile = File("config/$env-flags.json")
        outputFile.parentFile.mkdirs()

        val json = SnapshotSerializer.default.serialize(snapshot)
        outputFile.writeText(json)

        println("✓ Generated $env configuration (${outputFile.absolutePath})")
    }

    println("Done!")
}
```

Run it:

```bash
./gradlew generateFlagConfigs
```

## Validation

Before committing JSON to version control, validate it:

```kotlin
fun validateSerializedConfig(jsonPath: String): Boolean {
    return try {
        val json = File(jsonPath).readText()

        // Attempt to deserialize
        ConditionalRegistry.registerEnum<FeatureFlags>()
        val snapshot = SnapshotSerializer.default.deserialize(json)

        // Check basic properties
        require(snapshot.flags.isNotEmpty()) {
            "Configuration must contain at least one flag"
        }

        // Optionally: test some evaluations
        testConfiguration(snapshot)

        println("✓ Configuration is valid")
        true
    } catch (e: Exception) {
        println("✗ Validation failed: ${e.message}")
        e.printStackTrace()
        false
    }
}

fun testConfiguration(snapshot: Flags.Snapshot) {
    Flags.load(snapshot)

    val testContext = Context(
        AppLocale.EN_US,
        Platform.IOS,
        Version.of(2, 0, 0),
        StableId.of("test-user")
    )

    with(Flags) {
        // Verify critical flags evaluate without error
        testContext.evaluate(FeatureFlags.DARK_MODE)
        testContext.evaluate(FeatureFlags.NEW_ONBOARDING)
    }
}
```

## Version Control

Add generated JSON to version control:

```gitignore title=".gitignore"
# Don't ignore configuration files!
# config/*.json
```

Commit your configurations:

```bash
git add config/production-flags.json
git add config/staging-flags.json
git commit -m "Update feature flag configuration"
```

## CI/CD Integration

In your CI pipeline, validate configurations:

```yaml title=".github/workflows/validate-flags.yml"
name: Validate Flag Configurations

on: [push, pull_request]

jobs:
  validate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK
        uses: actions/setup-java@v3
        with:
          java-version: '17'

      - name: Validate configurations
        run: ./gradlew test --tests FlagValidationTest
```

## What's Next?

Now you have JSON files ready to be deserialized. In the next step, you'll learn how to load them back into your application.

<div style="display: flex; justify-content: space-between; margin-top: 2rem;">
  <a href="/serialization/steps/step-03-configuration/" style="text-decoration: none;">
    <strong>← Previous: Step 3 - Create Configuration</strong>
  </a>
  <a href="/serialization/steps/step-05-deserialize/" style="text-decoration: none;">
    <strong>Next: Step 5 - Deserialize →</strong>
  </a>
</div>
