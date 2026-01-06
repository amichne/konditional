package io.amichne.konditional.internal

/**
 * Marks a symbol as an internal implementation contract between Konditional modules.
 *
 * This is intentionally stricter than Kotlin's `internal` visibility:
 * - Kotlin `internal` prevents cross-module access entirely.
 * - This opt-in allows cross-module access while remaining explicitly non-public API.
 *
 * Consumers should not rely on these symbols for application logic; they may change without notice.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This is an internal Konditional API. Opt-in is required and the contract may change without notice.",
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.TYPEALIAS,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FIELD,
)
annotation class KonditionalInternalApi
