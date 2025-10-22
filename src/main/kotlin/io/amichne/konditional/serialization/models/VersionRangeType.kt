package io.amichne.konditional.serialization.models

/**
 * Enum discriminator for VersionRange types.
 */
enum class VersionRangeType {
    UNBOUNDED,
    LEFT_BOUND,
    RIGHT_BOUND,
    FULLY_BOUND,
}
