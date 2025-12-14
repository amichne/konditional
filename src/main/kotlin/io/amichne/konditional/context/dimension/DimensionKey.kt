package io.amichne.konditional.context.dimension

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * A value that lives along some dimension (env, region, tenant, etc.).
 * The engine treats `id` as an opaque, stable identifier.
 */
interface DimensionKey {
    val id: String

    companion object {
        @OptIn(ExperimentalUuidApi::class)
        internal operator fun <T> invoke(
            id: String = Uuid.random().toString(),
        ): DimensionKey where T : DimensionKey, T : Enum<T> = object : DimensionKey {
            override val id: String = id
        }
    }
}
