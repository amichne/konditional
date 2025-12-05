package io.amichne.konditional.kontext

import io.amichne.konditional.core.Namespace

@Deprecated("Use KontextAware instead", ReplaceWith("KontextAware<C>"))
typealias Contextualized<C> = KontextAware<C, *>

fun interface KontextAware<C : Kontext<M>, M : Namespace> {
    fun factory(): C
    val kontext: C get() = factory()

    fun interface Factory<out C : Kontext<*>> {
        fun kontext(): C
    }
}
