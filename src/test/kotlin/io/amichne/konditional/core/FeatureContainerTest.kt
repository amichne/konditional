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
import io.amichne.konditional.core.features.StringFeature
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.registry.ModuleRegistry
import io.amichne.konditional.core.registry.RegistryScope
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
            RegistryScope.setGlobal(ModuleRegistry())
        }

        val testBoolean by boolean<Context>(default = false)
        val testString by string<Context>(default = "default")
        val testInt by int<Context>(default = 0)
        val testDouble by double<Context>(default = 0.0)
    }

//    object Invalid {
//        val x by double()
//    }

    @Test
    fun `features are created with correct types`() {
        // Verify each feature has correct type
        assertTrue(TestFeatures.testBoolean is BooleanFeature<*, *>)
        assertTrue(TestFeatures.testString is StringFeature<*, *>)
        assertTrue(TestFeatures.testInt is IntFeature<*, *>)
        assertTrue(TestFeatures.testDouble is DoubleFeature<*, *>)
    }

    @Test
    fun `features have correct keys`() {
        assertEquals("testBoolean", TestFeatures.testBoolean.key)
        assertEquals("testString", TestFeatures.testString.key)
        assertEquals("testInt", TestFeatures.testInt.key)
        assertEquals("testDouble", TestFeatures.testDouble.key)
    }

    @Test
    fun `features have correct module`() {
        val expectedModule = Taxonomy.Domain.Payments

        assertEquals(expectedModule, TestFeatures.testBoolean.module)
        assertEquals(expectedModule, TestFeatures.testString.module)
        assertEquals(expectedModule, TestFeatures.testInt.module)
        assertEquals(expectedModule, TestFeatures.testDouble.module)
    }

    @Test
    fun `allFeatures returns all declared features`() {
        val allFeatures = TestFeatures.allFeatures()

        // Should have exactly 4 features
        assertEquals(4, allFeatures.size)

        // Should contain all feature keys
        val keys = allFeatures.map { it.key }.toSet()
        assertEquals(
            setOf("testBoolean", "testString", "testInt", "testDouble"),
            keys
        )
    }

    @Test
    fun `features are lazily initialized`() {
        // Create a new container that hasn't been accessed yet
        with(object : FeatureContainer<Taxonomy.Domain.Payments>(
            Taxonomy.Domain.Payments
        ) {
            val lazyA by boolean<Context>(default = true)
            val lazyB by boolean<Context>(default = true)
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
        // Create a test container with configured features
        val testFeatures = object : FeatureContainer<Taxonomy.Domain.Payments>(
            Taxonomy.Domain.Payments
        ) {
            val configuredBoolean by boolean<Context>(default = true)
            val configuredString by string<Context>("test-value") {}
            val configuredInt by int<Context>(100)
        }

        val context = Context(
            locale = AppLocale.EN_US,
            platform = Platform.WEB,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("12345678901234567890123456789012")
        )

        // Evaluate features - configuration is automatic through delegation
        assertEquals(true, context.evaluate(testFeatures.configuredBoolean))
        assertEquals("test-value", context.evaluateOrDefault(testFeatures.configuredString, ""))
        assertEquals(100, context.evaluateOrDefault(testFeatures.configuredInt, 0))
    }

    @Test
    fun `multiple containers maintain independent feature lists`() {
        val first = object : FeatureContainer<Taxonomy.Domain.Payments>(
            Taxonomy.Domain.Payments
        ) {
            val a1 by boolean<Context>(default = true)
            val a2 by boolean<Context>(default = true)
        }

        val second = object : FeatureContainer<Taxonomy.Domain.Messaging>(
            Taxonomy.Domain.Messaging
        ) {
            val b1 by boolean<Context>(default = true)
            val b2 by boolean<Context>(default = true)
            val b3 by boolean<Context>(default = true)
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
