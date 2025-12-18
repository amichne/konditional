package io.amichne.konditional.serialization
import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.instance.Configuration
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.fixtures.utilities.update
import io.amichne.konditional.values.FeatureId
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
    private val testNamespace =
        object : Namespace.TestNamespaceFacade("namespace-snapshot-serializer") {
            val boolFlag by boolean<Context>(default = false)
            val stringFlag by string<Context>(default = "default")
        }

    @BeforeEach
    fun setup() {
        // Clear both FeatureRegistry and the namespace registry before each test
        FeatureRegistry.clear()
        testNamespace.load(
            Configuration(emptyMap()),
        )

        // Register test features
        FeatureRegistry.register(testNamespace.boolFlag)
        FeatureRegistry.register(testNamespace.stringFlag)
    }

    private fun ctx(
        idHex: String,
        locale: AppLocale = AppLocale.UNITED_STATES,
        platform: Platform = Platform.IOS,
        version: String = "1.0.0",
    ) = Context(locale, platform, Version.parseUnsafe(version), StableId.of(idHex))

    @Test
    fun `Given namespace with no flags, When serialized, Then produces JSON with empty flags array`() {
        // Start with empty namespace
        testNamespace.load(
            Configuration(emptyMap()),
        )

        val serializer = NamespaceSnapshotSerializer(testNamespace)
        val json = serializer.toJson()

        assertNotNull(json)
        assertTrue(json.contains("\"flags\""))
        assertTrue(json.contains("[]"))
    }

    @Test
    fun `Given namespace with configured flags, When serialized, Then includes all flags`() {
        testNamespace.boolFlag.update(true) {
        }
        testNamespace.stringFlag.update("test-value") {
        }

        val serializer = NamespaceSnapshotSerializer(testNamespace)
        val json = serializer.toJson()

        assertNotNull(json)
        assertTrue(json.contains(testNamespace.boolFlag.id.toString()))
        assertTrue(json.contains(testNamespace.stringFlag.id.toString()))
        assertTrue(json.contains("\"value\": true"))
        assertTrue(json.contains("\"value\": \"test-value\""))
    }

    @Test
    fun `Given valid JSON, When deserialized, Then loads into namespace and returns success`() {
        val json =
            """
            {
              "flags" : [
                {
                  "key" : "${testNamespace.boolFlag.id}",
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

        val serializer = NamespaceSnapshotSerializer(testNamespace)
        val result = serializer.fromJson(json)

        assertIs<ParseResult.Success<Configuration>>(result)

        // Verify the flag was loaded into the namespace
        val context = ctx("11111111111111111111111111111111")
        val flagValue = testNamespace.boolFlag.evaluate(context)
        assertEquals(true, flagValue)
    }

    @Test
    fun `Given invalid JSON, When deserialized, Then returns failure without loading`() {
        val invalidJson = "not valid json"

        val serializer = NamespaceSnapshotSerializer(testNamespace)
        val result = serializer.fromJson(invalidJson)

        assertIs<ParseResult.Failure>(result)
        assertIs<ParseError.InvalidJson>(result.error)
        assertTrue((result.error as ParseError.InvalidJson).message.contains(testNamespace.id))
    }

    @Test
    fun `Given JSON with unregistered feature, When deserialized, Then returns failure`() {
        val json =
            """
            {
              "flags" : [
                {
                  "key" : "${FeatureId.create("global", "unregistered_feature")}",
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

        val serializer = NamespaceSnapshotSerializer(testNamespace)
        val result = serializer.fromJson(json)

        assertIs<ParseResult.Failure>(result)
        assertIs<ParseError.FeatureNotFound>(result.error)
    }

    @Test
    fun `Given namespace, When round-tripped, Then configuration is preserved`() {
        // Configure flags
        testNamespace.boolFlag.update(true) {
            rule(false) {
                platforms(Platform.IOS)
            }
        }
        testNamespace.stringFlag.update("original") {
            rule("french") {
                locales(AppLocale.FRANCE)
            }
        }

        // Serialize
        val serializer = NamespaceSnapshotSerializer(testNamespace)
        val json = serializer.toJson()

        // Clear and deserialize
        testNamespace.load(
            Configuration(emptyMap()),
        )
        val result = serializer.fromJson(json)
        assertIs<ParseResult.Success<Configuration>>(result)

        // Verify flags work correctly
        val iosContext = ctx("11111111111111111111111111111111", platform = Platform.IOS)
        val androidContext = ctx("22222222222222222222222222222222", platform = Platform.ANDROID)
        val frenchContext = ctx("33333333333333333333333333333333", locale = AppLocale.FRANCE)

        assertEquals(false, testNamespace.boolFlag.evaluate(iosContext))
        assertEquals(true, testNamespace.boolFlag.evaluate(androidContext))
        assertEquals("french", testNamespace.stringFlag.evaluate(frenchContext))
        assertEquals("original", testNamespace.stringFlag.evaluate(iosContext))
    }

    @Test
    fun `Given forModule factory, When created, Then works same as constructor`() {
        testNamespace.boolFlag.update(true) {}

        val serializer = NamespaceSnapshotSerializer.forModule(testNamespace)
        val json = serializer.toJson()

        assertNotNull(json)
        assertTrue(json.contains(testNamespace.boolFlag.id.toString()))
    }

    @Test
    fun `Given different containers, When serialized separately, Then each has only its own flags`() {
        val paymentsNamespace =
            object : Namespace.TestNamespaceFacade("payments") {
                val paymentEnabled by boolean<Context>(default = true)
            }

        val searchNamespace =
            object : Namespace.TestNamespaceFacade("search") {
                val searchEnabled by boolean<Context>(default = false)
            }

        // Register features
        FeatureRegistry.register(paymentsNamespace.paymentEnabled)
        FeatureRegistry.register(searchNamespace.searchEnabled)

        // Serialize each namespace
        val paymentsSerializer = NamespaceSnapshotSerializer(paymentsNamespace)
        val searchSerializer = NamespaceSnapshotSerializer(searchNamespace)

        val paymentsJson = paymentsSerializer.toJson()
        val searchJson = searchSerializer.toJson()

        // Verify separation
        assertTrue(paymentsJson.contains(paymentsNamespace.paymentEnabled.id.toString()))
        assertTrue(!paymentsJson.contains(searchNamespace.searchEnabled.id.toString()))

        assertTrue(searchJson.contains(searchNamespace.searchEnabled.id.toString()))
        assertTrue(!searchJson.contains(paymentsNamespace.paymentEnabled.id.toString()))
    }
}
