package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import java.util.concurrent.atomic.AtomicReference
import kotlin.collections.mapValues
import kotlin.collections.toMutableMap

object Flags {
    private val snapshot = AtomicReference(Registry(emptyMap()))

    data class Registry(val flags: Map<FeatureFlag<*, *>, Flag<*, *>>)

    fun load(config: Registry) {
        snapshot.set(config)
    }

    fun <T : Flaggable<S>, S : Any> update(
        flag: Flag<T, S>
    ) {
        snapshot.get().flags.toMutableMap().let {
            it[flag.key] = flag
            snapshot.set(Registry(it))
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Flaggable<S>, S : Any> Context.evaluate(key: FeatureFlag<T, S>): S =
        (snapshot.get().flags[key] as? Flag<T, S>)?.evaluate(this)!!.value

    fun Context.evaluate(): Map<FeatureFlag<*, *>, Any?> =
        snapshot.get().flags.mapValues { (_, f) -> f.evaluate(this) }
}
