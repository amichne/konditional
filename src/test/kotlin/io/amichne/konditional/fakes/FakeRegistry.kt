package io.amichne.konditional.fakes

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.FlagRegistry
import io.amichne.konditional.core.snapshot.Snapshot
import io.amichne.konditional.core.snapshot.SnapshotPatch

/**
 * A test fake implementation of [FlagRegistry] for unit testing.
 *
 * This implementation provides a simple, mutable in-memory registry
 * that's ideal for testing flag evaluation logic without the overhead
 * of thread-safety mechanisms used in production implementations.
 *
 * ## Usage in Tests
 *
 * ### Basic Setup
 * ```kotlin
 * @Test
 * fun `test flag evaluation`() {
 *     val registry = FakeRegistry()
 *     val snapshot = buildSnapshot {
 *         MY_FLAG with { default(true) }
 *     }
 *     registry.load(snapshot)
 *
 *     val value = context.evaluate(MY_FLAG, registry)
 *     assertTrue(value)
 * }
 * ```
 *
 * ### Testing Patches
 * ```kotlin
 * @Test
 * fun `test incremental updates`() {
 *     val registry = FakeRegistry()
 *     registry.load(initialSnapshot)
 *
 *     val patch = SnapshotPatch.from(registry.getCurrentSnapshot()) {
 *         add(NEW_FLAG to newDefinition)
 *         remove(OLD_FLAG)
 *     }
 *     registry.applyPatch(patch)
 *
 *     assertNotNull(registry.getFlag(NEW_FLAG))
 *     assertNull(registry.getFlag(OLD_FLAG))
 * }
 * ```
 *
 * ### State Inspection
 * ```kotlin
 * @Test
 * fun `verify flag state`() {
 *     val registry = FakeRegistry()
 *     registry.update(myFlagDefinition)
 *
 *     assertEquals(1, registry.getCurrentSnapshot().flags.size)
 *     assertEquals(myFlagDefinition, registry.getFlag(MY_FLAG))
 * }
 * ```
 *
 * ## Design Rationale
 *
 * This fake uses a simple mutable variable instead of thread-safe constructs
 * because:
 * - Tests are single-threaded by default
 * - Simpler implementation makes test failures easier to diagnose
 * - No performance overhead from synchronization primitives
 * - Follows the "parse, don't validate" principle by delegating validation
 *   to the Snapshot type itself
 *
 * @see FlagRegistry
 * @see io.amichne.konditional.core.SingletonFlagRegistry
 */
class FakeRegistry : FlagRegistry {
    private var currentSnapshot: Snapshot = Snapshot(emptyMap())

    /**
     * Loads a complete flag configuration from the provided snapshot.
     *
     * Replaces the entire current configuration atomically.
     *
     * @param config The [Snapshot] containing the flag configuration to load
     */
    override fun load(config: Snapshot) {
        currentSnapshot = config
    }

    /**
     * Applies an incremental patch to the current configuration.
     *
     * Updates only the flags specified in the patch, leaving others unchanged.
     *
     * @param patch The [SnapshotPatch] to apply
     */
    override fun applyPatch(patch: SnapshotPatch) {
        currentSnapshot = patch.applyTo(currentSnapshot)
    }

    /**
     * Updates a single flag definition in the current configuration.
     *
     * @param definition The [FlagDefinition] to update
     * @param S The type of the flag's value
     * @param C The type of the context used for evaluation
     */
    override fun <S : Any, C : Context> update(definition: FlagDefinition<S, C>) {
        val mutableFlags = currentSnapshot.flags.toMutableMap()
        mutableFlags[definition.conditional] = definition
        currentSnapshot = Snapshot(mutableFlags)
    }

    /**
     * Retrieves the current snapshot of all flag configurations.
     *
     * @return The current [Snapshot]
     */
    override fun getCurrentSnapshot(): Snapshot = currentSnapshot
}
