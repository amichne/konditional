@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.serialization.instance

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.internal.KonditionalInternalApi

/**
 * Represents an incremental update to a [Configuration].
 */
data class ConfigurationPatch(
    val flags: Map<Feature<*, *, *>, FlagDefinition<*, *, *>>,
    val removeKeys: Set<Feature<*, *, *>> = emptySet(),
) {
    fun applyTo(configuration: Configuration): Configuration =
        configuration.flags.toMutableMap().let { map ->
            removeKeys.forEach { map.remove(it) }
            Configuration(map.also { it.putAll(flags) }, configuration.metadata)
        }

    companion object {
        fun empty(): ConfigurationPatch = ConfigurationPatch(emptyMap(), emptySet())

        fun patch(
            builder: PatchBuilder.() -> Unit,
        ): ConfigurationPatch = PatchBuilder().apply(builder).build()
    }

    class PatchBuilder {
        private val flags = mutableMapOf<Feature<*, *, *>, FlagDefinition<*, *, *>>()
        private val removeKeys = mutableSetOf<Feature<*, *, *>>()

        fun <T : Any, C : Context> add(
            entry: FlagDefinition<T, C, *>,
        ) {
            flags[entry.feature] = entry
            removeKeys.remove(entry.feature)
        }

        fun remove(key: Feature<*, *, *>) {
            removeKeys.add(key)
            flags.remove(key)
        }

        internal fun build(): ConfigurationPatch = ConfigurationPatch(flags.toMap(), removeKeys.toSet())
    }
}
