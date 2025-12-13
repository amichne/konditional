package io.amichne.konditional.context

/**
 * A value that lives along some dimension (env, region, tenant, etc.).
 * The engine treats `id` as an opaque, stable identifier.
 */
interface DimensionKey {
    val id: String
}
