package io.amichne.konditional.core

/**
 * Represents a feature flag featureModule with isolated configuration and runtime isolation.
 *
 * Modules provide:
 * - **Compile-time isolation**: Features are type-bound to their featureModule
 * - **Runtime isolation**: Each featureModule has its own [ModuleRegistry] instance
 * - **Governance**: All modules enumerated in one sealed hierarchy
 * - **Type safety**: FeatureModule identity is encoded in the type system
 *
 * ## FeatureModule Types
 *
 * ### Core FeatureModule
 * The [Core] featureModule contains shared flags accessible to all teams:
 * ```kotlin
 * enum class CoreFeatures(override val key: String)
 *     : Feature<BoolEncodeable, Boolean, Context, FeatureModule.Core> {
 *     KILL_SWITCH("kill_switch");
 *     override val featureModule = FeatureModule.Core
 * }
 * ```
 *
 * ### Team Modules
 * Team modules provide isolated namespaces for functional areas:
 * ```kotlin
 * enum class PaymentFeatures(override val key: String)
 *     : Feature<BoolEncodeable, Boolean, Context, FeatureModule.Team.Payments> {
 *     APPLE_PAY("apple_pay");
 *     override val featureModule = FeatureModule.Team.Payments
 * }
 * ```
 *
 * ## Adding New Modules
 *
 * To add a new team featureModule, add an object to the [Team] sealed class:
 * ```kotlin
 * sealed class Team(id: String) : FeatureModule(id) {
 *     // ... existing teams ...
 *     data object NewTeam : Team("new-team")
 * }
 * ```
 *
 * The sealed hierarchy ensures:
 * - No featureModule ID collisions at compile time
 * - Exhaustive when-expressions
 * - IDE autocomplete for all modules
 *
 * @property id Unique identifier for this featureModule
 * @property registry Isolated registry instance for this featureModule's flags
 */
sealed class FeatureModule(val id: String) {
    /**
     * Isolated flag registry for this featureModule.
     *
     * Each featureModule gets its own registry instance, ensuring complete runtime isolation
     * between teams. Flags from different modules cannot interfere with each other.
     */
    abstract val registry: ModuleRegistry

    /**
     * Core featureModule containing shared flags accessible to all teams.
     *
     * Use this featureModule for:
     * - System-wide kill switches
     * - Maintenance mode flags
     * - Cross-cutting feature toggles
     * - Common infrastructure flags
     *
     * Example:
     * ```kotlin
     * FeatureModule.Core.config {
     *     CoreFeatures.KILL_SWITCH with { default(false) }
     * }
     * ```
     */
    data object Core : FeatureModule("core") {
        override val registry: ModuleRegistry = ModuleRegistry.create()
    }

    /**
     * Team modules providing isolated namespaces for functional areas.
     *
     * Each team featureModule:
     * - Has its own registry instance (runtime isolation)
     * - Is type-bound to its features (compile-time isolation)
     * - Can be serialized/deployed independently
     * - Cannot access other teams' flags
     *
     * ## Governance
     *
     * New team modules are added here via PR. This ensures:
     * - Central visibility of all modules
     * - No duplicate featureModule IDs
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
    sealed class Team(id: String) : FeatureModule(id) {
        override val registry: ModuleRegistry = ModuleRegistry.create()

        /** Authentication and authorization features */
        data object Authentication : Team("auth")

        /** Payment processing and checkout features */
        data object Payments : Team("payments")

        /** Messaging, chat, and notification features */
        data object Messaging : Team("messaging")

        /** Search functionality and algorithms */
        data object Search : Team("search")

        /** Recommendation engine and personalization */
        data object Recommendations : Team("recommendations")

        // Add your organization's team modules here:
        // data object YourTeam : Team("your-team")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FeatureModule) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "FeatureModule($id)"
}
