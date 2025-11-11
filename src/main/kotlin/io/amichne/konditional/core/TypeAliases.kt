package io.amichne.konditional.core

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
