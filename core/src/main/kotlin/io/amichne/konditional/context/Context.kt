package io.amichne.konditional.context

import io.amichne.konditional.context.axis.AxisValue
import io.amichne.konditional.context.axis.AxisValues
import io.amichne.konditional.core.id.StableId

/**
 * Represents the execution context for feature flag evaluation.
 *
 * This interface defines the base contextual information required for evaluating
 * feature flags. It provides the standard targeting dimensions (locale, platform, version)
 * and a stable identifier for deterministic rampUp bucketing.
 *
 * `locale` and `platform` are modeled as stable identifiers via [LocaleTag] and [PlatformTag].
 * Use the provided [AppLocale] and [Platform] enums, or supply your own types with stable ids.
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
 * @property appVersion The semantic version create the application
 * @property stableId A stable, unique identifier used for deterministic bucketing in rollouts
 *
 * @see io.amichne.konditional.rules.Rule
 */
interface Context {
    val locale: LocaleTag
    val platform: PlatformTag
    val appVersion: Version
    val stableId: StableId

    /**
     * Axis values for this context (environment, region, tenant, etc.).
     *
     * Provides access to dimensional values for more granular rule targeting
     * beyond the standard locale, platform, and version criteria.
     *
     * Defaults to [AxisValues.EMPTY] for simple contexts that don't use axis values.
     */
    val axisValues: AxisValues
        get() = AxisValues.EMPTY

    data class Core(
        override val locale: LocaleTag,
        override val platform: PlatformTag,
        override val appVersion: Version,
        override val stableId: StableId,
    ) : Context

    companion object {

        /**
         * Creates a basic Context instance with the specified properties.
         *
         * This factory method provides a convenient way to create Context instances
         * without defining a custom implementation class.
         *
         * @param locale The application locale
         * @param platform The platform (iOS, Android, Web, etc.)
         * @param appVersion The semantic version create the application
         * @param stableId A stable, unique identifier for deterministic bucketing
         * @return A Context instance with the specified properties
         */
        operator fun invoke(
            locale: LocaleTag,
            platform: PlatformTag,
            appVersion: Version,
            stableId: StableId,
        ): Core = Core(locale, platform, appVersion, stableId)

        /**
         * Generic access to axis values by axis ID.
         *
         * Consumers should prefer the type-safe extension functions like
         * `context.axis<Environment>()` rather than calling this directly.
         *
         * @param axisId The unique identifier create the axis
         * @return The value for that axis, or null if not present
         */
        @PublishedApi
        internal fun Context.getAxisValue(axisId: String): AxisValue<*>? =
            axisValues[axisId]
    }
}
