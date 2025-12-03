package io.amichne.konditional.core

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Context.Companion.evaluate
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.features.EnumFeature
import io.amichne.konditional.core.features.FeatureContainer
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.result.utils.evaluateOrDefault
import io.amichne.konditional.core.types.EncodableEvidence
import io.amichne.konditional.core.types.EncodableValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

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
    object EnumFeatures : FeatureContainer<Namespace.Payments>(
        Namespace.Payments
    ) {
        val logLevel by enum<LogLevel, Context>(default = LogLevel.INFO) {
            rule {
                platforms(Platform.WEB)
            } returns LogLevel.DEBUG
        }

        val theme by enum<Theme, Context>(default = Theme.AUTO)
        val environment by enum<Environment, Context>(default = Environment.PRODUCTION)
    }

    @Test
    fun `enum features are created with correct types`() {
        // Verify each feature has correct type
        assertTrue(EnumFeatures.logLevel is EnumFeature<*, *, *>)
        assertTrue(EnumFeatures.theme is EnumFeature<*, *, *>)
        assertTrue(EnumFeatures.environment is EnumFeature<*, *, *>)
    }

    @Test
    fun `enum features have correct keys`() {
        assertEquals("logLevel", EnumFeatures.logLevel.key)
        assertEquals("theme", EnumFeatures.theme.key)
        assertEquals("environment", EnumFeatures.environment.key)
    }

    @Test
    fun `enum features have correct namespace`() {
        val expectedNamespace = Namespace.Payments

        assertEquals(expectedNamespace, EnumFeatures.logLevel.namespace)
        assertEquals(expectedNamespace, EnumFeatures.theme.namespace)
        assertEquals(expectedNamespace, EnumFeatures.environment.namespace)
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
        assertEquals(LogLevel.INFO, context.evaluateOrDefault(EnumFeatures.logLevel, LogLevel.ERROR))
        assertEquals(Theme.AUTO, context.evaluateOrDefault(EnumFeatures.theme, Theme.LIGHT))
        assertEquals(
            Environment.PRODUCTION,
            context.evaluateOrDefault(EnumFeatures.environment, Environment.DEVELOPMENT)
        )
    }

    @Test
    fun `enum features evaluate with rules`() {
        val webContext = Context(
            locale = AppLocale.UNITED_STATES,
            platform = Platform.WEB,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("12345678901234567890123456789012")
        )

        // Should return DEBUG for WEB platform based on rule
        assertEquals(LogLevel.DEBUG, webContext.evaluate(EnumFeatures.logLevel))
    }

    @Test
    fun `enum evidence is properly created`() {
        // EncodableEvidence should recognize enum types
        val logLevelEvidence = EncodableEvidence.get<LogLevel>()
        assertEquals(EncodableValue.Encoding.ENUM, logLevelEvidence.encoding)

        val themeEvidence = EncodableEvidence.get<Theme>()
        assertEquals(EncodableValue.Encoding.ENUM, themeEvidence.encoding)
    }

    @Test
    fun `enum evidence can check if type is encodable`() {
        assertTrue(EncodableEvidence.isEncodable<LogLevel>())
        assertTrue(EncodableEvidence.isEncodable<Theme>())
        assertTrue(EncodableEvidence.isEncodable<Environment>())
    }

    @Test
    fun `enum encodeable can be created from enum value`() {
        val logLevel = LogLevel.INFO
        val encodeable = EncodableValue.EnumEncodeable.of(logLevel)

        assertEquals(LogLevel.INFO, encodeable.value)
        assertEquals(LogLevel::class, encodeable.enumClass)
        assertEquals("INFO", encodeable.toEncodedString())
    }

    @Test
    fun `enum encodeable can decode from string`() {
        val encodeable = EncodableValue.EnumEncodeable.fromString("DEBUG", LogLevel::class)

        assertEquals(LogLevel.DEBUG, encodeable.value)
        assertEquals(LogLevel::class, encodeable.enumClass)
    }

    @Test
    fun `enum encodeable encoding type is ENUM`() {
        val encodeable = EncodableValue.EnumEncodeable.of(LogLevel.WARN)
        assertEquals(EncodableValue.Encoding.ENUM, encodeable.encoding)
    }

    @Test
    fun `multiple enum types can coexist in feature container`() {
        // Create a container with multiple enum types
        val mixedFeatures = object : FeatureContainer<Namespace.Messaging>(Namespace.Messaging) {
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
        val mixedTypeFeatures = object : FeatureContainer<Namespace.Authentication>(Namespace.Authentication) {
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
        val logLevelFeature: EnumFeature<LogLevel, Context, Namespace.Payments> =
            EnumFeatures.logLevel

        val themeFeature: EnumFeature<Theme, Context, Namespace.Payments> =
            EnumFeatures.theme

        // Verify types are preserved
        assertEquals("logLevel", logLevelFeature.key)
        assertEquals("theme", themeFeature.key)
    }

    @Test
    fun `enum features can have complex rule configurations`() {
        val complexFeatures = object : FeatureContainer<Namespace.Payments>(Namespace.Payments) {
            val environment by enum<Environment, Context>(default = Environment.PRODUCTION) {
                rule {
                    platforms(Platform.WEB)
                    locales(AppLocale.UNITED_STATES)
                } returns Environment.DEVELOPMENT

                rule {
                    platforms(Platform.IOS)
                } returns Environment.STAGING
            }
        }

        val webUSContext = Context(
            locale = AppLocale.UNITED_STATES,
            platform = Platform.WEB,
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

        assertEquals(Environment.DEVELOPMENT, webUSContext.evaluate(complexFeatures.environment))
        assertEquals(Environment.STAGING, iosContext.evaluate(complexFeatures.environment))
        assertEquals(
            Environment.PRODUCTION,
            androidContext.evaluateOrDefault(complexFeatures.environment, Environment.DEVELOPMENT)
        )
    }

    enum class SingleValue {
        ONLY_VALUE
    }

    @Test
    fun `enum with single value works correctly`() {

        val singleEnumFeature = object : FeatureContainer<Namespace.Payments>(Namespace.Payments) {
            val single by enum<SingleValue, Context>(default = SingleValue.ONLY_VALUE)
        }

        val context = Context(
            locale = AppLocale.UNITED_STATES,
            platform = Platform.WEB,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("12345678901234567890123456789012")
        )

        assertEquals(SingleValue.ONLY_VALUE, context.evaluate(singleEnumFeature.single))
    }
}
