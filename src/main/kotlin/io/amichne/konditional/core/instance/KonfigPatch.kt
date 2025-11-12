package io.amichne.konditional.core.instance

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Feature
import io.amichne.konditional.core.FlagDefinition

/**
 * Represents an incremental update to a [Konfig].
 *
 * A patch contains:
 * - SingletonModuleRegistry to add or update
 * - Keys of flags to remove
 *
 * Patches can be created from a current snapshot and applied to produce a new snapshot,
 * enabling efficient partial updates without replacing the entire configuration.
 *
 * @property flags Map of flags to add or update
 * @property removeKeys Set of flag keys to remove
 */
@ConsistentCopyVisibility
data class KonfigPatch internal constructor(
    val flags: Map<Feature<*, *, *, *>, FlagDefinition<*, *, *, *>>,
    val removeKeys: Set<Feature<*, *, *, *>> = emptySet(),
) {
    /**
     * Applies a patch to a konfig, creating a new konfig with the changes.
     *
     * @param konfig The konfig to apply the patch to
     * @return A new Konfig with the patch applied
     */
    fun applyTo(konfig: Konfig): Konfig = konfig.flags.toMutableMap().let { map ->
        removeKeys.forEach { map.remove(it) }
        Konfig(map.also { it.putAll(flags) })
    }

    companion object {

        /**
         * Creates an empty patch with no changes.
         */
        fun empty(): KonfigPatch = KonfigPatch(emptyMap(), emptySet())

        fun patch(
            builder: PatchBuilder.() -> Unit
        ): KonfigPatch = PatchBuilder().apply(builder).build()
    }

    /**
     * Builder for creating patches with a DSL-style API.
     */
    class PatchBuilder internal constructor() {
        private val flags = mutableMapOf<Feature<*, *, *, *>, FlagDefinition<*, *, *, *>>()
        private val removeKeys = mutableSetOf<Feature<*, *, *, *>>()

        /**
         * Adds or updates a flag in the patch.
         *
         * @param entry Pair of Feature key and its flag definition
         */
        fun <S : io.amichne.konditional.core.types.EncodableValue<T>, T : Any, C : Context> add(
            entry: FlagDefinition<S, T, C, *>
        ) {
            flags[entry.feature] = entry
            // If we're adding a flag, ensure it's not also in removeKeys
            removeKeys.remove(entry.feature)
        }

        /**
         * Marks a flag for removal in the patch.
         *
         * @param key The conditional key to remove
         */
        fun remove(key: Feature<*, *, *, *>) {
            removeKeys.add(key)
            // If we're removing a flag, ensure it's not also in flags
            flags.remove(key)
        }

        internal fun build(): KonfigPatch = KonfigPatch(flags.toMap(), removeKeys.toSet())
    }
}
