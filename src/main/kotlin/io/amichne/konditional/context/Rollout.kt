package io.amichne.konditional.context

/**
 * Represents a rollout percentage (0-100) for gradual feature flag deployment.
 * This value class ensures type-safe rollout percentages across the feature flag system.
 *
 * @property value The rollout percentage (0.0 to 100.0)
 */
@JvmInline
value class Rollout private constructor(
    val value: Double,
) : Comparable<Number> {
    init {
        require(value in MIN_DOUBLE..MAX_DOUBLE) { "Rollout out of range, must be between 0.0 and 100.0, got $value" }
    }

    companion object {
        fun of(value: Double): Rollout = Rollout(value)

        fun of(value: Int): Rollout = Rollout(value.toDouble())

        fun of(value: String): Rollout = Rollout(value.toDouble())

        fun of(value: Rollout): Rollout = value

        private val MIN_DOUBLE = 0.0
        val MIN: Rollout = Rollout(MIN_DOUBLE)
        private val MAX_DOUBLE = 100.0
        val MAX: Rollout = Rollout(MAX_DOUBLE)
        val default: Rollout = MAX
    }

    override fun compareTo(other: Number): Int = value.compareTo(other.toDouble())
}
