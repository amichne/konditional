package io.amichne.konditional.core.dsl

/**
 * Marker annotation for Konditional DSL receivers.
 *
 * Prevents accidental mixing of nested DSL scopes. On an inline condition it hides the enclosing
 * flag scope, so a condition can read facts but cannot declare rules.
 */
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class KonditionalDsl
