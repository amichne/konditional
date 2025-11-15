package io.amichne.konditional.core

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Context.Companion.evaluate
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.features.BooleanFeature
import io.amichne.konditional.core.features.DoubleFeature
import io.amichne.konditional.core.features.FeatureContainer
import io.amichne.konditional.core.features.IntFeature
import io.amichne.konditional.core.features.JsonFeature
import io.amichne.konditional.core.features.StringFeature
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.result.utils.evaluateOrDefault
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Test suite demonstrating FeatureContainer functionality
 */
class FeatureContainerTest {

    // Test container with mixed feature types
    object TestFeatures : FeatureContainer<Taxonomy.Domain.Payments>(
        Taxonomy.Domain.Payments
    ) {
        init {
            RegistryScope.setGlobal(ModuleRegistry.create())
        }

        val defaultTestConfig = TestConfig(
            enabled = false,
            value = 0
        )
        val testBoolean by boolean<Context> {
        }
        val testString by string<Context> { }
        val testInt by int<Context> { }
        val testDouble by double<Context> { }
        val testJson by jsonObject<Context, TestConfig>(defaultTestConfig, "testJson")
    }

//    object Invalid {
//        val x by double()
//    }

    data class TestConfig(
        val enabled: Boolean,
        val value: Int,
    )

    @Test
    fun `features are created with correct types`() {
        // Verify each feature has correct type
        assertTrue(TestFeatures.testBoolean is BooleanFeature<*, *>)
        assertTrue(TestFeatures.testString is StringFeature<*, *>)
        assertTrue(TestFeatures.testInt is IntFeature<*, *>)
        assertTrue(TestFeatures.testDouble is DoubleFeature<*, *>)
        assertTrue(TestFeatures.testJson is JsonFeature<*, *, *>)
    }

    @Test
    fun `features have correct keys`() {
        assertEquals("testBoolean", TestFeatures.testBoolean.key)
        assertEquals("testString", TestFeatures.testString.key)
        assertEquals("testInt", TestFeatures.testInt.key)
        assertEquals("testDouble", TestFeatures.testDouble.key)
        assertEquals("testJson", TestFeatures.testJson.key)
    }

    @Test
    fun `features have correct module`() {
        val expectedModule = Taxonomy.Domain.Payments

        assertEquals(expectedModule, TestFeatures.testBoolean.module)
        assertEquals(expectedModule, TestFeatures.testString.module)
        assertEquals(expectedModule, TestFeatures.testInt.module)
        assertEquals(expectedModule, TestFeatures.testDouble.module)
        assertEquals(expectedModule, TestFeatures.testJson.module)
    }

    @Test
    fun `allFeatures returns all declared features`() {
        val allFeatures = TestFeatures.allFeatures()

        // Should have exactly 5 features
        assertEquals(5, allFeatures.size)

        // Should contain all feature keys
        val keys = allFeatures.map { it.key }.toSet()
        assertEquals(
            setOf("testBoolean", "testString", "testInt", "testDouble", "testJson"),
            keys
        )
    }

    @Test
    fun `features are lazily initialized`() {
        // Create a new container that hasn't been accessed yet
        with(object : FeatureContainer<Taxonomy.Domain.Payments>(
            Taxonomy.Domain.Payments
        ) {
            val lazyA by boolean<Context> { }
            val lazyB by boolean<Context> { }
        }) {

            // allFeatures() should return empty before any feature is accessed
            // Note: In current implementation, accessing allFeatures() triggers initialization
            // This is fine - features register on first access (either directly or via allFeatures)

            // After calling allFeatures(), all features should be registered
            val features = allFeatures()
            assertEquals(0, features.size)

            // Accessing individual features doesn't change count
            val featureA = lazyA
            val featureB = lazyB
            assertEquals(2, allFeatures().size)
        }
    }

    @Test
    fun `features can be evaluated with context`() {
        // Configure the registry
        Taxonomy.Domain.Payments.config {
            TestFeatures.testBoolean with {
                default(true)
            }
            TestFeatures.testString with {
                default("test-value")
            }
            TestFeatures.testInt with {
                default(100)
            }
        }

        val context = Context(
            locale = AppLocale.EN_US,
            platform = Platform.WEB,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("12345678901234567890123456789012")
        )

        // Evaluate features
        assertEquals(true, context.evaluate(TestFeatures.testBoolean))
        assertEquals("test-value", context.evaluateOrDefault(TestFeatures.testString, ""))
        assertEquals(100, context.evaluateOrDefault(TestFeatures.testInt, 0))
    }

    @Test
    fun `multiple containers maintain independent feature lists`() {
        val first = object : FeatureContainer<Taxonomy.Domain.Payments>(
            Taxonomy.Domain.Payments
        ) {
            val a1 by boolean<Context> { }
            val a2 by boolean<Context> { }
        }

        val second = object : FeatureContainer<Taxonomy.Domain.Messaging>(
            Taxonomy.Domain.Messaging
        ) {
            val b1 by boolean<Context> { }
            val b2 by boolean<Context> { }
            val b3 by boolean<Context> { }
        }

        // Access properties to trigger registration
        first.a1
        first.a2
        second.b1
        second.b2
        second.b3

        assertEquals(2, first.allFeatures().size)
        assertEquals(3, second.allFeatures().size)

        // Features are distinct
        val keysA = first.allFeatures().map { it.key }.toSet()
        val keysB = second.allFeatures().map { it.key }.toSet()

        assertEquals(setOf("a1", "a2"), keysA)
        assertEquals(setOf("b1", "b2", "b3"), keysB)
    }

    @Test
    fun `can iterate over all features for validation`() {
        // Real-world use case: validate all features are configured
        val configuredKeys = setOf("testBoolean", "testString", "testInt")

        val allKeys = TestFeatures.allFeatures().map { it.key }.toSet()
        val missingKeys = allKeys - configuredKeys

        // In this test, we expect some features to be "missing" from config
        assertTrue(missingKeys.contains("testDouble"))
        assertTrue(missingKeys.contains("testJson"))
    }

    @Test
    fun `features maintain type safety through container`() {
        // Type inference works correctly
        val booleanFeature: BooleanFeature<Context, Taxonomy.Domain.Payments> =
            TestFeatures.testBoolean

        val stringFeature: StringFeature<Context, Taxonomy.Domain.Payments> =
            TestFeatures.testString

        val intFeature: IntFeature<Context, Taxonomy.Domain.Payments> =
            TestFeatures.testInt

        // Verify types are preserved
        assertEquals("testBoolean", booleanFeature.key)
        assertEquals("testString", stringFeature.key)
        assertEquals("testInt", intFeature.key)
    }
}
