package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.registry.InMemoryNamespaceRegistry
import io.amichne.konditional.core.registry.NamespaceRegistry
import io.amichne.konditional.values.IdentifierEncoding.SEPARATOR
import org.jetbrains.annotations.TestOnly
import java.util.UUID

/**
 * Represents a feature flag namespace with isolated configuration and runtime isolation.
 *
 * Namespaces provide:
 * - **Compile-time isolation**: Features are type-bound to their namespace
 * - **Runtime isolation**: Each namespace has its own flag registry
 * - **Type safety**: Namespace identity is encoded in the type system
 * - **Direct registry operations**: Namespaces implement [NamespaceRegistry], eliminating the need for `.registry` access
 *
 * ## Namespace Types
 *
 * ### Global Namespace
 * The [Global] namespace is the only namespace shipped by Konditional. It is intended for shared flags that are
 * broadly accessible across the application:
 * ```kotlin
 * object CoreFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
 *     val KILL_SWITCH by boolean<Context>(default = false)
 * }
 * ```
 *
 * ### Consumer-defined namespaces
 * If you need isolation boundaries beyond [Global], define namespaces in your own codebase.
 * A namespace is just a [Namespace] instance, typically modeled as an `object`:
 * ```kotlin
 * object Payments : Namespace("payments")
 * ```
 *
 * ## Adding New Modules
 *
 * Define a namespace in your module, and have it own its [io.amichne.konditional.core.features.FeatureContainer]:
 *
 * ```kotlin
 * object Payments : Namespace("payments") {
 *     object Features : FeatureContainer<Payments>(this) {
 *         val APPLE_PAY by boolean<Context>(default = false)
 *     }
 *
 *     init {
 *         // Ensure the container initializes at t0
 *         Features
 *     }
 * }
 * ```
 *
 * ## Direct Registry Operations
 *
 * [Namespace] implements [NamespaceRegistry] via delegation, so you can call registry methods directly:
 * ```kotlin
 * // Load configuration
 * Namespace.Global.load(configuration)
 *
 * // Get current state
 * val snapshot = Namespace.Global.configuration
 *
 * // Query flags
 * val flag = Namespace.Global.flag(MY_FLAG)
 * ```
 *
 * @property id Unique identifier for this namespace
 */
open class Namespace(
    val id: String,
    @PublishedApi internal val registry: NamespaceRegistry = NamespaceRegistry(namespaceId = id),
    /**
     * Seed used to construct stable [io.amichne.konditional.values.FeatureId] values for features.
     *
     * By default this is the namespace [id], which is appropriate for "real" namespaces that are intended
     * to be federated and stable within an application.
     *
     * Test-only/ephemeral namespaces should provide a per-instance unique seed to avoid collisions.
     */
    @PublishedApi internal val identifierSeed: String = id,
) : NamespaceRegistry by registry {

    init {
        require(id.isNotBlank()) { "Namespace id must not be blank" }
        require(!id.contains(SEPARATOR)) { "Namespace id must not contain '$SEPARATOR': '$id'" }
        require(identifierSeed.isNotBlank()) { "Namespace identifierSeed must not be blank" }
        require(!identifierSeed.contains(SEPARATOR)) {
            "Namespace identifierSeed must not contain '$SEPARATOR': '$identifierSeed'"
        }
    }

    /**
     * Global namespace containing shared flags accessible across the application.
     *
     * Use this namespace for:
     * - System-wide kill switches
     * - Maintenance mode flags
     * - Cross-cutting feature toggles
     * - Common infrastructure flags
     *
     * Example:
     * ```kotlin
     * object CoreFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
     *     val KILL_SWITCH by boolean(default = false)
     * }
     * ```
     */
    data object Global : Namespace("global")

    /**
     * Consumer-defined namespaces.
     *
     * Konditional only provides [Global]. Consumers define any additional namespaces in their own codebase by
     * extending [Namespace].
     *
     * Example:
     * ```kotlin
     * object Payments : Namespace("payments")
     * object Auth : Namespace("auth")
     * ```
     */
//     Intentionally no additional built-in namespaces.

    @TestOnly
    abstract class TestNamespaceFacade(id: String) : Namespace(
        id = id,
        registry = NamespaceRegistry(namespaceId = id),
        identifierSeed = UUID.randomUUID().toString(),
    )

    /**
     * Sets a test-scoped override for a feature flag.
     * Internal API used by test utilities.
     */
    @PublishedApi
    internal fun <T : Any, C : Context, M : Namespace> setOverride(
        feature: Feature<T, C, M>,
        value: T,
    ) {
        (registry as InMemoryNamespaceRegistry).setOverride(feature, value)
    }

    /**
     * Clears a test-scoped override for a feature flag.
     * Internal API used by test utilities.
     */
    @PublishedApi
    internal fun <T : Any, C : Context, M : Namespace> clearOverride(
        feature: Feature<T, C, M>,
    ) {
        (registry as InMemoryNamespaceRegistry).clearOverride(feature)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Namespace) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "Namespace($id)"
}
