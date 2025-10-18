package io.amichne.konditional.context

@JvmInline
value class RampUp private constructor(
    val value: Double,
) : Comparable<Number> {
    init {
        require(value in MIN_DOUBLE..MAX_DOUBLE) { "RampUp out of range, must be between 0.0 and 100.0, got $value" }
    }

    companion object {
        fun of(value: Double): RampUp = RampUp(value)

        fun of(value: Int): RampUp = RampUp(value.toDouble())

        fun of(value: String): RampUp = RampUp(value.toDouble())

        fun of(value: RampUp): RampUp = value

        private val MIN_DOUBLE = 0.0
        val MIN: RampUp = RampUp(MIN_DOUBLE)
        private val MAX_DOUBLE = 100.0
        val MAX: RampUp = RampUp(MAX_DOUBLE)
        val default: RampUp = MAX
    }

    override fun compareTo(other: Number): Int = value.compareTo(other.toDouble())
}
