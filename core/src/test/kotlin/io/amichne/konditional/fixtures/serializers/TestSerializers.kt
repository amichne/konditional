package io.amichne.konditional.fixtures.serializers

import io.amichne.konditional.core.types.Konstrained
import io.amichne.kontracts.dsl.of
import io.amichne.kontracts.dsl.schemaRoot
import io.amichne.kontracts.schema.ObjectSchema

/**
 * Common test data class for Konstrained tests.
 * Used across multiple test files.
 */
data class RetryPolicy(
    val maxAttempts: Int = 3,
    val backoffMs: Double = 1000.0,
    val enabled: Boolean = true,
    val mode: String = "exponential",
) : Konstrained<ObjectSchema> {
    override val schema: ObjectSchema =
        schemaRoot {
            ::maxAttempts of { minimum = 1 }
            ::backoffMs of { minimum = 0.0 }
            ::enabled of { default = true }
            ::mode of { minLength = 1 }
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
) : Konstrained<ObjectSchema> {
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
}
