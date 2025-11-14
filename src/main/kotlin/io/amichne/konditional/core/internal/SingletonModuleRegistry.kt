package io.amichne.konditional.core.internal

import io.amichne.konditional.core.InMemoryModuleRegistry
import io.amichne.konditional.core.ModuleRegistry

/**
 * Default singleton implementation of [ModuleRegistry] for the Konditional core taxonomy.
 *
 * This object provides a thread-safe, in-memory registry for managing feature flags.
 * It uses the same implementation as [InMemoryModuleRegistry], but as a singleton.
 *
 * ## Thread Safety
 *
 * All operations on this registry are atomic and thread-safe. Multiple threads can
 * safely read and update flags concurrently.
 *
 * @see ModuleRegistry
 * @see InMemoryModuleRegistry
 */
internal object SingletonModuleRegistry : ModuleRegistry by InMemoryModuleRegistry()
