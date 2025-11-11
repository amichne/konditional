package io.amichne.konditional.serialization

import io.amichne.konditional.core.buildSnapshot
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Rollout
import io.amichne.konditional.context.Version
import io.amichne.konditional.context.evaluate
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.internal.SingletonFlagRegistry
import io.amichne.konditional.core.result.getOrThrow
import io.amichne.konditional.example.SampleFeatureEnum
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Example demonstrating how to use the serialization system.
 *
 * This example shows:
 * 1. Creating a configuration with the ConfigBuilder
 * 2. Serializing the configuration to JSON
 * 3. Deserializing JSON back to a Konfig
 * 4. Applying patch updates
 * 5. Verifying round-trip equality
 */
class SerializationExampleTest {

    private val serializer = SnapshotSerializer.default

    @BeforeEach
    fun setUp() {
        // Register feature flags before deserialization
        FeatureRegistry.registerEnum<SampleFeatureEnum>()
    }

    @AfterEach
    fun tearDown() {
        FeatureRegistry.clear()
    }

    @Test
    fun `example - complete workflow with serialization`() {
        println("=== Konditional Serialization Example ===\n")

        // Step 1: Create a configuration using the ConfigBuilder
        println("Step 1: Creating configuration...")
        val snapshot = buildSnapshot {
            SampleFeatureEnum.ENABLE_COMPACT_CARDS with {
                default(false)
                rule {
                    rollout = Rollout.of(50.0)
                    locales(AppLocale.EN_US)
                    platforms(Platform.IOS)
                }.implies(true)
            }

            SampleFeatureEnum.USE_LIGHTWEIGHT_HOME with {
                default(false)
                rule {
                    platforms(Platform.WEB)
                }.implies(true)
            }
        }
        println("✓ Configuration created with 2 flags\n")

        // Step 2: Serialize to JSON
        println("Step 2: Serializing to JSON...")
        val json = serializer.serialize(snapshot)
        println("✓ Serialized JSON:")
        println(json)
        println()

        // Step 3: Deserialize from JSON
        println("Step 3: Deserializing from JSON...")
        val deserialized = serializer.deserialize(json).getOrThrow()
        println("✓ Successfully deserialized\n")

        // Step 4: Verify the configurations behave identically
        println("Step 4: Verifying round-trip equality...")
        val testContext = Context(
            locale = AppLocale.EN_US,
            platform = Platform.IOS,
            appVersion = Version.of(1, 0, 0),
            stableId = StableId.of("a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6")
        )

        SingletonFlagRegistry.load(snapshot)
        val originalCompactCards = testContext.evaluate(SampleFeatureEnum.ENABLE_COMPACT_CARDS)
        val originalLightweightHome = testContext.evaluate(SampleFeatureEnum.USE_LIGHTWEIGHT_HOME)

        SingletonFlagRegistry.load(deserialized)
        val deserializedCompactCards = testContext.evaluate(SampleFeatureEnum.ENABLE_COMPACT_CARDS)
        val deserializedLightweightHome = testContext.evaluate(SampleFeatureEnum.USE_LIGHTWEIGHT_HOME)

        assertEquals(originalCompactCards, deserializedCompactCards)
        assertEquals(originalLightweightHome, deserializedLightweightHome)

        println("✓ Original and deserialized configs produce identical results:")
        println("  - ENABLE_COMPACT_CARDS: $originalCompactCards")
        println("  - USE_LIGHTWEIGHT_HOME: $originalLightweightHome")
        println()

        // Step 5: Apply a patch update
        println("Step 5: Applying patch update...")
        val patchJson = """
            {
              "flags": [
                {
                  "key": "enable_compact_cards",
                  "defaultValue": {
                    "type": "BOOLEAN",
                    "value": true
                  },
                  "salt": "v2",
                  "isActive": true,
                  "rules": []
                }
              ],
              "removeKeys": ["use_lightweight_home"]
            }
        """.trimIndent()

        val patched = serializer.applyPatchJson(deserialized, patchJson).getOrThrow()
        println("✓ Patch applied (updated ENABLE_COMPACT_CARDS, removed USE_LIGHTWEIGHT_HOME)\n")

        // Step 6: Verify patch results
        println("Step 6: Verifying patched configuration...")
        SingletonFlagRegistry.load(patched)

        println("  - ENABLE_COMPACT_CARDS: ${testContext.evaluate(SampleFeatureEnum.ENABLE_COMPACT_CARDS)}")

        assertEquals(true, testContext.evaluate(SampleFeatureEnum.ENABLE_COMPACT_CARDS))

        println("\n=== Example Complete ===")
    }

    @Test
    fun `example - saving and loading from file system`() {
        println("=== File System Integration Example ===\n")

        // Create a configuration
        val snapshot = buildSnapshot {
            SampleFeatureEnum.ENABLE_COMPACT_CARDS with {
                default(true)
            }
        }

        // Serialize to JSON string (ready to save to file)
        val json = serializer.serialize(snapshot)

        println("JSON ready to save to file:")
        println(json)
        println()

        // In a real application, you would:
        // File("config.json").writeText(json)

        println("To load from file, you would:")
        println("  val json = File(\"config.json\").readText()")
        println("  val snapshot = SnapshotSerializer.default.deserialize(json)")
        println("  SingletonFlagRegistry.load(snapshot)")
        println()

        // Simulate loading from file
        val loadedSnapshot = serializer.deserialize(json).getOrThrow()

        // Verify it works
        SingletonFlagRegistry.load(loadedSnapshot)
        val context = Context(
            AppLocale.EN_US,
            Platform.IOS,
            Version.of(1, 0, 0),
            StableId.of("a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6")
        )
        val value = context.evaluate(SampleFeatureEnum.ENABLE_COMPACT_CARDS)
        assertEquals(true, value)
        println("✓ Configuration loaded and evaluated successfully: $value")

        println("\n=== Example Complete ===")
    }
}
