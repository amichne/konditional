package io.amichne.konditional.core

import kotlin.reflect.KProperty

sealed interface FeatureName {
    val name: String

    companion object {
        operator fun <E> invoke(
            name: String,
        ): FeatureName where E : Enum<E>, E : Feature<*, *, *, *> = FeatureNameImpl(name)

        fun <E> KProperty<E>.reflect(): FeatureName where E : Enum<E>, E : Feature<*, *, *, *> = FeatureNameImpl(name)

        private data class FeatureNameImpl(
            override val name: String,
        ) : FeatureName
    }
}
