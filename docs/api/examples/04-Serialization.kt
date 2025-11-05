package examples

import io.amichne.konditional.builders.ConfigBuilder
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Rollout
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.Conditional
import io.amichne.konditional.core.FlagRegistry
import io.amichne.konditional.core.instance.KonfigPatch
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.serialization.SnapshotSerializer

/**
 * Example 1: Basic Serialization/Deserialization
 *
 * Serialize and deserialize a simple configuration.
 */
fun basicSerialization() {
    val FEATURE_FLAG: Conditional<Boolean, Context> =
        Conditional("feature_flag")

    // Build a configuration
    val konfig = ConfigBuilder.buildSnapshot {
        FEATURE_FLAG with {
            default(value = false)

            rule {
                platforms(Platform.IOS)
                rollout = Rollout.of(50.0)
            } implies true
        }
    }

    // Serialize to JSON
    val serializer = SnapshotSerializer.default
    val json = serializer.serialize(konfig)

    println("Serialized configuration:")
    println(json)

    // Deserialize back
    when (val result = serializer.deserialize(json)) {
        is ParseResult.Success -> {
            println("\nDeserialization successful!")
            println("Flags in snapshot: ${result.value.flags.size}")
        }
        is ParseResult.Failure -> {
            println("\nDeserialization failed: ${result.error}")
        }
    }
}

/**
 * Example 2: Error Handling
 *
 * Handle different types of parse errors.
 */
fun errorHandling() {
    val serializer = SnapshotSerializer.default

    // Test 1: Invalid JSON
    val invalidJson = "{ not valid json }"
    when (val result = serializer.deserialize(invalidJson)) {
        is ParseResult.Success -> println("Unexpected success")
        is ParseResult.Failure -> when (val error = result.error) {
            is ParseError.InvalidJson -> {
                println("Invalid JSON detected: ${error.message}")
            }
            else -> println("Other error: $error")
        }
    }

    // Test 2: Empty JSON
    val emptyJson = "{}"
    when (val result = serializer.deserialize(emptyJson)) {
        is ParseResult.Success -> println("Parsed empty config")
        is ParseResult.Failure -> {
            println("Failed to parse empty: ${result.error}")
        }
    }

    // Test 3: Valid JSON
    val validJson = """
        {
          "flags": []
        }
    """.trimIndent()
    when (val result = serializer.deserialize(validJson)) {
        is ParseResult.Success -> {
            println("Successfully parsed valid JSON with ${result.value.flags.size} flags")
        }
        is ParseResult.Failure -> {
            println("Unexpected failure: ${result.error}")
        }
    }
}

/**
 * Example 3: Patch Serialization
 *
 * Create and serialize configuration patches.
 */
fun patchSerialization() {
    val FLAG_A: Conditional<Boolean, Context> = Conditional("flag_a")
    val FLAG_B: Conditional<String, Context> = Conditional("flag_b")
    val FLAG_C: Conditional<Int, Context> = Conditional("flag_c")

    // Create initial configuration
    val initialKonfig = ConfigBuilder.buildSnapshot {
        FLAG_A with { default(value = false) }
        FLAG_B with { default(value = "old") }
    }

    FlagRegistry.load(initialKonfig)

    // Create a patch
    val patch = KonfigPatch.patch {
        // Update FLAG_B
        add(ConfigBuilder.buildSnapshot {
            FLAG_B with { default(value = "new") }
        }.flags[FLAG_B]!!)

        // Add FLAG_C
        add(ConfigBuilder.buildSnapshot {
            FLAG_C with { default(value = 42) }
        }.flags[FLAG_C]!!)
    }

    // Serialize the patch
    val serializer = SnapshotSerializer.default
    val patchJson = serializer.serializePatch(patch)

    println("Serialized patch:")
    println(patchJson)

    // Deserialize and apply
    when (val result = serializer.deserializePatchToCore(patchJson)) {
        is ParseResult.Success -> {
            FlagRegistry.update(result.value)
            println("\nPatch applied successfully!")
            println("Flags in registry: ${FlagRegistry.allFlags().size}")
        }
        is ParseResult.Failure -> {
            println("\nPatch deserialization failed: ${result.error}")
        }
    }
}

/**
 * Example 4: Remote Configuration Loading
 *
 * Simulate loading configuration from a remote source.
 */
fun remoteConfigLoading() {
    // Simulate fetching from remote server
    fun fetchRemoteConfig(): String {
        return """
            {
              "flags": [
                {
                  "key": "remote_feature",
                  "defaultValue": {
                    "type": "boolean",
                    "value": true
                  },
                  "isActive": true,
                  "salt": "v1",
                  "values": [
                    {
                      "rule": {
                        "rollout": 100.0,
                        "baseEvaluable": {
                          "platforms": ["IOS", "ANDROID"]
                        }
                      },
                      "value": {
                        "type": "boolean",
                        "value": true
                      }
                    }
                  ]
                }
              ]
            }
        """.trimIndent()
    }

    val serializer = SnapshotSerializer.default

    println("Fetching remote configuration...")
    val json = fetchRemoteConfig()

    when (val result = serializer.deserialize(json)) {
        is ParseResult.Success -> {
            FlagRegistry.load(result.value)
            println("Remote configuration loaded successfully!")
            println("Loaded ${result.value.flags.size} flags")
        }
        is ParseResult.Failure -> {
            println("Failed to load remote configuration: ${result.error}")
        }
    }
}

