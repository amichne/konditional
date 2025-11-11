package io.amichne.konditional.serialization

import io.amichne.konditional.core.buildSnapshot
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Rollout
import io.amichne.konditional.context.Version
import io.amichne.konditional.context.evaluate
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.FlagDefinitionImpl
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.instance.Konfig
import io.amichne.konditional.core.internal.SingletonFlagRegistry
import io.amichne.konditional.core.result.getOrThrow
import io.amichne.konditional.example.SampleFeatureEnum
import io.amichne.konditional.rules.ConditionalValue.Companion.targetedBy
import io.amichne.konditional.rules.Rule
import io.amichne.konditional.rules.versions.LeftBound
import io.amichne.konditional.internal.serialization.models.FlagValue
import io.amichne.konditional.internal.serialization.models.SerializableFlag
import io.amichne.konditional.internal.serialization.models.SerializablePatch
import io.amichne.konditional.internal.serialization.models.SerializableRule
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SnapshotSerializerTest {
    private val serializer = SnapshotSerializer.default

    @BeforeEach
    fun setUp() {
        // Register the test enum flags
        FeatureRegistry.registerEnum<SampleFeatureEnum>()
    }

    @AfterEach
    fun tearDown() {
        // Clean up registry after each test
        FeatureRegistry.clear()
    }

    @Test
    fun `test simple flag serialization and deserialization`() {
        // Create a simple snapshot with one flag
        val snapshot = buildSnapshot {
            SampleFeatureEnum.ENABLE_COMPACT_CARDS with {
                default(true)
            }
        }

        // Serialize
        val json = serializer.serialize(snapshot)
        assertNotNull(json)
        assertTrue(json.contains("enable_compact_cards"))
        assertTrue(json.contains("BOOLEAN"))

        // Deserialize
        val deserialized = serializer.deserialize(json).getOrThrow()
        assertNotNull(deserialized)

        // Verify equality by evaluating both
        val testContext = Context(
            locale = AppLocale.EN_US,
            platform = Platform.IOS,
            appVersion = Version.of(1, 0, 0),
            stableId = StableId.of("a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6")
        )

        SingletonFlagRegistry.load(snapshot)
        val originalValue = testContext.evaluate(SampleFeatureEnum.ENABLE_COMPACT_CARDS)

        SingletonFlagRegistry.load(deserialized)
        val deserializedValue = testContext.evaluate(SampleFeatureEnum.ENABLE_COMPACT_CARDS)

        assertEquals(originalValue, deserializedValue)
    }

    @Test
    fun `test flag with rules serialization and deserialization`() {
        // Create a snapshot with complex rules using direct Condition construction
        val condition = FlagDefinitionImpl(
            conditional = SampleFeatureEnum.FIFTY_TRUE_US_IOS,
            values = listOf(
                Rule<Context>(
                    rollout = Rollout.of(50.0),
                    locales = setOf(AppLocale.EN_US),
                    platforms = setOf(Platform.IOS)
                ).targetedBy(true)
            ),
            defaultValue = false
        )

        val konfig = Konfig(
            mapOf(SampleFeatureEnum.FIFTY_TRUE_US_IOS to (condition))
        )

        // Serialize
        val json = serializer.serialize(konfig)
        assertNotNull(json)
        assertTrue(json.contains("fifty_true_us_ios"))
        assertTrue(json.contains("EN_US"))
        assertTrue(json.contains("IOS"))
        assertTrue(json.contains("50.0"))

        // Deserialize
        val deserialized = serializer.deserialize(json).getOrThrow()
        assertNotNull(deserialized)

        // Verify by comparing evaluation results with different contexts
        val contexts = listOf(
            Context(AppLocale.EN_US, Platform.IOS, Version.of(1, 0, 0), StableId.of("a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d1")),
            Context(AppLocale.EN_US, Platform.IOS, Version.of(1, 0, 0), StableId.of("a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d2")),
            Context(AppLocale.EN_US, Platform.ANDROID, Version.of(1, 0, 0), StableId.of("a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d3")),
            Context(AppLocale.ES_US, Platform.IOS, Version.of(1, 0, 0), StableId.of("a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d4"))
        )

        contexts.forEach { context ->
            SingletonFlagRegistry.load(konfig)
            val originalValue = context.evaluate(SampleFeatureEnum.FIFTY_TRUE_US_IOS)

            SingletonFlagRegistry.load(deserialized)
            val deserializedValue = context.evaluate(SampleFeatureEnum.FIFTY_TRUE_US_IOS)

            assertEquals(
                originalValue,
                deserializedValue,
                "Values should match for context: $context"
            )
        }
    }

    @Test
    fun `test flag with version ranges serialization`() {
        val condition = FlagDefinition(
            conditional = SampleFeatureEnum.VERSIONED,
            bounds = listOf(
                Rule<Context>(
                    rollout = Rollout.of(100.0),
                    versionRange = LeftBound(Version(7, 10, 0))
                ).targetedBy(true)
            ),
            defaultValue = false
        )

        val konfig = Konfig(
            mapOf(SampleFeatureEnum.VERSIONED to condition)
        )

        // Serialize
        val json = serializer.serialize(konfig)
        assertNotNull(json)
        assertTrue(json.contains("MIN_BOUND"))
        assertTrue(json.contains("\"major\": 7") || json.contains("\"major\":7"))
        assertTrue(json.contains("\"minor\": 10") || json.contains("\"minor\":10"))

        // Deserialize
        val deserialized = serializer.deserialize(json).getOrThrow()

        // Test with various versions
        val testCases = listOf(
            Version.of(7, 9, 0) to false,  // Too old
            Version.of(7, 10, 0) to true,  // Min bound (inclusive)
            Version.of(7, 15, 3) to true,  // Within range
            Version.of(8, 0, 0) to true,   // Higher version
        )

        testCases.forEach { (version, expected) ->
            val context = Context(
                AppLocale.EN_US,
                Platform.IOS,
                version,
                StableId.of("a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6")
            )

            SingletonFlagRegistry.load(konfig)
            val originalValue = context.evaluate(SampleFeatureEnum.VERSIONED)

            SingletonFlagRegistry.load(deserialized)
            val deserializedValue = context.evaluate(SampleFeatureEnum.VERSIONED)

            assertEquals(originalValue, deserializedValue)
            assertEquals(expected, deserializedValue, "Version $version should evaluate to $expected")
        }
    }

    @Test
    fun `test multiple flags serialization`() {
        val snapshot = buildSnapshot {
            SampleFeatureEnum.ENABLE_COMPACT_CARDS with {
                default(true)
            }
            SampleFeatureEnum.USE_LIGHTWEIGHT_HOME with {
                default(false)
            }
            SampleFeatureEnum.UNIFORM50 with {
                default(false)
            }
        }

        // Serialize
        val json = serializer.serialize(snapshot)
        assertNotNull(json)
        assertTrue(json.contains("enable_compact_cards"))
        assertTrue(json.contains("use_lightweight_home"))
        assertTrue(json.contains("uniform50"))

        // Deserialize
        val deserialized = serializer.deserialize(json).getOrThrow()

        // Verify all flags evaluate correctly
        val context = Context(
            AppLocale.EN_US,
            Platform.WEB,
            Version.of(1, 0, 0),
            StableId.of("a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6")
        )

        SingletonFlagRegistry.load(snapshot)
        val originalValues = context.evaluate()

        SingletonFlagRegistry.load(deserialized)
        val deserializedValues = context.evaluate()

        assertEquals(3, originalValues.size)
        assertEquals(3, deserializedValues.size)

        // Compare each flag
        originalValues.forEach { (key, value) ->
            assertEquals(value, deserializedValues[key], "Flag ${(key as Feature<*, *>).key} should match")
        }
    }

    @Test
    fun `test patch update - add new flag`() {
        // Create initial snapshot with one flag
        val initialSnapshot = buildSnapshot {
            SampleFeatureEnum.ENABLE_COMPACT_CARDS with {
                default(true)
            }
        }

        // Create a patch that adds a new flag
        val patch = SerializablePatch(
            flags = listOf(
                SerializableFlag(
                    key = "use_lightweight_home",
                    defaultValue = FlagValue.from(false),
                    rules = listOf(
                        SerializableRule(
                            value = FlagValue.from(true),
                            locales = setOf("EN_US"),
                            platforms = setOf("IOS")
                        )
                    )
                )
            )
        )

        // Apply patch
        val patchedSnapshot = serializer.applyPatch(initialSnapshot, patch).getOrThrow()

        // Verify the patch was applied
        val context = Context(
            AppLocale.EN_US,
            Platform.IOS,
            Version.of(1, 0, 0),
            StableId.of("a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6")
        )

        SingletonFlagRegistry.load(patchedSnapshot)
        val values = context.evaluate()

        assertEquals(2, values.size, "Should have 2 flags after patch")
        assertTrue(values.containsKey(SampleFeatureEnum.ENABLE_COMPACT_CARDS))
        assertTrue(values.containsKey(SampleFeatureEnum.USE_LIGHTWEIGHT_HOME))
    }

    @Test
    fun `test patch update - update existing flag`() {
        // Create initial snapshot
        val initialSnapshot = buildSnapshot {
            SampleFeatureEnum.ENABLE_COMPACT_CARDS with {
                default(false)
            }
        }

        // Create a patch that updates the flag
        val patch = SerializablePatch(
            flags = listOf(
                SerializableFlag(
                    key = "enable_compact_cards",
                    defaultValue = FlagValue.from(true)  // Changed from false to true
                )
            )
        )

        // Apply patch
        val patchedSnapshot = serializer.applyPatch(initialSnapshot, patch).getOrThrow()

        // Verify the update
        val context = Context(
            AppLocale.EN_US,
            Platform.IOS,
            Version.of(1, 0, 0),
            StableId.of("a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6")
        )

        SingletonFlagRegistry.load(patchedSnapshot)
        val value = context.evaluate(SampleFeatureEnum.ENABLE_COMPACT_CARDS)

        assertEquals(true, value, "Flag should now default to true")
    }

    @Test
    fun `test patch update - remove flag`() {
        // Create initial snapshot with two flags
        val initialSnapshot = buildSnapshot {
            SampleFeatureEnum.ENABLE_COMPACT_CARDS with {
                default(true)
            }
            SampleFeatureEnum.USE_LIGHTWEIGHT_HOME with {
                default(false)
            }
        }

        // Create a patch that removes one flag
        val patch = SerializablePatch(
            flags = emptyList(),
            removeKeys = listOf("use_lightweight_home")
        )

        // Apply patch
        val patchedSnapshot = serializer.applyPatch(initialSnapshot, patch).getOrThrow()

        // Verify the flag was removed
        val context = Context(
            AppLocale.EN_US,
            Platform.IOS,
            Version.of(1, 0, 0),
            StableId.of("a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6")
        )

        SingletonFlagRegistry.load(patchedSnapshot)
        val values = context.evaluate()

        assertEquals(1, values.size, "Should have 1 flag after removal")
        assertTrue(values.containsKey(SampleFeatureEnum.ENABLE_COMPACT_CARDS))
    }

    @Test
    fun `test patch from JSON`() {
        // Create initial snapshot
        val initialSnapshot = buildSnapshot {
            SampleFeatureEnum.ENABLE_COMPACT_CARDS with {
                default(false)
            }
        }

        // Create patch JSON
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
              "removeKeys": []
            }
        """.trimIndent()

        // Apply patch from JSON
        val patchedSnapshot = serializer.applyPatchJson(initialSnapshot, patchJson).getOrThrow()

        // Verify the patch was applied
        val context = Context(
            AppLocale.EN_US,
            Platform.IOS,
            Version.of(1, 0, 0),
            StableId.of("a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6")
        )

        SingletonFlagRegistry.load(patchedSnapshot)
        val value = context.evaluate(SampleFeatureEnum.ENABLE_COMPACT_CARDS)

        assertEquals(true, value, "Flag should be updated to true")
    }

    @Test
    fun `test round-trip equality with complex configuration`() {
        // Create a complex snapshot with multiple flags
        val snapshot = buildSnapshot {
            SampleFeatureEnum.ENABLE_COMPACT_CARDS with {
                default(true)
            }
            SampleFeatureEnum.USE_LIGHTWEIGHT_HOME with {
                default(false)
            }
            SampleFeatureEnum.VERSIONED with {
                default(false)
            }
        }

        // Perform round-trip: serialize then deserialize
        val json = serializer.serialize(snapshot)
        val deserialized = serializer.deserialize(json).getOrThrow()

        // Test with multiple contexts to ensure behavior is identical
        val contexts = listOf(
            Context(AppLocale.EN_US, Platform.IOS, Version.of(1, 0, 0), StableId.of("a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d1")),
            Context(AppLocale.EN_CA, Platform.IOS, Version.of(7, 5, 0), StableId.of("a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d2")),
            Context(AppLocale.ES_US, Platform.ANDROID, Version.of(8, 0, 0), StableId.of("a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d3")),
            Context(AppLocale.HI_IN, Platform.WEB, Version.of(6, 0, 0), StableId.of("a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d4"))
        )

        contexts.forEach { context ->
            SingletonFlagRegistry.load(snapshot)
            val originalValues = context.evaluate()

            SingletonFlagRegistry.load(deserialized)
            val deserializedValues = context.evaluate()

            assertEquals(
                originalValues.size,
                deserializedValues.size,
                "Number of flags should match for context: $context"
            )

            originalValues.forEach { (key, value) ->
                assertEquals(
                    value,
                    deserializedValues[key],
                    "Flag ${(key as Feature<*, *>).key} should match for context: $context"
                )
            }
        }
    }
}
