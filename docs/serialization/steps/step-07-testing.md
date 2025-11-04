---
title: 'Step 7: Testing'
description: Write comprehensive tests for your serialization integration
---


## Overview

Thorough testing ensures your serialization implementation works correctly and prevents regressions. This step covers unit tests, integration tests, and property-based tests.

::: tip
**Time estimate:** 30 minutes

**Goal:** Comprehensive test coverage for serialization workflows
:::

## Test Setup

Create a base test class with proper setup/teardown:

```kotlin title="BaseSerializationTest.kt"
import io.amichne.konditional.serialization.ConditionalRegistry
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

abstract class BaseSerializationTest {

    @BeforeEach
    fun setUp() {
        // Register flags before each test
        ConditionalRegistry.registerEnum<FeatureFlags>()
    }

    @AfterEach
    fun tearDown() {
        // Clean up registry to avoid test pollution
        ConditionalRegistry.clear()
    }

    protected fun createTestContext() = Context(
        locale = AppLocale.EN_US,
        platform = Platform.IOS,
        appVersion = Version.of(2, 0, 0),
        stableId = StableId.of("a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6")
    )
}
```

## Unit Tests

### Test 1: Basic Serialization

```kotlin
class BasicSerializationTest : BaseSerializationTest() {

    @Test
    fun `serialize simple flag configuration`() {
        val konfig = ConfigBuilder.buildSnapshot {
            FeatureFlags.DARK_MODE with {
                default(true)
            }
        }

        val json = SnapshotSerializer.default.serialize(konfig)

        assertNotNull(json)
        assertTrue(json.contains("dark_mode"))
        assertTrue(json.contains("BOOLEAN"))
        assertTrue(json.contains("\"defaultValue\": true"))
    }

    @Test
    fun `deserialize simple flag configuration`() {
        val json = """
            {
              "flags": [
                {
                  "key": "dark_mode",
                  "type": "BOOLEAN",
                  "defaultValue": true,
                  "default": {"value": true, "type": "BOOLEAN"},
                  "salt": "v1",
                  "isActive": true,
                  "rules": []
                }
              ]
            }
        """.trimIndent()

        val konfig = SnapshotSerializer.default.deserialize(json)

        assertEquals(1, konfig.flags.size)
        assertTrue(konfig.flags.containsKey(FeatureFlags.DARK_MODE))
    }
}
```

### Test 2: Round-Trip Equality

```kotlin
class RoundTripTest : BaseSerializationTest() {

    @Test
    fun `round-trip preserves simple configuration`() {
        val original = ConfigBuilder.buildSnapshot {
            FeatureFlags.DARK_MODE with { default(true) }
            FeatureFlags.NEW_ONBOARDING with { default(false) }
        }

        val json = SnapshotSerializer.default.serialize(original)
        val restored = SnapshotSerializer.default.deserialize(json)

        assertSnapshotsEqual(original, restored)
    }

    @Test
    fun `round-trip preserves complex rules`() {
        val original = ConfigBuilder.buildSnapshot {
            FeatureFlags.NEW_ONBOARDING with {
                default(false)
                rule {
                    rollout = Rollout.of(50.0)
                    locales(AppLocale.EN_US, AppLocale.EN_CA)
                    platforms(Platform.IOS)
                    note = "50% rollout for US/CA iOS"
                }.implies(true)
            }
        }

        val json = SnapshotSerializer.default.serialize(original)
        val restored = SnapshotSerializer.default.deserialize(json)

        assertSnapshotsEqual(original, restored)
    }

    private fun assertSnapshotsEqual(
        expected: Flags.Snapshot,
        actual: Flags.Snapshot
    ) {
        val context = createTestContext()

        with(Flags) {
            Flags.load(expected)
            val expectedValues = context.evaluate()

            Flags.load(actual)
            val actualValues = context.evaluate()

            assertEquals(expectedValues.size, actualValues.size)

            expectedValues.forEach { (key, value) ->
                assertEquals(
                    value,
                    actualValues[key],
                    "Flag ${(key as Conditional<*, *>).key} values should match"
                )
            }
        }
    }
}
```

### Test 3: Error Handling

