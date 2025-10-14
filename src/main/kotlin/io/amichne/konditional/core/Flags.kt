package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import java.util.concurrent.atomic.AtomicReference

object Flags {
    private val snapshot = AtomicReference(Registry(emptyMap()))

    data class Registry(val flags: Map<FeatureFlag<*>, Flag<*>>)

    fun load(config: Registry) {
        snapshot.set(config)
    }

    fun <T : Flaggable<T>> update(
        flag: Flag<T>
    ) {
        snapshot.get().flags.toMutableMap().let {
            it[flag.key] = flag
            snapshot.set(Registry(it))
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Flaggable<T>> Context.evaluate(key: FeatureFlag<T>): T? =
        (snapshot.get().flags[key] as? Flag<T>)?.evaluate(this)

    fun Context.evaluate(): Map<FeatureFlag<*>, Any?> =
        snapshot.get().flags.mapValues { (_, f) -> f.evaluate(this) }
}
