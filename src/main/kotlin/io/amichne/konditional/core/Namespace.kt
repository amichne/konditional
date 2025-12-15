package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.registry.InMemoryNamespaceRegistry
import io.amichne.konditional.core.registry.NamespaceRegistry
import io.amichne.konditional.core.types.EncodableValue
import org.jetbrains.annotations.TestOnly
import java.util.UUID

/**
 * Represents a feature flag namespace with isolated configuration and runtime isolation.
 *
 * Taxonomies provide:
 * - **Compile-time isolation**: Features are type-bound to their namespace
 * - **Runtime isolation**: Each namespace has its own flag registry
 * - **Type safety**: Namespace identity is encoded in the type system
 * - **Direct registry operations**: Taxonomies implement [NamespaceRegistry], eliminating the need for `.registry` access
 *
 * ## Namespace Types
 *
 * ### Global Namespace
 * The [Global] namespace contains shared flags accessible to all teams:
 * ```kotlin
 * enum class CoreFeatures(override val key: String)
 *     : Feature<BoolEncodeable, Boolean, Context, Namespace.Global> {
 *     KILL_SWITCH("kill_switch");
 *     override val namespace = Namespace.Global
 * }
 * ```
 *
 * ### Domain Modules
 * Domain modules provide isolated namespaces for functional areas:
 * ```kotlin
 * enum class PaymentFeatures(override val key: String)
 *     : Feature<BoolEncodeable, Boolean, Context, Namespace.Payments> {
 *     APPLE_PAY("apple_pay");
 *     override val namespace = Namespace.Payments
 * }
 * ```
 *
 * ## Adding New Modules
 *
 * Define a namespace in your module, and have it own its feature containers:
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
 * Taxonomies implement [NamespaceRegistry], so you can call registry methods directly:
 * ```kotlin
 * // Load configuration
 * Namespace.Global.load(configuration)
 *
 * // Get current state
 * val snapshot = Namespace.Global.configuration()
 *
 * // Query flags
 * val flag = Namespace.Global.flag(MY_FLAG)
 * ```
 *
 * @property id Unique identifier for this namespace
 */
open class Namespace(
    val id: String,
    @PublishedApi internal val registry: NamespaceRegistry = NamespaceRegistry(),
    /**
     * Seed used to construct stable [io.amichne.konditional.values.Identifier] values for features.
     *
     * By default this is the namespace [id], which is appropriate for "real" namespaces that are intended
     * to be federated and stable within an application.
     *
     * Test-only/ephemeral namespaces should provide a per-instance unique seed to avoid collisions.
     */
    @PublishedApi internal val identifierSeed: String = id,
) : NamespaceRegistry by registry {

    /**
     * Global namespace containing shared flags accessible to all teams.
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

    /** Authentication and authorization features */
    data object Authentication : Namespace("auth")

    /** Payment processing and checkout features */
    data object Payments : Namespace("payments")

    /** Messaging, chat, and notification features */
    data object Messaging : Namespace("messaging")

    /** Search functionality and algorithms */
    data object Search : Namespace("search")

    /** Recommendation engine and personalization */
    data object Recommendations : Namespace("recommendations")

    /**
     * Domain modules providing isolated namespaces for functional areas.
     *
     * Each team namespace:
     * - Has its own registry instance (runtime isolation)
     * - Is type-bound to its features (compile-time isolation)
     * - Can be serialized/deployed independently
     * - Cannot access other teams' flags
     *
     * ## Governance
     *
     * New team modules are added here via PR. This ensures:
     * - Central visibility of all modules
     * - No duplicate namespace IDs
     * - Clear ownership tracking
     *
     * ## Example Teams
     *
     * The modules below represent example functional areas. In a real system,
     * you would add your organization's actual team modules:
     *
     * - **Authentication**: Login, SSO, MFA flags
     * - **Payments**: Payment methods, checkout features
     * - **Messaging**: Chat, notifications, email features
     * - **Search**: Search algorithms, ranking experiments
     * - **Recommendations**: Recommendation engine flags
     */
    sealed class Domain(id: String) : Namespace(id) {

        // Add your organization's team modules here:
        // data object YourTeam : Domain("your-team")
    }

    @TestOnly
    abstract class TestNamespaceFacade(id: String) : Namespace(
        id = id,
        registry = NamespaceRegistry(),
        identifierSeed = UUID.randomUUID().toString(),
    )

    /**
     * Sets a test-scoped override for a feature flag.
     * Internal API used by test utilities.
     */
    @PublishedApi
    internal fun <S : EncodableValue<T>, T : Any, C : Context, M : Namespace> setOverride(
        feature: Feature<S, T, C, M>,
        value: T,
    ) {
        (registry as InMemoryNamespaceRegistry).setOverride(feature, value)
    }

    /**
     * Clears a test-scoped override for a feature flag.
     * Internal API used by test utilities.
     */
    @PublishedApi
    internal fun <S : EncodableValue<T>, T : Any, C : Context, M : Namespace> clearOverride(
        feature: Feature<S, T, C, M>,
    ) {
        (registry as InMemoryNamespaceRegistry).clearOverride(feature)
    }

    /**
     * Sets a test-scoped override for a feature flag.
     * Internal API used by test utilities.
     */
    @PublishedApi
    internal fun <S : EncodableValue<T>, T : Any, C : Context, M : Namespace> setOverride(
        feature: Feature<S, T, C, M>,
        value: T,
    ) {
        (registry as InMemoryNamespaceRegistry).setOverride(feature, value)
    }

    /**
     * Clears a test-scoped override for a feature flag.
     * Internal API used by test utilities.
     */
    @PublishedApi
    internal fun <S : EncodableValue<T>, T : Any, C : Context, M : Namespace> clearOverride(
        feature: Feature<S, T, C, M>,
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
