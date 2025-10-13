package io.amichne.konditional.core

import io.amichne.konditional.core.context.Context
import java.util.concurrent.atomic.AtomicReference

object Flags {
    private val snapshot = AtomicReference(Registry(emptyMap()))

    data class Registry(val flags: Map<FeatureFlagPlaceholder, Flag>)

    fun load(config: Registry) {
        snapshot.set(config)
    }

    fun eval(
        key: FeatureFlagPlaceholder,
        ctx: Context
    ): Boolean =
        snapshot.get().flags[key]?.evaluate(ctx) ?: false

    fun evalAll(ctx: Context): Map<FeatureFlagPlaceholder, Boolean> =
        snapshot.get().flags.mapValues { (_, f) -> f.evaluate(ctx) }
}
