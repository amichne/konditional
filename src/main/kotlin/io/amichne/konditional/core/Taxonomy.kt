package io.amichne.konditional.core

/**
 * Represents a feature flag taxonomy with isolated configuration and runtime isolation.
 *
 * Modules provide:
 * - **Compile-time isolation**: Features are type-bound to their taxonomy
 * - **Runtime isolation**: Each taxonomy has its own [ModuleRegistry] instance
 * - **Governance**: All modules enumerated in one sealed hierarchy
 * - **Type safety**: Taxonomy identity is encoded in the type system
 *
 * ## Taxonomy Types
 *
 * ### Core Taxonomy
 * The [Core] taxonomy contains shared flags accessible to all teams:
 * ```kotlin
 * enum class CoreFeatures(override val key: String)
 *     : Feature<BoolEncodeable, Boolean, Context, Taxonomy.Core> {
 *     KILL_SWITCH("kill_switch");
 *     override val taxonomy = Taxonomy.Core
 * }
 * ```
 *
 * ### Domain Modules
 * Domain modules provide isolated namespaces for functional areas:
 * ```kotlin
 * enum class PaymentFeatures(override val key: String)
 *     : Feature<BoolEncodeable, Boolean, Context, Taxonomy.Domain.Payments> {
 *     APPLE_PAY("apple_pay");
 *     override val taxonomy = Taxonomy.Domain.Payments
 * }
 * ```
 *
 * ## Adding New Modules
 *
 * To add a new team taxonomy, add an object to the [Domain] sealed class:
 * ```kotlin
 * sealed class Domain(id: String) : Taxonomy(id) {
 *     // ... existing teams ...
 *     data object NewTeam : Domain("new-team")
 * }
 * ```
 *
 * The sealed hierarchy ensures:
 * - No taxonomy ID collisions at compile time
 * - Exhaustive when-expressions
 * - IDE autocomplete for all modules
 *
 * @property id Unique identifier for this taxonomy
 * @property registry Isolated registry instance for this taxonomy's flags
 */
sealed class Taxonomy(val id: String) {
    /**
     * Isolated flag registry for this taxonomy.
     *
     * Each taxonomy gets its own registry instance, ensuring complete runtime isolation
     * between teams. Flags from different modules cannot interfere with each other.
     */
    abstract var registry: ModuleRegistry

    /**
     * Core taxonomy containing shared flags accessible to all teams.
     *
     * Use this taxonomy for:
     * - System-wide kill switches
     * - Maintenance mode flags
     * - Cross-cutting feature toggles
     * - Common infrastructure flags
     *
     * Example:
     * ```kotlin
     * Taxonomy.Core.config {
     *     CoreFeatures.KILL_SWITCH with { default(false) }
     * }
     * ```
     */
    data object Core : Taxonomy("core") {
        override var registry: ModuleRegistry = ModuleRegistry.create()
    }

    /**
     * Domain modules providing isolated namespaces for functional areas.
     *
     * Each team taxonomy:
     * - Has its own registry instance (runtime isolation)
     * - Is type-bound to its features (compile-time isolation)
     * - Can be serialized/deployed independently
     * - Cannot access other teams' flags
     *
     * ## Governance
     *
     * New team modules are added here via PR. This ensures:
     * - Central visibility of all modules
     * - No duplicate taxonomy IDs
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
    sealed class Domain(id: String) : Taxonomy(id) {
        override var registry: ModuleRegistry = ModuleRegistry.create()

        /** Authentication and authorization features */
        data object Authentication : Domain("auth")

        /** Payment processing and checkout features */
        data object Payments : Domain("payments")

        /** Messaging, chat, and notification features */
        data object Messaging : Domain("messaging")

        /** Search functionality and algorithms */
        data object Search : Domain("search")

        /** Recommendation engine and personalization */
        data object Recommendations : Domain("recommendations")

        // Add your organization's team modules here:
        // data object YourTeam : Domain("your-team")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Taxonomy) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "Taxonomy($id)"
}
