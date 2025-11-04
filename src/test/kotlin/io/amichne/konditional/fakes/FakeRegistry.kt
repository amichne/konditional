package io.amichne.konditional.fakes

import io.amichne.konditional.core.FlagRegistry
import io.amichne.konditional.core.internal.SingletonFlagRegistry

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
 *     val patch = KonfigPatch.from(registry.konfig()) {
 *         add(NEW_FLAG to newDefinition)
 *         remove(OLD_FLAG)
 *     }
 *     registry.update(patch)
 *
 *     assertNotNull(registry.featureFlag(NEW_FLAG))
 *     assertNull(registry.featureFlag(OLD_FLAG))
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
 *     assertEquals(1, registry.konfig().flags.size)
 *     assertEquals(myFlagDefinition, registry.featureFlag(MY_FLAG))
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
 *   to the Konfig type itself
 *
 * @see FlagRegistry
 * @see io.amichne.konditional.core.internal.SingletonFlagRegistry
 */
class FakeRegistry : FlagRegistry by SingletonFlagRegistry
