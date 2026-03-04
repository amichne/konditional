package io.amichne.konditional.values

import io.amichne.konditional.values.Identifiable.NonBlank

@JvmInline
value class PredicateId(override val value: String) : NonBlank {
    init { validate() }
}
