package io.amichne.konditional.core.instance

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Conditional
import io.amichne.konditional.core.FeatureFlag

/**
 * Represents an incremental update to a [Konfig].
 *
 * A patch contains:
 * - SingletonFlagRegistry to add or update
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
    val flags: Map<Conditional<*, *>, FeatureFlag<*, *>>,
    val removeKeys: Set<Conditional<*, *>> = emptySet(),
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
         * Creates a new [KonfigPatch] from a current snapshot by building new flag definitions.
         *
         * Example:
         * ```
         * val patch = KonfigPatch.from(currentSnapshot) {
         *     add(MY_FLAG to myFlagDefinition)
         *     remove(OLD_FLAG)
         * }
         * ```
         *
         * @param current The current snapshot to base the patch on
         * @param builder A builder function to configure the patch
         * @return A new KonfigPatch
         */
        fun from(current: Konfig, builder: PatchBuilder.() -> Unit): KonfigPatch =
            PatchBuilder().apply(builder).build()

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
        private val flags = mutableMapOf<Conditional<*, *>, FeatureFlag<*, *>>()
        private val removeKeys = mutableSetOf<Conditional<*, *>>()

        /**
         * Adds or updates a flag in the patch.
         *
         * @param entry Pair of Conditional key and its flag definition
         */
        fun <S : Any, C : Context> add(
            entry: FeatureFlag<S, C>
        ) {
            flags[entry.conditional] = entry
            // If we're adding a flag, ensure it's not also in removeKeys
            removeKeys.remove(entry.conditional)
        }

        /**
         * Marks a flag for removal in the patch.
         *
         * @param key The conditional key to remove
         */
        fun remove(key: Conditional<*, *>) {
            removeKeys.add(key)
            // If we're removing a flag, ensure it's not also in flags
            flags.remove(key)
        }

        internal fun build(): KonfigPatch = KonfigPatch(flags.toMap(), removeKeys.toSet())
    }
}