/**
 * Example 5: Graceful Degradation
 *
 * Implement fallback behavior when remote config fails.
 */
fun gracefulDegradation() {
    val FEATURE_FLAG: Conditional<Boolean, Context> =
        Conditional("feature_flag")

    // Embedded fallback configuration
    val fallbackConfig = ConfigBuilder.buildSnapshot {
        FEATURE_FLAG with {
            default(value = false)
        }
    }

    fun loadWithFallback(remoteJson: String?) {
        val serializer = SnapshotSerializer.default

        if (remoteJson == null) {
            println("No remote config available, using fallback")
            FlagRegistry.load(fallbackConfig)
            return
        }

        when (val result = serializer.deserialize(remoteJson)) {
            is ParseResult.Success -> {
                FlagRegistry.load(result.value)
                println("Remote configuration loaded")
            }
            is ParseResult.Failure -> {
                println("Remote config failed (${result.error}), using fallback")
                FlagRegistry.load(fallbackConfig)
            }
        }
    }

    // Test with null (network error)
    println("=== Scenario 1: Network Error ===")
    loadWithFallback(null)

    // Test with invalid JSON
    println("\n=== Scenario 2: Invalid JSON ===")
    loadWithFallback("{ invalid }")

    // Test with valid JSON
    println("\n=== Scenario 3: Valid Config ===")
    loadWithFallback("""
        {
          "flags": [
            {
              "key": "feature_flag",
              "defaultValue": {
                "type": "boolean",
                "value": true
              },
              "isActive": true,
              "salt": "v1",
              "values": []
            }
          ]
        }
    """.trimIndent())
}

/**
 * Example 6: Configuration Versioning
 *
 * Track and manage different versions of configuration.
 */
fun configurationVersioning() {
    data class VersionedSnapshot(
        val version: String,
        val timestamp: Long,
        val json: String
    )

    val versions = mutableListOf<VersionedSnapshot>()

    fun saveVersion(label: String) {
        val konfig = FlagRegistry.konfig()
        val json = SnapshotSerializer.default.serialize(konfig)

        versions.add(VersionedSnapshot(
            version = label,
            timestamp = System.currentTimeMillis(),
            json = json
        ))

        println("Saved version: $label")
    }

    fun rollbackTo(version: String) {
        val snapshot = versions.find { it.version == version }
            ?: run {
                println("Version $version not found")
                return
            }

        when (val result = SnapshotSerializer.default.deserialize(snapshot.json)) {
            is ParseResult.Success -> {
                FlagRegistry.load(result.value)
                println("Rolled back to version $version")
            }
            is ParseResult.Failure -> {
                println("Rollback failed: ${result.error}")
            }
        }
    }

    val FEATURE: Conditional<String, Context> = Conditional("feature")

    // Version 1
    ConfigBuilder.config {
        FEATURE with { default(value = "v1") }
    }
    saveVersion("v1")

    // Version 2
    ConfigBuilder.config {
        FEATURE with { default(value = "v2") }
    }
    saveVersion("v2")

    // Version 3
    ConfigBuilder.config {
        FEATURE with { default(value = "v3") }
    }
    saveVersion("v3")

    println("\nAvailable versions: ${versions.map { it.version }}")

    // Rollback to v1
    println("\nRolling back to v1...")
    rollbackTo("v1")
}

/**
 * Example 7: Hot Reloading
 *
 * Simulate hot reloading of configuration without restart.
 */
fun hotReloading() {
    val FEATURE_FLAG: Conditional<String, Context> =
        Conditional("feature_flag")

    // Initial configuration
    println("=== Initial Configuration ===")
    ConfigBuilder.config {
        FEATURE_FLAG with {
            default(value = "initial")
        }
    }

    val initialJson = SnapshotSerializer.default.serialize(FlagRegistry.konfig())
    println("Initial value: $initialJson")

    // Simulate configuration update
    println("\n=== Configuration Update ===")
    val updatedJson = """
        {
          "flags": [
            {
              "key": "feature_flag",
              "defaultValue": {
                "type": "string",
                "value": "updated"
              },
              "isActive": true,
              "salt": "v1",
              "values": []
            }
          ]
        }
    """.trimIndent()

    when (val result = SnapshotSerializer.default.deserialize(updatedJson)) {
        is ParseResult.Success -> {
            FlagRegistry.load(result.value)
            println("Configuration hot-reloaded!")

            val newJson = SnapshotSerializer.default.serialize(FlagRegistry.konfig())
            println("New value: $newJson")
        }
        is ParseResult.Failure -> {
            println("Hot reload failed: ${result.error}")
        }
    }
}

fun main() {
    println("=== Basic Serialization ===")
    basicSerialization()

    println("\n\n=== Error Handling ===")
    errorHandling()

    println("\n\n=== Patch Serialization ===")
    patchSerialization()

    println("\n\n=== Remote Config Loading ===")
    remoteConfigLoading()

    println("\n\n=== Graceful Degradation ===")
    gracefulDegradation()

    println("\n\n=== Configuration Versioning ===")
    configurationVersioning()

    println("\n\n=== Hot Reloading ===")
    hotReloading()
}
