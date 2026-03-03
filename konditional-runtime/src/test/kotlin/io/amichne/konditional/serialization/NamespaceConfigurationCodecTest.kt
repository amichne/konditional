@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.serialization

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.disable
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.parseErrorOrNull
import io.amichne.konditional.core.schema.CompiledNamespaceSchema
import io.amichne.konditional.fixtures.utilities.update
import io.amichne.konditional.runtime.dump
import io.amichne.konditional.runtime.json
import io.amichne.konditional.runtime.update
import io.amichne.konditional.serialization.instance.Configuration
import io.amichne.konditional.serialization.instance.ConfigurationMetadata
import io.amichne.konditional.serialization.options.SnapshotLoadOptions
import io.amichne.konditional.serialization.snapshot.ConfigurationCodec
import io.amichne.konditional.serialization.snapshot.NamespaceSnapshotLoader
import io.amichne.konditional.values.FeatureId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for NamespaceSnapshotLoader.
 *
 * Validates the enforced boundary:
 * - Encoding is done via namespace-scoped [io.amichne.konditional.runtime.dump]
 * - Loading into a namespace is done via [NamespaceSnapshotLoader]
 */
class NamespaceConfigurationCodecTest {
    private val testNamespace =
        object : Namespace.TestNamespaceFacade("namespace-snapshot-serializer") {
            val boolFlag by boolean<Context>(default = false)
            val stringFlag by string<Context>(default = "default")
        }

    @BeforeEach
    fun setup() {
        // Reset namespace registry state before each test
        testNamespace.update(declaredDefaultConfiguration())
    }

    private fun declaredDefaultConfiguration(): Configuration {
        val schema = CompiledNamespaceSchema.from(testNamespace)
        val flags = schema.entriesById.values.associate { entry -> entry.feature to entry.declaredDefinition }
        return Configuration(flags)
    }

    private fun ctx(
        idHex: String,
        locale: AppLocale = AppLocale.UNITED_STATES,
        platform: Platform = Platform.IOS,
        version: String = "1.0.0",
    ) = Context(locale, platform, Version.parse(version).getOrThrow(), StableId.of(idHex))

    @Test
    fun `Given namespace with no flags, When serialized, Then produces JSON with empty flags array`() {
        // Start with empty namespace
        testNamespace.update(declaredDefaultConfiguration())

        val json = testNamespace.dump()

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

        val json = testNamespace.dump()

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

        val result =
            NamespaceSnapshotLoader.forNamespace(testNamespace).load(
                json,
                options = SnapshotLoadOptions.fillMissingDeclaredFlags(),
            )

        assertTrue(result.isSuccess)

        // Verify the flag was loaded into the namespace
        val context = ctx("11111111111111111111111111111111")
        val flagValue = testNamespace.boolFlag.evaluate(context)
        assertEquals(true, flagValue)
    }

    @Test
    fun `Given invalid JSON, When deserialized, Then returns failure without loading`() {
        val invalidJson = "not valid json"

        val result = NamespaceSnapshotLoader.forNamespace(testNamespace).load(invalidJson)

        assertTrue(result.isFailure)
        val error = assertIs<ParseError.InvalidJson>(result.parseErrorOrNull())
        assertTrue(error.message.contains(testNamespace.id))
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

        val result =
            NamespaceSnapshotLoader.forNamespace(testNamespace).load(
                json,
                options = SnapshotLoadOptions.fillMissingDeclaredFlags(),
            )

        assertTrue(result.isFailure)
        assertIs<ParseError.FeatureNotFound>(result.parseErrorOrNull())
    }

    @Test
    fun `Given namespace, When round-tripped, Then configuration is preserved`() {
        // Configure flags
        testNamespace.boolFlag.update(true) {
            disable {
                platforms(Platform.IOS)
            }
        }
        testNamespace.stringFlag.update("original") {
            rule("french") {
                locales(AppLocale.FRANCE)
            }
        }

        // Serialize
        val json = testNamespace.dump()

        // Clear and deserialize
        testNamespace.update(declaredDefaultConfiguration())
        val result = NamespaceSnapshotLoader.forNamespace(testNamespace).load(json)
        assertTrue(result.isSuccess)

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
    fun `namespace json property delegates to dump`() {
        testNamespace.boolFlag.update(true) {}

        assertEquals(testNamespace.dump(), testNamespace.json)
    }

    @Test
    fun `namespace dump roundtrip preserves exact configuration equality`() {
        testNamespace.boolFlag.update(true) {
            disable {
                platforms(Platform.IOS)
            }
        }
        testNamespace.stringFlag.update("serialized") {
            rule("fr") {
                locales(AppLocale.FRANCE)
            }
        }

        val initial =
            Configuration(
                flags = testNamespace.configuration.flags.toMap(),
                metadata =
                    ConfigurationMetadata(
                        version = testNamespace.configuration.metadata.version,
                        generatedAtEpochMillis = testNamespace.configuration.metadata.generatedAtEpochMillis,
                        source = testNamespace.configuration.metadata.source,
                    ),
            )

        val json = testNamespace.dump()
        val output = ConfigurationCodec.decode(json, testNamespace).getOrThrow()

        assertEquals(initial, output)
    }

    @Test
    fun `Given forModule factory, When created, Then works same as constructor`() {
        testNamespace.boolFlag.update(true) {}

        val json = testNamespace.dump()

        val loader = NamespaceSnapshotLoader.forNamespace(testNamespace)
        val loaded = loader.load(json, options = SnapshotLoadOptions.fillMissingDeclaredFlags())

        assertNotNull(json)
        assertTrue(json.contains(testNamespace.boolFlag.id.toString()))
        assertTrue(loaded.isSuccess)
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

        // Serialize each namespace
        val paymentsJson = paymentsNamespace.dump()
        val searchJson = searchNamespace.dump()

        // Verify separation
        assertTrue(paymentsJson.contains(paymentsNamespace.paymentEnabled.id.toString()))
        assertTrue(!paymentsJson.contains(searchNamespace.searchEnabled.id.toString()))

        assertTrue(searchJson.contains(searchNamespace.searchEnabled.id.toString()))
        assertTrue(!searchJson.contains(paymentsNamespace.paymentEnabled.id.toString()))
    }

    @Test
    fun `namespace loader deserializes without relying on global feature registry`() {
        testNamespace.boolFlag.update(true) {}
        val json = testNamespace.dump()

        testNamespace.update(declaredDefaultConfiguration())
        val result =
            NamespaceSnapshotLoader.forNamespace(testNamespace).load(
                json,
                options = SnapshotLoadOptions.fillMissingDeclaredFlags(),
            )

        assertTrue(result.isSuccess)
        assertEquals(true, testNamespace.boolFlag.evaluate(ctx("11111111111111111111111111111111")))
    }

    @Test
    fun `namespace loader rejects snapshot from different namespace`() {
        val otherNamespace =
            object : Namespace.TestNamespaceFacade("other-namespace") {
                val otherFlag by boolean<Context>(default = true)
            }

        val otherJson = otherNamespace.dump()
        val result = NamespaceSnapshotLoader.forNamespace(testNamespace).load(otherJson)

        assertTrue(result.isFailure)
        assertIs<ParseError.FeatureNotFound>(result.parseErrorOrNull())
    }
}