```kotlin
class ErrorHandlingTest : BaseSerializationTest() {

    @Test
    fun `deserialization fails on invalid JSON`() {
        val invalidJson = "not valid json at all"

        assertThrows<JsonDataException> {
            SnapshotSerializer.default.deserialize(invalidJson)
        }
    }

    @Test
    fun `deserialization fails on unregistered flag`() {
        ConditionalRegistry.clear() // Clear all registrations

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

        val exception = assertThrows<IllegalArgumentException> {
            SnapshotSerializer.default.deserialize(json)
        }

        assertTrue(exception.message!!.contains("unknown_flag"))
        assertTrue(exception.message!!.contains("not found in registry"))
    }

    @Test
    fun `deserialization fails on type mismatch`() {
        val json = """
            {
              "flags": [
                {
                  "key": "dark_mode",
                  "type": "STRING",
                  "defaultValue": "not a boolean",
                  "default": {"value": "not a boolean", "type": "STRING"},
                  "rules": []
                }
              ]
            }
        """.trimIndent()

        // This should fail because FeatureFlags.DARK_MODE expects Boolean
        assertThrows<ClassCastException> {
            val konfig = SnapshotSerializer.default.deserialize(json)
            Flags.load(konfig)

            with(Flags) {
                createTestContext().evaluate(FeatureFlags.DARK_MODE)
            }
        }
    }
}
```

## Integration Tests

### Test Loading Workflow

```kotlin
class LoadingWorkflowTest : BaseSerializationTest() {

    @Test
    fun `full workflow from config to evaluation`() {
        // 1. Create configuration
        val konfig = ConfigBuilder.buildSnapshot {
            FeatureFlags.DARK_MODE with {
                default(false)
                rule {
                    platforms(Platform.IOS)
                }.implies(true)
            }
        }

        // 2. Serialize
        val json = SnapshotSerializer.default.serialize(konfig)
        assertNotNull(json)

        // 3. Deserialize
        val restored = SnapshotSerializer.default.deserialize(json)

        // 4. Load
        Flags.load(restored)

        // 5. Evaluate
        val iosContext = Context(
            AppLocale.EN_US,
            Platform.IOS,
            Version.of(1, 0, 0),
            StableId.of("test-user-ios")
        )

        val androidContext = Context(
            AppLocale.EN_US,
            Platform.ANDROID,
            Version.of(1, 0, 0),
            StableId.of("test-user-android")
        )

        with(Flags) {
            assertTrue(iosContext.evaluate(FeatureFlags.DARK_MODE))
            assertFalse(androidContext.evaluate(FeatureFlags.DARK_MODE))
        }
    }
}
```

### Test Patch Updates

```kotlin
class PatchUpdateTest : BaseSerializationTest() {

    @Test
    fun `patch adds new flag`() {
        val initial = ConfigBuilder.buildSnapshot {
            FeatureFlags.DARK_MODE with { default(true) }
        }

        val patch = SerializablePatch(
            flags = listOf(
                SerializableFlag(
                    key = "new_onboarding",
                    type = ValueType.BOOLEAN,
                    defaultValue = false
                )
            )
        )

        val updated = SnapshotSerializer.default.applyPatch(initial, patch)

        assertEquals(2, updated.flags.size)
        assertTrue(updated.flags.containsKey(FeatureFlags.DARK_MODE))
        assertTrue(updated.flags.containsKey(FeatureFlags.NEW_ONBOARDING))
    }

    @Test
    fun `patch updates existing flag`() {
        val initial = ConfigBuilder.buildSnapshot {
            FeatureFlags.DARK_MODE with { default(false) }
        }

        val patch = SerializablePatch(
            flags = listOf(
                SerializableFlag(
                    key = "dark_mode",
                    type = ValueType.BOOLEAN,
                    defaultValue = true // Changed
                )
            )
        )

        val updated = SnapshotSerializer.default.applyPatch(initial, patch)

        Flags.load(updated)
        val context = createTestContext()

        with(Flags) {
            assertTrue(context.evaluate(FeatureFlags.DARK_MODE))
        }
    }

    @Test
    fun `patch removes flag`() {
        val initial = ConfigBuilder.buildSnapshot {
            FeatureFlags.DARK_MODE with { default(true) }
            FeatureFlags.NEW_ONBOARDING with { default(false) }
        }

        val patch = SerializablePatch(
            flags = emptyList(),
            removeKeys = listOf("new_onboarding")
        )

        val updated = SnapshotSerializer.default.applyPatch(initial, patch)

        assertEquals(1, updated.flags.size)
        assertTrue(updated.flags.containsKey(FeatureFlags.DARK_MODE))
        assertFalse(updated.flags.containsKey(FeatureFlags.NEW_ONBOARDING))
    }
}
```

