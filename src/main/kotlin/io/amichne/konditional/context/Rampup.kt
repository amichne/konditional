package io.amichne.konditional.context

/**
 * Represents a rollout percentage (0-100) for gradual feature flag deployment.
 * This value class ensures type-safe rollout percentages across the feature flag system.
 *
 * @property value The rollout percentage (0.0 to 100.0)
 */
@JvmInline
value class Rampup private constructor(
    val value: Double,
) : Comparable<Number> {
    init {
        require(value in MIN_DOUBLE..MAX_DOUBLE) { "Rampup out of range, must be between 0.0 and 100.0, got $value" }
    }

    companion object {
        fun of(value: Double): Rampup = Rampup(value)

        fun of(value: Int): Rampup = Rampup(value.toDouble())

        fun of(value: String): Rampup = Rampup(value.toDouble())

        fun of(value: Rampup): Rampup = value

        private const val MIN_DOUBLE = 0.0
        private const val MAX_DOUBLE = 100.0
        val MAX: Rampup = Rampup(MAX_DOUBLE)
        val default: Rampup = MAX
    }

    override fun compareTo(other: Number): Int = value.compareTo(other.toDouble())
}

