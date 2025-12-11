package io.amichne.konditional.core

import io.amichne.konditional.core.types.JsonSchemaClass
import io.amichne.kontracts.dsl.of
import io.amichne.kontracts.dsl.schemaRoot

/**
 * Comprehensive integration tests for JsonSchemaClass support.
 *
 * These tests validate:
 * - Data class to JsonValue conversion
 * - JsonValue to data class parsing
 * - Schema generation and validation
 * - Serialization with Moshi
 * - Feature flag integration
 */
class DataClassWithSchemaIntegrationTest {

    // Test data class with manually defined schema
    data class UserSettings(
        val theme: String = "light",
        val notificationsEnabled: Boolean = true,
        val maxRetries: Int = 3,
        val timeout: Double = 30.0,
    ) : JsonSchemaClass {
        override val schema = schemaRoot {
            // Type-inferred DSL: no need to call string(), the property type determines the schema
            ::theme of {
                minLength = 1
                maxLength = 50
                description = "UI theme preference"
                enum = listOf("light", "dark", "auto")
            }

            // Boolean schema with automatic type inference
            ::notificationsEnabled of {
                description = "Enable push notifications"
                default = true
            }

            // Int schema with constraints
            ::maxRetries of {
                minimum = 0
                maximum = 10
                description = "Maximum retry attempts"
            }

            // Double schema with format
            ::timeout of {
                minimum = 0.0
                maximum = 300.0
                format = "double"
                description = "Request timeout in seconds"
            }


        }
    }
}
