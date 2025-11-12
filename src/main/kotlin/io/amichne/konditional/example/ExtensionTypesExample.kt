package io.amichne.konditional.example

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.Feature
import io.amichne.konditional.core.FeatureModule
import io.amichne.konditional.core.config
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.types.EncodableValue
import io.amichne.konditional.core.types.asCustomDouble
import io.amichne.konditional.core.types.asCustomString
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Example demonstrating extension type support: "0-depth primitive-like values"
 * These are wrapper types that encode to JSON primitives (DateTime, UUID, etc.)
 */
object ExtensionTypesExample {
    val module = FeatureModule.Team.Search

    // ========== Custom Wrapper Type: DateTime ==========

    /**
     * DateTime wrapper that encodes to ISO-8601 String.
     * This is a "0-depth primitive-like value" - wraps a JSON primitive.
     */
    data class DateTime(val instant: Instant) {
        fun toIso8601(): String = DateTimeFormatter.ISO_INSTANT.format(instant)

        companion object {
            fun parse(iso8601: String): DateTime = DateTime(Instant.parse(iso8601))
            fun now(): DateTime = DateTime(Instant.now())
        }
    }

    /**
     * Conditional for DateTime values.
     * Uses CustomEncodeable with String encoding.
     */
    val CREATED_AT: Feature.OfCustom<DateTime, String, Context, FeatureModule.Team.Search> =
        Feature.custom("created_at", module)

    /**
     * Helper to create EncodableValue from DateTime.
     */
    fun DateTime.toEncodable(): EncodableValue.CustomEncodeable<DateTime, String> =
        asCustomString().encoder { it.toIso8601() }.decoder { DateTime.parse(it) }

    // ========== Custom Wrapper Type: UUID ==========

    data class UUID(val value: String) {
        init {
            require(value.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))) {
                "Invalid UUID format"
            }
        }

        companion object {
            fun random(): UUID = UUID(java.util.UUID.randomUUID().toString())
        }
    }

    fun UUID.toEncodable(): EncodableValue.CustomEncodeable<UUID, String> =
        asCustomString().encoder { it.value }.decoder { UUID(it) }

    val REQUEST_ID: Feature.OfCustom<UUID, String, Context, FeatureModule.Team.Search> =
        Feature.custom("request_id", module)

    // ========== Custom Wrapper Type: Duration (milliseconds) ==========

    data class Duration(val millis: Long) {
        fun toSeconds(): Long = millis / 1000

        companion object {
            fun ofSeconds(seconds: Long): Duration = Duration(seconds * 1000)
            fun ofMinutes(minutes: Long): Duration = Duration(minutes * 60 * 1000)
        }
    }

    val TIMEOUT: Feature.OfCustom<Duration, Double, Context, FeatureModule.Team.Messaging> =
        Feature.custom("timeout", FeatureModule.Team.Messaging)

    fun Duration.toEncodable(): EncodableValue.CustomEncodeable<Duration, Double> =
        asCustomDouble().encoder { it.millis.toDouble() }.decoder { Duration(it.toLong()) }

    // ========== Usage Example ==========

    fun demonstrateUsage() {
        // Configure flags with extension types
        FeatureModule.Core.config {
            CREATED_AT with {
                default(DateTime.now().toEncodable().value)

                rule {
                    platforms(Platform.IOS)
                } implies DateTime.parse("2025-01-01T00:00:00Z").toEncodable().value
            }

            REQUEST_ID with {
                default(UUID.random().toEncodable().value)
            }

            TIMEOUT with {
                default(Duration.ofSeconds(30).toEncodable().value)

                rule {
                    platforms(Platform.WEB)
                } implies Duration.ofMinutes(2).toEncodable().value
            }
        }

        // Evaluate
        Context(
            locale = AppLocale.EN_US,
            platform = Platform.IOS,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("11111111111111111111111111111111")
        )

        // These would return the raw values (DateTime, UUID, Duration)
        // In practice, you'd need to wrap evaluation results with the appropriate encoders
        println("Extension types example configured successfully")
    }
}
