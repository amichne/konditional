@file:Suppress("DEPRECATION")

package io.amichne.konditional.serialization

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.parseErrorOrNull
import io.amichne.konditional.values.FeatureId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for FeatureRegistry.
 *
 * Validates feature registration, retrieval, and error handling.
 */
class FeatureRegistryTest {

    private object TestFeatures : Namespace.TestNamespaceFacade("feature-registry") {
        val feature1 by boolean<Context>(default = false)
        val feature2 by string<Context>(default = "default")
        val feature3 by integer<Context>(default = 0)
    }

    @BeforeEach
    fun setup() {
        // Clear registry before each test
        FeatureRegistry.clear()
    }

    // ========== Registration Tests ==========

    @Test
    fun `Given feature, When registered, Then can be retrieved by key`() {
        FeatureRegistry.register(TestFeatures.feature1)

        val result = FeatureRegistry.get(TestFeatures.feature1.id)

        assertTrue(result.isSuccess)
        assertEquals(TestFeatures.feature1, result.getOrThrow())
    }

    @Test
    fun `Given multiple features, When registered, Then all can be retrieved`() {
        FeatureRegistry.register(TestFeatures.feature1)
        FeatureRegistry.register(TestFeatures.feature2)
        FeatureRegistry.register(TestFeatures.feature3)

        val result1 = FeatureRegistry.get(TestFeatures.feature1.id)
        val result2 = FeatureRegistry.get(TestFeatures.feature2.id)
        val result3 = FeatureRegistry.get(TestFeatures.feature3.id)

        assertTrue(result1.isSuccess)
        assertTrue(result2.isSuccess)
        assertTrue(result3.isSuccess)

        assertEquals(TestFeatures.feature1, result1.getOrThrow())
        assertEquals(TestFeatures.feature2, result2.getOrThrow())
        assertEquals(TestFeatures.feature3, result3.getOrThrow())
    }

    @Test
    fun `Given feature, When registered twice, Then registration is idempotent`() {
        FeatureRegistry.register(TestFeatures.feature1)
        FeatureRegistry.register(TestFeatures.feature1) // Register again

        val result = FeatureRegistry.get(TestFeatures.feature1.id)

        assertTrue(result.isSuccess)
        assertEquals(TestFeatures.feature1, result.getOrThrow())
    }

    // ========== Retrieval Tests ==========

    @Test
    fun `Given unregistered key, When retrieved, Then returns FeatureNotFound error`() {
        val result = FeatureRegistry.get(FeatureId.create("test", "nonexistent_key"))
        assertTrue(result.isFailure)
        val error = result.parseErrorOrNull()
        assertTrue(error is ParseError.FeatureNotFound)
        assertEquals(
            FeatureId.create("test", "nonexistent_key"), (error as ParseError.FeatureNotFound).key
        )
    }

    @Test
    fun `Given registered feature, When contains checked, Then returns true`() {
        FeatureRegistry.register(TestFeatures.feature1)

        val result = FeatureRegistry.contains(TestFeatures.feature1.id)

        assertTrue(result)
    }

    @Test
    fun `Given unregistered feature, When contains checked, Then returns false`() {
        val result = FeatureRegistry.contains(
            FeatureId.create("test", "nonexistent_key")
        )

        assertFalse(result)
    }

    // ========== Clear Tests ==========

    @Test
    fun `Given registered features, When cleared, Then all features are removed`() {
        FeatureRegistry.register(TestFeatures.feature1)
        FeatureRegistry.register(TestFeatures.feature2)

        FeatureRegistry.clear()

        val result1 = FeatureRegistry.get(TestFeatures.feature1.id)
        val result2 = FeatureRegistry.get(TestFeatures.feature2.id)

        assertTrue(result1.isFailure)
        assertTrue(result2.isFailure)
    }

    @Test
    fun `Given empty registry, When cleared, Then no error occurs`() {
        // Should not throw
        FeatureRegistry.clear()

        val result = FeatureRegistry.get(
            FeatureId.create("test", "any_key")
        )
        assertTrue(result.isFailure)
    }

    // ========== Multiple Features with Same Key Tests ==========

    @Test
    fun `Given features with different keys, When registered, Then both are stored separately`() {
        val anotherContainer = object : Namespace.TestNamespaceFacade("feature-registry-other") {
            val differentFeature by boolean<Context>(default = false)
        }

        FeatureRegistry.register(TestFeatures.feature1)
        FeatureRegistry.register(anotherContainer.differentFeature)

        val result1 = FeatureRegistry.get(TestFeatures.feature1.id)
        val result2 = FeatureRegistry.get(anotherContainer.differentFeature.id)

        assertTrue(result1.isSuccess)
        assertTrue(result2.isSuccess)

        assertEquals(TestFeatures.feature1, result1.getOrThrow())
        assertEquals(anotherContainer.differentFeature, result2.getOrThrow())
    }

    // ========== Type Preservation Tests ==========

    @Test
    fun `Given boolean feature, When retrieved, Then maintains type information`() {
        FeatureRegistry.register(TestFeatures.feature1)

        val result = FeatureRegistry.get(TestFeatures.feature1.id)

        assertTrue(result.isSuccess)
        val feature = result.getOrThrow()
        assertEquals(TestFeatures.feature1.key, feature.key)
    }

    @Test
    fun `Given string feature, When retrieved, Then maintains type information`() {
        FeatureRegistry.register(TestFeatures.feature2)

        val result = FeatureRegistry.get(TestFeatures.feature2.id)

        assertTrue(result.isSuccess)
        val feature = result.getOrThrow()
        assertEquals(TestFeatures.feature2.key, feature.key)
    }

    @Test
    fun `Given int feature, When retrieved, Then maintains type information`() {
        FeatureRegistry.register(TestFeatures.feature3)

        val result = FeatureRegistry.get(TestFeatures.feature3.id)

        assertTrue(result.isSuccess)
        val feature = result.getOrThrow()
        assertEquals(TestFeatures.feature3.key, feature.key)
    }
}
