@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.core

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.features.EnumFeature
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.runtime.load
import io.amichne.konditional.serialization.instance.Configuration
import io.amichne.konditional.serialization.instance.MaterializedConfiguration
import io.amichne.konditional.serialization.snapshot.ConfigurationSnapshotCodec
import io.amichne.konditional.serialization.snapshot.NamespaceSnapshotLoader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

/**
 * Test suite demonstrating Enum feature functionality
 */
class EnumFeatureTest {

    // Test enums
    enum class LogLevel {
        DEBUG, INFO, WARN, ERROR
    }

    enum class Theme {
        LIGHT, DARK, AUTO
    }

    enum class Environment {
        DEVELOPMENT, STAGING, PRODUCTION
    }

    // Test features with enum types
    private object EnumFeatures : Namespace.TestNamespaceFacade("enum-features") {
        val logLevel by enum<LogLevel, Context>(default = LogLevel.INFO) {
            rule(LogLevel.DEBUG) {
                android()
            }
        }

        val theme by enum<Theme, Context>(default = Theme.AUTO)
        val environment by enum<Environment, Context>(default = Environment.PRODUCTION)
    }

    @Test
    fun `enum features have correct keys`() {
        assertEquals("logLevel", EnumFeatures.logLevel.key)
        assertEquals("theme", EnumFeatures.theme.key)
        assertEquals("environment", EnumFeatures.environment.key)
    }

    @Test
    fun `enum features have correct namespace`() {
        assertEquals(EnumFeatures, EnumFeatures.logLevel.namespace)
        assertEquals(EnumFeatures, EnumFeatures.theme.namespace)
        assertEquals(EnumFeatures, EnumFeatures.environment.namespace)
    }

    @Test
    fun `enum features return default values`() {
        val context = Context(
            locale = AppLocale.UNITED_STATES,
            platform = Platform.IOS,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("12345678901234567890123456789012")
        )

        // Should return defaults when no rules match
        assertEquals(LogLevel.INFO, EnumFeatures.logLevel.evaluate(context))
        assertEquals(Theme.AUTO, EnumFeatures.theme.evaluate(context))
        assertEquals(
            Environment.PRODUCTION,
            EnumFeatures.environment.evaluate(context)
        )
    }

    @Test
    fun `enum features evaluate with rules`() {
        val androidContext = Context(
            locale = AppLocale.UNITED_STATES,
            platform = Platform.ANDROID,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("12345678901234567890123456789012")
        )

        // Should return DEBUG for Android platform based on rule
        assertEquals(LogLevel.DEBUG, EnumFeatures.logLevel.evaluate(androidContext))
    }

    @Test
    fun `multiple enum types can coexist in feature container`() {
        // Create a namespace with multiple enum types
        val mixedFeatures = object : Namespace.TestNamespaceFacade("mixed-enums") {
            val level by enum<LogLevel, Context>(default = LogLevel.INFO)
            val themePreference by enum<Theme, Context>(default = Theme.LIGHT)
            val env by enum<Environment, Context>(default = Environment.DEVELOPMENT)
        }

        // Access properties to trigger registration
        mixedFeatures.level
        mixedFeatures.themePreference
        mixedFeatures.env

        // Verify all are registered
        assertEquals(3, mixedFeatures.allFeatures().size)
        val keys = mixedFeatures.allFeatures().map { it.key }.toSet()
        assertEquals(setOf("level", "themePreference", "env"), keys)
    }

    @Test
    fun `enum features work alongside primitive types in container`() {
        val mixedTypeFeatures = object : Namespace.TestNamespaceFacade("mixed-types") {
            val enableLogging by boolean<Context>(default = true)
            val logLevel by enum<LogLevel, Context>(default = LogLevel.INFO)
            val maxLogSize by integer<Context>(default = 1000)
            val logPrefix by string<Context>(default = "[LOG]")
        }

        // Access all properties
        mixedTypeFeatures.enableLogging
        mixedTypeFeatures.logLevel
        mixedTypeFeatures.maxLogSize
        mixedTypeFeatures.logPrefix

        assertEquals(4, mixedTypeFeatures.allFeatures().size)
    }

    @Test
    fun `enum features maintain type safety through container`() {
        // Type inference works correctly
        val logLevelFeature: EnumFeature<LogLevel, Context, EnumFeatures> =
            EnumFeatures.logLevel

        val themeFeature: EnumFeature<Theme, Context, EnumFeatures> =
            EnumFeatures.theme

        // Verify types are preserved
        assertEquals("logLevel", logLevelFeature.key)
        assertEquals("theme", themeFeature.key)
    }

    @Test
    fun `enum features can have complex rule configurations`() {
        val complexFeatures = object : Namespace.TestNamespaceFacade("complex-enum-rules") {
            val environment by enum<Environment, Context>(default = Environment.PRODUCTION) {
                rule(Environment.DEVELOPMENT) {
                    android()
                    locales(AppLocale.UNITED_STATES)
                }

                rule(Environment.STAGING) {
                    ios()
                }
            }
        }

        val androidUSContext = Context(
            locale = AppLocale.UNITED_STATES,
            platform = Platform.ANDROID,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("12345678901234567890123456789012")
        )

        val iosContext = Context(
            locale = AppLocale.CANADA,
            platform = Platform.IOS,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("12345678901234567890123456789012")
        )

        val androidContext = Context(
            locale = AppLocale.UNITED_KINGDOM,
            platform = Platform.ANDROID,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("12345678901234567890123456789012")
        )

        assertEquals(Environment.DEVELOPMENT, complexFeatures.environment.evaluate(androidUSContext))
        assertEquals(Environment.STAGING, complexFeatures.environment.evaluate(iosContext))
        assertEquals(
            Environment.PRODUCTION,
            complexFeatures.environment.evaluate(androidContext)
        )
    }

    enum class SingleValue {
        ONLY_VALUE
    }

    @Test
    fun `enum with single value works correctly`() {
        val singleEnumFeature = object : Namespace.TestNamespaceFacade("single-enum") {
            val single by enum<SingleValue, Context>(default = SingleValue.ONLY_VALUE)
        }

        val context = Context(
            locale = AppLocale.UNITED_STATES,
            platform = Platform.ANDROID,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("12345678901234567890123456789012")
        )

        assertEquals(SingleValue.ONLY_VALUE, singleEnumFeature.single.evaluate(context))
    }

    @Test
    fun `enum values survive namespace snapshot roundtrip`() {
        val namespace = object : Namespace.TestNamespaceFacade("enum-roundtrip") {
            val logLevel by enum<LogLevel, Context>(default = LogLevel.INFO) {
                rule(LogLevel.DEBUG) { android() }
            }
        }

        val androidContext = Context(
            locale = AppLocale.UNITED_STATES,
            platform = Platform.ANDROID,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("12345678901234567890123456789012")
        )

        val json = ConfigurationSnapshotCodec.encode(namespace.configuration)
        namespace.load(MaterializedConfiguration.of(namespace.compiledSchema(), Configuration(emptyMap())))

        val reloadResult = NamespaceSnapshotLoader(namespace).load(json)
        assertTrue(reloadResult.isSuccess)

        assertEquals(LogLevel.DEBUG, namespace.logLevel.evaluate(androidContext))
    }
}
