package io.amichne.konditional.fixtures.serializers

import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.core.types.KotlinEncodeable
import io.amichne.konditional.serialization.TypeSerializer
import io.amichne.konditional.serialization.asBoolean
import io.amichne.konditional.serialization.asDouble
import io.amichne.konditional.serialization.asInt
import io.amichne.konditional.serialization.asString
import io.amichne.konditional.serialization.jsonObject
import io.amichne.kontracts.dsl.of
import io.amichne.kontracts.dsl.schemaRoot
import io.amichne.kontracts.schema.ObjectSchema
import io.amichne.kontracts.value.JsonObject
import io.amichne.kontracts.value.JsonValue

/**
 * Common test data class for KotlinEncodeable tests.
 * Used across multiple test files.
 */
data class RetryPolicy(
    val maxAttempts: Int = 3,
    val backoffMs: Double = 1000.0,
    val enabled: Boolean = true,
    val mode: String = "exponential",
) : KotlinEncodeable<ObjectSchema> {
    override val schema: ObjectSchema =
        schemaRoot {
            ::maxAttempts of { minimum = 1 }
            ::backoffMs of { minimum = 0.0 }
            ::enabled of { default = true }
            ::mode of { minLength = 1 }
        }

    companion object {
        val serializer = object : TypeSerializer<RetryPolicy> {
            override fun encode(value: RetryPolicy): JsonValue =
                jsonObject {
                    "maxAttempts" to value.maxAttempts
                    "backoffMs" to value.backoffMs
                    "enabled" to value.enabled
                    "mode" to value.mode
                }

            override fun decode(json: JsonValue): ParseResult<RetryPolicy> =
                when (json) {
                    is JsonObject -> {
                        val maxAttempts = json.fields["maxAttempts"]?.asInt()
                        val backoffMs = json.fields["backoffMs"]?.asDouble()
                        val enabled = json.fields["enabled"]?.asBoolean() ?: true
                        val mode = json.fields["mode"]?.asString() ?: "exponential"

                        if (maxAttempts != null && backoffMs != null) {
                            ParseResult.Success(RetryPolicy(maxAttempts, backoffMs, enabled, mode))
                        } else {
                            ParseResult.Failure(
                                ParseError.InvalidSnapshot("Missing required fields for RetryPolicy")
                            )
                        }
                    }
                    else -> ParseResult.Failure(
                        ParseError.InvalidSnapshot("Expected JsonObject for RetryPolicy, got ${json::class.simpleName}")
                    )
                }
        }
    }
}

/**
 * Test data class for user settings.
 */
data class UserSettings(
    val theme: String = "light",
    val notificationsEnabled: Boolean = true,
    val maxRetries: Int = 3,
    val timeout: Double = 30.0,
) : KotlinEncodeable<ObjectSchema> {
    override val schema = schemaRoot {
        ::theme of {
            minLength = 1
            maxLength = 50
            description = "UI theme preference"
            enum = listOf("light", "dark", "auto")
        }
        ::notificationsEnabled of {
            description = "Enable push notifications"
            default = true
        }
        ::maxRetries of {
            minimum = 0
            maximum = 10
            description = "Maximum retry attempts"
        }
        ::timeout of {
            minimum = 0.0
            maximum = 300.0
            format = "double"
            description = "Request timeout in seconds"
        }
    }

    companion object {
        val serializer = object : TypeSerializer<UserSettings> {
            override fun encode(value: UserSettings): JsonValue =
                jsonObject {
                    "theme" to value.theme
                    "notificationsEnabled" to value.notificationsEnabled
                    "maxRetries" to value.maxRetries
                    "timeout" to value.timeout
                }

            override fun decode(json: JsonValue): ParseResult<UserSettings> =
                when (json) {
                    is JsonObject -> {
                        val theme = json.fields["theme"]?.asString()
                        val notificationsEnabled = json.fields["notificationsEnabled"]?.asBoolean()
                        val maxRetries = json.fields["maxRetries"]?.asInt()
                        val timeout = json.fields["timeout"]?.asDouble()

                        if (theme != null && notificationsEnabled != null && maxRetries != null && timeout != null) {
                            ParseResult.Success(
                                UserSettings(theme, notificationsEnabled, maxRetries, timeout)
                            )
                        } else {
                            ParseResult.Failure(
                                ParseError.InvalidSnapshot("Missing required fields for UserSettings")
                            )
                        }
                    }
                    else -> ParseResult.Failure(
                        ParseError.InvalidSnapshot("Expected JsonObject for UserSettings, got ${json::class.simpleName}")
                    )
                }
        }
    }
}
