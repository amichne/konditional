package io.amichne.konditional.core

import io.amichne.konditional.core.context.Context
import java.util.concurrent.atomic.AtomicReference

object Flags {
    private val snapshot = AtomicReference(Registry(emptyMap()))

    data class Registry(val flags: Map<FeatureFlag<*>, Flag>)

    fun load(config: Registry) {
        snapshot.set(config)
    }

    fun update(
        flag: Flag
    ) {
        snapshot.get().flags.toMutableMap().let {
            it.set(flag.key, flag)
            snapshot.set(Registry(it))
        }
    }

    fun Context.evaluate(key: FeatureFlag<*>): Boolean = snapshot.get().flags[key]?.evaluate(this) ?: false

    fun Context.evaluate(): Map<FeatureFlag<*>, Boolean> =
        snapshot.get().flags.mapValues { (_, f) -> f.evaluate(this) }
}
