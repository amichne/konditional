package io.amichne.konditional.core

/**
 * Enum representing the supported value types for feature flags.
 *
 * Includes:
 * - Primitives: BOOLEAN, STRING, INT, LONG, DOUBLE
 * - Complex: JSON (for objects, data classes, etc.)
 */
enum class ValueType {
    BOOLEAN,
    STRING,
    INT,
    LONG,
    DOUBLE,
    JSON,  // For JSON objects and complex types
}
