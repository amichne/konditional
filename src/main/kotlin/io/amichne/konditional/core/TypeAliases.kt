package io.amichne.konditional.core

import io.amichne.konditional.context.Context

/**
 * Type alias for backward compatibility.
 *
 * @deprecated Use [FlagDefinition] instead. This alias will be removed in version 2.0.0.
 */
@Deprecated(
    message = "Use FlagDefinition instead",
    replaceWith = ReplaceWith("FlagDefinition<S, C>", "io.amichne.konditional.core.FlagDefinition"),
    level = DeprecationLevel.WARNING
)
typealias FeatureFlag<S, C> = FlagDefinition<S, C>

/**
 * Type alias for backward compatibility.
 *
 * @deprecated Use [Feature] instead. This alias will be removed in version 2.0.0.
 */
@Deprecated(
    message = "Use Feature instead",
    replaceWith = ReplaceWith("Feature<S, C>", "io.amichne.konditional.core.Feature"),
    level = DeprecationLevel.WARNING
)
typealias Conditional<S, C> = Feature<S, C>