## Property-Based Tests

Test properties that should always hold true:

```kotlin
class PropertyBasedTest : BaseSerializationTest() {

    @Test
    fun `serialization is deterministic`() {
        val konfig = ConfigBuilder.buildSnapshot {
            FeatureFlags.DARK_MODE with { default(true) }
            FeatureFlags.NEW_ONBOARDING with { default(false) }
        }

        val json1 = SnapshotSerializer.default.serialize(konfig)
        val json2 = SnapshotSerializer.default.serialize(konfig)

        assertEquals(json1, json2, "Serialization should be deterministic")
    }

    @Test
    fun `evaluation is consistent across reload`() {
        val konfig = ConfigBuilder.buildSnapshot {
            FeatureFlags.NEW_ONBOARDING with {
                default(false)
                rule {
                    rollout = Rollout.of(50.0)
                }.implies(true)
            }
        }

        val context = createTestContext()

        // Evaluate before reload
        Flags.load(konfig)
        val valueBefore = with(Flags) {
            context.evaluate(FeatureFlags.NEW_ONBOARDING)
        }

        // Reload same configuration
        Flags.load(konfig)
        val valueAfter = with(Flags) {
            context.evaluate(FeatureFlags.NEW_ONBOARDING)
        }

        assertEquals(valueBefore, valueAfter, "Values should be consistent")
    }

    @Test
    fun `all registered flags can be serialized and deserialized`() {
        // Create config with all flags
        val konfig = ConfigBuilder.buildSnapshot {
            FeatureFlags.values().forEach { flag ->
                flag with { default(false) }
            }
        }

        // Serialize
        val json = SnapshotSerializer.default.serialize(konfig)

        // Deserialize
        val restored = SnapshotSerializer.default.deserialize(json)

        // All flags should be present
        assertEquals(FeatureFlags.values().size, restored.flags.size)

        FeatureFlags.values().forEach { flag ->
            assertTrue(
                restored.flags.containsKey(flag),
                "Flag ${flag.key} should be present after round-trip"
            )
        }
    }
}
```

## Testing Best Practices

### 1. Test JSON Samples

Keep real JSON samples for regression testing:

```kotlin
@Test
fun `deserialize production JSON sample`() {
    val json = File("src/test/resources/production-flags.json").readText()
    val konfig = SnapshotSerializer.default.deserialize(json)

    assertNotNull(konfig)
    assertTrue(konfig.flags.isNotEmpty())
}
```

### 2. Parameterized Tests

Test multiple scenarios with one test:

```kotlin
@ParameterizedTest
@MethodSource("flagScenarios")
fun `test various flag configurations`(scenario: FlagScenario) {
    val konfig = scenario.createSnapshot()
    val json = SnapshotSerializer.default.serialize(konfig)
    val restored = SnapshotSerializer.default.deserialize(json)

    assertSnapshotsEqual(konfig, restored)
}

companion object {
    @JvmStatic
    fun flagScenarios() = listOf(
        FlagScenario("simple default", { FeatureFlags.DARK_MODE with { default(true) } }),
        FlagScenario("with rules", { FeatureFlags.DARK_MODE with {
            default(false)
            rule { platforms(Platform.IOS) }.implies(true)
        }}),
        // More scenarios...
    )
}
```

## What's Next?

With comprehensive tests in place, you're ready to deploy to production!

<div style="display: flex; justify-content: space-between; margin-top: 2rem;">
  <a href="/serialization/steps/step-06-load/" style="text-decoration: none;">
    <strong>← Previous: Step 6 - Load into Runtime</strong>
  </a>
  <a href="/serialization/steps/step-08-production/" style="text-decoration: none;">
    <strong>Next: Step 8 - Production Setup →</strong>
  </a>
</div>
