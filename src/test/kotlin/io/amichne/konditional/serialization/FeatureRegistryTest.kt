package io.amichne.konditional.serialization

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.features.FeatureContainer
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.fixtures.core.TestNamespace
import io.amichne.konditional.values.Identifier
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests for FeatureRegistry.
 *
 * Validates feature registration, retrieval, and error handling.
 */
class FeatureRegistryTest {

    private val testNamespace = TestNamespace.test("feature-registry")
    private val TestFeatures = object : FeatureContainer<TestNamespace>(testNamespace) {
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

        assertIs<ParseResult.Success<*>>(result)
        assertEquals(TestFeatures.feature1, result.value)
    }

    @Test
    fun `Given multiple features, When registered, Then all can be retrieved`() {
        FeatureRegistry.register(TestFeatures.feature1)
        FeatureRegistry.register(TestFeatures.feature2)
        FeatureRegistry.register(TestFeatures.feature3)

        val result1 = FeatureRegistry.get(TestFeatures.feature1.id)
        val result2 = FeatureRegistry.get(TestFeatures.feature2.id)
        val result3 = FeatureRegistry.get(TestFeatures.feature3.id)

        assertIs<ParseResult.Success<*>>(result1)
        assertIs<ParseResult.Success<*>>(result2)
        assertIs<ParseResult.Success<*>>(result3)

        assertEquals(TestFeatures.feature1, result1.value)
        assertEquals(TestFeatures.feature2, result2.value)
        assertEquals(TestFeatures.feature3, result3.value)
    }

    @Test
    fun `Given feature, When registered twice, Then registration is idempotent`() {
        FeatureRegistry.register(TestFeatures.feature1)
        FeatureRegistry.register(TestFeatures.feature1) // Register again

        val result = FeatureRegistry.get(TestFeatures.feature1.id)

        assertIs<ParseResult.Success<*>>(result)
        assertEquals(TestFeatures.feature1, result.value)
    }

    // ========== Retrieval Tests ==========

    @Test
    fun `Given unregistered key, When retrieved, Then returns FeatureNotFound error`() {
        val result = FeatureRegistry.get(Identifier("nonexistent_key"))

        assertIs<ParseResult.Failure>(result)
        assertIs<ParseError.FeatureNotFound>(result.error)
        assertEquals(Identifier("nonexistent_key"), (result.error as ParseError.FeatureNotFound).key)
    }

    @Test
    fun `Given registered feature, When contains checked, Then returns true`() {
        FeatureRegistry.register(TestFeatures.feature1)

        val result = FeatureRegistry.contains(TestFeatures.feature1.id)

        assertTrue(result)
    }

    @Test
    fun `Given unregistered feature, When contains checked, Then returns false`() {
        val result = FeatureRegistry.contains(Identifier("nonexistent_key"))

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

        assertIs<ParseResult.Failure>(result1)
        assertIs<ParseResult.Failure>(result2)
    }

    @Test
    fun `Given empty registry, When cleared, Then no error occurs`() {
        // Should not throw
        FeatureRegistry.clear()

        val result = FeatureRegistry.get(Identifier("any_key"))
        assertIs<ParseResult.Failure>(result)
    }

    // ========== Multiple Features with Same Key Tests ==========

    @Test
    fun `Given features with different keys, When registered, Then both are stored separately`() {
        val AnotherContainer = object : FeatureContainer<TestNamespace>(testNamespace) {
            val differentFeature by boolean<Context>(default = false)
        }

        FeatureRegistry.register(TestFeatures.feature1)
        FeatureRegistry.register(AnotherContainer.differentFeature)

        val result1 = FeatureRegistry.get(TestFeatures.feature1.id)
        val result2 = FeatureRegistry.get(AnotherContainer.differentFeature.id)

        assertIs<ParseResult.Success<*>>(result1)
        assertIs<ParseResult.Success<*>>(result2)

        assertEquals(TestFeatures.feature1, result1.value)
        assertEquals(AnotherContainer.differentFeature, result2.value)
    }

    // ========== Type Preservation Tests ==========

    @Test
    fun `Given boolean feature, When retrieved, Then maintains type information`() {
        FeatureRegistry.register(TestFeatures.feature1)

        val result = FeatureRegistry.get(TestFeatures.feature1.id)

        assertIs<ParseResult.Success<Feature<*, *, *>>>(result)
        val feature = result.value
        assertEquals(TestFeatures.feature1.key, feature.key)
    }

    @Test
    fun `Given string feature, When retrieved, Then maintains type information`() {
        FeatureRegistry.register(TestFeatures.feature2)

        val result = FeatureRegistry.get(TestFeatures.feature2.id)

        assertIs<ParseResult.Success<Feature<*, *, *>>>(result)
        val feature = result.value
        assertEquals(TestFeatures.feature2.key, feature.key)
    }

    @Test
    fun `Given int feature, When retrieved, Then maintains type information`() {
        FeatureRegistry.register(TestFeatures.feature3)

        val result = FeatureRegistry.get(TestFeatures.feature3.id)

        assertIs<ParseResult.Success<Feature<*, *, *>>>(result)
        val feature = result.value
        assertEquals(TestFeatures.feature3.key, feature.key)
    }
}
