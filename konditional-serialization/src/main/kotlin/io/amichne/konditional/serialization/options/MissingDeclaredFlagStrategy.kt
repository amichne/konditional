package io.amichne.konditional.serialization.options

/**
 * Policy for declared flags that are absent from an incoming data-plane payload.
 */
sealed interface MissingDeclaredFlagStrategy {
    /**
     * Reject payloads that do not explicitly specify all compile-time declared flags.
     */
    data object Reject : MissingDeclaredFlagStrategy

    /**
     * Materialize absent declared flags from compile-time defaults.
     */
    data object FillFromDeclaredDefaults : MissingDeclaredFlagStrategy
}
