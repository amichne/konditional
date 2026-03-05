package io.amichne.konditional.values

import io.amichne.konditional.core.features.Identifiable.Named.NonBlank

@JvmInline
value class PredicateId(override val value: String) : NonBlank {
    init { validate() }

    override fun toString(): String = value
}
