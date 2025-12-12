package io.amichne.konditional.context

import io.amichne.konditional.core.id.StableId

/**
 * Shared base implementation of [Context].
 *
 * This class is intended to be used as a delegate in platform-specific Context implementations.
 *
 * The properties are marked as `final` to prevent overriding, ensuring consistent behavior
 * across different Context implementations. This design choice helps maintain the integrity of the core context
 * attributes while allowing for extension through composition.
 *
 * @property locale
 * @property platform
 * @property appVersion
 * @property stableId
 * @property dimensions
 * @constructor Create empty Core context
 */
abstract class CoreContext(
    final override val locale: AppLocale,
    final override val platform: Platform,
    final override val appVersion: Version,
    final override val stableId: StableId,
    final override val dimensions: Dimensions,
) : Context
