package io.amichne.konditional.core

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.context.feature
import io.amichne.konditional.core.dsl.buildJsonArray
import io.amichne.konditional.core.dsl.buildJsonObject
import io.amichne.konditional.core.dsl.buildJsonObjectArray
import io.amichne.konditional.core.dsl.jsonObject
import io.amichne.konditional.core.features.FeatureContainer
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.types.json.JsonSchema
import io.amichne.konditional.fixtures.core.id.TestStableId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Integration tests for JSON object and array features in FeatureContainer.
 *
 * Tests demonstrate:
 * - Creating JSON object features with schemas
 * - Creating JSON array features with schemas
 * - Using the DSL to define complex nested structures
 * - Type safety and validation
 */
class JsonFeatureIntegrationTest {

    enum class Theme {
        LIGHT, DARK, AUTO
    }

    enum class Environment {
        DEVELOPMENT, STAGING, PRODUCTION
    }

    // Test namespace
    sealed class TestNamespace : Namespace.TestNamespaceFacade("id:test-namespace") {
        data object Config : TestNamespace()
    }

    // Test context
    data class TestContext(
        val environment: Environment = Environment.DEVELOPMENT,
    ) : Context {
        override val locale: AppLocale = AppLocale.UNITED_STATES
        override val platform: Platform = Platform.IOS
        override val appVersion: Version = Version.default
        override val stableId: StableId = TestStableId
    }

    object TestFeatures : FeatureContainer<TestNamespace.Config>(TestNamespace.Config) {
        val USER_CONFIG by jsonObject<TestContext>(default = buildJsonObject {
            "id" to 1
            "name" to "Default User"
            "email" to "[REDACTED_EMAIL_ADDRESS_1]"
            "theme" to Theme.LIGHT
        }) {

            default(
                buildJsonObject {
                    "id" to 1
                    "name" to "Default User"
                    "email" to "[REDACTED_EMAIL_ADDRESS_1]"
                    "theme" to Theme.LIGHT
                }
            )
        }

        val featureTags by jsonArray<TestContext>(default = buildJsonArray("feature-a", "enabled", "production")) {
            default(buildJsonArray("feature-a", "enabled", "production"))
        }

        private val devConfig = buildJsonObject {
            "debug" to true
            "logLevel" to "DEBUG"
        }

        private val prodConfig = buildJsonObject {
            "debug" to false
            "logLevel" to "INFO"
        }

        val DEBUG_CONFIG by jsonObject<TestContext>(
            default = devConfig
        ) {
            default(devConfig)
            rule {
                custom {
                    it.environment == Environment.PRODUCTION
                }
            } returns prodConfig
        }
    }

    /**
     * Test that JSON object features can be declared and used in a FeatureContainer.
     */
    @Test
    fun `can declare JSON object features in FeatureContainer`() {
        val userSchema = jsonObject {
            requiredField("id") { int() }
            requiredField("name") { string() }
            optionalField("email") { string() }
            optionalField("theme") { enum<Theme>() }
        }

        val defaultUser = buildJsonObject(schema = userSchema) {
            "id" to 1
            "name" to "Default User"
            "email" to "default@example.com"
            "theme" to Theme.LIGHT
        }

        // Verify the feature was created
        val feature = TestFeatures.USER_CONFIG
        assertNotNull(feature)
        assertEquals("USER_CONFIG", feature.key)
        assertEquals(TestNamespace.Config, feature.namespace)
    }

    /**
     * Test that JSON array features can be declared and used in a FeatureContainer.
     */
    @Test
    fun `can declare JSON array features in FeatureContainer`() {
        val defaultTags = buildJsonArray("feature-a", "enabled", "production")

//        object TestFeatures : FeatureContainer<TestNamespace.Config>(TestNamespace.Config) {
//            val FEATURE_TAGS by jsonArray(default = defaultTags) {
//                default(defaultTags)
//            }
//        }
//
        // Verify the feature was created
        val feature = TestFeatures.featureTags
        assertNotNull(feature)
        assertEquals("FEATURE_TAGS", feature.key)
        assertEquals(TestNamespace.Config, feature.namespace)
    }

    /**
     * Test complex nested JSON object structures.
     */
    @Test
    fun `can create complex nested JSON object features`() {
        val configSchema = jsonObject {
            requiredField("appName") { string() }
            requiredField("version") { string() }
            optionalField("settings") {
                jsonObject {
                    optionalField("theme") { enum<Theme>() }
                    optionalField("notifications") { boolean() }
                    optionalField("timeout") { double() }
                }
            }
            optionalField("features") { array { string() } }
        }

        val defaultConfig = buildJsonObject(schema = configSchema) {
            "appName" to "MyApp"
            "version" to "1.0.0"
            "settings" to buildJsonObject {
                "theme" to Theme.DARK
                "notifications" to true
                "timeout" to 30.0
            }
            "features" to buildJsonArray("feature-a", "feature-b")
        }

        val feature = TestFeatures.featureTags
        assertNotNull(feature)
        assertEquals("APP_CONFIG", feature.key)
    }

