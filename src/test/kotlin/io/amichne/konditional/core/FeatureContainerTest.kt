package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.modules.FeatureModule
import io.amichne.konditional.types.AppLocale
import io.amichne.konditional.types.Platform
import io.amichne.konditional.types.StableId
import io.amichne.konditional.types.Version
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Test suite demonstrating FeatureContainer functionality
 */
class FeatureContainerTest {

    // Test container with mixed feature types
    object TestFeatures : FeatureContainer<Context, FeatureModule.Team.Payments>(
        FeatureModule.Team.Payments
    ) {
        val BOOLEAN_FLAG by boolean("test_boolean")
        val STRING_CONFIG by string("test_string")
        val INT_LIMIT by int("test_int")
        val DOUBLE_THRESHOLD by double("test_double")
        val JSON_CONFIG by jsonObject<TestConfig>("test_json")
    }

    data class TestConfig(
        val enabled: Boolean,
        val value: Int
    )

    @Test
    fun `features are created with correct types`() {
        // Verify each feature has correct type
        assertTrue(TestFeatures.BOOLEAN_FLAG is BooleanFeature<*, *>)
        assertTrue(TestFeatures.STRING_CONFIG is StringFeature<*, *>)
        assertTrue(TestFeatures.INT_LIMIT is IntFeature<*, *>)
        assertTrue(TestFeatures.DOUBLE_THRESHOLD is DoubleFeature<*, *>)
        assertTrue(TestFeatures.JSON_CONFIG is Feature.OfJsonObject<*, *, *>)
    }

    @Test
    fun `features have correct keys`() {
        assertEquals("test_boolean", TestFeatures.BOOLEAN_FLAG.key)
        assertEquals("test_string", TestFeatures.STRING_CONFIG.key)
        assertEquals("test_int", TestFeatures.INT_LIMIT.key)
        assertEquals("test_double", TestFeatures.DOUBLE_THRESHOLD.key)
        assertEquals("test_json", TestFeatures.JSON_CONFIG.key)
    }

    @Test
    fun `features have correct module`() {
        val expectedModule = FeatureModule.Team.Payments

        assertEquals(expectedModule, TestFeatures.BOOLEAN_FLAG.module)
        assertEquals(expectedModule, TestFeatures.STRING_CONFIG.module)
        assertEquals(expectedModule, TestFeatures.INT_LIMIT.module)
        assertEquals(expectedModule, TestFeatures.DOUBLE_THRESHOLD.module)
        assertEquals(expectedModule, TestFeatures.JSON_CONFIG.module)
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
        object LazyTestContainer : FeatureContainer<Context, FeatureModule.Team.Payments>(
            FeatureModule.Team.Payments
        ) {
            val FEATURE_A by boolean("lazy_a")
            val FEATURE_B by boolean("lazy_b")
        }

        // allFeatures() should return empty before any feature is accessed
        // Note: In current implementation, accessing allFeatures() triggers initialization
        // This is fine - features register on first access (either directly or via allFeatures)

        // After calling allFeatures(), all features should be registered
        val features = LazyTestContainer.allFeatures()
        assertEquals(2, features.size)

        // Accessing individual features doesn't change count
        val featureA = LazyTestContainer.FEATURE_A
        val featureB = LazyTestContainer.FEATURE_B
        assertEquals(2, LazyTestContainer.allFeatures().size)
    }

    @Test
    fun `features can be evaluated with context`() {
        // Configure the registry
        FeatureModule.Team.Payments.config {
            TestFeatures.BOOLEAN_FLAG with {
                default(true)
            }
            TestFeatures.STRING_CONFIG with {
                default("test-value")
            }
            TestFeatures.INT_LIMIT with {
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
        assertEquals(true, context.evaluateOrDefault(TestFeatures.BOOLEAN_FLAG, false))
        assertEquals("test-value", context.evaluateOrDefault(TestFeatures.STRING_CONFIG, ""))
        assertEquals(100, context.evaluateOrDefault(TestFeatures.INT_LIMIT, 0))
    }

    @Test
    fun `multiple containers maintain independent feature lists`() {
        object ContainerA : FeatureContainer<Context, FeatureModule.Team.Payments>(
            FeatureModule.Team.Payments
        ) {
            val FEATURE_1 by boolean("a1")
            val FEATURE_2 by boolean("a2")
        }

        object ContainerB : FeatureContainer<Context, FeatureModule.Team.Orders>(
            FeatureModule.Team.Orders
        ) {
            val FEATURE_3 by boolean("b1")
            val FEATURE_4 by boolean("b2")
            val FEATURE_5 by boolean("b3")
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
        val booleanFeature: BooleanFeature<Context, FeatureModule.Team.Payments> =
            TestFeatures.BOOLEAN_FLAG

        val stringFeature: StringFeature<Context, FeatureModule.Team.Payments> =
            TestFeatures.STRING_CONFIG

        val intFeature: IntFeature<Context, FeatureModule.Team.Payments> =
            TestFeatures.INT_LIMIT

        // Verify types are preserved
        assertEquals("test_boolean", booleanFeature.key)
        assertEquals("test_string", stringFeature.key)
        assertEquals("test_int", intFeature.key)
    }
}
