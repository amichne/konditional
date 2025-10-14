package io.amichne.konditional.core

/**
 * String implementation of Flaggable for string-valued feature flags.
 * Useful for configuration values, themes, API endpoints, etc.
 */
data class StringFlaggable(val value: String) : Flaggable<StringFlaggable> {
    override fun parse(value: String): StringFlaggable = StringFlaggable(value)

    companion object {
        fun of(value: String): StringFlaggable = StringFlaggable(value)
    }
}
