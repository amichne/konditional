package io.amichne.konditional.context

import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.Taxonomy
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.types.EncodableValue

/**
 * Represents the execution context for feature flag evaluation.
 *
 * This interface defines the base contextual information required for evaluating
 * feature flags. It provides the standard targeting dimensions (locale, platform, version)
 * and a stable identifier for deterministic rollout bucketing.
 *
 * You can extend this interface to add custom fields for domain-specific targeting:
 * ```kotlin
 * data class EnterpriseContext(
 *     override val locale: AppLocale,
 *     override val platform: Platform,
 *     override val appVersion: Version,
 *     override val stableId: StableId,
 *     val organizationId: String,
 *     val subscriptionTier: SubscriptionTier,
 * ) : Context
 * ```
 *
 * @property locale The application locale for this context
 * @property platform The platform (iOS, Android, Web, etc.) for this context
 * @property appVersion The semantic version of the application
 * @property stableId A stable, unique identifier used for deterministic bucketing in rollouts
 *
 * @see io.amichne.konditional.core.registry.RegistryScope
 * @see io.amichne.konditional.rules.Rule
 */
interface Context {
    val locale: AppLocale
    val platform: Platform
    val appVersion: Version
    val stableId: StableId

    companion object {
        /**
         * Creates a basic Context instance with the specified properties.
         *
         * This factory method provides a convenient way to create Context instances
         * without defining a custom implementation class.
         *
         * @param locale The application locale
         * @param platform The platform (iOS, Android, Web, etc.)
         * @param appVersion The semantic version of the application
         * @param stableId A stable, unique identifier for deterministic bucketing
         * @return A Context instance with the specified properties
         */
        operator fun invoke(
            locale: AppLocale,
            platform: Platform,
            appVersion: Version,
            stableId: StableId,
        ): Context = object : Context {
            override val locale: AppLocale = locale
            override val platform: Platform = platform
            override val appVersion: Version = appVersion
            override val stableId: StableId = stableId
        }

        /**
         * Evaluates a specific feature flag in the context of this [Context].
         *
         * This extension function provides convenient access to flag evaluation.
         * The feature's taxonomy registry is automatically used.
         *
         * @param key The feature flag to evaluate
         * @return The evaluated value of type [T]
         * @throws IllegalStateException if the flag is not found in the registry
         * @param S The EncodableValue type wrapping the actual value
         * @param T The actual value type
         * @param C The type of the context
         * @param M The taxonomy the feature belongs to
         */
        fun <S : EncodableValue<T>, T : Any, C : Context, M : Taxonomy> C.evaluate(
            key: Feature<S, T, C, M>,
        ): T = key.module.featureFlag(key)?.evaluate(this)
               ?: throw IllegalStateException("Flag not found, Key: ${key.key}, Taxonomy: ${key.module.id}")
    }
}
