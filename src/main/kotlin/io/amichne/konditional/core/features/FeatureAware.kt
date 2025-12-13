package io.amichne.konditional.core.features

import io.amichne.konditional.core.Namespace

/**
 * Mixin interface for types that have access to a [FeatureContainer].
 *
 * This is primarily used by [FeatureContainer] itself, which implements this interface
 * to enable the feature evaluation DSL through extension functions.
 *
 * @param M The namespace type of the feature container
 */
interface FeatureAware<M : Namespace> {
    /**
     * The feature container this instance has access to.
     */
    val container: FeatureContainer<M>
}
