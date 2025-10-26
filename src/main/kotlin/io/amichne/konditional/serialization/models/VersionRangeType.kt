package io.amichne.konditional.serialization.models

/**
 * Enum discriminator for VersionRange types.
 */
enum class VersionRangeType {
    UNBOUNDED,
    MIN_BOUND,
    MAX_BOUND,
    MIN_AND_MAX_BOUND,
}
