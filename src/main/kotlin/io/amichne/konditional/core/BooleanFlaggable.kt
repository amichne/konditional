package io.amichne.konditional.core

/**
 * Boolean implementation of Flaggable for traditional boolean feature flags.
 */
data class BooleanFlaggable(val value: Boolean) : Flaggable<BooleanFlaggable> {
    override fun parse(value: String): BooleanFlaggable = BooleanFlaggable(value.toBoolean())

    companion object {
        val TRUE = BooleanFlaggable(true)
        val FALSE = BooleanFlaggable(false)

        fun of(value: Boolean): BooleanFlaggable = if (value) TRUE else FALSE
    }
}
