package io.amichne.konditional.context

/** Stable external identifier for a namespace-declared context fact. Rejects blank identifiers. */
@JvmInline
value class ContextKey(val id: String) {
    init {
        require(id.isNotBlank()) { "ContextKey.id must not be blank" }
    }

    override fun toString(): String = "ContextKey($id)"
}
