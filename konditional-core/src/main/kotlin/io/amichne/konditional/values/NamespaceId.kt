package io.amichne.konditional.values

import io.amichne.konditional.values.Identifiable.Composable
import io.amichne.konditional.values.IdentifierEncoding.SEPARATOR

/**
 * Strongly-typed identifier for a [io.amichne.konditional.core.Namespace].
 *
 * Wraps the raw namespace id string and enforces structural invariants at construction time:
 * - Must not be blank.
 * - Must not contain the [SEPARATOR] sequence used in composite identifiers.
 */
@JvmInline
value class NamespaceId(override val value: String) : Composable {
    init {
        validate()
    }

    override fun toString(): String = value
}
