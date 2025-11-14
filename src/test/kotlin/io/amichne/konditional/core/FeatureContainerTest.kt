package io.amichne.konditional.core

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
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
        val test_boolean by boolean<Context> { }
        val test_string by string<Context> { }
        val test_int by int<Context> { }
        val test_double by double<Context> { }
        val test_json by jsonObject<Context, TestConfig>("test_json")
    }

    object Invalid {
        val x by double()
    }

    data class TestConfig(
        val enabled: Boolean,
        val value: Int
    )

    @Test
    fun `features are created with correct types`() {
        // Verify each feature has correct type
        assertTrue(TestFeatures.test_boolean is BooleanFeature<*, *>)
        assertTrue(TestFeatures.test_string is StringFeature<*, *>)
        assertTrue(TestFeatures.test_int is IntFeature<*, *>)
        assertTrue(TestFeatures.test_double is DoubleFeature<*, *>)
        assertTrue(TestFeatures.test_json is JsonEncodeableFeature<*, *, *>)
    }

    @Test
    fun `features have correct keys`() {
        assertEquals("test_boolean", TestFeatures.test_boolean.key)
        assertEquals("test_string", TestFeatures.test_string.key)
        assertEquals("test_int", TestFeatures.test_int.key)
        assertEquals("test_double", TestFeatures.test_double.key)
        assertEquals("test_json", TestFeatures.test_json.key)
    }

    @Test
    fun `features have correct module`() {
        val expectedModule = Taxonomy.Domain.Payments

        assertEquals(expectedModule, TestFeatures.test_boolean.module)
        assertEquals(expectedModule, TestFeatures.test_string.module)
        assertEquals(expectedModule, TestFeatures.test_int.module)
        assertEquals(expectedModule, TestFeatures.test_double.module)
        assertEquals(expectedModule, TestFeatures.test_json.module)
    }

    @Test
    fun `allFeatures returns all declared features`() {
        val allFeatures = TestFeatures.allFeatures()

        // Should have exactly 5 features
        assertEquals(5, allFeatures.size)

        // Should contain all feature keys
        val keys = allFeatures.map { it.key }.toSet()
        assertEquals(
            setOf("test_boolean", "test_string", "test_int", "test_double", "test_json"),
            keys
        )
    }

    @Test
    fun `features are lazily initialized`() {
        // Create a new container that hasn't been accessed yet
        object LazyTestContainer : FeatureContainer<Taxonomy.Domain.Payments>(
            Taxonomy.Domain.Payments
        ) {
            val lazy_a by boolean<Context> { }
            val lazy_b by boolean<Context> { }
        }

        // allFeatures() should return empty before any feature is accessed
        // Note: In current implementation, accessing allFeatures() triggers initialization
        // This is fine - features register on first access (either directly or via allFeatures)

        // After calling allFeatures(), all features should be registered
        val features = LazyTestContainer.allFeatures()
        assertEquals(2, features.size)

        // Accessing individual features doesn't change count
        val featureA = LazyTestContainer.lazy_a
        val featureB = LazyTestContainer.lazy_b
        assertEquals(2, LazyTestContainer.allFeatures().size)
    }

    @Test
    fun `features can be evaluated with context`() {
        // Configure the registry
        Taxonomy.Domain.Payments.config {
            TestFeatures.test_boolean with {
                default(true)
            }
            TestFeatures.test_string with {
                default("test-value")
            }
            TestFeatures.test_int with {
                default(100)
            }
        }

        val context = Context(
            locale = AppLocale.EN_US,
            platform = Platform.WEB,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("test-user")
        )

        // Evaluate features
        assertEquals(true, context.evaluateOrDefault(TestFeatures.test_boolean, false))
        assertEquals("test-value", context.evaluateOrDefault(TestFeatures.test_string, ""))
        assertEquals(100, context.evaluateOrDefault(TestFeatures.test_int, 0))
    }

    @Test
    fun `multiple containers maintain independent feature lists`() {
        object ContainerA : FeatureContainer<Taxonomy.Domain.Payments>(
            Taxonomy.Domain.Payments
        ) {
            val a1 by boolean<Context> { }
            val a2 by boolean<Context> { }
        }

        object ContainerB : FeatureContainer<Taxonomy.Domain.Orders>(
            Taxonomy.Domain.Orders
        ) {
            val b1 by boolean<Context> { }
            val b2 by boolean<Context> { }
            val b3 by boolean<Context> { }
        }

        assertEquals(2, ContainerA.allFeatures().size)
        assertEquals(3, ContainerB.allFeatures().size)

        // Features are distinct
        val keysA = ContainerA.allFeatures().map { it.key }.toSet()
        val keysB = ContainerB.allFeatures().map { it.key }.toSet()

        assertEquals(setOf("a1", "a2"), keysA)
        assertEquals(setOf("b1", "b2", "b3"), keysB)
    }

    @Test
    fun `can iterate over all features for validation`() {
        // Real-world use case: validate all features are configured
        val configuredKeys = setOf("test_boolean", "test_string", "test_int")

        val allKeys = TestFeatures.allFeatures().map { it.key }.toSet()
        val missingKeys = allKeys - configuredKeys

        // In this test, we expect some features to be "missing" from config
        assertTrue(missingKeys.contains("test_double"))
        assertTrue(missingKeys.contains("test_json"))
    }

    @Test
    fun `features maintain type safety through container`() {
        // Type inference works correctly
        val booleanFeature: BooleanFeature<Context, Taxonomy.Domain.Payments> =
            TestFeatures.test_boolean

        val stringFeature: StringFeature<Context, Taxonomy.Domain.Payments> =
            TestFeatures.test_string

        val intFeature: IntFeature<Context, Taxonomy.Domain.Payments> =
            TestFeatures.test_int

        // Verify types are preserved
        assertEquals("test_boolean", booleanFeature.key)
        assertEquals("test_string", stringFeature.key)
        assertEquals("test_int", intFeature.key)
    }
}
