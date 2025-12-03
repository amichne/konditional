package io.amichne.konditional.core.features

import io.amichne.konditional.core.Namespace

@Deprecated("Use FeatureAware instead", ReplaceWith("FeatureAware<M>"))
typealias Featurized<M> = FeatureAware<M>

interface FeatureAware<M : Namespace> {
    val container: FeatureContainer<M>

    companion object {
        inline operator fun <reified M : Namespace> invoke(
            container: FeatureContainer<M>,
        ): FeatureAware<M> = object : FeatureAware<M> {
            override val container: FeatureContainer<M> = container
        }
    }
}
