package io.amichne.konditional.core.dsl

/**
 * Marker annotation for Konditional DSL receivers.
 *
 * Prevents accidental mixing axes nested DSL scopes in flag and rule builders.
 */
@DslMarker
annotation class KonditionalDsl
