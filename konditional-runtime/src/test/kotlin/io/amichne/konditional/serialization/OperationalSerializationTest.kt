@file:OptIn(io.amichne.konditional.internal.KonditionalInternalApi::class)

package io.amichne.konditional.serialization

import io.amichne.konditional.api.axisValues
import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.unaryPlus
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.core.result.getOrThrow
import io.amichne.konditional.fixtures.TestContext
import io.amichne.konditional.fixtures.TestEnvironment
import io.amichne.konditional.runtime.load
import io.amichne.konditional.serialization.instance.Configuration
import io.amichne.konditional.serialization.instance.ConfigurationMetadata
import io.amichne.konditional.serialization.options.SnapshotLoadOptions
import io.amichne.konditional.serialization.options.SnapshotWarning
import io.amichne.konditional.serialization.snapshot.ConfigurationSnapshotCodec
import io.amichne.konditional.serialization.snapshot.NamespaceSnapshotLoader
import io.amichne.konditional.values.FeatureId
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OperationalSerializationTest {

    @Test
    fun `skipUnknownKeys loads known flags and emits warning`() {
        val namespace = object : Namespace("lenient-${UUID.randomUUID()}") {
            val knownFeature by boolean<TestContext>(default = false)
        }

        val unknownKey = FeatureId.create(namespace.id, "missing-${UUID.randomUUID()}")
        val snapshotJson = """
            {
              "flags" : [
                {
                  "key" : "${namespace.knownFeature.id}",
                  "defaultValue" : {
                    "type" : "BOOLEAN",
                    "value" : false
                  },
                  "salt" : "v1",
                  "isActive" : true,
                  "rules" : []
                },
                {
                  "key" : "$unknownKey",
                  "defaultValue" : {
                    "type" : "BOOLEAN",
                    "value" : false
                  },
                  "salt" : "v1",
                  "isActive" : true,
                  "rules" : []
                }
              ]
            }
        """.trimIndent()

        val strictResult = ConfigurationSnapshotCodec.decode(snapshotJson)
        assertIs<ParseResult.Failure>(strictResult)
        assertIs<ParseError.FeatureNotFound>(strictResult.error)

        val warnings = mutableListOf<SnapshotWarning>()
        val lenient = SnapshotLoadOptions.skipUnknownKeys { warnings.add(it) }
        val lenientResult = ConfigurationSnapshotCodec.decode(snapshotJson, lenient)
        assertIs<ParseResult.Success<Configuration>>(lenientResult)
        assertEquals(setOf(namespace.knownFeature), lenientResult.value.flags.keys)
        assertEquals(1, warnings.size)
        assertEquals(SnapshotWarning.Kind.UNKNOWN_FEATURE_KEY, warnings.single().kind)
    }

    @Test
    fun `axis constraints roundtrip preserves evaluation semantics`() {
        val namespace = object : Namespace("axis-roundtrip-${UUID.randomUUID()}") {
            val envScopedFlag by boolean<TestContext>(default = false) {
                rule(true) {
                    axis(TestEnvironment.PROD)
                }
            }
        }

        val productionContext = TestContext(
            appVersion = Version.parse("1.0.0").getOrThrow(),
            axisValues = axisValues {
                +TestEnvironment.PROD
            },
        )
        val developementContext = TestContext(
            appVersion = Version.parse("1.0.0").getOrThrow(),
            axisValues = axisValues {
                +TestEnvironment.DEV
            },
        )

        assertTrue(namespace.envScopedFlag.evaluate(productionContext))
        assertFalse(namespace.envScopedFlag.evaluate(developementContext))

        val json = ConfigurationSnapshotCodec.encode(namespace.configuration)

        // Reset to a configuration that still contains the feature key but has no rules.
        namespace.load(
            Configuration(
                flags = mapOf(
                    namespace.envScopedFlag to FlagDefinition(
                        feature = namespace.envScopedFlag,
                        bounds = emptyList(),
                        defaultValue = false,
                    )
                )
            )
        )
        assertFalse(
            namespace.envScopedFlag.evaluate(productionContext),
            "Sanity: after resetting rules, flag should be default"
        )

        val loaded = NamespaceSnapshotLoader(namespace).load(json)
        assertIs<ParseResult.Success<Configuration>>(loaded)

        assertTrue(namespace.envScopedFlag.evaluate(productionContext))
        assertFalse(namespace.envScopedFlag.evaluate(developementContext))
    }

    @Test
    fun `configuration metadata roundtrips via snapshot json`() {
        val config = Configuration(
            flags = emptyMap(),
            metadata = ConfigurationMetadata(
                version = "rev-123",
                generatedAtEpochMillis = 1_700_000_000_000,
                source = "unit-test",
            ),
        )

        val json = ConfigurationSnapshotCodec.encode(config)
        val parsed = ConfigurationSnapshotCodec.decode(json)
        assertIs<ParseResult.Success<Configuration>>(parsed)
        assertEquals(config.metadata, parsed.value.metadata)
    }
}
