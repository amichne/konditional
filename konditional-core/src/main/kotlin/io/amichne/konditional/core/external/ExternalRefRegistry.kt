package io.amichne.konditional.core.external

import io.amichne.konditional.core.result.ParseError

/**
 * Namespace-scoped registry for [ExternalSnapshotRef] registrations.
 *
 * All external snapshot refs must carry a non-blank [ExternalSnapshotRef.version] field.
 * Refs that do not satisfy this requirement are rejected at [register] time with
 * [ParseError.UnversionedExternalRef] — they never reach the evaluation path.
 *
 * ## Namespace isolation
 *
 * Each [ExternalRefRegistry] is scoped to a single namespace via [namespaceId].
 * External refs registered in namespace A are invisible to namespace B.
 */
interface ExternalRefRegistry {
    /** The namespace this registry is scoped to. */
    val namespaceId: String

    /**
     * Registers an external snapshot ref.
     *
     * Returns [Result.failure] with [ParseError.UnversionedExternalRef] if [ref.version] is blank,
     * or [ParseError.InvalidValue] if [ref.id] is blank.
     *
     * On success, the ref is stored and included in [registeredRefs].
     */
    fun register(ref: ExternalSnapshotRef): Result<Unit>

    /**
     * All registered refs in this registry, in insertion order.
     */
    val registeredRefs: List<ExternalSnapshotRef>
}
