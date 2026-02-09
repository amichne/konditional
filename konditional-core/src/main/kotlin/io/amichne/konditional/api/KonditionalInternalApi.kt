package io.amichne.konditional.api

import org.jetbrains.annotations.ApiStatus

/**
 * Marks a symbol as an internal implementation contract between Konditional modules.
 *
 * This is intentionally different than Kotlin's `internal` visibility:
 * - Kotlin `internal` prevents cross-module access entirely.
 * - This opt-in allows cross-module access while remaining explicitly non-public API.
 * - For prohibited package boundary enforcement, any top‑level entity without internal/private visibility is
 *      only permitted if it is annotated with [KonditionalInternalApi]
 *
 * Consumers should not rely on these symbols for application logic; they may change without notice.
 *
 * @see `/build-logic/src/main/kotlin/io/amichne/konditional/gradle/KonditionalCoreApiBoundaryTask.kt`
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This is an internal Konditional API. Opt-in is required and the contract may change without notice.",
)
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.TYPEALIAS,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FIELD,
)
@ApiStatus.Internal
annotation class KonditionalInternalApi
