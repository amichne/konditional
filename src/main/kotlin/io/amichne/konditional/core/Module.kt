package io.amichne.konditional.core

import io.amichne.konditional.context.Context

/**
 * Represents a module containing a fixed set of feature flags.
 *
 * Modules are the primary abstraction for organizing and registering feature flags.
 * Instead of registering individual flags directly to the registry, flags must be
 * organized into modules.
 *
 * This interface is designed to be implemented by enums, where the enum constant name
 * becomes the module name. This provides a type-safe way to define and manage modules.
 *
 * Example usage:
 * ```kotlin
 * enum class MyModules : Module<Context> {
 *     USER_FEATURES {
 *         override fun flags(): Set<Conditional<*, *, Context>> = setOf(
 *             UserFlags.ENABLE_PROFILE,
 *             UserFlags.ENABLE_NOTIFICATIONS
 *         )
 *     },
 *     PAYMENT_FEATURES {
 *         override fun flags(): Set<Conditional<*, *, Context>> = setOf(
 *             PaymentFlags.ENABLE_CRYPTO,
 *             PaymentFlags.ENABLE_SUBSCRIPTIONS
 *         )
 *     }
 * }
 * ```
 *
 * @param C The type of context used for flag evaluation
 */
interface Module<C : Context> {
    /**
     * The name of this module.
     *
     * When implemented by an enum, this should return the enum constant name.
     */
    val moduleName: String

    /**
     * Returns the fixed set of feature flags that belong to this module.
     *
     * This set should contain only primitive flags and should not change
     * during the lifetime of the module.
     *
     * @return The set of [Conditional] flags for this module
     */
    fun flags(): Set<Conditional<*, *, C>>
}

/**
 * Extension property for enums implementing Module to automatically derive
 * the module name from the enum constant name.
 */
val <E> E.moduleNameFromEnum: String
    get() where E : Enum<E>, E : Module<*> = this.name
