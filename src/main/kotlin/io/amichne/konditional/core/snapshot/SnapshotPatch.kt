package io.amichne.konditional.core.snapshot

import io.amichne.konditional.core.Conditional
import io.amichne.konditional.core.ContextualFeatureFlag

/**
 * Represents an incremental update to a [Snapshot].
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
data class SnapshotPatch internal constructor(
    val flags: Map<Conditional<*, *>, ContextualFeatureFlag<*, *>>,
    val removeKeys: Set<Conditional<*, *>> = emptySet(),
) {
    /**
     * Applies a patch to a snapshot, creating a new snapshot with the changes.
     *
     * @param snapshot The snapshot to apply the patch to
     * @return A new Snapshot with the patch applied
     */
    fun applyTo(snapshot: Snapshot): Snapshot = snapshot.flags.toMutableMap().let { map ->
        removeKeys.forEach { map.remove(it) }
        Snapshot(map.also { it.putAll(flags) })
    }

    companion object {
        /**
         * Creates a new [SnapshotPatch] from a current snapshot by building new flag definitions.
         *
         * Example:
         * ```
         * val patch = SnapshotPatch.from(currentSnapshot) {
         *     add(MY_FLAG to myFlagDefinition)
         *     remove(OLD_FLAG)
         * }
         * ```
         *
         * @param current The current snapshot to base the patch on
         * @param builder A builder function to configure the patch
         * @return A new SnapshotPatch
         */
        fun from(current: Snapshot, builder: PatchBuilder.() -> Unit): SnapshotPatch =
            PatchBuilder().apply(builder).build()

        /**
         * Creates an empty patch with no changes.
         */
        fun empty(): SnapshotPatch = SnapshotPatch(emptyMap(), emptySet())
    }

    /**
     * Builder for creating patches with a DSL-style API.
     */
    class PatchBuilder internal constructor() {
        private val flags = mutableMapOf<Conditional<*, *>, ContextualFeatureFlag<*, *>>()
        private val removeKeys = mutableSetOf<Conditional<*, *>>()

        /**
         * Adds or updates a flag in the patch.
         *
         * @param entry Pair of Conditional key and its flag definition
         */
        fun <S : Any, C : io.amichne.konditional.context.Context> add(
            entry: Pair<Conditional<S, C>, ContextualFeatureFlag<S, C>>
        ) {
            flags[entry.first] = entry.second
            // If we're adding a flag, ensure it's not also in removeKeys
            removeKeys.remove(entry.first)
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

        internal fun build(): SnapshotPatch = SnapshotPatch(flags.toMap(), removeKeys.toSet())
    }
}
