package io.amichne.konditional.serialization

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Context.Companion.evaluate
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.FeatureContainer
import io.amichne.konditional.core.features.update
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for NamespaceSnapshotSerializer.
 *
 * Validates that namespace-scoped serialization works correctly,
 * including automatic loading into the namespace registry.
 */
class NamespaceSnapshotSerializerTest {

    private object TestFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
        val boolFlag by boolean<Context>(default = false)
        val stringFlag by string<Context>(default = "default")
    }

    @BeforeEach
    fun setup() {
        // Clear both FeatureRegistry and Namespace.Global registry before each test
        FeatureRegistry.clear()
        Namespace.Global.load(io.amichne.konditional.core.instance.Configuration(emptyMap()))

        // Register test features
        FeatureRegistry.register(TestFeatures.boolFlag)
        FeatureRegistry.register(TestFeatures.stringFlag)
    }

    private fun ctx(
        idHex: String,
        locale: AppLocale = AppLocale.EN_US,
        platform: Platform = Platform.IOS,
        version: String = "1.0.0",
    ) = Context(locale, platform, Version.parse(version), StableId.of(idHex))

    @Test
    fun `Given namespace with no flags, When serialized, Then produces JSON with empty flags array`() {
        // Start with empty namespace
        Namespace.Global.load(io.amichne.konditional.core.instance.Configuration(emptyMap()))

        val serializer = NamespaceSnapshotSerializer(Namespace.Global)
        val json = serializer.toJson()

        assertNotNull(json)
        assertTrue(json.contains("\"flags\""))
        assertTrue(json.contains("[]"))
    }

    @Test
    fun `Given namespace with configured flags, When serialized, Then includes all flags`() {
        TestFeatures.boolFlag.update {
            default(true)
        }
        TestFeatures.stringFlag.update {
            default("test-value")
        }

        val serializer = NamespaceSnapshotSerializer(Namespace.Global)
        val json = serializer.toJson()

        assertNotNull(json)
        assertTrue(json.contains(TestFeatures.boolFlag.key))
        assertTrue(json.contains(TestFeatures.stringFlag.key))
        assertTrue(json.contains("\"value\": true"))
        assertTrue(json.contains("\"value\": \"test-value\""))
    }

    @Test
    fun `Given valid JSON, When deserialized, Then loads into namespace and returns success`() {
        val json = """
            {
              "flags" : [
                {
                  "key" : "${TestFeatures.boolFlag.key}",
                  "defaultValue" : {
                    "type" : "BOOLEAN",
                    "value" : true
                  },
                  "salt" : "v1",
                  "isActive" : true,
                  "rules" : []
                }
              ]
            }
        """.trimIndent()

        val serializer = NamespaceSnapshotSerializer(Namespace.Global)
        val result = serializer.fromJson(json)

        assertIs<ParseResult.Success<io.amichne.konditional.core.instance.Configuration>>(result)

        // Verify the flag was loaded into the namespace
        val context = ctx("11111111111111111111111111111111")
        val flagValue = context.evaluate(TestFeatures.boolFlag)
        assertEquals(true, flagValue)
    }

    @Test
    fun `Given invalid JSON, When deserialized, Then returns failure without loading`() {
        val invalidJson = "not valid json"

        val serializer = NamespaceSnapshotSerializer(Namespace.Global)
        val result = serializer.fromJson(invalidJson)

        assertIs<ParseResult.Failure>(result)
        assertIs<ParseError.InvalidJson>(result.error)
        assertTrue((result.error as ParseError.InvalidJson).message.contains("global"))
    }

    @Test
    fun `Given JSON with unregistered feature, When deserialized, Then returns failure`() {
        val json = """
            {
              "flags" : [
                {
                  "key" : "unregistered_feature",
                  "defaultValue" : {
                    "type" : "BOOLEAN",
                    "value" : true
                  },
                  "salt" : "v1",
                  "isActive" : true,
                  "rules" : []
                }
              ]
            }
        """.trimIndent()

        val serializer = NamespaceSnapshotSerializer(Namespace.Global)
        val result = serializer.fromJson(json)

        assertIs<ParseResult.Failure>(result)
        assertIs<ParseError.FeatureNotFound>(result.error)
    }

    @Test
    fun `Given namespace, When round-tripped, Then configuration is preserved`() {
        // Configure flags
        TestFeatures.boolFlag.update {
            default(true)
            rule {
                platforms(Platform.IOS)
            } returns false
        }
        TestFeatures.stringFlag.update {
            default("original")
            rule {
                locales(AppLocale.FR_FR)
            } returns "french"
        }

        // Serialize
        val serializer = NamespaceSnapshotSerializer(Namespace.Global)
        val json = serializer.toJson()

        // Clear and deserialize
        Namespace.Global.load(io.amichne.konditional.core.instance.Configuration(emptyMap()))
        val result = serializer.fromJson(json)
        assertIs<ParseResult.Success<io.amichne.konditional.core.instance.Configuration>>(result)

        // Verify flags work correctly
        val iosContext = ctx("11111111111111111111111111111111", platform = Platform.IOS)
        val androidContext = ctx("22222222222222222222222222222222", platform = Platform.ANDROID)
        val frenchContext = ctx("33333333333333333333333333333333", locale = AppLocale.FR_FR)

        assertEquals(false, iosContext.evaluate(TestFeatures.boolFlag))
        assertEquals(true, androidContext.evaluate(TestFeatures.boolFlag))
        assertEquals("french", frenchContext.evaluate(TestFeatures.stringFlag))
        assertEquals("original", iosContext.evaluate(TestFeatures.stringFlag))
    }

    @Test
    fun `Given forModule factory, When created, Then works same as constructor`() {
        TestFeatures.boolFlag.update {
            default(true)
        }

        val serializer = NamespaceSnapshotSerializer.forModule(Namespace.Global)
        val json = serializer.toJson()

        assertNotNull(json)
        assertTrue(json.contains(TestFeatures.boolFlag.key))
    }

    @Test
    fun `Given different taxonomies, When serialized separately, Then each has only its own flags`() {
        // Domain.Payments features
        val PaymentsFeatures = object : FeatureContainer<Namespace.Payments>(Namespace.Payments) {
            val paymentEnabled by boolean<Context>(default = true)
        }

        // Domain.Search features
        val SearchFeatures = object : FeatureContainer<Namespace.Search>(Namespace.Search) {
            val searchEnabled by boolean<Context>(default = false)
        }

        // Register features
        FeatureRegistry.register(PaymentsFeatures.paymentEnabled)
        FeatureRegistry.register(SearchFeatures.searchEnabled)

        // Serialize each namespace
        val paymentsSerializer = NamespaceSnapshotSerializer(Namespace.Payments)
        val searchSerializer = NamespaceSnapshotSerializer(Namespace.Search)

        val paymentsJson = paymentsSerializer.toJson()
        val searchJson = searchSerializer.toJson()

        // Verify separation
        assertTrue(paymentsJson.contains(PaymentsFeatures.paymentEnabled.key))
        assertTrue(!paymentsJson.contains(SearchFeatures.searchEnabled.key))

        assertTrue(searchJson.contains(SearchFeatures.searchEnabled.key))
        assertTrue(!searchJson.contains(PaymentsFeatures.paymentEnabled.key))
    }
}
