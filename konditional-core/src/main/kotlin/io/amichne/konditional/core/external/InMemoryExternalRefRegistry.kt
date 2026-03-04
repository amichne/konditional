package io.amichne.konditional.core.external

import io.amichne.konditional.core.result.KonditionalBoundaryFailure
import io.amichne.konditional.core.result.ParseError

/**
 * Default [ExternalRefRegistry] backed by an insertion-ordered list.
 *
 * ## Validation
 *
 * At [register] time, both [ExternalSnapshotRef.id] and [ExternalSnapshotRef.version]
 * are validated to be non-blank. Failures produce typed [ParseError] values — no exceptions.
 *
 * ## Thread safety
 *
 * Registration must complete before the registry is shared across threads.
 * [registeredRefs] (read) is safe to call concurrently after setup.
 *
 * @param namespaceId The namespace this registry is scoped to.
 */
class InMemoryExternalRefRegistry(
    override val namespaceId: String,
) : ExternalRefRegistry {

    init {
        require(namespaceId.isNotBlank()) { "ExternalRefRegistry.namespaceId must not be blank" }
    }

    private val store: MutableList<ExternalSnapshotRef> = mutableListOf()

    @Suppress("ReturnCount") // for early returns on validation failure
    override fun register(ref: ExternalSnapshotRef): Result<Unit> {
        if (ref.id.isBlank()) {
            return Result.failure(
                KonditionalBoundaryFailure(
                    ParseError.UnversionedExternalRef(
                        id = "(blank)",
                        reason = "id must not be blank",
                    ),
                ),
            )
        }

        if (ref.version.isBlank()) {
            return Result.failure(
                KonditionalBoundaryFailure(
                    ParseError.UnversionedExternalRef(
                        id = ref.id,
                        reason = "version must not be blank",
                    ),
                ),
            )
        }

        store.add(ref)
        return Result.success(Unit)
    }

    override val registeredRefs: List<ExternalSnapshotRef>
        get() = store.toList()
}
