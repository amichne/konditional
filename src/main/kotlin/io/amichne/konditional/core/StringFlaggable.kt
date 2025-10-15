package io.amichne.konditional.core

/**
 * String implementation of Flaggable for string-valued feature flags.
 * Useful for configuration values, themes, API endpoints, etc.
 */
data class StringFlaggable(override val value: String) : Flaggable<String> {
    override fun parse(value: String): String = value

    companion object {
        fun of(value: String): StringFlaggable = StringFlaggable(value)
    }
}