    /**
     * Test JSON object arrays (arrays of objects with shared schema).
     */
    @Test
    fun `can create JSON array of objects feature`() {
        val userSchema = jsonObject {
            requiredField("id") { int() }
            requiredField("name") { string() }
            optionalField("active") { boolean() }
        }

        val defaultUsers = buildJsonObjectArray(schema = userSchema) {
            add {
                "id" to 1
                "name" to "Alice"
                "active" to true
            }
            add {
                "id" to 2
                "name" to "Bob"
                "active" to false
            }
        }

        val feature = TestFeatures.featureTags
        assertNotNull(feature)
        assertEquals("featureTags", feature.key)
        assertEquals(2, defaultUsers.size)
    }

    /**
     * Test that schemas are properly attached and validated.
     */
    @Test
    fun `JSON values have schemas attached`() {
        val schema = jsonObject {
            requiredField("id") { int() }
            requiredField("name") { string() }
        }

        val jsonObject = buildJsonObject(schema = schema) {
            "id" to 123
            "name" to "Test"
        }

        assertNotNull(jsonObject.schema)
        assertEquals(schema, jsonObject.schema)

        val result = jsonObject.validate(schema)
        assertTrue(result.isValid)
    }

    /**
     * Test typed array builders for different primitive types.
     */
    @Test
    fun `can build typed arrays of primitives`() {
        val stringArray = buildJsonArray("one", "two", "three")
        assertEquals(3, stringArray.size)
        assertEquals(JsonSchema.StringSchema, stringArray.elementSchema)

        val intArray = buildJsonArray(1, 2, 3, 4, 5)
        assertEquals(5, intArray.size)
        assertEquals(JsonSchema.IntSchema, intArray.elementSchema)

        val boolArray = buildJsonArray(true, false, true)
        assertEquals(3, boolArray.size)
        assertEquals(JsonSchema.BooleanSchema, boolArray.elementSchema)

        val doubleArray = buildJsonArray(1.5, 2.5, 3.5)
        assertEquals(3, doubleArray.size)
        assertEquals(JsonSchema.DoubleSchema, doubleArray.elementSchema)
    }

    /**
     * Test convenience methods for required/optional fields.
     */
    @Test
    fun `requiredField and optionalField create correct schemas`() {
        val schema = jsonObject {
            requiredField("id") { int() }
            requiredField("name") { string() }
            optionalField("email") { string() }
            optionalField("age") { int() }
        }

        assertEquals(4, schema.fields.size)
        assertTrue(schema.fields["id"]?.required == true)
        assertTrue(schema.fields["name"]?.required == true)
        assertTrue(schema.fields["email"]?.required == false)
        assertTrue(schema.fields["age"]?.required == false)
        assertEquals(setOf("id", "name"), schema.required)
    }

    /**
     * Test empty array creation with schema.
     */
    @Test
    fun `can create empty arrays with element schema`() {
        val emptyStringArray = buildJsonArray { string() }
        assertEquals(0, emptyStringArray.size)
        assertEquals(JsonSchema.StringSchema, emptyStringArray.elementSchema)

        val emptyIntArray = buildJsonArray { int() }
        assertEquals(0, emptyIntArray.size)
        assertEquals(JsonSchema.IntSchema, emptyIntArray.elementSchema)

        val emptyObjectArray = buildJsonArray {
            jsonObject {
                field("id") { int() }
            }
        }
        assertEquals(0, emptyObjectArray.size)
        assertNotNull(emptyObjectArray.elementSchema)
        assertTrue(emptyObjectArray.elementSchema is JsonSchema.ObjectSchema)
    }

    /**
     * Test that accessing JSON values is type-safe.
     */
    @Test
    fun `can access typed values from JSON objects`() {
        val obj = buildJsonObject {
            "id" to 123
            "name" to "Test User"
            "enabled" to true
            "score" to 95.5
        }

        assertEquals(123, obj.getTyped<Int>("id"))
        assertEquals("Test User", obj.getTyped<String>("name"))
        assertEquals(true, obj.getTyped<Boolean>("enabled"))
        assertEquals(95.5, obj.getTyped<Double>("score"))
    }

    /**
     * Test full feature lifecycle with rules.
     */
    @Test
    fun `JSON features support rules and targeting`() {
        val feature = TestFeatures.DEBUG_CONFIG
        assertNotNull(feature)
        assertEquals("DEBUG_CONFIG", feature.key)
    }
}
